# TASKS

진행률이 그대로 보이게 각 Phase가 끝날 때마다 갱신한다. 막힌 항목은 `BLOCKED — 이유`를 붙인다.

## Phase 0: 부트스트랩 + 스파이크 (기능 구현 금지)

- [x] 0-1 레포 구조: backend/, frontend/, contract/, docs/, fixtures/, spike/, docker-compose, .gitignore
- [x] 0-2 docs/SPEC.md, TASKS.md, DECISIONS.md (+ BUDGET.md, AGENT_MISTAKES.md)
- [x] 0-3 하네스: CLAUDE.md, AGENTS.md, .claude/agents 5개, feature 스킬, hooks(.env 차단·live 게이트·시크릿 스캔·포맷터), .codex
- [ ] 0-4 contract/openapi.yaml 초안: 재고, 등록 초안, 수령 요청, 배정(HOLD/확정/취소), 인계, 통계 (지금은 health만 있음)
- [ ] 0-5a GET /v1/models 로 실제 모델 ID 기록
- [ ] 0-5b Spring AI로 Spark 텍스트 / 이미지 입력 / function calling 확인 (안 되면 RestClient)
- [ ] 0-5c SAM 3.1 API 호출 가능 여부, spike/samples/ 사진으로 개수 정확도·지연 측정
- [ ] 0-5d Voice Transcribe 한국어 음성 품질
- [ ] 0-5e 호출당 비용 추정 → docs/BUDGET.md
- [ ] 0-6 spike/REPORT.md 작성
- [ ] **G0 STOP** — REPORT 요약과 사람이 정할 것 보고

## Phase 1: 병렬 구현 (목표 11/1 밤 KST)

### 스트림 A — feat/domain (backend-domain)
- [ ] A-1 도메인 엔티티 + Flyway 마이그레이션 + 시드(단체, 품목 3종)
- [ ] A-2 조건부 UPDATE 수량 차감 + 멱등키
- [ ] A-3 예약 상태머신 + 전이 이력
- [ ] A-4 HOLD 만료·철거 마감 스케줄러 + 원복
- [ ] A-5 차량 적합성 (상수 + 사유 코드)
- [ ] A-6 에이전트 툴 3개 + 배정 에이전트 엔드포인트 (AiGateway 경유, 서버 검증)
- [ ] A-7 재사용 통계
- [ ] A-8 필수 테스트 5종 (Testcontainers)

### 스트림 B — feat/intake-front (ai-pipeline + frontend)
- [ ] B-1 AiGateway + live/record/replay
- [ ] B-2 사진 등록 파이프라인 (단계 상태 저장, 스키마 검증 1회 재요청, 확인 필요 표시)
- [ ] B-3 음성 조건 파이프라인 (원문 + 해석)
- [ ] B-4 인계 사진 재카운트
- [ ] B-5 화면: 촬영·등록
- [ ] B-6 화면: 수령 요청
- [ ] B-7 화면: 배정 보드

## Phase 2: 통합 + 데모 안정화 (목표 11/2~11/3)

- [ ] 2-1 두 브랜치 main 머지
- [ ] 2-2 Playwright E2E 데모 시나리오 (컨텍스트 2개 동시 신청 포함)
- [ ] 2-3 live 통과 → record → replay 통과
- [ ] 2-4 단계별 지연 측정, 로딩 UX, 실패 시 replay 자동 전환, `docker compose up` 한 번으로 실행
- [ ] 2-5 reviewer 셀프 채점 → 가장 약한 항목 보강

## Phase 3: 제출 자료 (목표 11/4 오전)

- [ ] 3-1 README(영어)
- [ ] 3-2 docs/DEMO_SCRIPT.md
- [ ] 3-3 제출 체크리스트
- [ ] **G3 STOP** — 실행 방법, 데모 결과, 셀프 채점, 남은 리스크 보고
