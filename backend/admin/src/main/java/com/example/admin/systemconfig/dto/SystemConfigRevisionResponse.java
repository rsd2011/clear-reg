package com.example.admin.systemconfig.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.UnaryOperator;

import com.example.admin.systemconfig.domain.SystemConfigRevision;
import com.example.common.version.ChangeAction;
import com.example.common.version.VersionStatus;

/**
 * 시스템 설정 리비전 응답 DTO (버전 이력 포함).
 */
public record SystemConfigRevisionResponse(
    UUID id,
    UUID rootId,
    String configCode,
    Integer version,
    OffsetDateTime validFrom,
    OffsetDateTime validTo,
    String yamlContent,
    boolean active,
    VersionStatus status,
    ChangeAction changeAction,
    String changeReason,
    String changedBy,
    String changedByName,
    OffsetDateTime changedAt,
    Integer rollbackFromVersion,
    String versionTag
) {
  public static SystemConfigRevisionResponse from(SystemConfigRevision revision) {
    return from(revision, UnaryOperator.identity());
  }

  public static SystemConfigRevisionResponse from(SystemConfigRevision revision, UnaryOperator<String> masker) {
    UnaryOperator<String> fn = masker == null ? UnaryOperator.identity() : masker;

    return new SystemConfigRevisionResponse(
        revision.getId(),
        revision.getRoot().getId(),
        revision.getRoot().getConfigCode(),
        revision.getVersion(),
        revision.getValidFrom(),
        revision.getValidTo(),
        revision.getYamlContent(),
        revision.isActive(),
        revision.getStatus(),
        revision.getChangeAction(),
        fn.apply(revision.getChangeReason()),
        fn.apply(revision.getChangedBy()),
        fn.apply(revision.getChangedByName()),
        revision.getChangedAt(),
        revision.getRollbackFromVersion(),
        revision.getVersionTag()
    );
  }
}
