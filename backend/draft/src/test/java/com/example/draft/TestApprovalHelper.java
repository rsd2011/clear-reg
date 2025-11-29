package com.example.draft;

import java.time.OffsetDateTime;

import com.example.admin.approval.domain.ApprovalGroup;
import com.example.admin.approval.domain.ApprovalTemplate;
import com.example.admin.approval.domain.ApprovalTemplateRoot;
import com.example.admin.approval.domain.ApprovalTemplateStep;
import com.example.common.version.ChangeAction;

/**
 * 테스트에서 Approval 관련 객체를 쉽게 생성하기 위한 헬퍼 클래스
 */
public class TestApprovalHelper {

    /**
     * ApprovalTemplateStep을 생성합니다.
     * @param stepOrder 단계 순서
     * @param groupCode 그룹 코드
     * @return ApprovalTemplateStep
     */
    public static ApprovalTemplateStep createTemplateStep(int stepOrder, String groupCode) {
        OffsetDateTime now = OffsetDateTime.now();
        ApprovalGroup group = ApprovalGroup.create(groupCode, "그룹명", "설명", stepOrder, now);
        ApprovalTemplateRoot root = ApprovalTemplateRoot.create(now);
        ApprovalTemplate version = ApprovalTemplate.create(
                root, 1, "템플릿", 0, null, true,
                ChangeAction.CREATE, null, "system", "System", now);
        return ApprovalTemplateStep.create(version, stepOrder, group, false);
    }

    /**
     * ApprovalTemplateStep을 생성합니다.
     * @param root 템플릿 루트 (사용 안함, 하위 호환성을 위해 존재)
     * @param stepOrder 단계 순서
     * @param groupCode 그룹 코드
     * @param description 설명 (하위 호환성을 위해 존재하지만 무시됨)
     * @return ApprovalTemplateStep
     */
    public static ApprovalTemplateStep createTemplateStep(ApprovalTemplateRoot root, int stepOrder, String groupCode, String description) {
        OffsetDateTime now = OffsetDateTime.now();
        ApprovalGroup group = ApprovalGroup.create(groupCode, "그룹명", "설명", stepOrder, now);
        ApprovalTemplateRoot r = root != null ? root : ApprovalTemplateRoot.create(now);
        ApprovalTemplate version = ApprovalTemplate.create(
                r, 1, "템플릿", 0, null, true,
                ChangeAction.CREATE, null, "system", "System", now);
        return ApprovalTemplateStep.create(version, stepOrder, group, false);
    }

    /**
     * 테스트용 ApprovalTemplateRoot와 ApprovalTemplate을 생성합니다.
     * @param name 템플릿 이름
     * @return ApprovalTemplateRoot (currentVersion이 설정된 상태)
     */
    public static ApprovalTemplateRoot createTemplateRootWithVersion(String name) {
        OffsetDateTime now = OffsetDateTime.now();
        ApprovalTemplateRoot root = ApprovalTemplateRoot.create(now);
        ApprovalTemplate version = ApprovalTemplate.create(
                root, 1, name, 0, null, true,
                ChangeAction.CREATE, null, "system", "System", now);
        // SCD Type 2: 버전을 활성화하면 currentVersion으로 설정됨
        root.activateNewVersion(version, now);
        return root;
    }

    /**
     * 테스트용 ApprovalTemplate(버전)을 직접 생성합니다.
     * @param name 템플릿 이름
     * @return ApprovalTemplate
     */
    public static ApprovalTemplate createTemplateVersion(String name) {
        OffsetDateTime now = OffsetDateTime.now();
        ApprovalTemplateRoot root = ApprovalTemplateRoot.create(now);
        return ApprovalTemplate.create(
                root, 1, name, 0, null, true,
                ChangeAction.CREATE, null, "system", "System", now);
    }
}
