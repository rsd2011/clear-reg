package com.example.auth.permission;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class FieldMaskRule {

  @Column(name = "mask_tag", nullable = false)
  private String tag;

  @Column(name = "mask_with", nullable = false)
  private String maskWith = "***";

  @Enumerated(EnumType.STRING)
  @Column(name = "required_action", nullable = false)
  private ActionCode requiredAction = ActionCode.UNMASK;

  @Column(name = "audit", nullable = false)
  private boolean audit;

  protected FieldMaskRule() {}

  public FieldMaskRule(String tag, String maskWith, ActionCode requiredAction, boolean audit) {
    this.tag = tag;
    this.maskWith = maskWith == null ? "***" : maskWith;
    this.requiredAction = requiredAction == null ? ActionCode.UNMASK : requiredAction;
    this.audit = audit;
  }

  public String getTag() {
    return tag;
  }

  public String getMaskWith() {
    return maskWith;
  }

  public ActionCode getRequiredAction() {
    return requiredAction;
  }

  public boolean isAudit() {
    return audit;
  }
}
