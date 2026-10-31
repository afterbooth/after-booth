import { http, HttpResponse } from 'msw'
import {
  getInterpretConditionsMockHandler,
  getListOrganizationsMockHandler,
} from '../../api/generated/afterbooth.msw'
import type { CreatePickupRequest } from '../../api/generated/model'

export const requestHandlers = [
  getListOrganizationsMockHandler([
    { id: 1, name: '주말 행사 운영팀' },
    { id: 2, name: '이웃 문화모임' },
  ]),
  getInterpretConditionsMockHandler({
    context: 'RECEIVER',
    transcript: '주말 행사에 쓸 진열대 세 개가 필요해요. 승용차로 가져갈게요.',
    conditions: {
      category: 'DISPLAY_RACK',
      quantity: 3,
      vehicleType: 'SEDAN',
      purpose: '주말 행사 진열용',
    },
    missingFields: [],
  }),
  http.post('*/api/pickup-requests', async ({ request }) => {
    if (!request.headers.get('Idempotency-Key'))
      return HttpResponse.json(
        { code: 'INVALID_INPUT', message: '다시 저장해 주세요.' },
        { status: 400 },
      )
    const body = (await request.json()) as CreatePickupRequest
    return HttpResponse.json(
      { id: 1, ...body, createdAt: '2026-10-31T12:00:00+09:00' },
      { status: 201 },
    )
  }),
]
