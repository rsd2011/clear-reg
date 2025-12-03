package com.example.admin.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.admin.orggroup.service.OrgGroupPermissionResolver;
import com.example.admin.user.config.JitProvisioningProperties;
import com.example.admin.user.domain.EmployeeRole;
import com.example.admin.user.domain.UserAccount;
import com.example.admin.user.dto.JitProvisioningResult;
import com.example.admin.user.exception.JitProvisioningException;
import com.example.admin.user.repository.UserAccountRepository;
import com.example.dw.application.DwEmployeeDirectoryService;
import com.example.dw.application.DwEmployeeSnapshot;
import com.example.dw.domain.HrOrganizationEntity;
import com.example.dw.infrastructure.persistence.HrOrganizationRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("JitProvisioningService 테스트")
@ExtendWith(MockitoExtension.class)
class JitProvisioningServiceTest {

  @Mock private JitProvisioningProperties properties;
  @Mock private UserAccountRepository userAccountRepository;
  @Mock private DwEmployeeDirectoryService employeeDirectoryService;
  @Mock private HrOrganizationRepository organizationRepository;
  @Mock private OrgGroupPermissionResolver permissionResolver;
  @Mock private PasswordEncoder passwordEncoder;

  private JitProvisioningService service;

  @BeforeEach
  void setUp() {
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
  @DisplayName("isEnabled 메서드")
  class IsEnabledTests {

    @Test
    @DisplayName("Given JIT 활성화 When isEnabled Then true")
    void givenJitEnabled_whenCheck_thenTrue() {
      // Given
      when(properties.isEnabled()).thenReturn(true);

      // When
      boolean result = service.isEnabled();

      // Then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Given JIT 비활성화 When isEnabled Then false")
    void givenJitDisabled_whenCheck_thenFalse() {
      // Given
      when(properties.isEnabled()).thenReturn(false);

      // When
      boolean result = service.isEnabled();

      // Then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("isEnabledFor 메서드")
  class IsEnabledForTests {

    @Test
    @DisplayName("Given JIT 비활성화 When isEnabledFor Then false")
    void givenJitDisabled_whenCheckFor_thenFalse() {
      // Given
      when(properties.isEnabled()).thenReturn(false);

      // When
      boolean result = service.isEnabledFor("SSO");

      // Then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Given 빈 로그인 타입 목록 When isEnabledFor Then true (모든 타입 허용)")
    void givenEmptyLoginTypes_whenCheckFor_thenTrue() {
      // Given
      when(properties.isEnabled()).thenReturn(true);
      when(properties.getEnabledLoginTypes()).thenReturn(Set.of());

      // When
      boolean result = service.isEnabledFor("SSO");

      // Then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Given 특정 타입 허용 When isEnabledFor 해당 타입 Then true")
    void givenSpecificTypes_whenCheckForAllowedType_thenTrue() {
      // Given
      when(properties.isEnabled()).thenReturn(true);
      when(properties.getEnabledLoginTypes()).thenReturn(Set.of("SSO", "AD"));

      // When
      boolean result = service.isEnabledFor("SSO");

      // Then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Given 특정 타입 허용 When isEnabledFor 미허용 타입 Then false")
    void givenSpecificTypes_whenCheckForDisallowedType_thenFalse() {
      // Given
      when(properties.isEnabled()).thenReturn(true);
      when(properties.getEnabledLoginTypes()).thenReturn(Set.of("SSO"));

      // When
      boolean result = service.isEnabledFor("AD");

      // Then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("provisionForSso 메서드")
  class ProvisionForSsoTests {

    @Test
    @DisplayName("Given SSO ID로 기존 사용자 존재 When provisionForSso Then existing 결과 반환")
    void givenExistingUserBySsoId_whenProvision_thenReturnExisting() {
      // Given
      UserAccount existingAccount = createUserAccount("testuser");
      when(userAccountRepository.findBySsoId("sso-id-123")).thenReturn(Optional.of(existingAccount));

      // When
      JitProvisioningResult result = service.provisionForSso("sso-id-123", "testuser");

      // Then
      assertThat(result.account()).isEqualTo(existingAccount);
      assertThat(result.created()).isFalse();
      assertThat(result.linked()).isFalse();
      verify(userAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given username으로 기존 사용자 존재 및 연결 활성화 When provisionForSso Then SSO ID 연결")
    void givenExistingUserByUsername_whenProvision_thenLinkSsoId() {
      // Given
      UserAccount existingAccount = createUserAccount("testuser");
      when(userAccountRepository.findBySsoId("sso-id-123")).thenReturn(Optional.empty());
      when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(existingAccount));
      when(properties.isLinkExistingUsers()).thenReturn(true);

      // When
      JitProvisioningResult result = service.provisionForSso("sso-id-123", "testuser");

      // Then
      assertThat(existingAccount.getSsoId()).isEqualTo("sso-id-123");
      verify(userAccountRepository).save(existingAccount);
    }

    @Test
    @DisplayName("Given 신규 사용자, DW 정보 없음, DW 필수 When provisionForSso Then JitProvisioningException 발생")
    void givenNoUserNoDwAndDwRequired_whenProvision_thenThrowException() {
      // Given
      when(userAccountRepository.findBySsoId("sso-id-123")).thenReturn(Optional.empty());
      when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.empty());
      when(employeeDirectoryService.findActive("testuser")).thenReturn(Optional.empty());
      when(properties.isAllowWithoutDwRecord()).thenReturn(false);

      // When & Then
      assertThatThrownBy(() -> service.provisionForSso("sso-id-123", "testuser"))
          .isInstanceOf(JitProvisioningException.class)
          .hasMessageContaining("DW에 등록되지 않은 사용자");
    }

    @Test
    @DisplayName("Given 신규 사용자, DW 정보 없음, DW 선택 When provisionForSso Then 기본값으로 생성")
    void givenNoUserNoDwAndDwOptional_whenProvision_thenCreateWithDefaults() {
      // Given
      when(userAccountRepository.findBySsoId("sso-id-123")).thenReturn(Optional.empty());
      when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.empty());
      when(employeeDirectoryService.findActive("testuser")).thenReturn(Optional.empty());
      when(properties.isAllowWithoutDwRecord()).thenReturn(true);
      when(properties.getFallbackOrganizationCode()).thenReturn("FALLBACK_ORG");
      when(properties.getFallbackPermissionGroupCode()).thenReturn("FALLBACK_PERM");
      when(properties.getDefaultRoles()).thenReturn(Set.of("ROLE_USER"));
      when(passwordEncoder.encode(any())).thenReturn("encoded-random-password");

      // When
      JitProvisioningResult result = service.provisionForSso("sso-id-123", "testuser");

      // Then
      assertThat(result.created()).isTrue();
      verify(userAccountRepository).save(any(UserAccount.class));
    }

    @Test
    @DisplayName("Given 신규 사용자, DW 정보 있음 When provisionForSso Then DW 기반으로 생성")
    void givenNoUserWithDw_whenProvision_thenCreateFromDw() {
      // Given
      when(userAccountRepository.findBySsoId("sso-id-123")).thenReturn(Optional.empty());
      when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.empty());

      DwEmployeeSnapshot employee = createDwEmployeeSnapshot("testuser", "Test User", "ORG001", "test@example.com");
      when(employeeDirectoryService.findActive("testuser")).thenReturn(Optional.of(employee));

      when(organizationRepository.findFirstByLeaderEmployeeIdAndEffectiveEndIsNullOrderByVersionDesc("testuser"))
          .thenReturn(Optional.empty());
      when(organizationRepository.findFirstByManagerEmployeeIdAndEffectiveEndIsNullOrderByVersionDesc("testuser"))
          .thenReturn(Optional.empty());

      when(permissionResolver.resolvePermGroups(anyList(), anyBoolean())).thenReturn(Set.of("PERM_GROUP_A"));
      when(properties.getDefaultRoles()).thenReturn(Set.of("ROLE_USER"));
      when(passwordEncoder.encode(anyString())).thenReturn("encoded-random-password");

      // When
      JitProvisioningResult result = service.provisionForSso("sso-id-123", "testuser");

      // Then
      assertThat(result.created()).isTrue();
      verify(userAccountRepository).save(any(UserAccount.class));
    }
  }

  @Nested
  @DisplayName("provisionForAd 메서드")
  class ProvisionForAdTests {

    @Test
    @DisplayName("Given username으로 기존 사용자 존재 When provisionForAd Then existing 결과 반환")
    void givenExistingUser_whenProvision_thenReturnExisting() {
      // Given
      UserAccount existingAccount = createUserAccount("testuser");
      when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(existingAccount));
      when(properties.isLinkExistingUsers()).thenReturn(false);

      // When
      JitProvisioningResult result = service.provisionForAd("testuser", "CORP.LOCAL");

      // Then
      assertThat(result.account()).isEqualTo(existingAccount);
      assertThat(result.created()).isFalse();
      assertThat(result.linked()).isFalse();
    }

    @Test
    @DisplayName("Given AD 도메인 미연결 및 연결 활성화 When provisionForAd Then AD 도메인 연결")
    void givenNoAdDomainAndLinkEnabled_whenProvision_thenLinkAdDomain() {
      // Given
      UserAccount existingAccount = createUserAccount("testuser");
      when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.of(existingAccount));
      when(properties.isLinkExistingUsers()).thenReturn(true);

      // When
      JitProvisioningResult result = service.provisionForAd("testuser", "CORP.LOCAL");

      // Then
      assertThat(existingAccount.getActiveDirectoryDomain()).isEqualTo("CORP.LOCAL");
      verify(userAccountRepository).save(existingAccount);
    }

    @Test
    @DisplayName("Given 신규 사용자 When provisionForAd Then 신규 계정 생성")
    void givenNewUser_whenProvision_thenCreateNewAccount() {
      // Given
      when(userAccountRepository.findByUsername("testuser")).thenReturn(Optional.empty());
      when(employeeDirectoryService.findActive("testuser")).thenReturn(Optional.empty());
      when(properties.isAllowWithoutDwRecord()).thenReturn(true);
      when(properties.getFallbackOrganizationCode()).thenReturn("FALLBACK_ORG");
      when(properties.getFallbackPermissionGroupCode()).thenReturn("FALLBACK_PERM");
      when(properties.getDefaultRoles()).thenReturn(Set.of("ROLE_USER"));
      when(passwordEncoder.encode(any())).thenReturn("encoded-random-password");

      // When
      JitProvisioningResult result = service.provisionForAd("testuser", "CORP.LOCAL");

      // Then
      assertThat(result.created()).isTrue();
      verify(userAccountRepository).save(any(UserAccount.class));
    }
  }

  @Nested
  @DisplayName("직원 역할 결정 테스트")
  class DetermineEmployeeRoleTests {

    @Test
    @DisplayName("Given 직원이 리더인 조직 존재 When 신규 생성 Then LEADER 역할 부여")
    void givenLeaderOrg_whenCreate_thenLeaderRole() {
      // Given
      when(userAccountRepository.findBySsoId("sso-id")).thenReturn(Optional.empty());
      when(userAccountRepository.findByUsername("leader-user")).thenReturn(Optional.empty());

      DwEmployeeSnapshot employee = createDwEmployeeSnapshot("leader-user", "Leader", "ORG001", "leader@example.com");
      when(employeeDirectoryService.findActive("leader-user")).thenReturn(Optional.of(employee));

      HrOrganizationEntity leaderOrg = org.mockito.Mockito.mock(HrOrganizationEntity.class);
      when(organizationRepository.findFirstByLeaderEmployeeIdAndEffectiveEndIsNullOrderByVersionDesc("leader-user"))
          .thenReturn(Optional.of(leaderOrg));

      when(permissionResolver.resolvePermGroups(anyList(), anyBoolean())).thenReturn(Set.of("LEADER_PERM_GROUP"));
      when(properties.getDefaultRoles()).thenReturn(Set.of("ROLE_USER"));
      when(passwordEncoder.encode(anyString())).thenReturn("encoded");

      // When
      JitProvisioningResult result = service.provisionForSso("sso-id", "leader-user");

      // Then
      assertThat(result.role()).isEqualTo(EmployeeRole.LEADER);
    }

    @Test
    @DisplayName("Given 직원이 매니저인 조직 존재 When 신규 생성 Then MANAGER 역할 부여")
    void givenManagerOrg_whenCreate_thenManagerRole() {
      // Given
      when(userAccountRepository.findBySsoId("sso-id")).thenReturn(Optional.empty());
      when(userAccountRepository.findByUsername("manager-user")).thenReturn(Optional.empty());

      DwEmployeeSnapshot employee = createDwEmployeeSnapshot("manager-user", "Manager", "ORG001", "manager@example.com");
      when(employeeDirectoryService.findActive("manager-user")).thenReturn(Optional.of(employee));

      when(organizationRepository.findFirstByLeaderEmployeeIdAndEffectiveEndIsNullOrderByVersionDesc("manager-user"))
          .thenReturn(Optional.empty());

      HrOrganizationEntity managerOrg = org.mockito.Mockito.mock(HrOrganizationEntity.class);
      when(organizationRepository.findFirstByManagerEmployeeIdAndEffectiveEndIsNullOrderByVersionDesc("manager-user"))
          .thenReturn(Optional.of(managerOrg));

      when(permissionResolver.resolveManagerPermGroups(anyList())).thenReturn(Set.of("MANAGER_PERM_GROUP"));
      when(properties.getDefaultRoles()).thenReturn(Set.of("ROLE_USER"));
      when(passwordEncoder.encode(anyString())).thenReturn("encoded");

      // When
      JitProvisioningResult result = service.provisionForSso("sso-id", "manager-user");

      // Then
      assertThat(result.role()).isEqualTo(EmployeeRole.MANAGER);
    }
  }

  private UserAccount createUserAccount(String username) {
    return UserAccount.builder()
        .username(username)
        .password("encoded-password")
        .organizationCode("ORG001")
        .build();
  }

  private DwEmployeeSnapshot createDwEmployeeSnapshot(
      String employeeId, String fullName, String orgCode, String email) {
    return new DwEmployeeSnapshot(
        employeeId,
        1,
        fullName,
        email,
        orgCode,
        "REGULAR",
        "ACTIVE",
        LocalDate.now().minusYears(1),
        null,
        OffsetDateTime.now());
  }
}
