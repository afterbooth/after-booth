# fixtures

AI responses that `AiGateway` reads when `AI_MODE=replay`, plus input files for E2E and tests. `record` mode writes here.

- No photos or audio with real faces or personal data
- secret-scan checks before each commit that no key or token ended up in a response
- `spike/` holds the raw Phase 0 responses (see its README); they are not read by replay

## Replay format

One file per recorded call: `fixtures/replay/<capability>/<key>.json`, where `<capability>` is `spark`, `sam` or `transcribe` (`com.afterbooth.ai.FixtureStore`).

```json
{
  "request": { "method": "POST", "path": "/v1/responses", "body": { "...": "redacted request" } },
  "response": { "status": 200, "body": { "...": "the response exactly as Meta sent it" } }
}
```

- `<key>` is the SHA-256 (hex) of `POST <path>\n<canonical body>`.
  - `<path>` is the endpoint path from `afterbooth.ai.paths.*` (`/v1/chat/completions`, `/v1/responses`, `/v1/asr/transcribe`). The base URL is not part of the key.
  - `<canonical body>` is the request JSON with keys sorted at every level and no whitespace, after redaction: every `data:<mime>;base64,...` string becomes `{"bytes", "redactedDataUrl", "sha256"}` (the same redaction `spike/run.mjs` uses).
  - Transcribe is multipart, so its body is `{"request": <the JSON request part>, "audio": {"bytes", "sha256"}}`.
- Anything in the request changes the key: model ID, prompt, schema, tools, `reasoning_effort`, image or audio bytes. Spark answers differ between runs, so the response is never part of the key.
- The file holds the redacted request for humans and the response. The key and headers are never written. Only 2xx responses are recorded.
- A replay request with no file throws `MissingFixtureException` naming the capability, the key, the expected path and how to record it. Replay never calls the network and never makes up a response.

## Recording

`AI_MODE=record` makes the live call and writes the file above. It spends credits, so a human runs it (AGENTS.md hard rule 6; the Claude Code hook asks first), then logs the cost in `docs/BUDGET.md`.

The gateway smoke records one tiny call per model (a Korean Spark text prompt at `reasoning_effort: minimal`, one SAM box prompt on `spike/samples/table-lifetime-single.jpg`, one Transcribe call on `spike/samples/audio-giver-van-tonight.wav`), about $0.004 in total at the spike's per-call costs. From the repo root:

```bash
node --env-file=.env spike/with-env.mjs backend/gradlew.bat -p backend test --tests com.afterbooth.ai.AiGatewayRecordSmokeTest -PrecordSmoke --rerun
node scripts/secret-scan.mjs fixtures/replay/spark/*.json fixtures/replay/sam/*.json fixtures/replay/transcribe/*.json
```

`AiGatewaySmokeReplayTest` then replays those three files on every test run, with no key and no network. It is skipped only while `fixtures/replay/` does not exist.

The model IDs in `.env` must match the defaults in `application.properties` (D-027), because tests replay with the defaults and the model ID is part of the key.

Other calls are recorded the same way, through an opt-in test like `AiGatewayRecordSmokeTest` (Gradle forces `AI_MODE=replay` for tests) or the app started with `AI_MODE=record`. Commit the new files under `fixtures/replay/` only after the secret scan passes.
