package com.example.draft.application.business;

import org.springframework.stereotype.Component;

import com.example.draft.domain.DraftAction;
import com.example.draft.domain.DraftStatus;

/**
 * 기본 정책: 별도 제약/후처리 없음.
 */
@Component
public class NoOpDraftBusinessPolicy implements DraftBusinessPolicy {
    @Override
    public void assertCreatable(String businessFeatureCode, String organizationCode, String actor) { }

    @Override
    public void afterStateChanged(java.util.UUID draftId, String businessFeatureCode, DraftStatus newStatus, DraftAction action, String actor) { }
}
