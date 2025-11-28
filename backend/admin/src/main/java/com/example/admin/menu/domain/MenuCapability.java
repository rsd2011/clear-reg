package com.example.admin.menu.domain;

import com.example.admin.permission.domain.ActionCode;
import com.example.admin.permission.domain.FeatureCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.Objects;

/**
 * 메뉴에 필요한 Capability(Feature + Action) 조합.
 *
 * <p>사용자가 이 Capability를 보유하면 해당 메뉴에 접근할 수 있다.</p>
 */
@Embeddable
public class MenuCapability {

    @Enumerated(EnumType.STRING)
    @Column(name = "feature_code", nullable = false, length = 50)
    private FeatureCode feature;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_code", nullable = false, length = 50)
    private ActionCode action;

    protected MenuCapability() {}

    public MenuCapability(FeatureCode feature, ActionCode action) {
        this.feature = Objects.requireNonNull(feature, "feature must not be null");
        this.action = Objects.requireNonNull(action, "action must not be null");
    }

    public FeatureCode getFeature() {
        return feature;
    }

    public ActionCode getAction() {
        return action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MenuCapability that = (MenuCapability) o;
        return feature == that.feature && action == that.action;
    }

    @Override
    public int hashCode() {
        return Objects.hash(feature, action);
    }

    @Override
    public String toString() {
        return feature.name() + ":" + action.name();
    }
}
