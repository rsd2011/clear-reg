package com.example.auth.jit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.admin.orggroup.service.OrgGroupPermissionResolver;
import com.example.auth.LoginType;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountRepository;
import com.example.dw.application.DwEmployeeDirectoryService;
import com.example.dw.application.DwEmployeeSnapshot;
import com.example.dw.domain.HrOrganizationEntity;
import com.example.dw.infrastructure.persistence.HrOrganizationRepository;
import com.example.auth.jit.dto.JitProvisioningResult;

@ExtendWith(MockitoExtension.class)
@DisplayName("JitProvisioningService 테스트")
class JitProvisioningServiceTest {

  @Mock private UserAccountRepository userAccountRepository;
  @Mock private DwEmployeeDirectoryService employeeDirectoryService;
  @Mock private HrOrganizationRepository organizationRepository;
  @Mock private OrgGroupPermissionResolver permissionResolver;
  @Mock private PasswordEncoder passwordEncoder;

  private JitProvisioningProperties properties;
  private JitProvisioningService service;

  @BeforeEach
  void setUp() {
    properties = new JitProvisioningProperties();
    properties.setEnabled(true);
    properties.setLinkExistingUsers(true);
    properties.setAllowWithoutDwRecord(false);
    properties.setFallbackOrganizationCode("ROOT");
    properties.setFallbackPermissionGroupCode("DEFAULT");
    properties.setDefaultRoles(Set.of("USER"));
    properties.setEnabledLoginTypes(Set.of("SSO", "AD"));

    service =
        new JitProvisioningService(
            properties,
            userAccountRepository,
            employeeDirectoryService,
            organizationRepository,
            permissionResolver,
            passwordEncoder);
  }

  @Nested
  @DisplayName("isEnabled / isEnabledFor 테스트")
  class EnabledTests {

    @Test
    @DisplayName("Given JIT 비활성화 When isEnabled 호출 Then false 반환")
    void givenDisabledWhenIsEnabledThenReturnFalse() {
      properties.setEnabled(false);

      assertThat(service.isEnabled()).isFalse();
      assertThat(service.isEnabledFor(LoginType.SSO)).isFalse();
    }

    @Test
    @DisplayName("Given JIT 활성화 When isEnabledFor(SSO) 호출 Then true 반환")
    void givenEnabledWhenIsEnabledForSsoThenReturnTrue() {
      assertThat(service.isEnabledFor(LoginType.SSO)).isTrue();
    }

    @Test
    @DisplayName("Given enabledLoginTypes가 AD만 포함 When isEnabledFor(SSO) 호출 Then false 반환")
    void givenAdOnlyWhenIsEnabledForSsoThenReturnFalse() {
      properties.setEnabledLoginTypes(Set.of("AD"));

      assertThat(service.isEnabledFor(LoginType.SSO)).isFalse();
      assertThat(service.isEnabledFor(LoginType.AD)).isTrue();
    }
  }

  @Nested
  @DisplayName("provisionForSso 테스트")
  class ProvisionForSsoTests {

