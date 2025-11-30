package com.example.admin.draft.schema.builder;

import java.math.BigDecimal;
import java.util.List;

import com.example.admin.draft.schema.ArrayField;
import com.example.admin.draft.schema.AttachmentConfig;
import com.example.admin.draft.schema.CheckboxField;
import com.example.admin.draft.schema.DateField;
import com.example.admin.draft.schema.DraftFormSchema;
import com.example.admin.draft.schema.FileField;
import com.example.admin.draft.schema.FormLayout;
import com.example.admin.draft.schema.GroupField;
import com.example.admin.draft.schema.NumberField;
import com.example.admin.draft.schema.SelectField;
import com.example.admin.draft.schema.TextField;
import com.example.common.orggroup.WorkType;

/**
 * 업무 유형별 사전 정의된 폼 스키마 빌더 (시드 데이터용).
 * <p>
 * <b>용도:</b> 시스템 초기화 또는 새 업무 유형 추가 시 기본 템플릿으로 사용합니다.
 * </p>
 * <p>
 * <b>버전 관리:</b> 스키마의 버전 관리는 {@link com.example.admin.draft.domain.DraftFormTemplate}을
 * 통해 DB에서 수행됩니다. 이 빌더는 초기 템플릿 생성에만 사용하고,
 * 이후 수정은 관리자 UI를 통해 새 버전으로 저장됩니다.
 * </p>
 *
 * <pre>
 * [빌더] → 초기 템플릿 생성 → [DB 저장] → 관리자 수정 → [새 버전 생성]
 *          (v1.0)              (v1)        (v2, v3...)
 * </pre>
 *
 * <p>사용 예시:</p>
 * <pre>{@code
 * // 시스템 초기화 시
 * DraftFormSchema schema = FormSchemaBuilders.forWorkType(WorkType.HR_UPDATE);
 * String schemaJson = schema.toJson(objectMapper);
 * templateService.createInitialTemplate("HR_UPDATE_FORM", schemaJson, ...);
 * }</pre>
 *
 * @see com.example.admin.draft.domain.DraftFormTemplate
 */
public final class FormSchemaBuilders {

    private FormSchemaBuilders() {
        // 유틸리티 클래스
    }

    /**
     * WorkType에 따른 기본 스키마를 반환합니다.
     *
     * @param workType 업무 유형
     * @return 해당 업무 유형의 기본 스키마
     */
    public static DraftFormSchema forWorkType(WorkType workType) {
        return switch (workType) {
            case GENERAL -> general();
            case FILE_EXPORT -> fileExport();
            case DATA_CORRECTION -> dataCorrection();
            case HR_UPDATE -> hrUpdate();
            case POLICY_CHANGE -> policyChange();
        };
    }

    /**
     * 일반 업무 기안 스키마.
     * <p>
     * 제목, 내용, 첨부파일로 구성된 기본적인 기안 양식입니다.
     * </p>
     */
    public static DraftFormSchema general() {
        return DraftFormSchema.builder()
                .version("1.0")
                .fields(List.of(
                        TextField.of("title", "제목", true),
                        TextField.multiline("content", "내용", true),
                        FileField.any("attachments", "첨부파일", false)
                ))
                .layout(FormLayout.singleColumn())
                .attachmentConfig(AttachmentConfig.optional(5, 50L * 1024 * 1024))
                .build();
    }

    /**
     * 파일 반출 신청 스키마.
     * <p>
     * 반출 목적, 대상 파일, 반출 기간, 보안 확인 등을 포함합니다.
     * </p>
     */
    public static DraftFormSchema fileExport() {
        return DraftFormSchema.builder()
                .version("1.0")
                .fields(List.of(
                        TextField.of("title", "반출 건명", true),
                        SelectField.of("exportType", "반출 유형", true, List.of(
                                SelectField.SelectOption.of("EMAIL", "이메일 발송"),
                                SelectField.SelectOption.of("USB", "USB 저장"),
                                SelectField.SelectOption.of("CLOUD", "클라우드 업로드"),
                                SelectField.SelectOption.of("PRINT", "출력물"),
                                SelectField.SelectOption.of("OTHER", "기타")
                        )),
                        TextField.multiline("purpose", "반출 목적", true),
                        TextField.of("recipient", "수신처/수신인", true),
                        GroupField.of("exportPeriod", "반출 기간", List.of(
                                DateField.of("startDate", "시작일", true),
                                DateField.of("endDate", "종료일", true)
                        )),
                        ArrayField.of("files", "반출 대상 파일", true, List.of(
                                TextField.of("fileName", "파일명", true),
                                TextField.of("filePath", "파일 경로", true),
                                NumberField.of("fileSize", "파일 크기(KB)", false),
                                SelectField.of("sensitivity", "민감도", true, List.of(
                                        SelectField.SelectOption.of("LOW", "일반"),
                                        SelectField.SelectOption.of("MEDIUM", "대외비"),
                                        SelectField.SelectOption.of("HIGH", "기밀")
                                ))
                        )),
                        CheckboxField.agreement("securityConfirm", "반출 파일에 개인정보 또는 기밀정보가 포함되어 있지 않음을 확인합니다."),
                        TextField.multiline("remarks", "비고", false)
                ))
                .layout(FormLayout.singleColumn())
                .attachmentConfig(AttachmentConfig.disabled())
                .build();
    }

