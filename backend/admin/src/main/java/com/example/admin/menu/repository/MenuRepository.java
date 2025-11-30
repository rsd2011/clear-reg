package com.example.admin.menu.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.admin.menu.domain.Menu;
import com.example.admin.menu.domain.MenuCode;
import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Menu 엔티티 리포지토리.
 */
public interface MenuRepository extends JpaRepository<Menu, UUID> {

    /**
     * 메뉴 코드로 조회한다.
     */
    Optional<Menu> findByCode(MenuCode code);

    /**
     * 활성화된 메뉴 코드로 조회한다.
     */
    Optional<Menu> findByCodeAndActiveTrue(MenuCode code);

    /**
     * 모든 활성화된 메뉴를 정렬 순서대로 조회한다.
     */
    List<Menu> findByActiveTrueOrderBySortOrderAsc();

    /**
     * 메뉴 코드 목록으로 메뉴를 조회한다.
     */
    List<Menu> findByCodeInAndActiveTrue(List<MenuCode> codes);

    /**
     * 특정 Capability를 요구하는 메뉴 목록을 조회한다.
     */
    @Query("SELECT DISTINCT m FROM Menu m JOIN m.requiredCapabilities c " +
           "WHERE c.feature = :feature AND c.action = :action AND m.active = true " +
           "ORDER BY m.sortOrder ASC")
    List<Menu> findByRequiredCapability(
            @Param("feature") FeatureCode feature,
            @Param("action") ActionCode action);

    /**
     * 메뉴 코드 존재 여부를 확인한다.
     */
    boolean existsByCode(MenuCode code);
}
