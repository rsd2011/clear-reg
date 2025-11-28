package com.example.draft.application.notification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.auth.domain.UserAccount;
import com.example.auth.domain.UserAccountRepository;
import com.example.admin.permission.domain.PermissionGroup;
import com.example.admin.permission.repository.PermissionGroupRepository;
import com.example.draft.domain.Draft;
import com.example.draft.domain.DraftApprovalState;
import com.example.draft.domain.repository.DraftReferenceRepository;

@Service
public class DraftNotificationService {

    private final DraftNotificationPublisher publisher;
    private final DraftReferenceRepository referenceRepository;
    private final PermissionGroupRepository permissionGroupRepository;
    private final UserAccountRepository userAccountRepository;

    public DraftNotificationService(DraftNotificationPublisher publisher,
                                    DraftReferenceRepository referenceRepository,
                                    PermissionGroupRepository permissionGroupRepository,
                                    UserAccountRepository userAccountRepository) {
        this.publisher = publisher;
        this.referenceRepository = referenceRepository;
        this.permissionGroupRepository = permissionGroupRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    public void notify(String action,
                       Draft draft,
                       String actor,
                       UUID stepId,
                       String delegatedTo,
                       String comment,
                       OffsetDateTime occurredAt) {
        List<String> recipients = resolveRecipients(draft, actor, delegatedTo);
        DraftNotificationPayload payload = new DraftNotificationPayload(
                draft.getId(),
                action,
                actor,
                draft.getCreatedBy(),
                draft.getOrganizationCode(),
                draft.getBusinessFeatureCode(),
                stepId,
                delegatedTo,
                comment,
                occurredAt,
                recipients
        );
        publisher.publish(payload);
    }

    private List<String> resolveRecipients(Draft draft, String actor, String delegatedTo) {
        List<String> recipients = new ArrayList<>();
        recipients.add(draft.getCreatedBy());
        if (actor != null) {
            recipients.add(actor);
        }
        if (delegatedTo != null) {
            recipients.add(delegatedTo);
        }
        referenceRepository.findByDraftIdAndActiveTrue(draft.getId())
                .forEach(ref -> recipients.add(ref.getReferencedUserId()));
        resolveNextApprovers(draft).forEach(recipients::add);
        return recipients.stream().distinct().toList();
    }

    private List<String> resolveNextApprovers(Draft draft) {
        return draft.getApprovalSteps().stream()
                .filter(step -> step.getState() == DraftApprovalState.WAITING)
                .findFirst()
                .map(step -> {
                    List<PermissionGroup> groups =
                            permissionGroupRepository.findByApprovalGroupCode(step.getApprovalGroupCode());

                    if (groups.isEmpty()) {
                        return List.<String>of();
                    }

                    List<String> groupCodes = groups.stream()
                            .map(PermissionGroup::getCode)
                            .toList();

                    return userAccountRepository.findByPermissionGroupCodeIn(groupCodes)
                            .stream()
                            .map(UserAccount::getUsername)
                            .toList();
                })
                .orElse(List.of());
    }
}
