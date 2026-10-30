# DECISIONS

판단 한 건에 한 줄. 무엇을 정했는지, 왜인지. 계약 변경은 `[contract]`, Muse Code로 한 작업은 `[Muse Code]` 태그를 붙인다.

| ID | 날짜 | 결정 | 이유 |
|---|---|---|---|
| D-001 | 2026-10-30 | Spring Boot **4.1.2** + Spring AI **2.0.1** (스펙의 "3.x"에서 변경) | start.spring.io가 3.x를 더 이상 제공하지 않고, Spring AI OpenAI 스타터는 Boot 4.0 이상만 지원. 팀이 finplay에서 Boot 4.1을 써 봤음 |
| D-002 | 2026-10-30 | PostgreSQL 17, 로컬 포트 15432 | 기본 5432 충돌 방지 |
| D-003 | 2026-10-30 | 브랜치는 main + feat/*, 스트림은 worktree(`.claude/worktrees/`) | 6일짜리라 dev 단계 생략 |
| D-004 | 2026-10-30 | 프론트 API 클라이언트·MSW 목은 orval로 contract에서 생성, 생성물은 커밋 안 함 | 계약과 프론트가 어긋나지 않게. build/test/dev 전에 자동 생성 |
| D-005 | 2026-10-30 | Java 포맷은 편집 hook이 아니라 커밋 전 `spotlessApply`(feature 스킬 3단계) | Gradle 기동이 편집마다 돌기엔 느림. 프론트는 편집 직후 prettier |
| D-006 | 2026-10-30 | 역할 에이전트는 영역 단위 5개(스펙)를 쓰고, finplay의 implementer/tester 분리는 하지 않음. 대신 reviewer를 매번 새로 띄우고 메인이 검증을 직접 돌림 | 병렬 스트림의 파일 소유권 분리가 더 중요. 테스트는 구현 에이전트가 같이 씀 |
| D-007 | 2026-10-30 | live/record 호출은 PreToolUse hook이 사람에게 묻는다 (`AB_LIVE_GATE=ask`). 누적 $50마다 STOP | 풀자동에서 크레딧이 새는 걸 막는 유일한 사람 게이트 |
| D-008 | 2026-10-30 | Meta 키 2개를 역할로 나눔: A(개발·스파이크·record), B(데모 리허설·녹화·라이브 결선 예비) | docs/BUDGET.md |
| D-009 | 2026-10-30 | fine-tuning(학습)은 계획에 넣지 않음. 필요하면 G0에서 스파이크 결과로 다시 판단 | 6일 일정, 정확도 개선은 프롬프트·스키마·SAM 조합으로 충분할 가능성이 큼 |
