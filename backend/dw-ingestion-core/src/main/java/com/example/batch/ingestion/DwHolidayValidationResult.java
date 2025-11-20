package com.example.batch.ingestion;

import java.util.List;

import com.example.dw.dto.DwHolidayRecord;

public record DwHolidayValidationResult(List<DwHolidayRecord> validRecords,
                                        List<DwHolidayValidationError> errors) {
}
