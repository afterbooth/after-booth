# spike

Phase 0 technical spike (#2). **Throwaway code only.** Write conclusions in `REPORT.md`; product code never imports from here.

- Let the runtime read the key directly, e.g. `node --env-file=../.env <script>` (the hook asks a human first)
- `samples/`: photos and audio for measurements. No faces or personal data. Sources and licenses in `samples/SOURCES.md`, ground truth in `samples/labels.json` and `samples/audio.json`
- `out/`: raw responses and measurement logs (not committed). Move responses needed for replay to `fixtures/`

REPORT.md covers: works / does not work per capability, actual model IDs and endpoints, latency (p50/max), accuracy, cost per call, fallback when something fails.

## Running (from the repo root)

```bash
node spike/run.mjs all --dry-run                       # prints every request, no network, no key
node --env-file=.env spike/run.mjs <command>           # live: models | spark-text | spark-image | spark-tools | spark-struct | sam | transcribe | all
node spike/summarize.mjs [--last]                      # spike/out/calls.jsonl → summary.json + Markdown table
node --env-file=.env spike/with-env.mjs backend/gradlew.bat -p spike/spring-ai bootRun --args=all   # Spring AI + RestClient check
```

Options for `run.mjs`: `--repeat N`, `--input <photo or clip id>`, `--effort <minimal|low|medium|high>` (sets `reasoning_effort` on Spark calls), `--no-lang-bias` (drops `languageBias: ["Korean"]` from Transcribe).
