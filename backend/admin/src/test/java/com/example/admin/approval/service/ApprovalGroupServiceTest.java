package com.example.admin.approval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.admin.approval.domain.ApprovalGroup;
import com.example.admin.approval.exception.ApprovalGroupNotFoundException;
import com.example.admin.approval.repository.ApprovalGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.example.admin.approval.dto.DisplayOrderUpdateRequest;
import com.example.admin.approval.dto.ApprovalGroupRequest;
import com.example.admin.approval.dto.ApprovalGroupResponse;
import com.example.admin.approval.dto.ApprovalGroupSummaryResponse;
import com.example.admin.approval.dto.ApprovalGroupUpdateRequest;
import com.example.admin.permission.context.AuthContext;
import com.example.common.security.RowScope;

class ApprovalGroupServiceTest {

    private ApprovalGroupRepository groupRepo;
    private ApprovalGroupService service;

    @BeforeEach
    void setUp() {
        groupRepo = mock(ApprovalGroupRepository.class);
        service = new ApprovalGroupService(groupRepo);
    }

    @Nested
    @DisplayName("createApprovalGroup")
    class CreateApprovalGroup {

        @Test
        @DisplayName("approvalGroup 생성 성공 시 저장된 엔티티를 반환한다")
        void createApprovalGroupSuccess() {
            given(groupRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));

            ApprovalGroupRequest req = new ApprovalGroupRequest("G1", "name", "desc", 10);
            AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, List.of());

            ApprovalGroupResponse res = service.createApprovalGroup(req, ctx, false);

