#!/usr/bin/env node
// Checks that a PR description follows .github/PULL_REQUEST_TEMPLATE.md (docs/CONVENTIONS.md).
// CI: PR_TITLE and PR_BODY env vars. Local: node scripts/check-pr-body.mjs --title "<title>" --body-file <file>
import { readFileSync } from 'node:fs'

const arg = (name) => {
  const i = process.argv.indexOf(name)
  return i > -1 ? process.argv[i + 1] : undefined
}
const title = arg('--title') ?? process.env.PR_TITLE ?? ''
const bodyFile = arg('--body-file')
const body = (bodyFile ? readFileSync(bodyFile, 'utf8') : (process.env.PR_BODY ?? '')).replace(/\r\n/g, '\n')

const TYPES = ['feat', 'fix', 'refactor', 'perf', 'test', 'docs', 'build', 'ci', 'chore', 'revert']
const AREAS = ['backend', 'frontend', 'ai', 'contract', 'infra', 'docs']
const stripComments = (s) => s.replace(/<!--[\s\S]*?-->/g, '').trim()

function section(heading) {
  const m = body.match(new RegExp(`^## ${heading}\\s*\\n([\\s\\S]*?)(?=^## |(?![\\s\\S]))`, 'm'))
  return m ? stripComments(m[1]) : null
}

function field(text, name) {
  const m = text?.match(new RegExp(`^- ${name}:(.*)$`, 'm'))
  return m ? stripComments(m[1]) : ''
}

const problems = []

if (!/\b(Closes|Fixes|Refs) #\d+/.test(body)) problems.push('Link the issue: "Closes #<n>" in the Trace section')

const summary = section('In one sentence')
if (summary === null) problems.push('Missing "## In one sentence" section')
else if (!summary) problems.push('"In one sentence" is empty')
else if (summary.length > 400) problems.push(`"In one sentence" is ${summary.length} characters; keep it to two lines`)

const trace = section('Trace')
if (trace === null) problems.push('Missing "## Trace" section')
else {
  const type = field(trace, 'Type')
  const titleType = title.match(/^(\w+)(\(|!|:)/)?.[1]
  if (!TYPES.includes(type)) problems.push(`Trace "Type:" must be one of ${TYPES.join(', ')}`)
  else if (titleType && type !== titleType) problems.push(`Title type "${titleType}" and Trace "Type: ${type}" disagree`)

  const areas = field(trace, 'Area').split(',').map((a) => a.trim()).filter(Boolean)
  if (areas.length === 0 || areas.some((a) => !AREAS.includes(a))) {
    problems.push(`Trace "Area:" must list ${AREAS.join(', ')}`)
  }
  if (!/^(low|medium|high)\b/.test(field(trace, 'Risk'))) problems.push('Trace "Risk:" must start with low, medium or high')
}

if (section('Acceptance criteria') === null) problems.push('Missing "## Acceptance criteria" section')
if (section('Verification') === null) problems.push('Missing "## Verification" section')

if (problems.length > 0) {
  console.error('[pr-body] The PR description does not follow .github/PULL_REQUEST_TEMPLATE.md:')
  for (const p of problems) console.error(`  - ${p}`)
  process.exit(1)
}
console.log('[pr-body] OK')
