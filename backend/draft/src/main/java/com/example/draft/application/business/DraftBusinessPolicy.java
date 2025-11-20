package com.example.draft.application.business;

import java.util.UUID;

import com.example.draft.domain.DraftAction;
import com.example.draft.domain.DraftStatus;

/**
 * 업무 모듈에서 기안 가능 여부와 상태 후처리를 주입하기 위한 전략 훅.
 * Draft 도메인 엔티티에 직접 의존하지 않도록 최소 컨텍스트만 전달한다.
 */
public interface DraftBusinessPolicy {

    /**
     * 기안 생성 전 비즈니스 상태/권한 등을 검증한다.
     */
    void assertCreatable(String businessFeatureCode, String organizationCode, String actor);

    /**
     * 상태 변경 후 후처리(업무 상태 업데이트 등)를 수행한다.
     * @param draftId 기안 ID
     * @param businessFeatureCode 업무 코드
     * @param newStatus 변경된 상태
     * @param action 수행된 액션
     * @param actor 수행자
     */
    void afterStateChanged(UUID draftId, String businessFeatureCode, DraftStatus newStatus, DraftAction action, String actor);
}
