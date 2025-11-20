package com.example.draft.application.business;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.example.draft.domain.DraftAction;
import com.example.draft.domain.DraftStatus;
import com.example.draft.domain.exception.DraftAccessDeniedException;

@Component
@Primary
public class DelegatingDraftBusinessPolicy implements DraftBusinessPolicy {

    private static final Logger log = LoggerFactory.getLogger(DelegatingDraftBusinessPolicy.class);

    private final List<BusinessWorkflowPort> delegates;

    public DelegatingDraftBusinessPolicy(List<BusinessWorkflowPort> delegates) {
        this.delegates = delegates;
    }

    @Override
    public void assertCreatable(String businessFeatureCode, String organizationCode, String actor) {
        boolean allowed = delegates.stream().allMatch(delegate -> delegate.canCreate(businessFeatureCode, organizationCode, actor));
        if (!allowed) {
            throw new DraftAccessDeniedException("해당 업무의 기안을 생성할 수 없습니다.");
        }
    }

    @Override
    public void afterStateChanged(UUID draftId, String businessFeatureCode, DraftStatus newStatus, DraftAction action, String actor) {
        delegates.forEach(delegate -> {
            try {
                delegate.onStateChanged(draftId, businessFeatureCode, newStatus, action, actor);
            } catch (Exception ex) {
                log.warn("Business workflow hook failed action={} business={} draftId={} error={}", action, businessFeatureCode, draftId, ex.getMessage());
            }
        });
    }
}
