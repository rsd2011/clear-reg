package com.example.admin.orggroup;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * OrgGroup 기반으로 신규 사용자에게 자동 부여할 권한 그룹을 해석한다.
 * 폴백은 System Policy(OrgGroupSettingsProperties.defaultGroups) 기반으로 처리한다.
 */
@Component
public class OrgGroupPermissionResolver {

  private final OrgGroupRepository repository;
  private final OrgGroupSettingsProperties settings;

  public OrgGroupPermissionResolver(
      OrgGroupRepository repository, OrgGroupSettingsProperties settings) {
    this.repository = repository;
    this.settings = settings;
  }

  /**
   * 조직이 속한 그룹을 기반으로 리더/일반 조직원 권한그룹 코드를 해석한다.
   *
   * @param orgIds 대상 조직 ID 목록(DW)
   * @param isLeader true면 leaderPermGroupCode, false면 memberPermGroupCode 사용
   */
  public Set<String> resolvePermGroups(List<String> orgIds, boolean isLeader) {
    if (orgIds == null || orgIds.isEmpty()) {
      return Set.of();
    }

    var ordered = new LinkedHashSet<String>();
    var groups = repository.findByMemberOrgIdsOrderBySortAsc(orgIds);

    for (OrgGroup g : groups) {
      String code = isLeader ? g.getLeaderPermGroupCode() : g.getMemberPermGroupCode();
      if (StringUtils.hasText(code)) {
        ordered.add(code);
      }
    }

    // 폴백: System Policy (설정 기반)
    if (ordered.isEmpty() && settings.getDefaultGroups() != null) {
      ordered.addAll(settings.getDefaultGroups());
    }

    return ordered;
  }

  /**
   * 조직 업무 매니저(리더와 별개 역할)에 대한 권한 그룹을 해석한다.
   * 우선 순서: 그룹별 managerPermGroupCode → 설정 defaultManagerGroups → defaultGroups.
   */
  public Set<String> resolveManagerPermGroups(List<String> orgIds) {
    if (orgIds == null || orgIds.isEmpty()) {
      return Set.of();
    }

    var ordered = new LinkedHashSet<String>();
    var groups = repository.findByMemberOrgIdsOrderBySortAsc(orgIds);

    for (OrgGroup g : groups) {
      if (StringUtils.hasText(g.getManagerPermGroupCode())) {
        ordered.add(g.getManagerPermGroupCode());
      }
    }

    // 폴백: System Policy (설정 기반)
    if (ordered.isEmpty()) {
      if (settings.getDefaultManagerGroups() != null
          && !settings.getDefaultManagerGroups().isEmpty()) {
        ordered.addAll(settings.getDefaultManagerGroups());
      } else if (settings.getDefaultGroups() != null) {
        ordered.addAll(settings.getDefaultGroups());
      }
    }

    return ordered;
  }
}
