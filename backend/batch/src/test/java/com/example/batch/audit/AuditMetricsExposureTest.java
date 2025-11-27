package com.example.batch.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import io.micrometer.core.instrument.MeterRegistry;

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
import com.example.admin.organization.OrganizationPolicyRepository;
import com.example.admin.permission.PermissionGroupRepository;
import com.example.audit.infra.persistence.AuditLogRepository;
import com.example.audit.infra.AuditRecordService;
import com.example.audit.infra.persistence.AuditMonthlySummaryRepository;
import com.example.audit.infra.masking.UnmaskAuditRepository;
import com.example.file.StoredFileVersionRepository;
import com.example.file.audit.FileAuditOutboxRelay;
import com.example.admin.menu.MenuService;
import com.example.admin.menu.MenuRepository;
import com.example.admin.menu.MenuViewConfigRepository;
import com.example.admin.approval.ApprovalGroupRepository;
import com.example.admin.approval.ApprovalLineTemplateRepository;
import com.example.admin.approval.history.ApprovalLineTemplateHistoryRepository;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "management.endpoints.web.exposure.include=prometheus",
        "management.endpoint.prometheus.enabled=true",
        "audit.archive.enabled=false" // 실행은 막지만 빈 초기화로 메트릭은 등록됨
})
@Import(TestObjectMapperConfig.class)
@ActiveProfiles("test")
class AuditMetricsExposureTest {

    @Autowired
    MeterRegistry meterRegistry;

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
    ApprovalLineTemplateHistoryRepository approvalLineTemplateHistoryRepository;

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
    @DisplayName("Prometheus 엔드포인트에 audit 전용 메트릭이 노출된다")
    void prometheusContainsAuditMetrics() {
        assertThat(meterRegistry.find("audit_archive_success_total").counter()).isNotNull();
        assertThat(meterRegistry.find("audit_archive_failure_total").counter()).isNotNull();
        assertThat(meterRegistry.find("audit_archive_elapsed_ms").timer()).isNotNull();
    }
}
