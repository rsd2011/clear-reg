package com.example.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.common.policy.PolicyToggleSettings;
import com.example.policy.dto.PolicyUpdateRequest;
import com.example.policy.dto.PolicyView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@DisplayName("PolicyAdminService")
class PolicyAdminServiceTest {

    @Mock
    private PolicyDocumentRepository repository;

    private PolicyAdminService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        PolicyToggleSettings defaults = new PolicyToggleSettings(true, true, true, List.of("PASSWORD"),
                10_485_760L, List.of("pdf"), true, 365);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        given(repository.findByCode("security.policy")).willReturn(Optional.empty());
        service = new PolicyAdminService(repository, mapper, defaults, null);
    }

    @Test
    @DisplayName("Given update request When update Then persist merged state")
    void givenRequestWhenUpdateThenPersist() {
        PolicyUpdateRequest request = new PolicyUpdateRequest(
                false,
                null,
                true,
                List.of("SSO"),
                5_000_000L,
                List.of("pdf", "png"),
                true,
                90,
                null, null, null, null, null, null, null, null,
                null, // auditUnmaskRoles
                null, // auditPartitionEnabled
                null, // auditPartitionCron
                null, // auditPartitionPreloadMonths
                null, // auditPartitionTablespaceHot
                null, // auditPartitionTablespaceCold
                null, // auditPartitionHotMonths
                null, // auditPartitionColdMonths
                null, // auditMonthlyReportEnabled
                null, // auditMonthlyReportCron
                null, // auditLogRetentionEnabled
                null, // auditLogRetentionCron
                null, // auditColdArchiveEnabled
                null, // auditColdArchiveCron
                null, // auditRetentionCleanupEnabled
                null  // auditRetentionCleanupCron
        );

        PolicyAdminService.PolicySnapshot snapshot = service.update(request);

        assertThat(snapshot.state().passwordPolicyEnabled()).isFalse();
        assertThat(snapshot.state().enabledLoginTypes()).containsExactly("SSO");
        ArgumentCaptor<PolicyDocument> captor = ArgumentCaptor.forClass(PolicyDocument.class);
        then(repository).should().save(captor.capture());
        assertThat(captor.getValue().getYaml()).contains("enabledLoginTypes");
    }

    @Test
    @DisplayName("Given YAML When applyYaml Then refresh cache")
    void givenYamlWhenApplyThenRefresh() {
        String yaml = "passwordPolicyEnabled: false\naccountLockEnabled: false\npasswordHistoryEnabled: false\nenabledLoginTypes:\n  - SSO\nmaxFileSizeBytes: 1048576\nallowedFileExtensions:\n  - pdf\nstrictMimeValidation: true\n";

        PolicyAdminService.PolicySnapshot snapshot = service.applyYaml(yaml);

        assertThat(snapshot.state().enabledLoginTypes()).containsExactly("SSO");
        then(repository).should().save(any());
    }

    @Test
    @DisplayName("신규 감사 스케줄 필드를 업데이트하면 상태에 반영된다")
    void updatesNewAuditScheduleFields() {
        PolicyUpdateRequest request = new PolicyUpdateRequest(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                true, "0 0 1 * * *",
                true, "0 15 2 2 * *",
                false, "0 0 5 * * *");

        PolicyAdminService.PolicySnapshot snapshot = service.update(request);

        assertThat(snapshot.state().auditLogRetentionEnabled()).isTrue();
        assertThat(snapshot.state().auditLogRetentionCron()).isEqualTo("0 0 1 * * *");
        assertThat(snapshot.state().auditColdArchiveEnabled()).isTrue();
        assertThat(snapshot.state().auditColdArchiveCron()).isEqualTo("0 15 2 2 * *");
        assertThat(snapshot.state().auditRetentionCleanupEnabled()).isFalse();
        assertThat(snapshot.state().auditRetentionCleanupCron()).isEqualTo("0 0 5 * * *");
    }

    @Test
    @DisplayName("Given invalid YAML When applyYaml Then throw")
    void givenInvalidYamlWhenApplyThenThrow() {
        assertThatThrownBy(() -> service.applyYaml("invalid: ["))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Given defaults When currentView Then expose cached state")
    void givenDefaultsWhenCurrentViewThenExpose() {
        PolicyView view = service.currentView();
        assertThat(view.passwordPolicyEnabled()).isTrue();
        assertThat(view.enabledLoginTypes()).contains("PASSWORD");
        assertThat(view.allowedFileExtensions()).contains("pdf");
        assertThat(view.auditMaskingEnabled()).isTrue();
    }

    @Test
    @DisplayName("Given update request When updateView Then include rendered yaml")
    void givenUpdateRequestWhenUpdateViewThenIncludeYaml() {
        PolicyUpdateRequest request = new PolicyUpdateRequest(
                true,
                true,
                false,
                List.of("AD"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        PolicyView view = service.updateView(request);

        assertThat(view.yaml()).contains("AD");
        assertThat(view.accountLockEnabled()).isFalse();
    }

    @Test
    @DisplayName("auditPartition 섹션을 YAML로 적용하면 tablespace/hotMonths가 반영된다")
    void applyYamlWithAuditPartitionSection() {
        String yaml = """
                passwordPolicyEnabled: true
                passwordHistoryEnabled: true
                accountLockEnabled: true
                enabledLoginTypes: [PASSWORD]
                auditPartitionEnabled: true
                auditPartitionCron: "0 30 2 1 * *"
                auditPartitionPreloadMonths: 2
                auditPartition:
                  enabled: true
                  cron: "0 30 2 1 * *"
                  preloadMonths: 2
                  tablespaceHot: ts_hot
                  tablespaceCold: ts_cold
                  hotMonths: 12
                  coldMonths: 48
                """;

        PolicyView view = service.applyYamlView(yaml);

        assertThat(view.auditPartitionTablespaceHot()).isEqualTo("ts_hot");
        assertThat(view.auditPartitionTablespaceCold()).isEqualTo("ts_cold");
        assertThat(view.auditPartitionHotMonths()).isEqualTo(12);
        assertThat(view.auditPartitionColdMonths()).isEqualTo(48);
        assertThat(view.yaml()).contains("tablespaceHot: ts_hot");
    }

    @Test
    @DisplayName("auditPartition 상위 필드만 있어도 tablespace/hotMonths를 읽어들인다")
    void applyYamlWithTopLevelAuditPartitionFields() {
        String yaml = """
                passwordPolicyEnabled: true
                passwordHistoryEnabled: true
                accountLockEnabled: true
                enabledLoginTypes: [PASSWORD]
                auditPartitionEnabled: true
                auditPartitionCron: "0 15 1 1 * *"
                auditPartitionPreloadMonths: 3
                auditPartitionTablespaceHot: ts_hot2
                auditPartitionTablespaceCold: ts_cold2
                auditPartitionHotMonths: 9
                auditPartitionColdMonths: 36
                """;

        PolicyView view = service.applyYamlView(yaml);

        assertThat(view.auditPartitionTablespaceHot()).isEqualTo("ts_hot2");
        assertThat(view.auditPartitionTablespaceCold()).isEqualTo("ts_cold2");
        assertThat(view.auditPartitionHotMonths()).isEqualTo(9);
        assertThat(view.auditPartitionColdMonths()).isEqualTo(36);
    }

    @Test
    @DisplayName("snapshot 내보낼 때 auditPartition 섹션이 포함된다")
    void snapshotExportsAuditPartitionSection() {
        String yaml = """
                passwordPolicyEnabled: true
                passwordHistoryEnabled: true
                accountLockEnabled: true
                enabledLoginTypes: [PASSWORD]
                auditPartitionEnabled: true
                auditPartitionCron: "0 0 1 1 * *"
                auditPartitionPreloadMonths: 2
                auditPartition:
                  enabled: true
                  cron: "0 0 1 1 * *"
                  preloadMonths: 2
                  tablespaceHot: ts_hot3
                  tablespaceCold: ts_cold3
                  hotMonths: 24
                  coldMonths: 72
                """;

        service.applyYaml(yaml);
        var snapshot = service.snapshot();

        assertThat(snapshot.yaml()).contains("auditPartition:");
        assertThat(snapshot.yaml()).contains("tablespaceCold: \"ts_cold3\"");
    }

    @Test
    @DisplayName("서비스 초기화 시 기존 YAML 문서를 읽어 캐시한다")
    void loadsExistingDocumentOnStartup() {
        String yaml = """
                passwordPolicyEnabled: false
                accountLockEnabled: false
                auditPartitionEnabled: true
                auditPartitionCron: "0 0 1 1 * *"
                auditPartitionPreloadMonths: 2
                auditPartition:
                  enabled: true
                  cron: "0 0 1 1 * *"
                  preloadMonths: 2
                  tablespaceHot: preload_ts_hot
                  tablespaceCold: preload_ts_cold
                  hotMonths: 18
                  coldMonths: 90
                """;

        var repo = mock(PolicyDocumentRepository.class);
        given(repo.findByCode("security.policy")).willReturn(java.util.Optional.of(new PolicyDocument("security.policy", yaml)));
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        PolicyToggleSettings defaults = new PolicyToggleSettings(true, true, true, List.of("PASSWORD"), 10_485_760L, List.of("pdf"), true, 365);

        PolicyAdminService svc = new PolicyAdminService(repo, mapper, defaults, null);

        var state = svc.currentState();
        assertThat(state.auditPartitionEnabled()).isTrue();
        assertThat(state.auditPartitionCron()).isEqualTo("0 0 1 1 * *");
        assertThat(state.auditPartitionTablespaceHot()).isEqualTo("preload_ts_hot");
        assertThat(state.auditPartitionColdMonths()).isEqualTo(90);
    }

    @Test
    @DisplayName("auditPartition 섹션이 없으면 기본 partition 설정으로 유지된다")
    void applyYamlWithoutPartitionKeepsDefaults() {
        String yaml = """
                passwordPolicyEnabled: true
                accountLockEnabled: true
                enabledLoginTypes: [PASSWORD]
                """;

        service.applyYaml(yaml);

        var state = service.currentState();
        assertThat(state.auditPartitionEnabled()).isFalse();
        assertThat(state.auditPartitionTablespaceHot()).isEmpty();
    }

    @Test
    @DisplayName("hotMonths/coldMonths가 0 이하이면 기본값으로 보정된다")
    void partitionMonthsClampToDefaults() {
        String yaml = """
                passwordPolicyEnabled: true
                accountLockEnabled: true
                auditPartitionEnabled: true
                auditPartitionCron: "0 0 1 1 * *"
                auditPartitionPreloadMonths: 1
                auditPartition:
                  enabled: true
                  cron: "0 0 1 1 * *"
                  preloadMonths: 1
                  tablespaceHot: ts_hot4
                  tablespaceCold: ts_cold4
                  hotMonths: 0
                  coldMonths: 0
                """;

        service.applyYaml(yaml);

        var state = service.currentState();
        assertThat(state.auditPartitionHotMonths()).isEqualTo(6);
        assertThat(state.auditPartitionColdMonths()).isEqualTo(60);
    }

    @Test
    @DisplayName("Given YAML When applyYamlView Then return PolicyView")
    void givenYamlWhenApplyYamlViewThenReturnView() {
        String yaml = "passwordPolicyEnabled: true\npasswordHistoryEnabled: true\naccountLockEnabled: true\nenabledLoginTypes:\n  - PASSWORD\n";
        PolicyView view = service.applyYamlView(yaml);
        assertThat(view.passwordHistoryEnabled()).isTrue();
    }
}
