package com.example.policy.datapolicy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OrgGroupPermissionResolver {

    private final OrgGroupRepository repository;
    private final OrgGroupSettingsProperties properties;

    public OrgGroupPermissionResolver(OrgGroupRepository repository,
                                      OrgGroupSettingsProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /**
     * 조직이 속한 그룹을 기반으로 리더/일반 조직원 권한그룹 코드를 해석한다.
     * @param orgIds 대상 조직 ID 목록(DW)
     * @param isLeader true면 leader_perm_group_code, false면 member_perm_group_code 사용
     */
    public Set<String> resolvePermGroups(List<String> orgIds, boolean isLeader) {
        if (orgIds == null || orgIds.isEmpty()) {
            return Set.of();
        }
        var members = repository.findByOrgIdInOrderByPriorityAsc(orgIds);
        var ordered = new java.util.LinkedHashSet<String>();
        for (OrgGroupMember m : members) {
            String code = isLeader ? m.getLeaderPermGroupCode() : m.getMemberPermGroupCode();
            if (StringUtils.hasText(code)) {
                ordered.add(code);
            }
        }
        if (ordered.isEmpty()) {
            if (properties.getDefaultGroups() != null && !properties.getDefaultGroups().isEmpty()) {
                ordered.addAll(properties.getDefaultGroups());
            } else if (properties.isFallbackToLowestPriorityGroup()) {
                OrgGroup lowest = repository.findTopByOrderByPriorityDesc();
                if (lowest != null) {
                    ordered.add(lowest.getCode());
                }
            }
        }
        return ordered;
    }
}
