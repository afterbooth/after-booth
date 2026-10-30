# SPEC — AfterBooth (애프터부스)

행사 잉여 물품 연결 서비스. 축제·전시·팝업이 끝나고 남는 물품(진열대, 수납함, 접이식 테이블)을 **철거 마감 전에** 다음 행사나 단체로 넘긴다.

## 대회 조건

- Meta Global AI Developer Hackathon, 인디 트랙, 2인 팀
- 제출 마감 2026-11-05 11:00 KST, 팀 목표 11/4 저녁
- 심사: Impact 30% / Use of Meta AI Tooling 25% / Demo 25% / Technical Depth 20%
- 제품 런타임 AI는 전부 Meta Model API: **Muse Spark, SAM 3.1, Muse Voice Transcribe**. Muse Image는 쓰지 않는다
- 개발 도구(Claude Code, Codex, Muse Code)는 README에 밝힌다
- 비권장 카테고리(기본 RAG, 단순 이미지 분석기)에 걸리지 않도록 AI 결과는 반드시 재고 등록·배정·인계로 이어진다

## 핵심 원칙

**AI는 제안만 하고, 최종 판단은 서버가 한다.** 수량·시간·차량 조건은 서버가 검증한다. 에이전트는 HOLD까지만 만들 수 있고 확정은 사용자가 한다.

## 기능

### 1. 사진 등록 (주는 쪽)
- 부스 사진 → **SAM 3.1**이 물품을 하나씩 분리하고 개수를 센다
- **Muse Spark**가 잘라낸 이미지로 품목, 대략 크기(cm), 상태를 추정하고 confidence를 낸다
- 재고 초안 생성 → 사람이 확인·수정해서 확정. confidence가 낮은 항목은 "확인 필요"

### 2. 음성 조건
- "8시까지 철거, 승합차 필요" → **Voice Transcribe** → **Spark**가 JSON 스키마로 구조화 (`pickupDeadline`, `vehicleType`, `quantity`, `purpose`)
- 원문과 해석 결과를 둘 다 반환

### 3. 수령 요청 (받는 쪽)
- 음성 또는 텍스트 (예: "주말 행사용 진열대, 승용차만 있음")

### 4. 배정 에이전트
- Spark가 function calling으로 Spring 도메인 툴 호출: `searchCandidates`, `checkVehicleFit`, `holdQuantity`
- 서버가 검증. 조건이 안 맞으면 Spark가 이유와 대안을 한국어로 설명
  - 예: "높이 180cm라 승용차 불가, 대신 접이식 테이블 3개 어떠세요?"

### 5. 인계 확인
- 수령 때 사진 1장 → SAM이 다시 개수를 셈 → 예약 수량과 비교해 완료 처리, 재사용 수량 기록

## 도메인

- Organization(시드), Item(품목, 크기, 상태, totalQty, availableQty, pickupDeadline, vehicleRequirement), Request, Reservation, HandoverLog, ReuseStat
- Reservation 상태: `HELD → CONFIRMED → HANDED_OVER`, `HELD|CONFIRMED → RELEASED`, `HELD → EXPIRED`
- 수량 차감: 조건부 UPDATE (`available_qty >= :q`), 영향 행 0이면 실패
- 요청마다 멱등키. 취소하면 수량 원복. HOLD 만료·철거 마감 경과 시 스케줄러가 자동 해제
- 차량 적합성: 차량 타입별 적재 공간 상수(승용차, SUV, 승합차, 1톤) vs 물품 크기 → 결과 + 사유 코드

## 범위

- 최소: 지역 1곳, 품목 3종(진열대·수납함·접이식 테이블), 직접 수령만, 단체 계정은 시드 데이터
- 만들지 않음: 지도, 알림, 결제, 회원가입

## 필수 테스트 (Testcontainers)

1. 남은 2개에 100스레드 동시 신청 → 정확히 2개만 성공
2. 취소·만료 후 수량 원복
3. 같은 멱등키 재요청 → 중복 차감 없음
4. 금지된 상태 전이 차단
5. 에이전트 툴이 서버 검증을 우회할 수 없음

## 화면 (한국어, 모바일 우선)

1. **촬영·등록**: `<input capture>` 촬영, 분리된 물품 박스 오버레이, 품목별 개수, 수정 후 확정
2. **수령 요청**: MediaRecorder 녹음, 해석된 조건 카드, 배정안과 "안 맞는 이유 + 대안"
3. **배정 보드**: 철거 마감 카운트다운, 남은 수량 실시간 표시(폴링), 동시 신청 결과

## 데모 시나리오

1. 사진 촬영 → "진열대 4, 수납함 12" 자동 등록
2. 음성으로 수령 요청
3. 배정안 제시
4. 브라우저 두 개로 마지막 진열대 동시 신청 → 한쪽 성공, 다른 쪽은 에이전트가 바로 대안 제시
5. 인계 완료
6. 재사용 수량 집계

데모 영상의 첫 15초 안에 1번이 나와야 한다.
