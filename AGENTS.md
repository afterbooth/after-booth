# AGENTS.md — AfterBooth

Entry point for agents that read AGENTS.md, such as Codex and Muse Code. **`CLAUDE.md` is the rulebook of record**; this file only lists the differences.

## Read first

1. `CLAUDE.md` — hard rules, STOP gates, file ownership, commands, and the **doc routing table**: read only the row for your task
2. `docs/TASKS.md` — where the work stands, and the GitHub issue you are working on (`gh issue view <n>`)
3. `docs/AGENT_MISTAKES.md` — pitfalls reproduced on this PC
4. Sources of truth, in this order: `docs/SPEC.md` (product) → `contract/openapi.yaml` (API) → `docs/ERD.md` (tables). Never implement something that contradicts them; propose a change instead
5. `docs/CONVENTIONS.md`, `docs/templates/` — code and commit rules and templates
6. Issues, PRs and reviews: `docs/CONVENTIONS.md` §Writing issues, PRs and reviews. Conclusion first; title, summary and linked issue say the same thing. Reviews use `docs/REVIEW_GUIDE.md` §Review format (verdict first, finding IDs, at most 25 lines) and §Re-review format

## Tool differences

- Role definitions of record are `.claude/agents/*.md`. Codex roles (`.codex/agents/*.toml`) only say to read and follow those files.
- Feature work follows `.claude/skills/feature/SKILL.md` as written.
- Do not open `.env`. The rule holds even in tools without the Claude hooks. Commits are checked by the git pre-commit hook (`scripts/secret-scan.mjs`).
- Ask a human before any live/record Meta API call, and log the cost in `docs/BUDGET.md` afterwards.
- Humans run `git push` to main and merges.

## Working with Muse Code

So it counts toward the "Use of Meta AI Tooling" criterion, log every task done with Muse Code in `docs/DECISIONS.md` with a `[Muse Code]` tag and keep a screenshot.

To review a PR with Muse Code, post the result with `gh pr review <n> --approve|--request-changes --body-file <file>`, written in the `docs/REVIEW_GUIDE.md` §Review format. Do not edit files during a review.
