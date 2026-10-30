#!/usr/bin/env node
// commit-msg hook. Conventional Commits, English subject (docs/CONVENTIONS.md).
import { readFileSync } from 'node:fs'

const file = process.argv[2]
const subject = readFileSync(file, 'utf8').split(/\r?\n/)[0].trim()

// git이 만드는 머지·리버트·fixup 메시지는 통과시킨다
if (/^(Merge |Revert "|fixup! |squash! )/.test(subject)) process.exit(0)

const TYPES = 'feat|fix|refactor|perf|test|docs|build|ci|chore|revert'
const pattern = new RegExp(`^(${TYPES})(\\([a-z0-9-]+\\))?!?: \\S.*$`)
const problems = []

if (!pattern.test(subject)) {
  problems.push('형식: <type>(<scope>): <summary>   예) feat(allocation): hold quantity with conditional update')
}
if (/[ㄱ-ㆎ가-힣]/.test(subject)) {
  problems.push('제목은 영어로 씁니다 (본문은 자유)')
}
if (subject.length > 72) {
  problems.push(`제목이 ${subject.length}자입니다. 72자 이하로 줄여 주세요`)
}
if (/\.$/.test(subject)) {
  problems.push('제목 끝에 마침표를 찍지 않습니다')
}

if (problems.length > 0) {
  console.error(`[commit-msg] "${subject}"`)
  for (const p of problems) console.error(`  - ${p}`)
  console.error('  docs/CONVENTIONS.md#commit-messages')
  process.exit(1)
}
