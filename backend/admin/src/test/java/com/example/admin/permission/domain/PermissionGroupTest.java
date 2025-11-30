package com.example.admin.permission.domain;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PermissionGroup 도메인 테스트")
class PermissionGroupTest {

  private final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

  @Nested
  @DisplayName("create 메서드")
  class CreateTests {

    @Test
    @DisplayName("Given 권한 할당 When 조회하면 Then 해당 규칙이 반환된다")
    void givenAssignments_whenLookup_thenReturnMatchingPermission() {
      PermissionGroupRoot root = PermissionGroupRoot.createWithCode("AUDIT", now);
      PermissionAssignment assignment =
          new PermissionAssignment(FeatureCode.ORGANIZATION, ActionCode.READ);

      PermissionGroup group = PermissionGroup.create(
          root, 1, "Auditor", "감사 그룹", true,
          List.of(assignment), List.of(),
          ChangeAction.CREATE, "초기 생성", "SYSTEM", "System", now);

      assertThat(group.assignmentFor(FeatureCode.ORGANIZATION, ActionCode.READ))
          .contains(assignment);
    }

    @Test
    @DisplayName("Given 권한 할당이 없을 때 When 조회하면 Then empty 반환")
    void givenNoAssignment_whenLookup_thenReturnEmpty() {
      PermissionGroupRoot root = PermissionGroupRoot.createWithCode("AUDIT", now);

      PermissionGroup group = PermissionGroup.create(
          root, 1, "Auditor", "감사 그룹", true,
          List.of(), List.of(),
          ChangeAction.CREATE, "초기 생성", "SYSTEM", "System", now);

      assertThat(group.assignmentFor(FeatureCode.ORGANIZATION, ActionCode.READ))
          .isEmpty();
    }

    @Test
    @DisplayName("Given 생성 시 Then PUBLISHED 상태로 생성된다")
    void createSetsPublishedStatus() {
      PermissionGroupRoot root = PermissionGroupRoot.createWithCode("GROUP", now);

      PermissionGroup group = PermissionGroup.create(
          root, 1, "Test", "테스트", true,
          List.of(), List.of(),
          ChangeAction.CREATE, "생성", "admin", "관리자", now);

      assertThat(group.getStatus()).isEqualTo(VersionStatus.PUBLISHED);
      assertThat(group.isDraft()).isFalse();
      assertThat(group.isCurrent()).isTrue();
    }

    @Test
    @DisplayName("Given 승인 그룹 코드 목록 When 생성 Then 코드 목록이 저장된다")
    void createWithApprovalGroupCodes() {
      PermissionGroupRoot root = PermissionGroupRoot.createWithCode("GROUP", now);
      List<String> approvalCodes = List.of("GRP1", "GRP2", "GRP3");

      PermissionGroup group = PermissionGroup.create(
          root, 1, "Test", "테스트", true,
          List.of(), approvalCodes,
          ChangeAction.CREATE, "생성", "admin", "관리자", now);

      assertThat(group.getApprovalGroupCodes()).containsExactlyElementsOf(approvalCodes);
    }
  }

  @Nested
  @DisplayName("createDraft 메서드")
  class CreateDraftTests {

    @Test
    @DisplayName("Given 초안 생성 요청 When createDraft 호출 Then DRAFT 상태로 생성")
    void createDraftSetsDraftStatus() {
      PermissionGroupRoot root = PermissionGroupRoot.createWithCode("GROUP", now);

      PermissionGroup draft = PermissionGroup.createDraft(
          root, 1, "Draft Test", "초안 테스트", true,
          List.of(), List.of(),
          "초안 생성", "admin", "관리자", now);

      assertThat(draft.getStatus()).isEqualTo(VersionStatus.DRAFT);
      assertThat(draft.isDraft()).isTrue();
      assertThat(draft.isCurrent()).isFalse();
      // 초안도 생성 시점(validFrom)이 설정됨 - 게시 시 publish()에서 갱신됨
      assertThat(draft.getValidFrom()).isEqualTo(now);
      assertThat(draft.getChangeAction()).isEqualTo(ChangeAction.DRAFT);
    }
  }

  @Nested
  @DisplayName("close 메서드")
  class CloseTests {

    @Test
    @DisplayName("Given PUBLISHED 상태 When close 호출 Then HISTORICAL 상태로 변경")
    void closeChangesToHistorical() {
      PermissionGroupRoot root = PermissionGroupRoot.createWithCode("GROUP", now);
      PermissionGroup group = PermissionGroup.create(
          root, 1, "Test", "테스트", true,
          List.of(), List.of(),
          ChangeAction.CREATE, "생성", "admin", "관리자", now);

      OffsetDateTime closeTime = now.plusHours(1);
      group.close(closeTime);

      assertThat(group.getStatus()).isEqualTo(VersionStatus.HISTORICAL);
      assertThat(group.getValidTo()).isEqualTo(closeTime);
      assertThat(group.isCurrent()).isFalse();
    }

