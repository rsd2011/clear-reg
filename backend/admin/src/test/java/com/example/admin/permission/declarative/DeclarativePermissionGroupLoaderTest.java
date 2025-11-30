package com.example.admin.permission.declarative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.example.common.security.ActionCode;
import com.example.common.security.FeatureCode;
import com.example.common.version.ChangeAction;
import com.example.admin.permission.domain.PermissionGroup;
import com.example.admin.permission.domain.PermissionGroupRoot;
import com.example.admin.permission.repository.PermissionGroupRepository;
import com.example.admin.permission.repository.PermissionGroupRootRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.io.DefaultResourceLoader;

/**
 * DeclarativePermissionGroupLoader 테스트.
 *
 * <p>버전관리 적용됨 - PermissionGroupRoot와 PermissionGroup을 함께 관리합니다.
 * RowScope는 RowAccessPolicy로 이관되어 테스트에서 제거되었습니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeclarativePermissionGroupLoader 테스트")
class DeclarativePermissionGroupLoaderTest {

    @Mock
    private PermissionGroupRootRepository rootRepository;

    @Mock
    private PermissionGroupRepository versionRepository;

    @Captor
    private ArgumentCaptor<PermissionGroupRoot> rootCaptor;

    private DeclarativePermissionProperties properties;
    private DeclarativePermissionGroupLoader loader;

    @BeforeEach
    void setUp() {
        properties = new DeclarativePermissionProperties();
        properties.setLocation("classpath:permissions/test-permission-groups.yml");
        loader = new DeclarativePermissionGroupLoader(
                rootRepository,
                versionRepository,
                properties,
                new DefaultResourceLoader(),
                yamlMapper());
    }

    @Nested
    @DisplayName("run 메서드")
    class Run {

        @Test
        @DisplayName("Given YAML 정의 When 로더 실행 Then 권한 그룹이 저장된다")
        void givenYamlDefinitions_whenLoaderRuns_thenPermissionGroupsPersisted() throws Exception {
            // rootRepository.save()가 저장된 엔티티를 반환하도록 stub
            given(rootRepository.save(any(PermissionGroupRoot.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(versionRepository.findMaxVersionByRootId(any())).willReturn(0);

            loader.run(new DefaultApplicationArguments(new String[0]));

            // YAML에 정의된 그룹 수만큼 save 호출됨 (2개 * 2번 = 4회)
            verify(rootRepository, atLeastOnce()).save(rootCaptor.capture());
            List<PermissionGroupRoot> savedRoots = rootCaptor.getAllValues();
            assertThat(savedRoots).hasSizeGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("Given enabled=false When run 실행 Then 아무 동작 없이 반환")
        void givenDisabled_whenRun_thenNoAction() throws Exception {
            properties.setEnabled(false);

            loader.run(new DefaultApplicationArguments(new String[0]));

            verify(rootRepository, never()).save(any());
        }

        @Test
        @DisplayName("Given 존재하지 않는 파일 + failOnMissingFile=true When run 실행 Then 예외 발생")
        void givenMissingFileWithFailFlag_whenRun_thenThrowsException() {
            properties.setLocation("classpath:permissions/non-existent.yml");
            properties.setFailOnMissingFile(true);

            assertThatThrownBy(() -> loader.run(new DefaultApplicationArguments(new String[0])))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("권한 정의 파일을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("Given 존재하지 않는 파일 + failOnMissingFile=false When run 실행 Then 경고만 출력하고 반환")
        void givenMissingFileWithoutFailFlag_whenRun_thenWarnsAndReturns() throws Exception {
            properties.setLocation("classpath:permissions/non-existent.yml");
            properties.setFailOnMissingFile(false);

            // 예외 없이 실행되어야 함
            loader.run(new DefaultApplicationArguments(new String[0]));

            verify(rootRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("synchronize 메서드")
    class Synchronize {

        @Test
        @DisplayName("Given 그룹 정의 When synchronize Then 그룹이 생성된다")
        void givenGroupDefinition_whenSynchronize_thenGroupCreated() {
            PermissionGroupDefinition definition = new PermissionGroupDefinition(
                    "NEW_GROUP", "새 그룹", "설명", null, List.of());
            given(rootRepository.findByGroupCode("NEW_GROUP")).willReturn(Optional.empty());
            // save()가 저장된 엔티티를 반환하도록 stub
            given(rootRepository.save(any(PermissionGroupRoot.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(versionRepository.findMaxVersionByRootId(any())).willReturn(0);

            loader.synchronize(List.of(definition));

            verify(rootRepository, atLeastOnce()).save(rootCaptor.capture());
            PermissionGroupRoot savedRoot = rootCaptor.getValue();
            assertThat(savedRoot.getGroupCode()).isEqualTo("NEW_GROUP");
            assertThat(savedRoot.getCurrentVersion().getName()).isEqualTo("새 그룹");
        }

        @Test
        @DisplayName("Given approvalGroupCodes가 있는 정의 When synchronize Then 값이 설정된다")
        void givenApprovalGroupCodes_whenSynchronize_thenValueSet() {
            PermissionGroupDefinition definition = new PermissionGroupDefinition(
                    "NEW_GROUP", "새 그룹", "설명",
                    List.of("APPROVAL_GROUP_A", "APPROVAL_GROUP_B"),
                    List.of());
            given(rootRepository.findByGroupCode("NEW_GROUP")).willReturn(Optional.empty());
            given(rootRepository.save(any(PermissionGroupRoot.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(versionRepository.findMaxVersionByRootId(any())).willReturn(0);

            loader.synchronize(List.of(definition));

            verify(rootRepository, atLeastOnce()).save(rootCaptor.capture());
            PermissionGroupRoot savedRoot = rootCaptor.getValue();
            assertThat(savedRoot.getCurrentVersion().getApprovalGroupCodes())
                    .containsExactly("APPROVAL_GROUP_A", "APPROVAL_GROUP_B");
        }

        @Test
        @DisplayName("Given 기존 그룹 존재 When synchronize Then 새 버전이 생성된다")
        void givenExistingGroup_whenSynchronize_thenNewVersionCreated() {
            // 먼저 그룹 생성 - 기존 Root와 version 1 설정
            OffsetDateTime now = OffsetDateTime.now();
            PermissionGroupRoot existingRoot = PermissionGroupRoot.createWithCode("EXISTING", now);
            PermissionGroup version1 = PermissionGroup.create(
                    existingRoot, 1, "기존 그룹", null, true, List.of(), null,
                    ChangeAction.CREATE, null, "system", null, now);
            existingRoot.activateNewVersion(version1, now);

            given(rootRepository.findByGroupCode("EXISTING")).willReturn(Optional.of(existingRoot));
            given(versionRepository.findMaxVersionByRootId(any())).willReturn(1);

            // 업데이트
            PermissionGroupDefinition updated = new PermissionGroupDefinition(
                    "EXISTING", "업데이트된 그룹", "새 설명", null, List.of());
            loader.synchronize(List.of(updated));

            verify(rootRepository, atLeastOnce()).save(rootCaptor.capture());
            PermissionGroupRoot savedRoot = rootCaptor.getValue();
            assertThat(savedRoot.getCurrentVersion().getName()).isEqualTo("업데이트된 그룹");
            assertThat(savedRoot.getCurrentVersion().getDescription()).isEqualTo("새 설명");
            assertThat(savedRoot.getCurrentVersion().getVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("Given assignment 정의 When synchronize Then assignment가 생성된다")
        void givenAssignmentDefinition_whenSynchronize_thenAssignmentCreated() {
            PermissionAssignmentDefinition assignment = new PermissionAssignmentDefinition(
                    FeatureCode.DRAFT, ActionCode.READ);
            PermissionGroupDefinition definition = new PermissionGroupDefinition(
                    "GROUP_WITH_ASSIGN", "그룹", null, null, List.of(assignment));
            given(rootRepository.findByGroupCode("GROUP_WITH_ASSIGN")).willReturn(Optional.empty());
            given(rootRepository.save(any(PermissionGroupRoot.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(versionRepository.findMaxVersionByRootId(any())).willReturn(0);

            loader.synchronize(List.of(definition));

            verify(rootRepository, atLeastOnce()).save(rootCaptor.capture());
            PermissionGroupRoot savedRoot = rootCaptor.getValue();
            assertThat(savedRoot.getCurrentVersion().getAssignments())
                    .anySatisfy(a -> {
                        assertThat(a.getFeature()).isEqualTo(FeatureCode.DRAFT);
                        assertThat(a.getAction()).isEqualTo(ActionCode.READ);
                    });
        }

        @Test
        @DisplayName("Given 다중 assignment When synchronize Then 모든 assignment가 생성된다")
        void givenMultipleAssignments_whenSynchronize_thenAllCreated() {
            List<PermissionAssignmentDefinition> assignments = List.of(
                    new PermissionAssignmentDefinition(FeatureCode.DRAFT, ActionCode.READ),
                    new PermissionAssignmentDefinition(FeatureCode.DRAFT, ActionCode.CREATE),
                    new PermissionAssignmentDefinition(FeatureCode.APPROVAL, ActionCode.APPROVE)
            );
            PermissionGroupDefinition definition = new PermissionGroupDefinition(
                    "GROUP_MULTI", "다중 권한", null, null, assignments);
            given(rootRepository.findByGroupCode("GROUP_MULTI")).willReturn(Optional.empty());
            given(rootRepository.save(any(PermissionGroupRoot.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(versionRepository.findMaxVersionByRootId(any())).willReturn(0);

            loader.synchronize(List.of(definition));

            verify(rootRepository, atLeastOnce()).save(rootCaptor.capture());
            PermissionGroupRoot savedRoot = rootCaptor.getValue();
            assertThat(savedRoot.getCurrentVersion().getAssignments()).hasSize(3);
        }
    }

    private ObjectMapper yamlMapper() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
