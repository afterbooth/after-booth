## Summary

<!-- What changes and why, in 1-3 sentences. -->

Closes #

## Changes

-

## Meta AI usage

<!-- Which Meta model is involved (Muse Spark / SAM 3.1 / Voice Transcribe) and what action its output leads to. Write "None" if not applicable. -->

## Verification

<!-- Commands you actually ran and their results. Mark anything not run as "Not verified". -->

```
cd backend && ./gradlew spotlessCheck compileJava test --tests "..."
cd frontend && npm run build && npm test && npm run lint
```

- [ ] Tests pass locally
- [ ] Ran in `AI_MODE=replay` (no live calls), or listed live calls and cost in `docs/BUDGET.md`

## Review

- [ ] Claude `reviewer` agent: `RESULT: blocking 0`
- [ ] Codex review (`/codex:review --base main`): no high-severity findings
- [ ] Contract changes recorded in `docs/DECISIONS.md` (or none)

## Screenshots / demo

<!-- For UI changes: before/after screenshots or a short clip. -->
