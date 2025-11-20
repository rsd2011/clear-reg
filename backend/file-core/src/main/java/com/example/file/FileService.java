package com.example.file;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.tika.Tika;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.common.file.FileDownload;
import com.example.common.file.FileMetadataDto;
import com.example.common.file.FileStatus;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import com.example.file.storage.FileStorageClient;

@Service
public class FileService {

    private static final Set<String> ARCHIVE_EXTENSIONS = Set.of("zip", "jar");
    private static final int MAX_ARCHIVE_DEPTH = 3;
    private static final int MAX_ARCHIVE_ENTRIES = 500;
    private static final long DEFAULT_ARCHIVE_ENTRY_LIMIT = 20 * 1024 * 1024; // 20MB

    private final StoredFileRepository storedFileRepository;
    private final StoredFileVersionRepository versionRepository;
    private final FileAccessLogRepository accessLogRepository;
    private final FileStorageClient storageClient;
    private final PolicySettingsProvider policySettingsProvider;
    private final Clock clock;
    private final Tika tika = new Tika();
    private final MimeTypes mimeTypes = MimeTypes.getDefaultMimeTypes();

    public FileService(StoredFileRepository storedFileRepository,
                       StoredFileVersionRepository versionRepository,
                       FileAccessLogRepository accessLogRepository,
                       FileStorageClient storageClient,
                       PolicySettingsProvider policySettingsProvider,
                       Clock clock) {
        this.storedFileRepository = storedFileRepository;
        this.versionRepository = versionRepository;
        this.accessLogRepository = accessLogRepository;
        this.storageClient = storageClient;
        this.policySettingsProvider = policySettingsProvider;
        this.clock = clock;
    }

    @Transactional
    public StoredFile upload(FileUploadCommand command) {
        PolicyToggleSettings settings = policySettingsProvider.currentSettings();
        enforcePolicy(command, settings);
        OffsetDateTime now = now();
        StoredFile file = new StoredFile();
        file.setOriginalName(command.originalName());
        file.setContentType(command.contentType());
        file.setOwnerUsername(command.ownerUsername());
        file.setRetentionUntil(resolveRetention(command.retentionUntil(), settings, now));
        file.markCreated(command.ownerUsername(), now);

        StoredFileVersion version = createVersion(file, command, command.ownerUsername(), now);
        file.addVersion(version);
        file.setChecksum(version.getChecksum());
        file.setSha256(version.getChecksum());
        file.setScanStatus(ScanStatus.CLEAN); // TODO: 실제 AV 스캔 연동 시 결과로 대체
        file.setScannedAt(now);
        StoredFile persisted = storedFileRepository.save(file);
        logAccess(persisted, "UPLOAD", command.ownerUsername(), "v" + version.getVersionNumber(), now);
        return persisted;
    }

