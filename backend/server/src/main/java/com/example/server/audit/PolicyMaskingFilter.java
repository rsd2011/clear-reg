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
import com.example.common.policy.MaskingPolicyProvider;
import com.example.common.policy.MaskingQuery;
import com.example.common.policy.RowAccessContextHolder;
import com.example.common.policy.RowAccessPolicyProvider;
import com.example.common.policy.RowAccessQuery;
import com.example.common.masking.MaskingContextHolder;
import com.example.common.security.RowScopeContext;
import com.example.common.security.RowScopeContextHolder;

/**
 * RowAccessPolicy 평가 결과를 요청 컨텍스트에 주입해 RowScope 필터링에 활용한다.
 *
 * <p>MaskingPolicy는 필드 레벨에서 {@link com.example.admin.maskingpolicy.service.MaskingEvaluator}가
 * Jackson 직렬화 시점에 처리한다.
 */
public class PolicyMaskingFilter extends OncePerRequestFilter {

    private final RowAccessPolicyProvider rowAccessPolicyProvider;
    private final MaskingPolicyProvider maskingPolicyProvider;

    public static final String ATTR_ROW_ACCESS_MATCH = "ROW_ACCESS_POLICY_MATCH";
    public static final String ATTR_MASKING_MATCH = "MASKING_POLICY_MATCH";
    public static final String ATTR_MASKING_TARGET = "AUDIT_MASKING_TARGET";

    public PolicyMaskingFilter(RowAccessPolicyProvider rowAccessPolicyProvider,
                               MaskingPolicyProvider maskingPolicyProvider) {
        this.rowAccessPolicyProvider = rowAccessPolicyProvider;
        this.maskingPolicyProvider = maskingPolicyProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            var ctxOpt = AuthContextHolder.current();
            if (ctxOpt.isPresent()) {
                var ctx = ctxOpt.get();

                List<String> orgGroups = new ArrayList<>(ctx.orgGroupCodes());
                // 조직 계층 정보가 없으면 본인 조직만 우선 사용
                if (orgGroups.isEmpty() && StringUtils.hasText(ctx.organizationCode())) {
                    orgGroups.add(ctx.organizationCode());
                }

                // RowAccessPolicy 평가
                var rowAccessMatchOpt = rowAccessPolicyProvider.evaluate(new RowAccessQuery(
                        ctx.feature() != null ? ctx.feature().name() : null,
                        ctx.action() != null ? ctx.action().name() : null,
                        ctx.permissionGroupCode(),
                        orgGroups,
                        Instant.now()
                ));

                rowAccessMatchOpt.ifPresent(match -> {
                    request.setAttribute(ATTR_ROW_ACCESS_MATCH, match);
                    RowAccessContextHolder.set(match);
                    RowScopeContextHolder.set(new RowScopeContext(ctx.organizationCode(), orgGroups));
                });

                // MaskingPolicy 평가
                var maskingMatchOpt = maskingPolicyProvider.evaluate(new MaskingQuery(
                        ctx.feature() != null ? ctx.feature().name() : null,
                        ctx.action() != null ? ctx.action().name() : null,
                        ctx.permissionGroupCode(),
                        orgGroups,
                        null, // dataKind - 아래에서 MaskingTarget에서 설정
                        Instant.now()
                ));

                maskingMatchOpt.ifPresent(match -> {
                    request.setAttribute(ATTR_MASKING_MATCH, match);
                    com.example.common.policy.MaskingContextHolder.set(match);
                });

                // MaskingTarget 설정 (기본값)
                MaskingTarget existing = (MaskingTarget) request.getAttribute(ATTR_MASKING_TARGET);
                MaskingTarget target = buildMaskingTarget(existing);
                request.setAttribute(ATTR_MASKING_TARGET, target);
                MaskingContextHolder.set(target);
            }

            filterChain.doFilter(request, response);
        } finally {
            RowAccessContextHolder.clear();
            com.example.common.policy.MaskingContextHolder.clear();
            RowScopeContextHolder.clear();
            MaskingContextHolder.clear();
        }
    }

    private MaskingTarget buildMaskingTarget(MaskingTarget base) {
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
                .build();
    }
}
