package com.example.dw.dto;

import java.nio.file.Path;
import java.time.LocalDate;

import java.util.Collections;
import java.util.Map;

public record HrFileDescriptor(String fileName,
                               LocalDate businessDate,
                               int sequenceNumber,
                               Path path,
                               DataFeedType feedType,
                               Map<String, String> attributes) {

    public HrFileDescriptor(String fileName,
                             LocalDate businessDate,
                             int sequenceNumber,
                             Path path,
                             DataFeedType feedType) {
        this(fileName, businessDate, sequenceNumber, path, feedType, Collections.emptyMap());
    }
}
