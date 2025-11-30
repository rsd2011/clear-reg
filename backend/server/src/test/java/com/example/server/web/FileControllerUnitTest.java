package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.file.FileDownload;
import com.example.common.file.dto.FileMetadataDto;
import com.example.common.file.FileStatus;
import com.example.draft.application.DraftApplicationService;
import com.example.draft.application.dto.DraftApprovalStepResponse;
import com.example.draft.application.dto.DraftAttachmentResponse;
import com.example.draft.application.dto.DraftResponse;
import com.example.draft.domain.DraftStatus;
import com.example.file.FilePolicyViolationException;
import com.example.file.port.FileManagementPort;

@DisplayName("FileController 단위 테스트")
class FileControllerUnitTest {

    FileManagementPort fileManagementPort = Mockito.mock(FileManagementPort.class);
    DraftApplicationService draftApplicationService = Mockito.mock(DraftApplicationService.class);
    FileController controller = new FileController(fileManagementPort, draftApplicationService);

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("draftId 없이 다운로드하면 기본 content-type 과 파일명이 설정된다")
    void downloadWithoutDraft() {
        UUID id = UUID.randomUUID();
        FileMetadataDto metadata = new FileMetadataDto(id, "hello.txt", null, 12, "hash",
                "system", FileStatus.ACTIVE, null, null, null);
        FileDownload download = new FileDownload(metadata, new ByteArrayResource("hi".getBytes()));
        when(fileManagementPort.download(id, "system", List.of("system"))).thenReturn(download);

        ResponseEntity<?> response = controller.download(id, null);

        assertThat(response.getHeaders().getContentDisposition().getFilename()).contains("hello.txt");
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/octet-stream");
    }

    @Test
    @DisplayName("기안에 첨부되지 않은 파일이면 정책 위반 예외를 던진다")
    void downloadWithDraftNotAttachedThrows() {
        UUID fileId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();
        AuthContextHolder.set(AuthContext.of("user", "ORG", "PG", FeatureCode.FILE, ActionCode.READ, null));
        DraftResponse draft = new DraftResponse(draftId, "t", "c", "BF", "ORG", "creator",
                DraftStatus.DRAFT, null, null, null, null, null,
                null, null, null, null, null, null, null,
                List.<DraftApprovalStepResponse>of(), List.<DraftAttachmentResponse>of(), null, null);
        when(draftApplicationService.getDraft(draftId, "ORG", "user", false)).thenReturn(draft);
        when(draftApplicationService.listReferences(draftId, "ORG", "user", false)).thenReturn(List.of());

        assertThatThrownBy(() -> controller.download(fileId, draftId))
                .isInstanceOf(FilePolicyViolationException.class);
    }
}
