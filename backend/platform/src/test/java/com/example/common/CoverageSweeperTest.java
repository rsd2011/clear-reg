package com.example.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import com.example.common.cache.CacheInvalidationEvent;
import com.example.common.cache.CacheInvalidationType;
import com.example.common.cache.CacheKeyUtils;
import com.example.common.file.FileDownload;
import com.example.common.file.dto.FileMetadataDto;
import com.example.common.file.FileStatus;
import com.example.common.identifier.*;
import com.example.common.masking.*;
import com.example.common.masking.DataKind;
import com.example.common.policy.MaskingMatch;
import com.example.common.policy.RowAccessContextHolder;
import com.example.common.policy.RowAccessMatch;
import com.example.common.policy.PolicyToggleSettings;
import com.example.common.security.RowScope;
import com.example.common.security.RowScopeContext;
import com.example.common.security.RowScopeContextHolder;
import com.example.common.security.RowScopeEvaluator;
import com.example.common.security.RowScopeFilter;
import com.example.common.security.RowScopeSpecifications;
import com.example.common.value.*;

class CoverageSweeperTest {

    @Test
    @DisplayName("정책/스레드홀더/토글 DTO 기본 동작을 커버한다")
    void policyCoverage() {
        RowAccessMatch match = RowAccessMatch.builder()
                .rowScope(RowScope.OWN)
                .priority(1)
                .build();
        RowAccessContextHolder.set(match);
        assertThat(RowAccessContextHolder.get().getRowScope()).isEqualTo(RowScope.OWN);
        RowAccessContextHolder.clear();

        PolicyToggleSettings settings = new PolicyToggleSettings(
                true, true, true, List.of("PWD"), 1024, List.of("PDF", "XLSX"),
                true, 10, true, true, true, 365, true, "high",
                true, List.of("/sensitive"), List.of("AUDITOR"),
                true, "0 0 2 1 * *", 2,
                true, "0 0 4 1 * *",
                true, "0 0 3 * * *",
                false, "0 30 2 2 * *",
                true, "0 30 3 * * *",
                com.example.common.schedule.BatchJobDefaults.defaults());
        assertThat(settings.auditMaskingEnabled()).isTrue();
        assertThat(settings.auditUnmaskRoles()).contains("AUDITOR");
    }

