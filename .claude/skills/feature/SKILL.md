---
name: feature
description: AfterBooth GitHub 이슈 하나를 구현 → 검증 → 리뷰(Claude reviewer + Codex) → PR까지 진행한다. 메인 세션은 오케스트레이터로서 서브에이전트에 맡기고 직접 production 코드를 쓰지 않는다.
---

# Feature

GitHub 이슈 하나를 PR로 끝낸다. 인자: 이슈 번호 (예: `/feature 12`).

## 사전 확인

1. `gh issue view <번호>`로 목표·범위·완료 조건을 읽는다. 담당자가 나(현재 gh 계정)인지 확인한다.
2. `CLAUDE.md`의 문서 라우팅 표에서 이 작업 유형에 해당하는 문서만 읽는다. `docs/AGENT_MISTAKES.md`는 항상 읽는다.
3. `git status --short --branch`로 기존 변경을 확인한다. 남의 변경을 섞거나 되돌리지 않는다.
4. worktree와 브랜치를 만든다: `git worktree add .claude/worktrees/<번호>-<slug> -b <type>/<번호>-<slug> origin/main`
5. 계약 변경이 필요하면 `contract/`만 담은 첫 커밋을 만들고 `docs/DECISIONS.md`에 적는다.

## 구현 루프 (완료 조건 항목마다)

1. **구현**: 파일 소유권(CLAUDE.md 표)에 맞는 에이전트 한 명에게 항목 하나를 맡긴다. 전달: 이슈 번호, 항목, 완료 조건, 관련 계약·템플릿 경로, 직전 실패 로그(재시도면).
2. **확인**: 메인이 실제 diff가 항목 범위·보고한 파일과 일치하는지 본다. 소유권 밖 파일, 스펙에 없는 코드, 쓰지 않는 코드는 되돌리게 한다.
3. **검증**: 메인이 직접 돌린다.
   - backend: `.\gradlew.bat spotlessApply compileJava compileTestJava` + `test --tests "com.afterbooth.ArchitectureTest"` + 이번 항목 테스트 (Testcontainers는 PowerShell 도구)
   - frontend: `npm run build && npm test && npm run lint`
4. **실패 처리**: 같은 에이전트에게 실패 로그와 함께 되돌린다. 3회 실패하거나 30분을 넘기면 `/codex:rescue`에 한 번 넘기고, 그래도 안 되면 `docs/TASKS.md`와 이슈 코멘트에 `BLOCKED — 이유`를 남기고 다음 항목으로 간다.
5. **커밋**: 항목 단위, 영어 Conventional Commit, 본문 끝에 `Refs #<번호>`. 예: `feat(allocation): hold quantity with conditional update`

## 리뷰와 PR

1. `reviewer`를 **새로** 띄워 리뷰 모드로 `git diff origin/main...HEAD`를 보게 한다. `blocking`이 있으면 구현 루프로 되돌린다 (최대 2라운드).
2. Codex 리뷰: `node "<codex 플러그인>/scripts/codex-companion.mjs" review --wait --base origin/main`. 동시성·상태머신·AI 검증·계약 변경이면 `adversarial-review --wait --base origin/main "<집중할 위험>"`도 돌린다. 높은 심각도 지적은 고친다. 플러그인 경로는 `~/.claude/plugins/cache/openai-codex/codex/<버전>/`.
3. `docs/TASKS.md`에서 해당 항목을 체크하고 같이 커밋한다.
4. `git push -u origin <브랜치>` → `gh pr create --fill-first` 후 `.github/PULL_REQUEST_TEMPLATE.md` 형식으로 본문을 채운다 (`Closes #<번호>`, 실제 실행한 검증 명령, 리뷰 결과).
5. 머지는 사람이 한다. 머지되면 worktree를 정리한다: `git worktree remove .claude/worktrees/<번호>-<slug>`

## 크레딧이 드는 작업

live/record 호출이 필요하면 hook이 사람에게 묻는다. 묻기 전에 한 줄로 목적·예상 호출 수·예상 비용을 적는다. 호출 후 `docs/BUDGET.md` 원장을 갱신한다.

## 마무리 보고

- PR 링크, 커밋 목록
- 실행한 검증 명령과 결과 / 미검증
- reviewer `RESULT` 줄, Codex 리뷰 요약, 남은 suggestion
- BLOCKED가 생겼으면 그 이유
