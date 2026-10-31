# TASKS

Update at the end of every phase so progress is visible at a glance. Mark blocked items with `BLOCKED — <reason>`.
The unit of work is a GitHub issue. Owners: **DY** = namdongyeob, **WJ** = WookJaes. Both do backend and frontend.

## Phase 0: Bootstrap + spike (no feature implementation)

- [x] 0-1 Repo layout: backend/, frontend/, contract/, docs/, fixtures/, spike/, docker-compose, .gitignore
- [x] 0-2 docs/SPEC.md, TASKS.md, DECISIONS.md (+ BUDGET.md, AGENT_MISTAKES.md)
- [x] 0-3 Harness: AGENTS.md (single rulebook, #25), five .claude agents, feature skill, hooks, .codex
- [x] 0-3b Conventions, review guide, code templates, PR/issue templates, commit-msg hook, ArchitectureTest
- [x] #18 Translate docs, harness and code comments to English (DY)
- [x] #1 Contract draft + `docs/ERD.md` (DY)
- [x] #21 CI checks, required on main (DY)
- [x] #23 Conclusion-first, traceable issue/PR/review templates + `pr-description` check (DY)
- [x] #25 One rulebook: `AGENTS.md` for every agent, `CLAUDE.md` removed (DY)
- [x] #2 Spike: model list, Spark text/image/function calling, SAM 3.1, Transcribe, cost → spike/REPORT.md (DY, key A, $0.44)
- [x] **G0 STOP** — report the REPORT summary and the decisions for humans (all five accepted: D-029–D-033)
- [x] #28 SPEC, contract 0.2.0 and ERD follow the G0 pipeline (DY)

## Phase 1: Features (target 11/1 night KST)

| Issue | What | Owner | Scope |
|---|---|---|---|
| #3 | Domain model, Flyway, seed | WJ | BE |
| #4 | AiGateway live/record/replay | DY | AI |
| #5 | Photo intake (Spark → SAM box prompts → inventory draft → confirm, D-029) + capture screen | DY | BE+AI+FE |
| #6 | Voice pickup request (Transcribe → Spark structuring) + request screen | DY | BE+AI+FE |
| #7 | Reservation core: conditional HOLD, idempotency, state machine, expiry scheduler + required tests | WJ | BE |
| #8 | Vehicle fit + reason codes | WJ | BE |
| #9 | Allocation agent (function calling, server validation, alternatives) + allocation board | WJ | BE+AI+FE |
| #10 | Handover recount + reuse stats + screens | WJ | BE+AI+FE |

- [x] #3 · [x] #4 · [ ] #5 · [x] #6 · [ ] #7 · [ ] #8 · [ ] #9 · [ ] #10

Dependency order: #1 → (#3 and #4 in parallel) → #5·#6 (DY) / #7 → #8 → #9 → #10 (WJ)

## Phase 2: Integration + demo hardening (target 11/2–11/3)

- [ ] #11 Integrate, run everything with one `docker compose up` (DY)
- [ ] #12 Playwright E2E demo scenario, two contexts requesting at once (WJ)
- [ ] #13 Record → replay fallback, latency measurements, loading UX (DY)
- [ ] #14 Self-score against the judging criteria + Muse Code review → fix the weakest area (DY, WJ)

## Phase 3: Submission (target 11/4 morning)

- [ ] #15 English README (DY)
- [ ] #16 Demo script + 2–3 minute video (WJ, key B)
- [ ] #17 Submission checklist (DY, WJ)
- [ ] **G3 STOP** — report how to run, demo results, self-score, remaining risks
