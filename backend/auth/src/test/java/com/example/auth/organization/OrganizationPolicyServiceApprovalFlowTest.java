package com.example.auth.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationPolicyServiceApprovalFlowTest {

  @Mock OrganizationPolicyCache cache;
  @Mock OrganizationPolicyCache.OrganizationPolicySnapshot snapshot;

  @Test
  @DisplayName("approvalFlow는 캐시에서 승인 플로우를 반환한다")
  void approvalFlowReturnsSnapshot() {
    OrganizationPolicyService service = new OrganizationPolicyService(cache);
    given(cache.fetch("ORG")).willReturn(snapshot);
    given(snapshot.approvalFlow()).willReturn(List.of("step1", "step2"));

    assertThat(service.approvalFlow("ORG")).containsExactly("step1", "step2");
  }

  @Test
  @DisplayName("approvalFlow는 스냅샷이 missing일 때 빈 리스트를 반환한다")
  void approvalFlowMissingReturnsEmpty() {
    OrganizationPolicyService service = new OrganizationPolicyService(cache);
    given(cache.fetch("ORG"))
        .willReturn(OrganizationPolicyCache.OrganizationPolicySnapshot.missing("ORG"));

    assertThat(service.approvalFlow("ORG")).isEmpty();
  }
}
