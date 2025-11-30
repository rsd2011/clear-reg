package com.example.admin.draft.domain.schema.builder;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.example.admin.draft.schema.ArrayField;
import com.example.admin.draft.schema.CheckboxField;
import com.example.admin.draft.schema.DateField;
import com.example.admin.draft.schema.DraftFormSchema;
import com.example.admin.draft.schema.GroupField;
import com.example.admin.draft.schema.SelectField;
import com.example.admin.draft.schema.TextField;
import com.example.admin.draft.schema.builder.FormSchemaBuilders;
import com.example.common.orggroup.WorkType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class FormSchemaBuildersTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Nested
    @DisplayName("WorkType별 스키마 생성")
    class WorkTypeSchemaTest {

        @ParameterizedTest
        @EnumSource(WorkType.class)
        @DisplayName("모든 WorkType에 대해 스키마를 생성할 수 있다")
        void createSchemaForAllWorkTypes(WorkType workType) {
            DraftFormSchema schema = FormSchemaBuilders.forWorkType(workType);

            assertThat(schema).isNotNull();
            assertThat(schema.version()).isEqualTo("1.0");
            assertThat(schema.fields()).isNotEmpty();
        }

        @Test
        @DisplayName("일반 업무 스키마는 제목, 내용, 첨부파일 필드를 포함한다")
        void generalSchemaHasBasicFields() {
            DraftFormSchema schema = FormSchemaBuilders.general();

            assertThat(schema.fields()).hasSize(3);
            assertThat(schema.findField("title")).isInstanceOf(TextField.class);
            assertThat(schema.findField("content")).isInstanceOf(TextField.class);
            assertThat(schema.findField("attachments")).isNotNull();

            TextField contentField = (TextField) schema.findField("content");
            assertThat(contentField.multiline()).isTrue();
        }

        @Test
        @DisplayName("파일 반출 스키마는 반출 유형, 목적, 기간, 파일 목록을 포함한다")
        void fileExportSchemaHasRequiredFields() {
            DraftFormSchema schema = FormSchemaBuilders.fileExport();

            assertThat(schema.findField("title")).isNotNull();
            assertThat(schema.findField("exportType")).isInstanceOf(SelectField.class);
            assertThat(schema.findField("purpose")).isNotNull();
            assertThat(schema.findField("exportPeriod")).isInstanceOf(GroupField.class);
            assertThat(schema.findField("files")).isInstanceOf(ArrayField.class);
            assertThat(schema.findField("securityConfirm")).isInstanceOf(CheckboxField.class);

            // exportPeriod 그룹 내부 필드 확인
            GroupField periodGroup = (GroupField) schema.findField("exportPeriod");
            assertThat(periodGroup.fields()).hasSize(2);
        }

        @Test
        @DisplayName("데이터 정정 스키마는 정정 유형, 대상 테이블, 정정 내용을 포함한다")
        void dataCorrectionSchemaHasRequiredFields() {
            DraftFormSchema schema = FormSchemaBuilders.dataCorrection();

            assertThat(schema.findField("correctionType")).isInstanceOf(SelectField.class);
            assertThat(schema.findField("targetTable")).isInstanceOf(TextField.class);
            assertThat(schema.findField("correctionDetail")).isInstanceOf(GroupField.class);
            assertThat(schema.findField("impactConfirm")).isInstanceOf(CheckboxField.class);

            SelectField correctionType = (SelectField) schema.findField("correctionType");
            assertThat(correctionType.options()).extracting("value")
                    .containsExactly("INSERT", "UPDATE", "DELETE");
        }

        @Test
        @DisplayName("인사정보 변경 스키마는 대상자, 변경 유형, 변경 상세를 포함한다")
        void hrUpdateSchemaHasRequiredFields() {
            DraftFormSchema schema = FormSchemaBuilders.hrUpdate();

            assertThat(schema.findField("targetEmployee")).isInstanceOf(GroupField.class);
            assertThat(schema.findField("changeType")).isInstanceOf(SelectField.class);
            assertThat(schema.findField("changes")).isInstanceOf(ArrayField.class);
            assertThat(schema.findField("effectiveDate")).isInstanceOf(DateField.class);

            GroupField targetEmployee = (GroupField) schema.findField("targetEmployee");
            assertThat(targetEmployee.fields()).hasSize(4);
        }

        @Test
        @DisplayName("정책 변경 스키마는 정책 유형, 변경 범위, 정책 내용을 포함한다")
        void policyChangeSchemaHasRequiredFields() {
            DraftFormSchema schema = FormSchemaBuilders.policyChange();

            assertThat(schema.findField("policyType")).isInstanceOf(SelectField.class);
            assertThat(schema.findField("changeScope")).isInstanceOf(GroupField.class);
            assertThat(schema.findField("policyDetail")).isInstanceOf(GroupField.class);
            assertThat(schema.findField("impactReview")).isInstanceOf(CheckboxField.class);

            SelectField policyType = (SelectField) schema.findField("policyType");
            assertThat(policyType.options()).extracting("value")
                    .contains("MASKING", "PERMISSION", "AUDIT");
        }
    }

    @Nested
    @DisplayName("스키마 JSON 변환")
    class SchemaJsonTest {

        @ParameterizedTest
        @EnumSource(WorkType.class)
        @DisplayName("모든 WorkType 스키마를 JSON으로 변환하고 복원할 수 있다")
        void schemaRoundTripsToJson(WorkType workType) throws Exception {
            DraftFormSchema original = FormSchemaBuilders.forWorkType(workType);

            String json = original.toJson(objectMapper);
            DraftFormSchema restored = DraftFormSchema.fromJson(json, objectMapper);

            assertThat(restored.version()).isEqualTo(original.version());
            assertThat(restored.fields()).hasSameSizeAs(original.fields());
        }

        @Test
        @DisplayName("복잡한 중첩 구조도 올바르게 직렬화된다")
        void complexNestedStructureSerializesCorrectly() throws Exception {
            DraftFormSchema schema = FormSchemaBuilders.fileExport();

            String json = schema.toJson(objectMapper);

            // ArrayField 내부의 필드들이 올바르게 직렬화되었는지 확인
            assertThat(json).contains("\"type\":\"array\"");
            assertThat(json).contains("\"name\":\"files\"");
            assertThat(json).contains("\"itemFields\"");

            // GroupField 내부의 필드들이 올바르게 직렬화되었는지 확인
            assertThat(json).contains("\"type\":\"group\"");
            assertThat(json).contains("\"name\":\"exportPeriod\"");
        }
    }

    @Nested
    @DisplayName("첨부파일 설정")
    class AttachmentConfigTest {

        @Test
        @DisplayName("일반 업무는 선택적 첨부파일을 허용한다")
        void generalSchemaAllowsOptionalAttachments() {
            DraftFormSchema schema = FormSchemaBuilders.general();

            assertThat(schema.attachmentConfig().enabled()).isTrue();
            assertThat(schema.attachmentConfig().required()).isFalse();
        }

        @Test
        @DisplayName("파일 반출은 첨부파일을 비활성화한다")
        void fileExportDisablesAttachments() {
            DraftFormSchema schema = FormSchemaBuilders.fileExport();

            assertThat(schema.attachmentConfig().enabled()).isFalse();
        }
    }
}
