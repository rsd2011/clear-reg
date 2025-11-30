package com.example.admin.draft.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.admin.approval.domain.ApprovalTemplateRoot;

@DisplayName("DraftTemplatePreset 엔티티")
class DraftTemplatePresetTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2025, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC);

    private DraftFormTemplate createFormTemplate() {
        return DraftFormTemplate.create("테스트 양식", "GENERAL", "ORG1", "{}", NOW);
    }

    private ApprovalTemplateRoot createApprovalTemplate() {
        return ApprovalTemplateRoot.create(NOW);
    }

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("Given: 조직코드가 있으면 When: 생성하면 Then: ORGANIZATION 범위로 생성된다")
        void createsWithOrganizationScope() {
            DraftFormTemplate formTemplate = createFormTemplate();
            ApprovalTemplateRoot approvalTemplate = createApprovalTemplate();

            DraftTemplatePreset preset = DraftTemplatePreset.create(
                    "테스트 프리셋",
                    "GENERAL",
                    "ORG1",
                    "제목: {{date}}",
                    "내용 템플릿",
                    formTemplate,
                    approvalTemplate,
                    "{\"field\":\"value\"}",
                    "{\"var1\":\"val1\"}",
                    true,
                    NOW);

            assertThat(preset.getName()).isEqualTo("테스트 프리셋");
            assertThat(preset.getBusinessFeatureCode()).isEqualTo("GENERAL");
            assertThat(preset.getOrganizationCode()).isEqualTo("ORG1");
            assertThat(preset.getScope()).isEqualTo(TemplateScope.ORGANIZATION);
            assertThat(preset.getTitleTemplate()).isEqualTo("제목: {{date}}");
            assertThat(preset.getContentTemplate()).isEqualTo("내용 템플릿");
            assertThat(preset.getFormTemplate()).isEqualTo(formTemplate);
            assertThat(preset.getDefaultApprovalTemplate()).isEqualTo(approvalTemplate);
            assertThat(preset.getDefaultFormPayload()).isEqualTo("{\"field\":\"value\"}");
            assertThat(preset.getVariablesJson()).isEqualTo("{\"var1\":\"val1\"}");
            assertThat(preset.getVersion()).isEqualTo(1);
            assertThat(preset.isActive()).isTrue();
            assertThat(preset.getPresetCode()).isNotNull();
            assertThat(preset.getCreatedAt()).isEqualTo(NOW);
            assertThat(preset.getUpdatedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("Given: 조직코드가 null이면 When: 생성하면 Then: GLOBAL 범위로 생성된다")
        void createsWithGlobalScope() {
            DraftFormTemplate formTemplate = createFormTemplate();

            DraftTemplatePreset preset = DraftTemplatePreset.create(
                    "전역 프리셋",
                    "GENERAL",
                    null,
                    "제목",
                    "내용",
                    formTemplate,
                    null,
                    null,
                    null,
                    true,
                    NOW);

            assertThat(preset.getOrganizationCode()).isNull();
            assertThat(preset.getScope()).isEqualTo(TemplateScope.GLOBAL);
            assertThat(preset.isGlobal()).isTrue();
            assertThat(preset.getDefaultFormPayload()).isEqualTo("{}");
        }

        @Test
        @DisplayName("Given: defaultFormPayload가 null이면 When: 생성하면 Then: 빈 JSON으로 설정된다")
        void setsEmptyJsonWhenPayloadNull() {
            DraftFormTemplate formTemplate = createFormTemplate();

            DraftTemplatePreset preset = DraftTemplatePreset.create(
                    "프리셋",
                    "GENERAL",
                    "ORG1",
                    "제목",
                    "내용",
                    formTemplate,
                    null,
                    null,
                    null,
                    true,
                    NOW);

            assertThat(preset.getDefaultFormPayload()).isEqualTo("{}");
        }
    }

    @Nested
    @DisplayName("수정")
    class Update {

        @Test
        @DisplayName("Given: 프리셋이 있으면 When: 수정하면 Then: 값과 버전이 변경된다")
        void updatesSuccessfully() {
            DraftFormTemplate formTemplate = createFormTemplate();
            DraftTemplatePreset preset = DraftTemplatePreset.create(
                    "원본 프리셋",
                    "GENERAL",
                    "ORG1",
                    "원본 제목",
                    "원본 내용",
                    formTemplate,
                    null,
                    "{}",
                    null,
                    true,
                    NOW);

            DraftFormTemplate newFormTemplate = DraftFormTemplate.create(
                    "새 양식", "GENERAL", "ORG1", "{}", NOW);
            ApprovalTemplateRoot newApprovalTemplate = createApprovalTemplate();
            OffsetDateTime later = NOW.plusHours(1);

            preset.update(
                    "수정된 프리셋",
                    "수정된 제목",
                    "수정된 내용",
                    newFormTemplate,
                    newApprovalTemplate,
                    "{\"new\":\"payload\"}",
                    "{\"var\":\"val\"}",
                    false,
                    later);

            assertThat(preset.getName()).isEqualTo("수정된 프리셋");
            assertThat(preset.getTitleTemplate()).isEqualTo("수정된 제목");
            assertThat(preset.getContentTemplate()).isEqualTo("수정된 내용");
            assertThat(preset.getFormTemplate()).isEqualTo(newFormTemplate);
            assertThat(preset.getDefaultApprovalTemplate()).isEqualTo(newApprovalTemplate);
            assertThat(preset.getDefaultFormPayload()).isEqualTo("{\"new\":\"payload\"}");
            assertThat(preset.getVariablesJson()).isEqualTo("{\"var\":\"val\"}");
            assertThat(preset.isActive()).isFalse();
            assertThat(preset.getVersion()).isEqualTo(2);
            assertThat(preset.getUpdatedAt()).isEqualTo(later);
            assertThat(preset.getCreatedAt()).isEqualTo(NOW);
        }
    }

    @Nested
    @DisplayName("비즈니스 매칭")
    class BusinessMatching {

        @Test
        @DisplayName("Given: 비즈니스 코드가 일치하면 When: matchesBusiness Then: true 반환")
        void matchesWhenSameBusinessCode() {
            DraftFormTemplate formTemplate = createFormTemplate();
            DraftTemplatePreset preset = DraftTemplatePreset.create(
                    "프리셋",
                    "GENERAL",
                    null,
                    "제목",
                    "내용",
                    formTemplate,
                    null,
                    null,
                    null,
                    true,
                    NOW);

            assertThat(preset.matchesBusiness("GENERAL")).isTrue();
            assertThat(preset.matchesBusiness("general")).isTrue();
        }

        @Test
        @DisplayName("Given: 비즈니스 코드가 다르면 When: matchesBusiness Then: false 반환")
        void doesNotMatchWhenDifferentBusinessCode() {
            DraftFormTemplate formTemplate = createFormTemplate();
            DraftTemplatePreset preset = DraftTemplatePreset.create(
                    "프리셋",
                    "GENERAL",
                    null,
                    "제목",
                    "내용",
                    formTemplate,
                    null,
                    null,
                    null,
                    true,
                    NOW);

            assertThat(preset.matchesBusiness("OTHER")).isFalse();
        }
    }
}