    @Test
    @DisplayName("파일/캐시/인사여러 값 객체와 식별자들이 직렬화/마스킹 규칙을 충족한다")
    void valueAndIdentifierCoverage() {
        FileMetadataDto meta = new FileMetadataDto(UUID.randomUUID(), "a.txt", "text/plain", 10L,
                "chk", "user", FileStatus.ACTIVE, OffsetDateTime.now(), OffsetDateTime.now(), OffsetDateTime.now());
        FileDownload download = new FileDownload(meta, null);
        assertThat(download.metadata().originalName()).isEqualTo("a.txt");

        String cacheKey = CacheKeyUtils.organizationScopeKey("ORG1", PageRequest.of(1, 20));
        assertThat(cacheKey).contains("ORG1").contains("1").contains("20");
        CacheInvalidationEvent event = new CacheInvalidationEvent(CacheInvalidationType.MASKING, "tenant", "scope", 1L, Instant.now());
        assertThat(event.type()).isEqualTo(CacheInvalidationType.MASKING);

        // 식별자/값 객체 happy-path
        assertThat(CustomerId.of("CUST1234").masked()).endsWith("1234");
        assertThat(EmployeeId.of("EMP0001").jsonValue()).contains("EMP");
        assertThat(OrganizationId.of("ORG0001").toString()).contains("ORG");
        assertThat(AccountId.of("1234567890").raw()).isEqualTo("1234567890");
        assertThat(PassportId.of("P12345").masked()).contains("*");
        assertThat(SecuritiesId.of("SEC12345").masked()).contains("45");
        assertThat(EmailAddress.of("user1234@example.com").masked()).contains("@example.com");
        assertThat(OrganizationName.of("ORGNAME").jsonValue()).contains("ORG");
        assertThat(DriverLicenseId.of("1234567890").masked()).contains("7890");
        assertThat(BusinessRegistrationId.of("1234567890").masked()).endsWith("7890");
        assertThat(PersonName.of("AliceSmith").masked()).contains("*");
        assertThat(ResidentRegistrationId.of("900101-1234567").masked()).contains("*");
        assertThat(PhoneNumber.of("01012345678").masked()).contains("5678");
        assertThat(CardId.of("4111111111111").raw()).isEqualTo("4111111111111");
        assertThat(CorporateRegistrationId.of("1234567890123").masked()).contains("*");
        assertThat(Address.of("KR", "Seoul", "Gangnam", "Teheran-ro 1", "Line2", "06134").countryCode()).isEqualTo("KR");
        assertThrows(IllegalArgumentException.class, () -> CustomerId.of("  "));

        SessionId sessionId = SessionId.of("SESS1234");
        AuthToken authToken = AuthToken.of("0123456789ABCDEF");
        GenderCode gender = GenderCode.from("male");
        PaymentReference pay = PaymentReference.of("PAYREF1234");
        MoneyAmount amount = MoneyAmount.of(new BigDecimal("12.345"), "USD");
        PermissionGroupCode perm = PermissionGroupCode.of("PERM1234");
        BatchJobId job = BatchJobId.of("JOB1234");
        FileToken token = FileToken.of("FILETOKEN1234");
        NationalityCode nation = NationalityCode.of("KR");
        BirthDate birth = BirthDate.of(java.time.LocalDate.of(1990, 1, 1));

        assertThat(List.of(sessionId, authToken, gender, pay, amount, perm, job, token, nation, birth))
                .allSatisfy(v -> assertThat(v.toString()).isNotEmpty());
        assertThat(amount.toString()).contains("USD");
    }

    @Test
    @DisplayName("RowScope/Specification/Filter 동작을 커버한다")
    void rowScopeCoverage() {
        RowScopeContext ctx = new RowScopeContext("ORG1", List.of("ORG1", "ORG1-CHILD"));
        RowScopeContextHolder.set(ctx);
        RowAccessMatch match = RowAccessMatch.builder().rowScope(RowScope.ORG).build();

        var spec = RowScopeEvaluator.toSpecification(match, null, (root, query, cb) -> cb.conjunction());
        assertThat(spec).isNotNull();

        RowScopeFilter filter = RowScopeFilter.from(match, ctx, (root, q, cb) -> cb.disjunction());
        assertThat(filter.rowScope().includesHierarchy()).isTrue();
        RowScopeContextHolder.clear();

        // RowScopeSpecifications validation branches
        RowScopeSpecifications.organizationScoped("org", RowScope.ALL, null, null);
        assertThrows(IllegalArgumentException.class,
                () -> RowScopeSpecifications.organizationScoped("org", RowScope.CUSTOM, null, null, null));
    }

