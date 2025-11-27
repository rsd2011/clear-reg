package com.example.admin.permission;

import com.example.common.jpa.PrimaryKeyEntity;
import com.example.common.security.RowScope;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "SE_BAD_FIELD"},
    justification =
        "Entity returns unmodifiable views; persistence proxies handle serialization of collections")
@Entity
@Table(name = "permission_groups")
public class PermissionGroup extends PrimaryKeyEntity {

  @Column(nullable = false, unique = true)
  private String code;

  @Column(nullable = false)
  private String name;

  @Column(length = 1000)
  private String description;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "permission_group_assignments",
      joinColumns = @JoinColumn(name = "group_id"))
  private Set<PermissionAssignment> assignments = new HashSet<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "permission_group_mask_rules",
      joinColumns = @JoinColumn(name = "group_id"))
  private Set<FieldMaskRule> maskRules = new HashSet<>();

  @Enumerated(EnumType.STRING)
  @Column(name = "default_row_scope", nullable = false)
  private RowScope defaultRowScope = RowScope.OWN;

  @Column(name = "approval_group_code", length = 64)
  private String approvalGroupCode;

  protected PermissionGroup() {}

  public PermissionGroup(String code, String name) {
    this.code = code;
    this.name = name;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public RowScope getDefaultRowScope() {
    return defaultRowScope;
  }

  public void updateDetails(String name, String description, RowScope defaultRowScope) {
    this.name = name;
    this.description = description;
    this.defaultRowScope = defaultRowScope == null ? RowScope.OWN : defaultRowScope;
  }

  public Set<PermissionAssignment> getAssignments() {
    return Collections.unmodifiableSet(assignments);
  }

  public Optional<PermissionAssignment> assignmentFor(FeatureCode feature, ActionCode action) {
    return assignments.stream()
        .filter(
            assignment -> assignment.getFeature() == feature && assignment.getAction() == action)
        .findFirst();
  }

  public Map<String, FieldMaskRule> maskRulesByTag() {
    Map<String, FieldMaskRule> map = new HashMap<>();
    for (FieldMaskRule rule : maskRules) {
      map.put(rule.getTag(), rule);
    }
    return Collections.unmodifiableMap(map);
  }

  public void replaceAssignments(Collection<PermissionAssignment> replacements) {
    assignments.clear();
    if (replacements != null) {
      assignments.addAll(replacements);
    }
  }

  public void replaceMaskRules(Collection<FieldMaskRule> replacements) {
    maskRules.clear();
    if (replacements != null) {
      maskRules.addAll(replacements);
    }
  }

  public Optional<String> getApprovalGroupCode() {
    return Optional.ofNullable(approvalGroupCode);
  }

  public void setApprovalGroupCode(String approvalGroupCode) {
    this.approvalGroupCode = approvalGroupCode;
  }
}
