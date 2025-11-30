package com.example.admin.permission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.domain.PermissionAssignment;
import com.example.admin.permission.domain.PermissionGroup;
import com.example.admin.permission.domain.PermissionGroupRoot;
import com.example.admin.permission.dto.PermissionAssignmentDto;
import com.example.admin.permission.dto.PermissionGroupHistoryResponse;
import com.example.admin.permission.dto.PermissionGroupRootRequest;
import com.example.admin.permission.dto.PermissionGroupRootResponse;
import com.example.admin.permission.exception.PermissionGroupNotFoundException;
import com.example.admin.permission.repository.PermissionGroupRootRepository;
import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.common.version.ChangeAction;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * PermissionGroupRootService 테스트.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionGroupRootService 테스트")
class PermissionGroupRootServiceTest {

    @Mock
    private PermissionGroupRootRepository rootRepository;

    @Mock
    private PermissionGroupVersioningService versionService;

    @Mock
    private PermissionGroupService permissionGroupService;

    private PermissionGroupRootService service;

    private AuthContext testContext;

    @BeforeEach
    void setUp() {
        service = new PermissionGroupRootService(rootRepository, versionService, permissionGroupService);
        testContext = AuthContext.of("testUser", "TEST_ORG", "ADMIN", FeatureCode.RULE_MANAGE, ActionCode.READ);
    }

    private PermissionGroupRoot createTestGroupRoot(String code, String name, boolean active) {
        OffsetDateTime now = OffsetDateTime.now();
        PermissionGroupRoot root = PermissionGroupRoot.createWithCode(code, now);

        PermissionGroup version = PermissionGroup.create(
                root,
                1,
                name,
                "테스트용 그룹",
                active,
                List.of(new PermissionAssignment(FeatureCode.AUDIT_LOG, ActionCode.READ)),
                List.of("APPROVAL_GRP_1"),
                ChangeAction.CREATE,
                "테스트 생성",
                "SYSTEM",
                "System",
                now);

        root.activateNewVersion(version, now);
        return root;
    }

    @Nested
    @DisplayName("목록 조회")
    class ListTest {

