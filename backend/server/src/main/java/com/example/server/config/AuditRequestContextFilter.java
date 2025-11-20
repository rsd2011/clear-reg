package com.example.server.config;

import java.io.IOException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.draft.application.audit.AuditRequestContext;
import com.example.draft.application.audit.AuditRequestContextHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuditRequestContextFilter extends OncePerRequestFilter {

    private final Set<String> trustedProxies;

    public AuditRequestContextFilter(@Value("${audit.trusted-proxies:}") List<String> trustedProxies) {
        this.trustedProxies = trustedProxies == null ? Set.of() :
                trustedProxies.stream().map(String::trim).filter(s -> !s.isBlank()).collect(Collectors.toSet());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String ip = extractIp(request);
            String ua = request.getHeader("User-Agent");
            AuditRequestContextHolder.set(new AuditRequestContext(ip, ua));
            filterChain.doFilter(request, response);
        } finally {
            AuditRequestContextHolder.clear();
        }
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank() && isTrustedProxy(request.getRemoteAddr())) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isTrustedProxy(String remoteAddr) {
        if (trustedProxies.isEmpty()) {
            return false;
        }
        return trustedProxies.contains(remoteAddr);
    }
}
