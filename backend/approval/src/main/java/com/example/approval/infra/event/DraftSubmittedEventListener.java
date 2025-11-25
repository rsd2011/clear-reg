package com.example.approval.infra.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.approval.api.ApprovalFacade;
import com.example.approval.api.ApprovalRequestCommand;
import com.example.approval.api.event.DraftSubmittedEvent;

/**
 * 이벤트 기반 통합 시 DraftSubmittedEvent를 수신해 Approval 요청을 생성하는 어댑터.
 * 동기 호출이 이미 실행된 환경에서는 중복 호출되지 않도록 외부에서 토글/프로필로 관리한다.
 */
@Component
public class DraftSubmittedEventListener {

    private final ApprovalFacade approvalFacade;

    public DraftSubmittedEventListener(ApprovalFacade approvalFacade) {
        this.approvalFacade = approvalFacade;
    }

    @EventListener
    public void handle(DraftSubmittedEvent event) {
        approvalFacade.requestApproval(new ApprovalRequestCommand(
                event.draftId(),
                event.templateCode(),
                event.organizationCode(),
                event.requester(),
                event.summary(),
                event.approvalGroupCodes()
        ));
    }
}
