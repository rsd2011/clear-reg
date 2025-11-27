package com.example.server.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.masking.MaskingTarget;
import com.example.common.masking.SubjectType;
import com.example.common.policy.DataPolicyProvider;
import com.example.common.policy.DataPolicyQuery;
import com.example.common.policy.DataPolicyContextHolder;
import com.example.common.masking.MaskingContextHolder;
import com.example.common.security.RowScopeContext;
import com.example.common.security.RowScopeContextHolder;

/**
 * DataPolicy 평가 결과를 요청 컨텍스트에 주입해 마스킹/RowScope에 활용한다.
 */
public class DataPolicyMaskingFilter extends OncePerRequestFilter {

    private final DataPolicyProvider dataPolicyProvider;

    public static final String ATTR_POLICY_MATCH = "DATA_POLICY_MATCH";
    public static final String ATTR_MASKING_TARGET = "AUDIT_MASKING_TARGET";

    public DataPolicyMaskingFilter(DataPolicyProvider dataPolicyProvider) {
        this.dataPolicyProvider = dataPolicyProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            var ctxOpt = AuthContextHolder.current();
            if (ctxOpt.isPresent()) {
                var ctx = ctxOpt.get();

                List<String> orgGroups = new ArrayList<>();
                // 조직 계층 정보가 아직 없으므로 본인 조직만 우선 사용
                if (StringUtils.hasText(ctx.organizationCode())) {
                    orgGroups.add(ctx.organizationCode());
                }

                var matchOpt = dataPolicyProvider.evaluate(new DataPolicyQuery(
                        ctx.feature() != null ? ctx.feature().name() : null,
                        ctx.action() != null ? ctx.action().name() : null,
                        ctx.permissionGroupCode(),
                        null,
                        orgGroups,
                        null,
                        Instant.now()
                ));

                matchOpt.ifPresent(match -> {
                    request.setAttribute(ATTR_POLICY_MATCH, match);
                    DataPolicyContextHolder.set(match);
                    RowScopeContextHolder.set(new RowScopeContext(ctx.organizationCode(), orgGroups));
                    MaskingTarget existing = (MaskingTarget) request.getAttribute(ATTR_MASKING_TARGET);
                    MaskingTarget target = mergeMaskingTarget(existing, match.getMaskRule(), match.getMaskParams());
                    request.setAttribute(ATTR_MASKING_TARGET, target);
                    MaskingContextHolder.set(target);
                });
            }

            filterChain.doFilter(request, response);
        } finally {
            RowScopeContextHolder.clear();
            DataPolicyContextHolder.clear();
            MaskingContextHolder.clear();
        }
    }

    private MaskingTarget mergeMaskingTarget(MaskingTarget base, String maskRule, String maskParams) {
        SubjectType subject = base != null ? base.getSubjectType() : SubjectType.UNKNOWN;
        String dataKind = base != null ? base.getDataKind() : "HTTP";
        boolean defaultMask = base != null && base.isDefaultMask();
        boolean forceUnmask = base != null && base.isForceUnmask();
        Set<String> forceKinds = base != null ? base.getForceUnmaskKinds() : Set.of();
        Set<String> forceFields = base != null ? base.getForceUnmaskFields() : Set.of();
        Set<String> requesterRoles = base != null ? base.getRequesterRoles() : Set.of();
        String rowId = base != null ? base.getRowId() : null;

        return MaskingTarget.builder()
                .subjectType(subject)
                .dataKind(dataKind)
                .defaultMask(defaultMask || base == null)
                .forceUnmask(forceUnmask)
                .forceUnmaskKinds(new HashSet<>(forceKinds))
                .forceUnmaskFields(new HashSet<>(forceFields))
                .requesterRoles(new HashSet<>(requesterRoles))
                .rowId(rowId)
                .maskRule(maskRule)
                .maskParams(maskParams)
                .build();
    }
}
