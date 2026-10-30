# BUDGET — Meta Model API credits

Team credits: **key A $150 + key B $150 = $300**. Measure the cost per call in Phase 0 (#2) and update the table below.

## Key roles

| Key | Owner | Use | Cap |
|---|---|---|---|
| A | namdongyeob | Phase 0 spike, Phase 1 schema tuning, Phase 2 live E2E and record | $90 (reserve $60) |
| B | WookJaes | Small prompt tuning for #9 and #10 in Phase 1 (≤ $15). Demo rehearsal, video recording, 11/9 live finals reserve | $45 (reserve $105) |

Phase 1 development replays the fixtures saved by the #2 spike by default. Use your own key for live calls only when you must.

`META_API_KEY` in `.env` is swapped by a human. When switching to key B, update the "Current key" line below.

Current key: **A**

## Plan per stage

| Stage | Key | Estimate | Notes |
|---|---|---|---|
| Phase 0 spike | A | ≤ $15 | Model list is free; 5–10 calls per capability. Save responses straight to fixtures/ |
| Phase 1 development | A | ≤ $25 | Replay by default. Live only while ai-pipeline tunes prompts and schemas |
| Phase 2 live E2E + record | A | ≤ $30 | Scenario once or twice → record → replay afterwards |
| Demo rehearsal and recording | B | ≤ $30 | Record in live mode, fall back to replay on failure |
| Finals reserve | B | the rest | 11/9 live finals |

Automated tests and CI always use replay: $0.

## Measured cost per call (#2, key A, 2026-10-31)

Measured in the #2 spike from the `usage` in each response (details in `spike/REPORT.md`). Spark output is mostly reasoning tokens, so `reasoning_effort` drives both cost and latency.

| Call | Default effort | `reasoning_effort: minimal` |
|---|---|---|
| Spark text (one Korean sentence) | $0.0034 | not measured |
| Spark image → items JSON with boxes | $0.0106 | $0.0037 |
| Spark structuring (transcript → conditions) | $0.0053 | $0.0018 |
| Spark tool-calling turn (allocation agent, ~5 turns per run) | $0.0023 | not measured |
| SAM 3.1 image segmentation (per image, any number of objects) | $0.0025 | n/a |
| Voice Transcribe (per 5 s clip, billed in whole seconds) | $0.0002 | n/a |

A full demo pass (one photo intake, one voice request, one allocation, one handover photo) is about $0.05.

## Ledger

Add one row for every live/record session. STOP (G$) each time the cumulative total passes another $50.

| Date | Key | Purpose | Calls | Estimated cost | Cumulative |
|---|---|---|---|---|---|
| 2026-10-31 | A | #2 spike: models, Spark text/image/tools/structuring, SAM concept prompts, Transcribe (`run.mjs all`) | 79 | $0.27 | $0.27 |
| 2026-10-31 | A | #2 spike: SAM alternative phrases, SAM box prompts from Spark boxes | 40 | $0.10 | $0.37 |
| 2026-10-31 | A | #2 spike: Spring AI and RestClient (text, tools, image) | 6 | $0.02 | $0.40 |
| 2026-10-31 | A | #2 spike: Spark image and structuring at `reasoning_effort: minimal` | 13 | $0.04 | $0.44 |
