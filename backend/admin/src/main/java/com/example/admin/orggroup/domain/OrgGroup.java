package com.example.admin.orggroup.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.example.common.jpa.PrimaryKeyEntity;
import com.example.common.orggroup.OrgGroupRoleType;
import com.example.common.orggroup.WorkCategory;

/**
 * 조직그룹.
 *
 * <p>사용자 소속 조직을 그룹화하여 권한 및 승인선 템플릿을 일괄 적용한다.</p>
 */
@Entity
@Table(name = "org_group")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class OrgGroup extends PrimaryKeyEntity {

    @Column(length = 100, unique = true, nullable = false)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 500)
    private String description;

    /** 표시 순서 (nullable) */
    private Integer sort;

    /** 업무 카테고리 목록 (JSON 배열로 저장) */
    @Column(name = "work_categories", columnDefinition = "TEXT")
    @Convert(converter = WorkCategorySetConverter.class)
    @Builder.Default
    private Set<WorkCategory> workCategories = new HashSet<>();

    /** 승인선 매핑 (양방향 참조) */
    @OneToMany(mappedBy = "orgGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrgGroupApprovalMapping> approvalMappings = new ArrayList<>();

    /** 역할별 권한 매핑 (양방향 참조) */
    @OneToMany(mappedBy = "orgGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrgGroupRolePermission> rolePermissions = new ArrayList<>();

    // ========== 업무 카테고리 편의 메서드 ==========

    /**
     * 업무 카테고리를 추가한다.
     *
     * @param category 추가할 카테고리
     */
    public void addWorkCategory(WorkCategory category) {
        if (category != null) {
            this.workCategories.add(category);
        }
    }

    /**
     * 업무 카테고리를 제거한다.
     *
     * @param category 제거할 카테고리
     */
    public void removeWorkCategory(WorkCategory category) {
        this.workCategories.remove(category);
    }

    /**
     * 업무 카테고리를 일괄 설정한다.
     *
     * @param categories 설정할 카테고리 집합
     */
    public void setWorkCategories(Set<WorkCategory> categories) {
        this.workCategories.clear();
        if (categories != null) {
            this.workCategories.addAll(categories);
        }
    }

    /**
     * 특정 업무 카테고리를 포함하는지 확인한다.
     *
     * @param category 확인할 카테고리
     * @return 포함 여부
     */
    public boolean hasWorkCategory(WorkCategory category) {
        return this.workCategories.contains(category);
    }

    // ========== 승인선 매핑 편의 메서드 ==========

    /**
     * 승인선 매핑을 추가한다.
     *
     * @param mapping 추가할 매핑
     */
    public void addApprovalMapping(OrgGroupApprovalMapping mapping) {
        if (mapping != null) {
            this.approvalMappings.add(mapping);
        }
    }

    /**
     * 승인선 매핑을 제거한다.
     *
     * @param mapping 제거할 매핑
     */
    public void removeApprovalMapping(OrgGroupApprovalMapping mapping) {
        this.approvalMappings.remove(mapping);
    }

    // ========== 역할별 권한 매핑 편의 메서드 ==========

    /**
     * 역할별 권한 매핑을 추가한다.
     *
     * @param rolePermission 추가할 매핑
     */
    public void addRolePermission(OrgGroupRolePermission rolePermission) {
        if (rolePermission != null) {
            this.rolePermissions.add(rolePermission);
        }
    }

    /**
     * 역할별 권한 매핑을 제거한다.
     *
     * @param rolePermission 제거할 매핑
     */
    public void removeRolePermission(OrgGroupRolePermission rolePermission) {
        this.rolePermissions.remove(rolePermission);
    }

    /**
     * 특정 역할의 권한그룹 코드를 조회한다.
     *
     * @param roleType 역할 유형
     * @return 권한그룹 코드 (없으면 null)
     */
    public String getPermGroupCodeByRole(OrgGroupRoleType roleType) {
        return this.rolePermissions.stream()
                .filter(rp -> rp.getRoleType() == roleType)
                .findFirst()
                .map(OrgGroupRolePermission::getPermGroupCode)
                .orElse(null);
    }
}
