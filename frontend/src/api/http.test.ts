import { http as mswHttp, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { afterAll, beforeAll, expect, test } from 'vitest'
import { ApiError, http } from './http'

const server = setupServer(
  mswHttp.get('http://test/ok', () => HttpResponse.json({ value: 1 })),
  mswHttp.get('http://test/conflict', () =>
    HttpResponse.json(
      { code: 'QTY_UNAVAILABLE', message: '남은 수량이 부족해요.' },
      { status: 409 },
    ),
  ),
  mswHttp.get('http://test/broken', () => new HttpResponse('oops', { status: 500 })),
)

beforeAll(() => server.listen())
afterAll(() => server.close())

test('성공 응답은 data와 status를 돌려준다', async () => {
  const res = await http<{ data: { value: number }; status: number }>('http://test/ok')
  expect(res.status).toBe(200)
  expect(res.data.value).toBe(1)
})

test('실패 응답은 서버의 code와 message로 ApiError를 던진다', async () => {
  await expect(http('http://test/conflict')).rejects.toMatchObject({
    status: 409,
    code: 'QTY_UNAVAILABLE',
    message: '남은 수량이 부족해요.',
  })
})

test('본문이 JSON이 아니어도 ApiError를 던진다', async () => {
  await expect(http('http://test/broken')).rejects.toBeInstanceOf(ApiError)
})
