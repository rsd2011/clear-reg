package com.example.auth.permission;

/** 기능에서 수행할 수 있는 행위를 정의한다. */
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
}
