package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.example.server.notice.NoticeAudience;
import com.example.server.notice.NoticeService;
import com.example.server.notice.NoticeSeverity;
import com.example.server.notice.dto.NoticeResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoticeController 테스트")
class NoticeControllerTest {

    @Mock
    private NoticeService noticeService;

    @InjectMocks
    private NoticeController controller;

    @Test
    @DisplayName("Given 게시된 공지 When 조회하면 Then NoticeResponse 리스트를 반환한다")
    void givenPublishedNotices_whenQuerying_thenReturnResponses() {
        NoticeResponse responseDto = new NoticeResponse(
                java.util.UUID.randomUUID(),
                "2024-0001",
                "테스트 공지",
                "내용",
                NoticeSeverity.INFO,
                NoticeAudience.GLOBAL,
                true,
                OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                null);
        given(noticeService.listActiveNotices(NoticeAudience.GLOBAL)).willReturn(List.of(responseDto));

        List<NoticeResponse> response = controller.getNotices(NoticeAudience.GLOBAL);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).displayNumber()).isEqualTo("2024-0001");
    }
}
