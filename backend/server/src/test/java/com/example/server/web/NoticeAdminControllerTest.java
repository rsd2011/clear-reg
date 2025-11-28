package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.security.RowScope;
import com.example.server.notice.NoticeAudience;
import com.example.server.notice.NoticeService;
import com.example.server.notice.NoticeSeverity;
import com.example.server.notice.NoticeStatus;
import com.example.server.notice.dto.NoticeAdminResponse;
import com.example.server.notice.dto.NoticeCreateRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoticeAdminController 테스트")
class NoticeAdminControllerTest {

    @Mock
    private NoticeService noticeService;

    @InjectMocks
    private NoticeAdminController controller;

    @BeforeEach
    void setUp() {
        AuthContextHolder.set(AuthContext.of("tester", "ORG", "DEFAULT",
                FeatureCode.NOTICE, ActionCode.UPDATE, RowScope.ALL));
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    @DisplayName("Given 공지 생성 요청 When create 호출 Then 서비스에서 생성 후 응답을 반환한다")
    void givenCreateRequest_whenCreating_thenReturnResponse() {
        NoticeAdminResponse sample = sampleResponse();
        given(noticeService.createNotice(ArgumentMatchers.any(), ArgumentMatchers.eq("tester")))
                .willReturn(sample);
        NoticeCreateRequest request = new NoticeCreateRequest(
                "제목",
                "내용",
                NoticeSeverity.INFO,
                NoticeAudience.GLOBAL,
                null,
                null,
                true);

        NoticeAdminResponse response = controller.createNotice(request);

        assertThat(response.displayNumber()).isEqualTo("2024-0001");
        verify(noticeService).createNotice(request, "tester");
    }

    @Test
    @DisplayName("Given 공지 리스트 When 조회하면 Then 관리자용 목록을 반환한다")
    void givenAdminList_whenListing_thenReturnList() {
        NoticeAdminResponse sample = sampleResponse();
        given(noticeService.listNotices()).willReturn(List.of(sample));

        List<NoticeAdminResponse> response = controller.listNotices();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).status()).isEqualTo(NoticeStatus.PUBLISHED);
    }

    private NoticeAdminResponse sampleResponse() {
        OffsetDateTime now = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        return new NoticeAdminResponse(
                java.util.UUID.randomUUID(),
                "2024-0001",
                "테스트",
                "내용",
                NoticeSeverity.WARNING,
                NoticeAudience.ADMIN,
                NoticeStatus.PUBLISHED,
                true,
                now,
                now.plusDays(1),
                now,
                now,
                "tester",
                "tester");
    }
}
