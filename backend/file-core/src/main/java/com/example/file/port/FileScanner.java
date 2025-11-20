package com.example.file.port;

import java.io.InputStream;

import com.example.file.ScanStatus;

public interface FileScanner {

    ScanStatus scan(String filename, InputStream inputStream);
}
