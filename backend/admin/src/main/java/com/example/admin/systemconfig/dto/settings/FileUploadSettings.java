package com.example.admin.systemconfig.dto.settings;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 파일 업로드 관련 설정.
 * <p>
 * 설정 코드: file.settings
 * </p>
 */
public record FileUploadSettings(
    /** 최대 파일 크기 (bytes) */
    @JsonProperty(defaultValue = "20971520")
    long maxFileSizeBytes,

    /** 허용된 파일 확장자 목록 */
    List<String> allowedFileExtensions,

    /** 엄격한 MIME 타입 검증 활성화 */
    @JsonProperty(defaultValue = "true")
    boolean strictMimeValidation,

    /** 파일 보관 기간 (일) */
    @JsonProperty(defaultValue = "365")
    int fileRetentionDays,

    /** 바이러스 스캔 활성화 */
    @JsonProperty(defaultValue = "false")
    boolean virusScanEnabled,

    /** 이미지 자동 리사이즈 활성화 */
    @JsonProperty(defaultValue = "false")
    boolean imageResizeEnabled,

    /** 이미지 최대 너비 (픽셀) */
    @JsonProperty(defaultValue = "1920")
    int imageMaxWidth,

    /** 이미지 최대 높이 (픽셀) */
    @JsonProperty(defaultValue = "1080")
    int imageMaxHeight,

    /** 썸네일 생성 활성화 */
    @JsonProperty(defaultValue = "true")
    boolean thumbnailGenerationEnabled,

    /** 썸네일 크기 (픽셀) */
    @JsonProperty(defaultValue = "150")
    int thumbnailSize,

    /** 임시 파일 정리 주기 (시간) */
    @JsonProperty(defaultValue = "24")
    int tempFileCleanupHours,

    /** 저장소 타입 (LOCAL, S3, AZURE_BLOB 등) */
    @JsonProperty(defaultValue = "LOCAL")
    String storageType
) {
  private static final long DEFAULT_MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB

  /** 기본값으로 정규화 */
  public FileUploadSettings {
    if (maxFileSizeBytes <= 0) {
      maxFileSizeBytes = DEFAULT_MAX_FILE_SIZE;
    }
    allowedFileExtensions = allowedFileExtensions == null ? List.of() : allowedFileExtensions.stream()
        .map(ext -> ext.toLowerCase())
        .distinct()
        .toList();
    if (fileRetentionDays < 0) {
      fileRetentionDays = 365;
    }
    if (imageMaxWidth < 1) {
      imageMaxWidth = 1920;
    }
    if (imageMaxHeight < 1) {
      imageMaxHeight = 1080;
    }
    if (thumbnailSize < 1) {
      thumbnailSize = 150;
    }
    if (tempFileCleanupHours < 1) {
      tempFileCleanupHours = 24;
    }
    if (storageType == null || storageType.isBlank()) {
      storageType = "LOCAL";
    }
  }

  /** 기본 설정 생성 */
  public static FileUploadSettings defaults() {
    return new FileUploadSettings(
        DEFAULT_MAX_FILE_SIZE,
        List.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "jpg", "jpeg", "png", "gif"),
        true, 365, false, false, 1920, 1080, true, 150, 24, "LOCAL"
    );
  }
}
