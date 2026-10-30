# Review guide

Every PR gets three reviews before merge. All review comments are in English.

| # | Reviewer | How | Blocks merge on |
|---|---|---|---|
| 1 | Claude `reviewer` agent (fresh session) | `/feature` step 5, or ask Claude to "run the reviewer agent on this PR" | any `[blocking]` |
| 2 | Codex | `/codex:review --base main`; add `/codex:adversarial-review --base main <focus>` for concurrency, state machine, AI validation and contract PRs | any issue marked high severity |
| 3 | Teammate | GitHub review, optional for speed (0 approvals required) | — |

The PR author replies to every blocking comment with the fix commit or a reason.

## Comment format

We use [Conventional Comments](https://conventionalcomments.org/) labels so the severity is clear:

```
<label> [blocking|non-blocking]: <subject>

<discussion: why, and a suggested fix>
```

| Label | Meaning |
|---|---|
| `issue (blocking)` | Must fix before merge: a bug, a broken rule, a missing test |
| `suggestion` | Better approach, author decides |
| `question` | Reviewer does not understand. Answer, and consider a clearer name or comment |
| `nitpick` | Tiny style point. Never blocks |
| `praise` | Something done well |

Example:

```
issue (blocking): hold can oversell under concurrency

`ItemService.hold` reads availableQty and then saves. Two requests can both pass the check.
Use the conditional UPDATE from docs/SPEC.md (`... WHERE available_qty >= :q`) and check the row count.
```

## Checklist

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

- [ ] Responses match `contract/openapi.yaml`
- [ ] The demo scenario in `docs/SPEC.md` still works

Safety

- [ ] No secrets, `.env` or real faces/personal data in fixtures
