package com.example.auth.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

  Optional<UserAccount> findByUsername(String username);

  Optional<UserAccount> findBySsoId(String ssoId);

  List<UserAccount> findByPermissionGroupCodeIn(Collection<String> permissionGroupCodes);
}