    @Test
    @DisplayName("마스킹 전략/어댑터/서비스 분기 커버")
    void maskingCoverage() {
        PolicyToggleSettings settings = new PolicyToggleSettings(
                true, true, true, List.of(), 1024, List.of(), true, 1,
                true, true, true, 10, true, "medium", true, List.of(), List.of(),
                true, "0 0 2 1 * *", 1, true, "0 0 4 1 * *",
                true, "0 0 3 * * *",
                false, "0 30 2 2 * *",
                true, "0 30 3 * * *",
                com.example.common.schedule.BatchJobDefaults.defaults());

        MaskingTarget target = MaskingTarget.builder()
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .dataKind(DataKind.SSN)
                .defaultMask(true)
                .forceUnmaskFields(Set.of("name"))
                .requesterRoles(Set.of("AUDITOR"))
                .rowId("row-1")
                .build();

        MaskingStrategy strategy = new PolicyMaskingStrategy(settings, java.util.Map.of(
                SubjectType.CUSTOMER_INDIVIDUAL, true,
                SubjectType.EMPLOYEE, false
        ), Set.of("AUDITOR"));

        MaskingService service = new MaskingService(strategy);
        Maskable maskable = new Maskable() {
            @Override public String raw() { return "raw-value"; }
            @Override public String masked() { return "****alue"; }
        };

        assertThat(service.render(maskable, target, "name")).isEqualTo("raw-value"); // force unmask field

        // DataKind 기반 마스킹 테스트
        MaskingTarget accountTarget = MaskingTarget.builder().dataKind(DataKind.ACCOUNT_NO).build();
        String masked = OutputMaskingAdapter.mask("other", "123456", accountTarget, true);
        assertThat(masked).startsWith("12").endsWith("56");

        // maskingEnabled=true + TOKENIZE DataKind는 없으므로 DEFAULT(FULL) 적용
        // 화이트리스트(maskingEnabled=false)면 원본 반환
        String raw = OutputMaskingAdapter.mask("field", "abc", MaskingTarget.builder().build(), false);
        assertThat(raw).isEqualTo("abc");

        // schedule/policy artifacts coverage
        com.example.common.schedule.TriggerDescriptor cron = new com.example.common.schedule.TriggerDescriptor(true, com.example.common.schedule.TriggerType.CRON, "0 0 * * * *", 0, 0, null);
        assertThat(cron.enabled()).isTrue();
        com.example.common.schedule.BatchJobSchedule schedule = new com.example.common.schedule.BatchJobSchedule(true, com.example.common.schedule.TriggerType.FIXED_DELAY, null, 1000L, 0L, "UTC");
        assertThat(schedule.toTriggerDescriptor().expression()).isNull();
        com.example.common.policy.AuditPartitionSettings auditPartitionSettings = new com.example.common.policy.AuditPartitionSettings(true, "0 0 2 1 * *", 1, "hot", "cold", 6, 60);
        assertThat(auditPartitionSettings.enabled()).isTrue();
        com.example.common.policy.PolicyChangedEvent evt = new com.example.common.policy.PolicyChangedEvent("security.policy", "yaml-body");
        assertThat(evt.code()).isEqualTo("security.policy");
        com.example.common.policy.PolicySettingsProvider provider = new com.example.common.policy.PolicySettingsProvider() {
            @Override
            public com.example.common.policy.PolicyToggleSettings currentSettings() { return settings; }

            @Override
            public com.example.common.schedule.BatchJobSchedule batchJobSchedule(com.example.common.schedule.BatchJobCode code) {
                return schedule;
            }
        };
        assertThat(provider.batchJobSchedule(com.example.common.schedule.BatchJobCode.AUDIT_COLD_MAINTENANCE)).isEqualTo(schedule);
        assertThat(provider.partitionSettings()).isNull();

        var defaultsFromNulls = new com.example.common.policy.PolicyToggleSettings(true, true, true, null, -1, null, true, -1,
                true, true, true, -1, true, null, true, null, null,
                true, "", -1, true, "", true, "", false, "", true, "",
                com.example.common.schedule.BatchJobDefaults.defaults());
        assertThat(defaultsFromNulls.allowedFileExtensions()).isEmpty();
        assertThat(defaultsFromNulls.batchJobs()).isNotEmpty();

        var audit2 = new com.example.common.policy.AuditPartitionSettings(true, "0 0 2 1 * *", 1, "hot", "cold", 6, 60);
        assertThat(audit2).isEqualTo(auditPartitionSettings);
    }

    @Test
    @DisplayName("인사/권한 공통 서비스도 간단히 호출한다")
    void greetingCoverage() {
        GreetingService svc = new GreetingService();
        assertThat(svc.greet(null)).isEqualTo("Hello, World!");
        assertThat(svc.greet("Alice")).contains("Alice");
    }

