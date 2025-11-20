package com.example.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import com.example.common.file.FileDownload;
import com.example.common.file.FileStatus;
import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;
import com.example.file.storage.FileStorageClient;

@org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
@org.springframework.context.annotation.Import({FileService.class, FileServiceTest.TestConfig.class})
@org.springframework.test.context.ContextConfiguration(classes = FileServiceTest.TestApplication.class)
@DisplayName("FileService 테스트")
class FileServiceTest {

    @Autowired
    private FileService fileService;

    @Test
    @DisplayName("Given 업로드 명령 When 저장하면 Then 다운로드까지 가능하다")
    void givenUploadCommand_whenStored_thenCanDownload() {
        byte[] data = "hello world".getBytes();
        FileUploadCommand command = new FileUploadCommand(
                "hello.txt",
                "text/plain",
                data.length,
                () -> new ByteArrayInputStream(data),
                null,
                "tester");

        StoredFile storedFile = fileService.upload(command);

        assertThat(storedFile.getOriginalName()).isEqualTo("hello.txt");
        assertThat(storedFile.getChecksum()).isNotBlank();
        assertThat(storedFile.getRetentionUntil()).isEqualTo(TestConfig.NOW.plusDays(30));
        FileDownload download = fileService.download(storedFile.getId(), "tester");
        assertThat(download.metadata().id()).isEqualTo(storedFile.getId());
    }

    @Test
    @DisplayName("Given 저장된 파일 When 삭제하면 Then 상태가 DELETED로 변경된다")
    void givenStoredFile_whenDeleted_thenStatusUpdated() {
        byte[] data = "delete me".getBytes();
        StoredFile storedFile = fileService.upload(new FileUploadCommand(
                "delete.txt",
                "text/plain",
                data.length,
                () -> new ByteArrayInputStream(data),
                null,
                "tester"));

        StoredFile deleted = fileService.delete(storedFile.getId(), "tester");

        assertThat(deleted.getStatus()).isEqualTo(FileStatus.DELETED);
    }

    @Test
    @DisplayName("Given 허용되지 않은 확장자 When 업로드하면 Then 정책 위반 예외가 발생한다")
    void givenDisallowedExtension_whenUpload_thenThrow() {
        byte[] data = "bad".getBytes();
        FileUploadCommand command = new FileUploadCommand(
                "script.exe",
                "application/octet-stream",
                data.length,
                () -> new ByteArrayInputStream(data),
                null,
                "tester");

        assertThatThrownBy(() -> fileService.upload(command))
                .isInstanceOf(FilePolicyViolationException.class);
    }

    @Test
    @DisplayName("Given 허용된 항목만 있는 압축파일 When 업로드하면 Then 정상 저장된다")
    void givenArchiveWithAllowedEntries_whenUpload_thenStored() {
        byte[] archive = createZip(Map.of("doc.txt", "content".getBytes()));
        FileUploadCommand command = new FileUploadCommand(
                "bundle.zip",
                "application/zip",
                archive.length,
                () -> new ByteArrayInputStream(archive),
                null,
                "tester");

        StoredFile storedFile = fileService.upload(command);

        assertThat(storedFile.getOriginalName()).isEqualTo("bundle.zip");
    }

    @Test
    @DisplayName("Given 금지 항목이 포함된 압축파일 When 업로드하면 Then 정책 위반 예외가 발생한다")
    void givenArchiveWithDisallowedEntry_whenUpload_thenThrow() {
        byte[] archive = createZip(Map.of("evil.exe", "bad".getBytes()));
        FileUploadCommand command = new FileUploadCommand(
                "bundle.zip",
                "application/zip",
                archive.length,
                () -> new ByteArrayInputStream(archive),
                null,
                "tester");

        assertThatThrownBy(() -> fileService.upload(command))
                .isInstanceOf(FilePolicyViolationException.class);
    }

    @Test
    @DisplayName("Given 중첩 압축파일에 금지 항목이 있을 때 When 업로드하면 Then 정책 위반 예외가 발생한다")
    void givenNestedArchiveWithDisallowedEntry_whenUpload_thenThrow() {
        byte[] nested = createZip(Map.of("evil.exe", "bad".getBytes()));
        byte[] outer = createZip(Map.of("nested.zip", nested));
        FileUploadCommand command = new FileUploadCommand(
                "bundle.zip",
                "application/zip",
                outer.length,
                () -> new ByteArrayInputStream(outer),
                null,
                "tester");

        assertThatThrownBy(() -> fileService.upload(command))
                .isInstanceOf(FilePolicyViolationException.class);
    }

    static class TestConfig {

        static final OffsetDateTime NOW = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

        @Bean
        FileStorageClient fileStorageClient() {
            return new InMemoryStorage();
        }

        @Bean
        Clock clock() {
            return Clock.fixed(NOW.toInstant(), ZoneOffset.UTC);
        }

        @Bean
        PolicySettingsProvider policySettingsProvider() {
            return () -> new PolicyToggleSettings(true, true, true,
                    List.of("PASSWORD"),
                    10_485_760L,
                    List.of("txt", "pdf", "zip"),
                    true,
                    30);
        }
    }

    static class InMemoryStorage implements FileStorageClient {

        private final Map<String, byte[]> store = new HashMap<>();

        @Override
        public StoredObject store(InputStream inputStream, long size, String suggestedName) throws IOException {
            byte[] data = inputStream.readAllBytes();
            String path = UUID.randomUUID() + "-" + suggestedName;
            store.put(path, data);
            return new StoredObject(path, data.length);
        }

        @Override
        public Resource load(String storagePath) throws IOException {
            byte[] data = store.get(storagePath);
            if (data == null) {
                throw new IOException("not found");
            }
            return new ByteArrayResource(data);
        }

        @Override
        public void delete(String storagePath) {
            store.remove(storagePath);
        }
    }

    private byte[] createZip(Map<String, byte[]> entries) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(out)) {
                for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                    zip.putNextEntry(new ZipEntry(entry.getKey()));
                    zip.write(entry.getValue());
                    zip.closeEntry();
                }
            }
            return out.toByteArray();
        }
        catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    static class TestApplication {
    }
}
