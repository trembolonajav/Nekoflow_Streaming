package com.nekoflow.backend.api.v1.admin.dto;

import java.util.List;

public record AdminDashboardResponse(
    List<AdminDashboardMetricResponse> metrics,
    List<AdminDashboardPublicationResponse> recentPublications,
    List<AdminDashboardSectionResponse> homeSections,
    List<AdminCommentReportResponse> reports,
    List<AdminSuggestionResponse> suggestions,
    List<AdminDashboardHealthResponse> health
) {
}
