package com.example.dwgateway.web;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.audit.Actor;
import com.example.audit.ActorType;
import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.audit.RiskLevel;
import com.example.audit.Subject;
import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.RequirePermission;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.file.FileDownload;
import com.example.common.file.FileMetadataDto;
import com.example.file.FileUploadCommand;
import com.example.file.api.FileUploadRequest;
import com.example.file.port.FileManagementPort;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/files")
public class FileController {

    private final FileManagementPort fileManagementPort;
    private final AuditPort auditPort;

    public FileController(FileManagementPort fileManagementPort,
                          AuditPort auditPort) {
        this.fileManagementPort = fileManagementPort;
        this.auditPort = auditPort;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(feature = FeatureCode.FILE, action = ActionCode.UPLOAD)
    public FileMetadataDto uploadFile(@RequestPart("file") MultipartFile file,
                                      @Valid @RequestPart(value = "metadata", required = false) FileUploadRequest request)
            throws IOException {
        String actor = currentUsername();
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment";
        FileUploadCommand command = new FileUploadCommand(
                originalName,
                file.getContentType(),
                file.getSize(),
                () -> {
                    try {
                        return file.getInputStream();
                    }
                    catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                },
                request != null ? request.retentionUntil() : null,
                actor);
        return fileManagementPort.upload(command);
    }

    @GetMapping
    @RequirePermission(feature = FeatureCode.FILE, action = ActionCode.READ)
    public List<FileMetadataDto> listFiles() {
        return fileManagementPort.list();
    }

    @GetMapping("/{id}")
    @RequirePermission(feature = FeatureCode.FILE, action = ActionCode.READ)
    public FileMetadataDto metadata(@PathVariable UUID id) {
        return fileManagementPort.getMetadata(id);
    }

    @GetMapping("/{id}/content")
    @RequirePermission(feature = FeatureCode.FILE, action = ActionCode.DOWNLOAD)
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        FileDownload download = fileManagementPort.download(id, currentUsername());
        String filename = URLEncoder.encode(download.metadata().originalName(), StandardCharsets.UTF_8);
        auditDownload(id, download);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType(
                        download.metadata().contentType() != null ? download.metadata().contentType()
                                : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .contentLength(download.metadata().size())
                .body(download.resource());
    }

    @DeleteMapping("/{id}")
    @RequirePermission(feature = FeatureCode.FILE, action = ActionCode.DELETE)
    public FileMetadataDto delete(@PathVariable UUID id) {
        return fileManagementPort.delete(id, currentUsername());
    }

    private void auditDownload(UUID id, FileDownload download) {
        String actorId = currentUsername();
        ActorType actorType = "system".equalsIgnoreCase(actorId) ? ActorType.SYSTEM : ActorType.HUMAN;
        AuditEvent event = AuditEvent.builder()
                .eventType("FILE")
                .moduleName("dw-gateway")
                .action("DOWNLOAD")
                .actor(Actor.builder().id(actorId).type(actorType).build())
                .subject(Subject.builder().type("FILE").key(id.toString()).build())
                .success(true)
                .resultCode("OK")
                .riskLevel(RiskLevel.MEDIUM)
                .extraEntry("contentType", download.metadata().contentType())
                .extraEntry("size", download.metadata().size())
                .build();
        try {
            auditPort.record(event, AuditMode.ASYNC_FALLBACK);
        } catch (Exception ignored) {
            // 감사 실패가 다운로드를 막지 않음
        }
    }

    private String currentUsername() {
        return AuthContextHolder.current()
                .map(AuthContext::username)
                .orElse("system");
    }
}
