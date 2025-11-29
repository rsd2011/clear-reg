package com.example.admin.permission.repository;

import com.example.admin.permission.domain.PermissionMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PermissionMenu 엔티티 리포지토리.
 */
public interface PermissionMenuRepository extends JpaRepository<PermissionMenu, UUID> {

    /**
     * 권한 그룹의 모든 메뉴/카테고리를 표시 순서대로 조회한다.
     */
    @Query("SELECT pm FROM PermissionMenu pm " +
           "LEFT JOIN FETCH pm.menu " +
           "LEFT JOIN FETCH pm.parent " +
           "WHERE pm.permissionGroupCode = :permGroupCode " +
           "ORDER BY pm.displayOrder ASC NULLS LAST")
    List<PermissionMenu> findByPermissionGroupCode(
            @Param("permGroupCode") String permGroupCode);

    /**
     * 권한 그룹의 루트 레벨 메뉴/카테고리만 조회한다.
     */
    @Query("SELECT pm FROM PermissionMenu pm " +
           "LEFT JOIN FETCH pm.menu " +
           "WHERE pm.permissionGroupCode = :permGroupCode " +
           "AND pm.parent IS NULL " +
           "ORDER BY pm.displayOrder ASC NULLS LAST")
    List<PermissionMenu> findRootsByPermissionGroupCode(
            @Param("permGroupCode") String permGroupCode);

    /**
     * 특정 부모 아래의 자식 메뉴/카테고리를 조회한다.
     */
    @Query("SELECT pm FROM PermissionMenu pm " +
           "LEFT JOIN FETCH pm.menu " +
           "WHERE pm.parent.id = :parentId " +
           "ORDER BY pm.displayOrder ASC NULLS LAST")
    List<PermissionMenu> findByParentId(@Param("parentId") UUID parentId);

    /**
     * 권한 그룹에서 특정 메뉴를 찾는다.
     */
    @Query("SELECT pm FROM PermissionMenu pm " +
           "WHERE pm.permissionGroupCode = :permGroupCode " +
           "AND pm.menu.code = :menuCode")
    Optional<PermissionMenu> findByPermissionGroupCodeAndMenuCode(
            @Param("permGroupCode") String permGroupCode,
            @Param("menuCode") String menuCode);

    /**
     * 권한 그룹에서 특정 카테고리를 찾는다.
     */
    @Query("SELECT pm FROM PermissionMenu pm " +
           "WHERE pm.permissionGroupCode = :permGroupCode " +
           "AND pm.categoryCode = :categoryCode")
    Optional<PermissionMenu> findByPermissionGroupCodeAndCategoryCode(
            @Param("permGroupCode") String permGroupCode,
            @Param("categoryCode") String categoryCode);

    /**
     * 권한 그룹의 모든 메뉴/카테고리를 삭제한다.
     */
    @Modifying
    @Query("DELETE FROM PermissionMenu pm WHERE pm.permissionGroupCode = :permGroupCode")
    void deleteByPermissionGroupCode(@Param("permGroupCode") String permGroupCode);

    /**
     * 특정 메뉴를 참조하는 모든 PermissionMenu를 조회한다.
     */
    @Query("SELECT pm FROM PermissionMenu pm WHERE pm.menu.id = :menuId")
    List<PermissionMenu> findByMenuId(@Param("menuId") UUID menuId);

    /**
     * 특정 메뉴를 참조하는 모든 PermissionMenu를 삭제한다.
     */
    @Modifying
    @Query("DELETE FROM PermissionMenu pm WHERE pm.menu.id = :menuId")
    void deleteByMenuId(@Param("menuId") UUID menuId);

    /**
     * 권한 그룹에 해당 메뉴가 존재하는지 확인한다.
     */
    @Query("SELECT COUNT(pm) > 0 FROM PermissionMenu pm " +
           "WHERE pm.permissionGroupCode = :permGroupCode " +
           "AND pm.menu.code = :menuCode")
    boolean existsByPermissionGroupCodeAndMenuCode(
            @Param("permGroupCode") String permGroupCode,
            @Param("menuCode") String menuCode);

    /**
     * 권한 그룹에 해당 카테고리가 존재하는지 확인한다.
     */
    @Query("SELECT COUNT(pm) > 0 FROM PermissionMenu pm " +
           "WHERE pm.permissionGroupCode = :permGroupCode " +
           "AND pm.categoryCode = :categoryCode")
    boolean existsByPermissionGroupCodeAndCategoryCode(
            @Param("permGroupCode") String permGroupCode,
            @Param("categoryCode") String categoryCode);
}
