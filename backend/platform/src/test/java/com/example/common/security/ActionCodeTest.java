package com.example.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ActionCode 테스트")
class ActionCodeTest {

  @Test
  @DisplayName("Given 모든 ActionCode 상수 When values 호출 Then 모든 상수가 반환된다")
  void givenAllActionCodes_whenValues_thenReturnsAllConstants() {
    ActionCode[] values = ActionCode.values();

    assertThat(values).hasSize(20);
    assertThat(values).contains(
        ActionCode.CREATE,
        ActionCode.READ,
        ActionCode.UPDATE,
        ActionCode.DELETE,
        ActionCode.APPROVE,
        ActionCode.EXPORT,
        ActionCode.UNMASK,
        ActionCode.UPLOAD,
        ActionCode.DOWNLOAD,
        ActionCode.DRAFT_CREATE,
        ActionCode.DRAFT_SUBMIT,
        ActionCode.DRAFT_APPROVE,
        ActionCode.DRAFT_READ,
        ActionCode.DRAFT_CANCEL,
        ActionCode.DRAFT_AUDIT,
        ActionCode.DRAFT_WITHDRAW,
        ActionCode.DRAFT_RESUBMIT,
        ActionCode.DRAFT_DELEGATE,
        ActionCode.APPROVAL_ADMIN,
        ActionCode.APPROVAL_REVIEW
    );
  }

  @Nested
  @DisplayName("satisfies 메서드")
  class SatisfiesTest {

    @Test
    @DisplayName("Given null required When satisfies 호출 Then true 반환")
    void givenNullRequired_whenSatisfies_thenTrue() {
      assertThat(ActionCode.READ.satisfies(null)).isTrue();
      assertThat(ActionCode.CREATE.satisfies(null)).isTrue();
    }

    @Test
    @DisplayName("Given UNMASK 액션 When satisfies 호출 Then 항상 true")
    void givenUnmaskAction_whenSatisfies_thenAlwaysTrue() {
      assertThat(ActionCode.UNMASK.satisfies(ActionCode.READ)).isTrue();
      assertThat(ActionCode.UNMASK.satisfies(ActionCode.CREATE)).isTrue();
      assertThat(ActionCode.UNMASK.satisfies(ActionCode.UNMASK)).isTrue();
    }

    @Test
    @DisplayName("Given 동일한 액션 When satisfies 호출 Then true 반환")
    void givenSameAction_whenSatisfies_thenTrue() {
      assertThat(ActionCode.READ.satisfies(ActionCode.READ)).isTrue();
      assertThat(ActionCode.CREATE.satisfies(ActionCode.CREATE)).isTrue();
    }

    @Test
    @DisplayName("Given 다른 액션 When satisfies 호출 Then false 반환")
    void givenDifferentAction_whenSatisfies_thenFalse() {
      assertThat(ActionCode.READ.satisfies(ActionCode.CREATE)).isFalse();
      assertThat(ActionCode.CREATE.satisfies(ActionCode.DELETE)).isFalse();
    }
  }

  @Nested
  @DisplayName("isDataFetch 메서드")
  class IsDataFetchTest {

    @Test
    @DisplayName("Given 데이터 조회 액션 When isDataFetch 호출 Then true 반환")
    void givenDataFetchActions_whenIsDataFetch_thenTrue() {
      assertThat(ActionCode.READ.isDataFetch()).isTrue();
      assertThat(ActionCode.EXPORT.isDataFetch()).isTrue();
      assertThat(ActionCode.UNMASK.isDataFetch()).isTrue();
      assertThat(ActionCode.DOWNLOAD.isDataFetch()).isTrue();
      assertThat(ActionCode.DRAFT_READ.isDataFetch()).isTrue();
      assertThat(ActionCode.DRAFT_AUDIT.isDataFetch()).isTrue();
    }

    @Test
    @DisplayName("Given 데이터 조회가 아닌 액션 When isDataFetch 호출 Then false 반환")
    void givenNonDataFetchActions_whenIsDataFetch_thenFalse() {
      assertThat(ActionCode.CREATE.isDataFetch()).isFalse();
      assertThat(ActionCode.UPDATE.isDataFetch()).isFalse();
      assertThat(ActionCode.DELETE.isDataFetch()).isFalse();
      assertThat(ActionCode.APPROVE.isDataFetch()).isFalse();
      assertThat(ActionCode.UPLOAD.isDataFetch()).isFalse();
    }
  }

  @Nested
  @DisplayName("canUnmask 메서드")
  class CanUnmaskTest {

    @Test
    @DisplayName("Given UNMASK 액션 When canUnmask 호출 Then true 반환")
    void givenUnmaskAction_whenCanUnmask_thenTrue() {
      assertThat(ActionCode.UNMASK.canUnmask()).isTrue();
    }

    @Test
    @DisplayName("Given UNMASK가 아닌 액션 When canUnmask 호출 Then false 반환")
    void givenNonUnmaskAction_whenCanUnmask_thenFalse() {
      assertThat(ActionCode.READ.canUnmask()).isFalse();
      assertThat(ActionCode.EXPORT.canUnmask()).isFalse();
      assertThat(ActionCode.CREATE.canUnmask()).isFalse();
    }
  }
}
