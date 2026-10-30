# fixtures/spike

Raw Meta Model API responses captured by the Phase 0 spike (#2) on 2026-10-31 with key A. Nothing here is hand-written or edited.

Each file is `{ request: { method, url, body }, response: { status, latencyMs, body } }` as saved by `spike/run.mjs`. Image and audio payloads in the request are replaced by `{ redactedDataUrl, sha256, bytes }` or `{ file, sha256, bytes }`; the key and headers are never written. Inputs are in `spike/samples/`.

These files back `spike/REPORT.md` and are input for tuning prompts and parsers. They are **not** the replay format yet: #4 (AiGateway) decides the request-hash naming that `AI_MODE=replay` reads, documents it in `fixtures/README.md`, and may re-record.

| Folder | Files | What |
|---|---|---|
| `models/` | `list-1.json` | `GET /v1/models` |
| `spark-text/` | `rack-sedan-1.json` | Korean one-sentence answer, default effort |
| `spark-image/` | 8 × `<photo>-1.json` | Category, count, size, condition, boxes with `response_format` json_schema, `reasoning_effort: minimal` |
| `spark-image-default/` | `tables-closed-booths-1.json`, `racks-empty-store-1.json` | Same request at default effort (the cluttered photos) |
| `spark-struct/` | 5 × `<clip>-1.json` | Script text → `Conditions` + `missingFields`, `minimal` |
| `spark-struct-chain/` | 5 × `<clip>-1.json` | Transcribe output → `Conditions`, default effort |
| `spark-tools/` | `run1-turn1-1.json` … `run1-turn5-1.json` | One allocation agent loop with stub tools (full message history in each request) |
| `sam/` | 8 × `<photo>--<CATEGORY>-1.json` | SAM 3.1 concept prompt on the labelled category of each photo (`labels.json` phrases), with confidence, one_bit masks |
| `sam-boxes/` | 8 × `<photo>-1.json` | SAM 3.1 box prompts built from the default-effort Spark boxes |
| `transcribe/` | 5 × `<clip>-1.json` | Voice Transcribe, WAV, `PUSH_TO_TALK`, `languageBias: ["Korean"]` |

Spring AI and RestClient checks wrote only summary lines to `spike/out/calls.jsonl`, no raw responses.

Checked before copying: no `data:` payloads (only the `redactedDataUrl` mime prefix), no `Bearer`, `sk-`, `Authorization` or `META_API_KEY`; `node scripts/secret-scan.mjs` passes on every file.
