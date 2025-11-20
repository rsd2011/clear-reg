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

## 진행 현황 (T17)
- 완료: AV 스캔 훅 추가(`FileScanner` 포트, 기본 NoOp/Disabled 스캐너), 파일 메타 필드(`sha256`, `scanStatus`, `scannedAt`, `blockedReason`) 및 마이그레이션 적용, 파일 감사 퍼블리셔 훅(`FileAuditPublisher`, NoOp 기본) 추가
- 진행 예정/진행 중:
  1) (부분) FileController 다운로드에 `draftId` 컨텍스트 추가, 기안에 첨부된 파일인지 검증 후 비소유자도 다운로드 가능하게 허용. 결재자/참조자 권한 체크는 Draft 서비스가 수행.  
  2) 업로드/다운로드/삭제 감사 이벤트를 outbox/Kafka/SIEM으로 퍼블리시  
     - 현황: NoOp 기본, `file.audit.publisher=log|kafka|outbox|siem` 선택 가능, outbox 테이블(`docs/migrations/2025-11-20-file-audit-outbox.sql`) + 스케줄 릴레이(`FileAuditOutboxRelay`) 동작  
     - TODO: SIEM 전송 실제 어댑터 교체 및 브로커 소비자 통합 테스트  
  3) 설정 프로퍼티 적용: `file.security.scan-enabled`, `file.security.max-size-bytes`, `file.security.scan-timeout-ms`, `file.security.signed-url-ttl-seconds` (`FileSecurityProperties`) → 스캔 토글, 최대 크기, 타임아웃을 FileService에 반영 완료  
  4) 스캔 실패 재시도/큐 정책 및 보존 TTL 문서화
