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
@Table(name = "org_group_category")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrgGroupCategory extends PrimaryKeyEntity {
    @Column(length = 100, unique = true, nullable = false)
    private String code;

    @Column(nullable = false, length = 255)
    private String label;

    @Column(length = 500)
    private String description;
}
