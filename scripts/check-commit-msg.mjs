#!/usr/bin/env node
// commit-msg hook. Conventional Commits with an English subject (docs/CONVENTIONS.md).
import { readFileSync } from 'node:fs'

const file = process.argv[2]
const subject = readFileSync(file, 'utf8').split(/\r?\n/)[0].trim()

// let through the merge, revert and fixup messages git writes itself
if (/^(Merge |Revert "|fixup! |squash! )/.test(subject)) process.exit(0)

const TYPES = 'feat|fix|refactor|perf|test|docs|build|ci|chore|revert'
const pattern = new RegExp(`^(${TYPES})(\\([a-z0-9-]+\\))?!?: \\S.*$`)
const HANGUL = /[ㄱ-ㆎ가-힣]/
const problems = []

if (!pattern.test(subject)) {
  problems.push('Format: <type>(<scope>): <summary>   e.g. feat(allocation): hold quantity with conditional update')
}
if (HANGUL.test(subject)) {
  problems.push('Write the subject in English')
}
if (subject.length > 72) {
  problems.push(`Subject is ${subject.length} characters; keep it to 72 or fewer`)
}
if (/\.$/.test(subject)) {
  problems.push('Do not end the subject with a period')
}

if (problems.length > 0) {
  console.error(`[commit-msg] "${subject}"`)
  for (const p of problems) console.error(`  - ${p}`)
  console.error('  docs/CONVENTIONS.md#commit-messages')
  process.exit(1)
}
