package com.nekoflow.backend.api.v1.auth.dto;

import java.util.List;

public record AuthMeResponse(
    String id,
    String name,
    String email,
    List<String> roles,
    String provider
) {
}
