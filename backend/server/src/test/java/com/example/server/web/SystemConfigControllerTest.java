package com.example.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.admin.systemconfig.dto.SystemConfigCompareResponse;
import com.example.admin.systemconfig.dto.SystemConfigDraftRequest;
import com.example.admin.systemconfig.dto.SystemConfigRevisionResponse;
import com.example.admin.systemconfig.dto.SystemConfigRootRequest;
import com.example.admin.systemconfig.dto.SystemConfigRootResponse;
import com.example.admin.systemconfig.service.SystemConfigVersioningService;
import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;
import com.example.server.web.dto.SystemConfigInfoUpdateRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("SystemConfigController 테스트")
class SystemConfigControllerTest {

    @Mock
    private SystemConfigVersioningService versioningService;

    private SystemConfigController controller;

    private static final UUID CONFIG_ID = UUID.randomUUID();
    private static final String CONFIG_CODE = "auth.settings";
    private static final String TEST_USER = "testUser";

    @BeforeEach
    void setUp() {
        controller = new SystemConfigController(versioningService);
    }

    @Nested
    @DisplayName("설정 루트 관리")
    class ConfigRootManagement {

        @Test
        @DisplayName("모든 설정 목록을 조회할 수 있다")
        void getAllConfigs() {
            // given
            var response = createRootResponse();
            given(versioningService.getAllConfigs()).willReturn(List.of(response));

            // when
            var result = controller.getAllConfigs();

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).hasSize(1);
            assertThat(result.getBody().get(0).configCode()).isEqualTo(CONFIG_CODE);
        }

