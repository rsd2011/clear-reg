package com.example.auth.strategy;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.auth.LoginType;

@Component
public class AuthenticationStrategyResolver {

    private final Map<LoginType, AuthenticationStrategy> strategies;

    public AuthenticationStrategyResolver(List<AuthenticationStrategy> candidates) {
        this.strategies = new EnumMap<>(LoginType.class);
        candidates.forEach(strategy -> this.strategies.put(strategy.supportedType(), strategy));
    }

    public Optional<AuthenticationStrategy> resolve(LoginType type) {
        if (type == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(strategies.get(type));
    }
}
