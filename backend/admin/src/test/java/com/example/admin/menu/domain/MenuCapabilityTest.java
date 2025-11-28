package com.example.admin.menu.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * MenuCapability 단위 테스트.
 */
class MenuCapabilityTest {

    @Test
    @DisplayName("Given 동일한 Feature/Action - When equals 비교 - Then true 반환")
    void equals_sameFeatureAndAction_returnsTrue() {
        // Given
        MenuCapability cap1 = new MenuCapability(FeatureCode.DRAFT, ActionCode.DRAFT_CREATE);
        MenuCapability cap2 = new MenuCapability(FeatureCode.DRAFT, ActionCode.DRAFT_CREATE);

        // When & Then
        assertThat(cap1).isEqualTo(cap2);
        assertThat(cap1.hashCode()).isEqualTo(cap2.hashCode());
    }

    @Test
    @DisplayName("Given 다른 Feature - When equals 비교 - Then false 반환")
    void equals_differentFeature_returnsFalse() {
        // Given
        MenuCapability cap1 = new MenuCapability(FeatureCode.DRAFT, ActionCode.READ);
        MenuCapability cap2 = new MenuCapability(FeatureCode.APPROVAL, ActionCode.READ);

        // When & Then
        assertThat(cap1).isNotEqualTo(cap2);
    }

    @Test
    @DisplayName("Given 다른 Action - When equals 비교 - Then false 반환")
    void equals_differentAction_returnsFalse() {
        // Given
        MenuCapability cap1 = new MenuCapability(FeatureCode.DRAFT, ActionCode.READ);
        MenuCapability cap2 = new MenuCapability(FeatureCode.DRAFT, ActionCode.DRAFT_CREATE);

        // When & Then
        assertThat(cap1).isNotEqualTo(cap2);
    }

    @Test
    @DisplayName("Given MenuCapability 생성 - When getter 호출 - Then 올바른 값 반환")
    void getters_returnsCorrectValues() {
        // Given
        MenuCapability cap = new MenuCapability(FeatureCode.ORGANIZATION, ActionCode.UPDATE);

        // When & Then
        assertThat(cap.getFeature()).isEqualTo(FeatureCode.ORGANIZATION);
        assertThat(cap.getAction()).isEqualTo(ActionCode.UPDATE);
    }
}