    @Test
    @DisplayName("MaskingMatch/RowAccessMatch의 equals/hashCode/toString 브랜치를 커버한다")
    void matchEqualsHashCodeCoverage() {
        UUID policyId = UUID.randomUUID();

        // MaskingMatch - 새로운 API로 모든 필드 설정
        MaskingMatch mask1 = MaskingMatch.builder()
                .policyId(policyId)
                .dataKinds(java.util.Set.of(com.example.common.masking.DataKind.SSN))
                .maskingEnabled(true)
                .auditEnabled(true)
                .priority(1)
                .build();

        MaskingMatch mask2 = MaskingMatch.builder()
                .policyId(policyId)
                .dataKinds(java.util.Set.of(com.example.common.masking.DataKind.SSN))
                .maskingEnabled(true)
                .auditEnabled(true)
                .priority(1)
                .build();

        MaskingMatch mask3 = MaskingMatch.builder()
                .policyId(UUID.randomUUID())
                .dataKinds(java.util.Set.of(com.example.common.masking.DataKind.PHONE))
                .maskingEnabled(false)
                .auditEnabled(false)
                .priority(2)
                .build();

        // equals 브랜치 커버
        assertThat(mask1).isEqualTo(mask2);
        assertThat(mask1).isNotEqualTo(mask3);
        assertThat(mask1).isNotEqualTo(null);
        assertThat(mask1).isNotEqualTo("not a match");
        assertThat(mask1).isEqualTo(mask1); // self comparison

        // hashCode
        assertThat(mask1.hashCode()).isEqualTo(mask2.hashCode());
        assertThat(mask1.hashCode()).isNotEqualTo(mask3.hashCode());

        // toString
        assertThat(mask1.toString()).contains("MaskingMatch");
        assertThat(mask1.toString()).contains("SSN");

        // getter
        assertThat(mask1.getPolicyId()).isEqualTo(policyId);
        assertThat(mask1.getDataKinds()).containsExactly(com.example.common.masking.DataKind.SSN);
        assertThat(mask1.isMaskingEnabled()).isTrue();
        assertThat(mask1.isAuditEnabled()).isTrue();
        assertThat(mask1.getPriority()).isEqualTo(1);

        // RowAccessMatch - 모든 필드 설정
        RowAccessMatch row1 = RowAccessMatch.builder()
                .policyId(policyId)
                .rowScope(RowScope.ORG)
                .priority(1)
                .build();

        RowAccessMatch row2 = RowAccessMatch.builder()
                .policyId(policyId)
                .rowScope(RowScope.ORG)
                .priority(1)
                .build();

        RowAccessMatch row3 = RowAccessMatch.builder()
                .policyId(UUID.randomUUID())
                .rowScope(RowScope.OWN)
                .priority(2)
                .build();

        // equals 브랜치 커버
        assertThat(row1).isEqualTo(row2);
        assertThat(row1).isNotEqualTo(row3);
        assertThat(row1).isNotEqualTo(null);
        assertThat(row1).isNotEqualTo("not a match");
        assertThat(row1).isEqualTo(row1); // self comparison

        // hashCode
        assertThat(row1.hashCode()).isEqualTo(row2.hashCode());
        assertThat(row1.hashCode()).isNotEqualTo(row3.hashCode());

        // toString
        assertThat(row1.toString()).contains("RowAccessMatch");
        assertThat(row1.toString()).contains("ORG");

        // getter
        assertThat(row1.getPolicyId()).isEqualTo(policyId);
        assertThat(row1.getRowScope()).isEqualTo(RowScope.ORG);
        assertThat(row1.getPriority()).isEqualTo(1);

        // null 필드 케이스
        MaskingMatch maskNull = MaskingMatch.builder().build();
        RowAccessMatch rowNull = RowAccessMatch.builder().build();

        assertThat(maskNull.getPolicyId()).isNull();
        assertThat(rowNull.getPolicyId()).isNull();
        assertThat(maskNull).isNotEqualTo(mask1);
        assertThat(rowNull).isNotEqualTo(row1);
        assertThat(maskNull.hashCode()).isNotEqualTo(mask1.hashCode());
        assertThat(rowNull.hashCode()).isNotEqualTo(row1.hashCode());

    }

