package com.example.audit.infra.masking;

import java.time.Instant;
import java.util.UUID;

import com.example.common.jpa.PrimaryKeyEntity;
import com.example.common.masking.SubjectType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "unmask_audit")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnmaskAuditRecord extends PrimaryKeyEntity {

    @Column(nullable = false)
    private Instant eventTime;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private SubjectType subjectType;

    @Column(length = 200)
    private String dataKind;

    @Column(length = 200)
    private String fieldName;

    @Column(length = 200)
    private String rowId;

    @Column(columnDefinition = "text")
    private String requesterRoles; // comma-separated

    @Column(columnDefinition = "text")
    private String reason;
}
