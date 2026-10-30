#!/usr/bin/env node
// 커밋 전 시크릿 스캔. git pre-commit hook(.githooks/pre-commit)과 Claude PreToolUse hook이 함께 쓴다.
// 사용: node scripts/secret-scan.mjs [--staged | 파일...]
// 오탐이면 해당 줄에 `secret-scan:allow` 주석을 붙인다.
import { execFileSync } from 'node:child_process'
import { readFileSync } from 'node:fs'
import { basename, extname } from 'node:path'

const BINARY_EXT = new Set([
  '.png', '.jpg', '.jpeg', '.webp', '.gif', '.ico', '.jar', '.zip',
  '.wav', '.mp3', '.m4a', '.webm', '.mp4', '.pdf', '.woff', '.woff2',
])

const RULES = [
  { name: 'private key', re: /-----BEGIN (RSA |EC |OPENSSH |)PRIVATE KEY-----/ },
  { name: 'GitHub token', re: /\bgh[pousr]_[A-Za-z0-9]{30,}\b/ },
  { name: 'AWS access key', re: /\bAKIA[0-9A-Z]{16}\b/ },
  { name: 'OpenAI-style key', re: /\bsk-[A-Za-z0-9_-]{20,}\b/ },
  { name: 'Bearer token', re: /Bearer\s+[A-Za-z0-9._-]{30,}/ },
  {
    name: 'key/secret assignment',
    re: /(api[_-]?key|secret|token|password|passwd)["']?\s*[:=]\s*["']?[A-Za-z0-9_\-./+]{24,}/i,
  },
]

function git(args) {
  return execFileSync('git', args, { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 })
}

function isEnvFile(path) {
  const name = basename(path)
  return /^\.env(\..+)?$/.test(name) && name !== '.env.example'
}

function targets() {
  const files = process.argv.slice(2).filter((a) => !a.startsWith('--'))
  if (files.length > 0) {
    return files.map((f) => ({ path: f, read: () => readFileSync(f, 'utf8') }))
  }
  const staged = git(['diff', '--cached', '--name-only', '--diff-filter=ACMR'])
    .split('\n')
    .map((s) => s.trim())
    .filter(Boolean)
  return staged.map((f) => ({ path: f, read: () => git(['show', `:${f}`]) }))
}

const findings = []
for (const t of targets()) {
  if (isEnvFile(t.path)) {
    findings.push(`${t.path}: .env 파일은 커밋할 수 없습니다`)
    continue
  }
  if (BINARY_EXT.has(extname(t.path).toLowerCase())) continue
  if (/(^|\/)package-lock\.json$/.test(t.path)) continue
  let text
  try {
    text = t.read()
  } catch {
    continue
  }
  text.split(/\r?\n/).forEach((line, i) => {
    if (line.includes('secret-scan:allow')) return
    for (const rule of RULES) {
      if (rule.re.test(line)) findings.push(`${t.path}:${i + 1}: ${rule.name}`)
    }
  })
}

if (findings.length > 0) {
  console.error('[secret-scan] 시크릿으로 보이는 내용이 있어 커밋을 막았습니다:')
  for (const f of findings) console.error(`  - ${f}`)
  console.error('오탐이면 해당 줄에 `secret-scan:allow` 주석을 붙이세요.')
  process.exit(1)
}