    @Test
    @DisplayName("MaskRule enum 변환 메서드를 커버한다")
    void maskRuleCoverage() {
        // fromString 메서드
        assertThat(com.example.common.masking.MaskRule.fromString(null)).isEqualTo(com.example.common.masking.MaskRule.FULL);
        assertThat(com.example.common.masking.MaskRule.fromString("")).isEqualTo(com.example.common.masking.MaskRule.FULL);
        assertThat(com.example.common.masking.MaskRule.fromString("   ")).isEqualTo(com.example.common.masking.MaskRule.FULL);
        assertThat(com.example.common.masking.MaskRule.fromString("PARTIAL")).isEqualTo(com.example.common.masking.MaskRule.PARTIAL);
        assertThat(com.example.common.masking.MaskRule.fromString("partial")).isEqualTo(com.example.common.masking.MaskRule.PARTIAL);
        assertThat(com.example.common.masking.MaskRule.fromString("NONE")).isEqualTo(com.example.common.masking.MaskRule.NONE);
        assertThat(com.example.common.masking.MaskRule.fromString("HASH")).isEqualTo(com.example.common.masking.MaskRule.HASH);
        assertThat(com.example.common.masking.MaskRule.fromString("TOKENIZE")).isEqualTo(com.example.common.masking.MaskRule.TOKENIZE);
        assertThat(com.example.common.masking.MaskRule.fromString("INVALID")).isEqualTo(com.example.common.masking.MaskRule.FULL);

        // of 메서드
        assertThat(com.example.common.masking.MaskRule.of(null)).isEqualTo(com.example.common.masking.MaskRule.NONE);
        assertThat(com.example.common.masking.MaskRule.of("")).isEqualTo(com.example.common.masking.MaskRule.NONE);
        assertThat(com.example.common.masking.MaskRule.of("PARTIAL")).isEqualTo(com.example.common.masking.MaskRule.PARTIAL);
        assertThat(com.example.common.masking.MaskRule.of("hash")).isEqualTo(com.example.common.masking.MaskRule.HASH);
        assertThat(com.example.common.masking.MaskRule.of("UNKNOWN")).isEqualTo(com.example.common.masking.MaskRule.NONE);
    }

