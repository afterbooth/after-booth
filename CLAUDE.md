# CLAUDE.md — AfterBooth (애프터부스)

행사(축제·전시·팝업)가 끝나고 남는 물품을 철거 마감 전에 다음 행사·단체로 넘기는 서비스.
Meta Global AI Developer Hackathon 인디 트랙 출품작. 제품 스펙의 정본은 `docs/SPEC.md`.

- 제출 마감: **2026-11-05 11:00 KST** (11/4 21:00 ET). 팀 목표는 11/4 저녁 제출 완료
- 심사 비중: Impact 30 / Use of Meta AI Tooling 25 / Demo 25 / Technical Depth 20

## 명령어

```bash
docker compose up -d postgres                 # 로컬 DB (포트 15432)
cd backend && ./gradlew bootRun               # Windows: .\gradlew.bat (Docker 필요)
cd backend && ./gradlew compileJava compileTestJava spotlessCheck   # 빠른 게이트
cd backend && ./gradlew test --tests "<패턴>"  # 대상 테스트
cd backend && ./gradlew spotlessApply          # Java 포맷 (커밋 전 필수)
cd frontend && npm run dev:mock               # 목(MSW)으로 프론트 개발
cd frontend && npm run build && npm test      # 프론트 게이트 (gen:api 자동 실행)
```

## 절대 규칙

1. **제품 안의 AI는 전부 Meta Model API** (Muse Spark, SAM 3.1, Muse Voice Transcribe). Muse Image는 쓰지 않는다. Claude·OpenAI 등 타사 모델을 제품 런타임에 넣지 않는다.
2. **AI는 제안만, 최종 판단은 서버.** 수량·시간·차량 조건은 서버가 검증한다. 에이전트는 HOLD까지만 만들고 확정은 사용자가 한다.
3. **분석만 보여주고 끝나는 기능 금지.** AI 결과는 재고 등록·배정·인계 같은 행동으로 이어져야 한다 (주최측 비권장: 기본 RAG, 단순 이미지 분석기).
4. **모든 AI 호출은 `AiGateway` 하나를 거친다.** `AI_MODE=live|record|replay`. 기본값과 자동 테스트는 항상 replay. 모델 ID·base URL·엔드포인트는 설정값이며 **추측 금지** — `https://dev.meta.ai/docs`와 `GET /v1/models`로 확인한 값만 쓴다 (`spike/REPORT.md`).
5. **`.env`는 사람만 다룬다.** 에이전트는 읽지도 쓰지도 않는다(hook이 차단). 앱·스크립트는 `--env-file=.env`나 docker compose `env_file`로만 키를 읽는다.
6. **크레딧 게이트.** live/record 호출(`--env-file=.env`, `AI_MODE=live|record`)은 hook이 사람에게 묻는다. 호출 후 `docs/BUDGET.md`에 추정 비용을 적는다. 누적 $50마다 STOP하고 보고한다.
7. **계약 우선.** `contract/openapi.yaml`이 기준이다. 계약 변경은 `contract/`만 담은 별도 커밋 + `docs/DECISIONS.md` 기록.
8. **실제 사람 얼굴·개인정보가 든 사진·음성은 `fixtures/`·`spike/samples/`에 넣지 않는다.**
9. **완료를 주장하기 전에 검증을 새로 돌리고 명령과 결과를 적는다.** 돌리지 못한 검증은 "미검증"으로 분리한다. mock 성공을 실제 API 검증처럼 쓰지 않는다.
10. 판단이 필요한 곳에서 멈추지 말고 합리적으로 정한 뒤 `docs/DECISIONS.md`에 한 줄로 남긴다. STOP 지점에서만 사람에게 묻는다.
11. **커밋·PR·이슈·리뷰 코멘트는 영어**, Conventional Commits (`docs/CONVENTIONS.md`). 제품 UI와 내부 문서는 한국어. commit-msg hook이 검사한다.
12. **코드는 `docs/CONVENTIONS.md`와 `docs/templates/`를 따른다.** 규칙은 도메인 객체에, 서비스는 조율만, 컨트롤러는 HTTP만. 스펙에 없는 기능·쓰지 않는 코드·코드를 반복하는 주석은 쓰지 않는다. `ArchitectureTest`가 레이어 규칙을 검사한다.