    @Test
    @DisplayName("Given DRAFT 상태 When close 호출 Then validTo만 설정됨")
    void closeDraftDoesNotChangeStatus() {
      PermissionGroupRoot root = PermissionGroupRoot.createWithCode("GROUP", now);
      PermissionGroup draft = PermissionGroup.createDraft(
          root, 1, "Draft", "초안", true,
          List.of(), List.of(),
          "초안", "admin", "관리자", now);

      OffsetDateTime closeTime = now.plusHours(1);
      draft.close(closeTime);

      assertThat(draft.getStatus()).isEqualTo(VersionStatus.DRAFT);
      assertThat(draft.getValidTo()).isEqualTo(closeTime);
    }
  }

  @Nested
  @DisplayName("publish 메서드")
  class PublishTests {

    @Test
    @DisplayName("Given DRAFT 상태 When publish 호출 Then PUBLISHED 상태로 변경")
    void publishChangesToPublished() {
      PermissionGroupRoot root = PermissionGroupRoot.createWithCode("GROUP", now);
      PermissionGroup draft = PermissionGroup.createDraft(
          root, 1, "Draft", "초안", true,
          List.of(), List.of(),
          "초안", "admin", "관리자", now);

      OffsetDateTime publishTime = now.plusHours(1);
      draft.publish(publishTime);

      assertThat(draft.getStatus()).isEqualTo(VersionStatus.PUBLISHED);
      assertThat(draft.isDraft()).isFalse();
      assertThat(draft.getValidFrom()).isEqualTo(publishTime);
      assertThat(draft.getChangeAction()).isEqualTo(ChangeAction.PUBLISH);
    }

    @Test
    @DisplayName("Given PUBLISHED 상태 When publish 호출 Then IllegalStateException 발생")
    void publishFromPublishedThrows() {
      PermissionGroupRoot root = PermissionGroupRoot.createWithCode("GROUP", now);
      PermissionGroup group = PermissionGroup.create(
          root, 1, "Test", "테스트", true,
          List.of(), List.of(),
          ChangeAction.CREATE, "생성", "admin", "관리자", now);

      assertThatThrownBy(() -> group.publish(now.plusHours(1)))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("초안 상태에서만 게시할 수 있습니다");
    }
  }

  @Nested
  @DisplayName("updateDraft 메서드")
  class UpdateDraftTests {

    @Test
    @DisplayName("Given DRAFT 상태 When updateDraft 호출 Then 내용이 업데이트됨")
    void updateDraftModifiesContent() {
      PermissionGroupRoot root = PermissionGroupRoot.createWithCode("GROUP", now);
      PermissionGroup draft = PermissionGroup.createDraft(
          root, 1, "Original", "원본", true,
          List.of(), List.of(),
          "초안", "admin", "관리자", now);

      PermissionAssignment newAssignment = new PermissionAssignment(FeatureCode.DRAFT, ActionCode.DRAFT_CREATE);
      List<String> newApprovalCodes = List.of("NEW_GRP1");

      OffsetDateTime updateTime = now.plusMinutes(30);
      draft.updateDraft("Updated", "수정됨", false,
          List.of(newAssignment), newApprovalCodes, "내용 수정", updateTime);

      assertThat(draft.getName()).isEqualTo("Updated");
      assertThat(draft.getDescription()).isEqualTo("수정됨");
      assertThat(draft.isActive()).isFalse();
      assertThat(draft.getAssignments()).contains(newAssignment);
      assertThat(draft.getApprovalGroupCodes()).containsExactly("NEW_GRP1");
      assertThat(draft.getChangeReason()).isEqualTo("내용 수정");
    }

    @Test
    @DisplayName("Given PUBLISHED 상태 When updateDraft 호출 Then IllegalStateException 발생")
    void updateDraftFromPublishedThrows() {
      PermissionGroupRoot root = PermissionGroupRoot.createWithCode("GROUP", now);
      PermissionGroup group = PermissionGroup.create(
          root, 1, "Test", "테스트", true,
          List.of(), List.of(),
          ChangeAction.CREATE, "생성", "admin", "관리자", now);

      assertThatThrownBy(() -> group.updateDraft("New", "새로운", true,
          List.of(), List.of(), "수정", now.plusHours(1)))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("초안 상태에서만 수정할 수 있습니다");
    }
  }

  @Nested
  @DisplayName("getCode 메서드")
  class GetCodeTests {

    @Test
    @DisplayName("Given Root가 있는 PermissionGroup When getCode 호출 Then Root의 groupCode 반환")
    void getCodeReturnsRootGroupCode() {
      PermissionGroupRoot root = PermissionGroupRoot.createWithCode("TEST_CODE", now);
      PermissionGroup group = PermissionGroup.create(
          root, 1, "Test", "테스트", true,
          List.of(), List.of(),
          ChangeAction.CREATE, "생성", "admin", "관리자", now);

      assertThat(group.getCode()).isEqualTo("TEST_CODE");
    }
  }
}
