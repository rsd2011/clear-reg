package com.example.admin.draft.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DraftFormTemplate 엔티티")
class DraftFormTemplateTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2025, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC);

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("Given: 조직코드가 있으면 When: 생성하면 Then: ORGANIZATION 범위로 생성된다")
        void createsWithOrganizationScope() {
            DraftFormTemplate template = DraftFormTemplate.create(
                    "테스트 양식",
                    "GENERAL",
                    "ORG1",
                    "{\"type\":\"object\"}",
                    NOW);

            assertThat(template.getName()).isEqualTo("테스트 양식");
            assertThat(template.getBusinessType()).isEqualTo("GENERAL");
            assertThat(template.getOrganizationCode()).isEqualTo("ORG1");
            assertThat(template.getScope()).isEqualTo(TemplateScope.ORGANIZATION);
            assertThat(template.getSchemaJson()).isEqualTo("{\"type\":\"object\"}");
            assertThat(template.getVersion()).isEqualTo(1);
            assertThat(template.isActive()).isTrue();
            assertThat(template.getTemplateCode()).isNotNull();
            assertThat(template.getCreatedAt()).isEqualTo(NOW);
            assertThat(template.getUpdatedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("Given: 조직코드가 null이면 When: 생성하면 Then: GLOBAL 범위로 생성된다")
        void createsWithGlobalScope() {
            DraftFormTemplate template = DraftFormTemplate.create(
                    "전역 양식",
                    "GENERAL",
                    null,
                    "{}",
                    NOW);

            assertThat(template.getOrganizationCode()).isNull();
            assertThat(template.getScope()).isEqualTo(TemplateScope.GLOBAL);
            assertThat(template.isGlobal()).isTrue();
        }
    }

    @Nested
    @DisplayName("수정")
    class Update {

        @Test
        @DisplayName("Given: 템플릿이 있으면 When: 수정하면 Then: 값과 버전이 변경된다")
        void updatesSuccessfully() {
            DraftFormTemplate template = DraftFormTemplate.create(
                    "원본 양식",
                    "GENERAL",
                    "ORG1",
                    "{}",
                    NOW);

            OffsetDateTime later = NOW.plusHours(1);
            template.update("수정된 양식", "{\"updated\":true}", false, later);

            assertThat(template.getName()).isEqualTo("수정된 양식");
            assertThat(template.getSchemaJson()).isEqualTo("{\"updated\":true}");
            assertThat(template.isActive()).isFalse();
            assertThat(template.getVersion()).isEqualTo(2);
            assertThat(template.getUpdatedAt()).isEqualTo(later);
            assertThat(template.getCreatedAt()).isEqualTo(NOW);
        }
    }

    @Nested
    @DisplayName("조직 검증")
    class OrganizationValidation {

        @Test
        @DisplayName("Given: 조직별 템플릿인데 같은 조직이면 When: assertOrganization Then: 통과")
        void passesWhenSameOrganization() {
            DraftFormTemplate template = DraftFormTemplate.create(
                    "조직 양식",
                    "GENERAL",
                    "ORG1",
                    "{}",
                    NOW);

            template.assertOrganization("ORG1");
            // 예외 없이 통과
        }

        @Test
        @DisplayName("Given: 전역 템플릿이면 When: assertOrganization Then: 통과")
        void passesWhenGlobalTemplate() {
            DraftFormTemplate template = DraftFormTemplate.create(
                    "전역 양식",
                    "GENERAL",
                    null,
                    "{}",
                    NOW);

            template.assertOrganization("ANY_ORG");
            // 예외 없이 통과
        }
    }

    @Nested
    @DisplayName("비즈니스 매칭")
    class BusinessMatching {

        @Test
        @DisplayName("Given: 비즈니스 유형이 일치하면 When: matchesBusiness Then: true 반환")
        void matchesWhenSameBusinessType() {
            DraftFormTemplate template = DraftFormTemplate.create(
                    "양식",
                    "GENERAL",
                    null,
                    "{}",
                    NOW);

            assertThat(template.matchesBusiness("GENERAL")).isTrue();
            assertThat(template.matchesBusiness("general")).isTrue();
        }

        @Test
        @DisplayName("Given: 비즈니스 유형이 다르면 When: matchesBusiness Then: false 반환")
        void doesNotMatchWhenDifferentBusinessType() {
            DraftFormTemplate template = DraftFormTemplate.create(
                    "양식",
                    "GENERAL",
                    null,
                    "{}",
                    NOW);

            assertThat(template.matchesBusiness("OTHER")).isFalse();
        }
    }
}
