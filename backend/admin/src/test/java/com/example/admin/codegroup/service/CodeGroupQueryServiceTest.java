package com.example.admin.codegroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.admin.approval.service.ApprovalGroupService;
import com.example.admin.approval.dto.ApprovalGroupSummaryResponse;
import com.example.admin.codegroup.domain.CodeGroup;
import com.example.admin.codegroup.domain.CodeGroupSource;
import com.example.admin.codegroup.domain.CodeItem;
import com.example.admin.codegroup.dto.CodeGroupInfo;
import com.example.admin.codegroup.dto.CodeGroupItem;
import com.example.admin.codegroup.dto.CodeGroupItemResponse;
import com.example.admin.codegroup.dto.MigrationStatusResponse;
import com.example.admin.codegroup.locale.LocaleCodeProvider;
import com.example.admin.codegroup.locale.LocaleCodeProvider.LocaleCountryEntry;
import com.example.admin.codegroup.locale.LocaleCodeProvider.LocaleLanguageEntry;
import com.example.admin.codegroup.registry.StaticCodeRegistry;
import com.example.admin.codegroup.repository.CodeGroupRepository;
import com.example.admin.codegroup.repository.CodeItemRepository;
import com.example.dw.application.DwCommonCodeDirectoryService;
import com.example.dw.application.DwCommonCodeSnapshot;

