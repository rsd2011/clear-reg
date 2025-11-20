package com.example.file.port;

import java.io.InputStream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.example.file.ScanStatus;

@Component
@ConditionalOnProperty(name = "file.scan.enabled", havingValue = "false")
public class DisabledFileScanner implements FileScanner {
    @Override
    public ScanStatus scan(String filename, InputStream inputStream) {
        return ScanStatus.PENDING;
    }
}
