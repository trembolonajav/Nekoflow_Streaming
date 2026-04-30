package com.nekoflow.backend.api.v1.suggestion;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nekoflow.backend.api.v1.admin.dto.AdminSuggestionResponse;
import com.nekoflow.backend.api.v1.notification.NotificationService;
import com.nekoflow.backend.api.v1.suggestion.dto.CreateSuggestionRequest;
import com.nekoflow.backend.domain.entity.AnimeSuggestionEntity;
import com.nekoflow.backend.domain.enums.NotificationSeverity;
import com.nekoflow.backend.domain.enums.NotificationType;
import com.nekoflow.backend.domain.enums.RelatedEntityType;
import com.nekoflow.backend.domain.enums.RoleCode;
import com.nekoflow.backend.domain.repository.AnimeSuggestionRepository;

@Service
public class SuggestionService {

    private final AnimeSuggestionRepository animeSuggestionRepository;
    private final NotificationService notificationService;

    public SuggestionService(AnimeSuggestionRepository animeSuggestionRepository, NotificationService notificationService) {
        this.animeSuggestionRepository = animeSuggestionRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public AdminSuggestionResponse create(CreateSuggestionRequest request) {
        AnimeSuggestionEntity suggestion = new AnimeSuggestionEntity();
        suggestion.setId(UUID.randomUUID());
        suggestion.setTitle(request.title().trim());
        suggestion.setVotes(1);
        suggestion.setStatus("NEW");
        suggestion.setNote(blankToNull(request.note()));

        AnimeSuggestionEntity saved = animeSuggestionRepository.save(suggestion);
        notifyCurators(saved);

        return new AdminSuggestionResponse(
            saved.getId().toString(),
            0,
            saved.getTitle(),
            saved.getVotes(),
            saved.getStatus(),
            saved.getNote(),
            saved.getCreatedAt() != null ? saved.getCreatedAt().toString() : null
        );
    }

    private void notifyCurators(AnimeSuggestionEntity suggestion) {
        for (RoleCode role : new RoleCode[] { RoleCode.ADMIN, RoleCode.EDITOR }) {
            notificationService.notifyRole(
                role,
                NotificationType.ADMIN,
                NotificationSeverity.INFO,
                "Nova sugestão recebida",
                "A comunidade sugeriu \"" + suggestion.getTitle() + "\" para o catálogo.",
                RelatedEntityType.SUGGESTION,
                suggestion.getId(),
                "/admin/sugestoes"
            );
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