    @Transactional(readOnly = true)
    public List<FileSummaryView> listSummaries() {
        return storedFileRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public StoredFile getMetadata(UUID id) {
        return storedFileRepository.findById(id)
                .orElseThrow(() -> new StoredFileNotFoundException(id));
    }

    @Transactional
    public FileDownload download(UUID id, String actor) {
        StoredFile file = storedFileRepository.findById(id)
                .orElseThrow(() -> new StoredFileNotFoundException(id));
        if (file.isDeleted()) {
            throw new StoredFileNotFoundException(id);
        }
        if (file.getScanStatus() == ScanStatus.BLOCKED) {
            throw new FilePolicyViolationException("위험 파일로 차단된 첨부입니다.");
        }
        if (file.getScanStatus() != ScanStatus.CLEAN) {
            throw new FilePolicyViolationException("파일 스캔이 완료되지 않았습니다.");
        }
        StoredFileVersion version = versionRepository.findFirstByFileIdOrderByVersionNumberDesc(file.getId())
                .orElseThrow(() -> new StoredFileNotFoundException(id));
        try {
            var resource = storageClient.load(version.getStoragePath());
            OffsetDateTime now = now();
            file.markAccessed(now);
            storedFileRepository.save(file);
            logAccess(file, "DOWNLOAD", actor, "v" + version.getVersionNumber(), now);
            return new FileDownload(metadataOf(file), resource);
        }
        catch (IOException ex) {
            throw new FileStorageException("파일을 읽을 수 없습니다.", ex);
        }
    }

    @Transactional
    public StoredFile delete(UUID id, String actor) {
        StoredFile file = storedFileRepository.findById(id)
                .orElseThrow(() -> new StoredFileNotFoundException(id));
        if (file.isDeleted()) {
            return file;
        }
        file.setStatus(FileStatus.DELETED);
        file.markUpdated(actor, now());
        file.getVersions().forEach(version -> {
            try {
                storageClient.delete(version.getStoragePath());
            }
            catch (IOException ignored) {
            }
        });
        StoredFile saved = storedFileRepository.save(file);
        logAccess(saved, "DELETE", actor, null, now());
        return saved;
    }

    private StoredFileVersion createVersion(StoredFile file,
                                            FileUploadCommand command,
                                            String actor,
                                            OffsetDateTime now) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = command.inputStreamSupplier().get();
                 DigestInputStream digestStream = new DigestInputStream(inputStream, digest)) {
                FileStorageClient.StoredObject storedObject = storageClient.store(digestStream,
                        command.size(),
                        command.originalName());
                String checksum = HexFormat.of().formatHex(digest.digest());
                StoredFileVersion version = new StoredFileVersion();
                version.setVersionNumber(1);
                version.setStoragePath(storedObject.storagePath());
                version.setChecksum(checksum);
                version.setCreatedAt(now);
                version.setCreatedBy(actor);
                file.setSize(storedObject.size());
                return version;
            }
        }
        catch (NoSuchAlgorithmException | IOException ex) {
            throw new FileStorageException("파일을 저장하지 못했습니다.", ex);
        }
    }

    private void logAccess(StoredFile file, String action, String actor, String detail, OffsetDateTime now) {
        FileAccessLog log = new FileAccessLog();
        log.setFile(file);
        log.setAction(action);
        log.setActor(actor);
        log.setDetail(detail);
        log.setCreatedAt(now);
        accessLogRepository.save(log);
    }

    private void enforcePolicy(FileUploadCommand command, PolicyToggleSettings settings) {
        if (settings.maxFileSizeBytes() > 0 && command.size() > settings.maxFileSizeBytes()) {
            throw new FilePolicyViolationException("허용된 최대 파일 크기를 초과했습니다.");
        }
        List<String> allowedExtensions = settings.allowedFileExtensions();
        String extension = extensionOf(command.originalName());
        enforceExtensionRule(allowedExtensions, extension);
        boolean archive = isArchiveFile(extension, command.contentType());
        if (settings.strictMimeValidation()) {
            validateMime(command, extension);
        }
        if (archive) {
            long entryLimit = settings.maxFileSizeBytes() > 0 ? settings.maxFileSizeBytes() : DEFAULT_ARCHIVE_ENTRY_LIMIT;
            validateArchiveContents(command, allowedExtensions, entryLimit);
        }
    }

    private void validateMime(FileUploadCommand command, String declaredExtension) {
        try (InputStream stream = command.inputStreamSupplier().get()) {
            String detectedType = tika.detect(stream, command.originalName());
            if (!isMimeCompatible(detectedType, declaredExtension)) {
                throw new FilePolicyViolationException("파일 내용과 확장자가 일치하지 않습니다.");
            }
        }
        catch (IOException ex) {
            throw new FilePolicyViolationException("파일을 검사하는 중 오류가 발생했습니다.", ex);
        }
    }

    private OffsetDateTime resolveRetention(OffsetDateTime requested,
                                            PolicyToggleSettings settings,
                                            OffsetDateTime now) {
        if (requested != null) {
            return requested;
        }
        if (settings.fileRetentionDays() <= 0) {
            return null;
        }
        return now.plusDays(settings.fileRetentionDays());
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    private void validateArchiveContents(FileUploadCommand command,
                                         List<String> allowedExtensions,
                                         long maxEntryBytes) {
        try (InputStream stream = new BufferedInputStream(command.inputStreamSupplier().get())) {
            inspectArchive(stream, allowedExtensions, maxEntryBytes, 0, new AtomicInteger());
        }
        catch (IOException ex) {
            throw new FilePolicyViolationException("압축 파일을 검사하는 중 오류가 발생했습니다.", ex);
        }
    }

    private void enforceExtensionRule(List<String> allowedExtensions, String extension) {
        if (allowedExtensions.isEmpty()) {
            return;
        }
        boolean allowed = extension != null && allowedExtensions.stream()
                .anyMatch(ext -> ext.equalsIgnoreCase(extension));
        if (!allowed) {
            throw new FilePolicyViolationException("허용되지 않은 파일 확장자입니다.");
        }
    }

    private boolean isArchiveFile(String extension, String contentType) {
        if (isArchiveExtension(extension)) {
            return true;
        }
        if (contentType == null) {
            return false;
        }
        return contentType.contains("zip") || contentType.contains("compressed");
    }

    private boolean isArchiveExtension(String extension) {
        return extension != null && ARCHIVE_EXTENSIONS.contains(extension.toLowerCase());
    }

    private void inspectArchive(InputStream inputStream,
                                List<String> allowedExtensions,
                                long maxEntryBytes,
                                int depth,
                                AtomicInteger inspectedEntries) throws IOException {
        if (depth >= MAX_ARCHIVE_DEPTH) {
            throw new FilePolicyViolationException("허용된 압축 파일 중첩 깊이를 초과했습니다.");
        }
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zipInputStream.closeEntry();
                    continue;
                }
                if (inspectedEntries.incrementAndGet() > MAX_ARCHIVE_ENTRIES) {
                    throw new FilePolicyViolationException("압축 파일 항목 수가 정책을 초과했습니다.");
                }
                String entryExtension = extensionOf(entry.getName());
                enforceExtensionRule(allowedExtensions, entryExtension);
                if (isArchiveExtension(entryExtension)) {
                    byte[] nestedBytes = readEntryBytes(zipInputStream, maxEntryBytes, true);
                    inspectArchive(new ByteArrayInputStream(nestedBytes), allowedExtensions, maxEntryBytes,
                            depth + 1, inspectedEntries);
                } else {
                    readEntryBytes(zipInputStream, maxEntryBytes, false);
                }
                zipInputStream.closeEntry();
            }
        }
    }

    private byte[] readEntryBytes(ZipInputStream zipInputStream,
                                  long maxBytes,
                                  boolean retainContent) throws IOException {
        ByteArrayOutputStream buffer = retainContent ? new ByteArrayOutputStream() : null;
        byte[] chunk = new byte[8192];
        long total = 0;
        int read;
        while ((read = zipInputStream.read(chunk)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new FilePolicyViolationException("압축 항목 크기가 정책을 초과했습니다.");
            }
            if (retainContent) {
                buffer.write(chunk, 0, read);
            }
        }
        return retainContent ? buffer.toByteArray() : null;
    }

    private String extensionOf(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx + 1) : null;
    }

    private boolean isMimeCompatible(String detectedType, String declaredExtension) {
        try {
            var mimeType = mimeTypes.forName(detectedType);
            return mimeType.getExtension() != null && mimeType.getExtension().contains(declaredExtension);
        }
        catch (MimeTypeException ex) {
            return true; // treat unknown as compatible
        }
    }

    private FileMetadataDto metadataOf(StoredFile file) {
        return new FileMetadataDto(
                file.getId(),
                file.getOriginalName(),
                file.getContentType(),
                file.getSize(),
                file.getChecksum(),
                file.getOwnerUsername(),
                file.getStatus(),
                file.getRetentionUntil(),
                file.getCreatedAt(),
                file.getUpdatedAt());
    }

}
