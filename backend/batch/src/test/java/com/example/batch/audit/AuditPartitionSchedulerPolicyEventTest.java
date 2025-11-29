package com.example.batch.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.ZoneOffset;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.common.policy.PolicyChangedEvent;
import com.example.common.policy.PolicySettingsProvider;
import com.example.admin.policy.repository.PolicyDocumentRepository;
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
import com.example.admin.datapolicy.service.DataPolicyProviderAdapter;
import com.example.admin.datapolicy.service.DataPolicyService;
import com.example.admin.orggroup.service.OrgGroupPermissionResolver;
import com.example.admin.datapolicy.repository.DataPolicyRepository;
import com.example.admin.orggroup.repository.OrgGroupRepository;
import com.example.auth.domain.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.auth.domain.PasswordHistoryRepository;
import com.example.auth.config.AuthPolicyProperties;
import com.example.auth.AuthService;
import com.example.auth.security.JwtTokenProvider;
import com.example.auth.security.JwtProperties;
import com.example.auth.domain.RefreshTokenRepository;
import com.example.auth.config.SessionPolicyProperties;
import com.example.admin.permission.repository.PermissionGroupRepository;
import com.example.audit.infra.persistence.AuditLogRepository;
import com.example.audit.infra.AuditRecordService;
import com.example.audit.infra.persistence.AuditMonthlySummaryRepository;
import com.example.audit.infra.masking.UnmaskAuditRepository;
import com.example.file.StoredFileVersionRepository;
import com.example.file.audit.FileAuditOutboxRelay;
import com.example.admin.menu.service.MenuService;
import com.example.admin.menu.repository.MenuRepository;
import com.example.admin.menu.repository.MenuViewConfigRepository;
import com.example.admin.approval.repository.ApprovalGroupRepository;
import com.example.admin.approval.repository.ApprovalLineTemplateRepository;
import com.example.admin.approval.repository.ApprovalLineTemplateVersionRepository;
import com.example.admin.codegroup.locale.LocaleCodeProvider;
import com.example.admin.codegroup.service.CodeGroupService;
import com.example.admin.codegroup.service.CodeGroupQueryService;
import com.example.admin.codegroup.registry.StaticCodeRegistry;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Import(TestObjectMapperConfig.class)
class AuditPartitionSchedulerPolicyEventTest {

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

    @MockBean
    MenuService menuService;

    @MockBean
    MenuRepository menuRepository;

    @MockBean
    MenuViewConfigRepository menuViewConfigRepository;

    @MockBean
    ApprovalGroupRepository approvalGroupRepository;

    @MockBean
    ApprovalLineTemplateRepository approvalLineTemplateRepository;

    @MockBean
    ApprovalLineTemplateVersionRepository approvalLineTemplateHistoryRepository;

    @MockBean
    CodeGroupService codeGroupService;

    @MockBean
    LocaleCodeProvider localeCodeProvider;

    @MockBean
    CodeGroupQueryService codeGroupQueryService;

    @MockBean
    StaticCodeRegistry staticCodeRegistry;

    @org.junit.jupiter.api.BeforeEach
    void setUpPolicyRepository() {
        given(policyDocumentRepository.findByCode(anyString())).willReturn(java.util.Optional.empty());
        var database = new DwIngestionProperties.DatabaseProperties();
        database.setEnabled(false);
        given(dwIngestionProperties.getDatabase()).willReturn(database);
        given(dwIngestionProperties.isEnabled()).willReturn(false);
        given(hrExternalFeedRepository.findFirstByStatusOrderByCreatedAtAsc(any())).willReturn(java.util.Optional.empty());
    }

    @Test
    @DisplayName("PolicyChangedEvent 수신 시 파티션 생성이 즉시 트리거된다")
    void refreshOnPolicyChangeCreatesPartition() throws Exception {
        // given: drop any existing partition for deterministic assertion
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS audit_log (event_time date) PARTITION BY RANGE (event_time)");
            st.execute("DROP TABLE IF EXISTS audit_log_2099_01");
        }

        // when: fire policy changed event
        publisher.publishEvent(new PolicyChangedEvent("security.policy", "yaml"));

        // then: partition for next month (relative to Clock now) exists
        String suffix = Clock.systemUTC().instant().atZone(ZoneOffset.UTC).plusMonths(1)
                .withDayOfMonth(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyy_MM"));
        String partitionName = "audit_log_" + suffix;
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, null, partitionName, null)) {
            assertThat(rs.next()).as("partition table should exist after policy change event").isTrue();
        }
    }
}
