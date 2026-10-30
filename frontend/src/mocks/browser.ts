import { setupWorker } from 'msw/browser'
import { getAfterBoothAPIMock } from '../api/generated/afterbooth.msw'

// 백엔드 완성 전 개발용. contract/openapi.yaml에서 생성된 핸들러를 그대로 쓴다
export const worker = setupWorker(...getAfterBoothAPIMock())
