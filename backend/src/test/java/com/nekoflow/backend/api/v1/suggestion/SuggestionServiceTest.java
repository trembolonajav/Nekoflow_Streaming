package com.nekoflow.backend.api.v1.suggestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nekoflow.backend.api.v1.notification.NotificationService;
import com.nekoflow.backend.api.v1.suggestion.dto.CreateSuggestionRequest;
import com.nekoflow.backend.domain.entity.AnimeSuggestionEntity;
import com.nekoflow.backend.domain.repository.AnimeSuggestionRepository;

@ExtendWith(MockitoExtension.class)
class SuggestionServiceTest {

    @Mock private AnimeSuggestionRepository repository;
    @Mock private NotificationService notificationService;
    @InjectMocks private SuggestionService service;

    private AnimeSuggestionEntity captureSaved() {
        ArgumentCaptor<AnimeSuggestionEntity> captor = ArgumentCaptor.forClass(AnimeSuggestionEntity.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void createAlwaysStartsPendingAndTrimsTitle() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new CreateSuggestionRequest("  One Piece  ", "  nota  "));

        AnimeSuggestionEntity saved = captureSaved();
        assertThat(saved.getStatus()).isEqualTo("NEW"); // status inicial fixado pelo servidor
        assertThat(saved.getTitle()).isEqualTo("One Piece");
        assertThat(saved.getVotes()).isEqualTo(1);
        assertThat(saved.getNote()).isEqualTo("nota");
    }

    @Test
    void htmlInTitleIsStoredLiterallyNotSanitizedIntoMarkup() {
        // O backend guarda o texto como dado; a renderizacao segura (escape) e do
        // frontend (React). O importante e nao alterar/interpretar como markup aqui.
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new CreateSuggestionRequest("<script>alert(1)</script>", null));

        assertThat(captureSaved().getTitle()).isEqualTo("<script>alert(1)</script>");
    }

    @Test
    void blankNoteBecomesNull() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new CreateSuggestionRequest("Naruto", "   "));

        assertThat(captureSaved().getNote()).isNull();
    }
}
