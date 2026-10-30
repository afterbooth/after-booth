---
name: feature
description: Takes one AfterBooth GitHub issue from implementation → verification → review (Claude reviewer + Codex) → PR. The main session orchestrates and delegates to subagents; it does not write production code itself.
---

# Feature

Finish one GitHub issue as a PR. Argument: issue number (e.g. `/feature 12`).

## Before you start

1. Read the goal, scope and acceptance criteria with `gh issue view <number>`. Check that the assignee is you (the current gh account).
2. Read only the docs in the `CLAUDE.md` doc routing row for this kind of task. Always read `docs/AGENT_MISTAKES.md`.
3. Check existing changes with `git status --short --branch`. Never mix in or revert someone else's changes.
4. Create a worktree and branch: `git worktree add .claude/worktrees/<number>-<slug> -b <type>/<number>-<slug> origin/main`
5. If the contract must change, make a first commit that touches only `contract/` and record it in `docs/DECISIONS.md`.

## Build loop (per acceptance criterion)

1. **Implement**: give one item to the one agent that owns those files (CLAUDE.md table). Pass: issue number, item, acceptance criteria, relevant contract and template paths, the last failure log (on retry).
2. **Check**: the main session confirms the actual diff matches the item and the files reported. Send back changes outside file ownership, code outside the spec and unused code.
3. **Verify**: the main session runs these itself.
   - backend: `.\gradlew.bat spotlessApply compileJava compileTestJava` + `test --tests "com.afterbooth.ArchitectureTest"` + this item's tests (Testcontainers from the PowerShell tool)
   - frontend: `npm run build && npm test && npm run lint`
4. **On failure**: return it to the same agent with the failure log. After 3 failures or 30 minutes, hand it to `/codex:rescue` once. If still stuck, leave `BLOCKED — <reason>` in `docs/TASKS.md` and as an issue comment, and move to the next item.
5. **Commit**: one commit per item, English Conventional Commit, `Refs #<number>` at the end of the body. Example: `feat(allocation): hold quantity with conditional update`

## Review and PR

1. Start a **fresh** `reviewer` in review mode on `git diff origin/main...HEAD`. Any `blocking` goes back to the build loop (at most 2 rounds).
2. Codex review: `node "<codex plugin>/scripts/codex-companion.mjs" review --wait --base origin/main`. For concurrency, state machine, AI validation or contract changes, also run `adversarial-review --wait --base origin/main "<risk to focus on>"`. Fix high-severity findings. The plugin path is `~/.claude/plugins/cache/openai-codex/codex/<version>/`.
3. Tick the item in `docs/TASKS.md` and commit it with the change.
4. `git push -u origin <branch>` → `gh pr create --fill-first`, then fill the body in the `.github/PULL_REQUEST_TEMPLATE.md` format (`Closes #<number>`, verification commands actually run, review results).
5. A human merges. After the merge, clean up: `git worktree remove .claude/worktrees/<number>-<slug>`

## Work that costs credits

The hook asks a human before any live/record call. Before it asks, state in one line the purpose, expected number of calls and expected cost. After the call, update the `docs/BUDGET.md` ledger.

## Final report

- PR link, commit list
- Verification commands run and their results / not verified
- reviewer `RESULT` line, Codex review summary, remaining suggestions
- Any BLOCKED item and why
