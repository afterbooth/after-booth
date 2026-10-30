# TASKS

진행률이 그대로 보이게 각 Phase가 끝날 때마다 갱신한다. 막힌 항목은 `BLOCKED — 이유`를 붙인다.
작업 단위는 GitHub 이슈다. 담당: **DY** = namdongyeob, **WJ** = WookJaes. 둘 다 백엔드와 프론트를 같이 한다.

## Phase 0: 부트스트랩 + 스파이크 (기능 구현 금지)

- [x] 0-1 레포 구조: backend/, frontend/, contract/, docs/, fixtures/, spike/, docker-compose, .gitignore
- [x] 0-2 docs/SPEC.md, TASKS.md, DECISIONS.md (+ BUDGET.md, AGENT_MISTAKES.md)
- [x] 0-3 하네스: CLAUDE.md, AGENTS.md, .claude/agents 5개, feature 스킬, hooks, .codex
- [x] 0-3b 컨벤션·리뷰 가이드·코드 템플릿·PR/이슈 템플릿·commit-msg hook·ArchitectureTest
- [ ] #1 contract 초안 (DY)
- [ ] #2 스파이크: 모델 목록, Spark 텍스트·이미지·function calling, SAM 3.1, Transcribe, 비용 → spike/REPORT.md (DY, 키 A)
- [ ] **G0 STOP** — REPORT 요약과 사람이 정할 것 보고

## Phase 1: 기능 (목표 11/1 밤 KST)

| 이슈 | 내용 | 담당 | 범위 |
|---|---|---|---|
| #3 | 도메인 모델, Flyway, 시드 | WJ | BE |
| #4 | AiGateway live/record/replay | DY | AI |
| #5 | 사진 등록 (SAM → Spark → 재고 초안 → 확정) + 촬영·등록 화면 | DY | BE+AI+FE |
| #6 | 음성 수령 요청 (Transcribe → Spark 구조화) + 수령 요청 화면 | DY | BE+AI+FE |
| #7 | 예약 코어: 조건부 HOLD, 멱등키, 상태머신, 만료 스케줄러 + 필수 테스트 | WJ | BE |
| #8 | 차량 적합성 + 사유 코드 | WJ | BE |
| #9 | 배정 에이전트 (function calling, 서버 검증, 대안) + 배정 보드 화면 | WJ | BE+AI+FE |
| #10 | 인계 재카운트 + 재사용 통계 + 화면 | WJ | BE+AI+FE |

- [ ] #3 · [ ] #4 · [ ] #5 · [ ] #6 · [ ] #7 · [ ] #8 · [ ] #9 · [ ] #10

의존 순서: #1 → (#3, #4 동시) → #5·#6 (DY) / #7 → #8 → #9 → #10 (WJ)

## Phase 2: 통합 + 데모 안정화 (목표 11/2~11/3)

- [ ] #11 통합, `docker compose up` 한 번으로 실행 (DY)
- [ ] #12 Playwright E2E 데모 시나리오, 컨텍스트 2개 동시 신청 (WJ)
- [ ] #13 record → replay 폴백, 지연 측정, 로딩 UX (DY)
- [ ] #14 심사 기준 셀프 채점 + Muse Code 리뷰 → 약점 보강 (DY, WJ)

## Phase 3: 제출 자료 (목표 11/4 오전)

- [ ] #15 영어 README (DY)
- [ ] #16 데모 대본 + 2~3분 영상 (WJ, 키 B)
- [ ] #17 제출 체크리스트 (DY, WJ)
- [ ] **G3 STOP** — 실행 방법, 데모 결과, 셀프 채점, 남은 리스크 보고
