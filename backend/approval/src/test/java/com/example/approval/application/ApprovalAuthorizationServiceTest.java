package com.example.approval.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.approval.api.ApprovalAction;
import com.example.admin.approval.exception.ApprovalAccessDeniedException;
import com.example.approval.domain.ApprovalRequest;
import com.example.approval.domain.ApprovalStep;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountRepository;
import com.example.admin.permission.domain.PermissionGroup;
import com.example.admin.permission.repository.PermissionGroupRepository;

@ExtendWith(MockitoExtension.class)
class ApprovalAuthorizationServiceTest {

    @Mock
    PermissionGroupRepository permissionGroupRepository;
    @Mock
    UserAccountRepository userAccountRepository;

    ApprovalAuthorizationService service;

    @BeforeEach
    void setUp() {
        service = new ApprovalAuthorizationService(permissionGroupRepository, userAccountRepository);
    }

    @Test
    @DisplayName("Given 활성 단계의 결재자 When 승인 요청하면 Then 정상 처리된다")
    void authorizeActiveStep() {
        ApprovalRequest request = sampleRequest();
        PermissionGroup permGroup = createPermissionGroup("PERM_GRP", "GRP1");
        UserAccount user = createUserAccount("actor", "ORG", "PERM_GRP");

        given(permissionGroupRepository.findByApprovalGroupCode("GRP1")).willReturn(List.of(permGroup));
        given(userAccountRepository.findByPermissionGroupCodeIn(List.of("PERM_GRP"))).willReturn(List.of(user));

        service.ensureAuthorized(request, ApprovalAction.APPROVE, "actor", "ORG");
    }

    @Test
    @DisplayName("Given 보류된 단계의 결재자 When 후결 승인 요청하면 Then 정상 처리된다")
    void authorizeDeferredStep() {
        ApprovalRequest request = sampleRequest();
        request.defer("actor", OffsetDateTime.now());

        PermissionGroup permGroup = createPermissionGroup("PERM_GRP", "GRP1");
        UserAccount user = createUserAccount("actor", "ORG", "PERM_GRP");

        given(permissionGroupRepository.findByApprovalGroupCode("GRP1")).willReturn(List.of(permGroup));
        given(userAccountRepository.findByPermissionGroupCodeIn(List.of("PERM_GRP"))).willReturn(List.of(user));

        service.ensureAuthorized(request, ApprovalAction.DEFER_APPROVE, "actor", "ORG");
    }

    @Test
    @DisplayName("Given 활성 단계의 결재자 When 위임 요청하면 Then 정상 처리된다")
    void authorizeDelegate() {
        ApprovalRequest request = sampleRequest();
        PermissionGroup permGroup = createPermissionGroup("PERM_GRP", "GRP1");
        UserAccount user = createUserAccount("actor", "ORG", "PERM_GRP");

        given(permissionGroupRepository.findByApprovalGroupCode("GRP1")).willReturn(List.of(permGroup));
        given(userAccountRepository.findByPermissionGroupCodeIn(List.of("PERM_GRP"))).willReturn(List.of(user));

        service.ensureAuthorized(request, ApprovalAction.DELEGATE, "actor", "ORG");
    }

    @Test
    @DisplayName("Given 매핑된 권한 그룹 없음 When 승인 요청하면 Then 예외 발생")
    void denyWhenPermissionGroupMissing() {
        ApprovalRequest request = sampleRequest();
        given(permissionGroupRepository.findByApprovalGroupCode("GRP1")).willReturn(List.of());

        assertThatThrownBy(() -> service.ensureAuthorized(request, ApprovalAction.APPROVE, "actor", "ORG"))
                .isInstanceOf(ApprovalAccessDeniedException.class);
    }

    @Test
    @DisplayName("Given 조직 코드 불일치 When 승인 요청하면 Then 예외 발생")
    void denyWhenOrgMismatch() {
        ApprovalRequest request = sampleRequest();
        PermissionGroup permGroup = createPermissionGroup("PERM_GRP", "GRP1");
        UserAccount user = createUserAccount("actor", "OTHER_ORG", "PERM_GRP");

        given(permissionGroupRepository.findByApprovalGroupCode("GRP1")).willReturn(List.of(permGroup));
        given(userAccountRepository.findByPermissionGroupCodeIn(List.of("PERM_GRP"))).willReturn(List.of(user));

        assertThatThrownBy(() -> service.ensureAuthorized(request, ApprovalAction.APPROVE, "actor", "ORG"))
                .isInstanceOf(ApprovalAccessDeniedException.class);
    }

    @Test
    @DisplayName("Given 권한 그룹에 멤버 없음 When 승인 요청하면 Then 예외 발생")
    void denyWhenNotMember() {
        ApprovalRequest request = sampleRequest();
        PermissionGroup permGroup = createPermissionGroup("PERM_GRP", "GRP1");

        given(permissionGroupRepository.findByApprovalGroupCode("GRP1")).willReturn(List.of(permGroup));
        given(userAccountRepository.findByPermissionGroupCodeIn(List.of("PERM_GRP"))).willReturn(List.of());

        assertThatThrownBy(() -> service.ensureAuthorized(request, ApprovalAction.APPROVE, "actor", "ORG"))
                .isInstanceOf(ApprovalAccessDeniedException.class);
    }

    @Test
    @DisplayName("Given 임의의 사용자 When 철회 요청하면 Then 권한 검증 없이 통과")
    void allowWithdrawWithoutCheck() {
        ApprovalRequest request = sampleRequest();
        service.ensureAuthorized(request, ApprovalAction.WITHDRAW, "actor", "ORG");
    }

    @Test
    @DisplayName("Given 활성 단계 없음 When 승인 요청하면 Then 예외 발생")
    void denyWhenNoActiveStep() {
        ApprovalRequest request = sampleRequest();
        // make all steps approved to remove active step
        request.approve("actor", OffsetDateTime.now());

        assertThatThrownBy(() -> service.ensureAuthorized(request, ApprovalAction.APPROVE, "actor", "ORG"))
                .isInstanceOf(ApprovalAccessDeniedException.class);
    }

    private ApprovalRequest sampleRequest() {
        List<ApprovalStep> steps = List.of(new ApprovalStep(1, "GRP1"));
        return ApprovalRequest.create(java.util.UUID.randomUUID(), "TPL", "ORG", "req", "summary", steps, OffsetDateTime.now());
    }

    private PermissionGroup createPermissionGroup(String code, String approvalGroupCode) {
        PermissionGroup group = new PermissionGroup(code, "Test Group");
        group.setApprovalGroupCode(approvalGroupCode);
        return group;
    }

    private UserAccount createUserAccount(String username, String orgCode, String permGroupCode) {
        return UserAccount.builder()
                .username(username)
                .password("password")
                .organizationCode(orgCode)
                .permissionGroupCode(permGroupCode)
                .build();
    }
}
