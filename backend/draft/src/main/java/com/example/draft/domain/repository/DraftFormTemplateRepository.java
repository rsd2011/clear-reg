package com.example.draft.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.common.orggroup.WorkType;
import com.example.admin.draft.domain.DraftFormTemplate;
import com.example.admin.draft.domain.DraftFormTemplateRoot;

/**
 * 기안 양식 템플릿 리포지토리.
 */
public interface DraftFormTemplateRepository extends JpaRepository<DraftFormTemplate, UUID> {

    /**
     * 루트와 버전으로 템플릿을 조회한다.
     *
     * @param root    루트
     * @param version 버전
     * @return 템플릿 (없으면 empty)
     */
    Optional<DraftFormTemplate> findByRootAndVersion(DraftFormTemplateRoot root, Integer version);

    /**
     * 루트의 최대 버전 번호를 조회한다.
     *
     * @param root 루트
     * @return 최대 버전 번호 (없으면 null)
     */
    @Query("SELECT MAX(t.version) FROM DraftFormTemplate t WHERE t.root = :root")
    Integer findMaxVersionByRoot(@Param("root") DraftFormTemplateRoot root);

    /**
     * 업무유형으로 현재 활성화된 템플릿 목록을 조회한다.
     *
     * @param workType 업무유형
     * @return 활성화된 템플릿 목록
     */
    @Query("SELECT t FROM DraftFormTemplate t " +
            "WHERE t.workType = :workType " +
            "AND t.active = true " +
            "AND t.status = 'PUBLISHED' " +
            "AND t.validTo IS NULL")
    List<DraftFormTemplate> findCurrentByWorkType(@Param("workType") WorkType workType);

    /**
     * 모든 현재 활성화된 템플릿 목록을 조회한다.
     *
     * @return 활성화된 템플릿 목록
     */
    @Query("SELECT t FROM DraftFormTemplate t " +
            "WHERE t.active = true " +
            "AND t.status = 'PUBLISHED' " +
            "AND t.validTo IS NULL")
    List<DraftFormTemplate> findAllCurrent();

    /**
     * ID로 현재 활성화된 템플릿을 조회한다.
     *
     * @param id 템플릿 ID
     * @return 활성화된 템플릿 (없으면 empty)
     */
    @Query("SELECT t FROM DraftFormTemplate t " +
            "WHERE t.id = :id " +
            "AND t.active = true " +
            "AND t.status = 'PUBLISHED' " +
            "AND t.validTo IS NULL")
    Optional<DraftFormTemplate> findByIdAndActiveTrue(@Param("id") UUID id);
}
