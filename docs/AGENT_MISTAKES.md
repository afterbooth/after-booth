# AGENT_MISTAKES

Only agent mistakes that were actually reproduced and confirmed. No speculative prevention rules. Read this before implementing.

## Confirmed on finplay, on this same PC (2026-07 to 08)

1. **Never switch branches in a shared folder.** Files another session was editing disappeared. Work on other branches only in worktrees.
2. **Check for other running builds before starting Gradle.** Overlapping builds in one folder break each other. `:test FAILED` with zero failed tests means a build collision. Do not just re-run: `gradlew --stop` → confirm the java processes are gone → run alone.
3. **Run Testcontainers from the PowerShell tool.** From the Git Bash tool every test failed because the Docker socket could not be found.
4. **Do not habitually run the full test suite.** It has died from running out of memory. Normally run compile + spotlessCheck + affected tests with `--tests`.

## Confirmed on AfterBooth

| Date | Mistake | Fix |
|---|---|---|
| 2026-10-30 | A heredoc in the Bash tool collapsed `\\` into `\`, breaking a JS regex | Write files containing backslashes with the Write tool |
| 2026-10-30 | A commit subject was 74 characters and the commit-msg hook rejected it | Keep subjects ≤ 72 characters before committing |
| 2026-10-30 | A command that only mentioned `.env` (to check whether it exists) was blocked by the guard hook | Do not reference `.env` in shell commands at all; ask the human |
| 2026-10-31 | With both `CLAUDE.md` and `AGENTS.md` at the root, Muse Code ignored `CLAUDE.md` and never loaded the hard rules | Keep one rulebook, `AGENTS.md`. Never add `CLAUDE.md` or `.claude/CLAUDE.md` (CI `hygiene` fails) |