            assertThat(res.groupCode()).isEqualTo("G1");
            assertThat(res.name()).isEqualTo("name");
            assertThat(res.displayOrder()).isEqualTo(10);
        }

        @Test
        @DisplayName("Given: displayOrder가 null / When: 생성 / Then: 0으로 기본값 설정")
        void createWithNullDisplayOrder() {
            given(groupRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));

            ApprovalGroupRequest req = new ApprovalGroupRequest("G1", "name", "desc", null);
            AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, List.of());

            ApprovalGroupResponse res = service.createApprovalGroup(req, ctx, false);

            assertThat(res.displayOrder()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("updateApprovalGroup")
    class UpdateApprovalGroup {

        @Test
        @DisplayName("동일 조직이면 updateApprovalGroup이 성공적으로 값을 갱신한다")
        void updateApprovalGroupSuccess() {
            ApprovalGroup group = ApprovalGroup.create("G1", "old", "desc", 0, OffsetDateTime.now());
            UUID id = UUID.fromString("00000000-0000-0000-0000-000000000021");
            given(groupRepo.findById(id)).willReturn(Optional.of(group));
            given(groupRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));

            ApprovalGroupUpdateRequest req = new ApprovalGroupUpdateRequest("newName", "newDesc", 10);
            AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, List.of());

            ApprovalGroupResponse res = service.updateApprovalGroup(id, req, ctx, false);

            assertThat(res.name()).isEqualTo("newName");
            assertThat(res.displayOrder()).isEqualTo(10);
        }

        @Test
        @DisplayName("존재하지 않는 그룹 업데이트 시 ApprovalGroupNotFoundException 발생")
        void updateNotFoundGroup() {
            UUID id = UUID.randomUUID();
            given(groupRepo.findById(id)).willReturn(Optional.empty());

            ApprovalGroupUpdateRequest req = new ApprovalGroupUpdateRequest("newName", "newDesc", 10);
            AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, List.of());

            assertThatThrownBy(() -> service.updateApprovalGroup(id, req, ctx, false))
                    .isInstanceOf(ApprovalGroupNotFoundException.class);
        }

        @Test
        @DisplayName("Given: displayOrder가 null / When: 업데이트 / Then: 순서 변경하지 않음")
        void updateWithNullDisplayOrder() {
            ApprovalGroup group = ApprovalGroup.create("G1", "old", "desc", 5, OffsetDateTime.now());
            UUID id = UUID.randomUUID();
            given(groupRepo.findById(id)).willReturn(Optional.of(group));

            ApprovalGroupUpdateRequest req = new ApprovalGroupUpdateRequest("newName", "newDesc", null);
            AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, List.of());

            ApprovalGroupResponse res = service.updateApprovalGroup(id, req, ctx, false);

            assertThat(res.name()).isEqualTo("newName");
            assertThat(res.displayOrder()).isEqualTo(5); // 기존 값 유지
        }
    }

    @Nested
    @DisplayName("listApprovalGroups")
    class ListApprovalGroups {

        @Test
        @DisplayName("listApprovalGroups는 모든 그룹을 반환한다")
        void listReturnsAllGroups() {
            ApprovalGroup group1 = ApprovalGroup.create("G1", "name1", null, 0, OffsetDateTime.now());
            ApprovalGroup group2 = ApprovalGroup.create("G2", "name2", null, 10, OffsetDateTime.now());
            given(groupRepo.findAll()).willReturn(List.of(group1, group2));

            AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, List.of());

            List<ApprovalGroupResponse> result = service.listApprovalGroups(null, true, ctx, false);
            assertThat(result).hasSize(2);
            assertThat(result).extracting(ApprovalGroupResponse::groupCode).containsExactlyInAnyOrder("G1", "G2");
        }

        @Test
        @DisplayName("activeOnly가 true면 활성화된 그룹만 반환한다")
        void listReturnsActiveOnly() {
            ApprovalGroup active = ApprovalGroup.create("G1", "name1", null, 0, OffsetDateTime.now());
            ApprovalGroup inactive = ApprovalGroup.create("G2", "name2", null, 10, OffsetDateTime.now());
            inactive.deactivate(OffsetDateTime.now());
            given(groupRepo.findAll()).willReturn(List.of(active, inactive));

            AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, List.of());

            List<ApprovalGroupResponse> result = service.listApprovalGroups(null, true, ctx, false);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).groupCode()).isEqualTo("G1");
        }

        @Test
        @DisplayName("키워드 필터로 검색한다")
        void listWithKeyword() {
            ApprovalGroup group1 = ApprovalGroup.create("G1", "testname", null, 0, OffsetDateTime.now());
            ApprovalGroup group2 = ApprovalGroup.create("G2", "other", null, 10, OffsetDateTime.now());
            given(groupRepo.findAll()).willReturn(List.of(group1, group2));

            AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, List.of());

            List<ApprovalGroupResponse> result = service.listApprovalGroups("test", false, ctx, false);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).groupCode()).isEqualTo("G1");
        }

        @Test
        @DisplayName("Given: groupCode에 키워드 포함 / When: 검색 / Then: 해당 그룹 반환")
        void listWithKeywordInGroupCode() {
            ApprovalGroup group1 = ApprovalGroup.create("TEST_CODE", "name1", "desc1", 0, OffsetDateTime.now());
            ApprovalGroup group2 = ApprovalGroup.create("OTHER", "name2", "desc2", 10, OffsetDateTime.now());
            given(groupRepo.findAll()).willReturn(List.of(group1, group2));

            AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, List.of());

            List<ApprovalGroupResponse> result = service.listApprovalGroups("test", false, ctx, false);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).groupCode()).isEqualTo("TEST_CODE");
        }

        @Test
        @DisplayName("Given: description에 키워드 포함 / When: 검색 / Then: 해당 그룹 반환")
        void listWithKeywordInDescription() {
            ApprovalGroup group1 = ApprovalGroup.create("G1", "name1", "테스트 설명", 0, OffsetDateTime.now());
            ApprovalGroup group2 = ApprovalGroup.create("G2", "name2", "other desc", 10, OffsetDateTime.now());
            given(groupRepo.findAll()).willReturn(List.of(group1, group2));

            AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, List.of());

            List<ApprovalGroupResponse> result = service.listApprovalGroups("테스트", false, ctx, false);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).groupCode()).isEqualTo("G1");
        }

        @Test
        @DisplayName("Given: description이 null / When: 키워드 검색 / Then: description 매칭 무시")
        void listWithNullDescription() {
            ApprovalGroup group1 = ApprovalGroup.create("G1", "name1", null, 0, OffsetDateTime.now());
            ApprovalGroup group2 = ApprovalGroup.create("KEYWORD_CODE", "name2", null, 10, OffsetDateTime.now());
            given(groupRepo.findAll()).willReturn(List.of(group1, group2));

            AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, List.of());

            List<ApprovalGroupResponse> result = service.listApprovalGroups("KEYWORD", false, ctx, false);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).groupCode()).isEqualTo("KEYWORD_CODE");
        }

        @Test
        @DisplayName("Given: 빈 키워드 / When: 검색 / Then: 모든 그룹 반환")
        void listWithBlankKeyword() {
            ApprovalGroup group1 = ApprovalGroup.create("G1", "name1", null, 0, OffsetDateTime.now());
            ApprovalGroup group2 = ApprovalGroup.create("G2", "name2", null, 10, OffsetDateTime.now());
            given(groupRepo.findAll()).willReturn(List.of(group1, group2));

            AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, List.of());

            List<ApprovalGroupResponse> result = service.listApprovalGroups("   ", false, ctx, false);
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Given: 동일 순서 그룹들 / When: 목록 조회 / Then: 이름순 정렬")
        void listSortsByNameWhenSameOrder() {
            ApprovalGroup groupA = ApprovalGroup.create("GA", "Zebra", null, 0, OffsetDateTime.now());
            ApprovalGroup groupB = ApprovalGroup.create("GB", "Alpha", null, 0, OffsetDateTime.now());
            given(groupRepo.findAll()).willReturn(List.of(groupA, groupB));

            AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, List.of());

            List<ApprovalGroupResponse> result = service.listApprovalGroups(null, false, ctx, false);
            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("Alpha");
            assertThat(result.get(1).name()).isEqualTo("Zebra");
        }
    }

    @Nested
    @DisplayName("deleteApprovalGroup")
    class DeleteApprovalGroup {

        @Test
        @DisplayName("존재하는 그룹 삭제 시 soft delete 수행")
        void deleteSuccess() {
            ApprovalGroup group = ApprovalGroup.create("G1", "name", null, 0, OffsetDateTime.now());
            UUID id = UUID.randomUUID();
            given(groupRepo.findById(id)).willReturn(Optional.of(group));

            AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, List.of());

            service.deleteApprovalGroup(id, ctx, false);

            assertThat(group.isActive()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 그룹 삭제 시 ApprovalGroupNotFoundException 발생")
        void deleteNotFound() {
            UUID id = UUID.randomUUID();
            given(groupRepo.findById(id)).willReturn(Optional.empty());

            AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, List.of());

            assertThatThrownBy(() -> service.deleteApprovalGroup(id, ctx, false))
                    .isInstanceOf(ApprovalGroupNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("activateApprovalGroup")
    class ActivateApprovalGroup {

        @Test
        @DisplayName("비활성 그룹 활성화 성공")
        void activateSuccess() {
            ApprovalGroup group = ApprovalGroup.create("G1", "name", null, 0, OffsetDateTime.now());
            group.deactivate(OffsetDateTime.now());
            UUID id = UUID.randomUUID();
            given(groupRepo.findById(id)).willReturn(Optional.of(group));

            AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, List.of());

            ApprovalGroupResponse res = service.activateApprovalGroup(id, ctx, false);

            assertThat(res.active()).isTrue();
        }
    }

    @Nested
    @DisplayName("existsGroupCode")
    class ExistsGroupCode {

        @Test
        @DisplayName("코드가 존재하면 true 반환")
        void existsReturnsTrue() {
            given(groupRepo.existsByGroupCode("G1")).willReturn(true);

            assertThat(service.existsGroupCode("G1")).isTrue();
        }

        @Test
        @DisplayName("코드가 존재하지 않으면 false 반환")
        void existsReturnsFalse() {
            given(groupRepo.existsByGroupCode("G1")).willReturn(false);

            assertThat(service.existsGroupCode("G1")).isFalse();
        }
    }

    @Nested
    @DisplayName("updateApprovalGroupDisplayOrders")
    class UpdateDisplayOrders {

        @Test
        @DisplayName("표시순서 일괄 업데이트 성공")
        void updateDisplayOrdersSuccess() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            ApprovalGroup group1 = ApprovalGroup.create("G1", "name1", null, 0, OffsetDateTime.now());
            ApprovalGroup group2 = ApprovalGroup.create("G2", "name2", null, 10, OffsetDateTime.now());

            given(groupRepo.findById(id1)).willReturn(Optional.of(group1));
            given(groupRepo.findById(id2)).willReturn(Optional.of(group2));

            DisplayOrderUpdateRequest req = new DisplayOrderUpdateRequest(List.of(
                    new DisplayOrderUpdateRequest.DisplayOrderItem(id1, 5),
                    new DisplayOrderUpdateRequest.DisplayOrderItem(id2, 15)));
            AuthContext ctx = AuthContext.of("u", "ORG1", null, null, null, List.of());

            List<ApprovalGroupResponse> result = service.updateApprovalGroupDisplayOrders(req, ctx, false);

            assertThat(result).hasSize(2);
            assertThat(result).anyMatch(r -> r.groupCode().equals("G1") && r.displayOrder() == 5);
            assertThat(result).anyMatch(r -> r.groupCode().equals("G2") && r.displayOrder() == 15);
        }
    }

    @Nested
    @DisplayName("getApprovalGroup")
    class GetApprovalGroup {

        @Test
        @DisplayName("ID로 그룹 조회 성공")
        void getSuccess() {
            UUID id = UUID.randomUUID();
            ApprovalGroup group = ApprovalGroup.create("G1", "name", null, 0, OffsetDateTime.now());
            given(groupRepo.findById(id)).willReturn(Optional.of(group));

            ApprovalGroupResponse res = service.getApprovalGroup(id);

            assertThat(res.groupCode()).isEqualTo("G1");
        }

        @Test
        @DisplayName("존재하지 않는 그룹 조회 시 ApprovalGroupNotFoundException 발생")
        void getNotFound() {
            UUID id = UUID.randomUUID();
            given(groupRepo.findById(id)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getApprovalGroup(id))
                    .isInstanceOf(ApprovalGroupNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listGroupSummary")
    class ListGroupSummary {

        @Test
        @DisplayName("Given: 승인그룹이 존재할 때 / When: listGroupSummary 호출 / Then: 요약 정보만 반환")
        void listGroupSummaryReturnsOnlySummary() {
            ApprovalGroup g1 = ApprovalGroup.create("TEAM_LEADER", "팀장", null, 1, OffsetDateTime.now());
            ApprovalGroup g2 = ApprovalGroup.create("DEPT_HEAD", "부서장", null, 2, OffsetDateTime.now());
            given(groupRepo.findAll()).willReturn(List.of(g1, g2));

            List<ApprovalGroupSummaryResponse> result = service.listGroupSummary(true);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ApprovalGroupSummaryResponse::groupCode)
                    .containsExactlyInAnyOrder("TEAM_LEADER", "DEPT_HEAD");
        }

        @Test
        @DisplayName("Given: 비활성 그룹 포함 / When: activeOnly=true / Then: 활성 그룹만 반환")
        void listGroupSummaryFiltersInactive() {
            ApprovalGroup active = ApprovalGroup.create("ACTIVE", "활성", null, 1, OffsetDateTime.now());
            ApprovalGroup inactive = ApprovalGroup.create("INACTIVE", "비활성", null, 2, OffsetDateTime.now());
            inactive.deactivate(OffsetDateTime.now());
            given(groupRepo.findAll()).willReturn(List.of(active, inactive));

            List<ApprovalGroupSummaryResponse> result = service.listGroupSummary(true);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).groupCode()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("Given: 비활성 그룹 포함 / When: activeOnly=false / Then: 모든 그룹 반환")
        void listGroupSummaryReturnsAllGroups() {
            ApprovalGroup active = ApprovalGroup.create("ACTIVE", "활성", null, 1, OffsetDateTime.now());
            ApprovalGroup inactive = ApprovalGroup.create("INACTIVE", "비활성", null, 2, OffsetDateTime.now());
            inactive.deactivate(OffsetDateTime.now());
            given(groupRepo.findAll()).willReturn(List.of(active, inactive));

            List<ApprovalGroupSummaryResponse> result = service.listGroupSummary(false);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ApprovalGroupSummaryResponse::groupCode)
                    .containsExactlyInAnyOrder("ACTIVE", "INACTIVE");
        }
    }
}
