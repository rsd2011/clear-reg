package com.example.dw.dto;

/**
 * Parsed representation of a DW common-code row prior to synchronization.
 */
public record DwCommonCodeRecord(String codeType,
                                 String codeValue,
                                 String codeName,
                                 Integer displayOrder,
                                 boolean active,
                                 String category,
                                 String description,
                                 String metadataJson,
                                 int lineNumber) {
}
