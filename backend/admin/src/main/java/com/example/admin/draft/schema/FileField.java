package com.example.admin.draft.schema;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * 파일 업로드 필드.
 * <p>
 * 단일 또는 다중 파일 업로드를 지원합니다.
 * MIME 타입, 파일 크기, 개수 제한을 설정할 수 있습니다.
 * </p>
 *
 * @param name           필드 고유 식별자
 * @param label          필드 라벨
 * @param required       필수 여부
 * @param description    필드 설명
 * @param acceptedTypes  허용 MIME 타입 목록 (예: image/*, application/pdf)
 * @param maxFileSize    최대 파일 크기 (bytes)
 * @param maxFiles       최대 파일 개수
 * @param multiple       다중 파일 허용 여부
 */
@JsonTypeName("file")
public record FileField(
        String name,
        String label,
        boolean required,
        String description,
        List<String> acceptedTypes,
        Long maxFileSize,
        Integer maxFiles,
        boolean multiple
) implements FormField {

    public FileField {
        if (acceptedTypes == null) {
            acceptedTypes = List.of();
        }
    }

    /**
     * 간편 생성자.
     */
    public static FileField of(String name, String label, boolean required) {
        return new FileField(name, label, required, null, List.of(), null, 1, false);
    }

    /**
     * 다중 파일 필드 생성.
     */
    public static FileField multiple(String name, String label, boolean required, int maxFiles) {
        return new FileField(name, label, required, null, List.of(), null, maxFiles, true);
    }

    /**
     * 이미지 전용 필드 생성.
     */
    public static FileField image(String name, String label, boolean required) {
        return new FileField(name, label, required, null, List.of("image/*"), null, 1, false);
    }

    /**
     * 모든 파일 타입 허용 필드 생성.
     */
    public static FileField any(String name, String label, boolean required) {
        return new FileField(name, label, required, null, List.of(), null, 1, false);
    }

    /**
     * PDF 전용 필드 생성.
     */
    public static FileField pdf(String name, String label, boolean required) {
        return new FileField(name, label, required, null, List.of("application/pdf"), null, 1, false);
    }

    /**
     * 문서 파일 필드 생성 (PDF, Word, Excel, PPT).
     */
    public static FileField documents(String name, String label, boolean required) {
        return new FileField(name, label, required, null, List.of(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        ), null, 1, false);
    }

    @Override
    public String type() {
        return "file";
    }
}