## 사람 확인 지점 (STOP)

풀자동으로 진행하되 아래에서만 멈춰 보고한다.

| 게이트 | 언제 | 보고 내용 |
|---|---|---|
| G0 | Phase 0 스파이크 끝 | `spike/REPORT.md` 요약, 사람이 정할 것 목록 |
| G$ | live/record 호출 직전(hook) · 누적 $50마다 | 이번 호출 목적, 누적 비용 |
| G3 | Phase 3 끝 (제출 전) | 실행 방법, 데모 결과, 셀프 채점, 남은 리스크 |

`git push`, main 머지, PR 머지는 permissions의 `ask`로 항상 사람이 승인한다.

## 작업 흐름 (GitHub)

1. 모든 작업은 GitHub 이슈에서 시작한다. 담당자는 이슈 assignee (namdongyeob / WookJaes). 둘 다 백엔드와 프론트를 같이 한다.
2. 브랜치 `<type>/<이슈번호>-<slug>` (예: `feat/12-conditional-hold`)를 worktree로 만든다.
3. 커밋 → `git push -u origin <브랜치>` → `gh pr create` (본문에 `Closes #N`, PR 템플릿 채우기).
4. 리뷰 3단계: Claude `reviewer` 에이전트 → Codex (`/codex:review --base main`, 위험한 변경은 `/codex:adversarial-review`) → 팀원(선택). 기준은 `docs/REVIEW_GUIDE.md`.
5. main은 보호돼 있다 (PR 필수, 승인 0명, squash 머지만). 머지는 사람이 한다.

## 작업 방식

- 메인 세션은 **오케스트레이터**다. 기능 구현은 `/feature` 스킬(`.claude/skills/feature/SKILL.md`)로 서브에이전트에 맡긴다. 파일 1~2개짜리 설정·문서 수정만 직접 한다.
- 서브에이전트 5개 (`.claude/agents/`): `backend-domain` / `ai-pipeline` / `frontend` / `reviewer`(읽기 전용) / `demo-qa`. 파일 소유권은 아래 표를 따르고, 한 파일에는 한 작성자만 둔다.
- 병렬 스트림은 worktree로 격리한다: `git worktree add .claude/worktrees/<이름> -b feat/<이름>`. 같은 폴더에서 브랜치를 바꾸지 않는다.
- 서브에이전트 보고만 믿지 않는다. 메인이 diff와 테스트 결과를 직접 확인한다.
- reviewer는 매번 새로 띄운다 (구현자의 전제를 이어받지 않게).
- Codex는 두 번째 눈이다: PR마다 `/codex:review`, 막히면 `/codex:rescue`, 세션을 넘길 땐 `/codex:transfer`. 팀원 세션에서는 `/codex:setup --enable-review-gate`로 작업 종료 전 자동 리뷰를 켤 수 있다.
- 한 문제에 30분 이상 막히면 `codex:rescue`로 한 번 넘겨 보고, 그래도 안 되면 `docs/TASKS.md`에 `BLOCKED`로 적고 다음 작업으로 간다.
- 각 Phase가 끝날 때마다 `docs/TASKS.md`를 갱신한다.
- 같은 실수를 재현·확인하면 `docs/AGENT_MISTAKES.md`에 한 줄 추가한다 (추측성 규칙 금지).

### 파일 소유권

| 에이전트 | 쓰기 범위 |
|---|---|
| backend-domain | `backend/**` 중 `com.afterbooth.ai` 패키지 제외, `backend/src/main/resources/db/migration/**` |
| ai-pipeline | `backend/src/main/java/com/afterbooth/ai/**`와 그 테스트, `fixtures/**`, `spike/**` |
| frontend | `frontend/**` |
| demo-qa | `e2e/**`, `docs/DEMO_SCRIPT.md` |
| reviewer | 없음 (읽기 전용) |
| 메인(오케스트레이터) | `docs/**`, `contract/**`, 루트 설정, 머지 |

