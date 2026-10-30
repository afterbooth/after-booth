#!/usr/bin/env node
// PostToolUse hook. Formats frontend files with prettier right after each edit.
// Java is not formatted here because starting Gradle is slow; the feature skill runs spotlessApply before each commit.
import { execFileSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import { join, relative } from 'node:path'

const projectDir = process.env.CLAUDE_PROJECT_DIR || process.cwd()
let input = ''
process.stdin.on('data', (c) => (input += c))
process.stdin.on('end', () => {
  let file
  try {
    file = JSON.parse(input).tool_input?.file_path
  } catch {
    process.exit(0)
  }
  if (!file) process.exit(0)
  const rel = relative(projectDir, file).replace(/\\/g, '/')
  if (!/^(frontend|e2e)\/.+\.(ts|tsx|css|json|md)$/.test(rel)) process.exit(0)
  if (rel.includes('/generated/') || rel.includes('node_modules/')) process.exit(0)

  const pkg = rel.startsWith('e2e/') ? 'e2e' : 'frontend'
  const bin = join(projectDir, pkg, 'node_modules', 'prettier', 'bin', 'prettier.cjs')
  if (!existsSync(bin)) process.exit(0)
  try {
    execFileSync('node', [bin, '--write', file], { cwd: join(projectDir, pkg), stdio: 'ignore' })
  } catch {
    // a formatting failure never blocks work; format:check in the build catches it
  }
  process.exit(0)
})
