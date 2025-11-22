package com.example.draft.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.draft.domain.ApprovalGroup;
import com.example.draft.domain.ApprovalGroupMember;
import com.example.draft.domain.ApprovalTemplateStep;
import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftApprovalStep;
import com.example.draft.domain.repository.ApprovalGroupMemberRepository;
import com.example.draft.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.DraftReferenceRepository;

class DraftNotificationServiceIntegrationTest {

    @Test
    @DisplayName("승인 단계 진행 후 notify 호출 시 creator/actor/참조/다음결재자가 모두 수신자에 포함된다")
    void notifyAfterProgressIncludesAllRecipients() {
        DraftNotificationServiceTest.DraftNotificationPublisherStub publisher = new DraftNotificationServiceTest.DraftNotificationPublisherStub();
        DraftReferenceRepository refRepo = mock(DraftReferenceRepository.class);
        ApprovalGroupRepository groupRepo = mock(ApprovalGroupRepository.class);
        ApprovalGroupMemberRepository memberRepo = mock(ApprovalGroupMemberRepository.class);
        DraftNotificationService svc = new DraftNotificationService(publisher, refRepo, groupRepo, memberRepo);

        Draft draft = Draft.create("t", "c", "F", "ORG", "TPL", "creator", OffsetDateTime.now());
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(new ApprovalTemplateStep(null, 1, "GRP", "desc"));
        draft.addApprovalStep(step);

        // 진행된 단계 시뮬레이션: actor가 승인했지만 다음 결재자는 대기(WAITING)로 가정
        // approve 호출이 state를 COMPLETED로 바꾸므로 다음 결재자 분기는 기존 WAITING 대상만 찾으므로 여기서는 WAITING 유지

        com.example.draft.domain.DraftReference ref = com.example.draft.domain.DraftReference.create(
                "ref-user",
                "ORG",
                "creator",
                OffsetDateTime.now()
        );
        given(refRepo.findByDraftIdAndActiveTrue(any())).willReturn(List.of(ref));
        given(groupRepo.findByGroupCode("GRP")).willReturn(Optional.of(ApprovalGroup.create("GRP", "n", null, "ORG", null, OffsetDateTime.now())));
        given(memberRepo.findByApprovalGroupIdAndActiveTrue(any())).willReturn(List.of(
                ApprovalGroupMember.create("next", "ORG", null, OffsetDateTime.now())
        ));

        UUID stepId = step.getId();
        svc.notify("ACTION", draft, "actor", stepId, null, null, OffsetDateTime.now());

        assertThat(publisher.lastPayload.recipients()).containsExactlyInAnyOrder("creator", "actor", "ref-user", "next");
        assertThat(publisher.lastPayload.stepId()).isEqualTo(stepId);
    }
}
