package com.example.common.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AuditPartitionSettings 테스트")
class AuditPartitionSettingsTest {

    @Nested
    @DisplayName("기본값 적용")
    class DefaultValues {

        @Test
        @DisplayName("Given null cron When 생성하면 Then 기본 cron 표현식이 설정된다")
        void nullCronSetsDefault() {
            AuditPartitionSettings settings = new AuditPartitionSettings(
                    true, null, 3, "HOT_TS", "COLD_TS", 6, 60);

            assertThat(settings.cron()).isEqualTo("0 0 2 1 * *");
        }

        @Test
        @DisplayName("Given 빈 cron When 생성하면 Then 기본 cron 표현식이 설정된다")
        void emptyCronSetsDefault() {
            AuditPartitionSettings settings = new AuditPartitionSettings(
                    true, "", 3, "HOT_TS", "COLD_TS", 6, 60);

            assertThat(settings.cron()).isEqualTo("0 0 2 1 * *");
        }

        @Test
        @DisplayName("Given 공백 cron When 생성하면 Then 기본 cron 표현식이 설정된다")
        void blankCronSetsDefault() {
            AuditPartitionSettings settings = new AuditPartitionSettings(
                    true, "   ", 3, "HOT_TS", "COLD_TS", 6, 60);

            assertThat(settings.cron()).isEqualTo("0 0 2 1 * *");
        }

        @Test
        @DisplayName("Given 음수 preloadMonths When 생성하면 Then 0으로 설정된다")
        void negativePreloadMonthsSetsZero() {
            AuditPartitionSettings settings = new AuditPartitionSettings(
                    true, "0 0 * * * *", -5, "HOT_TS", "COLD_TS", 6, 60);

            assertThat(settings.preloadMonths()).isZero();
        }

        @Test
        @DisplayName("Given 0 hotMonths When 생성하면 Then 기본값 6이 설정된다")
        void zeroHotMonthsSetsDefault() {
            AuditPartitionSettings settings = new AuditPartitionSettings(
                    true, "0 0 * * * *", 3, "HOT_TS", "COLD_TS", 0, 60);

            assertThat(settings.hotMonths()).isEqualTo(6);
        }

        @Test
        @DisplayName("Given 음수 hotMonths When 생성하면 Then 기본값 6이 설정된다")
        void negativeHotMonthsSetsDefault() {
            AuditPartitionSettings settings = new AuditPartitionSettings(
                    true, "0 0 * * * *", 3, "HOT_TS", "COLD_TS", -3, 60);

            assertThat(settings.hotMonths()).isEqualTo(6);
        }

        @Test
        @DisplayName("Given 0 coldMonths When 생성하면 Then 기본값 60이 설정된다")
        void zeroColdMonthsSetsDefault() {
            AuditPartitionSettings settings = new AuditPartitionSettings(
                    true, "0 0 * * * *", 3, "HOT_TS", "COLD_TS", 6, 0);

            assertThat(settings.coldMonths()).isEqualTo(60);
        }

        @Test
        @DisplayName("Given 음수 coldMonths When 생성하면 Then 기본값 60이 설정된다")
        void negativeColdMonthsSetsDefault() {
            AuditPartitionSettings settings = new AuditPartitionSettings(
                    true, "0 0 * * * *", 3, "HOT_TS", "COLD_TS", 6, -10);

            assertThat(settings.coldMonths()).isEqualTo(60);
        }

        @Test
        @DisplayName("Given null tablespaceHot When 생성하면 Then 빈 문자열이 설정된다")
        void nullTablespaceHotSetsEmptyString() {
            AuditPartitionSettings settings = new AuditPartitionSettings(
                    true, "0 0 * * * *", 3, null, "COLD_TS", 6, 60);

            assertThat(settings.tablespaceHot()).isEmpty();
        }

        @Test
        @DisplayName("Given null tablespaceCold When 생성하면 Then 빈 문자열이 설정된다")
        void nullTablespaceColdSetsEmptyString() {
            AuditPartitionSettings settings = new AuditPartitionSettings(
                    true, "0 0 * * * *", 3, "HOT_TS", null, 6, 60);

            assertThat(settings.tablespaceCold()).isEmpty();
        }
    }

