package com.example.dw.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.audit.AuditPort;
import com.example.dw.domain.DwHolidayEntity;
import com.example.dw.infrastructure.persistence.DwHolidayRepository;

@ExtendWith(MockitoExtension.class)
class DwHolidayDirectoryServiceTest {

    @Mock
    private DwHolidayRepository holidayRepository;
    @Mock
    private AuditPort auditPort;

    private DwHolidayDirectoryService service;

    @BeforeEach
    void setUp() {
        service = new DwHolidayDirectoryService(holidayRepository, auditPort);
    }

    @Nested
    @DisplayName("Given: 특정 날짜와 국가코드로 휴일 조회 시")
    class FindByDateAndCountryTest {

        @Test
        @DisplayName("When: 휴일이 존재하면 Then: 스냅샷 반환")
        void givenExistingHoliday_whenFindByDateAndCountry_thenReturnSnapshot() {
            LocalDate date = LocalDate.of(2025, 1, 1);
            DwHolidayEntity entity = holiday(date, "KR", "신정", "New Year's Day");
            given(holidayRepository.findFirstByHolidayDateAndCountryCode(date, "KR"))
                    .willReturn(Optional.of(entity));

            Optional<DwHolidaySnapshot> result = service.findByDateAndCountry(date, "KR");

            assertThat(result).isPresent();
            assertThat(result.orElseThrow().localName()).isEqualTo("신정");
            assertThat(result.orElseThrow().countryCode()).isEqualTo("KR");
            verify(holidayRepository).findFirstByHolidayDateAndCountryCode(date, "KR");
        }

