# DECISIONS

One line per decision: what was decided and why. Tag contract changes `[contract]` and work done with Muse Code `[Muse Code]`.

| ID | Date | Decision | Why |
|---|---|---|---|
| D-001 | 2026-10-30 | Spring Boot **4.1.2** + Spring AI **2.0.1** (changed from "3.x" in the original plan) | start.spring.io no longer offers 3.x, and the Spring AI OpenAI starter supports Boot 4.0+ only. The team used Boot 4.1 on finplay |
| D-002 | 2026-10-30 | PostgreSQL 17, local port 15432 | Avoids clashing with a default 5432 |
| D-003 | 2026-10-30 | Branches: main + feat/*, streams in worktrees (`.claude/worktrees/`) | Six-day project, no dev branch |
| D-004 | 2026-10-30 | orval generates the frontend API client and MSW mocks from the contract; generated files are not committed | Keeps contract and frontend in sync. Generated before build, test and dev |
| D-005 | 2026-10-30 | Java is formatted by `spotlessApply` before commit (feature skill), not by an edit hook | Starting Gradle on every edit is too slow. Frontend files get prettier right after each edit |
| D-006 | 2026-10-30 | Five area-based agents (from the spec) instead of finplay's implementer/tester split. The reviewer starts fresh each time and the main session runs verification itself | File ownership across parallel streams matters more. Implementers write their own tests |
| D-007 | 2026-10-30 | A PreToolUse hook asks a human before live/record calls (`AB_LIVE_GATE=ask`). STOP every $50 cumulative | The one human gate that keeps full automation from leaking credits |
| D-008 | 2026-10-30 | The two Meta keys have roles: A (development, spike, record), B (demo rehearsal, recording, live finals reserve) | docs/BUDGET.md |
| D-009 | 2026-10-30 | No fine-tuning in the plan. Revisit at G0 with spike results if needed | Six-day schedule; prompts, schemas and SAM are likely enough for accuracy |
| D-010 | 2026-10-30 | Defaults from the official docs: base `https://api.meta.ai/v1` (OpenAI compatible, `Authorization: Bearer`), Spark `muse-spark-1.3`, SAM `sam-3.1` (`/v1/responses`, `/v1/files`), Transcribe `muse-voice-transcribe-1.0` (`/v1/asr/transcribe`, realtime `wss://api.meta.ai/v1/asr/realtime`) | Checked on dev.meta.ai/docs. Availability is re-checked with `GET /v1/models` in the spike |
| D-011 | 2026-10-30 | Commits, PRs, issues and review comments in English Conventional Commits, enforced by the commit-msg hook | Accessibility for international judges. Korean commits already pushed stay as they are |
| D-012 | 2026-10-30 | Work tracked as GitHub issues #1–#17, split by feature so both do BE+FE. DY: contract, spike, AiGateway, photo intake, voice request, integration, README / WJ: domain, reservation core, vehicle fit, allocation agent, handover, E2E, video | Owned files do not overlap; AI pipelines naturally use key A and the demo key B |
| D-013 | 2026-10-30 | main protection: PR required, 0 approvals, squash only, branch deleted after merge | Keeps automation fast and main history clean |
| D-014 | 2026-10-30 | ArchUnit `ArchitectureTest` enforces layering, domain boundaries, no field injection, no setters, Spring AI isolation and no bare `now()` | Several agents write code, so the rules live in tests rather than only in docs |
| D-015 | 2026-10-30 | orval client mutator `src/api/http.ts` throws `ApiError(code, message)` for non-2xx responses | The generated client resolved failed responses, so screens treated failures as success (Codex review) |
| D-016 | 2026-10-30 | Harness changes verified by two Codex adversarial reviews: 4 findings in the first pass, 1 in the second, all fixed | Requested by the team: Claude builds, Codex verifies |
| D-017 | 2026-10-30 | Everything in the repo is English — docs, harness, comments, test names. Only product UI text and user-facing error messages stay Korean (#18) | A half-Korean, half-English repo looked unfinished to international judges. Cheapest to switch before feature work starts |
