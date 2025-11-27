package com.example.draft.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.example.admin.approval.ApprovalLineTemplate;
import com.example.admin.approval.ApprovalLineTemplateRepository;
import com.example.admin.permission.context.AuthContext;
import com.example.common.security.RowScope;
import com.example.draft.application.dto.DraftTemplatePresetRequest;
import com.example.draft.domain.exception.DraftTemplateNotFoundException;
import com.example.draft.application.dto.DraftTemplatePresetResponse;
import com.example.draft.domain.DraftFormTemplate;
import com.example.draft.domain.DraftTemplatePreset;
import com.example.draft.domain.repository.DraftFormTemplateRepository;
import com.example.draft.domain.repository.DraftTemplatePresetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

class TemplateAdminServicePresetTest {

    private TemplateAdminService service;
    private DraftTemplatePresetRepository presetRepository;
    private DraftFormTemplateRepository formTemplateRepository;
    private ApprovalLineTemplateRepository approvalTemplateRepository;
    private AuthContext context;
    private final OffsetDateTime now = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        presetRepository = mock(DraftTemplatePresetRepository.class);
        formTemplateRepository = mock(DraftFormTemplateRepository.class);
        approvalTemplateRepository = mock(ApprovalLineTemplateRepository.class);
        service = new TemplateAdminService(
                approvalTemplateRepository,
                formTemplateRepository,
                presetRepository,
                new ObjectMapper());
        context = AuthContext.of("user", "ORG1", null, null, null, RowScope.ORG);
    }

    @Test
    @DisplayName("프리셋 생성 시 저장소에 persist하고 응답을 반환한다")
    void createPreset() {
        DraftFormTemplate form = DraftFormTemplate.create("폼", "NOTICE", "ORG1", "{}", now);
        ApprovalLineTemplate approval = ApprovalLineTemplate.create("결재", "NOTICE", "ORG1", now);
        given(formTemplateRepository.findByIdAndActiveTrue(form.getId())).willReturn(Optional.of(form));
        given(approvalTemplateRepository.findByIdAndActiveTrue(approval.getId())).willReturn(Optional.of(approval));
        ArgumentCaptor<DraftTemplatePreset> saved = ArgumentCaptor.forClass(DraftTemplatePreset.class);
        given(presetRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        DraftTemplatePresetRequest request = new DraftTemplatePresetRequest(
                "사전기안",
                "NOTICE",
                "ORG1",
                "제목",
                "내용",
                form.getId(),
                approval.getId(),
                "{}",
                List.of("작성자"),
                true);

        DraftTemplatePresetResponse response = service.createDraftTemplatePreset(request, context, false);

        verify(presetRepository).save(saved.capture());
        assertThat(saved.getValue().getBusinessFeatureCode()).isEqualTo("NOTICE");
        assertThat(response.name()).isEqualTo("사전기안");
        assertThat(response.defaultApprovalTemplateId()).isEqualTo(approval.getId());
        assertThat(response.formTemplateId()).isEqualTo(form.getId());
    }

    @Test
    @DisplayName("프리셋 업데이트 시 필드와 버전을 갱신한다")
    void updatePreset() {
        DraftFormTemplate form = DraftFormTemplate.create("폼", "NOTICE", "ORG1", "{}", now);
        DraftTemplatePreset preset = DraftTemplatePreset.create("사전기안", "NOTICE", "ORG1",
                "제목", "내용", form, null, "{}", "[]", true, now);
        given(presetRepository.findById(preset.getId())).willReturn(Optional.of(preset));
        given(formTemplateRepository.findByIdAndActiveTrue(form.getId())).willReturn(Optional.of(form));
        given(presetRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        DraftTemplatePresetRequest request = new DraftTemplatePresetRequest(
                "새이름",
                "NOTICE",
                "ORG1",
                "새제목",
                "새내용",
                form.getId(),
                null,
                "{\"field\":true}",
                List.of("작성자"),
                false);

        DraftTemplatePresetResponse response = service.updateDraftTemplatePreset(preset.getId(), request, context, false);

        assertThat(response.name()).isEqualTo("새이름");
        assertThat(response.active()).isFalse();
        assertThat(response.version()).isEqualTo(2);
    }

    @Test
    @DisplayName("프리셋 목록은 org/global을 합쳐 반환한다")
    void listPresets() {
        DraftFormTemplate form = DraftFormTemplate.create("폼", "NOTICE", "ORG1", "{}", now);
        DraftTemplatePreset orgPreset = DraftTemplatePreset.create("ORG", "NOTICE", "ORG1",
                "제목", "내용", form, null, "{}", "[]", true, now);
        DraftTemplatePreset globalPreset = DraftTemplatePreset.create("GLOBAL", "NOTICE", null,
                "제목", "내용", form, null, "{}", "[]", true, now);
        given(presetRepository.findAll()).willReturn(List.of(orgPreset, globalPreset));

        var responses = service.listDraftTemplatePresets("NOTICE", null, true, context, true);

        assertThat(responses).hasSize(2);
        assertThat(responses).anyMatch(r -> "ORG".equals(r.name()));
        assertThat(responses).anyMatch(r -> "GLOBAL".equals(r.name()));
    }

    @Test
    @DisplayName("audit=false이면 자신의 조직 프리셋만 activeOnly=true로 반환하고 businessType=null 허용")
    void listPresetsFiltersByOrgWhenNotAudit() {
        DraftFormTemplate form = DraftFormTemplate.create("폼", "NOTICE", "ORG1", "{}", now);
        DraftTemplatePreset myPreset = DraftTemplatePreset.create("MY", "NOTICE", "ORG1",
                "제목", "내용", form, null, "{}", "[]", true, now);
        DraftTemplatePreset otherPreset = DraftTemplatePreset.create("OTHER", "NOTICE", "ORG2",
                "제목", "내용", form, null, "{}", "[]", true, now);
        DraftTemplatePreset inactiveGlobal = DraftTemplatePreset.create("INACTIVE", "NOTICE", null,
                "제목", "내용", form, null, "{}", "[]", false, now);
        given(presetRepository.findAll()).willReturn(List.of(myPreset, otherPreset, inactiveGlobal));

        var responses = service.listDraftTemplatePresets(null, null, true, context, false);

        assertThat(responses).extracting(DraftTemplatePresetResponse::name).containsExactly("MY");
    }

    @Test
    @DisplayName("activeOnly=false이면 비활성 글로벌 프리셋도 반환된다")
    void listPresetsIncludesInactiveWhenRequested() {
        DraftFormTemplate form = DraftFormTemplate.create("폼", "NOTICE", null, "{}", now);
        DraftTemplatePreset inactiveGlobal = DraftTemplatePreset.create("INACTIVE", "NOTICE", null,
                "제목", "내용", form, null, "{}", "[]", false, now);
        given(presetRepository.findAll()).willReturn(List.of(inactiveGlobal));

        var responses = service.listDraftTemplatePresets("NOTICE", null, false, context, true);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().active()).isFalse();
    }

    @Test
    @DisplayName("폼 템플릿을 찾지 못하면 DraftTemplateNotFoundException을 던진다")
    void createPresetFormMissingThrows() {
        DraftTemplatePresetRequest request = new DraftTemplatePresetRequest(
                "사전기안", "NOTICE", "ORG1", "제목", "내용",
                UUID.randomUUID(), null, "{}", List.of(), true);

        assertThatThrownBy(() -> service.createDraftTemplatePreset(request, context, false))
                .isInstanceOf(DraftTemplateNotFoundException.class);
    }

    @Test
    @DisplayName("업데이트 대상 프리셋이 없으면 예외를 던진다")
    void updatePresetMissingThrows() {
        UUID presetId = UUID.randomUUID();
        DraftTemplatePresetRequest request = new DraftTemplatePresetRequest(
                "사전기안", "NOTICE", "ORG1", "제목", "내용",
                UUID.randomUUID(), null, "{}", List.of(), true);

        assertThatThrownBy(() -> service.updateDraftTemplatePreset(presetId, request, context, true))
                .isInstanceOf(DraftTemplateNotFoundException.class);
    }
}
