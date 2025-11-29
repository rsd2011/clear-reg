package com.example.server.config;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.example.admin.permission.context.AuthContext;
import com.example.admin.permission.context.AuthContextHolder;
import com.example.common.security.CurrentUser;
import com.example.common.security.RowScope;

/**
 * 컨트롤러 메서드의 CurrentUser 파라미터를 해결하는 ArgumentResolver.
 *
 * <p>AuthContextHolder에서 현재 인증 컨텍스트를 가져와 CurrentUser로 변환합니다.</p>
 */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return CurrentUser.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                   ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest,
                                   WebDataBinderFactory binderFactory) {
        return AuthContextHolder.current()
                .map(this::toCurrentUser)
                .orElse(null);
    }

    private CurrentUser toCurrentUser(AuthContext context) {
        return new CurrentUser(
                context.username(),
                context.organizationCode(),
                context.permissionGroupCode(),
                context.feature() != null ? context.feature().name() : null,
                context.action() != null ? context.action().name() : null,
                context.rowScope() != null ? context.rowScope() : RowScope.ALL,
                context.orgGroupCodes()
        );
    }
}
