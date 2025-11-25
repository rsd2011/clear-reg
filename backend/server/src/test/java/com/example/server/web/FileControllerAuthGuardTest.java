package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;

import com.example.auth.permission.PermissionDeniedException;
import com.example.auth.permission.context.AuthContextHolder;
import com.example.common.file.FileDownload;
import com.example.common.file.FileMetadataDto;
import com.example.common.file.FileStatus;
import com.example.draft.application.DraftApplicationService;
import com.example.draft.application.response.DraftApprovalStepResponse;
import com.example.draft.application.response.DraftReferenceResponse;
import com.example.draft.application.response.DraftResponse;
import com.example.draft.domain.DraftStatus;
import com.example.file.FilePolicyViolationException;
import com.example.file.port.FileManagementPort;

@DisplayName("FileController 권한/예외 분기")
class FileControllerAuthGuardTest {

    FileManagementPort fileManagementPort = Mockito.mock(FileManagementPort.class);
    DraftApplicationService draftApplicationService = Mockito.mock(DraftApplicationService.class);
    FileController controller = new FileController(fileManagementPort, draftApplicationService);

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("draftId가 있는데 인증 컨텍스트가 없으면 PermissionDeniedException을 던진다")
    void downloadWithDraftButNoAuthContext() {
        UUID draftId = UUID.randomUUID();
        when(draftApplicationService.getDraft(draftId, null, null, false))
                .thenThrow(new PermissionDeniedException("인증 정보가 없습니다."));

        assertThatThrownBy(() -> controller.download(UUID.randomUUID(), draftId))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    @DisplayName("업로드 시 MultipartFile read 실패면 RuntimeException을 전파한다")
    void uploadThrowsWhenMultipartReadFails() throws IOException {
        MultipartFile multipart = Mockito.mock(MultipartFile.class);
        when(multipart.getOriginalFilename()).thenReturn("a.txt");
        when(multipart.getContentType()).thenReturn("text/plain");
        when(multipart.getSize()).thenReturn(1L);
        when(multipart.getInputStream()).thenThrow(new IOException("io"));

        assertThatThrownBy(() -> controller.uploadFile(multipart, null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("draftId가 있고 첨부가 일치하면 다운로드를 허용한다")
    void downloadWithDraftAttachmentAllowed() {
        UUID fileId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();
        DraftResponse draft = new DraftResponse(draftId, "t", "c", "BF", "ORG", "creator",
                DraftStatus.DRAFT, null, null, null, null, null,
                null, null, null, null, null, null, null,
                List.of(new DraftApprovalStepResponse(null, 1, "AG1", null,
                        com.example.draft.domain.DraftApprovalState.WAITING, null, null, null, null, null)),
                List.of(new com.example.draft.application.response.DraftAttachmentResponse(fileId, "a.txt", "creator", 1L, null, null)),
                null, null);
        when(draftApplicationService.getDraft(draftId, "ORG", "user", false)).thenReturn(draft);
        when(draftApplicationService.listReferences(draftId, "ORG", "user", false))
                .thenReturn(List.of(new DraftReferenceResponse(UUID.randomUUID(), "ref-user", "ORG", "adder", null)));
        FileMetadataDto metadata = new FileMetadataDto(fileId, "a.txt", "text/plain", 1L, "h",
                "creator", FileStatus.ACTIVE, null, null, null);
        when(fileManagementPort.download(any(), any(), any()))
                .thenReturn(new FileDownload(metadata, null));
        AuthContextHolder.set(new com.example.auth.permission.context.AuthContext("user", "ORG", "PG", null, null, null, null));

        controller.download(fileId, draftId);
    }
}
