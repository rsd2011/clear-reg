# SIEM 전송 파이프라인 구현 가이드 (초안)

## 목표
- 감사 이벤트를 OTLP/syslog(JSON)로 SIEM에 전달하면서 mTLS·서명·필드 화이트리스트를 적용한다.
- 개인정보/신용정보는 마스킹/토큰화 상태로만 전송한다.

## 구성 요소
- Exporter: Spring Boot `MeterRegistry`/`Observation` 훅 또는 AuditEvent 리스너에서 OTLP JSON 생성.
- 전송 채널
  - OTLP/HTTP: mTLS 적용, `Authorization: Bearer` 또는 HMAC 서명 헤더 추가.
  - syslog over TLS: client cert + allowlist IP.
- 필드 화이트리스트: `event_time,event_type,module,action,actor.*,subject.*,client.*,reason.*,risk_level,hash_chain,audit_mode` 등 최소 필드만 전송.

## 샘플 설정 (application.yml)
```yaml
siem:
  enabled: true
  mode: otlp
  endpoint: https://siem.example.com/v1/otlp
  mtls:
    key-store: classpath:siem-client.p12
    key-store-password: changeit
    trust-store: classpath:siem-trust.jks
    trust-store-password: changeit
  hmac:
    secret: ${SIEM_HMAC_SECRET}
  whitelist:
    - event_time
    - event_type
    - module
    - action
    - actor_id
    - actor_role
    - subject_type
    - subject_key
    - client_ip
    - user_agent
    - channel
    - reason_code
    - reason_text
    - legal_basis_code
    - risk_level
    - hash_chain
    - audit_mode
```

## 전송 흐름
1) AuditEvent 생성 시 Exporter로 복사 → 화이트리스트 필터 → 마스킹/토큰화 확인.
2) 페이로드에 event_id/trace_id 추가, hash_chain 포함.
3) mTLS Handshake 후 HMAC 헤더 추가하여 POST (또는 syslog TLS 전송).
4) 실패 시 지수 백오프 + Slack/Webhook 알림.

## 보안 체크리스트
- [ ] mTLS 키·신뢰 저장소 주기적 로테이션
- [ ] HMAC 시계 허용 오차 검증(리플레이 방지)
- [ ] 페이로드 서명/압축 옵션
- [ ] 전송 실패 DLQ 저장 및 재전송 배치
