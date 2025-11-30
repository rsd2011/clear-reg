package com.example.admin.orggroup.domain;

import jakarta.persistence.Converter;

import com.example.common.jpa.AbstractEnumSetJsonConverter;
import com.example.common.orggroup.WorkCategory;

/**
 * Set&lt;WorkCategory&gt;를 JSON 문자열로 DB에 저장하는 JPA Converter.
 *
 * <p>저장 형식: ["COMPLIANCE", "SALES", "TRADING"]
 *
 * <p>동작:
 * <ul>
 *   <li>빈 Set → "[]"</li>
 *   <li>null → "[]"</li>
 *   <li>Set.of(COMPLIANCE, SALES) → '["COMPLIANCE","SALES"]'</li>
 * </ul>
 */
@Converter
public class WorkCategorySetConverter extends AbstractEnumSetJsonConverter<WorkCategory> {

    public WorkCategorySetConverter() {
        super(WorkCategory.class, WorkCategory::fromString);
    }
}
