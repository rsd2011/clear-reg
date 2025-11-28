package com.example.admin.orggroup.properties;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "policy.org-group")
public class OrgGroupSettingsProperties {

  /** 신규 사용자에게 기본으로 부여할 권한 그룹 코드 목록 (System Policy). */
  private List<String> defaultGroups = new ArrayList<>();

  /** 조직 업무 매니저에게 기본으로 부여할 권한 그룹 코드. 지정되지 않으면 defaultGroups를 재사용한다. */
  private List<String> defaultManagerGroups = new ArrayList<>();

  public List<String> getDefaultGroups() {
    return defaultGroups;
  }

  public void setDefaultGroups(List<String> defaultGroups) {
    this.defaultGroups = defaultGroups == null ? new ArrayList<>() : new ArrayList<>(defaultGroups);
  }

  public List<String> getDefaultManagerGroups() {
    return defaultManagerGroups;
  }

  public void setDefaultManagerGroups(List<String> defaultManagerGroups) {
    this.defaultManagerGroups =
        defaultManagerGroups == null ? new ArrayList<>() : new ArrayList<>(defaultManagerGroups);
  }
}
