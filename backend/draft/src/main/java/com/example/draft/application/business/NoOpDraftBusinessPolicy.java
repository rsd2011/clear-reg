package com.example.draft.application.business;

import org.springframework.stereotype.Component;

import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftAction;

/**
 * 기본 정책: 별도 제약/후처리 없음.
 */
@Component
public class NoOpDraftBusinessPolicy implements DraftBusinessPolicy {
    @Override
    public void assertCreatable(String businessFeatureCode, String organizationCode, String actor) {
        // no-op
    }

    @Override
    public void afterStateChanged(Draft draft, DraftAction action) {
        // no-op
    }
}
