package com.example.admin.rowaccesspolicy.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.rowaccesspolicy.domain.RowAccessPolicy;
import com.example.admin.rowaccesspolicy.domain.RowAccessPolicyRoot;
import com.example.common.security.RowScope;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

@DisplayName("RowAccessPolicy DTO 테스트")
class RowAccessPolicyDtoTest {

    private RowAccessPolicyRoot createTestRoot() {
        return RowAccessPolicyRoot.create(OffsetDateTime.now());
    }

    private RowAccessPolicy createTestVersion(RowAccessPolicyRoot root) {
        return RowAccessPolicy.create(
                root, 1, "테스트 정책", "테스트 설명",
                FeatureCode.DRAFT, ActionCode.READ, "ADMIN", "ORG1",
                RowScope.OWN, 100, true,
                Instant.now(), Instant.now().plusSeconds(86400),
                ChangeAction.CREATE, null, "tester", "테스터",
                OffsetDateTime.now());
    }

    @Nested
    @DisplayName("RowAccessPolicyRootResponse")
    class RootResponseTest {

        @Test
        @DisplayName("Given: Root와 currentVersion / When: from 호출 / Then: 올바른 응답 생성")
        void fromCreatesCorrectResponse() {
            RowAccessPolicyRoot root = createTestRoot();
            RowAccessPolicy version = createTestVersion(root);
            root.activateNewVersion(version, OffsetDateTime.now());

            RowAccessPolicyRootResponse response = RowAccessPolicyRootResponse.from(root);

            assertThat(response.id()).isEqualTo(root.getId());
            assertThat(response.name()).isEqualTo("테스트 정책");
            assertThat(response.featureCode()).isEqualTo(FeatureCode.DRAFT);
            assertThat(response.actionCode()).isEqualTo(ActionCode.READ);
            assertThat(response.rowScope()).isEqualTo(RowScope.OWN);
            assertThat(response.priority()).isEqualTo(100);
            assertThat(response.active()).isTrue();
            assertThat(response.currentVersion()).isEqualTo(1);
            assertThat(response.hasDraft()).isFalse();
        }

        @Test
        @DisplayName("Given: currentVersion이 없는 Root / When: from 호출 / Then: null 필드 처리")
        void fromHandlesNullCurrentVersion() {
            RowAccessPolicyRoot root = createTestRoot();

            RowAccessPolicyRootResponse response = RowAccessPolicyRootResponse.from(root);

            assertThat(response.id()).isEqualTo(root.getId());
            assertThat(response.featureCode()).isNull();
            assertThat(response.actionCode()).isNull();
            assertThat(response.permGroupCode()).isNull();
            assertThat(response.currentVersion()).isNull();
        }

        @Test
        @DisplayName("Given: 마스커 함수 / When: from 호출 / Then: 마스킹 적용")
        void fromAppliesMasker() {
            RowAccessPolicyRoot root = createTestRoot();
            RowAccessPolicy version = createTestVersion(root);
            root.activateNewVersion(version, OffsetDateTime.now());

            UnaryOperator<String> masker = s -> s == null ? null : "***";
            RowAccessPolicyRootResponse response = RowAccessPolicyRootResponse.from(root, masker);

            assertThat(response.name()).isEqualTo("***");
            assertThat(response.description()).isEqualTo("***");
            assertThat(response.permGroupCode()).isEqualTo("***");
        }

        @Test
        @DisplayName("Given: null 마스커 / When: from 호출 / Then: identity 함수 사용")
        void fromWithNullMaskerUsesIdentity() {
            RowAccessPolicyRoot root = createTestRoot();
            RowAccessPolicy version = createTestVersion(root);
            root.activateNewVersion(version, OffsetDateTime.now());

            RowAccessPolicyRootResponse response = RowAccessPolicyRootResponse.from(root, null);

            assertThat(response.name()).isEqualTo("테스트 정책");
        }
    }

    @Nested
    @DisplayName("RowAccessPolicyHistoryResponse")
    class HistoryResponseTest {

        @Test
        @DisplayName("Given: Version 엔티티 / When: from 호출 / Then: 올바른 이력 응답 생성")
        void fromCreatesCorrectHistoryResponse() {
            RowAccessPolicyRoot root = createTestRoot();
            RowAccessPolicy version = createTestVersion(root);
            root.activateNewVersion(version, OffsetDateTime.now());

            RowAccessPolicyHistoryResponse response = RowAccessPolicyHistoryResponse.from(version);

            assertThat(response.id()).isEqualTo(version.getId());
            assertThat(response.policyId()).isEqualTo(root.getId());
            assertThat(response.version()).isEqualTo(1);
            assertThat(response.name()).isEqualTo("테스트 정책");
            assertThat(response.featureCode()).isEqualTo(FeatureCode.DRAFT);
            assertThat(response.actionCode()).isEqualTo(ActionCode.READ);
            assertThat(response.permGroupCode()).isEqualTo("ADMIN");
            assertThat(response.orgGroupCode()).isEqualTo("ORG1");
            assertThat(response.rowScope()).isEqualTo(RowScope.OWN);
            assertThat(response.priority()).isEqualTo(100);
            assertThat(response.active()).isTrue();
            assertThat(response.status()).isEqualTo(VersionStatus.PUBLISHED);
            assertThat(response.changeAction()).isEqualTo(ChangeAction.CREATE);
            assertThat(response.changedBy()).isEqualTo("tester");
            assertThat(response.changedByName()).isEqualTo("테스터");
        }

