package com.example.draft.application.business;

import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftAction;

public interface DraftBusinessPolicy {

    /**
     * 기안 생성 전 비즈니스 상태/권한 등을 검증한다.
     */
    void assertCreatable(String businessFeatureCode, String organizationCode, String actor);

    /**
     * 상태 변경 후 후처리(업무 상태 업데이트 등)를 수행한다.
     */
    void afterStateChanged(Draft draft, DraftAction action);
}
