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
import com.example.admin.draft.dto.DraftFormTemplateRequest;
import com.example.admin.draft.dto.DraftFormTemplateResponse;
import com.example.admin.draft.exception.DraftTemplateNotFoundException;
import com.example.admin.draft.repository.DraftFormTemplateRepository;
import com.example.admin.draft.repository.DraftFormTemplateRootRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DraftFormTemplateServiceDraftVersionTest {

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
    @DisplayName("listDraftFormTemplateRoots")
    class ListDraftFormTemplateRoots {

        @Test
        @DisplayName("workType이 null이면 모든 활성 Root를 반환한다")
        void returnsAllActiveWhenWorkTypeIsNull() {
            OffsetDateTime now = OffsetDateTime.now();
            DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);
            when(draftFormTemplateRootRepository.findAllActive()).thenReturn(List.of(root));

            List<DraftFormTemplateRoot> result = service.listDraftFormTemplateRoots(null, true);

            assertThat(result).hasSize(1);
            verify(draftFormTemplateRootRepository).findAllActive();
        }

        @Test
        @DisplayName("workType이 지정되면 해당 유형의 Root만 반환한다")
        void returnsFilteredByWorkType() {
            OffsetDateTime now = OffsetDateTime.now();
            DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);
            when(draftFormTemplateRootRepository.findByWorkTypeAndActive(WorkType.GENERAL))
                    .thenReturn(List.of(root));

            List<DraftFormTemplateRoot> result = service.listDraftFormTemplateRoots(WorkType.GENERAL, true);

            assertThat(result).hasSize(1);
            verify(draftFormTemplateRootRepository).findByWorkTypeAndActive(WorkType.GENERAL);
        }
    }

    @Nested
    @DisplayName("findRootByTemplateCode")
    class FindRootByTemplateCode {

        @Test
        @DisplayName("템플릿 코드로 Root를 찾으면 반환한다")
        void returnsRootWhenFound() {
            OffsetDateTime now = OffsetDateTime.now();
            DraftFormTemplateRoot root = DraftFormTemplateRoot.createWithCode("CODE-001", now);
            when(draftFormTemplateRootRepository.findByTemplateCode("CODE-001"))
                    .thenReturn(Optional.of(root));

            DraftFormTemplateRoot result = service.findRootByTemplateCode("CODE-001");

            assertThat(result.getTemplateCode()).isEqualTo("CODE-001");
        }

        @Test
        @DisplayName("템플릿 코드로 Root를 찾지 못하면 예외를 던진다")
        void throwsWhenNotFound() {
            when(draftFormTemplateRootRepository.findByTemplateCode("NOT-EXIST"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findRootByTemplateCode("NOT-EXIST"))
                    .isInstanceOf(DraftTemplateNotFoundException.class)
                    .hasMessageContaining("기안 양식 템플릿을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("createDraft")
    class CreateDraft {

        @Test
        @DisplayName("Root에 초안이 없으면 새 초안을 생성한다")
        void createsDraftWhenNoDraftExists() {
            OffsetDateTime now = OffsetDateTime.now();
            UUID rootId = UUID.randomUUID();
            DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

            // Root에 현재 버전 설정 (초안 테스트를 위해)
            DraftFormTemplate current = DraftFormTemplate.create(
                    root, 1, "Current", WorkType.GENERAL, "{}", true,
                    ChangeAction.CREATE, null, "user", "User", now);
            root.activateNewVersion(current, now);

            when(draftFormTemplateRootRepository.findById(rootId)).thenReturn(Optional.of(root));
            when(draftFormTemplateRepository.findMaxVersionByRoot(root)).thenReturn(1);
            when(draftFormTemplateRepository.save(any(DraftFormTemplate.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            DraftFormTemplateRequest request = new DraftFormTemplateRequest(
                    "Draft Version", WorkType.HR_UPDATE, "{}", true, "초안 생성");

            DraftFormTemplateResponse result = service.createDraft(rootId, request, createContext());

            assertThat(result.name()).isEqualTo("Draft Version");
            assertThat(root.hasDraft()).isTrue();
        }

        @Test
        @DisplayName("Root에 이미 초안이 있으면 예외를 던진다")
        void throwsWhenDraftAlreadyExists() {
            OffsetDateTime now = OffsetDateTime.now();
            UUID rootId = UUID.randomUUID();
            DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

            // 초안 설정
            DraftFormTemplate draft = DraftFormTemplate.createDraft(
                    root, 1, "Existing Draft", WorkType.GENERAL, "{}", true,
                    null, "user", "User", now);
            root.setDraftVersion(draft);

            when(draftFormTemplateRootRepository.findById(rootId)).thenReturn(Optional.of(root));

            DraftFormTemplateRequest request = new DraftFormTemplateRequest(
                    "New Draft", WorkType.HR_UPDATE, "{}", true, null);

            assertThatThrownBy(() -> service.createDraft(rootId, request, createContext()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 초안 버전이 존재합니다");
        }

        @Test
        @DisplayName("Root를 찾을 수 없으면 예외를 던진다")
        void throwsWhenRootNotFound() {
            UUID rootId = UUID.randomUUID();
            when(draftFormTemplateRootRepository.findById(rootId)).thenReturn(Optional.empty());

            DraftFormTemplateRequest request = new DraftFormTemplateRequest(
                    "Draft", WorkType.GENERAL, "{}", true, null);

            assertThatThrownBy(() -> service.createDraft(rootId, request, createContext()))
                    .isInstanceOf(DraftTemplateNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("publishDraft")
    class PublishDraft {

        @Test
        @DisplayName("초안을 게시하면 현재 버전이 된다")
        void publishesDraftAsCurrentVersion() {
            OffsetDateTime now = OffsetDateTime.now();
            UUID rootId = UUID.randomUUID();
            DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

            // 현재 버전 설정
            DraftFormTemplate current = DraftFormTemplate.create(
                    root, 1, "Current", WorkType.GENERAL, "{}", true,
                    ChangeAction.CREATE, null, "user", "User", now);
            root.activateNewVersion(current, now);

            // 초안 설정
            DraftFormTemplate draft = DraftFormTemplate.createDraft(
                    root, 2, "Draft", WorkType.HR_UPDATE, "{}", true,
                    null, "user", "User", now);
            root.setDraftVersion(draft);

            when(draftFormTemplateRootRepository.findById(rootId)).thenReturn(Optional.of(root));

            DraftFormTemplateResponse result = service.publishDraft(rootId, createContext());

            assertThat(result.name()).isEqualTo("Draft");
            assertThat(root.getCurrentVersion()).isEqualTo(draft);
            assertThat(root.hasDraft()).isFalse();
        }

        @Test
        @DisplayName("Root를 찾을 수 없으면 예외를 던진다")
        void throwsWhenRootNotFound() {
            UUID rootId = UUID.randomUUID();
            when(draftFormTemplateRootRepository.findById(rootId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.publishDraft(rootId, createContext()))
                    .isInstanceOf(DraftTemplateNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("discardDraft")
    class DiscardDraft {

        @Test
        @DisplayName("초안을 삭제하면 hasDraft가 false가 된다")
        void discardsDraft() {
            OffsetDateTime now = OffsetDateTime.now();
            UUID rootId = UUID.randomUUID();
            DraftFormTemplateRoot root = DraftFormTemplateRoot.create(now);

            // 초안 설정
            DraftFormTemplate draft = DraftFormTemplate.createDraft(
                    root, 1, "Draft", WorkType.GENERAL, "{}", true,
                    null, "user", "User", now);
            root.setDraftVersion(draft);

            when(draftFormTemplateRootRepository.findById(rootId)).thenReturn(Optional.of(root));

            service.discardDraft(rootId, createContext());

            assertThat(root.hasDraft()).isFalse();
        }

        @Test
        @DisplayName("Root를 찾을 수 없으면 예외를 던진다")
        void throwsWhenRootNotFound() {
            UUID rootId = UUID.randomUUID();
            when(draftFormTemplateRootRepository.findById(rootId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.discardDraft(rootId, createContext()))
                    .isInstanceOf(DraftTemplateNotFoundException.class);
        }
    }
}
