package com.example.auth.permission;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.example.common.security.RowScope;

class PermissionGroupTest {

    @Test
    void givenAssignments_whenLookup_thenReturnMatchingPermission() throws Exception {
        PermissionGroup group = new PermissionGroup("AUDIT", "Auditor");
        Field assignments = PermissionGroup.class.getDeclaredField("assignments");
        assignments.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<PermissionAssignment> values = (Set<PermissionAssignment>) assignments.get(group);
        PermissionAssignment assignment = new PermissionAssignment(FeatureCode.ORGANIZATION, ActionCode.READ, RowScope.ALL);
        values.add(assignment);

        Field maskField = PermissionGroup.class.getDeclaredField("maskRules");
        maskField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<FieldMaskRule> maskRules = (Set<FieldMaskRule>) maskField.get(group);
        maskRules.add(new FieldMaskRule("ORG_NAME", "***", ActionCode.UNMASK, true));

        assertThat(group.assignmentFor(FeatureCode.ORGANIZATION, ActionCode.READ)).contains(assignment);
        assertThat(group.maskRulesByTag()).containsKey("ORG_NAME");
    }
}
