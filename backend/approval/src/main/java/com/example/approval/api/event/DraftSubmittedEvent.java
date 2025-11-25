package com.example.approval.api.event;

import java.util.List;
import java.util.UUID;

/**
 * Draft 모듈이 발행하는 결재 요청 트리거 이벤트 계약. 메시징/동기 호출 모두 이 payload를 사용할 수 있다.
 */
public record DraftSubmittedEvent(
        UUID draftId,
        String templateCode,
        String organizationCode,
        String requester,
        String summary,
        List<String> approvalGroupCodes
) {
}
