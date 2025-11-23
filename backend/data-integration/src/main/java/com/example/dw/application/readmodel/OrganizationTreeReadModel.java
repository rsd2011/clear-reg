package com.example.dw.application.readmodel;

import java.time.OffsetDateTime;
import java.util.List;

import com.example.dw.application.DwOrganizationNode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Serialized representation of the DW 조직 트리를 Redis 등 외부 저장소에 저장하기 위한 DTO.
 */
public record OrganizationTreeReadModel(
        @JsonProperty("version") String version,
        @JsonProperty("generatedAt") OffsetDateTime generatedAt,
        @JsonProperty("nodes") List<DwOrganizationNode> nodes
) {

    @JsonCreator
    public OrganizationTreeReadModel {
    }
}
