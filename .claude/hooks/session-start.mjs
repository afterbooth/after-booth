#!/usr/bin/env node
// SessionStart hook. git pre-commit hook 경로를 맞추고, 대화가 요약된 뒤에도 진행 상황을 다시 잡게 한다.
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
    // git 저장소가 아니면 건너뛴다
  }
}

const tasks = join(projectDir, 'docs', 'TASKS.md')
if (existsSync(tasks)) {
  const text = readFileSync(tasks, 'utf8')
  const done = (text.match(/- \[x\]/gi) || []).length
  const open = (text.match(/- \[ \]/g) || []).length
  // 안내 문구의 단어는 빼고, 체크리스트 항목에 붙은 BLOCKED만 센다
  const blocked = (text.match(/^\s*- \[[ x]\].*BLOCKED/gim) || []).length
  console.log(
    `[after-booth] TASKS.md 진행: 완료 ${done} / 남음 ${open} / BLOCKED ${blocked}. ` +
      '작업 전에 CLAUDE.md, docs/TASKS.md, docs/DECISIONS.md, docs/AGENT_MISTAKES.md를 읽고 이어서 진행한다.',
  )
}
