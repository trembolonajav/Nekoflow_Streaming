package com.nekoflow.backend.api.v1.suggestion;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nekoflow.backend.api.v1.admin.dto.AdminSuggestionResponse;
import com.nekoflow.backend.api.v1.suggestion.dto.CreateSuggestionRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/suggestions")
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @PostMapping
    public ResponseEntity<AdminSuggestionResponse> create(@Valid @RequestBody CreateSuggestionRequest request) {
        return ResponseEntity.ok(suggestionService.create(request));
    }
}
