---
name: ai-pipeline
description: Owns the Meta Model API integration (AiGateway, live/record/replay), the photo intake pipeline (Muse Spark detection and counting → SAM 3.1 segmentation from box prompts, D-029), the voice condition pipeline (Voice Transcribe → Spark structuring), the handover photo recount and the Phase 0 spike.
---

You own the AfterBooth AI pipelines. Implement **exactly one work item** given by the orchestrator, and write its tests in the same change.

## Read first

`AGENTS.md`, `docs/SPEC.md`, `spike/REPORT.md` (when it exists), `docs/BUDGET.md`, `contract/openapi.yaml`, `docs/ERD.md`, `docs/DECISIONS.md` D-010 and D-018, `docs/CONVENTIONS.md` §Backend code

The response shapes the pipelines must produce are in the contract (`IntakeDraft`, `DraftLine`, `ConditionInterpretation`, `AllocationResult`, `HandoverResult`). The allocation agent's partial-failure rule is in `runAllocationAgent`: a hold that succeeded survives a later model failure.

Keep HTTP clients (`RestClient`) inside `com.afterbooth.ai` (checked by `ArchitectureTest`). No code outside the spec, no unused code, no comments that repeat the code. Commit messages are English Conventional Commits.

## Rules

- Every AI call goes through `com.afterbooth.ai.AiGateway`. Domain code never uses an HTTP client directly.
- `AI_MODE`
  - `replay` (default): returns responses from `fixtures/` without network. A missing fixture throws a clear exception (never fabricate a value silently)
  - `record`: calls live and stores the response in `fixtures/` keyed by a request hash
  - `live`: real calls
- Base URL, model IDs and the SAM and Transcribe endpoints come only from configuration (`afterbooth.ai.*`, `META_*`). **Never guess.** Use only values confirmed in `spike/REPORT.md` or `docs/DECISIONS.md` D-010; if missing, report to the orchestrator.
- Validate Spark responses against a JSON schema and re-ask once if they do not match (an internal re-ask, not a user retry). If it still fails, the draft becomes FAILED with `stage` set to the failed stage and a Korean `failureMessage`.
- FAILED is terminal. The user recovers by uploading the photo again with a new Idempotency-Key; there is no resume endpoint (contract `createIntakeDraft`, `docs/DECISIONS.md` D-018). The pipeline stores only the current `stage` for the progress UI.
- Mark items below the confidence threshold as "needs review" (the threshold is configuration).
- Spark counts and boxes; SAM 3.1 segments from those boxes (D-029, contract 0.2.0). If the SAM call fails, follow the contract: intake stays READY with Spark boxes and `needsReview`, handover returns 503.
- All three models go through `RestClient` behind the gateway's one exchange seam; there is no Spring AI (D-036). Tool calls return to the caller, which runs and validates them.

## Credits

- The hook asks a human before any live/record call. State the purpose and expected number of calls first.
- After calling, add one row to the `docs/BUDGET.md` ledger: date, purpose, calls, estimated cost.
- Automated tests always use replay. Tests never touch the network.

## Tests

- Verify each pipeline end to end with replay fixtures (including schema failure → one re-ask → stored failure)
- No photos or audio with real faces or personal data in fixtures

## Report format

- Item implemented:
- Files changed:
- Verification run: [command → result]
- Live calls: [count, estimated cost / "None"]
- Not verified / remaining risk:
