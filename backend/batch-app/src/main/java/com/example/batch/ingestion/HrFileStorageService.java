package com.example.batch.ingestion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.hr.config.HrIngestionProperties;
import com.example.hr.dto.HrFeedType;
import com.example.hr.dto.HrFileDescriptor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class HrFileStorageService {
    private static final Pattern EMPLOYEE_PATTERN = Pattern.compile("employee_(\\d{8})_(\\d{3})\\.csv", Pattern.CASE_INSENSITIVE);
    private static final Pattern ORGANIZATION_PATTERN = Pattern.compile("organization_(\\d{8})_(\\d{3})\\.csv", Pattern.CASE_INSENSITIVE);

    private final HrIngestionProperties properties;

    public HrFileStorageService(HrIngestionProperties properties) {
        this.properties = properties;
        ensureDirectories();
    }

    public Optional<HrFileDescriptor> nextPendingFile() {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        ensureDirectories();
        try (Stream<Path> files = Files.list(properties.getIncomingDir())) {
            return files.filter(Files::isRegularFile)
                    .map(this::toDescriptor)
                    .flatMap(Optional::stream)
                    .sorted(Comparator.comparing(HrFileDescriptor::businessDate)
                            .thenComparing(HrFileDescriptor::sequenceNumber))
                    .findFirst();
        } catch (IOException ex) {
            log.error("Failed to list HR files", ex);
            return Optional.empty();
        }
    }

    public String readPayload(HrFileDescriptor descriptor) throws IOException {
        return Files.readString(descriptor.path(), StandardCharsets.UTF_8);
    }

    public void markProcessed(HrFileDescriptor descriptor, boolean success) {
        Path targetDir = success ? properties.getArchiveDir() : properties.getErrorDir();
        ensureDirectory(targetDir);
        Path target = targetDir.resolve(descriptor.path().getFileName());
        try {
            Files.move(descriptor.path(), target);
        } catch (IOException ex) {
            log.warn("Failed to move HR file {} to {}", descriptor.path(), target, ex);
        }
    }

    private Optional<HrFileDescriptor> toDescriptor(Path path) {
        String filename = path.getFileName().toString();
        Matcher matcher = EMPLOYEE_PATTERN.matcher(filename);
        HrFeedType feedType = null;
        if (matcher.matches()) {
            feedType = HrFeedType.EMPLOYEE;
        } else {
            matcher = ORGANIZATION_PATTERN.matcher(filename);
            if (matcher.matches()) {
                feedType = HrFeedType.ORGANIZATION;
            }
        }
        if (feedType == null) {
            log.debug("Ignoring HR file with unexpected name: {}", filename);
            return Optional.empty();
        }
        LocalDate businessDate = LocalDate.parse(matcher.group(1), java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        int sequenceNumber = Integer.parseInt(matcher.group(2));
        return Optional.of(new HrFileDescriptor(filename, businessDate, sequenceNumber, path, feedType));
    }

    private void ensureDirectories() {
        ensureDirectory(properties.getIncomingDir());
        if (properties.isAutoArchive()) {
            ensureDirectory(properties.getArchiveDir());
            ensureDirectory(properties.getErrorDir());
        }
    }

    private void ensureDirectory(Path path) {
        if (path == null) {
            return;
        }
        try {
            if (StringUtils.hasText(path.toString())) {
                Files.createDirectories(path);
            }
        } catch (IOException ex) {
            log.warn("Failed to create directory {}", path, ex);
        }
    }
}
