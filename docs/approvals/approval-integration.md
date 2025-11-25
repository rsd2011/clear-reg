# Approval 모듈 통합 가이드 (Draft ↔ Approval)

## 이벤트/포트 흐름
- Draft → Approval: `DraftSubmittedEvent(draftId, templateCode, orgCode, requester, summary, approvalGroupCodes)`
  - Spring ApplicationEvent 기본 발행.
  - `approval.kafka.enabled=true` 시 `DraftSubmittedEventBridge`가 Kafka로 전송(토픽 기본 `draft-submitted`).
- Approval → Draft: `ApprovalCompletedEvent(approvalRequestId, draftId, status, actedBy, comment)`
  - Kafka 리스너(`ApprovalCompletedKafkaListener`)가 수신하여 `ApprovalCompletedEventListener` → `ApprovalResultPort`(기본 구현: `ApprovalResultHandler`) 호출.
  - 동기 포트 사용 시 직접 `ApprovalResultPort` 호출 가능.

## 상태/응답
- DraftResponse 필드 추가
  - `approvalRequestId`: 생성된 결재 요청 ID
  - `approvalStatus`: Approval 모듈의 상태 스냅샷 (REQUESTED/IN_PROGRESS/APPROVED/REJECTED + 단계 정보)

### REST 계약 (요약)
- `GET /api/admin/approval/templates` (권한: Feature=APPROVAL, Action=APPROVAL_ADMIN)
- `GET /api/admin/approval/groups` (권한: Feature=APPROVAL, Action=APPROVAL_ADMIN)
- `DraftResponse`에 `approvalRequestId`, `approvalStatus` 포함됨을 API 문서/스펙에 반영

## 감사/알림
- Approval 완료 시 `ApprovalResultHandler`가 Draft 상태를 APPROVED/REJECTED로 반영하면서
  - DraftAuditPublisher를 통해 `DraftAction.APPROVED/REJECTED` 감사 이벤트 발행
  - DraftNotificationService로 `APPROVAL_RESULT` 알림 발행

## 구성
```properties
# application.yml 예시
approval.kafka.enabled=true
approval.kafka.draft-submitted-topic=draft-submitted
approval.kafka.approval-completed-topic=approval-completed
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.value.default.type=com.example.approval.api.event.ApprovalCompletedEvent
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
```

## 배포/마이그레이션 체크리스트
1) DB 스키마 적용: `docs/migrations/2025-11-25-approval-split.sql` (approval_request_id 컬럼 + approval_* 테이블).
2) 애플리케이션 설정: Kafka 토픽/버전 프로파일 별 적용, 중복 호출 방지를 위해 `approval.kafka.enabled` 한 경로만 활성화.
3) 모니터링: ApprovalCompletedEvent 수신 실패 시 재시도/데드레터 설정(Kafka DLQ 등).
4) 롤백: 토글 비활성화 후 draft-only 경로로 전환, DB 롤백 시 approval_* 테이블/컬럼 삭제.
