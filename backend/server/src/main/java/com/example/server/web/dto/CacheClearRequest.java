package com.example.server.web.dto;

import java.util.List;

public record CacheClearRequest(List<String> caches) {
}
