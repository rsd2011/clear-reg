package com.example.auth.jit;

/**
 * DW 조직 정보 기반 직원 역할.
 * OrgGroupPermissionResolver에서 역할별 권한 그룹을 해석할 때 사용된다.
 */
public enum EmployeeRole {
  /** 조직의 리더 (leaderEmployeeId와 일치). */
  LEADER,
  /** 조직의 업무 매니저 (managerEmployeeId와 일치). */
  MANAGER,
  /** 일반 조직원 (리더도 매니저도 아닌 경우). */
  MEMBER
}
