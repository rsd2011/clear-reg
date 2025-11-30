package com.example.admin.draft.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.common.orggroup.WorkType;
import com.example.admin.draft.domain.DraftFormTemplateRoot;

/**
 * 기안 양식 템플릿 루트 리포지토리.
 */
public interface DraftFormTemplateRootRepository extends JpaRepository<DraftFormTemplateRoot, UUID> {

    /**
     * 템플릿 코드로 루트를 조회한다.
     *
     * @param templateCode 템플릿 코드
     * @return 루트 (없으면 empty)
     */
    Optional<DraftFormTemplateRoot> findByTemplateCode(String templateCode);

    /**
     * 활성화된 현재 버전이 있는 루트 목록을 조회한다.
     *
     * @return 활성화된 루트 목록
     */
    @Query("SELECT r FROM DraftFormTemplateRoot r " +
            "JOIN FETCH r.currentVersion v " +
            "WHERE v.active = true AND v.status = 'PUBLISHED'")
    List<DraftFormTemplateRoot> findAllActive();

    /**
     * 업무유형으로 활성화된 루트 목록을 조회한다.
     *
     * @param workType 업무유형
     * @return 해당 업무유형의 활성화된 루트 목록
     */
    @Query("SELECT r FROM DraftFormTemplateRoot r " +
            "JOIN FETCH r.currentVersion v " +
            "WHERE v.workType = :workType AND v.active = true AND v.status = 'PUBLISHED'")
    List<DraftFormTemplateRoot> findByWorkTypeAndActive(@Param("workType") WorkType workType);
}
