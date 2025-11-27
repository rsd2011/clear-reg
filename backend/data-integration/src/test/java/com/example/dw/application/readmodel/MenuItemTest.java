package com.example.dw.application.readmodel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MenuItem 테스트")
class MenuItemTest {

    @Nested
    @DisplayName("MenuItem 레코드")
    class MenuItemTests {

        @Test
        @DisplayName("Given 모든 파라미터 When 생성 Then 올바른 값 반환")
        void givenAllParamsWhenCreateThenReturnCorrectValues() {
            // given
            List<MenuItem.MenuCapabilityRef> capabilities = List.of(
                    new MenuItem.MenuCapabilityRef("DRAFT", "READ"),
                    new MenuItem.MenuCapabilityRef("DRAFT", "CREATE")
            );

            // when
            MenuItem item = new MenuItem(
                    "MENU001",
                    "기안 관리",
                    "DRAFT",
                    "READ",
                    "/drafts",
                    "document-icon",
                    10,
                    "ROOT",
                    "기안 목록을 조회합니다",
                    capabilities
            );

            // then
            assertThat(item.code()).isEqualTo("MENU001");
            assertThat(item.name()).isEqualTo("기안 관리");
            assertThat(item.featureCode()).isEqualTo("DRAFT");
            assertThat(item.actionCode()).isEqualTo("READ");
            assertThat(item.path()).isEqualTo("/drafts");
            assertThat(item.icon()).isEqualTo("document-icon");
            assertThat(item.sortOrder()).isEqualTo(10);
            assertThat(item.parentCode()).isEqualTo("ROOT");
            assertThat(item.description()).isEqualTo("기안 목록을 조회합니다");
            assertThat(item.requiredCapabilities()).hasSize(2);
        }

        @Test
        @DisplayName("Given 간단한 생성자 When 생성 Then 기본값 설정")
        void givenSimpleConstructorWhenCreateThenSetDefaults() {
            // when
            MenuItem item = new MenuItem("CODE", "이름", "FEATURE", "ACTION", "/path");

            // then
            assertThat(item.code()).isEqualTo("CODE");
            assertThat(item.name()).isEqualTo("이름");
            assertThat(item.featureCode()).isEqualTo("FEATURE");
            assertThat(item.actionCode()).isEqualTo("ACTION");
            assertThat(item.path()).isEqualTo("/path");
            assertThat(item.icon()).isNull();
            assertThat(item.sortOrder()).isNull();
            assertThat(item.parentCode()).isNull();
            assertThat(item.description()).isNull();
            assertThat(item.requiredCapabilities()).isEmpty();
        }

        @Test
        @DisplayName("Given 같은 값 When equals 비교 Then true 반환")
        void givenSameValuesWhenEqualsThenReturnTrue() {
            // given
            MenuItem item1 = new MenuItem("CODE", "이름", "FEATURE", "ACTION", "/path");
            MenuItem item2 = new MenuItem("CODE", "이름", "FEATURE", "ACTION", "/path");

            // then
            assertThat(item1).isEqualTo(item2);
            assertThat(item1.hashCode()).isEqualTo(item2.hashCode());
        }
    }

    @Nested
    @DisplayName("MenuCapabilityRef 레코드")
    class MenuCapabilityRefTests {

        @Test
        @DisplayName("Given feature와 action When 생성 Then 올바른 값 반환")
        void givenFeatureAndActionWhenCreateThenReturnCorrectValues() {
            // when
            MenuItem.MenuCapabilityRef ref = new MenuItem.MenuCapabilityRef("DRAFT", "READ");

            // then
            assertThat(ref.feature()).isEqualTo("DRAFT");
            assertThat(ref.action()).isEqualTo("READ");
        }

        @Test
        @DisplayName("Given 같은 값 When equals 비교 Then true 반환")
        void givenSameValuesWhenEqualsThenReturnTrue() {
            // given
            MenuItem.MenuCapabilityRef ref1 = new MenuItem.MenuCapabilityRef("DRAFT", "CREATE");
            MenuItem.MenuCapabilityRef ref2 = new MenuItem.MenuCapabilityRef("DRAFT", "CREATE");

            // then
            assertThat(ref1).isEqualTo(ref2);
            assertThat(ref1.hashCode()).isEqualTo(ref2.hashCode());
        }

        @Test
        @DisplayName("Given 다른 값 When equals 비교 Then false 반환")
        void givenDifferentValuesWhenEqualsThenReturnFalse() {
            // given
            MenuItem.MenuCapabilityRef ref1 = new MenuItem.MenuCapabilityRef("DRAFT", "READ");
            MenuItem.MenuCapabilityRef ref2 = new MenuItem.MenuCapabilityRef("DRAFT", "CREATE");

            // then
            assertThat(ref1).isNotEqualTo(ref2);
        }

        @Test
        @DisplayName("Given null 값 When 생성 Then 정상 처리")
        void givenNullValuesWhenCreateThenHandle() {
            // when
            MenuItem.MenuCapabilityRef ref = new MenuItem.MenuCapabilityRef(null, null);

            // then
            assertThat(ref.feature()).isNull();
            assertThat(ref.action()).isNull();
        }

        @Test
        @DisplayName("toString은 의미있는 문자열을 반환한다")
        void toStringReturnsReadableString() {
            // given
            MenuItem.MenuCapabilityRef ref = new MenuItem.MenuCapabilityRef("FEATURE", "ACTION");

            // when
            String result = ref.toString();

            // then
            assertThat(result).contains("FEATURE");
            assertThat(result).contains("ACTION");
        }
    }
}
