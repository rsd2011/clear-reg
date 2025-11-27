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
import com.example.common.policy.DataPolicyContextHolder;
import com.example.common.policy.DataPolicyMatch;
import com.example.common.policy.DataPolicyQuery;
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
        DataPolicyMatch match = DataPolicyMatch.builder()
                .maskRule("FULL")
                .rowScope("OWN")
                .priority(1)
                .build();
        DataPolicyContextHolder.set(match);
        assertThat(DataPolicyContextHolder.get().getRowScope()).isEqualTo("OWN");
        DataPolicyContextHolder.clear();

        DataPolicyQuery query = new DataPolicyQuery("F", "A", "P", 1L, List.of("G1"), "BT", null, null);
        assertThat(query.nowOrDefault()).isNotNull();

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
        DataPolicyMatch match = DataPolicyMatch.builder().rowScope("ORG").build();

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
                .dataKind("RRN")
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
        String masked = OutputMaskingAdapter.mask("other", "123456", target, "PARTIAL", null);
        assertThat(masked).startsWith("12").endsWith("56");

        String tokenized = MaskingFunctions.masker(DataPolicyMatch.builder().maskRule("TOKENIZE").build()).apply("abc");
        assertThat(tokenized).hasSizeGreaterThan(5);

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
}
