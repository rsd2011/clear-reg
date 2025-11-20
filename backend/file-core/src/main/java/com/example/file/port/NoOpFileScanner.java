package com.example.file.port;

import java.io.InputStream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.example.file.ScanStatus;

@Component
@ConditionalOnMissingBean(FileScanner.class)
public class NoOpFileScanner implements FileScanner {

    @Override
    public ScanStatus scan(String filename, InputStream inputStream) {
        return ScanStatus.CLEAN;
    }
}
