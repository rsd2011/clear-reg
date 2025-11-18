package com.example.batch.ingestion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.example.hr.domain.HrEmployeeStagingEntity;
import com.example.hr.domain.HrImportBatchEntity;
import com.example.hr.domain.HrImportErrorEntity;
import com.example.hr.dto.HrEmployeeRecord;
import com.example.hr.dto.HrValidationError;
import com.example.hr.infrastructure.persistence.HrEmployeeStagingRepository;
import com.example.hr.infrastructure.persistence.HrImportErrorRepository;

@Service
@RequiredArgsConstructor
public class HrStagingService {

    private static final HexFormat HEX = HexFormat.of();

    private final HrEmployeeStagingRepository stagingRepository;
    private final HrImportErrorRepository errorRepository;

    @Transactional
    public void persistRecords(HrImportBatchEntity batch, List<HrEmployeeRecord> records) {
        for (HrEmployeeRecord record : records) {
            HrEmployeeStagingEntity staging = new HrEmployeeStagingEntity();
            staging.setBatch(batch);
            staging.setEmployeeId(record.employeeId());
            staging.setFullName(record.fullName());
            staging.setEmail(record.email());
            staging.setOrganizationCode(record.organizationCode());
            staging.setEmploymentType(record.employmentType());
            staging.setEmploymentStatus(record.employmentStatus());
            staging.setStartDate(record.startDate());
            staging.setEndDate(record.endDate());
            staging.setPayloadHash(hash(record.rawPayload()));
            staging.setRawPayload(record.rawPayload());
            stagingRepository.save(staging);
        }
    }

    @Transactional
    public void persistErrors(HrImportBatchEntity batch, List<HrValidationError> errors) {
        for (HrValidationError error : errors) {
            HrImportErrorEntity entity = new HrImportErrorEntity();
            entity.setBatch(batch);
            entity.setLineNumber(error.lineNumber());
            entity.setRecordType("EMPLOYEE");
            entity.setReferenceCode(error.employeeId());
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
