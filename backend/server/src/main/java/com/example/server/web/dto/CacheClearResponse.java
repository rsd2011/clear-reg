package com.example.server.web.dto;

import java.util.List;

public record CacheClearResponse(List<String> clearedCaches) {
}
