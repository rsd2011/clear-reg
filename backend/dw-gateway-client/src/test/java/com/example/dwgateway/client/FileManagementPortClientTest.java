package com.example.dwgateway.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import com.example.common.file.FileDownload;
import com.example.common.file.FileMetadataDto;
import com.example.common.file.FileStatus;
import com.example.dwgateway.client.DwGatewayClientException;
import com.example.file.FileUploadCommand;
import com.example.file.port.FileManagementPort;

class FileManagementPortClientTest {

    private MockRestServiceServer server;
    private FileManagementPort client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplateBuilder()
                .rootUri("http://localhost")
                .build();
        this.server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
        this.client = new FileManagementPortClient(restTemplate,
                RetryTemplate.builder().maxAttempts(1).fixedBackoff(10).build());
    }

    @Test
    void uploadReturnsMetadata() {
        server.expect(requestTo("http://localhost/api/files"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withSuccess(sampleMetadataJson("doc.pdf"), MediaType.APPLICATION_JSON));

        FileMetadataDto metadata = client.upload(uploadCommand("doc.pdf"));

        assertThat(metadata.originalName()).isEqualTo("doc.pdf");
    }

    @Test
    void uploadThrowsWhenBodyMissing() {
        server.expect(requestTo("http://localhost/api/files"))
                .andRespond(MockRestResponseCreators.withSuccess());

        assertThatThrownBy(() -> client.upload(uploadCommand("doc.pdf")))
                .isInstanceOf(DwGatewayClientException.class)
                .hasMessageContaining("response body was empty");
    }

    @Test
    void listReturnsResponse() {
        server.expect(requestTo("http://localhost/api/files"))
                .andRespond(MockRestResponseCreators.withSuccess("""
                        [
                          %s
                        ]
                        """.formatted(sampleMetadataJson("invoice.csv")), MediaType.APPLICATION_JSON));

        List<FileMetadataDto> files = client.list();

        assertThat(files).hasSize(1);
        assertThat(files.getFirst().originalName()).isEqualTo("invoice.csv");
    }

    @Test
    void getMetadataReturnsEntry() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo("http://localhost/api/files/" + id))
                .andRespond(MockRestResponseCreators.withSuccess(sampleMetadataJson("notes.txt"), MediaType.APPLICATION_JSON));

        assertThat(client.getMetadata(id).originalName()).isEqualTo("notes.txt");
    }

    @Test
    void downloadFetchesMetadataAndContent() throws Exception {
        UUID id = UUID.randomUUID();
        server.expect(requestTo("http://localhost/api/files/" + id))
                .andRespond(MockRestResponseCreators.withSuccess(sampleMetadataJson("archive.zip"), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost/api/files/" + id + "/content"))
                .andRespond(MockRestResponseCreators.withSuccess("payload", MediaType.APPLICATION_OCTET_STREAM));

        FileDownload download = client.download(id, "tester");

        assertThat(download.metadata().originalName()).isEqualTo("archive.zip");
        Resource resource = download.resource();
        assertThat(new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("payload");
    }

    @Test
    void deleteReturnsMetadata() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo("http://localhost/api/files/" + id))
                .andExpect(method(org.springframework.http.HttpMethod.DELETE))
                .andRespond(MockRestResponseCreators.withSuccess(sampleMetadataJson("report.csv"), MediaType.APPLICATION_JSON));

        assertThat(client.delete(id, "tester").originalName()).isEqualTo("report.csv");
    }

    private FileUploadCommand uploadCommand(String filename) {
        return new FileUploadCommand(
                filename,
                MediaType.APPLICATION_PDF_VALUE,
                4,
                () -> new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8)),
                OffsetDateTime.now().plusDays(1),
                "tester"
        );
    }

    private String sampleMetadataJson(String filename) {
        return """
                {
                  "id": "%s",
                  "originalName": "%s",
                  "contentType": "application/octet-stream",
                  "size": 10,
                  "checksum": "abc",
                  "status": "%s",
                  "createdAt": "2024-01-01T00:00:00Z",
                  "updatedAt": "2024-01-01T00:00:00Z"
                }
                """.formatted(UUID.randomUUID(), filename, FileStatus.ACTIVE.name());
    }
}
