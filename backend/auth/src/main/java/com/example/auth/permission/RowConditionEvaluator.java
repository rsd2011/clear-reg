package com.example.auth.permission;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.AccessException;
import org.springframework.expression.TypedValue;

@Component
public class RowConditionEvaluator {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final Map<String, Expression> compiled = new ConcurrentHashMap<>();

    public boolean isAllowed(String expression, Map<String, Object> attributes) {
        if (expression == null || expression.isBlank()) {
            return true;
        }
        try {
            Expression spel = compiled.computeIfAbsent(expression, parser::parseExpression);
            StandardEvaluationContext context = new StandardEvaluationContext(attributes);
            context.addPropertyAccessor(new MapPropertyAccessor());
            Boolean result = spel.getValue(context, Boolean.class);
            return Boolean.TRUE.equals(result);
        }
        catch (SpelEvaluationException | ParseException ex) {
            throw new IllegalArgumentException("잘못된 RowScope 조건 표현식입니다: " + expression, ex);
        }
    }

    public void validate(String expression) {
        if (expression == null || expression.isBlank()) {
            return;
        }
        try {
            compiled.computeIfAbsent(expression, parser::parseExpression);
        }
        catch (ParseException ex) {
            throw new IllegalArgumentException("잘못된 RowScope 조건 표현식입니다: " + expression, ex);
        }
    }

    static final class MapPropertyAccessor implements PropertyAccessor {

        @Override
        public Class<?>[] getSpecificTargetClasses() {
            return new Class<?>[]{Map.class};
        }

        @Override
        public boolean canRead(EvaluationContext context, Object target, String name) {
            if (target instanceof Map<?, ?> map) {
                return map.containsKey(name);
            }
            return false;
        }

        @Override
        public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
            if (target instanceof Map<?, ?> map) {
                return new TypedValue(map.get(name));
            }
            throw new AccessException("MapAccessor는 Map 타입만 지원합니다.");
        }

        @Override
        public boolean canWrite(EvaluationContext context, Object target, String name) {
            return false;
        }

        @Override
        public void write(EvaluationContext context, Object target, String name, Object newValue) {
            throw new UnsupportedOperationException("읽기 전용 접근자입니다.");
        }
    }
}
