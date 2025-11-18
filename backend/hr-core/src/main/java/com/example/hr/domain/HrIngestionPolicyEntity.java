package com.example.hr.domain;

import java.time.Instant;

import com.example.common.jpa.PrimaryKeyEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "hr_ingestion_policies")
public class HrIngestionPolicyEntity extends PrimaryKeyEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Lob
    @Column(nullable = false)
    private String yaml;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected HrIngestionPolicyEntity() {
    }

    public HrIngestionPolicyEntity(String code, String yaml) {
        this.code = code;
        this.yaml = yaml;
        this.updatedAt = Instant.now();
    }

    public String getCode() {
        return code;
    }

    public String getYaml() {
        return yaml;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateYaml(String yaml) {
        this.yaml = yaml;
        this.updatedAt = Instant.now();
    }
}
