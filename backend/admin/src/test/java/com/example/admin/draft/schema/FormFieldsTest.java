package com.example.admin.draft.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 폼 필드 구현체 테스트.
 */
class FormFieldsTest {

    @Nested
    @DisplayName("TextField")
    class TextFieldTest {

        @Test
        @DisplayName("Given null inputType When 생성 Then 기본값 'text' 설정")
        void constructor_nullInputType_defaultsToText() {
            TextField field = new TextField("name", "라벨", true, null, null, null, null, null, false, null);
            assertThat(field.inputType()).isEqualTo("text");
        }

        @Test
        @DisplayName("Given 빈 inputType When 생성 Then 기본값 'text' 설정")
        void constructor_blankInputType_defaultsToText() {
            TextField field = new TextField("name", "라벨", true, null, null, null, null, null, false, "  ");
            assertThat(field.inputType()).isEqualTo("text");
        }

        @Test
        @DisplayName("of 팩토리 메서드로 간단한 텍스트 필드 생성")
        void of_createsSimpleTextField() {
            TextField field = TextField.of("title", "제목", true);
            assertThat(field.name()).isEqualTo("title");
            assertThat(field.label()).isEqualTo("제목");
            assertThat(field.required()).isTrue();
            assertThat(field.inputType()).isEqualTo("text");
            assertThat(field.multiline()).isFalse();
            assertThat(field.type()).isEqualTo("text");
        }

        @Test
        @DisplayName("multiline 팩토리 메서드로 다중 행 텍스트 필드 생성")
        void multiline_createsMultilineField() {
            TextField field = TextField.multiline("content", "내용", true);
            assertThat(field.multiline()).isTrue();
            assertThat(field.inputType()).isEqualTo("text");
        }

        @Test
        @DisplayName("email 팩토리 메서드로 이메일 필드 생성")
        void email_createsEmailField() {
            TextField field = TextField.email("email", "이메일", true);
            assertThat(field.inputType()).isEqualTo("email");
        }

        @Test
        @DisplayName("url 팩토리 메서드로 URL 필드 생성")
        void url_createsUrlField() {
            TextField field = TextField.url("website", "웹사이트", false);
            assertThat(field.inputType()).isEqualTo("url");
            assertThat(field.required()).isFalse();
        }

        @Test
        @DisplayName("tel 팩토리 메서드로 전화번호 필드 생성")
        void tel_createsTelField() {
            TextField field = TextField.tel("phone", "전화번호", true);
            assertThat(field.inputType()).isEqualTo("tel");
        }

        @Test
        @DisplayName("모든 파라미터로 TextField 생성")
        void fullConstructor_createsCompleteField() {
            TextField field = new TextField(
                    "code", "코드", true, "설명 텍스트", "플레이스홀더",
                    5, 20, "^[A-Z]+$", false, "text"
            );
            assertThat(field.description()).isEqualTo("설명 텍스트");
            assertThat(field.placeholder()).isEqualTo("플레이스홀더");
            assertThat(field.minLength()).isEqualTo(5);
            assertThat(field.maxLength()).isEqualTo(20);
            assertThat(field.pattern()).isEqualTo("^[A-Z]+$");
        }
    }

    @Nested
    @DisplayName("FileField")
    class FileFieldTest {

        @Test
        @DisplayName("Given null acceptedTypes When 생성 Then 빈 리스트로 초기화")
        void constructor_nullAcceptedTypes_defaultsToEmptyList() {
            FileField field = new FileField("file", "파일", true, null, null, null, null, false);
            assertThat(field.acceptedTypes()).isEmpty();
        }

        @Test
        @DisplayName("of 팩토리 메서드로 단일 파일 필드 생성")
        void of_createsSingleFileField() {
            FileField field = FileField.of("attachment", "첨부파일", true);
            assertThat(field.name()).isEqualTo("attachment");
            assertThat(field.multiple()).isFalse();
            assertThat(field.maxFiles()).isEqualTo(1);
            assertThat(field.type()).isEqualTo("file");
        }

        @Test
        @DisplayName("multiple 팩토리 메서드로 다중 파일 필드 생성")
        void multiple_createsMultipleFileField() {
            FileField field = FileField.multiple("files", "파일들", true, 5);
            assertThat(field.multiple()).isTrue();
            assertThat(field.maxFiles()).isEqualTo(5);
        }

        @Test
        @DisplayName("image 팩토리 메서드로 이미지 전용 필드 생성")
        void image_createsImageField() {
            FileField field = FileField.image("photo", "사진", true);
            assertThat(field.acceptedTypes()).containsExactly("image/*");
            assertThat(field.multiple()).isFalse();
        }

