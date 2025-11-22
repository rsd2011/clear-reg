package com.example.draft.application.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import com.example.draft.domain.ApprovalGroup;
import com.example.draft.domain.ApprovalGroupMember;
import com.example.draft.domain.ApprovalLineTemplate;
import com.example.draft.domain.ApprovalTemplateStep;
import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftApprovalState;
import com.example.draft.domain.DraftApprovalStep;
import com.example.draft.domain.repository.ApprovalGroupMemberRepository;
import com.example.draft.domain.repository.ApprovalGroupRepository;
import com.example.draft.domain.repository.DraftReferenceRepository;

class DraftNotificationServiceTest {

    @Test
    @DisplayName("notify는 생성자, actor, 다음 결재자를 포함한 수신자 목록을 만든다")
    void notifyAddsRecipients() {
        DraftNotificationPublisherStub publisher = new DraftNotificationPublisherStub();
        DraftReferenceRepository refRepo = mock(DraftReferenceRepository.class);
        ApprovalGroupRepository groupRepo = mock(ApprovalGroupRepository.class);
        ApprovalGroupMemberRepository memberRepo = mock(ApprovalGroupMemberRepository.class);

        DraftNotificationService svc = new DraftNotificationService(publisher, refRepo, groupRepo, memberRepo);

        Draft draft = Draft.create("title", "content", "FEATURE", "ORG", "TPL", "creator", OffsetDateTime.now());
        ApprovalTemplateStep templateStep = new ApprovalTemplateStep(null, 1, "GRP", "");
        DraftApprovalStep step = DraftApprovalStep.fromTemplate(templateStep);
        draft.addApprovalStep(step);

        given(groupRepo.findByGroupCode("GRP")).willReturn(Optional.of(ApprovalGroup.create("GRP", "n", null, "ORG", null, OffsetDateTime.now())));
        ApprovalGroupMember member = ApprovalGroupMember.create("user1", "ORG", null, OffsetDateTime.now());
        given(memberRepo.findByApprovalGroupIdAndActiveTrue(any())).willReturn(List.of(member));
        given(refRepo.findByDraftIdAndActiveTrue(any())).willReturn(List.of());

        svc.notify("ACTION", draft, "actor", step.getId(), null, null, OffsetDateTime.now());

        assertThat(publisher.lastPayload.recipients()).contains("creator", "actor", "user1");
    }

    @Test
    @DisplayName("Kafka 퍼블리셔는 JSON을 직렬화해 send를 호출한다")
    void kafkaPublisherSends() {
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        KafkaDraftNotificationPublisher publisher = new KafkaDraftNotificationPublisher(kafka);
        DraftNotificationPayload payload = new DraftNotificationPayload(UUID.randomUUID(), "A", "actor", "creator", "ORG", "FEATURE", null, null,
                null, null, List.of("x"));
        publisher.publish(payload);
        verify(kafka).send(any(), any(), any());
    }

    static final class DraftNotificationPublisherStub implements DraftNotificationPublisher {
        DraftNotificationPayload lastPayload;
        @Override public void publish(DraftNotificationPayload payload) { this.lastPayload = payload; }
    }
}
