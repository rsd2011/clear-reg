package com.example.draft.application.port.out;

import java.util.UUID;

import com.example.approval.api.ApprovalStatus;

/**
 * Approval 모듈이 결재 완료/반려 결과를 전달할 때 Draft 쪽이 구현할 포트.
 * 비동기 이벤트 리스너나 동기 호출 어댑터에서 사용 가능.
 */
public interface ApprovalResultPort {

    void onApprovalCompleted(UUID draftId, UUID approvalRequestId, ApprovalStatus status, String actor, String comment);
}
