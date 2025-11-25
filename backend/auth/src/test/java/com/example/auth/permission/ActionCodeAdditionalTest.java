package com.example.auth.permission;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ActionCodeAdditionalTest {

  @Test
  @DisplayName("satisfies는 required가 null이면 true를 반환한다")
  void satisfiesReturnsTrueWhenRequiredNull() {
    assertThat(ActionCode.CREATE.satisfies(null)).isTrue();
  }

  @Test
  @DisplayName("isDataFetch는 UPLOAD에서는 false를 반환한다")
  void isDataFetchFalseForUpload() {
    assertThat(ActionCode.UPLOAD.isDataFetch()).isFalse();
  }

  @Test
  @DisplayName("isDataFetch는 DRAFT_AUDIT에서는 true를 반환한다")
  void isDataFetchTrueForDraftAudit() {
    assertThat(ActionCode.DRAFT_AUDIT.isDataFetch()).isTrue();
  }

  @Test
  @DisplayName("isDataFetch는 DOWNLOAD에서는 true를 반환한다")
  void isDataFetchTrueForDownload() {
    assertThat(ActionCode.DOWNLOAD.isDataFetch()).isTrue();
  }

  @Test
  @DisplayName("isDataFetch는 DRAFT_CREATE에서는 false를 반환한다")
  void isDataFetchFalseForDraftCreate() {
    assertThat(ActionCode.DRAFT_CREATE.isDataFetch()).isFalse();
  }
}
