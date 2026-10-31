import { useCallback, useEffect, useRef, useState } from 'react'
import {
  confirmIntakeDraft,
  createIntakeDraft,
  getIntakeDraft,
  listEvents,
} from '../../api/generated/afterbooth'
import type {
  Condition,
  ConfirmIntakeRequest,
  DraftCategory,
  IntakeDraft,
  Item,
} from '../../api/generated/model'
import { ApiError } from '../../api/http'

const POLL_INTERVAL_MS = 1000

export type EditableLine = {
  lineId: number
  originalLabel: string
  category: DraftCategory
  quantity: string
  width: string
  depth: string
  height: string
  condition: Condition
  needsReview: boolean
  boxes: IntakeDraft['lines'][number]['boxes']
}

// the client throws ApiError for non-2xx, so a resolved response is always the success body
const bodyOf = <T>(res: { data: unknown }) => res.data as T

const messageOf = (e: unknown, fallback: string) => (e instanceof ApiError ? e.message : fallback)

const toEditable = (line: IntakeDraft['lines'][number]): EditableLine => ({
  lineId: line.lineId,
  originalLabel: line.label,
  category: line.category,
  quantity: String(line.count),
  width: line.sizeCm ? String(line.sizeCm.width) : '',
  depth: line.sizeCm ? String(line.sizeCm.depth) : '',
  height: line.sizeCm ? String(line.sizeCm.height) : '',
  condition: line.condition,
  needsReview: line.needsReview,
  boxes: line.boxes,
})

const isPositiveInt = (value: string) => /^\d+$/.test(value) && Number(value) >= 1

export function problemOf(line: EditableLine): string | undefined {
  if (line.category === 'UNKNOWN') return '분류를 골라 주세요'
  if (!isPositiveInt(line.quantity)) return '수량은 1개 이상이어야 해요'
  if (![line.width, line.depth, line.height].every(isPositiveInt)) {
    return '크기를 1cm 이상으로 입력해 주세요'
  }
  return undefined
}

function toRequest(lines: EditableLine[]): ConfirmIntakeRequest {
  return {
    lines: lines.map((line) => ({
      lineId: line.lineId,
      category: line.category as Exclude<DraftCategory, 'UNKNOWN'>,
      quantity: Number(line.quantity),
      sizeCm: {
        width: Number(line.width),
        depth: Number(line.depth),
        height: Number(line.height),
      },
      condition: line.condition,
    })),
  }
}

export function useIntake() {
  const [photoUrl, setPhotoUrl] = useState<string>()
  const [draft, setDraft] = useState<IntakeDraft>()
  const [lines, setLines] = useState<EditableLine[]>([])
  const [uploading, setUploading] = useState(false)
  const [confirming, setConfirming] = useState(false)
  const [error, setError] = useState<string>()
  const [items, setItems] = useState<Item[]>()
  // a retry of the same confirm request reuses its key (D-019); an edited request is a new action
  const confirmAttempt = useRef<{ body: string; key: string }>(undefined)

  const receive = useCallback((next: IntakeDraft) => {
    setDraft(next)
    if (next.status === 'READY') setLines(next.lines.map(toEditable))
  }, [])

  useEffect(() => {
    if (draft?.status !== 'PROCESSING') return
    let cancelled = false
    const timer = setTimeout(async () => {
      try {
        const res = await getIntakeDraft(draft.id)
        if (!cancelled) receive(bodyOf<IntakeDraft>(res))
      } catch (e) {
        if (cancelled) return
        setDraft(undefined)
        setError(messageOf(e, '분석 상태를 확인하지 못했어요. 다시 찍어 주세요.'))
      }
    }, POLL_INTERVAL_MS)
    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [draft, receive])

  useEffect(() => {
    return () => {
      if (photoUrl) URL.revokeObjectURL(photoUrl)
    }
  }, [photoUrl])

  const upload = useCallback(
    async (photo: File) => {
      setPhotoUrl(URL.createObjectURL(photo))
      setDraft(undefined)
      setLines([])
      setItems(undefined)
      setError(undefined)
      setUploading(true)
      try {
        const event = (await listEvents()).data[0]
        if (!event) {
          setError('등록할 행사를 찾지 못했어요.')
          return
        }
        const res = await createIntakeDraft(
          { eventId: event.id, photo },
          { 'Idempotency-Key': crypto.randomUUID() },
        )
        receive(bodyOf<IntakeDraft>(res))
      } catch (e) {
        setError(messageOf(e, '사진을 올리지 못했어요. 다시 찍어 주세요.'))
      } finally {
        setUploading(false)
      }
    },
    [receive],
  )

  const editLine = useCallback((lineId: number, patch: Partial<EditableLine>) => {
    setLines((current) => current.map((l) => (l.lineId === lineId ? { ...l, ...patch } : l)))
  }, [])

  const canConfirm = lines.length > 0 && lines.every((l) => !problemOf(l))

  const confirm = useCallback(async () => {
    if (!draft || !canConfirm) return
    const request = toRequest(lines)
    const body = JSON.stringify(request)
    if (confirmAttempt.current?.body !== body) {
      confirmAttempt.current = { body, key: crypto.randomUUID() }
    }
    setError(undefined)
    setConfirming(true)
    try {
      const res = await confirmIntakeDraft(draft.id, request, {
        'Idempotency-Key': confirmAttempt.current.key,
      })
      setItems(bodyOf<Item[]>(res))
    } catch (e) {
      setError(messageOf(e, '재고로 등록하지 못했어요. 다시 시도해 주세요.'))
    } finally {
      setConfirming(false)
    }
  }, [draft, lines, canConfirm])

  return {
    photoUrl,
    draft,
    lines,
    uploading,
    confirming,
    error,
    items,
    canConfirm,
    upload,
    editLine,
    confirm,
  }
}
