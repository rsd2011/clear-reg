package com.example.admin.codegroup.locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.admin.codegroup.domain.CodeGroup;
import com.example.admin.codegroup.domain.CodeGroupSource;
import com.example.admin.codegroup.domain.CodeItem;
import com.example.admin.codegroup.locale.LocaleCodeProvider.LocaleCountryEntry;
import com.example.admin.codegroup.locale.LocaleCodeProvider.LocaleLanguageEntry;
import com.example.admin.codegroup.locale.dto.LocaleCountryMetadata;
import com.example.admin.codegroup.locale.dto.LocaleLanguageMetadata;
import com.example.admin.codegroup.service.CodeGroupService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class LocaleCodeProviderTest {

    @Mock
    private CodeGroupService codeGroupService;

    private ObjectMapper objectMapper;
    private LocaleCodeProvider localeCodeProvider;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        localeCodeProvider = new LocaleCodeProvider(codeGroupService, objectMapper);
    }

    @Nested
    @DisplayName("ISO 국가 코드 테스트")
    class CountryTests {

        @Test
        @DisplayName("Given: ISO 국가 목록 / When: getCountries 호출 / Then: 한국 포함 국가 목록 반환")
        void getCountries_returnsIsoCountries() {
            // Given
            when(codeGroupService.findGroupWithItems(eq(CodeGroupSource.LOCALE_COUNTRY), any()))
                    .thenReturn(Optional.empty());

            // When
            List<LocaleCountryEntry> countries = localeCodeProvider.getCountries();

            // Then
            assertThat(countries).isNotEmpty();
            assertThat(countries.size()).isGreaterThan(200); // ISO에는 약 249개 국가

            // 한국이 포함되어 있는지 확인
            Optional<LocaleCountryEntry> korea = countries.stream()
                    .filter(c -> "KR".equals(c.code()))
                    .findFirst();
            assertThat(korea).isPresent();
            assertThat(korea.get().name()).isEqualTo("대한민국");
            assertThat(korea.get().builtIn()).isTrue();
            assertThat(korea.get().hasOverride()).isFalse();
        }

        @Test
        @DisplayName("Given: ISO 국가 목록 / When: getCountries 호출 / Then: 한국어명 가나다순 정렬")
        void getCountries_sortedByKoreanName() {
            // Given
            when(codeGroupService.findGroupWithItems(eq(CodeGroupSource.LOCALE_COUNTRY), any()))
                    .thenReturn(Optional.empty());

            // When
            List<LocaleCountryEntry> countries = localeCodeProvider.getCountries();

            // Then
            for (int i = 0; i < countries.size() - 1; i++) {
                String current = countries.get(i).name();
                String next = countries.get(i + 1).name();
                assertThat(current.compareTo(next)).isLessThanOrEqualTo(0);
            }
        }

        @Test
        @DisplayName("Given: 국가 코드 KR / When: getCountry 호출 / Then: 대한민국 반환")
        void getCountry_returnsKorea() {
            // Given
            when(codeGroupService.findGroupWithItems(eq(CodeGroupSource.LOCALE_COUNTRY), any()))
                    .thenReturn(Optional.empty());

            // When
            Optional<LocaleCountryEntry> country = localeCodeProvider.getCountry("KR");

            // Then
            assertThat(country).isPresent();
            assertThat(country.get().code()).isEqualTo("KR");
            assertThat(country.get().name()).isEqualTo("대한민국");
            assertThat(country.get().metadata()).isNotNull();
            assertThat(country.get().metadata().alpha3()).isEqualTo("KOR");
        }

        @Test
        @DisplayName("Given: 소문자 국가 코드 / When: getCountry 호출 / Then: 대소문자 무관 조회")
        void getCountry_caseInsensitive() {
            // Given
            when(codeGroupService.findGroupWithItems(eq(CodeGroupSource.LOCALE_COUNTRY), any()))
                    .thenReturn(Optional.empty());

            // When
            Optional<LocaleCountryEntry> country = localeCodeProvider.getCountry("kr");

            // Then
            assertThat(country).isPresent();
            assertThat(country.get().code()).isEqualTo("KR");
        }

        @Test
        @DisplayName("Given: 존재하지 않는 코드 / When: getCountry 호출 / Then: Optional.empty 반환")
        void getCountry_notFound() {
            // Given
            when(codeGroupService.findGroupWithItems(eq(CodeGroupSource.LOCALE_COUNTRY), any()))
                    .thenReturn(Optional.empty());

            // When
            Optional<LocaleCountryEntry> country = localeCodeProvider.getCountry("XX");

            // Then
            assertThat(country).isEmpty();
        }

        @Test
        @DisplayName("Given: 검색어 '대한' / When: searchCountries 호출 / Then: 대한민국 포함 결과 반환")
        void searchCountries_byKoreanName() {
            // Given
            when(codeGroupService.findGroupWithItems(eq(CodeGroupSource.LOCALE_COUNTRY), any()))
                    .thenReturn(Optional.empty());

            // When
            List<LocaleCountryEntry> results = localeCodeProvider.searchCountries("대한");

            // Then
            assertThat(results).isNotEmpty();
            assertThat(results).anyMatch(c -> "대한민국".equals(c.name()));
        }

        @Test
        @DisplayName("Given: 검색어 'KR' / When: searchCountries 호출 / Then: 코드로도 검색 가능")
        void searchCountries_byCode() {
            // Given
            when(codeGroupService.findGroupWithItems(eq(CodeGroupSource.LOCALE_COUNTRY), any()))
                    .thenReturn(Optional.empty());

            // When
            List<LocaleCountryEntry> results = localeCodeProvider.searchCountries("KR");

            // Then
            assertThat(results).isNotEmpty();
            assertThat(results).anyMatch(c -> "KR".equals(c.code()));
        }

        @Test
        @DisplayName("Given: KR 코드 / When: isIsoCountryCode 호출 / Then: true 반환")
        void isIsoCountryCode_validCode() {
            // When & Then
            assertThat(localeCodeProvider.isIsoCountryCode("KR")).isTrue();
            assertThat(localeCodeProvider.isIsoCountryCode("US")).isTrue();
            assertThat(localeCodeProvider.isIsoCountryCode("JP")).isTrue();
        }

        @Test
        @DisplayName("Given: 유효하지 않은 코드 / When: isIsoCountryCode 호출 / Then: false 반환")
        void isIsoCountryCode_invalidCode() {
            // When & Then
            assertThat(localeCodeProvider.isIsoCountryCode("XX")).isFalse();
            assertThat(localeCodeProvider.isIsoCountryCode("ZZ")).isFalse();
            assertThat(localeCodeProvider.isIsoCountryCode(null)).isFalse();
            assertThat(localeCodeProvider.isIsoCountryCode("")).isFalse();
        }

        @Test
        @DisplayName("Given: DB 오버라이드 존재 / When: getCountries 호출 / Then: 오버라이드된 이름 적용")
        void getCountries_withDbOverride() {
            // Given
            CodeGroup group = CodeGroup.create(
                    CodeGroupSource.LOCALE_COUNTRY, "LOCALE_COUNTRY", "국가 코드", null,
                    true, null, 0, "system", null);
            CodeItem override = CodeItem.createLocaleItem(
                    group, "KR", "한국 (커스텀 이름)", 0, null, true, "admin");
            group.getItems().add(override);

            when(codeGroupService.findGroupWithItems(eq(CodeGroupSource.LOCALE_COUNTRY), any()))
                    .thenReturn(Optional.of(group));

            // When - 캐시 초기화를 위해 새 인스턴스 생성
            LocaleCodeProvider freshProvider = new LocaleCodeProvider(codeGroupService, objectMapper);
            List<LocaleCountryEntry> countries = freshProvider.getCountries();

            // Then
            Optional<LocaleCountryEntry> korea = countries.stream()
                    .filter(c -> "KR".equals(c.code()))
                    .findFirst();
            assertThat(korea).isPresent();
            assertThat(korea.get().name()).isEqualTo("한국 (커스텀 이름)");
            assertThat(korea.get().builtIn()).isTrue();
            assertThat(korea.get().hasOverride()).isTrue();
        }
    }

    @Nested
    @DisplayName("ISO 언어 코드 테스트")
    class LanguageTests {

        @Test
        @DisplayName("Given: ISO 언어 목록 / When: getLanguages 호출 / Then: 한국어 포함 언어 목록 반환")
        void getLanguages_returnsIsoLanguages() {
            // Given
            when(codeGroupService.findGroupWithItems(eq(CodeGroupSource.LOCALE_LANGUAGE), any()))
                    .thenReturn(Optional.empty());

            // When
            List<LocaleLanguageEntry> languages = localeCodeProvider.getLanguages();

            // Then
            assertThat(languages).isNotEmpty();
            assertThat(languages.size()).isGreaterThan(150); // ISO 639-1에는 약 184개 언어

            // 한국어가 포함되어 있는지 확인
            Optional<LocaleLanguageEntry> korean = languages.stream()
                    .filter(l -> "ko".equals(l.code()))
                    .findFirst();
            assertThat(korean).isPresent();
            assertThat(korean.get().name()).isEqualTo("한국어");
            assertThat(korean.get().builtIn()).isTrue();
        }

        @Test
        @DisplayName("Given: 언어 코드 ko / When: getLanguage 호출 / Then: 한국어 반환")
        void getLanguage_returnsKorean() {
            // Given
            when(codeGroupService.findGroupWithItems(eq(CodeGroupSource.LOCALE_LANGUAGE), any()))
                    .thenReturn(Optional.empty());

            // When
            Optional<LocaleLanguageEntry> language = localeCodeProvider.getLanguage("ko");

            // Then
            assertThat(language).isPresent();
            assertThat(language.get().code()).isEqualTo("ko");
            assertThat(language.get().name()).isEqualTo("한국어");
            assertThat(language.get().metadata()).isNotNull();
            assertThat(language.get().metadata().alpha3()).isEqualTo("kor");
        }

        @Test
        @DisplayName("Given: 대문자 언어 코드 / When: getLanguage 호출 / Then: 대소문자 무관 조회")
        void getLanguage_caseInsensitive() {
            // Given
            when(codeGroupService.findGroupWithItems(eq(CodeGroupSource.LOCALE_LANGUAGE), any()))
                    .thenReturn(Optional.empty());

            // When
            Optional<LocaleLanguageEntry> language = localeCodeProvider.getLanguage("KO");

            // Then
            assertThat(language).isPresent();
            assertThat(language.get().code()).isEqualTo("ko");
        }

        @Test
        @DisplayName("Given: 검색어 '한국' / When: searchLanguages 호출 / Then: 한국어 포함 결과 반환")
        void searchLanguages_byKoreanName() {
            // Given
            when(codeGroupService.findGroupWithItems(eq(CodeGroupSource.LOCALE_LANGUAGE), any()))
                    .thenReturn(Optional.empty());

            // When
            List<LocaleLanguageEntry> results = localeCodeProvider.searchLanguages("한국");

            // Then
            assertThat(results).isNotEmpty();
            assertThat(results).anyMatch(l -> "한국어".equals(l.name()));
        }

        @Test
        @DisplayName("Given: ko 코드 / When: isIsoLanguageCode 호출 / Then: true 반환")
        void isIsoLanguageCode_validCode() {
            // When & Then
            assertThat(localeCodeProvider.isIsoLanguageCode("ko")).isTrue();
            assertThat(localeCodeProvider.isIsoLanguageCode("en")).isTrue();
            assertThat(localeCodeProvider.isIsoLanguageCode("ja")).isTrue();
        }

        @Test
        @DisplayName("Given: 유효하지 않은 코드 / When: isIsoLanguageCode 호출 / Then: false 반환")
        void isIsoLanguageCode_invalidCode() {
            // When & Then
            assertThat(localeCodeProvider.isIsoLanguageCode("xx")).isFalse();
            assertThat(localeCodeProvider.isIsoLanguageCode(null)).isFalse();
            assertThat(localeCodeProvider.isIsoLanguageCode("")).isFalse();
        }
    }

    @Nested
    @DisplayName("ISO 원본 데이터 조회 테스트")
    class IsoOriginalDataTests {

        @Test
        @DisplayName("Given: KR 코드 / When: getIsoCountry 호출 / Then: 오버라이드 없는 ISO 원본 반환")
        void getIsoCountry_returnsOriginal() {
            // When
            Optional<LocaleCountryEntry> country = localeCodeProvider.getIsoCountry("KR");

            // Then
            assertThat(country).isPresent();
            assertThat(country.get().code()).isEqualTo("KR");
            assertThat(country.get().name()).isEqualTo("대한민국");
            assertThat(country.get().hasOverride()).isFalse();
        }

        @Test
        @DisplayName("Given: ko 코드 / When: getIsoLanguage 호출 / Then: 오버라이드 없는 ISO 원본 반환")
        void getIsoLanguage_returnsOriginal() {
            // When
            Optional<LocaleLanguageEntry> language = localeCodeProvider.getIsoLanguage("ko");

            // Then
            assertThat(language).isPresent();
            assertThat(language.get().code()).isEqualTo("ko");
            assertThat(language.get().name()).isEqualTo("한국어");
            assertThat(language.get().hasOverride()).isFalse();
        }
    }

    @Nested
    @DisplayName("메타데이터 테스트")
    class MetadataTests {

        @Test
        @DisplayName("Given: 국가 메타데이터 / When: 조회 / Then: alpha3, numeric, englishName 포함")
        void countryMetadata_hasAllFields() {
            // Given
            when(codeGroupService.findGroupWithItems(eq(CodeGroupSource.LOCALE_COUNTRY), any()))
                    .thenReturn(Optional.empty());

            // When
            Optional<LocaleCountryEntry> us = localeCodeProvider.getCountry("US");

            // Then
            assertThat(us).isPresent();
            LocaleCountryMetadata metadata = us.get().metadata();
            assertThat(metadata).isNotNull();
            assertThat(metadata.alpha3()).isEqualTo("USA");
            assertThat(metadata.numeric()).isEqualTo("840");
            assertThat(metadata.englishName()).isEqualTo("United States");
        }

        @Test
        @DisplayName("Given: 언어 메타데이터 / When: 조회 / Then: alpha3, englishName 포함")
        void languageMetadata_hasAllFields() {
            // Given
            when(codeGroupService.findGroupWithItems(eq(CodeGroupSource.LOCALE_LANGUAGE), any()))
                    .thenReturn(Optional.empty());

            // When
            Optional<LocaleLanguageEntry> en = localeCodeProvider.getLanguage("en");

            // Then
            assertThat(en).isPresent();
            LocaleLanguageMetadata metadata = en.get().metadata();
            assertThat(metadata).isNotNull();
            assertThat(metadata.alpha3()).isEqualTo("eng");
            assertThat(metadata.englishName()).isEqualTo("English");
        }
    }

    @Nested
    @DisplayName("커스텀 항목 추가 테스트")
    class CustomItemTests {

        @Test
        @DisplayName("Given: 커스텀 국가 추가 / When: getCountries 호출 / Then: 커스텀 항목 포함")
        void getCountries_withCustomItem() throws Exception {
            // Given
            CodeGroup group = CodeGroup.create(
                    CodeGroupSource.LOCALE_COUNTRY, "LOCALE_COUNTRY", "국가 코드", null,
                    true, null, 0, "system", null);

            // 커스텀 국가 추가 (ISO에 없는 코드 - XY는 ISO에 없음)
            String customMetadata = objectMapper.writeValueAsString(
                    LocaleCountryMetadata.ofCustom("XYZ", "999", "Custom Country"));
            CodeItem customItem = CodeItem.createLocaleItem(
                    group, "XY", "커스텀 국가", 0, customMetadata, false, "admin");
            group.getItems().add(customItem);

            when(codeGroupService.findGroupWithItems(eq(CodeGroupSource.LOCALE_COUNTRY), any()))
                    .thenReturn(Optional.of(group));

            // When
            LocaleCodeProvider freshProvider = new LocaleCodeProvider(codeGroupService, objectMapper);
            List<LocaleCountryEntry> countries = freshProvider.getCountries();

            // Then
            Optional<LocaleCountryEntry> custom = countries.stream()
                    .filter(c -> "XY".equals(c.code()))
                    .findFirst();
            assertThat(custom).isPresent();
            assertThat(custom.get().name()).isEqualTo("커스텀 국가");
            assertThat(custom.get().builtIn()).isFalse();
            assertThat(custom.get().hasOverride()).isFalse();
        }

        @Test
        @DisplayName("Given: 커스텀 언어 추가 / When: getLanguages 호출 / Then: 커스텀 항목 포함")
        void getLanguages_withCustomItem() throws Exception {
            // Given
            CodeGroup group = CodeGroup.create(
                    CodeGroupSource.LOCALE_LANGUAGE, "LOCALE_LANGUAGE", "언어 코드", null,
                    true, null, 0, "system", null);

            String customMetadata = objectMapper.writeValueAsString(
                    LocaleLanguageMetadata.ofCustom("cst", "Custom Language"));
            CodeItem customItem = CodeItem.createLocaleItem(
                    group, "cx", "커스텀 언어", 0, customMetadata, false, "admin");
            group.getItems().add(customItem);

            when(codeGroupService.findGroupWithItems(eq(CodeGroupSource.LOCALE_LANGUAGE), any()))
                    .thenReturn(Optional.of(group));

            // When
            LocaleCodeProvider freshProvider = new LocaleCodeProvider(codeGroupService, objectMapper);
            List<LocaleLanguageEntry> languages = freshProvider.getLanguages();

            // Then
            Optional<LocaleLanguageEntry> custom = languages.stream()
                    .filter(l -> "cx".equals(l.code()))
                    .findFirst();
            assertThat(custom).isPresent();
            assertThat(custom.get().name()).isEqualTo("커스텀 언어");
            assertThat(custom.get().builtIn()).isFalse();
        }

        @Test
        @DisplayName("Given: 비활성 커스텀 항목 / When: getCountries 호출 / Then: 비활성 항목 제외")
        void getCountries_excludesInactiveCustomItems() {
            // Given
            CodeGroup group = CodeGroup.create(
                    CodeGroupSource.LOCALE_COUNTRY, "LOCALE_COUNTRY", "국가 코드", null,
                    true, null, 0, "system", null);

            // 비활성 커스텀 항목 (ISO에 없는 XZ 코드)
            CodeItem inactiveItem = CodeItem.create(
                    group, "XZ", "비활성 국가", 0, false, null, null, "admin", null);
            group.getItems().add(inactiveItem);

            when(codeGroupService.findGroupWithItems(eq(CodeGroupSource.LOCALE_COUNTRY), any()))
                    .thenReturn(Optional.of(group));

            // When
            LocaleCodeProvider freshProvider = new LocaleCodeProvider(codeGroupService, objectMapper);
            List<LocaleCountryEntry> countries = freshProvider.getCountries();

            // Then - 비활성 커스텀 항목은 제외되어야 함
            assertThat(countries.stream().noneMatch(c -> "XZ".equals(c.code()))).isTrue();
        }
    }

    @Nested
    @DisplayName("캐시 무효화 테스트")
    class CacheEvictionTests {

        @Test
        @DisplayName("Given: 캐시된 데이터 / When: evictCountryCache 호출 / Then: 캐시 무효화 성공")
        void evictCountryCache() {
            // When & Then (예외 없이 실행)
            localeCodeProvider.evictCountryCache();
        }

        @Test
        @DisplayName("Given: 캐시된 데이터 / When: evictLanguageCache 호출 / Then: 캐시 무효화 성공")
        void evictLanguageCache() {
            // When & Then (예외 없이 실행)
            localeCodeProvider.evictLanguageCache();
        }
    }

    @Nested
    @DisplayName("Null/빈값 처리 테스트")
    class NullHandlingTests {

        @Test
        @DisplayName("Given: null 코드 / When: getCountry 호출 / Then: Optional.empty 반환")
        void getCountry_nullCode() {
            // When
            Optional<LocaleCountryEntry> result = localeCodeProvider.getCountry(null);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given: 빈 문자열 / When: getLanguage 호출 / Then: Optional.empty 반환")
        void getLanguage_emptyCode() {
            // When
            Optional<LocaleLanguageEntry> result = localeCodeProvider.getLanguage("");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given: null 검색어 / When: searchCountries 호출 / Then: 전체 목록 반환")
        void searchCountries_nullKeyword() {
            // Given
            when(codeGroupService.findGroupWithItems(eq(CodeGroupSource.LOCALE_COUNTRY), any()))
                    .thenReturn(Optional.empty());

            // When
            List<LocaleCountryEntry> results = localeCodeProvider.searchCountries(null);

            // Then
            assertThat(results).isNotEmpty();
            assertThat(results.size()).isGreaterThan(200);
        }
    }
}
