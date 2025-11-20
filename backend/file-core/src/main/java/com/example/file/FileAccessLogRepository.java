package com.example.file;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FileAccessLogRepository extends JpaRepository<FileAccessLog, UUID> {
}
