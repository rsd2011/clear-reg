package com.example.admin.menu;

import com.example.common.jpa.PrimaryKeyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

/**
 * 메뉴 가시성 설정 엔티티.
 *
 * <p>메뉴의 기본 Capability 기반 접근 제어 외에,
 * 역할(PermissionGroup) 또는 조직(OrgPolicy)별로 추가적인 가시성을 설정한다.</p>
 *
 * <p>가시성 평가 순서:</p>
 * <ol>
 *   <li>사용자의 Capability 보유 여부 (필수 조건)</li>
 *   <li>MenuViewConfig에 의한 역할/조직별 가시성 (선택적 추가 조건)</li>
 * </ol>
 *
 * <p>MenuViewConfig가 없으면 Capability만으로 가시성이 결정된다.
 * MenuViewConfig가 있으면 해당 설정을 추가로 적용한다.</p>
 */
@Entity
@Table(name = "menu_view_configs", indexes = {
    @Index(name = "idx_mvc_menu", columnList = "menu_id"),
    @Index(name = "idx_mvc_perm_group", columnList = "permission_group_code"),
    @Index(name = "idx_mvc_org_policy", columnList = "org_policy_id"),
    @Index(name = "idx_mvc_target_type", columnList = "target_type")
})
public class MenuViewConfig extends PrimaryKeyEntity {

    /**
     * 가시성 설정 대상 유형.
     */
    public enum TargetType {
        /** 특정 권한 그룹(역할)에 대한 설정 */
        PERMISSION_GROUP,
        /** 특정 조직 정책에 대한 설정 */
        ORG_POLICY,
        /** 모든 사용자에 대한 글로벌 설정 */
        GLOBAL
    }

    /**
     * 가시성 동작 유형.
     */
    public enum VisibilityAction {
        /** 메뉴 표시 (기본 Capability 체크 통과 후) */
        SHOW,
        /** 메뉴 숨김 (Capability가 있어도 숨김) */
        HIDE,
        /** 메뉴 표시 + 강조 표시 */
        HIGHLIGHT
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private TargetType targetType;

    @Column(name = "permission_group_code", length = 100)
    private String permissionGroupCode;

    @Column(name = "org_policy_id")
    private Long orgPolicyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_action", nullable = false, length = 20)
    private VisibilityAction visibilityAction = VisibilityAction.SHOW;

    @Column(name = "priority")
    private Integer priority = 0;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    protected MenuViewConfig() {}

    /**
     * 권한 그룹 기반 가시성 설정을 생성한다.
     */
    public static MenuViewConfig forPermissionGroup(Menu menu, String permissionGroupCode,
                                                     VisibilityAction action) {
        MenuViewConfig config = new MenuViewConfig();
        config.menu = Objects.requireNonNull(menu, "menu must not be null");
        config.targetType = TargetType.PERMISSION_GROUP;
        config.permissionGroupCode = Objects.requireNonNull(permissionGroupCode,
                "permissionGroupCode must not be null");
        config.visibilityAction = action;
        return config;
    }

    /**
     * 조직 정책 기반 가시성 설정을 생성한다.
     */
    public static MenuViewConfig forOrgPolicy(Menu menu, Long orgPolicyId,
                                               VisibilityAction action) {
        MenuViewConfig config = new MenuViewConfig();
        config.menu = Objects.requireNonNull(menu, "menu must not be null");
        config.targetType = TargetType.ORG_POLICY;
        config.orgPolicyId = Objects.requireNonNull(orgPolicyId, "orgPolicyId must not be null");
        config.visibilityAction = action;
        return config;
    }

    /**
     * 글로벌 가시성 설정을 생성한다.
     */
    public static MenuViewConfig forGlobal(Menu menu, VisibilityAction action) {
        MenuViewConfig config = new MenuViewConfig();
        config.menu = Objects.requireNonNull(menu, "menu must not be null");
        config.targetType = TargetType.GLOBAL;
        config.visibilityAction = action;
        return config;
    }

    // Getters

    public Menu getMenu() {
        return menu;
    }

    public String getMenuCode() {
        return menu != null ? menu.getCode() : null;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public String getPermissionGroupCode() {
        return permissionGroupCode;
    }

    public Long getOrgPolicyId() {
        return orgPolicyId;
    }

    public VisibilityAction getVisibilityAction() {
        return visibilityAction;
    }

    public Integer getPriority() {
        return priority;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    // Setters / Mutators

    public void setVisibilityAction(VisibilityAction visibilityAction) {
        this.visibilityAction = visibilityAction;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * 이 설정이 주어진 권한 그룹에 적용되는지 확인한다.
     */
    public boolean appliesTo(String permGroupCode) {
        if (targetType == TargetType.GLOBAL) {
            return true;
        }
        if (targetType == TargetType.PERMISSION_GROUP) {
            return permissionGroupCode != null && permissionGroupCode.equals(permGroupCode);
        }
        return false;
    }

    /**
     * 이 설정이 주어진 조직 정책에 적용되는지 확인한다.
     */
    public boolean appliesTo(Long orgPolId) {
        if (targetType == TargetType.GLOBAL) {
            return true;
        }
        if (targetType == TargetType.ORG_POLICY) {
            return orgPolicyId != null && orgPolicyId.equals(orgPolId);
        }
        return false;
    }

    @Override
    public String toString() {
        return "MenuViewConfig{" +
               "menuCode='" + getMenuCode() + '\'' +
               ", targetType=" + targetType +
               ", permissionGroupCode='" + permissionGroupCode + '\'' +
               ", orgPolicyId=" + orgPolicyId +
               ", visibilityAction=" + visibilityAction +
               ", priority=" + priority +
               ", active=" + active +
               '}';
    }
}
