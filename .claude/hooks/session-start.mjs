#!/usr/bin/env node
// SessionStart hook. Points git at the repo hooks and re-anchors progress after the conversation is summarized.
import { execFileSync } from 'node:child_process'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'

const projectDir = process.env.CLAUDE_PROJECT_DIR || process.cwd()

try {
  const current = execFileSync('git', ['config', '--get', 'core.hooksPath'], {
    cwd: projectDir,
    encoding: 'utf8',
  }).trim()
  if (current !== '.githooks') throw new Error('reset')
} catch {
  try {
    execFileSync('git', ['config', 'core.hooksPath', '.githooks'], { cwd: projectDir })
  } catch {
    // not a git repository
  }
}

const tasks = join(projectDir, 'docs', 'TASKS.md')
if (existsSync(tasks)) {
  const text = readFileSync(tasks, 'utf8')
  const done = (text.match(/- \[x\]/gi) || []).length
  const open = (text.match(/- \[ \]/g) || []).length
  // count BLOCKED only on checklist items, not in the explanation at the top
  const blocked = (text.match(/^\s*- \[[ x]\].*BLOCKED/gim) || []).length
  console.log(
    `[after-booth] TASKS.md: done ${done} / open ${open} / BLOCKED ${blocked}. ` +
      'Read AGENTS.md, docs/TASKS.md, docs/DECISIONS.md and docs/AGENT_MISTAKES.md before continuing.',
  )
}