    /**
     * 데이터 정정 요청 스키마.
     * <p>
     * 정정 대상 테이블, 정정 내용, 정정 사유 등을 포함합니다.
     * </p>
     */
    public static DraftFormSchema dataCorrection() {
        return DraftFormSchema.builder()
                .version("1.0")
                .fields(List.of(
                        TextField.of("title", "정정 건명", true),
                        SelectField.of("correctionType", "정정 유형", true, List.of(
                                SelectField.SelectOption.of("INSERT", "데이터 추가"),
                                SelectField.SelectOption.of("UPDATE", "데이터 수정"),
                                SelectField.SelectOption.of("DELETE", "데이터 삭제")
                        )),
                        TextField.of("targetTable", "대상 테이블", true),
                        TextField.of("targetKey", "대상 키값", true),
                        GroupField.of("correctionDetail", "정정 내용", List.of(
                                TextField.multiline("beforeValue", "정정 전 값", false),
                                TextField.multiline("afterValue", "정정 후 값", true)
                        )),
                        TextField.multiline("reason", "정정 사유", true),
                        DateField.of("effectiveDate", "적용 희망일", false),
                        CheckboxField.agreement("impactConfirm", "데이터 정정으로 인한 영향 범위를 확인하였습니다.")
                ))
                .layout(FormLayout.singleColumn())
                .attachmentConfig(AttachmentConfig.optional(3, 10L * 1024 * 1024))
                .build();
    }

    /**
     * 인사정보 변경 요청 스키마.
     * <p>
     * 변경 대상자, 변경 항목, 변경 내용 등을 포함합니다.
     * </p>
     */
    public static DraftFormSchema hrUpdate() {
        return DraftFormSchema.builder()
                .version("1.0")
                .fields(List.of(
                        TextField.of("title", "변경 건명", true),
                        GroupField.of("targetEmployee", "변경 대상자", List.of(
                                TextField.of("employeeId", "사번", true),
                                TextField.of("employeeName", "성명", true),
                                TextField.of("department", "현재 부서", false),
                                TextField.of("position", "현재 직위", false)
                        )),
                        SelectField.of("changeType", "변경 유형", true, List.of(
                                SelectField.SelectOption.of("TRANSFER", "부서 이동"),
                                SelectField.SelectOption.of("PROMOTION", "승진"),
                                SelectField.SelectOption.of("LEAVE", "휴직"),
                                SelectField.SelectOption.of("RETURN", "복직"),
                                SelectField.SelectOption.of("RETIRE", "퇴직"),
                                SelectField.SelectOption.of("INFO_CHANGE", "기본정보 변경"),
                                SelectField.SelectOption.of("OTHER", "기타")
                        )),
                        ArrayField.of("changes", "변경 상세", true, List.of(
                                TextField.of("fieldName", "변경 항목", true),
                                TextField.of("beforeValue", "변경 전", false),
                                TextField.of("afterValue", "변경 후", true)
                        )),
                        DateField.of("effectiveDate", "적용일", true),
                        TextField.multiline("reason", "변경 사유", true)
                ))
                .layout(FormLayout.singleColumn())
                .attachmentConfig(AttachmentConfig.optional(5, 20L * 1024 * 1024))
                .build();
    }

    /**
     * 정책 변경 요청 스키마.
     * <p>
     * 마스킹 정책, 권한 정책 등 시스템 정책 변경 요청용입니다.
     * </p>
     */
    public static DraftFormSchema policyChange() {
        return DraftFormSchema.builder()
                .version("1.0")
                .fields(List.of(
                        TextField.of("title", "정책 변경 건명", true),
                        SelectField.of("policyType", "정책 유형", true, List.of(
                                SelectField.SelectOption.of("MASKING", "마스킹 정책"),
                                SelectField.SelectOption.of("PERMISSION", "권한 정책"),
                                SelectField.SelectOption.of("AUDIT", "감사 정책"),
                                SelectField.SelectOption.of("RETENTION", "보존 정책"),
                                SelectField.SelectOption.of("ACCESS", "접근 제어 정책"),
                                SelectField.SelectOption.of("OTHER", "기타")
                        )),
                        TextField.of("policyName", "정책명", true),
                        GroupField.of("changeScope", "변경 범위", List.of(
                                TextField.of("targetSystem", "대상 시스템", true),
                                TextField.multiline("affectedEntities", "영향 받는 항목", true)
                        )),
                        GroupField.of("policyDetail", "정책 내용", List.of(
                                TextField.multiline("currentPolicy", "현행 정책", false),
                                TextField.multiline("proposedPolicy", "변경 정책", true)
                        )),
                        TextField.multiline("reason", "변경 사유", true),
                        TextField.multiline("riskAssessment", "위험 평가", false),
                        DateField.of("effectiveDate", "적용 희망일", true),
                        CheckboxField.agreement("impactReview", "정책 변경으로 인한 영향 분석을 완료하였습니다.")
                ))
                .layout(FormLayout.singleColumn())
                .attachmentConfig(AttachmentConfig.optional(10, 30L * 1024 * 1024))
                .build();
    }
}
