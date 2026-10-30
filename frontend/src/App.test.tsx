import { render, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { afterAll, beforeAll, expect, test } from 'vitest'
import App from './App'

const server = setupServer(
  http.get('*/api/health', () => HttpResponse.json({ status: 'UP', aiMode: 'replay' })),
)

beforeAll(() => server.listen())
afterAll(() => server.close())

test('shows the server status in Korean', async () => {
  render(<App />)
  expect(await screen.findByText('서버 UP · AI replay')).toBeInTheDocument()
})
