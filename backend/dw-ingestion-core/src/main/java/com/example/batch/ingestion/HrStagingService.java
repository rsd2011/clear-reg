package com.example.batch.ingestion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.example.dw.domain.HrEmployeeStagingEntity;
import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.domain.HrImportErrorEntity;
import com.example.dw.dto.HrEmployeeRecord;
import com.example.dw.dto.HrValidationError;
import com.example.dw.infrastructure.persistence.HrEmployeeStagingRepository;
import com.example.dw.infrastructure.persistence.HrImportErrorRepository;

@Service
@RequiredArgsConstructor
public class HrStagingService {

    private static final HexFormat HEX = HexFormat.of();

    private final HrEmployeeStagingRepository stagingRepository;
    private final HrImportErrorRepository errorRepository;

    @Transactional
    public void persistRecords(HrImportBatchEntity batch, List<HrEmployeeRecord> records) {
        for (HrEmployeeRecord record : records) {
            HrEmployeeStagingEntity staging = HrEmployeeStagingEntity.fromRecord(
                    batch,
                    record,
                    hash(record.rawPayload())
            );
            stagingRepository.save(staging);
        }
    }

    @Transactional
    public void persistErrors(HrImportBatchEntity batch, List<HrValidationError> errors) {
        for (HrValidationError error : errors) {
            HrImportErrorEntity entity = HrImportErrorEntity.of(
                    batch,
                    error.lineNumber(),
                    "EMPLOYEE",
                    error.employeeId(),
                    error.errorCode(),
                    error.errorMessage(),
                    error.rawPayload()
            );
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
