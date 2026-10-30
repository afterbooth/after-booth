# Code templates

Copy, rename and trim. These files are not compiled. They show the shape we want, using a simplified `Item` from the inventory domain. Real field lists come from `docs/SPEC.md` and `contract/openapi.yaml`.

Rules behind them: [`docs/CONVENTIONS.md`](../CONVENTIONS.md). The backend layering is enforced by `ArchitectureTest`.

## Backend (`backend/`)

| File | Shows |
|---|---|
| `Item.java` | Entity with behavior, static factory, no setters, other aggregates by ID |
| `ItemRepository.java` | Conditional `UPDATE` for concurrency-safe quantity changes |
| `ItemService.java` | One use case per method, injected `Clock`, typed errors |
| `ItemController.java` | HTTP only, `@Valid`, returns DTO |
| `ItemDtos.java` | Request/response records with validation and `from(entity)` |
| `ItemTest.java` | Plain JUnit domain rule test |
| `ItemServiceConcurrencyTest.java` | Testcontainers test: 100 threads, 2 items, exactly 2 succeed |

When you need a new error, add it to `com.afterbooth.global.error.ErrorCode` with its HTTP status and Korean message (the templates use `PICKUP_CLOSED` and `QTY_UNAVAILABLE`, which do not exist yet). The `code` string is what the frontend and the allocation agent read.

## Frontend (`frontend/`)

| File | Shows |
|---|---|
| `IntakeScreen.tsx` | Screen = layout + states (loading, error, empty, data). No fetch inside |
| `useIntake.ts` | Hook that calls the orval-generated client and owns the state |
| `IntakeScreen.test.tsx` | Vitest + Testing Library + MSW, one behavior per test |
