package com.example.hr.dto;

import java.nio.file.Path;
import java.time.LocalDate;

public record HrFileDescriptor(String fileName,
                               LocalDate businessDate,
                               int sequenceNumber,
                               Path path,
                               HrFeedType feedType) {
}
