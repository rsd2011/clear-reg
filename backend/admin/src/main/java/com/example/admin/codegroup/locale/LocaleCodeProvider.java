package com.example.admin.codegroup.locale;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.codegroup.domain.CodeGroup;
import com.example.admin.codegroup.domain.CodeGroupSource;
import com.example.admin.codegroup.domain.CodeItem;
import com.example.admin.codegroup.locale.dto.LocaleCountryMetadata;
import com.example.admin.codegroup.locale.dto.LocaleLanguageMetadata;
import com.example.admin.codegroup.service.CodeGroupService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Locale 코드 제공 서비스.
 *
 * <p>Java Locale API 기반 ISO 국가/언어 코드를 제공합니다.</p>
 *
 * <h3>주요 기능</h3>
 * <ul>
 *   <li>ISO 3166-1 국가 코드 (alpha-2, alpha-3, numeric)</li>
 *   <li>ISO 639 언어 코드 (alpha-2, alpha-3)</li>
 *   <li>한국어 표시명, 한국어 검색</li>
 *   <li>DB 오버라이드 지원 (커스텀 항목 추가, 이름 변경)</li>
 *   <li>삭제 시 ISO 원본 복원 (builtIn=true 항목)</li>
 * </ul>
 *
 * <h3>캐싱 전략</h3>
 * <ul>
 *   <li>ISO 원본 데이터: 애플리케이션 레벨 캐싱 (불변)</li>
 *   <li>DB 오버라이드 병합 결과: Spring Cache 적용</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocaleCodeProvider {

    private static final Locale DISPLAY_LOCALE = Locale.KOREAN;
    private static final String CACHE_COUNTRIES = "locale:countries";
    private static final String CACHE_LANGUAGES = "locale:languages";

    /**
     * ISO 국가 코드 그룹 코드.
     */
    public static final String GROUP_CODE_COUNTRY = "LOCALE_COUNTRY";

    /**
     * ISO 언어 코드 그룹 코드.
     */
    public static final String GROUP_CODE_LANGUAGE = "LOCALE_LANGUAGE";

    private final CodeGroupService codeGroupService;
    private final ObjectMapper objectMapper;

    // ========== ISO 원본 데이터 (불변, 클래스 레벨 캐싱) ==========

    private volatile List<LocaleCountryEntry> cachedIsoCountries;
    private volatile List<LocaleLanguageEntry> cachedIsoLanguages;

    /**
     * ISO 국가 목록 조회 (DB 오버라이드 적용).
     *
     * @return 국가 목록 (한국어명 가나다순 정렬)
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_COUNTRIES, key = "'all'")
    public List<LocaleCountryEntry> getCountries() {
        List<LocaleCountryEntry> isoCountries = getIsoCountries();
        Map<String, CodeItem> overrides = getDbOverrides(CodeGroupSource.LOCALE_COUNTRY, GROUP_CODE_COUNTRY);

        List<LocaleCountryEntry> result = new ArrayList<>();

        // 1. ISO 항목에 오버라이드 적용
        for (LocaleCountryEntry iso : isoCountries) {
            CodeItem override = overrides.get(iso.code());
            if (override != null) {
                // 오버라이드된 이름 적용
                result.add(new LocaleCountryEntry(
                        iso.code(),
                        override.getItemName(),
                        iso.metadata(),
                        true,  // builtIn
                        true   // hasOverride
                ));
                overrides.remove(iso.code());
            } else {
                result.add(iso);
            }
        }

        // 2. 커스텀 추가 항목 (ISO에 없는 항목)
        for (CodeItem custom : overrides.values()) {
            if (custom.isActive()) {
                LocaleCountryMetadata metadata = parseCountryMetadata(custom.getMetadataJson());
                result.add(new LocaleCountryEntry(
                        custom.getItemCode(),
                        custom.getItemName(),
                        metadata,
                        false,  // builtIn (커스텀)
                        false   // hasOverride
                ));
            }
        }

        // 3. 한국어명 가나다순 정렬
        result.sort(Comparator.comparing(LocaleCountryEntry::name));

        return result;
    }

    /**
     * ISO 언어 목록 조회 (DB 오버라이드 적용).
     *
     * @return 언어 목록 (한국어명 가나다순 정렬)
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_LANGUAGES, key = "'all'")
    public List<LocaleLanguageEntry> getLanguages() {
        List<LocaleLanguageEntry> isoLanguages = getIsoLanguages();
        Map<String, CodeItem> overrides = getDbOverrides(CodeGroupSource.LOCALE_LANGUAGE, GROUP_CODE_LANGUAGE);

        List<LocaleLanguageEntry> result = new ArrayList<>();

        // 1. ISO 항목에 오버라이드 적용
        for (LocaleLanguageEntry iso : isoLanguages) {
            CodeItem override = overrides.get(iso.code());
            if (override != null) {
                result.add(new LocaleLanguageEntry(
                        iso.code(),
                        override.getItemName(),
                        iso.metadata(),
                        true,
                        true
                ));
                overrides.remove(iso.code());
            } else {
                result.add(iso);
            }
        }

        // 2. 커스텀 추가 항목
        for (CodeItem custom : overrides.values()) {
            if (custom.isActive()) {
                LocaleLanguageMetadata metadata = parseLanguageMetadata(custom.getMetadataJson());
                result.add(new LocaleLanguageEntry(
                        custom.getItemCode(),
                        custom.getItemName(),
                        metadata,
                        false,
                        false
                ));
            }
        }

        // 3. 한국어명 가나다순 정렬
        result.sort(Comparator.comparing(LocaleLanguageEntry::name));

        return result;
    }

    /**
     * 국가 코드로 조회.
     *
     * @param code ISO 3166-1 alpha-2 코드
     * @return 국가 정보 (없으면 Optional.empty)
     */
    @Transactional(readOnly = true)
    public Optional<LocaleCountryEntry> getCountry(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalizedCode = code.toUpperCase(Locale.ROOT);
        return getCountries().stream()
                .filter(c -> c.code().equals(normalizedCode))
                .findFirst();
    }

    /**
     * 언어 코드로 조회.
     *
     * @param code ISO 639-1 alpha-2 코드
     * @return 언어 정보 (없으면 Optional.empty)
     */
    @Transactional(readOnly = true)
    public Optional<LocaleLanguageEntry> getLanguage(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalizedCode = code.toLowerCase(Locale.ROOT);
        return getLanguages().stream()
                .filter(l -> l.code().equals(normalizedCode))
                .findFirst();
    }

    /**
     * 국가 검색 (한국어명).
     *
     * @param keyword 검색어
     * @return 검색 결과 (한국어명 가나다순 정렬)
     */
    @Transactional(readOnly = true)
    public List<LocaleCountryEntry> searchCountries(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getCountries();
        }
        String searchKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        return getCountries().stream()
                .filter(c -> c.name().toLowerCase(Locale.ROOT).contains(searchKeyword) ||
                             c.code().toLowerCase(Locale.ROOT).contains(searchKeyword))
                .collect(Collectors.toList());
    }

    /**
     * 언어 검색 (한국어명).
     *
     * @param keyword 검색어
     * @return 검색 결과 (한국어명 가나다순 정렬)
     */
    @Transactional(readOnly = true)
    public List<LocaleLanguageEntry> searchLanguages(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getLanguages();
        }
        String searchKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        return getLanguages().stream()
                .filter(l -> l.name().toLowerCase(Locale.ROOT).contains(searchKeyword) ||
                             l.code().toLowerCase(Locale.ROOT).contains(searchKeyword))
                .collect(Collectors.toList());
    }

    /**
     * 국가 코드가 ISO 표준 코드인지 확인.
     *
     * @param code 국가 코드
     * @return ISO 표준 여부
     */
    public boolean isIsoCountryCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String normalizedCode = code.toUpperCase(Locale.ROOT);
        return getIsoCountries().stream()
                .anyMatch(c -> c.code().equals(normalizedCode));
    }

    /**
     * 언어 코드가 ISO 표준 코드인지 확인.
     *
     * @param code 언어 코드
     * @return ISO 표준 여부
     */
    public boolean isIsoLanguageCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String normalizedCode = code.toLowerCase(Locale.ROOT);
        return getIsoLanguages().stream()
                .anyMatch(l -> l.code().equals(normalizedCode));
    }

    /**
     * ISO 국가 원본 데이터 조회 (오버라이드 없이).
     *
     * @param code 국가 코드
     * @return ISO 원본 정보
     */
    public Optional<LocaleCountryEntry> getIsoCountry(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalizedCode = code.toUpperCase(Locale.ROOT);
        return getIsoCountries().stream()
                .filter(c -> c.code().equals(normalizedCode))
                .findFirst();
    }

    /**
     * ISO 언어 원본 데이터 조회 (오버라이드 없이).
     *
     * @param code 언어 코드
     * @return ISO 원본 정보
     */
    public Optional<LocaleLanguageEntry> getIsoLanguage(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalizedCode = code.toLowerCase(Locale.ROOT);
        return getIsoLanguages().stream()
                .filter(l -> l.code().equals(normalizedCode))
                .findFirst();
    }

    // ========== 캐시 무효화 ==========

    /**
     * 국가 캐시 무효화.
     */
    @CacheEvict(value = CACHE_COUNTRIES, allEntries = true)
    public void evictCountryCache() {
        log.debug("국가 캐시 무효화");
    }

    /**
     * 언어 캐시 무효화.
     */
    @CacheEvict(value = CACHE_LANGUAGES, allEntries = true)
    public void evictLanguageCache() {
        log.debug("언어 캐시 무효화");
    }

    // ========== ISO 원본 데이터 로드 (불변) ==========

    /**
     * ISO 국가 목록 (순수 Java Locale API).
     */
    private List<LocaleCountryEntry> getIsoCountries() {
        if (cachedIsoCountries == null) {
            synchronized (this) {
                if (cachedIsoCountries == null) {
                    cachedIsoCountries = loadIsoCountries();
                }
            }
        }
        return cachedIsoCountries;
    }

    /**
     * ISO 언어 목록 (순수 Java Locale API).
     */
    private List<LocaleLanguageEntry> getIsoLanguages() {
        if (cachedIsoLanguages == null) {
            synchronized (this) {
                if (cachedIsoLanguages == null) {
                    cachedIsoLanguages = loadIsoLanguages();
                }
            }
        }
        return cachedIsoLanguages;
    }

    private List<LocaleCountryEntry> loadIsoCountries() {
        log.info("ISO 국가 코드 로드 시작");
        List<LocaleCountryEntry> countries = Arrays.stream(Locale.getISOCountries())
                .map(code -> {
                    Locale locale = new Locale.Builder().setRegion(code).build();
                    String koreanName = locale.getDisplayCountry(DISPLAY_LOCALE);
                    String englishName = locale.getDisplayCountry(Locale.ENGLISH);
                    String alpha3 = getAlpha3Country(locale);
                    String numeric = getNumericCountry(code);

                    LocaleCountryMetadata metadata = LocaleCountryMetadata.ofIso(alpha3, numeric, englishName);

                    return new LocaleCountryEntry(
                            code,
                            koreanName,
                            metadata,
                            true,   // builtIn
                            false   // hasOverride
                    );
                })
                .sorted(Comparator.comparing(LocaleCountryEntry::name))
                .collect(Collectors.toList());

        log.info("ISO 국가 코드 로드 완료: {} 개국", countries.size());
        return countries;
    }

    private List<LocaleLanguageEntry> loadIsoLanguages() {
        log.info("ISO 언어 코드 로드 시작");
        List<LocaleLanguageEntry> languages = Arrays.stream(Locale.getISOLanguages())
                .map(code -> {
                    Locale locale = new Locale.Builder().setLanguage(code).build();
                    String koreanName = locale.getDisplayLanguage(DISPLAY_LOCALE);
                    String englishName = locale.getDisplayLanguage(Locale.ENGLISH);
                    String alpha3 = getAlpha3Language(locale);

                    LocaleLanguageMetadata metadata = LocaleLanguageMetadata.ofIso(alpha3, englishName);

                    return new LocaleLanguageEntry(
                            code,
                            koreanName,
                            metadata,
                            true,
                            false
                    );
                })
                .sorted(Comparator.comparing(LocaleLanguageEntry::name))
                .collect(Collectors.toList());

        log.info("ISO 언어 코드 로드 완료: {} 개 언어", languages.size());
        return languages;
    }

    // ========== 헬퍼 메서드 ==========

    private Map<String, CodeItem> getDbOverrides(CodeGroupSource source, String groupCode) {
        return codeGroupService.findGroupWithItems(source, groupCode)
                .map(CodeGroup::getItems)
                .orElse(List.of())
                .stream()
                .collect(Collectors.toMap(CodeItem::getItemCode, Function.identity(), (a, b) -> b));
    }

    private String getAlpha3Country(Locale locale) {
        try {
            return locale.getISO3Country();
        } catch (Exception e) {
            return null;
        }
    }

    private String getAlpha3Language(Locale locale) {
        try {
            return locale.getISO3Language();
        } catch (Exception e) {
            return null;
        }
    }

    private String getNumericCountry(String alpha2Code) {
        // ISO 3166-1 numeric 코드 매핑 (주요 국가)
        return switch (alpha2Code) {
            case "KR" -> "410";
            case "US" -> "840";
            case "JP" -> "392";
            case "CN" -> "156";
            case "GB" -> "826";
            case "DE" -> "276";
            case "FR" -> "250";
            case "CA" -> "124";
            case "AU" -> "036";
            case "IN" -> "356";
            default -> null;
        };
    }

    private LocaleCountryMetadata parseCountryMetadata(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, LocaleCountryMetadata.class);
        } catch (Exception e) {
            log.warn("국가 메타데이터 파싱 실패: {}", json, e);
            return null;
        }
    }

    private LocaleLanguageMetadata parseLanguageMetadata(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, LocaleLanguageMetadata.class);
        } catch (Exception e) {
            log.warn("언어 메타데이터 파싱 실패: {}", json, e);
            return null;
        }
    }

    // ========== 내부 레코드 ==========

    /**
     * 국가 엔트리.
     *
     * @param code ISO 3166-1 alpha-2 코드
     * @param name 한국어 표시명
     * @param metadata 추가 메타데이터
     * @param builtIn ISO 표준 항목 여부
     * @param hasOverride DB 오버라이드 적용 여부
     */
    public record LocaleCountryEntry(
            String code,
            String name,
            LocaleCountryMetadata metadata,
            boolean builtIn,
            boolean hasOverride
    ) {}

    /**
     * 언어 엔트리.
     *
     * @param code ISO 639-1 alpha-2 코드
     * @param name 한국어 표시명
     * @param metadata 추가 메타데이터
     * @param builtIn ISO 표준 항목 여부
     * @param hasOverride DB 오버라이드 적용 여부
     */
    public record LocaleLanguageEntry(
            String code,
            String name,
            LocaleLanguageMetadata metadata,
            boolean builtIn,
            boolean hasOverride
    ) {}
}
