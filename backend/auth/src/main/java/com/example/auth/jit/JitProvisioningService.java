package com.example.auth.jit;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.admin.orggroup.OrgGroupPermissionResolver;
import com.example.auth.LoginType;
import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountRepository;
import com.example.dw.application.DwEmployeeDirectoryService;
import com.example.dw.application.DwEmployeeSnapshot;
import com.example.dw.domain.HrOrganizationEntity;
import com.example.dw.infrastructure.persistence.HrOrganizationRepository;
import com.example.auth.jit.dto.JitProvisioningResult;

/**
 * SSO/AD 인증 시 JIT(Just-In-Time) Provisioning을 수행한다.
 *
 * <p>1. SSO/AD로 인증된 사용자가 시스템에 없을 때 DW 정보 기반으로 자동 생성
 * <p>2. 기존 사용자와 SSO/AD ID 연결
 * <p>3. OrgGroupPermissionResolver를 통한 권한 그룹 자동 할당
 */
@Service
public class JitProvisioningService {

  private static final Logger log = LoggerFactory.getLogger(JitProvisioningService.class);

  private final JitProvisioningProperties properties;
  private final UserAccountRepository userAccountRepository;
  private final DwEmployeeDirectoryService employeeDirectoryService;
  private final HrOrganizationRepository organizationRepository;
  private final OrgGroupPermissionResolver permissionResolver;
  private final PasswordEncoder passwordEncoder;

