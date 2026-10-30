# AGENTS.md — AfterBooth

Codex·Muse Code 등 AGENTS.md를 읽는 에이전트용 진입점이다. **규칙의 정본은 `CLAUDE.md`** 이며 이 파일은 차이점만 적는다.

## 먼저 읽을 것

1. `CLAUDE.md` — 절대 규칙, STOP 게이트, 파일 소유권, 명령어
2. `docs/TASKS.md` — 지금 어디까지 했는지
3. `docs/AGENT_MISTAKES.md` — 이 PC에서 재현된 함정
4. `docs/CONVENTIONS.md`, `docs/templates/` — 코드·커밋 규칙과 템플릿. 리뷰는 `docs/REVIEW_GUIDE.md` 형식(영어)으로 쓴다

## 도구별 차이

- 역할 정의는 `.claude/agents/*.md`가 정본이다. Codex 역할(`.codex/agents/*.toml`)은 해당 파일을 읽고 따르라고만 적혀 있다.
- 기능 구현 절차는 `.claude/skills/feature/SKILL.md`를 그대로 따른다.
- `.env`는 열지 않는다. Claude hook이 없는 도구에서도 이 규칙은 같다. 커밋은 git pre-commit hook(`scripts/secret-scan.mjs`)이 검사한다.
- live/record 모드로 Meta API를 호출하기 전에는 사람에게 묻고, 호출 후 `docs/BUDGET.md`에 비용을 적는다.
- `git push`, main 머지는 사람이 한다.

## Muse Code로 작업할 때

심사 항목 "Use of Meta AI Tooling"에 반영되도록, Muse Code로 맡은 작업은 `docs/DECISIONS.md`에 `[Muse Code]` 태그로 한 줄 남기고 화면을 캡처해 둔다.
