package com.nekoflow.backend.api.v1.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nekoflow.backend.api.v1.admin.dto.AdminCommentReportResponse;
import com.nekoflow.backend.api.v1.admin.dto.AdminDashboardResponse;
import com.nekoflow.backend.api.v1.admin.dto.AdminSuggestionResponse;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminOperationsController {

    private final AdminOperationsService adminOperationsService;

    public AdminOperationsController(AdminOperationsService adminOperationsService) {
        this.adminOperationsService = adminOperationsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> dashboard() {
        return ResponseEntity.ok(adminOperationsService.getDashboard());
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<AdminSuggestionResponse>> suggestions() {
        return ResponseEntity.ok(adminOperationsService.listSuggestions());
    }

    @PatchMapping("/suggestions/{id}/status/{status}")
    public ResponseEntity<AdminSuggestionResponse> updateSuggestionStatus(
        @PathVariable UUID id,
        @PathVariable String status
    ) {
        return ResponseEntity.ok(adminOperationsService.updateSuggestionStatus(id, status));
    }

    @PostMapping("/suggestions/{id}/convert-to-anime")
    public ResponseEntity<AdminSuggestionResponse> convertSuggestion(@PathVariable UUID id) {
        return ResponseEntity.ok(adminOperationsService.convertSuggestionToAnime(id));
    }

    @GetMapping("/reports")
    public ResponseEntity<List<AdminCommentReportResponse>> reports() {
        return ResponseEntity.ok(adminOperationsService.listReports());
    }

    @PatchMapping("/reports/{id}/status/{status}")
    public ResponseEntity<AdminCommentReportResponse> updateReportStatus(
        @PathVariable UUID id,
        @PathVariable String status
    ) {
        return ResponseEntity.ok(adminOperationsService.updateReportStatus(id, status));
    }

    @PatchMapping("/comments/{commentId}/visibility/{visibility}")
    public ResponseEntity<AdminCommentReportResponse> updateCommentVisibility(
        @PathVariable UUID commentId,
        @PathVariable String visibility
    ) {
        return ResponseEntity.ok(adminOperationsService.updateCommentVisibility(commentId, visibility));
    }
}
