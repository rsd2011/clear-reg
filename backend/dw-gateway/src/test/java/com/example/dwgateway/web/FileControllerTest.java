package com.example.dwgateway.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.example.auth.permission.ActionCode;
import com.example.auth.permission.FeatureCode;
import com.example.auth.permission.context.AuthContext;
import com.example.auth.permission.context.AuthContextHolder;
import com.example.common.file.FileMetadataDto;
import com.example.common.file.FileStatus;
import com.example.file.FileUploadCommand;
import com.example.file.port.FileManagementPort;

@WebMvcTest(FileController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@DisplayName("dw-gateway FileController 테스트")
class FileControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    FileManagementPort fileManagementPort;

    @BeforeEach
    void setUp() {
        AuthContextHolder.set(new AuthContext("tester", "ORG", "DEFAULT", FeatureCode.FILE, ActionCode.READ, null, java.util.Map.of()));
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("파일 목록을 반환한다")
    void listFiles() throws Exception {
        FileMetadataDto metadata = sampleMetadata();
        given(fileManagementPort.list()).willReturn(List.of(metadata));

        String response = mockMvc.perform(get("/api/files"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(response).contains("hello.txt");
    }

    @Test
    @DisplayName("파일을 업로드한다")
    void uploadFile() throws Exception {
        AuthContextHolder.set(new AuthContext("tester", "ORG", "DEFAULT", FeatureCode.FILE, ActionCode.UPLOAD, null, java.util.Map.of()));
        MockMultipartFile multipartFile = new MockMultipartFile("file", "hello.txt", "text/plain", "data".getBytes());
        FileMetadataDto metadata = sampleMetadata();
        given(fileManagementPort.upload(any(FileUploadCommand.class))).willReturn(metadata);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/files")
                        .file(multipartFile)
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isOk());
    }

    private FileMetadataDto sampleMetadata() {
        OffsetDateTime now = OffsetDateTime.now();
        return new FileMetadataDto(
                UUID.randomUUID(),
                "hello.txt",
                "text/plain",
                4,
                "abcd",
                "tester",
                FileStatus.ACTIVE,
                null,
                now,
                now);
    }
}
