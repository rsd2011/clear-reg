package com.example.admin.menu.domain;

import com.example.common.jpa.PrimaryKeyEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
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
 * <p>메뉴는 UI 네비게이션의 개별 항목을 표현하며, 하나 이상의 Capability를 참조한다.
 * 사용자가 해당 Capability 중 하나라도 보유하면 메뉴에 접근 가능하다.</p>
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

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String path;

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

    public Menu(String code, String name) {
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    // Setters / Mutators

    public void updateDetails(String name, String path, String icon,
                               Integer sortOrder, String description) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.path = path;
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
               "code='" + code + '\'' +
               ", name='" + name + '\'' +
               ", path='" + path + '\'' +
               ", active=" + active +
               '}';
    }
}
