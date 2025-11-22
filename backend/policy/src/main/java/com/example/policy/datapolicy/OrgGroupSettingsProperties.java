package com.example.policy.datapolicy;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "policy.org-group")
public class OrgGroupSettingsProperties {

    /**
     * 신규 조직이 자동으로 속하는 기본 그룹 코드 목록.
     */
    private List<String> defaultGroups = new ArrayList<>();

    /**
     * defaultGroups가 비어있을 때, org_group에서 priority가 가장 낮은(숫자가 큰) 그룹을 기본으로 사용.
     */
    private boolean fallbackToLowestPriorityGroup = true;

    public List<String> getDefaultGroups() {
        return defaultGroups;
    }

    public void setDefaultGroups(List<String> defaultGroups) {
        this.defaultGroups = defaultGroups;
    }

    public boolean isFallbackToLowestPriorityGroup() {
        return fallbackToLowestPriorityGroup;
    }

    public void setFallbackToLowestPriorityGroup(boolean fallbackToLowestPriorityGroup) {
        this.fallbackToLowestPriorityGroup = fallbackToLowestPriorityGroup;
    }
}
