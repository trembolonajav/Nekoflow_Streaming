package com.nekoflow.backend.api.v1.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.nekoflow.backend.domain.entity.HomeSectionEntity;
import com.nekoflow.backend.domain.enums.EpisodeStatus;
import com.nekoflow.backend.domain.enums.HomeSectionMode;
import com.nekoflow.backend.domain.enums.VisibilityStatus;
import com.nekoflow.backend.domain.repository.AnimeRepository;
import com.nekoflow.backend.domain.repository.EpisodeRepository;
import com.nekoflow.backend.domain.repository.HeroConfigRepository;
import com.nekoflow.backend.domain.repository.HomeSectionRepository;

@ExtendWith(MockitoExtension.class)
class CatalogHomeTest {

    @Mock private AnimeRepository animeRepository;
    @Mock private EpisodeRepository episodeRepository;
    @Mock private HomeSectionRepository homeSectionRepository;
    @Mock private HeroConfigRepository heroConfigRepository;

    private CatalogQueryService service() {
        return new CatalogQueryService(animeRepository, episodeRepository, homeSectionRepository, heroConfigRepository);
    }

    @Test
    void recentSectionQueriesEpisodesWithLimitInsteadOfLoadingAll() {
        HomeSectionEntity recent = new HomeSectionEntity();
        recent.setCode("recent");
        recent.setTitle("Recentes");
        recent.setMode(HomeSectionMode.AUTOMATIC);
        recent.setActive(true);
        recent.setSortOrder(1);
        recent.setItems(List.of());

        when(heroConfigRepository.findAllByActiveTrueOrderBySortOrderAscIdAsc()).thenReturn(List.of());
        when(homeSectionRepository.findByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(recent));
        when(episodeRepository.findRecentPublished(eq(EpisodeStatus.PUBLISHED), eq(VisibilityStatus.PUBLISHED), any()))
            .thenReturn(List.of());

        service().getHome();

        // Home nao carrega todos os episodios: pede a pagina dos N mais recentes.
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(episodeRepository).findRecentPublished(eq(EpisodeStatus.PUBLISHED), eq(VisibilityStatus.PUBLISHED), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(60);
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(0);
    }
}
