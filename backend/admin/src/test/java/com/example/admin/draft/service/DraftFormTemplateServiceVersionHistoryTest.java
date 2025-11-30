package com.example.admin.draft.service;
import com.example.admin.draft.service.DraftFormTemplateService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.admin.draft.domain.DraftFormTemplate;
import com.example.admin.draft.domain.DraftFormTemplateRoot;
import com.example.admin.permission.context.AuthContext;
import com.example.common.orggroup.WorkType;
import com.example.common.version.ChangeAction;
import com.example.admin.draft.dto.DraftFormTemplateResponse;
import com.example.admin.draft.exception.DraftTemplateNotFoundException;
import com.example.admin.draft.repository.DraftFormTemplateRepository;
import com.example.admin.draft.repository.DraftFormTemplateRootRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DraftFormTemplateServiceVersionHistoryTest {

    @Mock
    private DraftFormTemplateRepository draftFormTemplateRepository;

    @Mock
    private DraftFormTemplateRootRepository draftFormTemplateRootRepository;

    @InjectMocks
    private DraftFormTemplateService service;

    private AuthContext createContext() {
        AuthContext context = mock(AuthContext.class);
        when(context.username()).thenReturn("testuser");
        return context;
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("ID로 템플릿을 찾으면 응답을 반환한다")
        void returnsTemplateWhenFound() {
            OffsetDateTime now = OffsetDateTime.now();
            UUID templateId = UUID.randomUUID();
            DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);
            DraftFormTemplate template = DraftFormTemplate.create(
                    root, 1, "Test Template", WorkType.GENERAL, "{}", true,
                    ChangeAction.CREATE, null, "user", "User", now);

            when(draftFormTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));

            DraftFormTemplateResponse result = service.findById(templateId);

            assertThat(result.name()).isEqualTo("Test Template");
            assertThat(result.workType()).isEqualTo(WorkType.GENERAL);
        }

        @Test
        @DisplayName("ID로 템플릿을 찾지 못하면 예외를 던진다")
        void throwsWhenNotFound() {
            UUID templateId = UUID.randomUUID();
            when(draftFormTemplateRepository.findById(templateId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(templateId))
                    .isInstanceOf(DraftTemplateNotFoundException.class)
                    .hasMessageContaining("기안 양식 템플릿을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("getVersionHistory")
    class GetVersionHistory {

        @Test
        @DisplayName("Root ID로 버전 히스토리를 조회하면 버전 목록을 반환한다")
        void returnsVersionHistory() {
            OffsetDateTime now = OffsetDateTime.now();
            UUID rootId = UUID.randomUUID();
            DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

            DraftFormTemplate v1 = DraftFormTemplate.create(
                    root, 1, "Version 1", WorkType.GENERAL, "{}", true,
                    ChangeAction.CREATE, null, "user", "User", now);
            DraftFormTemplate v2 = DraftFormTemplate.create(
                    root, 2, "Version 2", WorkType.GENERAL, "{}", true,
                    ChangeAction.UPDATE, "Updated", "user", "User", now);

            when(draftFormTemplateRootRepository.existsById(rootId)).thenReturn(true);
            when(draftFormTemplateRepository.findAllByRootIdOrderByVersionDesc(rootId))
                    .thenReturn(List.of(v2, v1));

            List<DraftFormTemplateResponse> result = service.getVersionHistory(rootId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("Version 2");
            assertThat(result.get(1).name()).isEqualTo("Version 1");
        }

        @Test
        @DisplayName("Root가 존재하지 않으면 예외를 던진다")
        void throwsWhenRootNotFound() {
            UUID rootId = UUID.randomUUID();
            when(draftFormTemplateRootRepository.existsById(rootId)).thenReturn(false);

            assertThatThrownBy(() -> service.getVersionHistory(rootId))
                    .isInstanceOf(DraftTemplateNotFoundException.class)
                    .hasMessageContaining("기안 양식 템플릿 루트를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("rollbackToVersion")
    class RollbackToVersion {

        @Test
        @DisplayName("특정 버전으로 롤백하면 새 버전이 생성된다")
        void createsNewVersionOnRollback() {
            OffsetDateTime now = OffsetDateTime.now();
            UUID targetVersionId = UUID.randomUUID();
            DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

            DraftFormTemplate v1 = DraftFormTemplate.create(
                    root, 1, "Version 1", WorkType.HR_UPDATE, "{\"field\":\"value\"}", true,
                    ChangeAction.CREATE, null, "user", "User", now);
            root.activateNewVersion(v1, now);

            DraftFormTemplate v2 = DraftFormTemplate.create(
                    root, 2, "Version 2", WorkType.HR_UPDATE, "{}", true,
                    ChangeAction.UPDATE, "Updated", "user", "User", now);
            root.activateNewVersion(v2, now);

            when(draftFormTemplateRepository.findById(targetVersionId)).thenReturn(Optional.of(v1));
            when(draftFormTemplateRepository.findMaxVersionByRoot(root)).thenReturn(2);
            when(draftFormTemplateRepository.save(any(DraftFormTemplate.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            DraftFormTemplateResponse result = service.rollbackToVersion(
                    targetVersionId, "롤백 사유", createContext(), false);

            assertThat(result.name()).isEqualTo("Version 1");
            assertThat(result.version()).isEqualTo(3);
            assertThat(result.changeAction()).isEqualTo(ChangeAction.ROLLBACK);
            verify(draftFormTemplateRepository).save(any(DraftFormTemplate.class));
        }

        @Test
        @DisplayName("초안이 있고 덮어쓰기가 false면 예외를 던진다")
        void throwsWhenDraftExistsAndNoOverwrite() {
            OffsetDateTime now = OffsetDateTime.now();
            UUID targetVersionId = UUID.randomUUID();
            DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

            DraftFormTemplate v1 = DraftFormTemplate.create(
                    root, 1, "Version 1", WorkType.GENERAL, "{}", true,
                    ChangeAction.CREATE, null, "user", "User", now);
            root.activateNewVersion(v1, now);

            // 초안 설정
            DraftFormTemplate draft = DraftFormTemplate.createDraft(
                    root, 2, "Draft", WorkType.GENERAL, "{}", true,
                    null, "user", "User", now);
            root.setDraftVersion(draft);

            when(draftFormTemplateRepository.findById(targetVersionId)).thenReturn(Optional.of(v1));

            assertThatThrownBy(() -> service.rollbackToVersion(
                    targetVersionId, null, createContext(), false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 초안 버전이 존재합니다");
        }

        @Test
        @DisplayName("초안이 있어도 덮어쓰기가 true면 롤백을 수행한다")
        void rollbacksWhenDraftExistsAndOverwrite() {
            OffsetDateTime now = OffsetDateTime.now();
            UUID targetVersionId = UUID.randomUUID();
            DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

            DraftFormTemplate v1 = DraftFormTemplate.create(
                    root, 1, "Version 1", WorkType.GENERAL, "{}", true,
                    ChangeAction.CREATE, null, "user", "User", now);
            root.activateNewVersion(v1, now);

            // 초안 설정
            DraftFormTemplate draft = DraftFormTemplate.createDraft(
                    root, 2, "Draft", WorkType.GENERAL, "{}", true,
                    null, "user", "User", now);
            root.setDraftVersion(draft);

            when(draftFormTemplateRepository.findById(targetVersionId)).thenReturn(Optional.of(v1));
            when(draftFormTemplateRepository.findMaxVersionByRoot(root)).thenReturn(2);
            when(draftFormTemplateRepository.save(any(DraftFormTemplate.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            DraftFormTemplateResponse result = service.rollbackToVersion(
                    targetVersionId, "롤백 사유", createContext(), true);

            assertThat(result.name()).isEqualTo("Version 1");
            assertThat(root.hasDraft()).isFalse();
        }

        @Test
        @DisplayName("대상 버전을 찾을 수 없으면 예외를 던진다")
        void throwsWhenTargetVersionNotFound() {
            UUID targetVersionId = UUID.randomUUID();
            when(draftFormTemplateRepository.findById(targetVersionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.rollbackToVersion(
                    targetVersionId, null, createContext(), false))
                    .isInstanceOf(DraftTemplateNotFoundException.class)
                    .hasMessageContaining("롤백 대상 버전을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("deleteTemplate")
    class DeleteTemplate {

        @Test
        @DisplayName("템플릿을 삭제하면 비활성화된 새 버전이 생성된다")
        void createsInactiveVersionOnDelete() {
            OffsetDateTime now = OffsetDateTime.now();
            UUID rootId = UUID.randomUUID();
            DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

            DraftFormTemplate current = DraftFormTemplate.create(
                    root, 1, "Template", WorkType.GENERAL, "{}", true,
                    ChangeAction.CREATE, null, "user", "User", now);
            root.activateNewVersion(current, now);

            when(draftFormTemplateRootRepository.findById(rootId)).thenReturn(Optional.of(root));
            when(draftFormTemplateRepository.findMaxVersionByRoot(root)).thenReturn(1);
            when(draftFormTemplateRepository.save(any(DraftFormTemplate.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.deleteTemplate(rootId, createContext());

            verify(draftFormTemplateRepository).save(any(DraftFormTemplate.class));
            assertThat(root.getCurrentVersion().isActive()).isFalse();
        }

        @Test
        @DisplayName("Root를 찾을 수 없으면 예외를 던진다")
        void throwsWhenRootNotFound() {
            UUID rootId = UUID.randomUUID();
            when(draftFormTemplateRootRepository.findById(rootId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteTemplate(rootId, createContext()))
                    .isInstanceOf(DraftTemplateNotFoundException.class);
        }

        @Test
        @DisplayName("현재 버전이 없으면 예외를 던진다")
        void throwsWhenNoCurrentVersion() {
            OffsetDateTime now = OffsetDateTime.now();
            UUID rootId = UUID.randomUUID();
            DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

            when(draftFormTemplateRootRepository.findById(rootId)).thenReturn(Optional.of(root));

            assertThatThrownBy(() -> service.deleteTemplate(rootId, createContext()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("현재 버전이 없습니다");
        }
    }
}
