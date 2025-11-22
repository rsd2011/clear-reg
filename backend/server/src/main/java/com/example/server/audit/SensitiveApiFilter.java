package com.example.server.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.audit.AuditPolicySnapshot;
import com.example.audit.AuditPort;

/**
 * 민감 응답 API 호출 시 reason/legalBasis 필수 여부를 검사하는 필터.
 * 정책이 정의되지 않은 경우 secure-by-default 로 reason 필수.
 */
@ConditionalOnProperty(prefix = "audit.sensitive-api", name = "validation-enabled", havingValue = "true", matchIfMissing = true)
public class SensitiveApiFilter extends OncePerRequestFilter {

    private final AuditPort auditPort;
    private final String reasonParam;
    private final String legalBasisParam;

    public SensitiveApiFilter(AuditPort auditPort,
                              String reasonParam,
                              String legalBasisParam) {
        this.auditPort = auditPort;
        this.reasonParam = reasonParam;
        this.legalBasisParam = legalBasisParam;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 간단한 heuristic: 헤더 X-SENSITIVE-API: true 또는 정책상 sensitiveApi=true
        boolean headerSensitive = "true".equalsIgnoreCase(request.getHeader("X-SENSITIVE-API"));
        boolean policySensitive = auditPort.resolve(request.getRequestURI(), request.getMethod())
                .map(AuditPolicySnapshot::isSensitiveApi)
                .orElse(true); // secure-by-default

        if (headerSensitive || policySensitive) {
            String reason = extract(request, reasonParam);
            String legal = extract(request, legalBasisParam);
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
