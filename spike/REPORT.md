# Spike #2 report: Meta Model API for AfterBooth

Measured on 2026-10-31 with key A: 139 HTTP calls logged as 137 records (the two Java tool checks make two calls each) plus one unlogged `GET /v1/models`. Estimated cost (usage × the pricing page; not yet checked against the billing dashboard) is about **$0.44** of the $15 spike budget. Raw data: `spike/out/calls.jsonl` (gitignored); representative responses: `fixtures/spike/`. Inputs: 8 Wikimedia Commons photos and 5 Windows TTS Korean clips (`spike/samples/`).

## 1. TL;DR

| Capability | Verdict |
|---|---|
| Muse Spark text, Korean | **Works.** One-sentence Korean answers 5/5 |
| Spark image → category, count, size, condition, boxes (structured output) | **Works.** Count in range 8/8 at both default and `minimal` effort; schema-valid 16/16. Sizes are rough (180 cm rack read as 150 cm in all 4 calls) |
| Spark tool calling (allocation agent loop) | **Works.** 25/25 calls well-formed, 5/5 runs end with a Korean answer offering an alternative. Happy path with a hold **not tested** |
| Spark structuring to `ConditionInterpretation` | **Works.** 21/22 fields at default, 22/22 at `minimal`, 22/22 on real Transcribe output |
| SAM 3.1 concept prompt ("folding table") as the counter | **Does not work reliably.** Positives in range 4/8; very phrase-sensitive |
| SAM 3.1 box prompts seeded with Spark boxes | **Works.** 8/8 photos returned exactly one mask per box, median IoU 0.83 with the Spark box |
| Muse Voice Transcribe, Korean | **Works with errors.** 5/5 ok, mean CER 0.187 (numbers come back in Hangul, some duplicated or misheard words); downstream structuring still 22/22 |
| Spring AI 2.0.1 (OpenAI starter) | **Works** for text, `@Tool` calling and image input with `base-url=${META_BASE_URL}/v1`. RestClient fallback also works but is not needed |

**Recommended photo pipeline:** Spark (`minimal` effort) detects, classifies, counts and boxes every item → SAM 3.1 box prompts turn those boxes into masks and tight boxes for the overlay and crops → optionally one SAM concept-prompt count per category as a cross-check, where a disagreement with Spark's count (or a low Spark/SAM confidence) sets `needsReview`. This needs a human decision because the contract currently says SAM counts (section 6a).

## 2. Confirmed configuration

- Base URL `https://api.meta.ai/v1`, `Authorization: Bearer <key>`. `GET /v1/models` listed `sam-3.1`, `muse-spark-1.3`, `muse-spark-1.3-contributor`, `muse-spark-1.2`, `muse-spark-1.2-contributor`, `muse-spark-1.1`, `muse-voice-transcribe-1.0`, `muse-image-1.0`.
- `META_BASE_URL=https://api.meta.ai` (no `/v1`) is fine for our own clients, which append `/v1`. **Spring AI 2.0.1 does not append `/v1`**: it wraps the official `openai-java` SDK, which appends only `chat/completions` to `spring.ai.openai.base-url` (SDK default `https://api.openai.com/v1`). So set `spring.ai.openai.base-url=${META_BASE_URL}/v1`. The old `.env.example` comment said the opposite; it is corrected in this PR (D-027).
- Request shapes that worked:
  - Spark: `POST /v1/chat/completions`, image as `{"type":"image_url","image_url":{"url":"data:image/jpeg;base64,..."}}`, structured output as `response_format: {type:"json_schema", json_schema:{name, schema}}`, tools in the nested Chat Completions format, optional `reasoning_effort`.
  - SAM: `POST /v1/responses`, non-streaming, `model:"sam-3.1"`, one user message with `input_text` + `input_image` (data URL), `metadata: {include_confidence:"true", mask_encoding:"one_bit"}`. Result text is in `output[0].content[0].text` (there is no top-level `output_text`); no match = empty string with `status:"completed"`. Box prompts: `<0f>1<|box;x1=..;y1=..;x2=..;y2=..;w=W;h=H|>,2<|box;...|>` in source pixels. `usage` reports `images_processed: 1` and zero tokens.
  - Transcribe: `POST /v1/asr/transcribe`, multipart with a `request` part (`application/json`: `{"model","audioEncoding":"WAV","mode":"PUSH_TO_TALK","languageBias":["Korean"]}`) and an `audio` part (`audio/wav`). Response `{sessionId, transcript, audioDurationMs, turns:[]}`. Only 16 kHz mono WAV was sent. Per the docs (not exercised), it accepts WAV only (RIFF mono 16-bit PCM at 16 or 24 kHz, ≤ 10 min, ≤ 32 MB) and returns 400 for anything else.