    @Nested
    @DisplayName("유효한 값 유지")
    class ValidValues {

        @Test
        @DisplayName("Given 모든 유효한 값 When 생성하면 Then 값이 그대로 유지된다")
        void validValuesArePreserved() {
            AuditPartitionSettings settings = new AuditPartitionSettings(
                    true,
                    "0 30 3 * * *",
                    12,
                    "AUDIT_HOT",
                    "AUDIT_COLD",
                    12,
                    120
            );

            assertThat(settings.enabled()).isTrue();
            assertThat(settings.cron()).isEqualTo("0 30 3 * * *");
            assertThat(settings.preloadMonths()).isEqualTo(12);
            assertThat(settings.tablespaceHot()).isEqualTo("AUDIT_HOT");
            assertThat(settings.tablespaceCold()).isEqualTo("AUDIT_COLD");
            assertThat(settings.hotMonths()).isEqualTo(12);
            assertThat(settings.coldMonths()).isEqualTo(120);
        }

        @Test
        @DisplayName("Given enabled false When 생성하면 Then disabled 상태로 유지된다")
        void disabledSettingsArePreserved() {
            AuditPartitionSettings settings = new AuditPartitionSettings(
                    false, "0 0 * * * *", 0, "", "", 6, 60);

            assertThat(settings.enabled()).isFalse();
        }

        @Test
        @DisplayName("Given 0 preloadMonths When 생성하면 Then 0으로 유지된다")
        void zeroPreloadMonthsIsPreserved() {
            AuditPartitionSettings settings = new AuditPartitionSettings(
                    true, "0 0 * * * *", 0, "HOT", "COLD", 6, 60);

            assertThat(settings.preloadMonths()).isZero();
        }

        @Test
        @DisplayName("Given 양수 preloadMonths When 생성하면 Then 그대로 유지된다")
        void positivePreloadMonthsIsPreserved() {
            AuditPartitionSettings settings = new AuditPartitionSettings(
                    true, "0 0 * * * *", 24, "HOT", "COLD", 6, 60);

            assertThat(settings.preloadMonths()).isEqualTo(24);
        }
    }

    @Nested
    @DisplayName("레코드 동등성")
    class RecordEquality {

        @Test
        @DisplayName("Given 동일한 값 When equals 호출하면 Then true 반환")
        void equalValuesAreEqual() {
            AuditPartitionSettings settings1 = new AuditPartitionSettings(
                    true, "0 0 * * * *", 3, "HOT", "COLD", 6, 60);
            AuditPartitionSettings settings2 = new AuditPartitionSettings(
                    true, "0 0 * * * *", 3, "HOT", "COLD", 6, 60);

            assertThat(settings1).isEqualTo(settings2);
            assertThat(settings1.hashCode()).isEqualTo(settings2.hashCode());
        }

        @Test
        @DisplayName("Given 다른 enabled When equals 호출하면 Then false 반환")
        void differentEnabledNotEqual() {
            AuditPartitionSettings settings1 = new AuditPartitionSettings(
                    true, "0 0 * * * *", 3, "HOT", "COLD", 6, 60);
            AuditPartitionSettings settings2 = new AuditPartitionSettings(
                    false, "0 0 * * * *", 3, "HOT", "COLD", 6, 60);

            assertThat(settings1).isNotEqualTo(settings2);
        }
    }

    @Nested
    @DisplayName("모든 기본값 적용")
    class AllDefaults {

        @Test
        @DisplayName("Given 모든 필드에 기본값 필요 When 생성하면 Then 모두 기본값 적용")
        void allDefaultsApplied() {
            AuditPartitionSettings settings = new AuditPartitionSettings(
                    true, null, -1, null, null, 0, -1);

            assertThat(settings.cron()).isEqualTo("0 0 2 1 * *");
            assertThat(settings.preloadMonths()).isZero();
            assertThat(settings.tablespaceHot()).isEmpty();
            assertThat(settings.tablespaceCold()).isEmpty();
            assertThat(settings.hotMonths()).isEqualTo(6);
            assertThat(settings.coldMonths()).isEqualTo(60);
        }
    }
}
