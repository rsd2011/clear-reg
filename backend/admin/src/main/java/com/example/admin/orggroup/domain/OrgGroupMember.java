package com.example.admin.orggroup.domain;

import com.example.common.jpa.PrimaryKeyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "org_group_member")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrgGroupMember extends PrimaryKeyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_code", referencedColumnName = "code", nullable = false)
    private OrgGroup orgGroup;

    @Column(length = 100, nullable = false)
    private String orgId; // DW 조직 ID

    /** 표시 순서 (nullable) */
    private Integer displayOrder;

    /**
     * 조직그룹 코드를 반환한다.
     * @return 조직그룹 코드
     */
    public String getGroupCode() {
        return orgGroup != null ? orgGroup.getCode() : null;
    }
}
