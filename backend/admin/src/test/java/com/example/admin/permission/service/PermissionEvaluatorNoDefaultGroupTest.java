package com.example.admin.permission.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.admin.permission.TestUserInfo;
import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import com.example.admin.permission.exception.PermissionDeniedException;
import com.example.admin.permission.spi.UserInfo;
import com.example.admin.permission.spi.UserInfoProvider;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionEvaluatorNoDefaultGroupTest {

  @Mock UserInfoProvider userInfoProvider;
  @Mock PermissionGroupService groupService;

  @Test
  @DisplayName("DEFAULT 그룹이 존재하지 않으면 PermissionDeniedException을 던진다")
  void throwsWhenDefaultGroupNotFound() {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("user", "pw"));
    PermissionEvaluator evaluator =
        new PermissionEvaluator(userInfoProvider, groupService, List.of());

    UserInfo user = new TestUserInfo("user", "ORG", null, Set.of());
    given(userInfoProvider.getByUsernameOrThrow("user")).willReturn(user);
    given(groupService.getByCodeOrThrow("DEFAULT"))
        .willThrow(new PermissionDeniedException("그룹을 찾을 수 없습니다: DEFAULT"));

    assertThatThrownBy(() -> evaluator.evaluate(FeatureCode.ORGANIZATION, ActionCode.READ))
        .isInstanceOf(PermissionDeniedException.class);
  }
}
