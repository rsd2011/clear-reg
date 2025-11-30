package com.example.admin.draft.schema;

import java.util.List;

/**
 * 첨부파일 설정.
 * <p>
 * 기안 양식의 첨부파일 관련 설정을 정의합니다.
 * </p>
 *
 * @param enabled       첨부파일 허용 여부
 * @param required      첨부파일 필수 여부
 * @param maxFiles      최대 첨부파일 개수
 * @param maxTotalSize  최대 총 파일 크기 (bytes)
 * @param maxFileSize   개별 파일 최대 크기 (bytes)
 * @param acceptedTypes 허용 MIME 타입 목록
 * @param categories    첨부파일 카테고리 목록 (선택적 분류)
 */
public record AttachmentConfig(
        boolean enabled,
        boolean required,
        Integer maxFiles,
        Long maxTotalSize,
        Long maxFileSize,
        List<String> acceptedTypes,
        List<AttachmentCategory> categories
) {
    public AttachmentConfig {
        if (acceptedTypes == null) {
            acceptedTypes = List.of();
        }
        if (categories == null) {
            categories = List.of();
        }
    }

    /**
     * 첨부파일 비활성화 설정.
     */
    public static AttachmentConfig disabled() {
        return new AttachmentConfig(false, false, 0, 0L, 0L, List.of(), List.of());
    }

    /**
     * 기본 설정 (최대 10개, 50MB).
     */
    public static AttachmentConfig defaultConfig() {
        return new AttachmentConfig(
                true,
                false,
                10,
                50L * 1024 * 1024,  // 50MB
                10L * 1024 * 1024,  // 10MB per file
                List.of(),
                List.of()
        );
    }

    /**
     * 선택적 첨부파일 설정 (최대 개수, 총 용량 지정).
     *
     * @param maxFiles     최대 파일 개수
     * @param maxTotalSize 최대 총 파일 크기 (bytes)
     */
    public static AttachmentConfig optional(int maxFiles, long maxTotalSize) {
        return new AttachmentConfig(
                true,
                false,
                maxFiles,
                maxTotalSize,
                maxTotalSize / maxFiles,  // 개별 파일 크기 = 총 크기 / 개수
                List.of(),
                List.of()
        );
    }

    /**
     * 필수 첨부파일 설정.
     *
     * @param maxFiles     최대 파일 개수
     * @param maxTotalSize 최대 총 파일 크기 (bytes)
     */
    public static AttachmentConfig required(int maxFiles, long maxTotalSize) {
        return new AttachmentConfig(
                true,
                true,
                maxFiles,
                maxTotalSize,
                maxTotalSize / maxFiles,
                List.of(),
                List.of()
        );
    }

    /**
     * 첨부파일 카테고리.
     *
     * @param code        카테고리 코드
     * @param name        카테고리 이름
     * @param required    필수 여부
     * @param description 카테고리 설명
     */
    public record AttachmentCategory(
            String code,
            String name,
            boolean required,
            String description
    ) {
        public static AttachmentCategory of(String code, String name) {
            return new AttachmentCategory(code, name, false, null);
        }

        public static AttachmentCategory required(String code, String name) {
            return new AttachmentCategory(code, name, true, null);
        }
    }
}
