package com.example.common.masking;

import java.util.Set;

import jakarta.persistence.Converter;

import com.example.common.jpa.AbstractEnumSetJsonConverter;

/**
 * Set&lt;DataKind&gt;를 JSON 문자열로 DB에 저장하는 JPA Converter.
 *
 * <p>저장 형식: ["SSN", "PHONE", "EMAIL"]
 *
 * <p>동작:
 * <ul>
 *   <li>빈 Set → "[]" (모든 DataKind에 적용됨을 의미)</li>
 *   <li>null → "[]"</li>
 *   <li>Set.of(SSN, PHONE) → '["SSN","PHONE"]'</li>
 * </ul>
 */
@Converter
public class DataKindSetConverter extends AbstractEnumSetJsonConverter<DataKind> {

    public DataKindSetConverter() {
        super(DataKind.class, DataKind::fromString);
    }
}
