# spike

Phase 0 technical spike (#2). **Throwaway code only.** Write conclusions in `REPORT.md`; product code never imports from here.

- Let the runtime read the key directly, e.g. `node --env-file=../.env <script>` (the hook asks a human first)
- `samples/`: photos and audio for measurements. No faces or personal data
- `out/`: raw responses and measurement logs (not committed). Move responses needed for replay to `fixtures/`

REPORT.md covers: works / does not work per capability, actual model IDs and endpoints, latency (p50/max), accuracy, cost per call, fallback when something fails.
