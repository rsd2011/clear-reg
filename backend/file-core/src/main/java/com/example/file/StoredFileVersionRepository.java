package com.example.file;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileVersionRepository extends JpaRepository<StoredFileVersion, UUID> {

    Optional<StoredFileVersion> findFirstByFileIdOrderByVersionNumberDesc(UUID fileId);
}
