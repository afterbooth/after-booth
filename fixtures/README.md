# fixtures

AI responses that `AiGateway` reads when `AI_MODE=replay`, plus input files for E2E and tests. `record` mode writes here.

- No photos or audio with real faces or personal data
- ai-pipeline decides the file naming, based on a request hash (document the format here once #4 settles it)
- secret-scan checks before each commit that no key or token ended up in a response
