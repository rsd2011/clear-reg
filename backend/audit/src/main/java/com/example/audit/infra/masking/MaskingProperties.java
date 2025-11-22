package com.example.audit.infra.masking;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "audit.masking")
public class MaskingProperties {

    /**
     * 교체 규칙 리스트. 패턴은 정규식.
     */
    private List<MaskingRule> rules = new ArrayList<>();

    public MaskingProperties() {
        // 기본 규칙 세팅
        rules.add(new MaskingRule("\\d{6}-\\d{7}", "[REDACTED-RRN]")); // 주민등록번호
        rules.add(new MaskingRule("\\b(?:\\d{4}[- ]?){2,3}\\d{1,7}\\b", "[REDACTED-CARD]")); // 카드
        rules.add(new MaskingRule("\\b\\d{2,4}-\\d{1,4}-\\d{3,7}\\b", "[REDACTED-ACCT]")); // 계좌
        rules.add(new MaskingRule("\\d{15,}", "[REDACTED-DIGITS]")); // 장문 숫자
        rules.add(new MaskingRule("[가-힣]{1,10}(동|로|길)\\s?\\d+[가-힣0-9\\-]*", "[REDACTED-ADDR]")); // 주소
        rules.add(new MaskingRule("이름\\s*[가-힣]{2,4}", "이름 [REDACTED-NAME]")); // 이름
    }

    public List<MaskingRule> getRules() {
        return rules;
    }

    public void setRules(List<MaskingRule> rules) {
        if (rules != null && !rules.isEmpty()) {
            this.rules = rules;
        }
    }

    public String applyAll(String input) {
        String out = input;
        for (MaskingRule rule : rules) {
            out = rule.apply(out);
        }
        return out;
    }
}
