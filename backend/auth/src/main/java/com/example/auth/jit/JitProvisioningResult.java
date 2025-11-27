package com.example.auth.jit;

import com.example.auth.domain.UserAccount;

/**
 * JIT Provisioning 결과.
 */
public record JitProvisioningResult(
    UserAccount account,
    boolean created,
    boolean linked,
    EmployeeRole role) {

  public static JitProvisioningResult created(UserAccount account, EmployeeRole role) {
    return new JitProvisioningResult(account, true, false, role);
  }

  public static JitProvisioningResult linked(UserAccount account) {
    return new JitProvisioningResult(account, false, true, null);
  }

  public static JitProvisioningResult existing(UserAccount account) {
    return new JitProvisioningResult(account, false, false, null);
  }
}