        @Test
        @DisplayName("ID로 설정을 조회할 수 있다")
        void getConfig() {
            // given
            var response = createRootResponse();
            given(versioningService.getConfig(CONFIG_ID)).willReturn(response);

            // when
            var result = controller.getConfig(CONFIG_ID);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().id()).isEqualTo(CONFIG_ID);
            assertThat(result.getBody().configCode()).isEqualTo(CONFIG_CODE);
        }

        @Test
        @DisplayName("설정 코드로 설정을 조회할 수 있다")
        void getConfigByCode() {
            // given
            var response = createRootResponse();
            given(versioningService.getConfigByCode(CONFIG_CODE)).willReturn(response);

            // when
            var result = controller.getConfigByCode(CONFIG_CODE);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().configCode()).isEqualTo(CONFIG_CODE);
        }

        @Test
        @DisplayName("새 설정을 생성할 수 있다")
        void createConfig() {
            // given
            setUpAuthContext();
            var request = new SystemConfigRootRequest(CONFIG_CODE, "인증 설정", "설명", "yaml: content", true);
            var response = createRootResponse();
            given(versioningService.createConfig(eq(request), any(AuthContext.class))).willReturn(response);

            // when
            var result = controller.createConfig(request);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody().configCode()).isEqualTo(CONFIG_CODE);
        }

        @Test
        @DisplayName("설정 메타정보(이름, 설명)를 수정할 수 있다")
        void updateConfigInfo() {
            // given
            var request = new SystemConfigInfoUpdateRequest("수정된 이름", "수정된 설명");
            var response = new SystemConfigRootResponse(
                    CONFIG_ID, CONFIG_CODE, "수정된 이름", "수정된 설명",
                    OffsetDateTime.now(), OffsetDateTime.now(),
                    1, false, true
            );
            given(versioningService.updateConfigInfo(CONFIG_ID, "수정된 이름", "수정된 설명"))
                    .willReturn(response);

            // when
            var result = controller.updateConfigInfo(CONFIG_ID, request);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().name()).isEqualTo("수정된 이름");
            assertThat(result.getBody().description()).isEqualTo("수정된 설명");
        }

        @Test
        @DisplayName("설정을 수정할 수 있다")
        void updateConfig() {
            // given
            setUpAuthContext();
            var request = new SystemConfigDraftRequest("yaml: updated", true, "수정 이유");
            var response = createRevisionResponse(1);
            given(versioningService.updateConfig(eq(CONFIG_ID), eq(request), any(AuthContext.class)))
                    .willReturn(response);

            // when
            var result = controller.updateConfig(CONFIG_ID, request);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().version()).isEqualTo(1);
        }

        @Test
        @DisplayName("설정을 삭제할 수 있다")
        void deleteConfig() {
            // given
            setUpAuthContext();
            var response = createRevisionResponse(2);
            given(versioningService.deleteConfig(eq(CONFIG_ID), any(AuthContext.class))).willReturn(response);

            // when
            var result = controller.deleteConfig(CONFIG_ID);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(versioningService).deleteConfig(eq(CONFIG_ID), any(AuthContext.class));
        }

        @Test
        @DisplayName("인증 컨텍스트가 없으면 예외가 발생한다")
        void throwsExceptionWithoutAuthContext() {
            // given
            AuthContextHolder.clear();
            var request = new SystemConfigRootRequest(CONFIG_CODE, "이름", "설명", "yaml", true);

            // when & then
            assertThatThrownBy(() -> controller.createConfig(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("인증 컨텍스트가 없습니다");
        }
    }

    @Nested
    @DisplayName("버전 이력 조회")
    class VersionHistory {

        @Test
        @DisplayName("버전 이력 목록을 조회할 수 있다")
        void getVersionHistory() {
            // given
            var response = createRevisionResponse(1);
            given(versioningService.getVersionHistory(CONFIG_ID)).willReturn(List.of(response));

            // when
            var result = controller.getVersionHistory(CONFIG_ID);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).hasSize(1);
        }

        @Test
        @DisplayName("특정 버전을 조회할 수 있다")
        void getVersion() {
            // given
            var response = createRevisionResponse(3);
            given(versioningService.getVersion(CONFIG_ID, 3)).willReturn(response);

            // when
            var result = controller.getVersion(CONFIG_ID, 3);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().version()).isEqualTo(3);
        }

        @Test
        @DisplayName("특정 시점의 버전을 조회할 수 있다")
        void getVersionAsOf() {
            // given
            var asOf = OffsetDateTime.now().minusDays(1);
            var response = createRevisionResponse(2);
            given(versioningService.getVersionAsOf(CONFIG_ID, asOf)).willReturn(response);

            // when
            var result = controller.getVersionAsOf(CONFIG_ID, asOf);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().version()).isEqualTo(2);
        }

        @Test
        @DisplayName("두 버전을 비교할 수 있다")
        void compareVersions() {
            // given
            var v1 = createRevisionResponse(1);
            var v2 = createRevisionResponse(2);
            var response = new SystemConfigCompareResponse(v1, v2, "Content changed");
            given(versioningService.compareVersions(CONFIG_ID, 1, 2)).willReturn(response);

            // when
            var result = controller.compareVersions(CONFIG_ID, 1, 2);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().version1().version()).isEqualTo(1);
            assertThat(result.getBody().version2().version()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("버전 롤백")
    class VersionRollback {

        @Test
        @DisplayName("특정 버전으로 롤백할 수 있다")
        void rollbackToVersion() {
            // given
            setUpAuthContext();
            var response = createRevisionResponse(5);
            given(versioningService.rollbackToVersion(eq(CONFIG_ID), eq(2), eq("롤백 이유"), any(AuthContext.class)))
                    .willReturn(response);

            // when
            var result = controller.rollbackToVersion(CONFIG_ID, 2, "롤백 이유");

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(versioningService).rollbackToVersion(eq(CONFIG_ID), eq(2), eq("롤백 이유"), any(AuthContext.class));
        }
    }

    @Nested
    @DisplayName("Draft 워크플로우")
    class DraftWorkflow {

        @Test
        @DisplayName("초안을 조회할 수 있다")
        void getDraft() {
            // given
            var response = createRevisionResponse(3);
            given(versioningService.getDraft(CONFIG_ID)).willReturn(response);

            // when
            var result = controller.getDraft(CONFIG_ID);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("초안 존재 여부를 확인할 수 있다")
        void hasDraft() {
            // given
            given(versioningService.hasDraft(CONFIG_ID)).willReturn(true);

            // when
            var result = controller.hasDraft(CONFIG_ID);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isTrue();
        }

        @Test
        @DisplayName("초안을 저장할 수 있다")
        void saveDraft() {
            // given
            setUpAuthContext();
            var request = new SystemConfigDraftRequest("yaml: draft", false, "초안 저장");
            var response = createRevisionResponse(4);
            given(versioningService.saveDraft(eq(CONFIG_ID), eq(request), any(AuthContext.class)))
                    .willReturn(response);

            // when
            var result = controller.saveDraft(CONFIG_ID, request);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("초안과 현재 버전을 비교할 수 있다")
        void compareDraftWithCurrent() {
            // given
            var current = createRevisionResponse(2);
            var draft = createRevisionResponse(3);
            var response = new SystemConfigCompareResponse(current, draft, "Content changed");
            given(versioningService.compareDraftWithCurrent(CONFIG_ID)).willReturn(response);

            // when
            var result = controller.compareDraftWithCurrent(CONFIG_ID);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().version1().version()).isEqualTo(2);
            assertThat(result.getBody().version2().version()).isEqualTo(3);
        }

        @Test
        @DisplayName("초안을 게시할 수 있다")
        void publishDraft() {
            // given
            setUpAuthContext();
            var response = createRevisionResponse(4);
            given(versioningService.publishDraft(eq(CONFIG_ID), any(AuthContext.class))).willReturn(response);

            // when
            var result = controller.publishDraft(CONFIG_ID);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("초안을 삭제할 수 있다")
        void discardDraft() {
            // when
            var result = controller.discardDraft(CONFIG_ID);

            // then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(versioningService).discardDraft(CONFIG_ID);
        }
    }

    // === Helper Methods ===

    private void setUpAuthContext() {
        AuthContextHolder.set(AuthContext.of(TEST_USER, "ORG001", "ADMIN",
                FeatureCode.POLICY, ActionCode.UPDATE));
    }

    private SystemConfigRootResponse createRootResponse() {
        return new SystemConfigRootResponse(
                CONFIG_ID, CONFIG_CODE, "인증 설정", "인증 관련 설정",
                OffsetDateTime.now(), OffsetDateTime.now(),
                1, false, true
        );
    }

    private SystemConfigRevisionResponse createRevisionResponse(int versionNumber) {
        return new SystemConfigRevisionResponse(
                UUID.randomUUID(), CONFIG_ID, CONFIG_CODE, versionNumber,
                OffsetDateTime.now(), null,
                "yaml: content", true, VersionStatus.PUBLISHED,
                ChangeAction.UPDATE, "변경 사유",
                TEST_USER, "테스트 사용자", OffsetDateTime.now(),
                null, null
        );
    }
}
