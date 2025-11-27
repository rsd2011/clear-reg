package com.example.admin.menu;

import com.example.common.jpa.PrimaryKeyEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 메뉴 엔티티.
 *
 * <p>메뉴는 UI 네비게이션 구조를 표현하며, 하나 이상의 Capability를 참조한다.
 * 사용자가 해당 Capability 중 하나라도 보유하면 메뉴가 표시된다.</p>
 *
 * <p>메뉴 가시성은 다음 순서로 평가된다:</p>
 * <ol>
 *   <li>(필수) 사용자의 Capability 보유 여부</li>
 *   <li>(선택) MenuViewConfig에 의한 역할/조직별 가시성 설정</li>
 * </ol>
 */
@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Entity returns unmodifiable views; JPA proxies handle collection serialization")
@Entity
@Table(name = "menus", indexes = {
    @Index(name = "idx_menu_code", columnList = "code", unique = true),
    @Index(name = "idx_menu_parent", columnList = "parent_id"),
    @Index(name = "idx_menu_sort_order", columnList = "sort_order")
})
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Menu parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<Menu> children = new ArrayList<>();

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

    // Getters

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getIcon() {
        return icon;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public Menu getParent() {
        return parent;
    }

    public String getParentCode() {
        return parent != null ? parent.getCode() : null;
    }

    public List<Menu> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public Set<MenuCapability> getRequiredCapabilities() {
        return Collections.unmodifiableSet(requiredCapabilities);
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

    public void setParent(Menu parent) {
        this.parent = parent;
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

    /**
     * 메뉴의 전체 경로 (루트부터 현재 메뉴까지의 코드 목록)를 반환한다.
     */
    public List<String> getFullPath() {
        List<String> path = new ArrayList<>();
        Menu current = this;
        while (current != null) {
            path.add(0, current.getCode());
            current = current.getParent();
        }
        return path;
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
