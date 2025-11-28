package com.example.admin.orggroup.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import com.example.common.jpa.PrimaryKeyEntity;

@Entity
@Table(name = "org_group")
@Getter
@NoArgsConstructor
@AllArgsConstructor
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

    /** 신규 사용자가 리더 역할로 최초 접근 시 자동 부여할 권한 그룹 */
    @Column(length = 100)
    private String leaderPermGroupCode;

    /** 신규 사용자가 매니저 역할로 최초 접근 시 자동 부여할 권한 그룹 */
    @Column(length = 100)
    private String managerPermGroupCode;

    /** 신규 사용자가 일반 구성원 역할로 최초 접근 시 자동 부여할 권한 그룹 */
    @Column(length = 100)
    private String memberPermGroupCode;
}
