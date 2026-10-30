# Review guide

Every PR gets three reviews before merge, and CI must be green (`backend`, `frontend`, `hygiene`, `pr-description`). Reviews put the verdict first, give each finding an ID, and stay short enough to read in a minute. All review text is in English.

| # | Reviewer | How | Blocks merge on |
|---|---|---|---|
| 1 | Claude `reviewer` agent (fresh session) | `/feature` step 5, or ask Claude to "run the reviewer agent on this PR" | any `issue (blocking)` |
| 2 | Codex | `/codex:review --base main`; add `/codex:adversarial-review --base main <focus>` for concurrency, state machine, AI validation and contract PRs. Post its result in the review format below | any finding marked high severity |
| 3 | Teammate (Muse Code or by hand) | GitHub review in the format below, optional for speed (0 approvals required) | any `issue (blocking)` |

The PR author answers every blocking finding by ID with the fix commit or a reason.

## Review format

One review body, at most 25 lines. Nothing before the verdict; no background paragraphs, no praise section.

```markdown
**Verdict:** Request changes — R1 lets a handover finish without a recount
**Coherence:** OK | Title, "In one sentence" and Closes #N disagree: <one line>
**Counts:** blocking 1 · suggestion 2 · question 1 · nitpick 1

| ID | Label | Where | Finding | Fix |
|---|---|---|---|---|
| R1 | issue (blocking) | contract/openapi.yaml:523 | resolveHandover can complete with no SAM recount | Require the newest handover_log row to be MISMATCH |
| R2 | suggestion | contract/openapi.yaml:1138 | Hold TTL has no value | Pin 15 minutes |
| R3 | question | contract/openapi.yaml:330 | Is a partial HELD allowed? | State the policy |
| R4 | nitpick | docs/ERD.md:69 | Enum annotation missing | Annotate ItemCategory |
```

- Verdict is `Approve` or `Request changes — <the main reason>`.
- One table row per finding, one line per cell. `Where` is `file:line`.
- At most 5 suggestions; drop the rest or merge them.
- When a finding needs more than one line, put the detail in an inline comment on that line, starting with its ID: `R1 issue (blocking): ...` — problem, why, fix, one line each.
- IDs are stable across rounds (R1, R2, ...). New findings in later rounds continue the numbering.

## Re-review format

After the author pushes fixes, review only what changed. At most 15 lines.

```markdown
**Verdict:** Approve
**Previous findings:** 4 resolved, 0 not resolved

| ID | Status | Note (only if not resolved) |
|---|---|---|
| R1 | Resolved | |
| R2 | Resolved | |

**New findings:** None
```

## Labels

[Conventional Comments](https://conventionalcomments.org/) labels say how much a finding matters:

| Label | Meaning |
|---|---|
| `issue (blocking)` | Must fix before merge: a bug, a broken rule, a missing test, a contract or ERD mismatch |
| `suggestion` | Better approach, author decides |
| `question` | Reviewer does not understand. Answer, and consider a clearer name or comment |
| `nitpick` | Tiny style point. Never blocks, table only |

## Checklist

Coherence (check first)

- [ ] The PR title, its "In one sentence" and the linked issue's goal describe the same change
- [ ] The diff does what the title says and nothing unrelated
- [ ] Every acceptance criterion ticked in the PR names its proof

Correctness

- [ ] Quantity, deadline and vehicle checks happen on the server, not only in the AI output
- [ ] The agent can create at most a HOLD, never CONFIRMED
- [ ] Concurrent paths use the conditional UPDATE. Idempotency keys are honored
- [ ] Time uses the injected `Clock` and KST where shown to users

Meta AI usage

- [ ] All AI calls go through `AiGateway`. No model ID or URL is hard-coded
- [ ] AI output leads to an action (inventory, allocation, handover), not just a display
- [ ] Tests and default config use `AI_MODE=replay`

Design and readability ([CONVENTIONS.md](CONVENTIONS.md))

- [ ] Business rules live in domain objects, not in controllers or services
- [ ] No code for features outside `docs/SPEC.md`, no unused code
- [ ] Comments only explain why
- [ ] `ArchitectureTest` passes

Contract and demo

- [ ] Responses match `contract/openapi.yaml`, tables match `docs/ERD.md`
- [ ] The demo scenario in `docs/SPEC.md` still works

Safety

- [ ] No secrets, `.env` or real faces/personal data in fixtures
