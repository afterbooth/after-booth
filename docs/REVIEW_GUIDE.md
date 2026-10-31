# Review guide

Every PR gets three reviews before merge, and CI must be green (`backend`, `frontend`, `hygiene`, `pr-description`). Each round is posted as **one** review body that starts with the verdict, says what was and was not verified, and gives every finding a stable ID. All review text is in English.

| # | Reviewer | How | Blocks merge on |
|---|---|---|---|
| 1 | Claude `reviewer` agent (fresh session) | `/feature` step 5, or ask Claude to "run the reviewer agent on this PR" | any blocking finding |
| 2 | Codex | `/codex:review --base main`; add `/codex:adversarial-review --base main <focus>` for concurrency, state machine, AI validation and contract PRs | any finding marked high severity |
| 3 | Teammate (Muse Code or by hand) | GitHub review in the format below, optional for speed (0 approvals required) | any blocking finding |

Reviewers 1 and 2 run on the same head commit and are merged into one review: whoever posts verifies each finding against the code, drops what does not hold, and lists both tools in **Reviewers**. Never post one review per tool. The PR author answers in the [author reply format](#author-reply-format).

## Review format

```markdown
## Verdict: Request changes (blocking 1)

### Scope (handover / contract / ERD)

- Blocking 1 / suggestions 2 / notes 2.
- Compared `origin/main` `<sha>` with PR head `<sha>`.
- Coherence: the title, "In one sentence" and #10 describe the same change.
- <What the diff was checked against: the issue goal, SPEC section, contract operations, ERD tables.>

### Build verification

- Head: `<sha>`.
- CI: backend **pass** · frontend **pass** · hygiene **pass** · pr-description **pass**.
- Re-run locally: `./gradlew test --tests "*Handover*"` — 12 passed. (Or: "Not re-run locally.")

### QA

- <Behavior exercised or traced in the code, and what it showed.>
- Not run: <live Meta calls, the demo end to end, anything else not exercised>.

### Blocking

1. **R1 — `resolveHandover` completes without a photo recount** (`contract/openapi.yaml:523`)

   <What the code does, quoting the line.>

   <Why it breaks a rule: SPEC, contract, ERD or a decision, with its location.>

   <The fix.>

### Suggestions (non-blocking)

1. **R2 — Hold TTL has no value** (`contract/openapi.yaml:1138`). <Why, and the fix.>

### Notes

1. **R3 — question:** Is a partial HELD allowed? (`contract/openapi.yaml:330`)
2. Confirmed: <a risky part that checked out, so the author knows it was looked at>.

Reviewers: Claude `reviewer` (<model>) · Codex `/codex:review` (<model>, <what it found or could not check>) · <who verified and posted>.
```

- The heading is `## Verdict: Approve (no blocking)` or `## Verdict: Request changes (blocking N)`.
- Every finding is `**R<n> — <one-line title>** (<file:line>)`. IDs are stable across rounds; new findings continue the numbering.
- Blocking items get the problem, the rule it breaks and the fix, each in its own short paragraph. Suggestions and notes stay one item each.
- A section with nothing in it says `None.` Keep it, so agents can find every heading.
- Write only what you verified. Anything you did not run or could not check goes under **Not run** in QA, never as a pass.
- No tables in a review. Use headings, bullets and numbered lists.

## Re-review format

Same headings, plus **Previous findings** right after **Scope**: one numbered item per earlier ID. Review only what changed since the last round.

```markdown
## Verdict: Approve (no blocking)

### Scope (re-review / handover)

- Blocking 0 / suggestions 1 / notes 1.
- Re-reviewed up to `<sha>` since `<previous sha>`.

### Previous findings

1. **R1 — Resolved.** `<commit>` requires the newest row to be MISMATCH (`<file:line>`).
2. **R2 — Not resolved.** <Why it still matters, or "accepted by the author: <reason>".>

### Build verification
...
### Blocking

None.
```

## Author reply format

Post one PR comment per round, after pushing the fixes.

```markdown
Fixed R1 and R3; R2 kept with a reason.

## Blocking

1. **R1** — <what changed> (`<commit>`)

## Suggestions

1. **R2** — Not changed: <reason>

## Notes

1. **R3** — <answer>

## Verification

- `./gradlew test --tests "*Handover*"`: passed
- Not run: <anything not run>
```

## What counts as blocking

| Section | Meaning |
|---|---|
| Blocking | Must fix before merge: a bug, a broken hard rule, a missing test, a contract or ERD mismatch, anything that breaks the demo |
| Suggestions | A better approach; the author decides |
| Notes | Questions (label them `question:`), nitpicks, and risky parts that checked out. Never blocks |

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
