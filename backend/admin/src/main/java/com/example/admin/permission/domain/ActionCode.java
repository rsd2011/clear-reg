package com.example.admin.permission.domain;

import com.example.common.codegroup.annotation.ManagedCode;

/** 기능에서 수행할 수 있는 행위를 정의한다. */
@ManagedCode
public enum ActionCode {
  CREATE,
  READ,
  UPDATE,
  DELETE,
  APPROVE,
  EXPORT,
  UNMASK,
  UPLOAD,
  DOWNLOAD,
  DRAFT_CREATE,
  DRAFT_SUBMIT,
  DRAFT_APPROVE,
  DRAFT_READ,
  DRAFT_CANCEL,
  DRAFT_AUDIT,
  DRAFT_WITHDRAW,
  DRAFT_RESUBMIT,
  DRAFT_DELEGATE,

  APPROVAL_ADMIN,
  APPROVAL_REVIEW;

  public boolean satisfies(ActionCode required) {
    if (required == null) {
      return true;
    }
    if (this == UNMASK) {
      return true;
    }
    return this == required;
  }

  public boolean isDataFetch() {
    return this == READ
        || this == EXPORT
        || this == UNMASK
        || this == DOWNLOAD
        || this == DRAFT_READ
        || this == DRAFT_AUDIT;
  }

  /**
   * 마스킹 해제 권한이 있는 액션인지 확인한다.
   * UNMASK 액션만 마스킹된 민감 데이터의 원본을 조회할 수 있다.
   */
  public boolean canUnmask() {
    return this == UNMASK;
  }
}
