---
name: feature
description: AfterBooth 기능 하나(TASKS.md의 항목)를 구현 → 검증 → 리뷰 → 커밋까지 진행한다. 메인 세션은 오케스트레이터로서 서브에이전트에 맡기고 직접 production 코드를 쓰지 않는다.
---

# Feature

`docs/TASKS.md`의 기능 항목 하나를 품질 게이트 순서대로 끝낸다. 인자: 항목 이름 또는 번호.

## 사전 확인

1. `CLAUDE.md`, `docs/TASKS.md`, `docs/AGENT_MISTAKES.md`를 읽는다.
2. `git status --short --branch`로 브랜치와 기존 변경을 확인한다. 남의 변경을 섞거나 되돌리지 않는다.
3. main에 있다면 worktree를 만든다: `git worktree add .claude/worktrees/<이름> -b feat/<이름>`. 이미 스트림 worktree 안이면 그대로 쓴다.
4. 항목이 계약 변경을 요구하면 먼저 `contract/`만 담은 커밋을 만들고 `docs/DECISIONS.md`에 적는다.

## 루프

1. **구현**: 파일 소유권(CLAUDE.md 표)에 맞는 에이전트 한 명에게 항목 하나를 맡긴다. 전달: 항목 설명, 완료 조건, 관련 계약 경로, 직전 실패 로그(재시도면).
2. **확인**: 메인이 실제 diff가 항목 범위·보고한 파일과 일치하는지 본다. 소유권 밖 파일을 건드렸으면 되돌리게 한다.
3. **검증**: 메인이 직접 돌린다.
   - backend: `.\gradlew.bat spotlessApply compileJava compileTestJava` + 이번 항목 테스트 `--tests` (Testcontainers는 PowerShell 도구)
   - frontend: `npm run build && npm test && npm run lint`
4. **실패 처리**: 같은 에이전트에게 실패 로그와 함께 되돌린다. 같은 항목에서 3회 실패하거나 30분을 넘기면 `codex:rescue`에 한 번 넘기고, 그래도 안 되면 `docs/TASKS.md`에 `BLOCKED — 이유`를 적고 다음 항목으로 간다.
5. **리뷰**: `reviewer`를 **새로** 띄워 리뷰 모드로 이번 항목 diff를 보게 한다. `[차단]`이 있으면 구현 에이전트에게 되돌리고 3번부터 다시. 리뷰 라운드는 최대 2회, 그 뒤 남은 차단은 BLOCKED로 기록.
6. **커밋**: `docs/TASKS.md` 체크 → 항목 단위 시맨틱 커밋 (`feat(allocation): 조건부 UPDATE로 HOLD 수량 차감`). pre-commit hook이 시크릿을 검사한다.

## 크레딧이 드는 작업

live/record 호출이 필요하면 hook이 사람에게 묻는다. 묻기 전에 한 줄로 목적·예상 호출 수·예상 비용을 적는다. 호출 후 `docs/BUDGET.md` 원장을 갱신한다.

## 마무리 보고

- 완료 항목, 커밋 SHA
- 실행한 검증 명령과 결과 / 미검증
- 리뷰 결과(RESULT 줄), 남은 권장·참고
- BLOCKED가 생겼으면 그 이유
