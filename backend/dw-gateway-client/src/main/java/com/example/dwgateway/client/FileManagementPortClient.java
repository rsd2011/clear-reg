package com.example.dwgateway.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.common.file.FileDownload;
import com.example.common.file.FileMetadataDto;
import com.example.dwgateway.client.DwGatewayClientException;
import com.example.file.FileUploadCommand;
import com.example.file.api.FileUploadRequest;
import com.example.file.port.FileManagementPort;

/**
 * HTTP client implementation of {@link FileManagementPort} backed by the dw-gateway REST endpoints.
 */
public class FileManagementPortClient implements FileManagementPort {

    private static final ParameterizedTypeReference<List<FileMetadataDto>> LIST_TYPE =
            new ParameterizedTypeReference<>() { };
    private static final String FILES_PATH = "/api/files";

    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;

    public FileManagementPortClient(RestTemplate restTemplate, RetryTemplate retryTemplate) {
        this.restTemplate = restTemplate;
        this.retryTemplate = retryTemplate;
    }

    @Override
    public FileMetadataDto upload(FileUploadCommand command) {
        try {
            MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
            multipartBody.add("file", buildFileResource(command));
            if (command.retentionUntil() != null) {
                HttpHeaders metadataHeaders = new HttpHeaders();
                metadataHeaders.setContentType(MediaType.APPLICATION_JSON);
                multipartBody.add("metadata", new HttpEntity<>(new FileUploadRequest(command.retentionUntil()), metadataHeaders));
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(multipartBody, headers);

            FileMetadataDto metadata = retryTemplate.execute(context ->
                    restTemplate.postForObject(FILES_PATH, requestEntity, FileMetadataDto.class));
            if (metadata == null) {
                throw new DwGatewayClientException("File upload response body was empty");
            }
            return metadata;
        }
        catch (RestClientException ex) {
            throw new DwGatewayClientException("Failed to upload file", ex);
        }
    }

    @Override
    public List<FileMetadataDto> list() {
        try {
            ResponseEntity<List<FileMetadataDto>> response = retryTemplate.execute(context ->
                    restTemplate.exchange(FILES_PATH, HttpMethod.GET, HttpEntity.EMPTY, LIST_TYPE));
            List<FileMetadataDto> body = response.getBody();
            return body != null ? body : List.of();
        }
        catch (RestClientException ex) {
            throw new DwGatewayClientException("Failed to list files", ex);
        }
    }

    @Override
    public FileMetadataDto getMetadata(UUID id) {
        try {
            FileMetadataDto metadata = retryTemplate.execute(context ->
                    restTemplate.getForObject(FILES_PATH + "/" + id, FileMetadataDto.class));
            if (metadata == null) {
                throw new DwGatewayClientException("File metadata response was empty");
            }
            return metadata;
        }
        catch (RestClientException ex) {
            throw new DwGatewayClientException("Failed to fetch file metadata", ex);
        }
    }

    @Override
    public FileDownload download(UUID id, String actor) {
        // actor is captured via dw-gateway authentication headers; not part of the REST payload
        FileMetadataDto metadata = getMetadata(id);
        try {
            ResponseEntity<Resource> response = retryTemplate.execute(context ->
                    restTemplate.exchange(FILES_PATH + "/" + id + "/content", HttpMethod.GET, HttpEntity.EMPTY, Resource.class));
            Resource resource = response.getBody();
            if (resource == null) {
                throw new DwGatewayClientException("File download payload was empty");
            }
            return new FileDownload(metadata, resource);
        }
        catch (RestClientException ex) {
            throw new DwGatewayClientException("Failed to download file", ex);
        }
    }

    @Override
    public FileMetadataDto delete(UUID id, String actor) {
        try {
            ResponseEntity<FileMetadataDto> response = retryTemplate.execute(context ->
                    restTemplate.exchange(FILES_PATH + "/" + id, HttpMethod.DELETE, HttpEntity.EMPTY, FileMetadataDto.class));
            FileMetadataDto metadata = response.getBody();
            if (metadata == null) {
                throw new DwGatewayClientException("File delete response body was empty");
            }
            return metadata;
        }
        catch (RestClientException ex) {
            throw new DwGatewayClientException("Failed to delete file", ex);
        }
    }

    private HttpEntity<InputStreamResource> buildFileResource(FileUploadCommand command) {
        InputStream inputStream = command.inputStreamSupplier().get();
        InputStreamResource resource = new InputStreamResource(inputStream) {
            @Override
            public String getFilename() {
                return command.originalName();
            }

            @Override
            public long contentLength() throws IOException {
                return command.size() >= 0 ? command.size() : super.contentLength();
            }

            @Override
            public String getDescription() {
                return "FileUploadCommand resource for " + command.originalName();
            }
        };
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(command.contentType() != null ? MediaType.parseMediaType(command.contentType()) : MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("file", command.originalName());
        return new HttpEntity<>(resource, headers);
    }
}
