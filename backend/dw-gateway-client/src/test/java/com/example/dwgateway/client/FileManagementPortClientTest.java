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
import org.springframework.http.HttpStatus;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.core.io.InputStreamResource;
import org.springframework.test.util.ReflectionTestUtils;

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
    void uploadFailsOnServerError() {
        server.expect(requestTo("http://localhost/api/files"))
                .andRespond(MockRestResponseCreators.withServerError());

        assertThatThrownBy(() -> client.upload(uploadCommand("err.bin")))
                .isInstanceOf(DwGatewayClientException.class)
                .hasMessageContaining("upload");
    }

    @Test
    void uploadFailsWhenInputStreamErrors() {
        FileUploadCommand badCommand = new FileUploadCommand(
                "bad.txt", MediaType.TEXT_PLAIN_VALUE, 1,
                () -> { throw new RuntimeException("io failure"); },
                null, "tester");

        assertThatThrownBy(() -> client.upload(badCommand))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("io");
    }

    @Test
    void uploadFailsWhenRestClientError() {
        RestTemplate local = new RestTemplateBuilder().rootUri("http://localhost").build();
        MockRestServiceServer localServer = MockRestServiceServer.bindTo(local).ignoreExpectOrder(true).build();
        FileManagementPort localClient = new FileManagementPortClient(local,
                RetryTemplate.builder().maxAttempts(1).fixedBackoff(10).build());

        localServer.expect(requestTo("http://localhost/api/files"))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> localClient.upload(uploadCommand("fail.bin")))
                .isInstanceOf(DwGatewayClientException.class)
                .hasMessageContaining("upload");
    }

    @Test
    void uploadFailsOnNonOkStatus() {
        server.expect(requestTo("http://localhost/api/files"))
                .andRespond(MockRestResponseCreators.withStatus(org.springframework.http.HttpStatus.ACCEPTED));

        assertThatThrownBy(() -> client.upload(uploadCommand("foo.bin")))
                .isInstanceOf(DwGatewayClientException.class)
                .hasMessageContaining("upload");
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
    void listReturnsEmptyWhenNoContent() {
        server.expect(requestTo("http://localhost/api/files"))
                .andRespond(MockRestResponseCreators.withSuccess("[]", MediaType.APPLICATION_JSON));

        List<FileMetadataDto> files = client.list();

        assertThat(files).isEmpty();
    }


    @Test
    void getMetadataReturnsEntry() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo("http://localhost/api/files/" + id))
                .andRespond(MockRestResponseCreators.withSuccess(sampleMetadataJson("notes.txt"), MediaType.APPLICATION_JSON));

        assertThat(client.getMetadata(id).originalName()).isEqualTo("notes.txt");
    }

    @Test
    void getMetadataThrowsWhenBodyMissing() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo("http://localhost/api/files/" + id))
                .andRespond(MockRestResponseCreators.withSuccess());

        assertThatThrownBy(() -> client.getMetadata(id))
                .isInstanceOf(DwGatewayClientException.class)
                .hasMessageContaining("response was empty");
    }

    @Test
    void getMetadataThrowsOn404() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo("http://localhost/api/files/" + id))
                .andRespond(MockRestResponseCreators.withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getMetadata(id))
                .isInstanceOf(DwGatewayClientException.class)
                .hasMessageContaining("Failed to fetch file metadata");
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

    @Test
    void deleteThrowsWhenRestCallFails() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo("http://localhost/api/files/" + id))
                .andExpect(method(org.springframework.http.HttpMethod.DELETE))
                .andRespond(MockRestResponseCreators.withServerError());

        assertThatThrownBy(() -> client.delete(id, "tester"))
                .isInstanceOf(DwGatewayClientException.class)
                .hasMessageContaining("delete");
    }

    @Test
    void downloadFailsWhenContentMissing() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo("http://localhost/api/files/" + id))
                .andRespond(MockRestResponseCreators.withSuccess(sampleMetadataJson("archive.zip"), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost/api/files/" + id + "/content"))
                .andRespond(MockRestResponseCreators.withSuccess());

        assertThatThrownBy(() -> client.download(id, "tester"))
                .isInstanceOf(DwGatewayClientException.class)
                .hasMessageContaining("payload was empty");
    }

    @Test
    void downloadFailsWhenStatusIsNotActive() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo("http://localhost/api/files/" + id))
                .andRespond(MockRestResponseCreators.withSuccess(sampleMetadataJsonWithStatus(id, "archive.zip", FileStatus.DELETED.name()), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost/api/files/" + id + "/content"))
                .andRespond(MockRestResponseCreators.withSuccess("payload", MediaType.APPLICATION_OCTET_STREAM));

        assertThatThrownBy(() -> client.download(id, "tester"))
                .isInstanceOf(DwGatewayClientException.class)
                .hasMessageContaining("status");
    }

    @Test
    void downloadFailsWhenRestErrorDuringContentLoad() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo("http://localhost/api/files/" + id))
                .andRespond(MockRestResponseCreators.withSuccess(sampleMetadataJson("archive.zip"), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost/api/files/" + id + "/content"))
                .andRespond(MockRestResponseCreators.withServerError());

        assertThatThrownBy(() -> client.download(id, "tester"))
                .isInstanceOf(DwGatewayClientException.class)
                .hasMessageContaining("download");
    }

    @Test
    void listThrowsWhenRestError() {
        server.expect(requestTo("http://localhost/api/files"))
                .andRespond(MockRestResponseCreators.withServerError());

        assertThatThrownBy(() -> client.list())
                .isInstanceOf(DwGatewayClientException.class)
                .hasMessageContaining("list");
    }

    @Test
    void buildFileResourceExposesFilenameAndLength() throws Exception {
        FileUploadCommand command = uploadCommand("data.txt");

        @SuppressWarnings("unchecked")
        HttpEntity<InputStreamResource> entity = ReflectionTestUtils.invokeMethod(client, "buildFileResource", command);

        InputStreamResource resource = entity.getBody();
        assertThat(resource.getFilename()).isEqualTo("data.txt");
        assertThat(resource.contentLength()).isEqualTo(4);
        assertThat(resource.getDescription()).contains("data.txt");
    }

    @Test
    void deleteThrowsWhenBodyMissing() {
        UUID id = UUID.randomUUID();
        server.expect(requestTo("http://localhost/api/files/" + id))
                .andRespond(MockRestResponseCreators.withSuccess());

        assertThatThrownBy(() -> client.delete(id, "tester"))
                .isInstanceOf(DwGatewayClientException.class)
                .hasMessageContaining("response body was empty");
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

    private String sampleMetadataJsonWithStatus(UUID id, String filename, String status) {
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
                """.formatted(id, filename, status);
    }
}