        @Test
        @DisplayName("any 팩토리 메서드로 모든 타입 허용 필드 생성")
        void any_createsAnyTypeField() {
            FileField field = FileField.any("doc", "문서", false);
            assertThat(field.acceptedTypes()).isEmpty();
            assertThat(field.required()).isFalse();
        }

        @Test
        @DisplayName("pdf 팩토리 메서드로 PDF 전용 필드 생성")
        void pdf_createsPdfField() {
            FileField field = FileField.pdf("contract", "계약서", true);
            assertThat(field.acceptedTypes()).containsExactly("application/pdf");
        }

        @Test
        @DisplayName("documents 팩토리 메서드로 문서 파일 필드 생성")
        void documents_createsDocumentsField() {
            FileField field = FileField.documents("report", "보고서", true);
            assertThat(field.acceptedTypes()).hasSize(7);
            assertThat(field.acceptedTypes()).contains(
                    "application/pdf",
                    "application/msword",
                    "application/vnd.ms-excel"
            );
        }

        @Test
        @DisplayName("모든 파라미터로 FileField 생성")
        void fullConstructor_createsCompleteField() {
            FileField field = new FileField(
                    "upload", "업로드", true, "설명",
                    List.of("image/png", "image/jpeg"),
                    10_000_000L, 3, true
            );
            assertThat(field.description()).isEqualTo("설명");
            assertThat(field.maxFileSize()).isEqualTo(10_000_000L);
            assertThat(field.maxFiles()).isEqualTo(3);
            assertThat(field.acceptedTypes()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("SelectField")
    class SelectFieldTest {

        @Test
        @DisplayName("Given null options When 생성 Then 빈 리스트로 초기화")
        void constructor_nullOptions_defaultsToEmptyList() {
            SelectField field = new SelectField("select", "선택", true, null, null,
                    false, false, null, null);
            assertThat(field.options()).isEmpty();
        }

        @Test
        @DisplayName("Given null displayType When 생성 Then 기본값 'dropdown' 설정")
        void constructor_nullDisplayType_defaultsToDropdown() {
            SelectField field = new SelectField("select", "선택", true, null, List.of(),
                    false, false, null, null);
            assertThat(field.displayType()).isEqualTo("dropdown");
        }

        @Test
        @DisplayName("of 팩토리 메서드로 선택 필드 생성")
        void of_createsSelectField() {
            List<SelectField.SelectOption> options = List.of(
                    SelectField.SelectOption.of("A", "옵션 A"),
                    SelectField.SelectOption.of("B", "옵션 B")
            );
            SelectField field = SelectField.of("type", "유형", true, options);
            assertThat(field.options()).hasSize(2);
            assertThat(field.multiple()).isFalse();
            assertThat(field.type()).isEqualTo("select");
            assertThat(field.displayType()).isEqualTo("dropdown");
        }

        @Test
        @DisplayName("dynamic 팩토리 메서드로 동적 옵션 필드 생성")
        void dynamic_createsDynamicSelectField() {
            SelectField field = SelectField.dynamic("department", "부서", true, "/api/departments");
            assertThat(field.optionsSource()).isEqualTo("/api/departments");
            assertThat(field.searchable()).isTrue();
            assertThat(field.options()).isEmpty();
        }

        @Test
        @DisplayName("SelectOption.of로 옵션 생성")
        void selectOption_of_createsOption() {
            SelectField.SelectOption option = SelectField.SelectOption.of("VALUE", "표시 라벨");
            assertThat(option.value()).isEqualTo("VALUE");
            assertThat(option.label()).isEqualTo("표시 라벨");
            assertThat(option.disabled()).isFalse();
        }

        @Test
        @DisplayName("SelectOption 전체 생성자로 disabled 옵션 생성")
        void selectOption_fullConstructor_createsDisabledOption() {
            SelectField.SelectOption option = new SelectField.SelectOption("VALUE", "표시 라벨", true);
            assertThat(option.disabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("NumberField")
    class NumberFieldTest {

        @Test
        @DisplayName("of 팩토리 메서드로 숫자 필드 생성")
        void of_createsNumberField() {
            NumberField field = NumberField.of("amount", "금액", true);
            assertThat(field.name()).isEqualTo("amount");
            assertThat(field.type()).isEqualTo("number");
            assertThat(field.min()).isNull();
            assertThat(field.max()).isNull();
        }

        @Test
        @DisplayName("ofRange 팩토리 메서드로 범위 제한 필드 생성")
        void ofRange_createsRangeField() {
            NumberField field = NumberField.ofRange("score", "점수", true,
                    BigDecimal.ZERO, BigDecimal.valueOf(100));
            assertThat(field.min()).isEqualTo(BigDecimal.ZERO);
            assertThat(field.max()).isEqualTo(BigDecimal.valueOf(100));
        }

        @Test
        @DisplayName("모든 파라미터로 NumberField 생성")
        void fullConstructor_createsCompleteField() {
            NumberField field = new NumberField(
                    "price", "가격", true, "가격을 입력하세요", "0",
                    BigDecimal.ZERO, BigDecimal.valueOf(1000000),
                    BigDecimal.valueOf(100), 2, "KRW"
            );
            assertThat(field.description()).isEqualTo("가격을 입력하세요");
            assertThat(field.placeholder()).isEqualTo("0");
            assertThat(field.step()).isEqualTo(BigDecimal.valueOf(100));
            assertThat(field.precision()).isEqualTo(2);
            assertThat(field.currency()).isEqualTo("KRW");
        }
    }

    @Nested
    @DisplayName("DateField")
    class DateFieldTest {

        @Test
        @DisplayName("Given null dateType When 생성 Then 기본값 'date' 설정")
        void constructor_nullDateType_defaultsToDate() {
            DateField field = new DateField("date", "날짜", true, null, null, null, null, null, false);
            assertThat(field.dateType()).isEqualTo("date");
        }

        @Test
        @DisplayName("of 팩토리 메서드로 날짜 필드 생성")
        void of_createsDateField() {
            DateField field = DateField.of("startDate", "시작일", true);
            assertThat(field.type()).isEqualTo("date");
            assertThat(field.dateType()).isEqualTo("date");
            assertThat(field.includeTime()).isFalse();
        }

        @Test
        @DisplayName("datetime 팩토리 메서드로 날짜시간 필드 생성")
        void datetime_createsDateTimeField() {
            DateField field = DateField.datetime("eventTime", "이벤트 시간", true);
            assertThat(field.dateType()).isEqualTo("datetime");
            assertThat(field.includeTime()).isTrue();
        }

        @Test
        @DisplayName("모든 파라미터로 DateField 생성")
        void fullConstructor_createsCompleteField() {
            java.time.LocalDate minDate = java.time.LocalDate.of(2024, 1, 1);
            java.time.LocalDate maxDate = java.time.LocalDate.of(2024, 12, 31);
            DateField field = new DateField(
                    "deadline", "마감일", true, "마감일을 선택하세요",
                    "date", minDate, maxDate, "yyyy-MM-dd", false
            );
            assertThat(field.description()).isEqualTo("마감일을 선택하세요");
            assertThat(field.minDate()).isEqualTo(minDate);
            assertThat(field.maxDate()).isEqualTo(maxDate);
            assertThat(field.format()).isEqualTo("yyyy-MM-dd");
        }
    }

    @Nested
    @DisplayName("CheckboxField")
    class CheckboxFieldTest {

        @Test
        @DisplayName("of 팩토리 메서드로 체크박스 필드 생성")
        void of_createsCheckboxField() {
            CheckboxField field = CheckboxField.of("agree", "동의합니다", true);
            assertThat(field.type()).isEqualTo("checkbox");
            assertThat(field.name()).isEqualTo("agree");
            assertThat(field.label()).isEqualTo("동의합니다");
            assertThat(field.required()).isTrue();
            assertThat(field.defaultValue()).isFalse();
        }

        @Test
        @DisplayName("agreement 팩토리 메서드로 동의 체크박스 생성")
        void agreement_createsAgreementField() {
            CheckboxField field = CheckboxField.agreement("terms", "이용약관에 동의합니다.");
            assertThat(field.name()).isEqualTo("terms");
            assertThat(field.checkLabel()).isEqualTo("이용약관에 동의합니다.");
            assertThat(field.required()).isTrue();
            assertThat(field.label()).isNull();
        }

        @Test
        @DisplayName("모든 파라미터로 CheckboxField 생성")
        void fullConstructor_createsCompleteField() {
            CheckboxField field = new CheckboxField(
                    "newsletter", "뉴스레터", false, "뉴스레터 구독 동의",
                    true, "뉴스레터를 받겠습니다"
            );
            assertThat(field.description()).isEqualTo("뉴스레터 구독 동의");
            assertThat(field.defaultValue()).isTrue();
            assertThat(field.checkLabel()).isEqualTo("뉴스레터를 받겠습니다");
        }
    }

    @Nested
    @DisplayName("ArrayField")
    class ArrayFieldTest {

        @Test
        @DisplayName("Given null itemFields When 생성 Then 빈 리스트로 초기화")
        void constructor_nullItemFields_defaultsToEmptyList() {
            ArrayField field = new ArrayField("items", "항목", true, null, null, null, null, null);
            assertThat(field.itemFields()).isEmpty();
        }

        @Test
        @DisplayName("Given null addLabel When 생성 Then 기본값 '항목 추가' 설정")
        void constructor_nullAddLabel_defaultsToDefaultLabel() {
            ArrayField field = new ArrayField("items", "항목", true, null, List.of(), null, null, null);
            assertThat(field.addLabel()).isEqualTo("항목 추가");
        }

        @Test
        @DisplayName("of 팩토리 메서드로 배열 필드 생성")
        void of_createsArrayField() {
            List<FormField> itemFields = List.of(TextField.of("name", "이름", true));
            ArrayField field = ArrayField.of("contacts", "연락처 목록", true, itemFields);
            assertThat(field.type()).isEqualTo("array");
            assertThat(field.itemFields()).hasSize(1);
            assertThat(field.addLabel()).isEqualTo("항목 추가");
        }

        @Test
        @DisplayName("ofRange 팩토리 메서드로 범위 제한 배열 필드 생성")
        void ofRange_createsLimitedArrayField() {
            List<FormField> itemFields = List.of(TextField.of("item", "항목", true));
            ArrayField field = ArrayField.ofRange("list", "목록", true, itemFields, 1, 5);
            assertThat(field.minItems()).isEqualTo(1);
            assertThat(field.maxItems()).isEqualTo(5);
        }

        @Test
        @DisplayName("모든 파라미터로 ArrayField 생성")
        void fullConstructor_createsCompleteField() {
            List<FormField> itemFields = List.of(
                    TextField.of("name", "이름", true),
                    TextField.of("phone", "전화번호", false)
            );
            ArrayField field = new ArrayField(
                    "family", "가족 정보", false, "가족 구성원을 입력하세요",
                    itemFields, 0, 10, "가족 추가"
            );
            assertThat(field.description()).isEqualTo("가족 구성원을 입력하세요");
            assertThat(field.itemFields()).hasSize(2);
            assertThat(field.minItems()).isEqualTo(0);
            assertThat(field.maxItems()).isEqualTo(10);
            assertThat(field.addLabel()).isEqualTo("가족 추가");
        }
    }

    @Nested
    @DisplayName("GroupField")
    class GroupFieldTest {

        @Test
        @DisplayName("Given null fields When 생성 Then 빈 리스트로 초기화")
        void constructor_nullFields_defaultsToEmptyList() {
            GroupField field = new GroupField("group", "그룹", false, null, null, false, false);
            assertThat(field.fields()).isEmpty();
        }

        @Test
        @DisplayName("of 팩토리 메서드로 그룹 필드 생성")
        void of_createsGroupField() {
            List<FormField> fields = List.of(
                    TextField.of("firstName", "이름", true),
                    TextField.of("lastName", "성", true)
            );
            GroupField field = GroupField.of("name", "성명", fields);
            assertThat(field.type()).isEqualTo("group");
            assertThat(field.fields()).hasSize(2);
            assertThat(field.collapsible()).isFalse();
            assertThat(field.collapsed()).isFalse();
        }

        @Test
        @DisplayName("collapsible 팩토리 메서드로 접을 수 있는 그룹 생성 (펼쳐진 상태)")
        void collapsible_createsExpandedCollapsibleGroup() {
            List<FormField> fields = List.of(TextField.of("note", "비고", false));
            GroupField field = GroupField.collapsible("extra", "추가 정보", fields, false);
            assertThat(field.collapsible()).isTrue();
            assertThat(field.collapsed()).isFalse();
        }

        @Test
        @DisplayName("collapsible 팩토리 메서드로 접을 수 있는 그룹 생성 (접힌 상태)")
        void collapsible_createsCollapsedGroup() {
            List<FormField> fields = List.of(TextField.of("note", "비고", false));
            GroupField field = GroupField.collapsible("extra", "추가 정보", fields, true);
            assertThat(field.collapsible()).isTrue();
            assertThat(field.collapsed()).isTrue();
        }

        @Test
        @DisplayName("모든 파라미터로 GroupField 생성")
        void fullConstructor_createsCompleteField() {
            List<FormField> fields = List.of(
                    TextField.of("address", "주소", true),
                    TextField.of("zipCode", "우편번호", true)
            );
            GroupField field = new GroupField(
                    "addressInfo", "주소 정보", true, "주소를 입력해주세요",
                    fields, true, false
            );
            assertThat(field.description()).isEqualTo("주소를 입력해주세요");
            assertThat(field.required()).isTrue();
            assertThat(field.fields()).hasSize(2);
            assertThat(field.collapsible()).isTrue();
            assertThat(field.collapsed()).isFalse();
        }
    }
}
