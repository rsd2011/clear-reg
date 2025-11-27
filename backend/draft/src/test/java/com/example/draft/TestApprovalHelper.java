package com.example.draft;

import java.time.OffsetDateTime;

import com.example.admin.approval.ApprovalGroup;
import com.example.admin.approval.ApprovalLineTemplate;
import com.example.admin.approval.ApprovalTemplateStep;

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
        ApprovalLineTemplate template = ApprovalLineTemplate.create("템플릿", 0, null, now);
        return new ApprovalTemplateStep(template, stepOrder, group);
    }

    /**
     * ApprovalTemplateStep을 생성합니다. (description 파라미터 무시)
     * @param template 템플릿 (null 가능)
     * @param stepOrder 단계 순서
     * @param groupCode 그룹 코드
     * @param description 설명 (하위 호환성을 위해 존재하지만 무시됨)
     * @return ApprovalTemplateStep
     */
    public static ApprovalTemplateStep createTemplateStep(ApprovalLineTemplate template, int stepOrder, String groupCode, String description) {
        OffsetDateTime now = OffsetDateTime.now();
        ApprovalGroup group = ApprovalGroup.create(groupCode, "그룹명", "설명", stepOrder, now);
        ApprovalLineTemplate t = template != null ? template : ApprovalLineTemplate.create("템플릿", 0, null, now);
        return new ApprovalTemplateStep(t, stepOrder, group);
    }
}
