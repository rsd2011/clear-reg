package com.example.admin.orggroup.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.admin.orggroup.domain.OrgGroup;
import com.example.admin.orggroup.domain.OrgGroupApprovalMapping;
import com.example.common.orggroup.WorkType;

/**
 * 조직그룹 승인선 매핑 리포지토리.
 */
public interface OrgGroupApprovalMappingRepository extends JpaRepository<OrgGroupApprovalMapping, UUID> {

    /**
     * 조직그룹과 업무유형으로 매핑을 조회한다.
     *
     * @param orgGroup 조직그룹
     * @param workType 업무유형 (nullable)
     * @return 매핑 (없으면 empty)
     */
    Optional<OrgGroupApprovalMapping> findByOrgGroupAndWorkType(OrgGroup orgGroup, WorkType workType);

    /**
     * 조직그룹 ID와 업무유형으로 매핑을 조회한다.
     *
     * @param orgGroupId 조직그룹 ID
     * @param workType   업무유형 (nullable)
     * @return 매핑 (없으면 empty)
     */
    @Query("SELECT m FROM OrgGroupApprovalMapping m " +
           "WHERE m.orgGroup.id = :orgGroupId AND " +
           "((:workType IS NULL AND m.workType IS NULL) OR m.workType = :workType)")
    Optional<OrgGroupApprovalMapping> findByOrgGroupIdAndWorkType(
            @Param("orgGroupId") UUID orgGroupId,
            @Param("workType") WorkType workType);

    /**
     * 조직그룹의 기본 템플릿 매핑을 조회한다.
     *
     * @param orgGroup 조직그룹
     * @return 기본 매핑 (없으면 empty)
     */
    @Query("SELECT m FROM OrgGroupApprovalMapping m " +
           "WHERE m.orgGroup = :orgGroup AND m.workType IS NULL")
    Optional<OrgGroupApprovalMapping> findDefaultByOrgGroup(@Param("orgGroup") OrgGroup orgGroup);

    /**
     * 조직그룹 ID의 기본 템플릿 매핑을 조회한다.
     *
     * @param orgGroupId 조직그룹 ID
     * @return 기본 매핑 (없으면 empty)
     */
    @Query("SELECT m FROM OrgGroupApprovalMapping m " +
           "WHERE m.orgGroup.id = :orgGroupId AND m.workType IS NULL")
    Optional<OrgGroupApprovalMapping> findDefaultByOrgGroupId(@Param("orgGroupId") UUID orgGroupId);

    /**
     * 조직그룹의 모든 매핑을 조회한다.
     *
     * @param orgGroup 조직그룹
     * @return 매핑 목록
     */
    List<OrgGroupApprovalMapping> findByOrgGroup(OrgGroup orgGroup);

    /**
     * 조직그룹 ID의 모든 매핑을 조회한다.
     *
     * @param orgGroupId 조직그룹 ID
     * @return 매핑 목록
     */
    @Query("SELECT m FROM OrgGroupApprovalMapping m " +
           "LEFT JOIN FETCH m.approvalTemplateRoot " +
           "WHERE m.orgGroup.id = :orgGroupId " +
           "ORDER BY m.workType NULLS FIRST")
    List<OrgGroupApprovalMapping> findByOrgGroupIdWithTemplate(@Param("orgGroupId") UUID orgGroupId);

    /**
     * 조직그룹 코드로 모든 매핑을 조회한다.
     *
     * @param orgGroupCode 조직그룹 코드
     * @return 매핑 목록
     */
    @Query("SELECT m FROM OrgGroupApprovalMapping m " +
           "LEFT JOIN FETCH m.approvalTemplateRoot " +
           "WHERE m.orgGroup.code = :orgGroupCode " +
           "ORDER BY m.workType NULLS FIRST")
    List<OrgGroupApprovalMapping> findByOrgGroupCode(@Param("orgGroupCode") String orgGroupCode);

    /**
     * 조직그룹 코드와 업무유형으로 매핑을 조회한다.
     *
     * @param orgGroupCode 조직그룹 코드
     * @param workType     업무유형 (nullable)
     * @return 매핑 (없으면 empty)
     */
    @Query("SELECT m FROM OrgGroupApprovalMapping m " +
           "LEFT JOIN FETCH m.approvalTemplateRoot " +
           "WHERE m.orgGroup.code = :orgGroupCode AND " +
           "((:workType IS NULL AND m.workType IS NULL) OR m.workType = :workType)")
    Optional<OrgGroupApprovalMapping> findByOrgGroupCodeAndWorkType(
            @Param("orgGroupCode") String orgGroupCode,
            @Param("workType") WorkType workType);

    /**
     * 특정 템플릿을 사용하는 매핑이 존재하는지 확인한다.
     *
     * @param templateRootId 템플릿 루트 ID
     * @return 사용 중이면 true
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END " +
           "FROM OrgGroupApprovalMapping m " +
           "WHERE m.approvalTemplateRoot.id = :templateRootId")
    boolean existsByApprovalTemplateRootId(@Param("templateRootId") UUID templateRootId);

    /**
     * 조직그룹과 업무유형 조합의 매핑 존재 여부를 확인한다.
     *
     * @param orgGroup 조직그룹
     * @param workType 업무유형
     * @return 존재하면 true
     */
    boolean existsByOrgGroupAndWorkType(OrgGroup orgGroup, WorkType workType);

    /**
     * 조직그룹의 모든 매핑을 삭제한다.
     *
     * @param orgGroup 조직그룹
     */
    void deleteByOrgGroup(OrgGroup orgGroup);
}