        @Test
        @DisplayName("Given: 마스커 함수 / When: from 호출 / Then: 마스킹 적용")
        void fromAppliesMasker() {
            RowAccessPolicyRoot root = createTestRoot();
            RowAccessPolicy version = createTestVersion(root);
            root.activateNewVersion(version, OffsetDateTime.now());

            UnaryOperator<String> masker = s -> s == null ? null : "[MASKED]";
            RowAccessPolicyHistoryResponse response = RowAccessPolicyHistoryResponse.from(version, masker);

            assertThat(response.name()).isEqualTo("[MASKED]");
            assertThat(response.description()).isEqualTo("[MASKED]");
            assertThat(response.permGroupCode()).isEqualTo("[MASKED]");
            assertThat(response.changedBy()).isEqualTo("[MASKED]");
        }
    }

    @Nested
    @DisplayName("RowAccessPolicyRootRequest")
    class RootRequestTest {

        @Test
        @DisplayName("Given: 유효한 파라미터 / When: 생성 / Then: 올바른 요청 객체 생성")
        void createsValidRequest() {
            RowAccessPolicyRootRequest request = new RowAccessPolicyRootRequest(
                    "정책명", "설명",
                    FeatureCode.DRAFT, ActionCode.READ,
                    "ADMIN", "ORG1",
                    RowScope.OWN, 100, true,
                    Instant.now(), Instant.now().plusSeconds(86400));

            assertThat(request.name()).isEqualTo("정책명");
            assertThat(request.description()).isEqualTo("설명");
            assertThat(request.featureCode()).isEqualTo(FeatureCode.DRAFT);
            assertThat(request.actionCode()).isEqualTo(ActionCode.READ);
            assertThat(request.permGroupCode()).isEqualTo("ADMIN");
            assertThat(request.orgGroupCode()).isEqualTo("ORG1");
            assertThat(request.rowScope()).isEqualTo(RowScope.OWN);
            assertThat(request.priority()).isEqualTo(100);
            assertThat(request.active()).isTrue();
        }

        @Test
        @DisplayName("Given: null 값 / When: 생성 / Then: null 허용 필드는 null, priority는 기본값 100")
        void allowsNullFieldsAndDefaultPriority() {
            RowAccessPolicyRootRequest request = new RowAccessPolicyRootRequest(
                    "정책명", null,
                    FeatureCode.DRAFT, null,
                    null, null,
                    RowScope.ALL, null, true,
                    null, null);

            assertThat(request.name()).isEqualTo("정책명");
            assertThat(request.description()).isNull();
            assertThat(request.actionCode()).isNull();
            assertThat(request.permGroupCode()).isNull();
            assertThat(request.orgGroupCode()).isNull();
            // priority가 null로 전달되면 컴팩트 생성자에서 기본값 100으로 설정
            assertThat(request.priority()).isEqualTo(100);
            assertThat(request.effectiveFrom()).isNull();
            assertThat(request.effectiveTo()).isNull();
        }
    }

    @Nested
    @DisplayName("RowAccessPolicyDraftRequest")
    class DraftRequestTest {

        @Test
        @DisplayName("Given: 유효한 파라미터 / When: 생성 / Then: 올바른 초안 요청 객체 생성")
        void createsValidDraftRequest() {
            RowAccessPolicyDraftRequest request = new RowAccessPolicyDraftRequest(
                    "초안 정책", "초안 설명",
                    FeatureCode.CUSTOMER, ActionCode.UPDATE,
                    "EDITOR", "ORG2",
                    RowScope.ORG, 50, true,
                    Instant.now(), Instant.now().plusSeconds(86400),
                    "변경 사유");

            assertThat(request.name()).isEqualTo("초안 정책");
            assertThat(request.description()).isEqualTo("초안 설명");
            assertThat(request.featureCode()).isEqualTo(FeatureCode.CUSTOMER);
            assertThat(request.actionCode()).isEqualTo(ActionCode.UPDATE);
            assertThat(request.permGroupCode()).isEqualTo("EDITOR");
            assertThat(request.orgGroupCode()).isEqualTo("ORG2");
            assertThat(request.rowScope()).isEqualTo(RowScope.ORG);
            assertThat(request.priority()).isEqualTo(50);
            assertThat(request.active()).isTrue();
            assertThat(request.changeReason()).isEqualTo("변경 사유");
        }

        @Test
        @DisplayName("Given: null changeReason / When: 생성 / Then: 정상 생성")
        void allowsNullChangeReason() {
            RowAccessPolicyDraftRequest request = new RowAccessPolicyDraftRequest(
                    "초안", null,
                    FeatureCode.DRAFT, null,
                    null, null,
                    RowScope.OWN, 100, true,
                    null, null,
                    null);

            assertThat(request.changeReason()).isNull();
        }
    }
}
