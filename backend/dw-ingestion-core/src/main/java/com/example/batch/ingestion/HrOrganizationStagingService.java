package com.example.batch.ingestion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.domain.HrImportErrorEntity;
import com.example.dw.domain.HrOrganizationStagingEntity;
import com.example.dw.dto.HrOrganizationRecord;
import com.example.dw.dto.HrOrganizationValidationError;
import com.example.dw.infrastructure.persistence.HrImportErrorRepository;
import com.example.dw.infrastructure.persistence.HrOrganizationStagingRepository;

@Service
@RequiredArgsConstructor
public class HrOrganizationStagingService {

    private static final HexFormat HEX = HexFormat.of();

    private final HrOrganizationStagingRepository stagingRepository;
    private final HrImportErrorRepository errorRepository;

    @Transactional
    public void persistRecords(HrImportBatchEntity batch, List<HrOrganizationRecord> records) {
        for (HrOrganizationRecord record : records) {
            HrOrganizationStagingEntity staging = new HrOrganizationStagingEntity();
            staging.setBatch(batch);
            staging.setOrganizationCode(record.organizationCode());
            staging.setName(record.name());
            staging.setParentOrganizationCode(record.parentOrganizationCode());
            staging.setStatus(record.status());
            staging.setStartDate(record.startDate());
            staging.setEndDate(record.endDate());
            staging.setPayloadHash(hash(record.rawPayload()));
            staging.setRawPayload(record.rawPayload());
            stagingRepository.save(staging);
        }
    }

    @Transactional
    public void persistErrors(HrImportBatchEntity batch, List<HrOrganizationValidationError> errors) {
        for (HrOrganizationValidationError error : errors) {
            HrImportErrorEntity entity = new HrImportErrorEntity();
            entity.setBatch(batch);
            entity.setLineNumber(error.lineNumber());
            entity.setRecordType("ORGANIZATION");
            entity.setReferenceCode(error.organizationCode());
            entity.setErrorCode(error.errorCode());
            entity.setErrorMessage(error.errorMessage());
            entity.setRawPayload(error.rawPayload());
            errorRepository.save(entity);
        }
    }

    private static String hash(String payload) {
        if (payload == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
