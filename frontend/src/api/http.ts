// orval fetch 클라이언트는 2xx가 아닌 응답도 그대로 resolve하므로, 여기서 ApiError로 바꿔 던진다.
// 화면 코드는 try/catch 하나로 실패를 처리할 수 있다.
export class ApiError extends Error {
  readonly status: number
  readonly code: string

  constructor(status: number, code: string, message: string) {
    super(message)
    this.status = status
    this.code = code
  }
}

export async function http<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(url, options)
  const body = res.status === 204 ? undefined : await res.json().catch(() => undefined)
  if (!res.ok) {
    throw new ApiError(
      res.status,
      body?.code ?? 'UNKNOWN',
      body?.message ?? '요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요.',
    )
  }
  return { data: body, status: res.status, headers: res.headers } as T
}
