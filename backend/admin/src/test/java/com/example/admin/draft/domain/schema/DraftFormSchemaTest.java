package com.example.admin.draft.domain.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.admin.draft.schema.ArrayField;
import com.example.admin.draft.schema.AttachmentConfig;
import com.example.admin.draft.schema.CheckboxField;
import com.example.admin.draft.schema.DateField;
import com.example.admin.draft.schema.DraftFormSchema;
import com.example.admin.draft.schema.FileField;
import com.example.admin.draft.schema.FormField;
import com.example.admin.draft.schema.FormLayout;
import com.example.admin.draft.schema.GroupField;
import com.example.admin.draft.schema.NumberField;
import com.example.admin.draft.schema.SelectField;
import com.example.admin.draft.schema.TextField;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class DraftFormSchemaTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Nested
    @DisplayName("FormField 다형성 직렬화/역직렬화")
    class FormFieldPolymorphism {

        @Test
        @DisplayName("TextField를 JSON으로 직렬화하면 type 필드가 포함된다")
        void textFieldSerializesToJsonWithType() throws Exception {
            TextField field = TextField.of("title", "제목", true);

            String json = objectMapper.writeValueAsString(field);

            assertThat(json).contains("\"type\":\"text\"");
            assertThat(json).contains("\"name\":\"title\"");
            assertThat(json).contains("\"label\":\"제목\"");
            assertThat(json).contains("\"required\":true");
        }

        @Test
        @DisplayName("JSON에서 FormField로 역직렬화하면 올바른 타입으로 변환된다")
        void jsonDeserializesToCorrectFieldType() throws Exception {
            String json = """
                    {"type":"text","name":"title","label":"제목","required":true,"description":null,"placeholder":null,"minLength":null,"maxLength":null,"pattern":null,"multiline":false,"inputType":"text"}
                    """;

            FormField field = objectMapper.readValue(json, FormField.class);

            assertThat(field).isInstanceOf(TextField.class);
            TextField textField = (TextField) field;
            assertThat(textField.name()).isEqualTo("title");
            assertThat(textField.label()).isEqualTo("제목");
            assertThat(textField.required()).isTrue();
        }

        @Test
        @DisplayName("NumberField는 min/max 범위를 포함한다")
        void numberFieldIncludesMinMax() throws Exception {
            NumberField field = NumberField.ofRange("amount", "금액", true,
                    BigDecimal.ZERO, BigDecimal.valueOf(1000000));

            String json = objectMapper.writeValueAsString(field);

            assertThat(json).contains("\"type\":\"number\"");
            assertThat(json).contains("\"min\":0");
            assertThat(json).contains("\"max\":1000000");
        }

        @Test
        @DisplayName("SelectField 옵션 목록을 직렬화한다")
        void selectFieldSerializesOptions() throws Exception {
            SelectField field = SelectField.of("status", "상태", true, List.of(
                    SelectField.SelectOption.of("ACTIVE", "활성"),
                    SelectField.SelectOption.of("INACTIVE", "비활성")
            ));

            String json = objectMapper.writeValueAsString(field);

            assertThat(json).contains("\"type\":\"select\"");
            assertThat(json).contains("\"value\":\"ACTIVE\"");
            assertThat(json).contains("\"label\":\"활성\"");
        }

        @Test
        @DisplayName("ArrayField는 중첩된 필드 목록을 포함한다")
        void arrayFieldIncludesNestedFields() throws Exception {
            ArrayField field = ArrayField.of("experiences", "경력사항", false, List.of(
                    TextField.of("company", "회사명", true),
                    DateField.of("startDate", "입사일", true)
            ));

            String json = objectMapper.writeValueAsString(field);
            FormField parsed = objectMapper.readValue(json, FormField.class);

            assertThat(parsed).isInstanceOf(ArrayField.class);
            ArrayField arrayField = (ArrayField) parsed;
            assertThat(arrayField.itemFields()).hasSize(2);
            assertThat(arrayField.itemFields().get(0)).isInstanceOf(TextField.class);
            assertThat(arrayField.itemFields().get(1)).isInstanceOf(DateField.class);
        }

        @Test
        @DisplayName("GroupField는 중첩된 필드 그룹을 포함한다")
        void groupFieldIncludesNestedGroup() throws Exception {
            GroupField field = GroupField.of("personalInfo", "개인정보", List.of(
                    TextField.of("name", "이름", true),
                    TextField.of("email", "이메일", true),
                    CheckboxField.agreement("agree", "개인정보 수집에 동의합니다")
            ));

            String json = objectMapper.writeValueAsString(field);
            FormField parsed = objectMapper.readValue(json, FormField.class);

            assertThat(parsed).isInstanceOf(GroupField.class);
            GroupField groupField = (GroupField) parsed;
            assertThat(groupField.fields()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("DraftFormSchema 직렬화/역직렬화")
    class SchemaSerializationTest {

        @Test
        @DisplayName("전체 스키마를 JSON으로 변환하고 복원할 수 있다")
        void schemaRoundTripsToJson() throws Exception {
            DraftFormSchema schema = DraftFormSchema.builder()
                    .version("1.0")
                    .fields(List.of(
                            TextField.of("title", "제목", true),
                            NumberField.of("amount", "금액", true),
                            SelectField.of("type", "유형", true, List.of(
                                    SelectField.SelectOption.of("A", "유형A"),
                                    SelectField.SelectOption.of("B", "유형B")
                            )),
                            FileField.image("attachment", "첨부파일", false)
                    ))
                    .layout(FormLayout.columns(2))
                    .attachmentConfig(AttachmentConfig.defaultConfig())
                    .defaultValues(Map.of("type", "A"))
                    .build();

            String json = schema.toJson(objectMapper);
            DraftFormSchema restored = DraftFormSchema.fromJson(json, objectMapper);

            assertThat(restored.version()).isEqualTo("1.0");
            assertThat(restored.fields()).hasSize(4);
            assertThat(restored.layout().columns()).isEqualTo(2);
            assertThat(restored.attachmentConfig().enabled()).isTrue();
            assertThat(restored.defaultValues()).containsEntry("type", "A");
        }

        @Test
        @DisplayName("간편 생성자로 스키마를 생성할 수 있다")
        void createSchemaWithConvenienceMethod() {
            DraftFormSchema schema = DraftFormSchema.of(List.of(
                    TextField.of("name", "이름", true)
            ));

            assertThat(schema.version()).isEqualTo("1.0");
            assertThat(schema.fields()).hasSize(1);
            assertThat(schema.layout().columns()).isEqualTo(1);
            assertThat(schema.attachmentConfig().enabled()).isFalse();
        }

        @Test
        @DisplayName("findField로 필드를 조회할 수 있다")
        void findFieldByName() {
            DraftFormSchema schema = DraftFormSchema.of(List.of(
                    TextField.of("title", "제목", true),
                    NumberField.of("amount", "금액", false)
            ));

            FormField found = schema.findField("amount");

            assertThat(found).isNotNull();
            assertThat(found).isInstanceOf(NumberField.class);
            assertThat(found.name()).isEqualTo("amount");
        }

        @Test
        @DisplayName("존재하지 않는 필드를 조회하면 null을 반환한다")
        void findFieldReturnsNullWhenNotFound() {
            DraftFormSchema schema = DraftFormSchema.of(List.of(
                    TextField.of("title", "제목", true)
            ));

            FormField found = schema.findField("nonexistent");

            assertThat(found).isNull();
        }
    }

    @Nested
    @DisplayName("개별 필드 타입 테스트")
    class IndividualFieldTypeTest {

        @Test
        @DisplayName("DateField는 날짜 범위를 지정할 수 있다")
        void dateFieldWithRange() {
            DateField field = new DateField(
                    "birthDate", "생년월일", true, null,
                    "date",
                    LocalDate.of(1900, 1, 1),
                    LocalDate.now(),
                    "yyyy-MM-dd",
                    false
            );

            assertThat(field.minDate()).isEqualTo(LocalDate.of(1900, 1, 1));
            assertThat(field.maxDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("FileField는 MIME 타입과 크기 제한을 설정할 수 있다")
        void fileFieldWithConstraints() {
            FileField field = new FileField(
                    "document", "문서", true, null,
                    List.of("application/pdf", "image/*"),
                    10L * 1024 * 1024,
                    5,
                    true
            );

            assertThat(field.acceptedTypes()).containsExactly("application/pdf", "image/*");
            assertThat(field.maxFileSize()).isEqualTo(10L * 1024 * 1024);
            assertThat(field.maxFiles()).isEqualTo(5);
            assertThat(field.multiple()).isTrue();
        }

        @Test
        @DisplayName("CheckboxField는 동의 체크박스를 생성할 수 있다")
        void checkboxFieldForAgreement() {
            CheckboxField field = CheckboxField.agreement("privacy", "개인정보 처리방침에 동의합니다");

            assertThat(field.name()).isEqualTo("privacy");
            assertThat(field.required()).isTrue();
            assertThat(field.checkLabel()).isEqualTo("개인정보 처리방침에 동의합니다");
        }
    }

    @Nested
    @DisplayName("레이아웃 및 첨부파일 설정 테스트")
    class LayoutAndAttachmentTest {

        @Test
        @DisplayName("FormLayout 섹션 기반 레이아웃을 생성할 수 있다")
        void formLayoutWithSections() {
            FormLayout layout = FormLayout.withSections(List.of(
                    FormLayout.FormSection.of("basic", "기본정보", List.of("title", "type")),
                    FormLayout.FormSection.of("detail", "상세정보", List.of("amount", "description"))
            ));

            assertThat(layout.sections()).hasSize(2);
            assertThat(layout.sections().get(0).name()).isEqualTo("basic");
            assertThat(layout.sections().get(0).fieldNames()).containsExactly("title", "type");
        }

        @Test
        @DisplayName("AttachmentConfig 카테고리를 설정할 수 있다")
        void attachmentConfigWithCategories() {
            AttachmentConfig config = new AttachmentConfig(
                    true, false, 10, 50L * 1024 * 1024, 10L * 1024 * 1024,
                    List.of("application/pdf"),
                    List.of(
                            AttachmentConfig.AttachmentCategory.required("CONTRACT", "계약서"),
                            AttachmentConfig.AttachmentCategory.of("REFERENCE", "참고자료")
                    )
            );

            assertThat(config.categories()).hasSize(2);
            assertThat(config.categories().get(0).required()).isTrue();
            assertThat(config.categories().get(1).required()).isFalse();
        }
    }

    @Nested
    @DisplayName("JSON 변환 오류 처리")
    class JsonErrorHandlingTest {

        @Test
        @DisplayName("잘못된 JSON을 역직렬화하면 예외가 발생한다")
        void invalidJsonThrowsException() {
            String invalidJson = "{ invalid json }";

            assertThatThrownBy(() -> DraftFormSchema.fromJson(invalidJson, objectMapper))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Failed to deserialize");
        }
    }
}
