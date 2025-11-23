package com.example.dw.application.export;

import lombok.Value;

@Value
public class ExportFailureEvent {
    String exportType;
    String fileName;
    long recordCount;
    String resultCode;
}
