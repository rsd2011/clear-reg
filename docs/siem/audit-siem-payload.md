# 감사 로그 SIEM 연동 샘플 페이로드

## 공통 전송 원칙
- 전송 채널: TLS + (선택) 서명. 필드는 마스킹/토큰화 후 전송.
- 규격 예시: OTLP JSON 또는 syslog(JSON) 사용.
- 식별자: 주민번호/계좌/카드번호 등은 마스킹/토큰, hash_chain 포함.

## 샘플 OTLP JSON
```json
{
  "time": "2025-11-23T03:15:00Z",
  "severity": "INFO",
  "resource": {
    "service.name": "audit-service",
    "service.namespace": "clear-reg"
  },
  "attributes": {
    "event.type": "PERSONAL_DATA_ACCESS",
    "module": "server",
    "action": "ACCOUNT_VIEW",
    "actor.id": "emp12345",
    "actor.role": "CUSTOMER_SERVICE",
    "actor.dept": "CC",
    "subject.type": "CUSTOMER",
    "subject.key": "**********3456",
    "channel": "INTERNAL",
    "client.ip": "10.0.0.12",
    "user.agent": "Chrome",
    "reason.code": "CS_SUPPORT",
    "reason.text": "고객 문의 확인",
    "legal.basis": "PIPA_ART15",
    "risk.level": "MEDIUM",
    "result.code": "OK",
    "before.summary": "masked",
    "after.summary": "masked",
    "hash.chain": "abc123...",
    "audit.mode": "ASYNC_FALLBACK"
  }
}
```

## 샘플 syslog(JSON) 라인
```
<134>1 2025-11-23T03:15:00Z audit-service - - - {"event_type":"AUDIT_ACCESS","module":"server","action":"AUDIT_LOG_VIEW","actor_id":"audit_admin","subject_type":"AUDIT_LOG","subject_key":"page=1,size=50","result_code":"OK","hash_chain":"abc123..."}
```

## 필드 매핑 표 (문서 설계와 일치)
- event_time → time / @timestamp  
- event_type/module/action → attributes.event.type/module/action  
- actor_id/type/role/dept → actor.*  
- subject_type/key → subject.* (마스킹/토큰)  
- client_ip/user_agent/device_id/channel → client.* / device.*  
- reason_code/reason_text/legal_basis_code → reason.* / legal.*  
- risk_level → risk.level  
- before_summary/after_summary → before.summary / after.summary (민감 필드 제거 후)  
- hash_chain → hash.chain  
- audit_mode → audit.mode  

## 추가 보안 옵션
- 전송 전 필드 화이트리스트 적용.
- SIEM 수신 단에서 마스킹 해제 금지, 내부 규칙 기반 조회 권한 분리.
- 재전송/중복 방지용 event_id(ULID) 포함 권장.

## 전송·암호화 체크리스트 (미구현)
- [ ] OTLP/HTTP 전송 시 mTLS 적용
- [ ] syslog 전송 시 TLS + 클라이언트 인증
- [ ] 페이로드 서명(HMAC/EDS) 옵션
- [ ] 필드 화이트리스트/필터 적용
