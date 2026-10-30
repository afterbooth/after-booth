# fixtures

`AI_MODE=replay`일 때 AiGateway가 읽는 AI 응답과, E2E·테스트용 입력 파일을 둔다. `record` 모드가 여기에 저장한다.

- 실제 사람 얼굴·개인정보가 든 사진·음성은 넣지 않는다
- 응답 파일 이름은 요청 해시 기반으로 ai-pipeline이 정한다 (형식은 B-1에서 확정 후 이 파일에 적는다)
- 키·토큰이 응답에 섞여 있지 않은지 커밋 전에 secret-scan이 검사한다
