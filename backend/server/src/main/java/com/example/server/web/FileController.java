package com.example.server.web;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.admin.permission.ActionCode;
import com.example.admin.permission.FeatureCode;
import com.example.admin.permission.RequirePermission;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.admin.permission.context.AuthContext;
import com.example.common.file.FileDownload;
import com.example.common.file.FileMetadataDto;
import com.example.file.dto.FileUploadCommand;
import com.example.file.api.FileUploadRequest;
import com.example.file.port.FileManagementPort;
import com.example.server.file.dto.FileMetadataResponse;
import com.example.draft.application.response.DraftReferenceResponse;
import com.example.draft.application.response.DraftResponse;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/files")
@Tag(name = "File", description = "파일 업로드/다운로드/삭제 API")
public class FileController {

    private final FileManagementPort fileManagementPort;
    private final com.example.draft.application.DraftApplicationService draftApplicationService;

    public FileController(FileManagementPort fileManagementPort,
                          com.example.draft.application.DraftApplicationService draftApplicationService) {
        this.fileManagementPort = fileManagementPort;
        this.draftApplicationService = draftApplicationService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(feature = FeatureCode.FILE, action = ActionCode.UPLOAD)
    public FileMetadataResponse uploadFile(@RequestPart("file") MultipartFile file,
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
        FileMetadataDto metadata = fileManagementPort.upload(command);
        return FileMetadataResponse.fromDto(metadata);
    }

    @GetMapping
    @RequirePermission(feature = FeatureCode.FILE, action = ActionCode.READ)
    public java.util.List<FileMetadataResponse> listFiles() {
        var policyMatch = com.example.common.policy.DataPolicyContextHolder.get();
        java.util.function.UnaryOperator<String> masker = com.example.common.masking.MaskingFunctions.masker(policyMatch);
        return fileManagementPort.list()
                .stream()
                .map(meta -> FileMetadataResponse.fromDto(meta, masker))
                .toList();
    }

    @GetMapping("/{id}")
    @RequirePermission(feature = FeatureCode.FILE, action = ActionCode.DOWNLOAD)
    public ResponseEntity<Resource> download(@PathVariable UUID id,
                                             @RequestParam(value = "draftId", required = false) UUID draftId) {
        String actor = currentUsername();
        var match = com.example.common.policy.DataPolicyContextHolder.get();
        var masker = com.example.common.masking.MaskingFunctions.masker(match);
        java.util.List<String> allowed = new java.util.ArrayList<>();
        allowed.add(actor);
        if (draftId != null) {
            AuthContext context = AuthContextHolder.current()
                    .orElseThrow(() -> new com.example.admin.permission.PermissionDeniedException("인증 정보가 없습니다."));
            DraftResponse draft = draftApplicationService.getDraft(draftId, context.organizationCode(), actor, false);
            java.util.List<DraftReferenceResponse> refs = draftApplicationService.listReferences(draftId, context.organizationCode(), actor, false);
            allowed.add(draft.createdBy());
            draft.approvalSteps().forEach(step -> {
                if (step.actedBy() != null) allowed.add(step.actedBy());
                if (step.delegatedTo() != null) allowed.add(step.delegatedTo());
            });
            refs.forEach(r -> allowed.add(r.referencedUserId()));
            boolean attached = draft.attachments().stream().anyMatch(a -> a.fileId().equals(id));
            if (!attached) {
                throw new com.example.file.FilePolicyViolationException("해당 기안에 첨부되지 않은 파일입니다.");
            }
        }
        FileDownload download = fileManagementPort.download(id, actor, allowed);
        String maskedName = masker.apply(download.metadata().originalName());
        String filename = URLEncoder.encode(maskedName, StandardCharsets.UTF_8);
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
    public FileMetadataResponse delete(@PathVariable UUID id) {
        return FileMetadataResponse.fromDto(fileManagementPort.delete(id, currentUsername()));
    }

    private String currentUsername() {
        return AuthContextHolder.current()
                .map(AuthContext::username)
                .orElse("system");
    }
}
