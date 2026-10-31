import { useEffect, useState } from 'react'
import { getHealth } from './api/generated/afterbooth'
import type { Health } from './api/generated/model'
import { RequestScreen } from './features/request/RequestScreen'

// Bootstrap check screen. Replaced in Phase 1 by the capture, pickup request and allocation board screens
function HomeScreen() {
  const [health, setHealth] = useState<Health | null>(null)
  const [error, setError] = useState(false)

  useEffect(() => {
    getHealth()
      .then((res) => setHealth(res.data))
      .catch(() => setError(true))
  }, [])

  return (
    <main className="mx-auto min-h-dvh max-w-md bg-white px-4 py-8 text-slate-900">
      <h1 className="text-2xl font-bold">애프터부스</h1>
      <p className="mt-2 text-slate-600">행사가 끝난 물품을 다음 행사로 연결해요.</p>
      <a
        href="#/request"
        className="mt-6 inline-block rounded-xl bg-emerald-800 px-5 py-3 font-semibold text-white"
      >
        물품 픽업 요청하기
      </a>
      <p className="mt-6 text-sm" data-testid="health">
        {error
          ? '서버 연결 실패'
          : health
            ? `서버 ${health.status} · AI ${health.aiMode}`
            : '서버 확인 중…'}
      </p>
    </main>
  )
}

export default function App() {
  const [hash, setHash] = useState(window.location.hash)
  useEffect(() => {
    const navigate = () => setHash(window.location.hash)
    window.addEventListener('hashchange', navigate)
    return () => window.removeEventListener('hashchange', navigate)
  }, [])
  return hash === '#/request' ? <RequestScreen /> : <HomeScreen />
}