- API constraints from the docs (not exercised): `tool_choice` accepts only `"auto"`; HTTP 400 for `stop`, `n > 1`, `logprobs`, frequency/presence penalties and `reasoning_effort:"none"`; Spark is tuned for the default temperature. Spring AI 2.0.1 sends none of these by default.

## 3. Results

Latency is wall clock from Seoul, one request at a time; no 429 in any run.

| Capability (effort) | Calls | OK | p50 ms | max ms | Accuracy | Avg tokens in/out | $/call |
|---|---|---|---|---|---|---|---|
| models | 1 | 1 | 1223 | 1223 | 3/3 configured IDs listed | – | 0 |
| Spark text (default) | 5 | 5 | 4939 | 6908 | 5/5 Korean, one sentence | 35 / 786 | 0.0034 |
| Spark image (default) | 8 | 8 | 7676 | 19695 | count in range 8/8, exact 6/8 | 1273 / 2123 | 0.0106 |
| Spark image (`minimal`) | 8 | 8 | 3253 | 6417 | count in range 8/8, exact 7/8 | 1273 / 499 | 0.0037 |
| Spark tools, per turn (default) | 25 | 25 | 1636 | 2861 | 5/5 runs pass | 1590 / 326 | 0.0023 |
| Spark struct (default) | 5 | 5 | 4371 | 6499 | 21/22 fields + schema | 235 / 1169 | 0.0053 |
| Spark struct (`minimal`) | 5 | 5 | 1890 | 2461 | 22/22 | 235 / 356 | 0.0018 |
| SAM concept, `labels.json` phrases | 24 | 24 | 1198 | 3476 | positives 4/8 in range, negatives 16/16 zero | – | 0.0025 |
| SAM concept, other phrases | 32 | 32 | 1773 | 2948 | see 4.2 | – | 0.0025 |
| SAM box prompts from Spark boxes | 8 | 8 | 1879 | 2653 | 32/32 boxes returned, median IoU 0.83 | – | 0.0025 |
| Transcribe | 5 | 5 | 2616 | 3317 | mean CER 0.187 | 4–9 s audio | 0.0003 |
| Transcript → Spark struct (default) | 5 | 5 | 4488 | 9014 | 22/22 | 236 / 1220 | 0.0055 |
| Spring AI ChatClient: text, tools, image | 3 checks (4 HTTP) | 3 | 4956 | 7651 | all answered correctly | – | 0.0041 |
| RestClient: text, tools, image | 3 checks (4 HTTP) | 3 | 3136 | 5962 | all answered correctly | – | 0.0038 |

Default effort spends most output on reasoning (image: up to 3841 reasoning tokens of 4325 output on the cluttered booth photo). `minimal` cut image cost by 65% and p50 latency by 58% with no loss of count accuracy on these 8 photos.

## 4. Findings and fallbacks

### 4.1 Spark image estimation
- Counts: default 1/1/7/4/3/1/1/8 and minimal 1/1/6/4/3/1/1/9 against truth 1/1/6/4/3/1/1/7 (ranges in `labels.json`). It also listed chairs and tents as `OTHER` on the booth photo (5–6 items), which the product must ignore or drop.
- Boxes: one box per counted unit in all 16 responses.
- Size: the only photo with a known size (gondola 80×50×180 cm) came back as 100×50×150 cm in every call (default, minimal, Spring AI, RestClient). Height errors decide vehicle fit, so the confirm screen must make size easy to correct.
- Confidence is self-reported and clusters at 0.78–0.97; only the cluttered scenes drop below 0.86. Do not rely on it alone for `needsReview`.
- Fallback if it degrades: re-ask once on schema failure (already the plan), then FAILED at stage ESTIMATE.

### 4.2 SAM concept prompts
- With "folding table", "storage bin", "display rack": correct on 4/8 positives (single folding table, orange bins 4, gondola 1, empty store 9). Misses: picnic table 0, booth tables 0 of 6, postcard rack 0; nested bins 5 for 3. No cross-category false positives (16/16 zero).
- Phrase sensitivity: "table" → 2 on the single-table photo, 1 on the picnic table, 4 of 6 on booths. "shelf" counts shelf boards (4 on the gondola, 28 in the store). "rack" → 3 parts of the gondola, 0 on the postcard rack, 8 in the store. "plastic crate" → 5 for both 4 orange and 3 nested bins.
- Concept-prompt confidences never went below 0.51 (min 0.508), which suggests a server-side cut at 0.5.
- **Fallback:** do not use the concept-prompt count as the inventory count. Use Spark's count and box prompts (4.3).

