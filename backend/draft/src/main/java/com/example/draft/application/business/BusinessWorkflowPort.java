package com.example.draft.application.business;

import java.util.UUID;

import com.example.draft.domain.DraftAction;
import com.example.draft.domain.DraftStatus;

/**
 * 업무별로 기안 가능 여부 검증 및 상태 변경 후처리를 제공하는 포트.
 * 업무 모듈이 이 인터페이스를 구현해 스프링 빈으로 등록하면 자동으로 호출된다.
 */
public interface BusinessWorkflowPort {

    /**
     * 해당 업무 코드/조직/사용자가 기안을 생성할 수 있는지 여부.
     */
    boolean canCreate(String businessFeatureCode, String organizationCode, String actor);

    /**
     * 기안 상태가 변한 후 업무 측 후처리를 수행한다.
     */
    void onStateChanged(UUID draftId, String businessFeatureCode, DraftStatus newStatus, DraftAction action, String actor);
}