## 아키텍처

- `backend/`: Java 21, Spring Boot 4.1, Spring AI 2.0 (OpenAI 스타터의 base-url을 Meta Model API로), PostgreSQL 17, Flyway, Testcontainers
  - 패키지: `com.afterbooth.<도메인>` (inventory, intake, request, allocation, handover, stats), 공통은 `com.afterbooth.global`, AI는 `com.afterbooth.ai`
  - 스키마 변경은 Flyway(`V{N}__*.sql`)로만. `ddl-auto=validate`
- `frontend/`: React 19 + Vite + TypeScript + Tailwind v4, 한국어 UI, 모바일 우선. API 클라이언트·MSW 목은 orval이 계약에서 생성 (`src/api/generated/`, 커밋 안 함). UI 작업에는 `frontend-design` 스킬을 쓴다.
- `contract/openapi.yaml`: API 계약 정본
- `fixtures/`: replay용 AI 응답. `spike/`: 버려도 되는 실험 코드와 `REPORT.md`
- 개발 도구로 쓴 것(Claude Code, Codex, Muse Code)은 README에 밝힌다.

## 이 PC에서 알려진 함정

`docs/AGENT_MISTAKES.md`를 먼저 읽는다. 특히 Testcontainers는 PowerShell 도구에서 돌린다 (Git Bash는 Docker 소켓을 못 찾는 경우가 있었다).

## 문서 라우팅 — 작업별로 읽을 것

전체 문서를 다 읽지 않는다. 작업 유형에 맞는 줄만 읽는다. 모든 작업은 `CLAUDE.md` + `docs/TASKS.md` + `docs/AGENT_MISTAKES.md`가 기본이다.

| 작업 | 추가로 읽을 것 |
|---|---|
| 백엔드 도메인 | `docs/SPEC.md` §도메인·§필수 테스트, `contract/openapi.yaml`, `docs/CONVENTIONS.md` §Backend, `docs/templates/backend/` |
| AI 파이프라인 | `docs/SPEC.md` §기능, `spike/REPORT.md`, `docs/BUDGET.md`, `docs/DECISIONS.md` D-010 |
| 프론트 화면 | `docs/SPEC.md` §화면·§데모 시나리오, `contract/openapi.yaml`, `docs/CONVENTIONS.md` §Frontend, `docs/templates/frontend/` |
| 계약 변경 | `contract/openapi.yaml`, `docs/DECISIONS.md` |
| 리뷰 | `docs/REVIEW_GUIDE.md`, `docs/SPEC.md`, `contract/openapi.yaml` |
| E2E·데모 | `docs/SPEC.md` §데모 시나리오, `docs/BUDGET.md` |
| 커밋·PR·이슈 | `docs/CONVENTIONS.md` §Git workflow·§Commit messages·§Pull requests |
| 제출 자료 | `README.md`, `docs/SPEC.md` §대회 조건, `docs/REVIEW_GUIDE.md` §Checklist |

## 문서 지도

| 문서 | 용도 |
|---|---|
| `docs/SPEC.md` | 제품 스펙·데모 시나리오 (정본) |
| `docs/TASKS.md` | Phase별 체크리스트, 이슈 번호, 담당자, BLOCKED |
| `docs/DECISIONS.md` | 판단 기록 (계약 변경 포함) |
| `docs/CONVENTIONS.md` | 언어, Git 흐름, 커밋 규칙, 코드 설계·주석 규칙 (영어) |
| `docs/REVIEW_GUIDE.md` | 리뷰 3단계, 코멘트 형식, 체크리스트 (영어) |
| `docs/templates/` | 백엔드·프론트 코드 템플릿 |
| `docs/BUDGET.md` | Meta API 크레딧 사용 계획과 원장 |
| `docs/AGENT_MISTAKES.md` | 재현된 에이전트 실수 |
| `spike/REPORT.md` | Phase 0 스파이크 결과 (Phase 0에서 생성) |
