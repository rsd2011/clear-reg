package com.example.dw.domain;

import com.example.common.jpa.PrimaryKeyEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "dw_import_errors",
        indexes = {
                @Index(name = "idx_dw_error_batch", columnList = "batch_id")
        })
public class HrImportErrorEntity extends PrimaryKeyEntity {

    protected HrImportErrorEntity() {
    }

    private HrImportErrorEntity(HrImportBatchEntity batch,
                                int lineNumber,
                                String recordType,
                                String referenceCode,
                                String errorCode,
                                String errorMessage,
                                String rawPayload) {
        if (batch == null || recordType == null) {
            throw new IllegalArgumentException("batch와 recordType은 필수입니다.");
        }
        this.batch = batch;
        this.lineNumber = lineNumber;
        this.recordType = recordType;
        this.referenceCode = referenceCode;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.rawPayload = rawPayload;
    }

    public static HrImportErrorEntity of(HrImportBatchEntity batch,
                                         int lineNumber,
                                         String recordType,
                                         String referenceCode,
                                         String errorCode,
                                         String errorMessage,
                                         String rawPayload) {
        return new HrImportErrorEntity(batch, lineNumber, recordType, referenceCode, errorCode, errorMessage, rawPayload);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private HrImportBatchEntity batch;

    @Column(name = "line_number")
    private int lineNumber;

    @Column(name = "record_type", length = 32, nullable = false)
    private String recordType;

    @Column(name = "reference_code", length = 128)
    private String referenceCode;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "raw_payload", columnDefinition = "text")
    private String rawPayload;

    public HrImportBatchEntity getBatch() {
        return batch;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getRecordType() {
        return recordType;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getRawPayload() {
        return rawPayload;
    }
}