### 4.3 SAM box prompts seeded with Spark boxes (`run.mjs sam-boxes`)
- `sam-boxes` converts Spark's 0–1000 boxes from the latest successful `spark-image` response into SAM box prompts in source pixels (max 16 per request) and compares each returned box with the prompt box by IoU. That run used the **default-effort** Spark boxes, including the 6 `OTHER` boxes on the booth photo (13 prompts).
- Every prompt came back as one record with a mask (32/32). IoU between SAM's box and Spark's box: median 0.83; 3 of 32 below 0.5 (0.04 on a booth chair, 0.36 and 0.48 in the store), which flag Spark boxes SAM disagrees with.
- Confidence median 0.79 (phrase prompts: 0.68), but box prompts can return values down to 0.41.
- p50 1.9 s for all boxes of a photo in one request, $0.0025 per photo.

### 4.4 Voice Transcribe
- Errors on TTS audio: digits come back as Hangul numerals (8시 → 여덟 시, 12개 → 열두 개, 11월 2일 → 십일 월 이 일), duplicated words ("필요한데 필요한데", "오전 오전", "됩니다 됩니다"), mishearings ("접이식" → "저비식", "수납함" → "순합함"), punctuation sometimes dropped.
- Spark structuring tolerated all of these: 22/22 fields on the chained transcripts.
- Only synthetic TTS was tested. Real phone microphone audio, noise and accents are not measured.
- **Fallback:** always show the raw transcript next to the interpreted fields so the user can correct either; typed text goes through the same Spark path.

### 4.5 Spark structuring
- Default effort omitted `pickupDeadline` for "11월 2일 오후 3시 30분까지" once (the field was absent, not null); `minimal` got 22/22. Relative times ("오늘 저녁 8시", "내일 오전 10시", "모레 오전 9시 반") resolved correctly against the given `now` with `+09:00`.
- `purpose` is filled inconsistently (minimal filled "주말 행사에 쓸" once; default never did).
- Fallback: re-ask once on schema failure; missing fields go to `missingFields` for the UI to ask.

### 4.6 Allocation agent (stub tools)
- Stub: request = 2 display racks, sedan; both racks fail vehicle fit; a folding table and a storage bin both fit.
- All 5 runs: `searchCandidates(DISPLAY_RACK)` → `checkVehicleFit` ×2 (parallel) → `searchCandidates(STORAGE_BIN, FOLDING_TABLE)` → `checkVehicleFit` ×2 → final answer. 5 turns, 7.5–10.1 s per allocation. `holdQuantity` was never called, which matches D-018 (the agent holds only the requested category).
- Final answers were Korean and offered folding tables, but never named the limiting dimension ("높이 180cm") or a quantity. The server's rejection messages should carry the reason text the UI shows.
- Not tested: a request where a hold should succeed, partial holds, `QTY_*` rejections.

### 4.7 Spring AI
- ChatClient text, `@Tool` (two HTTP calls; usage is reported for the whole exchange) and image media all worked. No RestClient fallback is needed; the RestClient path in `spike/spring-ai` is kept only as evidence.

## 5. Implications for upcoming issues

- **#4 AiGateway:** Spring AI 2.0.1 for Spark chat, tools and structured output, with `base-url=${META_BASE_URL}/v1` (`backend/src/main/resources/application.properties` still sets it without `/v1`; #4 must fix it) and retries off or bounded (the SDK default is 3 retries). SAM `/v1/responses` and Transcribe multipart are outside Spring AI's model: use `RestClient` for both. Suggested replay key: SHA-256 over method + path + canonical JSON body where every image or audio payload is replaced by its SHA-256 (the redaction `run.mjs` already applies); Spark output is not deterministic, so keying by content would not work. Timeouts: Spark `minimal` 15 s, Spark default 30 s, SAM 10 s, Transcribe 15 s for ≤ 60 s clips (60 s clips not measured).
- **#5 photo intake:** order is ESTIMATE (Spark) then SEGMENT (SAM box prompts), the reverse of the current `IntakeStage` description. Expected time per photo is about 5–8 s (Spark minimal 3.3 s p50 / 6.4 s max + SAM 1.9 s), which fits the "registered within 15 s" demo step. Drop `OTHER` items. Size goes to the confirm screen for correction.
- **#6 spoken conditions:** WAV transcoding (section 6b), `languageBias:["Korean"]`, Spark `minimal` with structured output, show the raw transcript.
- **#10 handover recount:** the same Spark-count + SAM-box-prompt path on one pickup photo, filtered to the reserved category, compared with the reserved quantity (section 6e). Not measured on handover-style photos.
- **#9/#10 allocation agent:** default effort works at about 2 s per turn and 5 turns. Cap turns (6) and total time (30 s). Measure `minimal` and the hold happy path in #10.
- **Fixtures:** `fixtures/spike/` holds raw spike responses (see its README); #4 defines the replay naming and may re-record.

## 6. Decisions for humans (G0)