        @Test
        @DisplayName("When: 휴일이 존재하지 않으면 Then: 빈 Optional 반환")
        void givenNoHoliday_whenFindByDateAndCountry_thenReturnEmpty() {
            LocalDate date = LocalDate.of(2025, 1, 2);
            given(holidayRepository.findFirstByHolidayDateAndCountryCode(date, "KR"))
                    .willReturn(Optional.empty());

            Optional<DwHolidaySnapshot> result = service.findByDateAndCountry(date, "KR");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Given: 휴일 여부 확인 시")
    class IsHolidayTest {

        @Test
        @DisplayName("When: 해당 날짜가 휴일이면 Then: true 반환")
        void givenHolidayExists_whenIsHoliday_thenReturnTrue() {
            LocalDate date = LocalDate.of(2025, 3, 1);
            given(holidayRepository.existsByHolidayDateAndCountryCode(date, "KR")).willReturn(true);

            boolean result = service.isHoliday(date, "KR");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("When: 해당 날짜가 휴일이 아니면 Then: false 반환")
        void givenNoHoliday_whenIsHoliday_thenReturnFalse() {
            LocalDate date = LocalDate.of(2025, 3, 2);
            given(holidayRepository.existsByHolidayDateAndCountryCode(date, "KR")).willReturn(false);

            boolean result = service.isHoliday(date, "KR");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Given: 국가코드로 휴일 목록 조회 시")
    class FindByCountryTest {

        @Test
        @DisplayName("When: 휴일이 존재하면 Then: 날짜순 정렬된 목록 반환")
        void givenHolidaysExist_whenFindByCountry_thenReturnSortedList() {
            DwHolidayEntity holiday1 = holiday(LocalDate.of(2025, 1, 1), "KR", "신정", "New Year's Day");
            DwHolidayEntity holiday2 = holiday(LocalDate.of(2025, 3, 1), "KR", "삼일절", "Independence Movement Day");
            given(holidayRepository.findByCountryCodeOrderByHolidayDateAsc("KR"))
                    .willReturn(List.of(holiday1, holiday2));

            List<DwHolidaySnapshot> result = service.findByCountry("KR");

            assertThat(result).hasSize(2);
            assertThat(result.get(0).localName()).isEqualTo("신정");
            assertThat(result.get(1).localName()).isEqualTo("삼일절");
        }

        @Test
        @DisplayName("When: 휴일이 없으면 Then: 빈 목록 반환")
        void givenNoHolidays_whenFindByCountry_thenReturnEmptyList() {
            given(holidayRepository.findByCountryCodeOrderByHolidayDateAsc("XX"))
                    .willReturn(List.of());

            List<DwHolidaySnapshot> result = service.findByCountry("XX");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Given: 날짜 범위로 휴일 조회 시")
    class FindByDateRangeTest {

        @Test
        @DisplayName("When: 국가코드와 날짜 범위 지정 Then: 해당 범위 휴일 반환")
        void givenCountryAndRange_whenFindByCountryAndDateRange_thenReturnHolidays() {
            LocalDate start = LocalDate.of(2025, 1, 1);
            LocalDate end = LocalDate.of(2025, 3, 31);
            DwHolidayEntity holiday = holiday(LocalDate.of(2025, 3, 1), "KR", "삼일절", "Independence Movement Day");
            given(holidayRepository.findByCountryCodeAndDateRange("KR", start, end))
                    .willReturn(List.of(holiday));

            List<DwHolidaySnapshot> result = service.findByCountryAndDateRange("KR", start, end);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).localName()).isEqualTo("삼일절");
        }

        @Test
        @DisplayName("When: 날짜 범위만 지정 Then: 모든 국가 휴일 반환")
        void givenDateRangeOnly_whenFindByDateRange_thenReturnAllCountries() {
            LocalDate start = LocalDate.of(2025, 1, 1);
            LocalDate end = LocalDate.of(2025, 1, 31);
            DwHolidayEntity krHoliday = holiday(LocalDate.of(2025, 1, 1), "KR", "신정", "New Year's Day");
            DwHolidayEntity usHoliday = holiday(LocalDate.of(2025, 1, 1), "US", "New Year's Day", "New Year's Day");
            given(holidayRepository.findByDateRange(start, end))
                    .willReturn(List.of(krHoliday, usHoliday));

            List<DwHolidaySnapshot> result = service.findByDateRange(start, end);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Given: 페이징 조회 시")
    class FindByCountryPagedTest {

        @Test
        @DisplayName("When: 페이지 요청 Then: 페이지 결과 반환")
        void givenPageable_whenFindByCountry_thenReturnPage() {
            Pageable pageable = PageRequest.of(0, 10);
            DwHolidayEntity holiday = holiday(LocalDate.of(2025, 1, 1), "KR", "신정", "New Year's Day");
            Page<DwHolidayEntity> page = new PageImpl<>(List.of(holiday), pageable, 1);
            given(holidayRepository.findByCountryCode("KR", pageable)).willReturn(page);

            Page<DwHolidaySnapshot> result = service.findByCountry("KR", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Given: 사용 가능한 국가코드 조회 시")
    class FindAvailableCountryCodesTest {

        @Test
        @DisplayName("When: 국가코드 존재 Then: 중복 제거된 목록 반환")
        void givenCountryCodes_whenFindAvailable_thenReturnDistinctList() {
            given(holidayRepository.findDistinctCountryCodes()).willReturn(List.of("KR", "US", "JP"));

            List<String> result = service.findAvailableCountryCodes();

            assertThat(result).containsExactly("KR", "US", "JP");
        }
    }

    @Nested
    @DisplayName("Given: 캐시 무효화 시")
    class EvictTest {

        @Test
        @DisplayName("When: evict 호출 Then: 예외 없이 완료")
        void whenEvictMethods_thenNoException() {
            service.evict(LocalDate.of(2025, 1, 1), "KR");
            service.evictAll();
        }
    }

    private DwHolidayEntity holiday(LocalDate date, String countryCode, String localName, String englishName) {
        return DwHolidayEntity.create(
                date,
                countryCode,
                localName,
                englishName,
                false,
                UUID.randomUUID(),
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }
}
