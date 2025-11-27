package com.example.dwgateway.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;

import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.common.file.FileDownload;
import com.example.common.file.dto.FileMetadataDto;
import com.example.file.port.FileManagementPort;

class FileControllerAuditTest {

    FileManagementPort fileManagementPort = Mockito.mock(FileManagementPort.class);
    AuditPort auditPort = Mockito.mock(AuditPort.class);

    @Test
    @DisplayName("다운로드 시 AuditPort가 호출된다")
    void download_shouldAudit() {
        UUID id = UUID.randomUUID();
        FileMetadataDto metadata = new FileMetadataDto(
                id, "test.txt", "text/plain", 5L,
                null, "system", com.example.common.file.FileStatus.ACTIVE,
                null, null, null);
        FileDownload download = new FileDownload(metadata, new ByteArrayResource("hello".getBytes(StandardCharsets.UTF_8)));
        when(fileManagementPort.download(id, "system")).thenReturn(download);

        FileController controller = new FileController(fileManagementPort, auditPort);

        ResponseEntity<?> response = controller.download(id);

        assertThat(response.getBody()).isInstanceOf(ByteArrayResource.class);
        ArgumentCaptor<AuditEvent> eventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).record(eventCaptor.capture(), Mockito.eq(AuditMode.ASYNC_FALLBACK));
        AuditEvent event = eventCaptor.getValue();
        assertThat(event.getAction()).isEqualTo("DOWNLOAD");
        assertThat(event.getSubject().getKey()).isEqualTo(id.toString());
        assertThat(event.isSuccess()).isTrue();
    }
}
