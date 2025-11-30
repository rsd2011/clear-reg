package com.example.admin.orggroup.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.admin.orggroup.domain.OrgGroup;
import com.example.admin.orggroup.domain.OrgGroupRolePermission;
import com.example.common.orggroup.OrgGroupRoleType;

/**
 * 조직그룹 역할-권한 매핑 리포지토리.
 */
public interface OrgGroupRolePermissionRepository extends JpaRepository<OrgGroupRolePermission, UUID> {

    /**
     * 조직그룹과 역할유형으로 매핑을 조회한다.
     *
     * @param orgGroup 조직그룹
     * @param roleType 역할 유형
     * @return 매핑 (없으면 empty)
     */
    Optional<OrgGroupRolePermission> findByOrgGroupAndRoleType(OrgGroup orgGroup, OrgGroupRoleType roleType);

    /**
     * 조직그룹 ID와 역할유형으로 매핑을 조회한다.
     *
     * @param orgGroupId 조직그룹 ID
     * @param roleType   역할 유형
     * @return 매핑 (없으면 empty)
     */
    @Query("SELECT rp FROM OrgGroupRolePermission rp WHERE rp.orgGroup.id = :orgGroupId AND rp.roleType = :roleType")
    Optional<OrgGroupRolePermission> findByOrgGroupIdAndRoleType(
            @Param("orgGroupId") UUID orgGroupId,
            @Param("roleType") OrgGroupRoleType roleType);

    /**
     * 조직그룹 코드와 역할유형으로 매핑을 조회한다.
     *
     * @param orgGroupCode 조직그룹 코드
     * @param roleType     역할 유형
     * @return 매핑 (없으면 empty)
     */
    @Query("SELECT rp FROM OrgGroupRolePermission rp WHERE rp.orgGroup.code = :orgGroupCode AND rp.roleType = :roleType")
    Optional<OrgGroupRolePermission> findByOrgGroupCodeAndRoleType(
            @Param("orgGroupCode") String orgGroupCode,
            @Param("roleType") OrgGroupRoleType roleType);

    /**
     * 조직그룹의 모든 역할-권한 매핑을 조회한다.
     *
     * @param orgGroup 조직그룹
     * @return 매핑 목록
     */
    List<OrgGroupRolePermission> findByOrgGroup(OrgGroup orgGroup);

    /**
     * 조직그룹 ID로 모든 역할-권한 매핑을 조회한다.
     *
     * @param orgGroupId 조직그룹 ID
     * @return 매핑 목록
     */
    @Query("SELECT rp FROM OrgGroupRolePermission rp WHERE rp.orgGroup.id = :orgGroupId")
    List<OrgGroupRolePermission> findByOrgGroupId(@Param("orgGroupId") UUID orgGroupId);

    /**
     * 조직그룹 코드로 모든 역할-권한 매핑을 조회한다.
     *
     * @param orgGroupCode 조직그룹 코드
     * @return 매핑 목록
     */
    @Query("SELECT rp FROM OrgGroupRolePermission rp WHERE rp.orgGroup.code = :orgGroupCode")
    List<OrgGroupRolePermission> findByOrgGroupCode(@Param("orgGroupCode") String orgGroupCode);

    /**
     * 조직그룹과 역할유형 조합이 존재하는지 확인한다.
     *
     * @param orgGroup 조직그룹
     * @param roleType 역할 유형
     * @return 존재 여부
     */
    boolean existsByOrgGroupAndRoleType(OrgGroup orgGroup, OrgGroupRoleType roleType);

    /**
     * 특정 권한그룹을 사용하는 매핑이 존재하는지 확인한다.
     *
     * @param permGroupCode 권한그룹 코드
     * @return 사용 중이면 true
     */
    boolean existsByPermGroupCode(String permGroupCode);

    /**
     * 조직그룹의 모든 역할-권한 매핑을 삭제한다.
     *
     * @param orgGroup 조직그룹
     */
    void deleteByOrgGroup(OrgGroup orgGroup);
}