    @Test
    @DisplayName("Given 기존 SSO ID 사용자 When provisionForSso 호출 Then existing 반환")
    void givenExistingSsoIdWhenProvisionThenReturnExisting() {
      UserAccount existing = createUserAccount("emp001");
      given(userAccountRepository.findBySsoId("sso-123")).willReturn(Optional.of(existing));

      JitProvisioningResult result = service.provisionForSso("sso-123", "emp001");

      assertThat(result.created()).isFalse();
      assertThat(result.linked()).isFalse();
      assertThat(result.account()).isEqualTo(existing);
      verify(userAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given 기존 username 사용자 When provisionForSso 호출 Then SSO ID 연결")
    void givenExistingUsernameWhenProvisionThenLinkSsoId() {
      UserAccount existing = createUserAccount("emp001");
      given(userAccountRepository.findBySsoId("sso-123")).willReturn(Optional.empty());
      given(userAccountRepository.findByUsername("emp001")).willReturn(Optional.of(existing));

      JitProvisioningResult result = service.provisionForSso("sso-123", "emp001");

      assertThat(result.created()).isFalse();
      assertThat(result.linked()).isTrue();
      verify(userAccountRepository).save(existing);
      assertThat(existing.getSsoId()).isEqualTo("sso-123");
    }

    @Test
    @DisplayName("Given 신규 사용자 + DW 직원 When provisionForSso 호출 Then 새 계정 생성")
    void givenNewUserWithDwWhenProvisionThenCreateAccount() {
      given(userAccountRepository.findBySsoId("sso-123")).willReturn(Optional.empty());
      given(userAccountRepository.findByUsername("emp001")).willReturn(Optional.empty());
      given(employeeDirectoryService.findActive("emp001"))
          .willReturn(Optional.of(createEmployeeSnapshot("emp001", "ORG001")));
      given(organizationRepository.findFirstByLeaderEmployeeIdAndEffectiveEndIsNullOrderByVersionDesc("emp001"))
          .willReturn(Optional.empty());
      given(organizationRepository.findFirstByManagerEmployeeIdAndEffectiveEndIsNullOrderByVersionDesc("emp001"))
          .willReturn(Optional.empty());
      given(permissionResolver.resolvePermGroups(List.of("ORG001"), false))
          .willReturn(Set.of("MEMBER_PG"));
      given(passwordEncoder.encode(any())).willReturn("encoded-password");

      JitProvisioningResult result = service.provisionForSso("sso-123", "emp001");

      assertThat(result.created()).isTrue();
      assertThat(result.role()).isEqualTo(EmployeeRole.MEMBER);

      ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
      verify(userAccountRepository).save(captor.capture());

      UserAccount saved = captor.getValue();
      assertThat(saved.getUsername()).isEqualTo("emp001");
      assertThat(saved.getOrganizationCode()).isEqualTo("ORG001");
      assertThat(saved.getPermissionGroupCode()).isEqualTo("MEMBER_PG");
      assertThat(saved.getSsoId()).isEqualTo("sso-123");
    }

    @Test
    @DisplayName("Given 신규 사용자 + DW 없음 + allowWithoutDwRecord=false When provisionForSso 호출 Then 예외")
    void givenNewUserNoDwNotAllowedWhenProvisionThenThrow() {
      given(userAccountRepository.findBySsoId("sso-123")).willReturn(Optional.empty());
      given(userAccountRepository.findByUsername("emp001")).willReturn(Optional.empty());
      given(employeeDirectoryService.findActive("emp001")).willReturn(Optional.empty());

      assertThatThrownBy(() -> service.provisionForSso("sso-123", "emp001"))
          .isInstanceOf(JitProvisioningException.class)
          .hasMessageContaining("DW에 등록되지 않은 사용자");
    }
  }

  @Nested
  @DisplayName("provisionForAd 테스트")
  class ProvisionForAdTests {

    @Test
    @DisplayName("Given 기존 사용자 + AD 도메인 없음 When provisionForAd 호출 Then AD 도메인 연결")
    void givenExistingUserNoAdDomainWhenProvisionThenLinkDomain() {
      UserAccount existing = createUserAccount("emp001");
      given(userAccountRepository.findByUsername("emp001")).willReturn(Optional.of(existing));

      JitProvisioningResult result = service.provisionForAd("emp001", "CORP.LOCAL");

      assertThat(result.linked()).isTrue();
      verify(userAccountRepository).save(existing);
      assertThat(existing.getActiveDirectoryDomain()).isEqualTo("CORP.LOCAL");
    }

    @Test
    @DisplayName("Given 신규 사용자 + DW 리더 When provisionForAd 호출 Then 리더 권한으로 생성")
    void givenNewLeaderWhenProvisionThenCreateWithLeaderPermGroup() {
      given(userAccountRepository.findByUsername("emp001")).willReturn(Optional.empty());
      given(employeeDirectoryService.findActive("emp001"))
          .willReturn(Optional.of(createEmployeeSnapshot("emp001", "ORG001")));
      given(organizationRepository.findFirstByLeaderEmployeeIdAndEffectiveEndIsNullOrderByVersionDesc("emp001"))
          .willReturn(Optional.of(createOrganizationEntity("ORG001", "emp001", null)));
      given(permissionResolver.resolvePermGroups(List.of("ORG001"), true))
          .willReturn(Set.of("LEADER_PG"));
      given(passwordEncoder.encode(any())).willReturn("encoded-password");

      JitProvisioningResult result = service.provisionForAd("emp001", "CORP.LOCAL");

      assertThat(result.created()).isTrue();
      assertThat(result.role()).isEqualTo(EmployeeRole.LEADER);

      ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
      verify(userAccountRepository).save(captor.capture());

      UserAccount saved = captor.getValue();
      assertThat(saved.getPermissionGroupCode()).isEqualTo("LEADER_PG");
      assertThat(saved.getActiveDirectoryDomain()).isEqualTo("CORP.LOCAL");
    }

    @Test
    @DisplayName("Given 신규 사용자 + DW 매니저 When provisionForAd 호출 Then 매니저 권한으로 생성")
    void givenNewManagerWhenProvisionThenCreateWithManagerPermGroup() {
      given(userAccountRepository.findByUsername("emp001")).willReturn(Optional.empty());
      given(employeeDirectoryService.findActive("emp001"))
          .willReturn(Optional.of(createEmployeeSnapshot("emp001", "ORG001")));
      given(organizationRepository.findFirstByLeaderEmployeeIdAndEffectiveEndIsNullOrderByVersionDesc("emp001"))
          .willReturn(Optional.empty());
      given(organizationRepository.findFirstByManagerEmployeeIdAndEffectiveEndIsNullOrderByVersionDesc("emp001"))
          .willReturn(Optional.of(createOrganizationEntity("ORG001", null, "emp001")));
      given(permissionResolver.resolveManagerPermGroups(List.of("ORG001")))
          .willReturn(Set.of("MANAGER_PG"));
      given(passwordEncoder.encode(any())).willReturn("encoded-password");

      JitProvisioningResult result = service.provisionForAd("emp001", "CORP.LOCAL");

      assertThat(result.created()).isTrue();
      assertThat(result.role()).isEqualTo(EmployeeRole.MANAGER);

      ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
      verify(userAccountRepository).save(captor.capture());

      UserAccount saved = captor.getValue();
      assertThat(saved.getPermissionGroupCode()).isEqualTo("MANAGER_PG");
    }
  }

  @Nested
  @DisplayName("DW 없이 생성 허용 테스트")
  class AllowWithoutDwTests {

    @Test
    @DisplayName("Given allowWithoutDwRecord=true When DW 없는 사용자 생성 Then 기본값으로 생성")
    void givenAllowWithoutDwWhenNoDwThenCreateWithDefaults() {
      properties.setAllowWithoutDwRecord(true);

      given(userAccountRepository.findBySsoId("sso-123")).willReturn(Optional.empty());
      given(userAccountRepository.findByUsername("emp001")).willReturn(Optional.empty());
      given(employeeDirectoryService.findActive("emp001")).willReturn(Optional.empty());
      given(passwordEncoder.encode(any())).willReturn("encoded-password");

      JitProvisioningResult result = service.provisionForSso("sso-123", "emp001");

      assertThat(result.created()).isTrue();
      assertThat(result.role()).isEqualTo(EmployeeRole.MEMBER);

      ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
      verify(userAccountRepository).save(captor.capture());

      UserAccount saved = captor.getValue();
      assertThat(saved.getOrganizationCode()).isEqualTo("ROOT");
      assertThat(saved.getPermissionGroupCode()).isEqualTo("DEFAULT");
    }

    @Test
    @DisplayName("Given allowWithoutDwRecord=true + AD When DW 없는 사용자 생성 Then 기본값으로 생성")
    void givenAllowWithoutDwForAdWhenNoDwThenCreateWithDefaults() {
      properties.setAllowWithoutDwRecord(true);

      given(userAccountRepository.findByUsername("emp001")).willReturn(Optional.empty());
      given(employeeDirectoryService.findActive("emp001")).willReturn(Optional.empty());
      given(passwordEncoder.encode(any())).willReturn("encoded-password");

      JitProvisioningResult result = service.provisionForAd("emp001", "CORP.LOCAL");

      assertThat(result.created()).isTrue();
      assertThat(result.role()).isEqualTo(EmployeeRole.MEMBER);

      ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
      verify(userAccountRepository).save(captor.capture());

      UserAccount saved = captor.getValue();
      assertThat(saved.getOrganizationCode()).isEqualTo("ROOT");
      assertThat(saved.getPermissionGroupCode()).isEqualTo("DEFAULT");
      assertThat(saved.getActiveDirectoryDomain()).isEqualTo("CORP.LOCAL");
    }
  }

  @Nested
  @DisplayName("JitProvisioningException 테스트")
  class ExceptionTests {

    @Test
    @DisplayName("JitProvisioningException 메시지 생성자 테스트")
    void exceptionWithMessage() {
      JitProvisioningException ex = new JitProvisioningException("Test error");
      assertThat(ex.getMessage()).isEqualTo("Test error");
      assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("JitProvisioningException 메시지+원인 생성자 테스트")
    void exceptionWithMessageAndCause() {
      RuntimeException cause = new RuntimeException("Original error");
      JitProvisioningException ex = new JitProvisioningException("Test error", cause);
      assertThat(ex.getMessage()).isEqualTo("Test error");
      assertThat(ex.getCause()).isEqualTo(cause);
    }
  }

  @Nested
  @DisplayName("기존 사용자 링크 비활성화 테스트")
  class LinkExistingUsersDisabledTests {

    @Test
    @DisplayName("Given linkExistingUsers=false When 기존 username 사용자 SSO 로그인 Then SSO 연결 없이 반환")
    void givenLinkDisabledWhenExistingUserThenReturnWithoutLink() {
      properties.setLinkExistingUsers(false);

      UserAccount existing = createUserAccount("emp001");
      given(userAccountRepository.findBySsoId("sso-123")).willReturn(Optional.empty());
      given(userAccountRepository.findByUsername("emp001")).willReturn(Optional.of(existing));

      JitProvisioningResult result = service.provisionForSso("sso-123", "emp001");

      assertThat(result.created()).isFalse();
      assertThat(result.linked()).isFalse();
      assertThat(result.account()).isEqualTo(existing);
      verify(userAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given 기존 AD 도메인 있음 When AD 로그인 Then AD 연결 없이 반환")
    void givenExistingAdDomainWhenAdLoginThenReturnWithoutLink() {
      UserAccount existing = createUserAccount("emp001");
      existing.assignActiveDirectoryDomain("OLD.LOCAL");
      given(userAccountRepository.findByUsername("emp001")).willReturn(Optional.of(existing));

      JitProvisioningResult result = service.provisionForAd("emp001", "CORP.LOCAL");

      assertThat(result.created()).isFalse();
      assertThat(result.linked()).isFalse();
      assertThat(result.account()).isEqualTo(existing);
      assertThat(existing.getActiveDirectoryDomain()).isEqualTo("OLD.LOCAL"); // 변경 안됨
      verify(userAccountRepository, never()).save(any());
    }
  }

  private UserAccount createUserAccount(String username) {
    return UserAccount.builder()
        .username(username)
        .password("password")
        .email(username + "@example.com")
        .organizationCode("ORG001")
        .permissionGroupCode("DEFAULT")
        .build();
  }

  private DwEmployeeSnapshot createEmployeeSnapshot(String employeeId, String orgCode) {
    return new DwEmployeeSnapshot(
        employeeId,
        1,
        "Test User",
        employeeId + "@example.com",
        orgCode,
        "FULL_TIME",
        "ACTIVE",
        LocalDate.now().minusYears(1),
        null,
        OffsetDateTime.now());
  }

  private HrOrganizationEntity createOrganizationEntity(
      String orgCode, String leaderId, String managerId) {
    return HrOrganizationEntity.snapshot(
        orgCode,
        1,
        "Test Organization",
        null,
        "ACTIVE",
        leaderId,
        managerId,
        LocalDate.now().minusYears(1),
        null,
        UUID.randomUUID(),
        OffsetDateTime.now());
  }
}
