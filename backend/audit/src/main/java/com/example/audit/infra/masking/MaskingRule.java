package com.example.audit.infra.masking;

import java.util.regex.Pattern;

public class MaskingRule {
    private String pattern;
    private String replacement;
    private Pattern compiled;

    public MaskingRule() {
    }

    public MaskingRule(String pattern, String replacement) {
        this.pattern = pattern;
        this.replacement = replacement;
        this.compiled = Pattern.compile(pattern);
    }

    public String apply(String input) {
        if (input == null) return null;
        if (compiled == null) compiled = Pattern.compile(pattern);
        return compiled.matcher(input).replaceAll(replacement);
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
        this.compiled = Pattern.compile(pattern);
    }

    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }
}
