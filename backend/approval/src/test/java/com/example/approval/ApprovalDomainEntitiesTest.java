package com.example.approval;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;

import com.example.admin.approval.domain.ApprovalGroup;
import com.example.admin.approval.domain.ApprovalLineTemplate;
import com.example.admin.approval.domain.ApprovalTemplateStep;
import org.junit.jupiter.api.Test;

class ApprovalDomainEntitiesTest {

    private static final OffsetDateTime NOW = OffsetDateTime.now();

    @Test
    void approvalGroupCreatesAndRenames() {
        ApprovalGroup group = ApprovalGroup.create("CODE", "이름", "desc", 0, NOW);
        group.rename("새 이름", "새 desc", NOW.plusSeconds(1));

        assertThat(group.getName()).isEqualTo("새 이름");
        assertThat(group.getUpdatedAt()).isAfter(NOW);
        assertThat(group.getDescription()).isEqualTo("새 desc");
    }

    @Test
    void approvalLineTemplateCreatesAndAddsSteps() {
        ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", 0, null, NOW);
        assertThat(template.getName()).isEqualTo("템플릿");
        assertThat(template.isActive()).isTrue();

        ApprovalGroup group1 = ApprovalGroup.create("GRP1", "그룹1", "desc", 1, NOW);
        ApprovalGroup group2 = ApprovalGroup.create("GRP2", "그룹2", "desc", 2, NOW);

        template.addStep(1, group1);
        template.addStep(2, group2);
        assertThat(template.getSteps()).hasSize(2);

        ApprovalGroup group3 = ApprovalGroup.create("GRP3", "그룹3", "desc", 3, NOW);
        var newSteps = List.of(new ApprovalTemplateStep(template, 3, group3));
        template.replaceSteps(newSteps);
        assertThat(template.getSteps()).hasSize(1);
        assertThat(template.getSteps().getFirst().getApprovalGroup().getGroupCode()).isEqualTo("GRP3");
    }

    @Test
    void approvalLineTemplateRenameUpdatesFlags() {
        ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", 0, null, NOW);
        template.rename("새템플릿", 1, "설명", false, NOW.plusSeconds(5));

        assertThat(template.getName()).isEqualTo("새템플릿");
        assertThat(template.isActive()).isFalse();
        assertThat(template.getUpdatedAt()).isAfter(NOW);
    }

    @Test
    void approvalTemplateStepStoresValues() {
        ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", 0, null, NOW);
        ApprovalGroup group = ApprovalGroup.create("GRP2", "그룹2", "설명", 2, NOW);
        var step = new ApprovalTemplateStep(template, 2, group);
        assertThat(step.getTemplate()).isEqualTo(template);
        assertThat(step.getStepOrder()).isEqualTo(2);
        assertThat(step.getApprovalGroup().getGroupCode()).isEqualTo("GRP2");
    }
}
