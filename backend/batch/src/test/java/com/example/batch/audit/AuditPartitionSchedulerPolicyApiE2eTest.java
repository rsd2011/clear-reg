package com.example.batch.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.common.policy.PolicyChangedEvent;
import com.example.common.policy.PolicySettingsProvider;
import com.example.admin.policy.PolicyDocumentRepository;
import com.example.dw.infrastructure.persistence.HrExternalFeedRepository;
import com.example.dw.config.DwIngestionProperties;
import com.example.dw.infrastructure.persistence.DwCommonCodeRepository;
import com.example.dw.infrastructure.persistence.DwHolidayRepository;
import com.example.dw.infrastructure.persistence.HrEmployeeRepository;
import com.example.dw.infrastructure.persistence.HrEmployeeStagingRepository;
import com.example.dw.infrastructure.persistence.HrImportErrorRepository;
import com.example.dw.infrastructure.persistence.HrOrganizationRepository;
import com.example.dw.infrastructure.persistence.HrOrganizationStagingRepository;
import com.example.dw.domain.repository.HrBatchRepository;
import com.example.batch.ingestion.DwIngestionService;
import com.example.dw.application.job.DwIngestionOutboxService;
import com.example.batch.quartz.DwQuartzScheduleManager;
import com.example.dw.application.DwEmployeeDirectoryService;
import com.example.batch.ingestion.HrEmployeeSynchronizationService;
import com.example.dwgateway.dw.DwBatchPortAdapter;
import com.example.dw.application.DwBatchQueryService;
import com.example.dwgateway.dw.DwIngestionPolicyPortAdapter;
import com.example.dw.application.policy.DwIngestionPolicyService;
import com.example.file.FileService;
import com.example.file.StoredFileRepository;
import com.example.dwgateway.web.FileController;
import com.example.dw.application.DwOrganizationQueryService;
import com.example.dw.application.export.ExportAuditService;
import com.example.dw.application.export.ExportExecutionHelper;
import com.example.admin.masking.DataPolicyProviderAdapter;
import com.example.admin.masking.DataPolicyService;
import com.example.admin.masking.OrgGroupPermissionResolver;
import com.example.admin.masking.DataPolicyRepository;
import com.example.admin.masking.OrgGroupRepository;
import com.example.auth.domain.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.auth.domain.PasswordHistoryRepository;
import com.example.auth.config.AuthPolicyProperties;
import com.example.auth.AuthService;
import com.example.auth.security.JwtTokenProvider;
import com.example.auth.security.JwtProperties;
import com.example.auth.domain.RefreshTokenRepository;
import com.example.auth.config.SessionPolicyProperties;
import com.example.auth.organization.OrganizationPolicyRepository;
import com.example.admin.permission.PermissionGroupRepository;
import com.example.audit.infra.persistence.AuditLogRepository;
import com.example.audit.infra.AuditRecordService;
import com.example.audit.infra.persistence.AuditMonthlySummaryRepository;
import com.example.audit.infra.masking.UnmaskAuditRepository;
import com.example.file.StoredFileVersionRepository;
import com.example.file.audit.FileAuditOutboxRelay;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(TestObjectMapperConfig.class)
class AuditPartitionSchedulerPolicyApiE2eTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("audit")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("security.policy.audit-partition-enabled", () -> "true");
        registry.add("security.policy.audit-partition-preload-months", () -> "0");
    }

    @Autowired
    TestRestTemplate rest;

    @Autowired
    DataSource dataSource;

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    AuditPartitionScheduler scheduler;

    @Autowired
    PolicySettingsProvider policySettingsProvider;

    @MockBean
    PolicyDocumentRepository policyDocumentRepository;

    @MockBean
    HrExternalFeedRepository hrExternalFeedRepository;

    @MockBean
    DwIngestionProperties dwIngestionProperties;

    @MockBean
    HrBatchRepository hrBatchRepository;

    @MockBean
    HrEmployeeRepository hrEmployeeRepository;

    @MockBean
    HrEmployeeStagingRepository hrEmployeeStagingRepository;

    @MockBean
    HrOrganizationRepository hrOrganizationRepository;

    @MockBean
    HrOrganizationStagingRepository hrOrganizationStagingRepository;

    @MockBean
    HrImportErrorRepository hrImportErrorRepository;

    @MockBean
    DwCommonCodeRepository dwCommonCodeRepository;

    @MockBean
    DwHolidayRepository dwHolidayRepository;

    @MockBean
    DwIngestionService dwIngestionService;

    @MockBean
    DwIngestionOutboxService dwIngestionOutboxService;

    @MockBean
    DwQuartzScheduleManager dwQuartzScheduleManager;

    @MockBean
    DwEmployeeDirectoryService dwEmployeeDirectoryService;

    @MockBean
    HrEmployeeSynchronizationService hrEmployeeSynchronizationService;

    @MockBean
    DwBatchPortAdapter dwBatchPortAdapter;

    @MockBean
    DwBatchQueryService dwBatchQueryService;

    @MockBean
    DwIngestionPolicyPortAdapter dwIngestionPolicyPortAdapter;

    @MockBean
    DwIngestionPolicyService dwIngestionPolicyService;

    @MockBean
    FileService fileService;

    @MockBean
    StoredFileRepository storedFileRepository;

    @MockBean
    FileController fileController;

    @MockBean
    DwOrganizationQueryService dwOrganizationQueryService;

    @MockBean
    ExportAuditService exportAuditService;

    @MockBean
    ExportExecutionHelper exportExecutionHelper;

    @MockBean
    DataPolicyProviderAdapter dataPolicyProviderAdapter;

    @MockBean
    DataPolicyService dataPolicyService;

    @MockBean
    OrgGroupPermissionResolver orgGroupPermissionResolver;

    @MockBean
    DataPolicyRepository dataPolicyRepository;

    @MockBean
    OrgGroupRepository orgGroupRepository;

    @MockBean
    UserAccountRepository userAccountRepository;

    @MockBean
    PasswordEncoder passwordEncoder;

    @MockBean
    PasswordHistoryRepository passwordHistoryRepository;

    @MockBean
    AuthPolicyProperties authPolicyProperties;

    @MockBean
    AuthService authService;

    @MockBean
    JwtTokenProvider jwtTokenProvider;

    @MockBean
    JwtProperties jwtProperties;

    @MockBean
    RefreshTokenRepository refreshTokenRepository;

    @MockBean
    SessionPolicyProperties sessionPolicyProperties;

    @MockBean
    OrganizationPolicyRepository organizationPolicyRepository;

    @MockBean
    PermissionGroupRepository permissionGroupRepository;

    @MockBean
    AuditLogRepository auditLogRepository;

    @MockBean
    AuditRecordService auditRecordService;

    @MockBean
    AuditMonthlySummaryRepository auditMonthlySummaryRepository;

    @MockBean
    UnmaskAuditRepository unmaskAuditRepository;

    @MockBean
    StoredFileVersionRepository storedFileVersionRepository;

    @MockBean
    FileAuditOutboxRelay fileAuditOutboxRelay;

    @org.junit.jupiter.api.BeforeEach
    void setUpPolicyRepository() {
        given(policyDocumentRepository.findByCode(anyString())).willReturn(java.util.Optional.empty());
        var database = new DwIngestionProperties.DatabaseProperties();
        database.setEnabled(false);
        given(dwIngestionProperties.getDatabase()).willReturn(database);
        given(dwIngestionProperties.isEnabled()).willReturn(false);
        given(hrExternalFeedRepository.findFirstByStatusOrderByCreatedAtAsc(any())).willReturn(java.util.Optional.empty());
    }

    @AfterEach
    void cleanUp() throws Exception {
        try (var conn = dataSource.getConnection(); var st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS audit_log CASCADE");
            st.execute("CREATE TABLE audit_log (event_time date) PARTITION BY RANGE (event_time)");
        }
    }

    @Test
    @DisplayName("Policy API 호출로 Cron/enable 변경 → PolicyChangedEvent → 파티션 생성이 즉시 반영된다")
    @Transactional
    void policyApiTriggersSchedulerRefresh() throws Exception {
        // given: base audit_log table
        try (var conn = dataSource.getConnection(); var st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS audit_log (event_time date) PARTITION BY RANGE (event_time)");
        }

        // when: simulate policy API update (publish event)
        publisher.publishEvent(new PolicyChangedEvent("security.policy", "yaml"));

        // then: next-month partition created
        String suffix = Clock.systemUTC().instant().atZone(ZoneOffset.UTC).plusMonths(1)
                .withDayOfMonth(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyy_MM"));
        String partitionName = "audit_log_" + suffix;
        try (var conn = dataSource.getConnection();
             var rs = conn.getMetaData().getTables(null, null, partitionName, null)) {
            assertThat(rs.next()).isTrue();
        }
    }
}
