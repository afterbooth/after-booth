// TEMPLATE: screen component. Owns layout and states, delegates data to the hook.
// Presentational pieces go to src/components when a second screen needs them.
import { useIntake } from './useIntake'

export function IntakeScreen() {
  const { state, upload, confirm } = useIntake()

  return (
    <main className="mx-auto min-h-dvh max-w-md px-4 py-6">
      <h1 className="text-xl font-bold">남은 물품 등록</h1>

      <label className="mt-6 block rounded-xl bg-slate-900 py-4 text-center text-white">
        부스 사진 찍기
        <input
          type="file"
          accept="image/*"
          capture="environment"
          className="sr-only"
          disabled={state.status === 'loading'}
          onChange={(e) => e.target.files?.[0] && upload(e.target.files[0])}
        />
      </label>

      {state.status === 'loading' && <p className="mt-6">물품을 세는 중이에요…</p>}
      {state.status === 'error' && (
        <p role="alert" className="mt-6 text-red-600">
          {state.message}
        </p>
      )}
      {state.status === 'confirmed' && <p className="mt-6">재고로 등록했어요.</p>}
      {state.status === 'ready' && (
        <section className="mt-6">
          <ul>
            {state.draft.items.map((item) => (
              <li key={item.category}>
                {item.label} {item.count}개{item.needsReview && ' · 확인 필요'}
              </li>
            ))}
          </ul>
          <button
            className="mt-4 w-full rounded-xl bg-emerald-600 py-4 text-white"
            onClick={() => confirm(state.draft)}
          >
            재고로 등록
          </button>
        </section>
      )}
    </main>
  )
}
