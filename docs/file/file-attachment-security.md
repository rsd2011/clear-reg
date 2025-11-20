# File Attachment Security Plan (T17)

## 목표
- 첨부 업로드/다운로드/삭제 시 권한·조직 스코프 검증 강화
- 악성 파일 유입 방지(AV 스캔), 안전한 서빙(서명/성능), 감사 로깅 포함

## 권한/스코프
- 업로드: `FeatureCode.FILE` + `ActionCode.UPLOAD` (기안 맥락 시 작성자/조직 확인)
- 다운로드: `ActionCode.DOWNLOAD`, 기안 작성자/결재자/참조자 또는 관리자만 허용
- 삭제: `ActionCode.DELETE`, 소유자 또는 관리자만
- 조직 범위: RowScope(ORG/AUDIT)로 파일 메타데이터 접근 제한

## 보안/스캔
- 업로드 시 AV 스캔 필수 (선호: ClamAV/Provider API). 스캔 실패/시간초과 → 업로드 거부.
- MIME/확장자 화이트리스트, 사이즈 제한. 압축 파일은 재귀 해제 후 스캔 옵션.
- 저장 시 해시(SHA-256) 기록, 중복 리유즈 있으면 재스캔 스킵 옵션.

## 전달/서빙
- 다운로드 URL은 서명(단기 만료) 또는 직접 스트림. 캐시 헤더 제한(no-store).
- `Content-Disposition` 안전 인코딩(이미 적용), `Content-Type` 오버라이드 방지.

## 감사/알림
- 업로드/다운로드/삭제마다 감사 이벤트 기록 + outbox/SIEM 전달.
- 실패/차단 이벤트도 감사에 포함.

## 운영/보존
- 파일 메타/히스토리 보존: 1년 이상 (규제에 맞춰 조정).
- AV 시그니처 업데이트 모니터링, 스캔 실패 재시도 큐.

## 다음 작업(TODO)
1) FileManagementPort 어댑터에 AV 스캔 훅 추가 (동기 or 비동기 폴러)  
2) 파일 메타에 `sha256`, `scannedAt`, `scanStatus`, `blockedReason` 필드 추가 + 마이그레이션  
3) FileController 다운로드 시 요청자 검증 로직에 기안 맥락 권한 체크 추가 (작성자/결재자/참조자)  
4) 감사 퍼블리셔 연계: 업로드/다운로드/삭제 감사 outbox 기록  
5) 설정: `file.scan.enabled`, `file.scan.max-size`, `file.scan.timeout-ms`, `file.signed-url.ttl` 등 프로퍼티 추가  
