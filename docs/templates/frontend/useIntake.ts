// TEMPLATE: data hook for one screen. Calls the generated client only. Rename the operations
// to the ones orval generates from contract/openapi.yaml.
// The generated client throws ApiError for any non-2xx response (src/api/http.ts),
// so one catch covers network failures and server errors.
import { useCallback, useRef, useState } from 'react'
import { createIntakeDraft, confirmIntakeDraft } from '../../api/generated/afterbooth'
import type { IntakeDraft } from '../../api/generated/model'
import { ApiError } from '../../api/http'

type State =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'ready'; draft: IntakeDraft }
  | { status: 'confirmed' }

const messageOf = (e: unknown, fallback: string) => (e instanceof ApiError ? e.message : fallback)

export function useIntake() {
  const [state, setState] = useState<State>({ status: 'idle' })
  // 응답 순서가 뒤바뀌어도 마지막 요청의 결과만 화면에 반영한다
  const latest = useRef(0)

  const run = useCallback(async (task: () => Promise<State>) => {
    const id = ++latest.current
    setState({ status: 'loading' })
    const next = await task()
    if (id === latest.current) setState(next)
  }, [])

  const upload = useCallback(
    (photo: File) =>
      run(async () => {
        try {
          const res = await createIntakeDraft({ photo })
          return { status: 'ready', draft: res.data }
        } catch (e) {
          return { status: 'error', message: messageOf(e, '사진을 분석하지 못했어요. 다시 찍어 주세요.') }
        }
      }),
    [run],
  )

  const confirm = useCallback(
    (draft: IntakeDraft) =>
      run(async () => {
        try {
          await confirmIntakeDraft(draft.id, draft)
          return { status: 'confirmed' }
        } catch (e) {
          return { status: 'error', message: messageOf(e, '재고로 등록하지 못했어요. 다시 시도해 주세요.') }
        }
      }),
    [run],
  )

  return { state, upload, confirm }
}
