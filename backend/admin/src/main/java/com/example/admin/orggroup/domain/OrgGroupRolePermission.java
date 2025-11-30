package com.example.admin.orggroup.domain;

import java.time.OffsetDateTime;

import com.example.common.jpa.PrimaryKeyEntity;
import com.example.common.orggroup.OrgGroupRoleType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 조직그룹별 역할-권한그룹 매핑.
 *
 * <p>조직그룹과 역할유형 조합에 따라 적용할 권한그룹을 지정한다.</p>
 * <p>신규 사용자가 해당 역할로 최초 접근 시 자동으로 권한그룹이 부여된다.</p>
 */
@Entity
@Table(
        name = "org_group_role_permission",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_org_group_role_type",
                columnNames = {"org_group_id", "role_type"}
        ),
        indexes = {
                @Index(name = "idx_org_group_role_perm_org_group_id", columnList = "org_group_id"),
                @Index(name = "idx_org_group_role_perm_perm_group_code", columnList = "perm_group_code")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrgGroupRolePermission extends PrimaryKeyEntity {

    /**
     * 조직그룹 (FK 참조).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_group_id", nullable = false)
    private OrgGroup orgGroup;

    /**
     * 역할 유형.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 20)
    private OrgGroupRoleType roleType;

    /**
     * 적용할 권한그룹 코드.
     */
    @Column(name = "perm_group_code", nullable = false, length = 100)
    private String permGroupCode;

    /**
     * 생성 일시.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * 수정 일시.
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * 생성자.
     *
     * @param orgGroup      조직그룹 (필수)
     * @param roleType      역할 유형 (필수)
     * @param permGroupCode 권한그룹 코드 (필수)
     * @param now           생성 시점
     */
    private OrgGroupRolePermission(
            OrgGroup orgGroup,
            OrgGroupRoleType roleType,
            String permGroupCode,
            OffsetDateTime now) {
        this.orgGroup = orgGroup;
        this.roleType = roleType;
        this.permGroupCode = permGroupCode;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 역할-권한 매핑을 생성한다.
     *
     * @param orgGroup      조직그룹
     * @param roleType      역할 유형
     * @param permGroupCode 권한그룹 코드
     * @param now           생성 시점
     * @return 새로운 매핑 인스턴스
     */
    public static OrgGroupRolePermission create(
            OrgGroup orgGroup,
            OrgGroupRoleType roleType,
            String permGroupCode,
            OffsetDateTime now) {
        if (orgGroup == null) {
            throw new IllegalArgumentException("조직그룹은 필수입니다.");
        }
        if (roleType == null) {
            throw new IllegalArgumentException("역할 유형은 필수입니다.");
        }
        if (permGroupCode == null || permGroupCode.isBlank()) {
            throw new IllegalArgumentException("권한그룹 코드는 필수입니다.");
        }
        return new OrgGroupRolePermission(orgGroup, roleType, permGroupCode, now);
    }

    /**
     * 권한그룹을 변경한다.
     *
     * @param newPermGroupCode 새 권한그룹 코드
     * @param now              변경 시점
     */
    public void changePermGroupCode(String newPermGroupCode, OffsetDateTime now) {
        if (newPermGroupCode == null || newPermGroupCode.isBlank()) {
            throw new IllegalArgumentException("권한그룹 코드는 필수입니다.");
        }
        this.permGroupCode = newPermGroupCode;
        this.updatedAt = now;
    }
}
