package com.example.policy.datapolicy;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.example.common.jpa.PrimaryKeyEntity;

@Entity
@Table(name = "data_policy")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataPolicy extends PrimaryKeyEntity {

    @Column(nullable = false, length = 100)
    private String featureCode;

    @Column(length = 100)
    private String actionCode;

    @Column(length = 100)
    private String permGroupCode;

    private Long orgPolicyId;

    @Column(length = 100)
    private String orgGroupCode;

    @Column(length = 100)
    private String businessType;

    @Column(nullable = false, length = 30)
    private String rowScope; // OWN | ORG | ORG_AND_DESC | ALL | CUSTOM

    @Lob
    private String rowScopeExpr;

    @Column(nullable = false, length = 30)
    private String defaultMaskRule; // NONE | PARTIAL | FULL | HASH | TOKENIZE

    @Column(columnDefinition = "TEXT")
    private String maskParams; // JSON string

    @Column(nullable = false)
    private Integer priority;

    @Column(nullable = false)
    private Boolean active;

    private Instant effectiveFrom;
    private Instant effectiveTo;

    private Instant createdAt;
    private Instant updatedAt;
}
