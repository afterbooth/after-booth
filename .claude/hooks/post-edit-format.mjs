#!/usr/bin/env node
// PostToolUse hook. frontend 파일은 편집 직후 prettier로 포맷한다.
// Java는 Gradle 기동이 느려서 여기서 돌리지 않고, feature 스킬의 커밋 직전 단계에서 spotlessApply로 처리한다.
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
    // 포맷 실패는 작업을 막지 않는다. 빌드의 format:check가 잡는다
  }
  process.exit(0)
})
