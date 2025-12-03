package com.example.server.web;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.dw.application.port.DwHolidayPort;
import com.example.server.web.dto.DwHolidayResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("DwHolidayController 테스트")
class DwHolidayControllerTest {

    @Mock
    private DwHolidayPort holidayPort;

    @InjectMocks
    private DwHolidayController controller;

    private DwHolidayPort.DwHolidayRecord createTestRecord(LocalDate date, String countryCode, String name) {
        return new DwHolidayPort.DwHolidayRecord(
                UUID.randomUUID(),
                date,
                countryCode,
                name,
                name,
                false,
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Nested
    @DisplayName("holidays")
    class Holidays {

        @Test
        @DisplayName("Given 국가의 휴일이 존재할 때 When 조회하면 Then 휴일 목록을 반환한다")
        void givenHolidays_whenListing_thenReturnList() {
            var record = createTestRecord(LocalDate.of(2025, 1, 1), "KR", "신정");
            given(holidayPort.findByCountry("KR")).willReturn(List.of(record));

            List<DwHolidayResponse> result = controller.holidays("KR");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).countryCode()).isEqualTo("KR");
            assertThat(result.get(0).localName()).isEqualTo("신정");
        }

        @Test
        @DisplayName("Given 휴일이 없을 때 When 조회하면 Then 빈 목록을 반환한다")
        void givenNoHolidays_whenListing_thenReturnEmptyList() {
            given(holidayPort.findByCountry("XX")).willReturn(List.of());

            List<DwHolidayResponse> result = controller.holidays("XX");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("holidaysInRange")
    class HolidaysInRange {

        @Test
        @DisplayName("Given 날짜 범위 내 휴일이 존재할 때 When 조회하면 Then 해당 휴일 목록을 반환한다")
        void givenHolidaysInRange_whenQuerying_thenReturnList() {
            LocalDate start = LocalDate.of(2025, 1, 1);
            LocalDate end = LocalDate.of(2025, 12, 31);
            var record = createTestRecord(LocalDate.of(2025, 3, 1), "KR", "삼일절");
            given(holidayPort.findByCountryAndDateRange("KR", start, end)).willReturn(List.of(record));

            List<DwHolidayResponse> result = controller.holidaysInRange("KR", start, end);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).localName()).isEqualTo("삼일절");
        }

        @Test
        @DisplayName("Given countryCode가 null일 때 When 조회하면 Then 모든 국가의 휴일을 반환한다")
        void givenNoCountryCode_whenQuerying_thenReturnAllCountries() {
            LocalDate start = LocalDate.of(2025, 1, 1);
            LocalDate end = LocalDate.of(2025, 12, 31);
            var krRecord = createTestRecord(LocalDate.of(2025, 1, 1), "KR", "신정");
            var usRecord = createTestRecord(LocalDate.of(2025, 1, 1), "US", "New Year");
            given(holidayPort.findByDateRange(start, end)).willReturn(List.of(krRecord, usRecord));

            List<DwHolidayResponse> result = controller.holidaysInRange(null, start, end);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("isHoliday")
    class IsHoliday {

        @Test
        @DisplayName("Given 해당 날짜가 휴일일 때 When 확인하면 Then true를 반환한다")
        void givenHoliday_whenChecking_thenReturnTrue() {
            LocalDate date = LocalDate.of(2025, 1, 1);
            given(holidayPort.isHoliday(date, "KR")).willReturn(true);

            ResponseEntity<Boolean> result = controller.isHoliday(date, "KR");

            assertThat(result.getBody()).isTrue();
        }

        @Test
        @DisplayName("Given 해당 날짜가 휴일이 아닐 때 When 확인하면 Then false를 반환한다")
        void givenNotHoliday_whenChecking_thenReturnFalse() {
            LocalDate date = LocalDate.of(2025, 1, 2);
            given(holidayPort.isHoliday(date, "KR")).willReturn(false);

            ResponseEntity<Boolean> result = controller.isHoliday(date, "KR");

            assertThat(result.getBody()).isFalse();
        }
    }

    @Nested
    @DisplayName("holiday")
    class Holiday {

        @Test
        @DisplayName("Given 휴일이 존재할 때 When 상세 조회하면 Then 휴일 정보를 반환한다")
        void givenHoliday_whenQuerying_thenReturnHoliday() {
            LocalDate date = LocalDate.of(2025, 1, 1);
            var record = createTestRecord(date, "KR", "신정");
            given(holidayPort.findByDateAndCountry(date, "KR")).willReturn(Optional.of(record));

            ResponseEntity<DwHolidayResponse> result = controller.holiday(date, "KR");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().localName()).isEqualTo("신정");
        }

        @Test
        @DisplayName("Given 휴일이 없을 때 When 상세 조회하면 Then 404를 반환한다")
        void givenNoHoliday_whenQuerying_thenReturnNotFound() {
            LocalDate date = LocalDate.of(2025, 1, 2);
            given(holidayPort.findByDateAndCountry(date, "KR")).willReturn(Optional.empty());

            ResponseEntity<DwHolidayResponse> result = controller.holiday(date, "KR");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("availableCountryCodes")
    class AvailableCountryCodes {

        @Test
        @DisplayName("Given 국가 코드가 존재할 때 When 조회하면 Then 코드 목록을 반환한다")
        void givenCountryCodes_whenListing_thenReturnList() {
            given(holidayPort.findAvailableCountryCodes()).willReturn(List.of("KR", "US", "JP"));

            List<String> result = controller.availableCountryCodes();

            assertThat(result).containsExactly("KR", "US", "JP");
        }
    }
}
