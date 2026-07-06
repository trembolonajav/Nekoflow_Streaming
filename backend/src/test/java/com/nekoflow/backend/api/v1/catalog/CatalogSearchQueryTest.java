package com.nekoflow.backend.api.v1.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import com.nekoflow.backend.api.v1.catalog.dto.AnimeSummaryProjection;
import com.nekoflow.backend.api.v1.catalog.dto.AnimeSummaryResponse;
import com.nekoflow.backend.domain.enums.AnimeStatus;
import com.nekoflow.backend.domain.enums.AnimeType;
import com.nekoflow.backend.domain.enums.VisibilityStatus;
import com.nekoflow.backend.domain.repository.AnimeRepository;
import com.nekoflow.backend.domain.repository.EpisodeRepository;
import com.nekoflow.backend.domain.repository.HeroConfigRepository;
import com.nekoflow.backend.domain.repository.HomeSectionRepository;

@ExtendWith(MockitoExtension.class)
class CatalogSearchQueryTest {

    @Mock private AnimeRepository animeRepository;
    @Mock private EpisodeRepository episodeRepository;
    @Mock private HomeSectionRepository homeSectionRepository;
    @Mock private HeroConfigRepository heroConfigRepository;

    private CatalogQueryService service() {
        return new CatalogQueryService(animeRepository, episodeRepository, homeSectionRepository, heroConfigRepository);
    }

    private void mockEmptySlice() {
        Slice<AnimeSummaryProjection> empty = new SliceImpl<>(List.of());
        when(animeRepository.searchPublished(any(), any(), any(), any())).thenReturn(empty);
    }

    @Test
    void emptyQueryDelegatesToDbWithBlankNormalizedQuery() {
        mockEmptySlice();
        assertThat(service().listPublishedAnimes("", 0, 20)).isEmpty();

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(animeRepository).searchPublished(eq(VisibilityStatus.PUBLISHED), query.capture(), any(), any());
        assertThat(query.getValue()).isEmpty();
    }

    @Test
    void nullQueryIsHandledSafely() {
        mockEmptySlice();
        assertThatCode(() -> service().listPublishedAnimes(null, 0, 20)).doesNotThrowAnyException();
    }

    @Test
    void unicodeAndEmojiQueryIsNormalizedWithoutError() {
        mockEmptySlice();
        assertThatCode(() -> service().listPublishedAnimes("フリーレン 😀 ção", 0, 20))
            .doesNotThrowAnyException();
    }

    @Test
    void giantQueryIsCappedBeforeReachingTheDatabase() {
        mockEmptySlice();
        service().listPublishedAnimes("a".repeat(100_000), 0, 20);

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(animeRepository).searchPublished(any(), query.capture(), any(), any());
        // Cap de 100 chars aplicado antes de ir ao banco.
        assertThat(query.getValue().length()).isLessThanOrEqualTo(100);
    }

    @Test
    void mapsEpisodeCountFromProjectionWithoutN1() {
        AnimeSummaryProjection projection = new AnimeSummaryProjection(
            UUID.randomUUID(), "frieren", 123L, "Frieren", "Frieren", "sinopse",
            "cover", "banner", AnimeType.SERIES, AnimeStatus.FINISHED, VisibilityStatus.PUBLISHED,
            "Fall 2023", 2023, "Madhouse", new BigDecimal("9.1"), "Aventura\nFantasia", 12L
        );
        when(animeRepository.searchPublished(any(), any(), any(), any()))
            .thenReturn(new SliceImpl<>(List.of(projection)));

        List<AnimeSummaryResponse> result = service().listPublishedAnimes("frieren", 0, 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).episodesCount()).isEqualTo(12);
        assertThat(result.get(0).genres()).containsExactly("Aventura", "Fantasia");
        // A contagem veio na projecao agregada: nenhum acesso ao repositorio de episodios.
        verifyNoInteractions(episodeRepository);
    }

    @Test
    void pageSizeIsCappedAtHundred() {
        mockEmptySlice();
        service().listPublishedAnimes("", 0, 9999);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(animeRepository).searchPublished(any(), any(), any(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isLessThanOrEqualTo(100);
    }
}