        @Test
        @DisplayName("Given 여러 그룹 존재 When list Then 모든 그룹 반환")
        void givenMultipleGroups_whenList_thenReturnsAll() {
            PermissionGroupRoot root1 = createTestGroupRoot("ADMIN", "Admin Group", true);
            PermissionGroupRoot root2 = createTestGroupRoot("USER", "User Group", true);
            given(rootRepository.findAll()).willReturn(List.of(root1, root2));

            List<PermissionGroupRootResponse> result = service.list(null, false);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Given 활성/비활성 그룹 존재 When list with activeOnly Then 활성 그룹만 반환")
        void givenActiveAndInactive_whenListActiveOnly_thenReturnsActiveOnly() {
            PermissionGroupRoot active = createTestGroupRoot("ACTIVE", "Active Group", true);
            PermissionGroupRoot inactive = createTestGroupRoot("INACTIVE", "Inactive Group", false);
            given(rootRepository.findAll()).willReturn(List.of(active, inactive));

            List<PermissionGroupRootResponse> result = service.list(null, true);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Active Group");
        }

        @Test
        @DisplayName("Given 여러 그룹 When list with keyword Then 필터링된 결과 반환")
        void givenMultipleGroups_whenListWithKeyword_thenReturnsFiltered() {
            PermissionGroupRoot admin = createTestGroupRoot("ADMIN", "Admin Group", true);
            PermissionGroupRoot user = createTestGroupRoot("USER", "User Group", true);
            given(rootRepository.findAll()).willReturn(List.of(admin, user));

            List<PermissionGroupRootResponse> result = service.list("admin", false);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).groupCode()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("Given 여러 그룹 When list with pageable Then 페이지 반환")
        void givenMultipleGroups_whenListWithPageable_thenReturnsPage() {
            PermissionGroupRoot root = createTestGroupRoot("ADMIN", "Admin Group", true);
            Pageable pageable = PageRequest.of(0, 10);
            given(rootRepository.findAll(pageable))
                    .willReturn(new PageImpl<>(List.of(root), pageable, 1));

            Page<PermissionGroupRootResponse> result = service.list(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("단일 조회")
    class GetByIdTest {

        @Test
        @DisplayName("Given 그룹 존재 When getById Then 그룹 반환")
        void givenGroupExists_whenGetById_thenReturnsGroup() {
            UUID id = UUID.randomUUID();
            PermissionGroupRoot root = createTestGroupRoot("ADMIN", "Admin Group", true);
            given(rootRepository.findById(id)).willReturn(Optional.of(root));

            PermissionGroupRootResponse result = service.getById(id);

            assertThat(result.name()).isEqualTo("Admin Group");
        }

        @Test
        @DisplayName("Given 그룹 없음 When getById Then 예외")
        void givenGroupNotExists_whenGetById_thenThrows() {
            UUID id = UUID.randomUUID();
            given(rootRepository.findById(id)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(id))
                    .isInstanceOf(PermissionGroupNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("그룹 코드로 조회")
    class GetByGroupCodeTest {

        @Test
        @DisplayName("Given 그룹 존재 When getByGroupCode Then 그룹 반환")
        void givenGroupExists_whenGetByGroupCode_thenReturnsGroup() {
            PermissionGroupRoot root = createTestGroupRoot("ADMIN", "Admin Group", true);
            given(rootRepository.findByGroupCode("ADMIN")).willReturn(Optional.of(root));

            PermissionGroupRootResponse result = service.getByGroupCode("ADMIN");

            assertThat(result.groupCode()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("Given 그룹 없음 When getByGroupCode Then 예외")
        void givenGroupNotExists_whenGetByGroupCode_thenThrows() {
            given(rootRepository.findByGroupCode("MISSING")).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getByGroupCode("MISSING"))
                    .isInstanceOf(PermissionGroupNotFoundException.class)
                    .hasMessageContaining("권한 그룹을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("그룹 생성")
    class CreateTest {

        @Test
        @DisplayName("Given 요청 정보 When create Then 그룹 생성")
        void givenRequest_whenCreate_thenCreatesGroup() {
            PermissionGroupRootRequest request = new PermissionGroupRootRequest(
                    null, "New Group", "Description", true,
                    List.of(new PermissionAssignmentDto(FeatureCode.AUDIT_LOG, ActionCode.READ)),
                    List.of("GRP1")
            );

            given(rootRepository.save(any(PermissionGroupRoot.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            PermissionGroupRootResponse result = service.create(request, testContext);

            assertThat(result).isNotNull();
            verify(rootRepository).save(any(PermissionGroupRoot.class));
            verify(versionService).createInitialVersion(any(), eq(request), eq(testContext), any());
        }

        @Test
        @DisplayName("Given 그룹 코드 지정 When createWithCode Then 코드로 그룹 생성")
        void givenGroupCode_whenCreateWithCode_thenCreatesWithCode() {
            String groupCode = "CUSTOM_CODE";
            PermissionGroupRootRequest request = new PermissionGroupRootRequest(
                    groupCode, "Custom Group", "Description", true,
                    List.of(), List.of()
            );

            given(rootRepository.existsByGroupCode(groupCode)).willReturn(false);
            given(rootRepository.save(any(PermissionGroupRoot.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            PermissionGroupRootResponse result = service.createWithCode(groupCode, request, testContext);

            assertThat(result.groupCode()).isEqualTo(groupCode);
        }

        @Test
        @DisplayName("Given 중복 그룹 코드 When createWithCode Then 예외")
        void givenDuplicateCode_whenCreateWithCode_thenThrows() {
            String groupCode = "EXISTING";
            PermissionGroupRootRequest request = new PermissionGroupRootRequest(
                    groupCode, "Group", "Description", true, List.of(), List.of()
            );

            given(rootRepository.existsByGroupCode(groupCode)).willReturn(true);

            assertThatThrownBy(() -> service.createWithCode(groupCode, request, testContext))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 존재하는 그룹 코드");
        }
    }

    @Nested
    @DisplayName("그룹 삭제")
    class DeleteTest {

        @Test
        @DisplayName("Given 그룹 존재 When delete Then 비활성화 버전 생성")
        void givenGroupExists_whenDelete_thenCreatesDeleteVersion() {
            UUID id = UUID.randomUUID();
            PermissionGroupRoot root = createTestGroupRoot("ADMIN", "Admin Group", true);
            given(rootRepository.findById(id)).willReturn(Optional.of(root));

            service.delete(id, testContext);

            verify(versionService).createDeleteVersion(eq(root), eq(testContext), any());
        }
    }

    @Nested
    @DisplayName("그룹 활성화")
    class ActivateTest {

        @Test
        @DisplayName("Given 비활성 그룹 When activate Then 활성화 버전 생성")
        void givenInactiveGroup_whenActivate_thenCreatesRestoreVersion() {
            UUID id = UUID.randomUUID();
            PermissionGroupRoot root = createTestGroupRoot("ADMIN", "Admin Group", false);
            given(rootRepository.findById(id)).willReturn(Optional.of(root));

            service.activate(id, testContext);

            verify(versionService).createRestoreVersion(eq(root), eq(testContext), any());
        }
    }

    @Nested
    @DisplayName("이력 조회")
    class GetHistoryTest {

        @Test
        @DisplayName("Given 그룹 존재 When getHistory Then 이력 반환")
        void givenGroupExists_whenGetHistory_thenReturnsHistory() {
            UUID id = UUID.randomUUID();
            List<PermissionGroupHistoryResponse> history = List.of();
            given(versionService.getVersionHistory(id)).willReturn(history);

            List<PermissionGroupHistoryResponse> result = service.getHistory(id);

            assertThat(result).isEqualTo(history);
            verify(versionService).getVersionHistory(id);
        }
    }

    @Nested
    @DisplayName("초안 있는 그룹 목록")
    class ListWithDraftTest {

        @Test
        @DisplayName("Given 초안 있는 그룹 When listWithDraft Then 해당 그룹만 반환")
        void givenGroupsWithDraft_whenListWithDraft_thenReturnsOnlyWithDraft() {
            OffsetDateTime now = OffsetDateTime.now();
            PermissionGroupRoot withDraft = createTestGroupRoot("WITH_DRAFT", "With Draft", true);
            PermissionGroupRoot withoutDraft = createTestGroupRoot("NO_DRAFT", "No Draft", true);

            // withDraft에 드래프트 추가
            PermissionGroup draft = PermissionGroup.createDraft(
                    withDraft, 2, "Draft", "Desc", true,
                    List.of(), List.of(), null, "user", "User", now);
            withDraft.setDraftVersion(draft);

            given(rootRepository.findAll()).willReturn(List.of(withDraft, withoutDraft));

            List<PermissionGroupRootResponse> result = service.listWithDraft();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).groupCode()).isEqualTo("WITH_DRAFT");
            assertThat(result.get(0).hasDraft()).isTrue();
        }
    }
}
