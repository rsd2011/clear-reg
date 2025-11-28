package com.example.server.policy;

import org.springframework.stereotype.Component;

import com.example.admin.permission.context.AuthContextHolder;
import com.example.audit.Actor;
import com.example.audit.ActorType;
import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.audit.RiskLevel;

import com.example.admin.policy.service.PolicyAdminService;
import com.example.admin.policy.dto.PolicyUpdateRequest;
import com.example.admin.policy.dto.PolicyView;
import com.example.admin.policy.dto.PolicyYamlRequest;

@Component
public class PolicyAdminPortAdapter implements PolicyAdminPort {

    private final PolicyAdminService policyAdminService;
    private final AuditPort auditPort;

    private static final String POLICY_DOC = "security.policy";

    public PolicyAdminPortAdapter(PolicyAdminService policyAdminService,
                                 AuditPort auditPort) {
        this.policyAdminService = policyAdminService;
        this.auditPort = auditPort;
    }

    @Override
    public PolicyView currentPolicy() {
        return policyAdminService.currentView();
    }

    @Override
    public PolicyView updateToggles(PolicyUpdateRequest request) {
        PolicyView before = policyAdminService.currentView();
        PolicyView after = policyAdminService.updateView(request);
        recordPolicyChange("UPDATE_TOGGLES", before, after);
        return after;
    }

    @Override
    public PolicyView updateFromYaml(PolicyYamlRequest request) {
        PolicyView before = policyAdminService.currentView();
        PolicyView after = policyAdminService.applyYamlView(request.yaml());
        recordPolicyChange("UPDATE_YAML", before, after);
        return after;
    }

    private void recordPolicyChange(String action, PolicyView before, PolicyView after) {
        var ctx = AuthContextHolder.current().orElse(null);
        AuditEvent event = AuditEvent.builder()
                .eventType("POLICY_CHANGE")
                .moduleName("policy")
                .action(action)
                .actor(Actor.builder()
                        .id(ctx != null ? ctx.username() : "unknown")
                        .role(ctx != null ? ctx.permissionGroupCode() : null)
                        .type(ActorType.HUMAN)
                        .dept(ctx != null ? ctx.organizationCode() : null)
                        .build())
                .subject(com.example.audit.Subject.builder()
                        .type("POLICY_DOC")
                        .key(POLICY_DOC)
                        .build())
                .success(true)
                .resultCode("OK")
                .riskLevel(RiskLevel.MEDIUM)
                .beforeSummary(Integer.toString(before.hashCode()))
                .afterSummary(Integer.toString(after.hashCode()))
                .extraEntry("beforeYaml", before.yaml())
                .extraEntry("afterYaml", after.yaml())
                .build();
        try {
            auditPort.record(event, AuditMode.ASYNC_FALLBACK);
        } catch (Exception ex) {
            // 정책 변경 실패로 이어지지 않도록 예외 삼킴
        }
    }
}
