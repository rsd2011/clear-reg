package com.example.file.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.common.file.FileDownload;
import com.example.common.file.FileMetadataDto;
import com.example.common.file.FileStatus;
import com.example.file.FileService;
import com.example.file.FileSummaryView;
import com.example.file.FileUploadCommand;
import com.example.file.StoredFile;
import com.example.file.audit.FileAuditEvent;
import com.example.file.audit.FileAuditPublisher;

@DisplayName("FileManagementPortAdapter 테스트")
class FileManagementPortAdapterTest {

    private final FileService fileService = Mockito.mock(FileService.class);
    private final FileAuditPublisher auditPublisher = Mockito.mock(FileAuditPublisher.class);
    private final FileManagementPortAdapter adapter = new FileManagementPortAdapter(fileService, auditPublisher);

    @Test
    @DisplayName("업로드 호출을 위임한다")
    void uploadDelegates() {
        StoredFile file = sampleFile();
        FileUploadCommand command = new FileUploadCommand("test.txt", "text/plain", 10, () -> InputStream.nullInputStream(), null, "tester");
        given(fileService.upload(command)).willReturn(file);

        FileMetadataDto result = adapter.upload(command);

        assertThat(result.originalName()).isEqualTo("test.txt");
        then(fileService).should().upload(command);
        then(auditPublisher).should().publish(Mockito.any(FileAuditEvent.class));
    }

    @Test
    @DisplayName("다운로드 호출을 위임한다")
    void downloadDelegates() {
        FileDownload download = new FileDownload(sampleMetadata(), null);
        UUID id = UUID.randomUUID();
        given(fileService.download(id, "tester", List.of())).willReturn(download);

        assertThat(adapter.download(id, "tester")).isEqualTo(download);
        then(fileService).should().download(id, "tester", List.of());
    }

    @Test
    @DisplayName("목록 조회를 위임한다")
    void listDelegates() {
        FileSummaryView view = new FileSummaryView() {
            @Override public UUID getId() { return UUID.randomUUID(); }
            @Override public String getOriginalName() { return "test.txt"; }
            @Override public String getContentType() { return "text/plain"; }
            @Override public long getSize() { return 10; }
            @Override public String getOwnerUsername() { return "tester"; }
            @Override public FileStatus getStatus() { return FileStatus.ACTIVE; }
            @Override public OffsetDateTime getCreatedAt() { return OffsetDateTime.now(); }
            @Override public OffsetDateTime getUpdatedAt() { return OffsetDateTime.now(); }
        };
        given(fileService.listSummaries()).willReturn(List.of(view));

        assertThat(adapter.list()).hasSize(1);
        then(fileService).should().listSummaries();
    }

    @Test
    @DisplayName("상태가 null이면 ACTIVE로 매핑한다")
    void listMapsNullStatusToActive() {
        FileSummaryView view = new FileSummaryView() {
            @Override public UUID getId() { return UUID.randomUUID(); }
            @Override public String getOriginalName() { return "test.txt"; }
            @Override public String getContentType() { return "text/plain"; }
            @Override public long getSize() { return 10; }
            @Override public String getOwnerUsername() { return "tester"; }
            @Override public FileStatus getStatus() { return null; }
            @Override public OffsetDateTime getCreatedAt() { return OffsetDateTime.now(); }
            @Override public OffsetDateTime getUpdatedAt() { return null; }
        };
        given(fileService.listSummaries()).willReturn(List.of(view));

        List<FileMetadataDto> list = adapter.list();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).status()).isEqualTo(FileStatus.ACTIVE);
    }

    @Test
    @DisplayName("삭제 호출을 위임하고 감사 로그를 남긴다")
    void deleteDelegatesAndPublishes() {
        StoredFile file = sampleFile();
        file.markDeleted("tester", OffsetDateTime.now());
        UUID id = UUID.randomUUID();
        given(fileService.delete(id, "tester")).willReturn(file);

        FileMetadataDto metadata = adapter.delete(id, "tester");

        assertThat(metadata.status()).isEqualTo(FileStatus.DELETED);
        then(auditPublisher).should().publish(Mockito.argThat(event ->
                event.action().equals("DELETE") && event.fileId().equals(id)));
    }

    @Test
    @DisplayName("다운로드 감사 로그는 updatedAt이 없으면 createdAt을 사용한다")
    void downloadPublishesUsingCreatedAtWhenUpdatedNull() {
        FileMetadataDto metadata = new FileMetadataDto(
                UUID.randomUUID(),
                "tmp.txt",
                "text/plain",
                10,
                "hash",
                "tester",
                FileStatus.ACTIVE,
                null,
                OffsetDateTime.now(),
                null);
        FileDownload download = new FileDownload(metadata, null);
        UUID id = metadata.id();
        given(fileService.download(id, "tester", List.of())).willReturn(download);

        adapter.download(id, "tester");

        then(auditPublisher).should().publish(Mockito.argThat(event ->
                event.action().equals("DOWNLOAD") && event.fileId().equals(id)));
    }

    private StoredFile sampleFile() {
        StoredFile file = StoredFile.create("test.txt", "text/plain", "tester", null, "tester", OffsetDateTime.now());
        file.updateHashes(10, "abcd", "abcd");
        return file;
    }

    private FileMetadataDto sampleMetadata() {
        OffsetDateTime ts = OffsetDateTime.now();
        return new FileMetadataDto(
                UUID.randomUUID(),
                "test.txt",
                "text/plain",
                10,
                "abcd",
                "tester",
                FileStatus.ACTIVE,
                null,
                ts,
                ts);
    }
}
