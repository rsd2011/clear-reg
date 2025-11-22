package com.example.dwgateway.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.example.common.file.FileDownload;
import com.example.common.file.FileMetadataDto;
import com.example.common.file.FileStatus;
import com.example.file.port.FileManagementPort;
import com.example.file.FileUploadCommand;

@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(FileController.class)
class FileControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    FileManagementPort fileManagementPort;

    @MockBean
    com.example.auth.permission.RequirePermissionAspect requirePermissionAspect;


    private static FileMetadataDto sampleMeta() {
        return new FileMetadataDto(UUID.randomUUID(), "file.txt", MediaType.TEXT_PLAIN_VALUE, 4L, "chk",
                "user", FileStatus.ACTIVE, null, OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    @DisplayName("파일 목록이 비어 있으면 빈 배열을 반환한다")
    void listReturnsEmpty() throws Exception {
        when(fileManagementPort.list()).thenReturn(List.of());

        var mvcResult = mockMvc.perform(get("/api/files"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString()).isEqualTo("[]");
    }

    @Test
    @DisplayName("메타데이터 조회가 존재하지 않을 때 404를 반환하지 않고 그대로 예외 위임한다")
    void metadataDelegates() throws Exception {
        FileMetadataDto dto = sampleMeta();
        when(fileManagementPort.getMetadata(dto.id())).thenReturn(dto);

        var resp = mockMvc.perform(get("/api/files/{id}", dto.id()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(resp).contains("file.txt");
    }

    @Test
    @DisplayName("다운로드 시 Content-Disposition과 길이/타입 헤더를 설정한다")
    void downloadSetsHeaders() throws Exception {
        FileMetadataDto meta = sampleMeta();
        FileDownload download = new FileDownload(meta, new ByteArrayResource("data".getBytes()));
        when(fileManagementPort.download(any(), any())).thenReturn(download);

        var response = mockMvc.perform(get("/api/files/{id}/content", meta.id()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        assertThat(response.getHeader("Content-Disposition")).contains("file.txt");
        assertThat(response.getHeader("Content-Length")).isEqualTo("4");
        assertThat(response.getContentType()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
    }

    @Test
    @DisplayName("파일 업로드가 성공하면 포트의 반환값을 그대로 전달한다")
    void uploadSuccess() throws Exception {
        FileMetadataDto meta = sampleMeta();
        when(fileManagementPort.upload(any(FileUploadCommand.class))).thenReturn(meta);

        MockMultipartHttpServletRequestBuilder builder = multipart("/api/files");
        builder.file("file", "hello".getBytes(StandardCharsets.UTF_8));

        var response = mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(response).contains("file.txt");
    }

    @Test
    @DisplayName("파일 삭제 시 포트 예외를 전파한다")
    void deleteFails() {
        UUID id = UUID.randomUUID();
        Mockito.doThrow(new IllegalStateException("fail")).when(fileManagementPort).delete(Mockito.eq(id), any());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> fileManagementPort.delete(id, "actor"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("메타데이터 조회 실패 시 예외를 전파한다")
    void metadataFailure() {
        UUID id = UUID.randomUUID();
        when(fileManagementPort.getMetadata(id)).thenThrow(new RuntimeException("boom"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> fileManagementPort.getMetadata(id))
                .isInstanceOf(RuntimeException.class);
    }

}