a) **Photo pipeline and SAM's role.** Options: (1) SAM concept prompts count, Spark labels: measured 4/8 correct. (2) Spark counts and boxes, SAM box prompts make masks, and a SAM concept count cross-checks for `needsReview`: Spark 8/8 in range, SAM 32/32 masks. (3) Spark only, no SAM: simplest, but drops a required Meta model and the masks. **Recommend (2).** If accepted, these change first (SPEC outranks the contract): `docs/SPEC.md` §Features 1 ("SAM 3.1 segments items one by one and counts them", "Spark estimates … from each crop"), then `contract/openapi.yaml` `createIntakeDraft` stages (line 111), `IntakeStage` (line 825, order ESTIMATE → SEGMENT) and `DraftLine.count` (line 871, count from Spark), recorded in `docs/DECISIONS.md`.

b) **D-022 upload limits and audio format.** Transcribe takes only WAV mono 16-bit PCM at 16/24 kHz. Options: (1) the browser records or converts to 16 kHz mono WAV (Web Audio / AudioWorklet PCM capture, no server dependency, larger upload: 60 s ≈ 1.9 MB); (2) the backend transcodes webm/ogg/m4a with ffmpeg (adds a binary to the image and CI). **Recommend (1)**: audio limit 60 s, `audio/wav` only in the contract. Photos: keep 10 MB (Spark inline limit is 50 MB; SAM accepted 1280 px JPEGs; larger images cost more image tokens).

c) **`reasoning_effort` defaults.** **Recommend** `minimal` for photo estimation and condition structuring (same accuracy here, ~60–65% cheaper, 2–3× faster), and the model default for the allocation agent until #10 measures `minimal`. Make it configuration per call type.

e) **Handover recount.** SPEC §Features 5, the contract (`createHandover` line 506, `HandoverResult.count` line 1280) and D-024 ("a handover can never complete without a SAM 3.1 recount") assume a SAM concept-prompt count, which measured 4/8 here. Options: (1) keep the SAM concept count and route every disagreement with the reserved quantity to MISMATCH → `resolveHandover`: honest, but many false MISMATCHes in the demo. (2) Same path as intake option (2): Spark counts the reserved category and SAM box prompts segment each counted unit; the count matches only if SAM returns a mask for every Spark box above a confidence threshold, otherwise MISMATCH. SAM 3.1 still runs on every handover, so D-024's rule holds if it is reworded "SAM 3.1 segmentation". **Recommend (2)**, with the same SPEC → contract (lines 506, 1280) → D-024 updates as in (a).

f) **Budget.** The spike used about $0.44 of the $15 estimate. Per-call costs: Spark image $0.004 (minimal), struct $0.002, agent ≈ $0.012 per allocation, SAM $0.0025 per image, Transcribe ≈ $0.0003 per clip. A full demo scenario is under $0.05. **Recommend** lowering the Phase 1 estimate from $25 to $5 and Phase 2 from $30 to $10, keeping the rest in reserve.

## 7. Not verified / risks

- Accuracy is from 8 photos and 5 synthetic clips. Real booth photos, phone-camera audio, noise and accents are untested.
- Size estimation is checked against one known size only.
- No agent run where a hold should succeed; no test of concurrent agent runs or of rate limits (no 429 seen; limits are per team).
- `minimal` effort is untested for tool calling and Korean explanations.
- Spark responses vary between runs; all numbers are single runs per input (5 for text and tools).
- Streaming was not used for any capability.
- During this spike, `run.mjs` still wrote raw exchanges to `spike/out/<capability>/`, so the `minimal` re-run overwrote the default-effort `spark-image` and `spark-struct` raws. They were copied to `spark-image-default/` and `spark-struct-default/` in the local `spike/out` first. `fixtures/spike/spark-image-default/` keeps only the two cluttered photos; the default-effort struct raws are not in fixtures. `run.mjs` now writes `spike/out/<run id>/<capability>/` (Codex review).
- Ground truth for the two cluttered photos is a judgment call (ranges in `labels.json`).

## 8. Reproduce

From the repo root (live calls ask a human through the credit hook):

```bash
node spike/run.mjs all --dry-run                                     # no network, prints 81 requests
node --env-file=.env spike/run.mjs all                               # ~78 calls, ~$0.27
node --env-file=.env spike/run.mjs sam --phrase FOLDING_TABLE=table --phrase DISPLAY_RACK=shelf --phrase STORAGE_BIN="plastic crate"
node --env-file=.env spike/run.mjs sam --phrase DISPLAY_RACK=rack
node --env-file=.env spike/run.mjs sam-boxes                         # uses the latest ok spark-image call per photo in calls.jsonl
node --env-file=.env spike/run.mjs spark-image --effort minimal
node --env-file=.env spike/run.mjs spark-struct --effort minimal
node --env-file=.env spike/with-env.mjs backend/gradlew.bat -p spike/spring-ai bootRun --args=all
node spike/summarize.mjs                                             # writes spike/out/summary.json
```
