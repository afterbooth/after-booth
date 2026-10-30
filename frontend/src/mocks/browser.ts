import { setupWorker } from 'msw/browser'
import { getAfterBoothAPIMock } from '../api/generated/afterbooth.msw'

// For development before the backend is ready. Uses the handlers generated from contract/openapi.yaml
export const worker = setupWorker(...getAfterBoothAPIMock())
