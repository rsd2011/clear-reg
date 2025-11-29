package com.example.common.policy;

import java.time.Instant;
import java.util.List;

import com.example.common.masking.DataKind;

/**
 * 마스킹 정책 조회 쿼리.
 */
public record MaskingQuery(String featureCode,
                           String actionCode,
                           String permGroupCode,
                           List<String> orgGroupCodes,
                           DataKind dataKind,
                           Instant now) {

    /**
     * 레거시 호환: String dataKind를 받는 생성자.
     * @deprecated DataKind enum을 직접 사용하세요.
     */
    @Deprecated
    public MaskingQuery(String featureCode,
                        String actionCode,
                        String permGroupCode,
                        List<String> orgGroupCodes,
                        String dataKindStr,
                        Instant now) {
        this(featureCode, actionCode, permGroupCode, orgGroupCodes,
                DataKind.fromString(dataKindStr), now);
    }

    public Instant nowOrDefault() {
        return now != null ? now : Instant.now();
    }

    /**
     * 레거시 호환: dataKind를 String으로 반환.
     * @deprecated getDataKind()를 사용하세요.
     */
    @Deprecated
    public String dataKindString() {
        return dataKind != null ? dataKind.name() : null;
    }
}
