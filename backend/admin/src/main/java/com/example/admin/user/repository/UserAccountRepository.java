package com.example.admin.user.repository;

import com.example.admin.user.domain.UserAccount;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 사용자 계정 리포지토리.
 */
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID>,
    JpaSpecificationExecutor<UserAccount> {

  /**
   * 사용자명으로 조회.
   */
  Optional<UserAccount> findByUsername(String username);

  /**
   * SSO ID로 조회.
   */
  Optional<UserAccount> findBySsoId(String ssoId);

  /**
   * 권한 그룹 코드 목록에 해당하는 사용자 조회.
   */
  List<UserAccount> findByPermissionGroupCodeIn(Collection<String> permissionGroupCodes);

  /**
   * 사번으로 조회.
   *
   * @param employeeId 사번
   * @return 사용자 계정 (존재하는 경우)
   */
  Optional<UserAccount> findByEmployeeId(String employeeId);
}
