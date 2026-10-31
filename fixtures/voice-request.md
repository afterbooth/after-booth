# Voice pickup request fixtures

The synthetic Korean sample is `spike/samples/audio-receiver-racks-sedan.wav`.
The request fixture tests use the fixed instant `2026-10-31T03:00:00Z` and the same
`ConditionService` pipeline, prompt, schema and WAV bytes for record and replay.
No response is fabricated while recording is pending.

A human may record the Transcribe and Spark responses after the credit gate:

```powershell
node --env-file=.env spike/with-env.mjs backend/gradlew.bat -p backend test --tests com.afterbooth.request.service.ConditionInterpretationRecordTest -PrecordSmoke --rerun
```

This reuses the existing opt-in `recordSmoke` Gradle property. It can make two Spark
calls if the first structured response fails server validation. Inspect the saved
files and log the actual estimated spend in `docs/BUDGET.md`, then run the secret scan.

The replay test `com.afterbooth.request.service.ConditionInterpretationReplayTest.replaysReceiverRacksSedan`
runs in every default build. It asserts DISPLAY_RACK, SEDAN, a nonempty purpose, the transcript and
no missing required fields. Re-record whenever the prompt, schema, model ID or WAV changes.

Prompt and schema: `backend/src/main/java/com/afterbooth/request/domain/ConditionPrompt.java`
(`SYSTEM` and `SCHEMA`). Changing these constants, the configured model IDs, fixed
instant or audio bytes requires recording new fixture keys.
