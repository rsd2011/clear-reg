package com.example.batch.ingestion.template;

public record DwIngestionResult(int totalRecords,
                                int insertedRecords,
                                int updatedRecords,
                                int failedRecords) {
}
