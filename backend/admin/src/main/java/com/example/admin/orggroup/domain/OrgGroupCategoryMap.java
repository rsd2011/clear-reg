package com.example.admin.orggroup.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.example.common.jpa.PrimaryKeyEntity;

@Entity
@Table(name = "org_group_category_map")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrgGroupCategoryMap extends PrimaryKeyEntity {

    @Column(length = 100, nullable = false)
    private String groupCode;

    @Column(length = 100, nullable = false)
    private String categoryCode;
}
