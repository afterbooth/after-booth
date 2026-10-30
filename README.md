# AfterBooth

Surplus booth equipment from festivals, exhibitions and pop-ups — display racks, storage bins, folding tables — gets thrown away because teardown deadlines are hours away. AfterBooth hands it to the next event before the deadline.

Built for the Meta Global AI Developer Hackathon (Indie track). Full write-up comes in Phase 3. Product spec (Korean): [`docs/SPEC.md`](docs/SPEC.md).

## Meta models in the product

| Model | Role |
|---|---|
| SAM 3.1 | Segments and counts items in a booth photo; recounts at handover |
| Muse Spark | Estimates item type, size, condition; structures voice requests; allocation agent via function calling |
| Muse Voice Transcribe | Turns spoken pickup conditions into text |

AI only proposes. The server validates quantity, time and vehicle fit, and the agent can only create a HOLD.

## Run locally

Requirements: Docker Desktop, JDK 21, Node 22+.

```bash
cp .env.example .env          # fill META_API_KEY etc. (never commit)
docker compose up -d postgres
cd backend && ./gradlew bootRun
cd frontend && npm install && npm run dev        # or npm run dev:mock without backend
```

`AI_MODE=replay` (default) runs without network or credits using `fixtures/`.

## Development tools

Developed with Claude Code and Codex as coding agents, and Muse Code. Agent harness: [`CLAUDE.md`](CLAUDE.md), [`AGENTS.md`](AGENTS.md), [`.claude/`](.claude).
