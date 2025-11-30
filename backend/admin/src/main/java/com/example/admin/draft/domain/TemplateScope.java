package com.example.admin.draft.domain;

import com.example.common.codegroup.annotation.ManagedCode;

/**
 * 템플릿 범위.
 *
 * <p>템플릿이 전역적으로 적용되는지 또는 특정 조직에만 적용되는지를 나타낸다.</p>
 */
@ManagedCode(displayName = "템플릿 범위", group = "DRAFT")
public enum TemplateScope {
    /** 전역 (모든 조직에 적용) */
    GLOBAL,

    /** 조직별 (특정 조직에만 적용) */
    ORGANIZATION
}