/**
 * CodeGroupQueryService 테스트.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CodeGroupQueryService 테스트")
class CodeGroupQueryServiceTest {

    @Mock
    private DwCommonCodeDirectoryService dwCommonCodeDirectoryService;

    @Mock
    private CodeGroupService codeGroupService;

    @Mock
    private StaticCodeRegistry staticCodeRegistry;

    @Mock
    private CodeGroupRepository codeGroupRepository;

    @Mock
    private CodeItemRepository codeItemRepository;

    @Mock
    private ApprovalGroupService approvalGroupService;

    @Mock
    private LocaleCodeProvider localeCodeProvider;

    @InjectMocks
    private CodeGroupQueryService queryService;

    // 테스트용 Enum
    private enum TestStatus { ACTIVE, INACTIVE }

    @Nested
    @DisplayName("aggregateAll 테스트")
    class AggregateAllTests {

        @Test
        @DisplayName("Given: 정적, 동적, 승인그룹 데이터 / When: aggregateAll 호출 / Then: 모든 소스 통합 반환")
        void aggregatesAllSources() {
            // Given
            Set<Class<? extends Enum<?>>> enumSet = new HashSet<>();
            enumSet.add(TestStatus.class);
            given(staticCodeRegistry.getRegisteredEnums()).willReturn(enumSet);
            given(staticCodeRegistry.getCodeGroupItems(TestStatus.class))
                    .willReturn(List.of(
                            new CodeGroupItem("TEST_STATUS", "ACTIVE", "활성", 0, true, CodeGroupSource.STATIC_ENUM, null, null),
                            new CodeGroupItem("TEST_STATUS", "INACTIVE", "비활성", 1, true, CodeGroupSource.STATIC_ENUM, null, null)
                    ));

            CodeGroup dbGroup = CodeGroup.createDynamic("NOTICE_TYPE", "공지유형", null, "admin");
            CodeItem dbItem = CodeItem.create(dbGroup, "NT01", "일반공지", 0, true, null, null, "admin", null);
            dbGroup.getItems().add(dbItem);
            given(codeGroupRepository.findAllBySourceWithItems(CodeGroupSource.DYNAMIC_DB))
                    .willReturn(List.of(dbGroup));

            given(approvalGroupService.listGroupSummary(true))
                    .willReturn(List.of(new ApprovalGroupSummaryResponse(UUID.randomUUID(), "AG001", "승인그룹1")));

            // When
            Map<String, List<CodeGroupItem>> result = queryService.aggregateAll();

            // Then
            assertThat(result).containsKey("TEST_STATUS");
            assertThat(result).containsKey("NOTICE_TYPE");
            assertThat(result).containsKey("APPROVAL_GROUP");
            assertThat(result.get("TEST_STATUS")).hasSize(2);
            assertThat(result.get("NOTICE_TYPE")).hasSize(1);
            assertThat(result.get("APPROVAL_GROUP")).hasSize(1);
        }

        @Test
        @DisplayName("Given: DB에 중복 그룹코드 / When: aggregateAll 호출 / Then: 정적 우선 사용")
        void staticEnumHasPriorityOverDb() {
            // Given
            Set<Class<? extends Enum<?>>> enumSet = new HashSet<>();
            enumSet.add(TestStatus.class);
            given(staticCodeRegistry.getRegisteredEnums()).willReturn(enumSet);
            given(staticCodeRegistry.getCodeGroupItems(TestStatus.class))
                    .willReturn(List.of(
                            new CodeGroupItem("TEST_STATUS", "ACTIVE", "활성", 0, true, CodeGroupSource.STATIC_ENUM, null, null)
                    ));

            // DB에도 같은 그룹코드 존재
            CodeGroup dbGroup = CodeGroup.createDynamic("TEST_STATUS", "테스트상태", null, "admin");
            given(codeGroupRepository.findAllBySourceWithItems(CodeGroupSource.DYNAMIC_DB))
                    .willReturn(List.of(dbGroup));

            given(approvalGroupService.listGroupSummary(true)).willReturn(Collections.emptyList());

            // When
            Map<String, List<CodeGroupItem>> result = queryService.aggregateAll();

            // Then - 정적 Enum이 우선
            assertThat(result.get("TEST_STATUS")).hasSize(1);
            assertThat(result.get("TEST_STATUS").get(0).itemCode()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("Given: 승인그룹 없음 / When: aggregateAll 호출 / Then: APPROVAL_GROUP 키 없음")
        void noApprovalGroupWhenEmpty() {
            // Given
            given(staticCodeRegistry.getRegisteredEnums()).willReturn(Collections.emptySet());
            given(codeGroupRepository.findAllBySourceWithItems(CodeGroupSource.DYNAMIC_DB))
                    .willReturn(Collections.emptyList());
            given(approvalGroupService.listGroupSummary(true)).willReturn(Collections.emptyList());

            // When
            Map<String, List<CodeGroupItem>> result = queryService.aggregateAll();

            // Then
            assertThat(result).doesNotContainKey("APPROVAL_GROUP");
        }
    }

    @Nested
    @DisplayName("findByGroupCode 테스트")
    class FindByGroupCodeTests {

        @Test
        @DisplayName("Given: 정적 Enum 그룹코드 / When: findByGroupCode 호출 / Then: Enum 데이터 반환")
        void findsStaticEnumByGroupCode() {
            // Given
            given(staticCodeRegistry.findByCodeType("TEST_STATUS"))
                    .willReturn(Optional.of(TestStatus.class));
            given(staticCodeRegistry.getCodeGroupItems(TestStatus.class))
                    .willReturn(List.of(
                            new CodeGroupItem("TEST_STATUS", "ACTIVE", "활성", 0, true, CodeGroupSource.STATIC_ENUM, null, null)
                    ));

            // When
            List<CodeGroupItem> result = queryService.findByGroupCode("test_status");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).itemCode()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("Given: 동적 DB 그룹코드 / When: findByGroupCode 호출 / Then: DB 데이터 반환")
        void findsDynamicDbByGroupCode() {
            // Given
            given(staticCodeRegistry.findByCodeType("NOTICE_TYPE"))
                    .willReturn(Optional.empty());

            CodeGroup group = CodeGroup.createDynamic("NOTICE_TYPE", "공지유형", null, "admin");
            CodeItem item = CodeItem.create(group, "NT01", "일반공지", 0, true, null, null, "admin", null);
            given(codeGroupService.findActiveItems(CodeGroupSource.DYNAMIC_DB, "NOTICE_TYPE"))
                    .willReturn(List.of(item));

            // When
            List<CodeGroupItem> result = queryService.findByGroupCode("notice_type");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).itemCode()).isEqualTo("NT01");
        }
    }

    @Nested
    @DisplayName("findByGroupCodes 테스트")
    class FindByGroupCodesTests {

        @Test
        @DisplayName("Given: 여러 그룹코드 / When: findByGroupCodes 호출 / Then: 그룹별 결과 반환")
        void findsByMultipleGroupCodes() {
            // Given
            given(staticCodeRegistry.findByCodeType("GROUP_A"))
                    .willReturn(Optional.empty());
            given(codeGroupService.findActiveItems(CodeGroupSource.DYNAMIC_DB, "GROUP_A"))
                    .willReturn(Collections.emptyList());

            given(staticCodeRegistry.findByCodeType("GROUP_B"))
                    .willReturn(Optional.of(TestStatus.class));
            given(staticCodeRegistry.getCodeGroupItems(TestStatus.class))
                    .willReturn(List.of(
                            new CodeGroupItem("GROUP_B", "ITEM1", "아이템1", 0, true, CodeGroupSource.STATIC_ENUM, null, null)
                    ));

            // When
            Map<String, List<CodeGroupItem>> result = queryService.findByGroupCodes(List.of("GROUP_A", "GROUP_B"));

            // Then
            assertThat(result).hasSize(1);  // GROUP_A는 빈 결과라 포함 안됨
            assertThat(result).containsKey("GROUP_B");
        }
    }

    @Nested
    @DisplayName("getCodeGroupInfos 테스트")
    class GetCodeGroupInfosTests {

        @Test
        @DisplayName("Given: 정적 Enum과 동적 타입 / When: getCodeGroupInfos 호출 / Then: 메타정보 목록 반환")
        void returnsCodeGroupInfos() {
            // Given
            Set<Class<? extends Enum<?>>> enumSet = new HashSet<>();
            enumSet.add(TestStatus.class);
            given(staticCodeRegistry.getRegisteredEnums()).willReturn(enumSet);
            given(staticCodeRegistry.getCodeGroupItems(TestStatus.class))
                    .willReturn(List.of(
                            new CodeGroupItem("TEST_STATUS", "ACTIVE", "활성", 0, true, CodeGroupSource.STATIC_ENUM, null, null)
                    ));
            given(codeItemRepository.countBySourceAndGroupCode(eq(CodeGroupSource.DYNAMIC_DB), anyString()))
                    .willReturn(0L);

            // When
            List<CodeGroupInfo> result = queryService.getCodeGroupInfos();

            // Then
            assertThat(result).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("evictCache 테스트")
    class EvictCacheTests {

        @Test
        @DisplayName("Given: 특정 그룹코드 / When: evictCache 호출 / Then: 해당 캐시만 무효화")
        void evictsCacheForSpecificGroup() {
            // When
            queryService.evictCache("TEST_GROUP");

            // Then
            verify(staticCodeRegistry).invalidateCache("TEST_GROUP");
        }

        @Test
        @DisplayName("Given: null / When: evictCache 호출 / Then: 전체 캐시 무효화")
        void evictsAllCachesWhenNull() {
            // When
            queryService.evictCache(null);

            // Then
            verify(staticCodeRegistry).invalidateCache(null);
        }
    }

    @Nested
    @DisplayName("findAllItems 테스트")
    class FindAllItemsTests {

        @Test
        @DisplayName("Given: 소스 필터 없음 / When: findAllItems 호출 / Then: 모든 소스에서 수집")
        void collectsFromAllSourcesWhenNoFilter() {
            // Given
            given(codeGroupRepository.findAllBySourceWithItems(CodeGroupSource.STATIC_ENUM))
                    .willReturn(Collections.emptyList());
            Set<Class<? extends Enum<?>>> enumSet = new HashSet<>();
            enumSet.add(TestStatus.class);
            given(staticCodeRegistry.getRegisteredEnums()).willReturn(enumSet);
            given(staticCodeRegistry.getCodeGroupItems(TestStatus.class))
                    .willReturn(List.of(
                            new CodeGroupItem("TEST_STATUS", "ACTIVE", "활성", 0, true, CodeGroupSource.STATIC_ENUM, null, null)
                    ));
            given(codeGroupService.findAllItems(eq(CodeGroupSource.DYNAMIC_DB), anyString()))
                    .willReturn(Collections.emptyList());
            given(approvalGroupService.listGroupSummary(true))
                    .willReturn(Collections.emptyList());
            given(localeCodeProvider.getCountries())
                    .willReturn(Collections.emptyList());
            given(localeCodeProvider.getLanguages())
                    .willReturn(Collections.emptyList());

            // When
            List<CodeGroupItemResponse> result = queryService.findAllItems(null, null, null, null);

            // Then
            assertThat(result).isNotEmpty();
        }

        @Test
        @DisplayName("Given: STATIC_ENUM 소스 필터 / When: findAllItems 호출 / Then: 정적 Enum만 수집")
        void collectsOnlyStaticEnumWhenFiltered() {
            // Given
            given(codeGroupRepository.findAllBySourceWithItems(CodeGroupSource.STATIC_ENUM))
                    .willReturn(Collections.emptyList());
            Set<Class<? extends Enum<?>>> enumSet = new HashSet<>();
            enumSet.add(TestStatus.class);
            given(staticCodeRegistry.getRegisteredEnums()).willReturn(enumSet);
            given(staticCodeRegistry.getCodeGroupItems(TestStatus.class))
                    .willReturn(List.of(
                            new CodeGroupItem("TEST_STATUS", "ACTIVE", "활성", 0, true, CodeGroupSource.STATIC_ENUM, null, null)
                    ));

            // When
            List<CodeGroupItemResponse> result = queryService.findAllItems(
                    List.of(CodeGroupSource.STATIC_ENUM), null, null, null);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).source()).isEqualTo(CodeGroupSource.STATIC_ENUM);
        }

        @Test
        @DisplayName("Given: 활성 필터 / When: findAllItems 호출 / Then: 활성 아이템만 반환")
        void filtersActiveItems() {
            // Given
            given(codeGroupRepository.findAllBySourceWithItems(CodeGroupSource.STATIC_ENUM))
                    .willReturn(Collections.emptyList());
            Set<Class<? extends Enum<?>>> enumSet = new HashSet<>();
            enumSet.add(TestStatus.class);
            given(staticCodeRegistry.getRegisteredEnums()).willReturn(enumSet);
            given(staticCodeRegistry.getCodeGroupItems(TestStatus.class))
                    .willReturn(List.of(
                            new CodeGroupItem("TEST_STATUS", "ACTIVE", "활성", 0, true, CodeGroupSource.STATIC_ENUM, null, null),
                            new CodeGroupItem("TEST_STATUS", "INACTIVE", "비활성", 1, false, CodeGroupSource.STATIC_ENUM, null, null)
                    ));

            // When
            List<CodeGroupItemResponse> result = queryService.findAllItems(
                    List.of(CodeGroupSource.STATIC_ENUM), null, true, null);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).itemCode()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("Given: 검색어 / When: findAllItems 호출 / Then: 검색 결과만 반환")
        void filtersWithSearchTerm() {
            // Given
            given(codeGroupRepository.findAllBySourceWithItems(CodeGroupSource.STATIC_ENUM))
                    .willReturn(Collections.emptyList());
            Set<Class<? extends Enum<?>>> enumSet = new HashSet<>();
            enumSet.add(TestStatus.class);
            given(staticCodeRegistry.getRegisteredEnums()).willReturn(enumSet);
            given(staticCodeRegistry.getCodeGroupItems(TestStatus.class))
                    .willReturn(List.of(
                            new CodeGroupItem("TEST_STATUS", "ENABLED", "활성화됨", 0, true, CodeGroupSource.STATIC_ENUM, null, null),
                            new CodeGroupItem("TEST_STATUS", "DISABLED", "비활성화됨", 1, true, CodeGroupSource.STATIC_ENUM, null, null)
                    ));

            // When - "ENABLED"로 검색하면 정확히 ENABLED만 매칭
            List<CodeGroupItemResponse> result = queryService.findAllItems(
                    List.of(CodeGroupSource.STATIC_ENUM), null, null, "ENABLED");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).itemCode()).isEqualTo("ENABLED");
        }

        @Test
        @DisplayName("Given: 그룹코드 필터 / When: findAllItems 호출 / Then: 해당 그룹만 반환")
        void filtersWithGroupCode() {
            // Given
            given(codeGroupRepository.findAllBySourceWithItems(CodeGroupSource.STATIC_ENUM))
                    .willReturn(Collections.emptyList());
            Set<Class<? extends Enum<?>>> enumSet = new HashSet<>();
            enumSet.add(TestStatus.class);
            given(staticCodeRegistry.getRegisteredEnums()).willReturn(enumSet);
            given(staticCodeRegistry.getCodeGroupItems(TestStatus.class))
                    .willReturn(List.of(
                            new CodeGroupItem("TEST_STATUS", "ACTIVE", "활성", 0, true, CodeGroupSource.STATIC_ENUM, null, null)
                    ));

            // When
            List<CodeGroupItemResponse> result = queryService.findAllItems(
                    List.of(CodeGroupSource.STATIC_ENUM), "TEST_STATUS", null, null);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).groupCode()).isEqualTo("TEST_STATUS");
        }

        @Test
        @DisplayName("Given: 다른 그룹코드 필터 / When: findAllItems 호출 / Then: 빈 결과")
        void returnsEmptyWhenGroupCodeNotMatch() {
            // Given
            // DB 오버라이드 맵 빌드를 위해 필요
            given(codeGroupRepository.findAllBySourceWithItems(CodeGroupSource.STATIC_ENUM))
                    .willReturn(Collections.emptyList());
            // Enum 루프를 돌기 위해 필요
            Set<Class<? extends Enum<?>>> enumSet = new HashSet<>();
            enumSet.add(TestStatus.class);
            given(staticCodeRegistry.getRegisteredEnums()).willReturn(enumSet);
            // groupCode 필터가 다르면 continue로 건너뛰므로 getCodeGroupItems 호출 안됨
            // 따라서 스터빙 불필요

            // When
            List<CodeGroupItemResponse> result = queryService.findAllItems(
                    List.of(CodeGroupSource.STATIC_ENUM), "OTHER_GROUP", null, null);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given: DB 오버라이드 / When: findAllItems 호출 / Then: 오버라이드 값 적용")
        void appliesDbOverride() {
            // Given
            CodeGroup overrideGroup = CodeGroup.createStaticOverride("TEST_STATUS", "테스트상태", null, "admin");
            CodeItem overrideItem = CodeItem.create(overrideGroup, "ACTIVE", "활성 오버라이드", 10, true, "설명", "{}", "admin", null);
            overrideGroup.getItems().add(overrideItem);

            given(codeGroupRepository.findAllBySourceWithItems(CodeGroupSource.STATIC_ENUM))
                    .willReturn(List.of(overrideGroup));
            Set<Class<? extends Enum<?>>> enumSet = new HashSet<>();
            enumSet.add(TestStatus.class);
            given(staticCodeRegistry.getRegisteredEnums()).willReturn(enumSet);
            given(staticCodeRegistry.getCodeGroupItems(TestStatus.class))
                    .willReturn(List.of(
                            new CodeGroupItem("TEST_STATUS", "ACTIVE", "활성", 0, true, CodeGroupSource.STATIC_ENUM, null, null)
                    ));

            // When
            List<CodeGroupItemResponse> result = queryService.findAllItems(
                    List.of(CodeGroupSource.STATIC_ENUM), null, null, null);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).itemName()).isEqualTo("활성 오버라이드");
            assertThat(result.get(0).displayOrder()).isEqualTo(10);
            assertThat(result.get(0).hasDbOverride()).isTrue();
        }
    }

    @Nested
    @DisplayName("DW 소스 테스트")
    class DwSourceTests {

        @Test
        @DisplayName("Given: DW 소스 필터 + 비활성 필터 / When: findAllItems 호출 / Then: 빈 결과")
        void returnsEmptyWhenDwWithInactiveFilter() {
            // When
            List<CodeGroupItemResponse> result = queryService.findAllItems(
                    List.of(CodeGroupSource.DW), null, false, null);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given: DW 소스 + 그룹코드 / When: findAllItems 호출 / Then: DW 데이터 반환")
        void returnsDwItemsWithGroupCode() {
            // Given
            DwCommonCodeSnapshot snapshot = new DwCommonCodeSnapshot(
                    "DW_GROUP", "CODE1", "코드1", 0, true, "카테고리", null, null);
            given(dwCommonCodeDirectoryService.findActive("DW_GROUP"))
                    .willReturn(List.of(snapshot));

            // When
            List<CodeGroupItemResponse> result = queryService.findAllItems(
                    List.of(CodeGroupSource.DW), "DW_GROUP", null, null);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).itemCode()).isEqualTo("CODE1");
        }

        @Test
        @DisplayName("Given: DW 소스 + 검색어 / When: findAllItems 호출 / Then: 검색 결과만 반환")
        void filtersDwItemsWithSearch() {
            // Given
            DwCommonCodeSnapshot snapshot1 = new DwCommonCodeSnapshot(
                    "DW_GROUP", "CODE1", "코드1", 0, true, null, null, null);
            DwCommonCodeSnapshot snapshot2 = new DwCommonCodeSnapshot(
                    "DW_GROUP", "CODE2", "테스트", 1, true, null, null, null);
            given(dwCommonCodeDirectoryService.findActive("DW_GROUP"))
                    .willReturn(List.of(snapshot1, snapshot2));

            // When
            List<CodeGroupItemResponse> result = queryService.findAllItems(
                    List.of(CodeGroupSource.DW), "DW_GROUP", null, "테스트");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).itemCode()).isEqualTo("CODE2");
        }
    }

    @Nested
    @DisplayName("Approval Group 소스 테스트")
    class ApprovalGroupSourceTests {

        @Test
        @DisplayName("Given: APPROVAL_GROUP 소스 / When: findAllItems 호출 / Then: 승인그룹 목록 반환")
        void returnsApprovalGroupItems() {
            // Given
            given(approvalGroupService.listGroupSummary(true))
                    .willReturn(List.of(
                            new ApprovalGroupSummaryResponse(UUID.randomUUID(), "AG001", "승인그룹1"),
                            new ApprovalGroupSummaryResponse(UUID.randomUUID(), "AG002", "승인그룹2")
                    ));

            // When
            List<CodeGroupItemResponse> result = queryService.findAllItems(
                    List.of(CodeGroupSource.APPROVAL_GROUP), null, null, null);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Given: 다른 그룹코드 필터 / When: findAllItems(APPROVAL_GROUP) 호출 / Then: 빈 결과")
        void skipsApprovalGroupWhenOtherGroupCode() {
            // When
            List<CodeGroupItemResponse> result = queryService.findAllItems(
                    List.of(CodeGroupSource.APPROVAL_GROUP), "OTHER_GROUP", null, null);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given: 비활성 필터 / When: findAllItems(APPROVAL_GROUP) 호출 / Then: 빈 결과")
        void skipsApprovalGroupWhenInactiveFilter() {
            // When
            List<CodeGroupItemResponse> result = queryService.findAllItems(
                    List.of(CodeGroupSource.APPROVAL_GROUP), null, false, null);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Locale 소스 테스트")
    class LocaleSourceTests {

        @Test
        @DisplayName("Given: LOCALE_COUNTRY 소스 / When: findAllItems 호출 / Then: 국가 목록 반환")
        void returnsLocaleCountryItems() {
            // Given
            given(localeCodeProvider.getCountries())
                    .willReturn(List.of(
                            new LocaleCountryEntry("KR", "대한민국", null, true, false),
                            new LocaleCountryEntry("US", "미국", null, true, false)
                    ));

            // When
            List<CodeGroupItemResponse> result = queryService.findAllItems(
                    List.of(CodeGroupSource.LOCALE_COUNTRY), null, null, null);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Given: LOCALE_LANGUAGE 소스 / When: findAllItems 호출 / Then: 언어 목록 반환")
        void returnsLocaleLanguageItems() {
            // Given
            given(localeCodeProvider.getLanguages())
                    .willReturn(List.of(
                            new LocaleLanguageEntry("ko", "한국어", null, true, false),
                            new LocaleLanguageEntry("en", "영어", null, true, false)
                    ));

            // When
            List<CodeGroupItemResponse> result = queryService.findAllItems(
                    List.of(CodeGroupSource.LOCALE_LANGUAGE), null, null, null);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Given: 검색어 / When: findAllItems(LOCALE_COUNTRY) 호출 / Then: 검색 API 사용")
        void searchesLocaleCountry() {
            // Given
            given(localeCodeProvider.searchCountries("한국"))
                    .willReturn(List.of(
                            new LocaleCountryEntry("KR", "대한민국", null, true, false)
                    ));

            // When
            List<CodeGroupItemResponse> result = queryService.findAllItems(
                    List.of(CodeGroupSource.LOCALE_COUNTRY), null, null, "한국");

            // Then
            assertThat(result).hasSize(1);
            verify(localeCodeProvider).searchCountries("한국");
        }

        @Test
        @DisplayName("Given: 검색어 / When: findAllItems(LOCALE_LANGUAGE) 호출 / Then: 검색 API 사용")
        void searchesLocaleLanguage() {
            // Given
            given(localeCodeProvider.searchLanguages("한국"))
                    .willReturn(List.of(
                            new LocaleLanguageEntry("ko", "한국어", null, true, false)
                    ));

            // When
            List<CodeGroupItemResponse> result = queryService.findAllItems(
                    List.of(CodeGroupSource.LOCALE_LANGUAGE), null, null, "한국");

            // Then
            assertThat(result).hasSize(1);
            verify(localeCodeProvider).searchLanguages("한국");
        }

        @Test
        @DisplayName("Given: 비활성 필터 / When: findAllItems(LOCALE) 호출 / Then: 빈 결과")
        void skipsLocaleWhenInactiveFilter() {
            // When
            List<CodeGroupItemResponse> countryResult = queryService.findAllItems(
                    List.of(CodeGroupSource.LOCALE_COUNTRY), null, false, null);
            List<CodeGroupItemResponse> languageResult = queryService.findAllItems(
                    List.of(CodeGroupSource.LOCALE_LANGUAGE), null, false, null);

            // Then
            assertThat(countryResult).isEmpty();
            assertThat(languageResult).isEmpty();
        }

        @Test
        @DisplayName("Given: 다른 그룹코드 필터 / When: findAllItems(LOCALE) 호출 / Then: 빈 결과")
        void skipsLocaleWhenOtherGroupCode() {
            // When
            List<CodeGroupItemResponse> countryResult = queryService.findAllItems(
                    List.of(CodeGroupSource.LOCALE_COUNTRY), "OTHER_GROUP", null, null);
            List<CodeGroupItemResponse> languageResult = queryService.findAllItems(
                    List.of(CodeGroupSource.LOCALE_LANGUAGE), "OTHER_GROUP", null, null);

            // Then
            assertThat(countryResult).isEmpty();
            assertThat(languageResult).isEmpty();
        }
    }

    @Nested
    @DisplayName("Dynamic DB 소스 테스트")
    class DynamicDbSourceTests {

        @Test
        @DisplayName("Given: 빈 Dynamic 그룹 / When: findAllItems 호출 / Then: placeholder row 생성")
        void createsPlaceholderForEmptyDynamicGroup() {
            // Given
            given(codeGroupRepository.findAllBySourceWithItems(CodeGroupSource.STATIC_ENUM))
                    .willReturn(Collections.emptyList());
            given(staticCodeRegistry.getRegisteredEnums()).willReturn(Collections.emptySet());
            given(codeGroupService.findAllItems(eq(CodeGroupSource.DYNAMIC_DB), anyString()))
                    .willReturn(Collections.emptyList());

            // When
            List<CodeGroupItemResponse> result = queryService.findAllItems(
                    List.of(CodeGroupSource.DYNAMIC_DB), null, null, null);

            // Then
            assertThat(result).isNotEmpty();
            // 빈 placeholder row는 itemCode가 null
            assertThat(result.stream().anyMatch(r -> r.itemCode() == null)).isTrue();
        }

        @Test
        @DisplayName("Given: 빈 Dynamic 그룹 + active=true / When: findAllItems 호출 / Then: placeholder 제외")
        void excludesPlaceholderWhenActiveTrue() {
            // Given
            given(codeGroupRepository.findAllBySourceWithItems(CodeGroupSource.STATIC_ENUM))
                    .willReturn(Collections.emptyList());
            given(staticCodeRegistry.getRegisteredEnums()).willReturn(Collections.emptySet());
            given(codeGroupService.findAllItems(eq(CodeGroupSource.DYNAMIC_DB), anyString()))
                    .willReturn(Collections.emptyList());

            // When
            List<CodeGroupItemResponse> result = queryService.findAllItems(
                    List.of(CodeGroupSource.DYNAMIC_DB), null, true, null);

            // Then - 빈 placeholder는 활성 필터에서 제외됨
            assertThat(result.stream().noneMatch(r -> r.itemCode() == null)).isTrue();
        }

        @Test
        @DisplayName("Given: 빈 Dynamic 그룹 + 검색어 / When: findAllItems 호출 / Then: placeholder 제외")
        void excludesPlaceholderWhenSearching() {
            // Given
            given(codeGroupRepository.findAllBySourceWithItems(CodeGroupSource.STATIC_ENUM))
                    .willReturn(Collections.emptyList());
            given(staticCodeRegistry.getRegisteredEnums()).willReturn(Collections.emptySet());
            given(codeGroupService.findAllItems(eq(CodeGroupSource.DYNAMIC_DB), anyString()))
                    .willReturn(Collections.emptyList());

            // When
            List<CodeGroupItemResponse> result = queryService.findAllItems(
                    List.of(CodeGroupSource.DYNAMIC_DB), null, null, "검색");

            // Then
            assertThat(result.stream().noneMatch(r -> r.itemCode() == null)).isTrue();
        }
    }

    @Nested
    @DisplayName("getMigrationStatus 테스트")
    class GetMigrationStatusTests {

        @Test
        @DisplayName("Given: Enum만 존재 / When: getMigrationStatus 호출 / Then: enumOnly에 포함")
        void categoriesEnumOnlyGroups() {
            // Given
            Set<Class<? extends Enum<?>>> enumSet = new HashSet<>();
            enumSet.add(TestStatus.class);
            given(staticCodeRegistry.getRegisteredEnums()).willReturn(enumSet);
            given(codeGroupRepository.findDistinctGroupCodes())
                    .willReturn(Collections.emptyList());
            given(staticCodeRegistry.findByCodeType("TEST_STATUS"))
                    .willReturn(Optional.of(TestStatus.class));
            given(staticCodeRegistry.getCodeGroupItems(TestStatus.class))
                    .willReturn(List.of(
                            new CodeGroupItem("TEST_STATUS", "ACTIVE", "활성", 0, true, CodeGroupSource.STATIC_ENUM, null, null)
                    ));

            // When
            MigrationStatusResponse result = queryService.getMigrationStatus();

            // Then
            assertThat(result.enumOnlyGroups()).hasSize(1);
            assertThat(result.enumOnlyGroups().get(0).groupCode()).isEqualTo("TEST_STATUS");
            assertThat(result.dbOnlyGroups()).isEmpty();
            assertThat(result.syncedGroups()).isEmpty();
        }

        @Test
        @DisplayName("Given: Enum과 DB 동기화됨 / When: getMigrationStatus 호출 / Then: synced에 포함")
        void categoriesSyncedGroups() {
            // Given
            Set<Class<? extends Enum<?>>> enumSet = new HashSet<>();
            enumSet.add(TestStatus.class);
            given(staticCodeRegistry.getRegisteredEnums()).willReturn(enumSet);
            given(codeGroupRepository.findDistinctGroupCodes())
                    .willReturn(List.of("TEST_STATUS"));
            given(staticCodeRegistry.findByCodeType("TEST_STATUS"))
                    .willReturn(Optional.of(TestStatus.class));
            given(staticCodeRegistry.getCodeGroupItems(TestStatus.class))
                    .willReturn(List.of(
                            new CodeGroupItem("TEST_STATUS", "ACTIVE", "활성", 0, true, CodeGroupSource.STATIC_ENUM, null, null)
                    ));
            given(codeItemRepository.countBySourceAndGroupCode(CodeGroupSource.STATIC_ENUM, "TEST_STATUS"))
                    .willReturn(1L);

            // When
            MigrationStatusResponse result = queryService.getMigrationStatus();

            // Then
            assertThat(result.syncedGroups()).hasSize(1);
            assertThat(result.syncedGroups().get(0).groupCode()).isEqualTo("TEST_STATUS");
        }

        @Test
        @DisplayName("Given: DB만 존재 (Enum 삭제됨) / When: getMigrationStatus 호출 / Then: dbOnly에 포함")
        void categoriesDbOnlyGroups() {
            // Given
            given(staticCodeRegistry.getRegisteredEnums()).willReturn(Collections.emptySet());
            given(codeGroupRepository.findDistinctGroupCodes())
                    .willReturn(List.of("DELETED_ENUM"));
            given(codeItemRepository.countBySourceAndGroupCode(CodeGroupSource.STATIC_ENUM, "DELETED_ENUM"))
                    .willReturn(5L);

            // When
            MigrationStatusResponse result = queryService.getMigrationStatus();

            // Then
            assertThat(result.dbOnlyGroups()).hasSize(1);
            assertThat(result.dbOnlyGroups().get(0).groupCode()).isEqualTo("DELETED_ENUM");
            assertThat(result.dbOnlyGroups().get(0).itemCount()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("normalize 테스트")
    class NormalizeTests {

        @Test
        @DisplayName("Given: null 그룹코드 / When: findByGroupCode 호출 / Then: 예외 발생")
        void throwsWhenGroupCodeIsNull() {
            // When / Then
            assertThatThrownBy(() -> queryService.findByGroupCode(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null");
        }
    }
}