    @Test
    @DisplayName("MaskingFunctions masker 메서드를 커버한다")
    void maskingFunctionsCoverage() {
        // null match인 경우 - 마스킹 없이 원본 반환
        var maskerNull = com.example.common.masking.MaskingFunctions.masker(null, DataKind.DEFAULT);
        assertThat(maskerNull.apply("test")).isEqualTo("test");

        var maskerNullBoth = com.example.common.masking.MaskingFunctions.masker(null);
        assertThat(maskerNullBoth.apply("test")).isEqualTo("test");

        // maskingEnabled=false인 경우 (화이트리스트) - 원본 반환
        MaskingMatch whitelist = MaskingMatch.builder()
                .maskingEnabled(false)
                .build();
        var maskerWhitelist = com.example.common.masking.MaskingFunctions.masker(whitelist, DataKind.DEFAULT);
        assertThat(maskerWhitelist.apply("original")).isEqualTo("original");

        // maskingEnabled=true인 경우 (블랙리스트) - 마스킹 적용
        MaskingMatch blacklist = MaskingMatch.builder()
                .maskingEnabled(true)
                .dataKind(DataKind.PHONE)
                .build();
        var maskerBlacklist = com.example.common.masking.MaskingFunctions.masker(blacklist, DataKind.PHONE);
        assertThat(maskerBlacklist.apply("01012345678")).isNotEqualTo("01012345678");

        // dataKind null인 경우에도 match가 null이면 원본 반환
        var maskerNullKind = com.example.common.masking.MaskingFunctions.masker(null, null);
        assertThat(maskerNullKind.apply("test")).isEqualTo("test");

        // MaskingMatch에서 dataKind 사용
        MaskingMatch withDataKind = MaskingMatch.builder()
                .maskingEnabled(true)
                .dataKind(DataKind.SSN)
                .build();
        var maskerWithDataKind = com.example.common.masking.MaskingFunctions.masker(withDataKind);
        assertThat(maskerWithDataKind.apply("900101-1234567")).isNotNull();

        // applyMaskRule의 다양한 MaskRule 경우 커버
        // NONE 규칙 테스트 - 원본 반환 (maskingEnabled=false)
        MaskingMatch noneMatch = MaskingMatch.builder()
                .maskingEnabled(false)
                .build();
        var noneRuleMasker = com.example.common.masking.MaskingFunctions.masker(noneMatch, DataKind.DEFAULT);
        assertThat(noneRuleMasker.apply("original")).isEqualTo("original");

        // PARTIAL 규칙 테스트 (PHONE은 PARTIAL) - 블랙리스트 정책 필요
        MaskingMatch partialMatch = MaskingMatch.builder()
                .maskingEnabled(true)
                .dataKind(DataKind.PHONE)
                .build();
        var partialMasker = com.example.common.masking.MaskingFunctions.masker(partialMatch, DataKind.PHONE);
        assertThat(partialMasker.apply("01012345678")).contains("*");

        // FULL 규칙 테스트 (SSN은 FULL) - 블랙리스트 정책 필요
        MaskingMatch fullMatch = MaskingMatch.builder()
                .maskingEnabled(true)
                .dataKind(DataKind.SSN)
                .build();
        var fullMasker = com.example.common.masking.MaskingFunctions.masker(fullMatch, DataKind.SSN);
        String fullMasked = fullMasker.apply("900101-1234567");
        // FULL 마스킹은 [MASKED] 또는 * 포함 가능
        assertThat(fullMasked).satisfiesAnyOf(
                m -> assertThat(m).contains("*"),
                m -> assertThat(m).contains("[MASKED]")
        );

        // null value 테스트
        var nullValueMasker = com.example.common.masking.MaskingFunctions.masker(null, DataKind.DEFAULT);
        assertThat(nullValueMasker.apply(null)).isNull();
    }

    @Test
    @DisplayName("Maskable 인터페이스 기본 메서드를 커버한다")
    void maskableCoverage() {
        // Maskable 인터페이스의 default dataKind() 메서드 커버
        Maskable<String> simpleMaskable = new Maskable<>() {
            @Override public String raw() { return "raw"; }
            @Override public String masked() { return "***"; }
        };

        assertThat(simpleMaskable.dataKind()).isEqualTo(DataKind.DEFAULT);
        assertThat(simpleMaskable.raw()).isEqualTo("raw");
        assertThat(simpleMaskable.masked()).isEqualTo("***");

        // 기존 값 객체들의 Maskable 구현 확인
        assertThat(CustomerId.of("CUST1234").dataKind()).isEqualTo(DataKind.CUSTOMER_ID);
        assertThat(ResidentRegistrationId.of("900101-1234567").dataKind()).isEqualTo(DataKind.SSN);
        assertThat(PhoneNumber.of("01012345678").dataKind()).isEqualTo(DataKind.PHONE);
        assertThat(EmailAddress.of("test@example.com").dataKind()).isEqualTo(DataKind.EMAIL);
        assertThat(CardId.of("4111111111111111").dataKind()).isEqualTo(DataKind.CARD_NO);
        assertThat(AccountId.of("1234567890").dataKind()).isEqualTo(DataKind.ACCOUNT_NO);
    }
}
