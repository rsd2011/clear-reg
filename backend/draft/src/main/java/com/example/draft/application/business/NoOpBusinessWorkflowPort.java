package com.example.draft.application.business;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.draft.domain.DraftAction;
import com.example.draft.domain.DraftStatus;

@Component
public class NoOpBusinessWorkflowPort implements BusinessWorkflowPort {
    @Override
    public boolean canCreate(String businessFeatureCode, String organizationCode, String actor) {
        return true;
    }

    @Override
    public void onStateChanged(UUID draftId, String businessFeatureCode, DraftStatus newStatus, DraftAction action, String actor) {
        // no-op
    }
}
