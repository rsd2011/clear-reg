package com.example.admin.permission.domain;

import com.example.admin.menu.domain.Menu;
import com.example.common.jpa.PrimaryKeyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 권한 그룹별 메뉴 설정 엔티티.
 *
 * <p>권한 그룹별로 메뉴의 계층 구조와 표시 여부를 정의한다.
 * 레코드가 존재하면 해당 메뉴/카테고리가 표시되고, 없으면 표시되지 않는다.</p>
 *
 * <h3>메뉴 vs 카테고리</h3>
 * <ul>
 *   <li><b>메뉴</b>: {@link #menu}가 not null - 실제 클릭 가능한 메뉴</li>
 *   <li><b>카테고리</b>: {@link #menu}가 null - 하위 메뉴를 그룹핑하는 가상 노드</li>
 * </ul>
 *
 * <h3>계층 구조</h3>
 * <p>{@link #parent}를 통해 자기참조로 트리 구조를 형성한다.
 * parent가 null이면 루트 레벨이다.</p>
 *
 * <h3>사용 예시</h3>
 * <pre>
 * 권한그룹: ADMIN
 * ├─ [카테고리] 관리 (menu=null, categoryCode="ADMIN_CAT")
 * │   ├─ [메뉴] 사용자관리 (menu=Menu("USER_MGMT"))
 * │   └─ [메뉴] 권한관리 (menu=Menu("PERM_MGMT"))
 * └─ [메뉴] 대시보드 (menu=Menu("DASHBOARD"), parent=null)
 * </pre>
 */
@Entity
@Table(name = "permission_menus",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_pm_group_menu",
            columnNames = {"permission_group_code", "menu_id"}
        ),
        @UniqueConstraint(
            name = "uk_pm_group_category",
            columnNames = {"permission_group_code", "category_code"}
        )
    },
    indexes = {
        @Index(name = "idx_pm_perm_group", columnList = "permission_group_code"),
        @Index(name = "idx_pm_menu", columnList = "menu_id"),
        @Index(name = "idx_pm_parent", columnList = "parent_id"),
        @Index(name = "idx_pm_category", columnList = "category_code")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
public class PermissionMenu extends PrimaryKeyEntity {

    /**
     * 권한 그룹 코드. 필수.
     */
    @Column(name = "permission_group_code", nullable = false, length = 100)
    private String permissionGroupCode;

    /**
     * 연결된 메뉴. null이면 카테고리.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id")
    private Menu menu;

    /**
     * 카테고리 코드. menu가 null일 때 사용.
     */
    @Column(name = "category_code", length = 100)
    private String categoryCode;

    /**
     * 카테고리 이름. menu가 null일 때 사용.
     */
    @Column(name = "category_name", length = 200)
    private String categoryName;

    /**
     * 카테고리 아이콘. menu가 null일 때 사용.
     */
    @Column(name = "category_icon", length = 50)
    private String categoryIcon;

    /**
     * 부모 PermissionMenu. null이면 루트.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private PermissionMenu parent;

    /**
     * 표시 순서. 같은 부모 아래에서 정렬 기준.
     */
    @Column(name = "display_order")
    private Integer displayOrder;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected PermissionMenu() {}

    /**
     * 메뉴 타입의 PermissionMenu를 생성한다.
     *
     * @param permissionGroupCode 권한 그룹 코드
     * @param menu 연결할 메뉴
     * @param parent 부모 PermissionMenu (null이면 루트)
     * @param displayOrder 표시 순서
     * @return 메뉴 타입 PermissionMenu
     */
    public static PermissionMenu forMenu(String permissionGroupCode, Menu menu,
                                          PermissionMenu parent, Integer displayOrder) {
        Objects.requireNonNull(permissionGroupCode, "permissionGroupCode must not be null");
        Objects.requireNonNull(menu, "menu must not be null for menu type");

        PermissionMenu pm = new PermissionMenu();
        pm.permissionGroupCode = permissionGroupCode;
        pm.menu = menu;
        pm.parent = parent;
        pm.displayOrder = displayOrder;
        return pm;
    }

    /**
     * 카테고리 타입의 PermissionMenu를 생성한다.
     *
     * @param permissionGroupCode 권한 그룹 코드
     * @param categoryCode 카테고리 코드
     * @param categoryName 카테고리 이름
     * @param categoryIcon 카테고리 아이콘 (nullable)
     * @param parent 부모 PermissionMenu (null이면 루트)
     * @param displayOrder 표시 순서
     * @return 카테고리 타입 PermissionMenu
     */
    public static PermissionMenu forCategory(String permissionGroupCode,
                                              String categoryCode, String categoryName,
                                              String categoryIcon,
                                              PermissionMenu parent, Integer displayOrder) {
        Objects.requireNonNull(permissionGroupCode, "permissionGroupCode must not be null");
        Objects.requireNonNull(categoryCode, "categoryCode must not be null for category type");
        Objects.requireNonNull(categoryName, "categoryName must not be null for category type");

        PermissionMenu pm = new PermissionMenu();
        pm.permissionGroupCode = permissionGroupCode;
        pm.categoryCode = categoryCode;
        pm.categoryName = categoryName;
        pm.categoryIcon = categoryIcon;
        pm.parent = parent;
        pm.displayOrder = displayOrder;
        return pm;
    }

    // ========== Query Methods ==========

    /**
     * 카테고리인지 확인한다.
     */
    public boolean isCategory() {
        return menu == null;
    }

    /**
     * 메뉴인지 확인한다.
     */
    public boolean isMenu() {
        return menu != null;
    }

    /**
     * 루트 레벨인지 확인한다.
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * 코드를 반환한다. 메뉴면 menu.code, 카테고리면 categoryCode.
     */
    public String getCode() {
        return isCategory() ? categoryCode : menu.getCode();
    }

    /**
     * 이름을 반환한다. 메뉴면 menu.name, 카테고리면 categoryName.
     */
    public String getName() {
        return isCategory() ? categoryName : menu.getName();
    }

    /**
     * 아이콘을 반환한다. 메뉴면 menu.icon, 카테고리면 categoryIcon.
     */
    public String getIcon() {
        return isCategory() ? categoryIcon : menu.getIcon();
    }

    /**
     * 경로를 반환한다. 카테고리면 null.
     */
    public String getPath() {
        return isCategory() ? null : menu.getPath();
    }

    // ========== Mutators ==========

    public void setParent(PermissionMenu parent) {
        this.parent = parent;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void updateCategory(String categoryName, String categoryIcon) {
        if (!isCategory()) {
            throw new IllegalStateException("Cannot update category fields on menu type");
        }
        this.categoryName = Objects.requireNonNull(categoryName, "categoryName must not be null");
        this.categoryIcon = categoryIcon;
    }

    @Override
    public String toString() {
        return "PermissionMenu{" +
               "permissionGroupCode='" + permissionGroupCode + '\'' +
               ", " + (isCategory() ? "category=" + categoryCode : "menu=" + menu.getCode()) +
               ", parent=" + (parent != null ? parent.getCode() : "null") +
               ", displayOrder=" + displayOrder +
               '}';
    }
}