  public JitProvisioningService(
      JitProvisioningProperties properties,
      UserAccountRepository userAccountRepository,
      DwEmployeeDirectoryService employeeDirectoryService,
      HrOrganizationRepository organizationRepository,
      OrgGroupPermissionResolver permissionResolver,
      PasswordEncoder passwordEncoder) {
    this.properties = properties;
    this.userAccountRepository = userAccountRepository;
    this.employeeDirectoryService = employeeDirectoryService;
    this.organizationRepository = organizationRepository;
    this.permissionResolver = permissionResolver;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * JIT Provisioning이 활성화되었는지 확인한다.
   */
  public boolean isEnabled() {
    return properties.isEnabled();
  }

  /**
   * 특정 로그인 타입에 JIT Provisioning이 적용되는지 확인한다.
   */
  public boolean isEnabledFor(LoginType loginType) {
    if (!properties.isEnabled()) {
      return false;
    }
    Set<String> enabledTypes = properties.getEnabledLoginTypes();
    return enabledTypes.isEmpty() || enabledTypes.contains(loginType.name());
  }

  /**
   * SSO 인증된 사용자에 대해 JIT Provisioning을 수행한다.
   *
   * @param ssoId SSO 시스템에서 발급한 고유 ID
   * @param username 사용자명 (employeeId로 가정)
   * @return Provisioning 결과
   */
  @Transactional
  public JitProvisioningResult provisionForSso(String ssoId, String username) {
    // 1. SSO ID로 기존 사용자 찾기
    Optional<UserAccount> bySsoId = userAccountRepository.findBySsoId(ssoId);
    if (bySsoId.isPresent()) {
      return JitProvisioningResult.existing(bySsoId.get());
    }

    // 2. username(employeeId)으로 기존 사용자 찾아서 SSO ID 연결
    Optional<UserAccount> byUsername = userAccountRepository.findByUsername(username);
    if (byUsername.isPresent()) {
      if (properties.isLinkExistingUsers()) {
        UserAccount account = byUsername.get();
        account.linkSsoId(ssoId);
        userAccountRepository.save(account);
        log.info("기존 사용자에 SSO ID 연결: username={}, ssoId={}", username, ssoId);
        return JitProvisioningResult.linked(account);
      }
      return JitProvisioningResult.existing(byUsername.get());
    }

    // 3. 신규 사용자 생성
    return createNewUser(username, ssoId, null);
  }

  /**
   * AD 인증된 사용자에 대해 JIT Provisioning을 수행한다.
   *
   * @param username 사용자명 (employeeId로 가정)
   * @param adDomain AD 도메인
   * @return Provisioning 결과
   */
  @Transactional
  public JitProvisioningResult provisionForAd(String username, String adDomain) {
    // 1. username으로 기존 사용자 찾기
    Optional<UserAccount> byUsername = userAccountRepository.findByUsername(username);
    if (byUsername.isPresent()) {
      UserAccount account = byUsername.get();
      // AD 도메인 연결
      if (properties.isLinkExistingUsers() && account.getActiveDirectoryDomain() == null) {
        account.assignActiveDirectoryDomain(adDomain);
        userAccountRepository.save(account);
        log.info("기존 사용자에 AD 도메인 연결: username={}, domain={}", username, adDomain);
        return JitProvisioningResult.linked(account);
      }
      return JitProvisioningResult.existing(account);
    }

    // 2. 신규 사용자 생성
    JitProvisioningResult result = createNewUser(username, null, adDomain);
    return result;
  }

  private JitProvisioningResult createNewUser(String username, String ssoId, String adDomain) {
    // DW에서 직원 정보 조회
    Optional<DwEmployeeSnapshot> employeeOpt = employeeDirectoryService.findActive(username);

    if (employeeOpt.isEmpty()) {
      if (!properties.isAllowWithoutDwRecord()) {
        log.warn("DW에 직원 정보가 없어 JIT 생성 거부: username={}", username);
        throw new JitProvisioningException("DW에 등록되지 않은 사용자입니다: " + username);
      }
      // DW 없이 기본값으로 생성
      return createUserWithDefaults(username, ssoId, adDomain);
    }

    DwEmployeeSnapshot employee = employeeOpt.get();
    String orgCode = employee.organizationCode();

    // 직원 역할 결정 (리더/매니저/멤버)
    EmployeeRole role = determineEmployeeRole(username, orgCode);

    // 권한 그룹 해석
    String permGroupCode = resolvePermissionGroupCode(orgCode, role);

    UserAccount account = UserAccount.builder()
        .username(username)
        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
        .email(employee.email())
        .organizationCode(orgCode != null ? orgCode : properties.getFallbackOrganizationCode())
        .permissionGroupCode(permGroupCode)
        .roles(properties.getDefaultRoles())
        .build();

    if (ssoId != null) {
      account.linkSsoId(ssoId);
    }
    if (adDomain != null) {
      account.assignActiveDirectoryDomain(adDomain);
    }

    userAccountRepository.save(account);
    log.info("JIT Provisioning 완료: username={}, role={}, permGroupCode={}",
        username, role, permGroupCode);

    return JitProvisioningResult.created(account, role);
  }

  private JitProvisioningResult createUserWithDefaults(
      String username, String ssoId, String adDomain) {

    UserAccount account = UserAccount.builder()
        .username(username)
        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
        .organizationCode(properties.getFallbackOrganizationCode())
        .permissionGroupCode(properties.getFallbackPermissionGroupCode())
        .roles(properties.getDefaultRoles())
        .build();

    if (ssoId != null) {
      account.linkSsoId(ssoId);
    }
    if (adDomain != null) {
      account.assignActiveDirectoryDomain(adDomain);
    }

    userAccountRepository.save(account);
    log.info("JIT Provisioning 완료 (DW 미등록, 기본값 사용): username={}", username);

    return JitProvisioningResult.created(account, EmployeeRole.MEMBER);
  }

  /**
   * 직원의 역할을 결정한다.
   * 조직의 리더 또는 매니저인 경우 해당 역할을, 아니면 MEMBER를 반환한다.
   */
  private EmployeeRole determineEmployeeRole(String employeeId, String orgCode) {
    if (employeeId == null || orgCode == null) {
      return EmployeeRole.MEMBER;
    }

    // 해당 직원이 리더인 조직 확인
    Optional<HrOrganizationEntity> leaderOrg =
        organizationRepository.findFirstByLeaderEmployeeIdAndEffectiveEndIsNullOrderByVersionDesc(employeeId);
    if (leaderOrg.isPresent()) {
      return EmployeeRole.LEADER;
    }

    // 해당 직원이 매니저인 조직 확인
    Optional<HrOrganizationEntity> managerOrg =
        organizationRepository.findFirstByManagerEmployeeIdAndEffectiveEndIsNullOrderByVersionDesc(employeeId);
    if (managerOrg.isPresent()) {
      return EmployeeRole.MANAGER;
    }

    return EmployeeRole.MEMBER;
  }

  /**
   * OrgGroupPermissionResolver를 사용하여 권한 그룹 코드를 해석한다.
   * 단일 부여이므로 첫 번째 결과만 사용한다.
   */
  private String resolvePermissionGroupCode(String orgCode, EmployeeRole role) {
    if (orgCode == null) {
      return properties.getFallbackPermissionGroupCode();
    }

    List<String> orgIds = List.of(orgCode);
    Set<String> permGroups;

    switch (role) {
      case LEADER:
        permGroups = permissionResolver.resolvePermGroups(orgIds, true);
        break;
      case MANAGER:
        permGroups = permissionResolver.resolveManagerPermGroups(orgIds);
        break;
      case MEMBER:
      default:
        permGroups = permissionResolver.resolvePermGroups(orgIds, false);
        break;
    }

    // 단일 부여: 첫 번째 권한 그룹만 사용
    return permGroups.stream()
        .findFirst()
        .orElse(properties.getFallbackPermissionGroupCode());
  }
}
