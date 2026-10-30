#!/usr/bin/env node
// PreToolUse hook. 세 가지를 지킨다.
// 1) .env 읽기·편집 차단 (키가 대화 기록에 남지 않게)
// 2) Meta API live 호출 게이트 (크레딧 보호, AB_LIVE_GATE=ask|allow)
// 3) git commit 전 시크릿 스캔
import { execFileSync } from 'node:child_process'
import { basename, join } from 'node:path'

const projectDir = process.env.CLAUDE_PROJECT_DIR || process.cwd()

function decide(permissionDecision, reason) {
  process.stdout.write(
    JSON.stringify({
      hookSpecificOutput: {
        hookEventName: 'PreToolUse',
        permissionDecision,
        permissionDecisionReason: reason,
      },
    }),
  )
  process.exit(0)
}

function isEnvPath(p) {
  if (!p) return false
  const name = basename(String(p).replace(/\\/g, '/'))
  return /^\.env(\..+)?$/.test(name) && name !== '.env.example'
}

let input = ''
process.stdin.on('data', (c) => (input += c))
process.stdin.on('end', () => {
  let data
  try {
    data = JSON.parse(input)
  } catch {
    process.exit(0)
  }
  const tool = data.tool_name || ''
  const ti = data.tool_input || {}

  // 1) 파일 도구로 .env 접근
  for (const key of ['file_path', 'path', 'notebook_path']) {
    if (isEnvPath(ti[key])) {
      decide('deny', '.env는 사람만 다룹니다. 필요한 변수 이름은 .env.example을 보세요.')
    }
  }

  if (tool !== 'Bash' && tool !== 'PowerShell') process.exit(0)
  const cmd = String(ti.command || '')

  // 2) 셸에서 .env 내용을 읽거나 쓰는 명령 (런타임이 직접 읽는 --env-file은 아래 live 게이트에서 다룬다)
  const withoutEnvFileFlag = cmd.replace(/--env-file[= ]\S+/g, '')
  if (/(^|[\s'"=/\\<>|;(])\.env(?![.\w-])/.test(withoutEnvFileFlag)) {
    decide('deny', '셸에서 .env를 직접 다루는 명령은 막혀 있습니다. 앱/스크립트는 --env-file로 읽으세요.')
  }

  // 3) live 호출 게이트
  const live =
    /--env-file[= ]\S*\.env\b/.test(cmd) ||
    /AI_MODE\s*=\s*['"]?(live|record)/i.test(cmd) ||
    /\$env:AI_MODE\s*=\s*['"](live|record)/i.test(cmd)
  if (live) {
    const gate = (process.env.AB_LIVE_GATE || 'ask').toLowerCase()
    const reason =
      'Meta API 크레딧을 쓰는 live/record 호출입니다. docs/BUDGET.md 누적액을 확인하고 승인하세요.'
    if (gate === 'deny') decide('deny', reason)
    if (gate !== 'allow') decide('ask', reason)
  }

  // 4) 커밋 전 시크릿 스캔
  if (/\bgit\b[^|;&]*\bcommit\b/.test(cmd)) {
    try {
      execFileSync('node', [join(projectDir, 'scripts', 'secret-scan.mjs'), '--staged'], {
        cwd: projectDir,
        encoding: 'utf8',
        stdio: ['ignore', 'pipe', 'pipe'],
      })
    } catch (e) {
      decide('deny', String(e.stderr || e.message).trim())
    }
  }
  process.exit(0)
})
