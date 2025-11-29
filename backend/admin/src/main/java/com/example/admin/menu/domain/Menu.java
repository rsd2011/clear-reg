package com.example.admin.menu.domain;

import com.example.common.jpa.PrimaryKeyEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 메뉴 엔티티.
 *
 * <p>MenuCode enum을 기준으로 메뉴를 정의하며,
 * 이름/아이콘/정렬순서 등은 DB에서 오버라이드 가능하다.</p>
 *
 * <p>메뉴의 계층 구조와 권한 그룹별 가시성은
 * {@link com.example.admin.permission.domain.PermissionMenu}에서 관리한다.</p>
 */
@Entity
@Table(name = "menus", indexes = {
    @Index(name = "idx_menu_code", columnList = "code", unique = true),
    @Index(name = "idx_menu_sort_order", columnList = "sort_order")
})
@Getter
public class Menu extends PrimaryKeyEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 100)
    private MenuCode code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 50)
    private String icon;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "menu_capabilities",
        joinColumns = @JoinColumn(name = "menu_id"),
        indexes = @Index(name = "idx_menu_cap_menu", columnList = "menu_id")
    )
    private Set<MenuCapability> requiredCapabilities = new HashSet<>();

    protected Menu() {}

    public Menu(MenuCode code, String name) {
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    // ========== Query Methods ==========

    /**
     * 경로 반환 (Enum에서 제공).
     */
    public String getPath() {
        return code.getPath();
    }

    /**
     * 표시 아이콘 반환 (DB 오버라이드 또는 Enum 기본값).
     */
    public String getDisplayIcon() {
        return icon != null ? icon : code.getDefaultIcon();
    }

    /**
     * 코드 문자열 반환.
     */
    public String getCodeValue() {
        return code.name();
    }

    // ========== Mutators ==========

    public void updateDetails(String name, String icon,
                               Integer sortOrder, String description) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.icon = icon;
        this.sortOrder = sortOrder;
        this.description = description;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void replaceCapabilities(Collection<MenuCapability> capabilities) {
        this.requiredCapabilities.clear();
        if (capabilities != null) {
            this.requiredCapabilities.addAll(capabilities);
        }
    }

    public void addCapability(MenuCapability capability) {
        this.requiredCapabilities.add(capability);
    }

    public void removeCapability(MenuCapability capability) {
        this.requiredCapabilities.remove(capability);
    }

    /**
     * 주어진 Capability가 이 메뉴에 필요한 Capability 중 하나인지 확인한다.
     */
    public boolean requiresCapability(MenuCapability capability) {
        return requiredCapabilities.contains(capability);
    }

    @Override
    public String toString() {
        return "Menu{" +
               "code=" + code +
               ", name='" + name + '\'' +
               ", active=" + active +
               '}';
    }
}
