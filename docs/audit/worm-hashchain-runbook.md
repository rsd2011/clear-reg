# Audit WORM & Hash-Chain 운영 가이드

## 1. 목적
- 감사 로그 위·변조 방지 강화를 위해 hash chain + HMAC을 운영 환경에서 적용/로테이션하는 절차를 정의한다.
- 필요 시 WORM(Storage) 옵션을 병행하여 규제 요구(접속기록 보존) 충족을 지원한다.

## 2. 현행 코드 스위치
- 프로퍼티
  - `audit.hash-chain.hmac-enabled` : true 설정 시 HMAC-SHA256 사용
  - `audit.hash-chain.secret` : HMAC 키(운영 KMS/HSM에서 주입)
  - `audit.hash-chain.key-id` : 키 식별자(예: `2025Q1`)
- Kafka 미사용 시 `audit.kafka.bootstrap-servers` 비우면 발행 스킵, DB 저장만 수행.

## 3. 운영 키 관리 & 로테이션 정책
1) 키 저장: KMS/HSM에 HMAC 키 생성, 애플리케이션에는 단기 토큰(예: Vault agent, AWS Secrets Manager)로 주입.
2) 키 식별자: 분기/월 단위(`2025Q1`, `2025Q2` 등)로 설정.
3) 로테이션 절차
   - (D-1) 새 키 생성 → `audit.hash-chain.secret` 교체, `key-id` 갱신 → 재기동/롤링.
   - (D) 교체 시점 이후 생성된 로그는 새 키로 서명.
   - (D+1) 무결성 검증 배치로 이전 구간 체인 검증 후 결과 보관.
   - 이전 키는 검증 목적으로 KMS에 보존(규제 보존기간 동안 삭제 금지).
4) 비밀 주입 실패 대비: 기본값은 이전 키로 지속 사용 + 알림.

## 4. Hash-chain 무결성 검증 배치(제안)
- 입력: 기간(start/end) 또는 최근 N건.
- 절차: DB에서 event_time 오름차순 조회 → hash_chain 재계산 → mismatch 발생 시 알림/차단.
- 빈번도: 일 1회 + 로테이션 직후 1회.
- 알림: Slack/Email/SIEM 이벤트.

## 5. WORM 스토리지 옵션
- S3 Object Lock (Compliance 모드, 보존기간=정책 보존일수) 또는 DB Append-only 파티션 + 주기적 스냅샷.
- 중요 필드만 저장(이미 요약/마스킹 적용) → 원본 민감정보 저장 금지.
- WORM 적용 시 삭제/수정 권한은 폐지하고 만료 후 Lifecycle로 파기.

## 6. 운용 체크리스트
- [ ] `audit.hash-chain.hmac-enabled=true` 설정
- [ ] `audit.hash-chain.secret`를 KMS/HSM에서 주입
- [ ] `audit.hash-chain.key-id`를 현재 키 식별자로 설정
- [ ] WORM 대상 버킷/테이블 보존기간을 정책에 맞게 설정
- [ ] 무결성 검증 배치(일 1회) 구성 및 알림 연동
- [ ] 키 로테이션 Runbook 문서화 및 주기 예약
- [ ] Kafka 사용 시 DLQ(`audit.events.dlq`) 존재/리텐션 확인

## 7. 실패 시 대응
- HMAC 검증 실패: 즉시 알림 → 원인 조사(시간 불일치, 키 미주입, 로그 변조) → 재검증.
- hash_chain 계산 실패 로그 지속 시: `audit.hash-chain.hmac-enabled` 임시 false 전환 + 근본 원인 해결 후 재활성.

## 8. 참고
- 코드 경로: `backend/audit/infra/AuditRecordService` (hash chain/HMAC), 프로퍼티는 `application.yml`(`audit.hash-chain.*`).
