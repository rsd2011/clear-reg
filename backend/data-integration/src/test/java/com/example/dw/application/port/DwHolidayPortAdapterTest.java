package com.example.dw.application.port;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dw.application.DwHolidayDirectoryService;
import com.example.dw.application.DwHolidaySnapshot;

@ExtendWith(MockitoExtension.class)
@DisplayName("DwHolidayPortAdapter 테스트")
class DwHolidayPortAdapterTest {

    @Mock
    private DwHolidayDirectoryService holidayService;

    @InjectMocks
    private DwHolidayPortAdapter adapter;

    private DwHolidaySnapshot createTestSnapshot(LocalDate date, String countryCode, String name) {
        return new DwHolidaySnapshot(UUID.randomUUID(), date, countryCode, name, name, false,
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Nested
    @DisplayName("findByDateAndCountry")
    class FindByDateAndCountry {

        @Test
        @DisplayName("Given 휴일이 존재할 때 When 조회하면 Then 휴일 레코드를 반환한다")
        void givenHoliday_whenQuerying_thenReturnRecord() {
            LocalDate date = LocalDate.of(2025, 1, 1);
            DwHolidaySnapshot snapshot = createTestSnapshot(date, "KR", "신정");
            given(holidayService.findByDateAndCountry(date, "KR")).willReturn(Optional.of(snapshot));

            Optional<DwHolidayPort.DwHolidayRecord> result = adapter.findByDateAndCountry(date, "KR");

            assertThat(result).isPresent();
            assertThat(result.get().localName()).isEqualTo("신정");
        }

        @Test
        @DisplayName("Given 휴일이 없을 때 When 조회하면 Then 빈 Optional을 반환한다")
        void givenNoHoliday_whenQuerying_thenReturnEmpty() {
            LocalDate date = LocalDate.of(2025, 1, 2);
            given(holidayService.findByDateAndCountry(date, "KR")).willReturn(Optional.empty());

            Optional<DwHolidayPort.DwHolidayRecord> result = adapter.findByDateAndCountry(date, "KR");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("isHoliday")
    class IsHoliday {

        @Test
        @DisplayName("Given 휴일인 날짜 When 확인하면 Then true를 반환한다")
        void givenHoliday_whenChecking_thenReturnTrue() {
            LocalDate date = LocalDate.of(2025, 1, 1);
            given(holidayService.isHoliday(date, "KR")).willReturn(true);

            boolean result = adapter.isHoliday(date, "KR");

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("findByCountry")
    class FindByCountry {

        @Test
        @DisplayName("Given 휴일이 존재할 때 When 국가로 조회하면 Then 휴일 목록을 반환한다")
        void givenHolidays_whenQuerying_thenReturnList() {
            DwHolidaySnapshot snapshot = createTestSnapshot(LocalDate.of(2025, 1, 1), "KR", "신정");
            given(holidayService.findByCountry("KR")).willReturn(List.of(snapshot));

            List<DwHolidayPort.DwHolidayRecord> result = adapter.findByCountry("KR");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).countryCode()).isEqualTo("KR");
        }
    }

    @Nested
    @DisplayName("findByCountryAndDateRange")
    class FindByCountryAndDateRange {

        @Test
        @DisplayName("Given 범위 내 휴일이 존재할 때 When 조회하면 Then 휴일 목록을 반환한다")
        void givenHolidaysInRange_whenQuerying_thenReturnList() {
            LocalDate start = LocalDate.of(2025, 1, 1);
            LocalDate end = LocalDate.of(2025, 12, 31);
            DwHolidaySnapshot snapshot = createTestSnapshot(LocalDate.of(2025, 3, 1), "KR", "삼일절");
            given(holidayService.findByCountryAndDateRange("KR", start, end)).willReturn(List.of(snapshot));

            List<DwHolidayPort.DwHolidayRecord> result = adapter.findByCountryAndDateRange("KR", start, end);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findByDateRange")
    class FindByDateRange {

        @Test
        @DisplayName("Given 범위 내 휴일이 존재할 때 When 조회하면 Then 모든 국가 휴일을 반환한다")
        void givenHolidaysInRange_whenQuerying_thenReturnAllCountries() {
            LocalDate start = LocalDate.of(2025, 1, 1);
            LocalDate end = LocalDate.of(2025, 12, 31);
            DwHolidaySnapshot kr = createTestSnapshot(LocalDate.of(2025, 1, 1), "KR", "신정");
            DwHolidaySnapshot us = createTestSnapshot(LocalDate.of(2025, 1, 1), "US", "New Year");
            given(holidayService.findByDateRange(start, end)).willReturn(List.of(kr, us));

            List<DwHolidayPort.DwHolidayRecord> result = adapter.findByDateRange(start, end);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("findAvailableCountryCodes")
    class FindAvailableCountryCodes {

        @Test
        @DisplayName("Given 국가 코드가 존재할 때 When 조회하면 Then 코드 목록을 반환한다")
        void givenCountryCodes_whenQuerying_thenReturnList() {
            given(holidayService.findAvailableCountryCodes()).willReturn(List.of("KR", "US", "JP"));

            List<String> result = adapter.findAvailableCountryCodes();

            assertThat(result).containsExactly("KR", "US", "JP");
        }
    }
}
