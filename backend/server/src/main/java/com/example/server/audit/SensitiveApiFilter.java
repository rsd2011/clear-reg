package com.example.server.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.audit.AuditPolicySnapshot;
import com.example.audit.AuditPort;
import com.example.server.config.SensitiveApiProperties;
import com.example.common.masking.DataKind;
import com.example.common.masking.MaskingTarget;
import com.example.common.masking.SubjectType;

/**
 * 민감 응답 API 호출 시 reason/legalBasis 필수 여부를 검사하는 필터.
 * 정책이 정의되지 않은 경우 secure-by-default 로 reason 필수.
 */
@ConditionalOnProperty(prefix = "audit.sensitive-api", name = "validation-enabled", havingValue = "true", matchIfMissing = true)
public class SensitiveApiFilter extends OncePerRequestFilter {

    private final AuditPort auditPort;
    private final SensitiveApiProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public SensitiveApiFilter(AuditPort auditPort, SensitiveApiProperties properties) {
        this.auditPort = auditPort;
        this.properties = properties;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 간단한 heuristic: 헤더 X-SENSITIVE-API: true 또는 정책상 sensitiveApi=true
        boolean headerSensitive = "true".equalsIgnoreCase(request.getHeader("X-SENSITIVE-API"));
        var policyEndpoints = auditPort.resolve(request.getRequestURI(), request.getMethod())
                .flatMap(s -> Optional.ofNullable((java.util.List<String>) s.getAttributes().get("sensitiveEndpoints")))
                .orElse(List.of());

        boolean pathSensitive = properties.getEndpoints().stream()
                .anyMatch(p -> pathMatcher.match(p, request.getRequestURI()));
        boolean policyPathSensitive = policyEndpoints.stream().anyMatch(p -> pathMatcher.match(p, request.getRequestURI()));
        boolean policySensitive = auditPort.resolve(request.getRequestURI(), request.getMethod())
                .map(AuditPolicySnapshot::isSensitiveApi)
                .orElse(true); // secure-by-default

        if (headerSensitive || pathSensitive || policyPathSensitive || policySensitive) {
            String reason = extract(request, properties.getReasonParameter());
            String legal = extract(request, properties.getLegalBasisParameter());
            boolean reasonRequired = auditPort.resolve(request.getRequestURI(), request.getMethod())
                    .map(AuditPolicySnapshot::isReasonRequired)
                    .orElse(true);

            if (reasonRequired && !StringUtils.hasText(reason)) {
                response.sendError(HttpStatus.BAD_REQUEST.value(), "reasonCode is required for sensitive API");
                return;
            }
            // legal basis는 정책상 reasonRequired와 함께 기본 필수로 간주
            if (reasonRequired && !StringUtils.hasText(legal)) {
                response.sendError(HttpStatus.BAD_REQUEST.value(), "legalBasisCode is required for sensitive API");
                return;
            }

            // 마스킹/해제 컨텍스트를 다음 인터셉터에서 사용하도록 전달
            MaskingTarget target = MaskingTarget.builder()
                    .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                    .dataKind(DataKind.DEFAULT)
                    .defaultMask(true)
                    .build();
            request.setAttribute("AUDIT_MASKING_TARGET", target);
        }

        filterChain.doFilter(request, response);
    }

    private String extract(HttpServletRequest request, String name) {
        String fromParam = request.getParameter(name);
        if (StringUtils.hasText(fromParam)) {
            return fromParam;
        }
        return request.getHeader(name);
    }
}
