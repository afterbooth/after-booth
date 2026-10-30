#!/usr/bin/env node
// PreToolUse hook. Guards three things:
// 1) blocks reading or editing .env (so keys never land in the transcript)
// 2) gates live Meta API calls (protects credits, AB_LIVE_GATE=ask|allow|deny)
// 3) scans for secrets before git commit
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

  // 1) .env through file tools
  for (const key of ['file_path', 'path', 'notebook_path']) {
    if (isEnvPath(ti[key])) {
      decide('deny', 'Only humans handle .env. See .env.example for the variable names.')
    }
  }

  if (tool !== 'Bash' && tool !== 'PowerShell') process.exit(0)
  const cmd = String(ti.command || '')

  // 2) shell commands that read or write .env (--env-file, read by the runtime itself, is handled by the live gate below)
  const withoutEnvFileFlag = cmd.replace(/--env-file[= ]\S+/g, '')
  if (/(^|[\s'"=/\\<>|;(])\.env(?![.\w-])/.test(withoutEnvFileFlag)) {
    decide('deny', 'Shell commands that touch .env are blocked. Apps and scripts read it with --env-file.')
  }

  // 3) live call gate
  const live =
    /--env-file[= ]\S*\.env\b/.test(cmd) ||
    /AI_MODE\s*=\s*['"]?(live|record)/i.test(cmd) ||
    /\$env:AI_MODE\s*=\s*['"](live|record)/i.test(cmd)
  if (live) {
    const gate = (process.env.AB_LIVE_GATE || 'ask').toLowerCase()
    const reason =
      'This live/record call spends Meta API credits. Check the running total in docs/BUDGET.md before approving.'
    if (gate === 'deny') decide('deny', reason)
    if (gate !== 'allow') decide('ask', reason)
  }

  // 4) secret scan before commit
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
