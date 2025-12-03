package com.example.batch.ingestion;

import java.util.List;

import com.example.dw.dto.DwCommonCodeRecord;

public record DwCommonCodeValidationResult(List<DwCommonCodeRecord> validRecords,
                                           List<DwCommonCodeValidationError> errors) {
}
