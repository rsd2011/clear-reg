package com.example.admin.menu;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * MenuViewConfig 엔티티 리포지토리.
 */
public interface MenuViewConfigRepository extends JpaRepository<MenuViewConfig, UUID> {

    /**
     * 특정 메뉴의 모든 활성화된 가시성 설정을 우선순위 순으로 조회한다.
     */
    List<MenuViewConfig> findByMenu_CodeAndActiveTrueOrderByPriorityAsc(String menuCode);

    /**
     * 특정 메뉴의 모든 가시성 설정을 조회한다.
     */
    List<MenuViewConfig> findByMenu_Code(String menuCode);

    /**
     * 특정 권한 그룹에 대한 모든 활성화된 가시성 설정을 조회한다.
     */
    List<MenuViewConfig> findByPermissionGroupCodeAndActiveTrueOrderByPriorityAsc(
            String permissionGroupCode);

    /**
     * 특정 조직 정책에 대한 모든 활성화된 가시성 설정을 조회한다.
     */
    List<MenuViewConfig> findByOrgPolicyIdAndActiveTrueOrderByPriorityAsc(Long orgPolicyId);

    /**
     * 글로벌 가시성 설정을 조회한다.
     */
    @Query("SELECT c FROM MenuViewConfig c WHERE c.targetType = 'GLOBAL' AND c.active = true " +
           "ORDER BY c.priority ASC")
    List<MenuViewConfig> findGlobalConfigs();

    /**
     * 특정 메뉴에 대해 사용자에게 적용될 수 있는 모든 가시성 설정을 조회한다.
     *
     * @param menuCode 메뉴 코드
     * @param permGroupCode 사용자의 권한 그룹 코드
     * @param orgPolicyId 사용자의 조직 정책 ID
     * @return 적용 가능한 가시성 설정 목록 (우선순위 순)
     */
    @Query("SELECT c FROM MenuViewConfig c WHERE c.menu.code = :menuCode AND c.active = true " +
           "AND (c.targetType = 'GLOBAL' " +
           "     OR (c.targetType = 'PERMISSION_GROUP' AND c.permissionGroupCode = :permGroupCode) " +
           "     OR (c.targetType = 'ORG_POLICY' AND c.orgPolicyId = :orgPolicyId)) " +
           "ORDER BY c.priority ASC")
    List<MenuViewConfig> findApplicableConfigs(
            @Param("menuCode") String menuCode,
            @Param("permGroupCode") String permGroupCode,
            @Param("orgPolicyId") Long orgPolicyId);

    /**
     * 여러 메뉴에 대해 사용자에게 적용될 수 있는 모든 가시성 설정을 조회한다.
     */
    @Query("SELECT c FROM MenuViewConfig c WHERE c.menu.code IN :menuCodes AND c.active = true " +
           "AND (c.targetType = 'GLOBAL' " +
           "     OR (c.targetType = 'PERMISSION_GROUP' AND c.permissionGroupCode = :permGroupCode) " +
           "     OR (c.targetType = 'ORG_POLICY' AND c.orgPolicyId = :orgPolicyId)) " +
           "ORDER BY c.menu.code, c.priority ASC")
    List<MenuViewConfig> findApplicableConfigsForMenus(
            @Param("menuCodes") List<String> menuCodes,
            @Param("permGroupCode") String permGroupCode,
            @Param("orgPolicyId") Long orgPolicyId);

    /**
     * 특정 대상 유형의 모든 활성화된 설정을 조회한다.
     */
    List<MenuViewConfig> findByTargetTypeAndActiveTrueOrderByPriorityAsc(
            MenuViewConfig.TargetType targetType);

    /**
     * 메뉴 ID로 모든 가시성 설정을 삭제한다.
     */
    void deleteByMenuId(UUID menuId);
}
