package com.nekoflow.backend.api.v1.catalog.seo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.nekoflow.backend.api.v1.catalog.CatalogQueryService;
import com.nekoflow.backend.api.v1.catalog.dto.AnimeDetailResponse;
import com.nekoflow.backend.api.v1.catalog.dto.AnimeEpisodeSummaryResponse;
import com.nekoflow.backend.api.v1.catalog.dto.WatchPlayerResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SeoMetadataServiceTest {

    @Mock private CatalogQueryService catalog;

    private SeoMetadataService service() {
        return new SeoMetadataService(catalog);
    }

    private AnimeDetailResponse anime(String slug, String title, String synopsis) {
        return anime(slug, title, synopsis, null, null);
    }

    private AnimeDetailResponse anime(String slug, String title, String synopsis, String coverUrl, String bannerUrl) {
        return new AnimeDetailResponse(null, slug, null, title, "Sousou no Frieren", null, "Frieren", synopsis,
            null, null, null, null, null, coverUrl, bannerUrl, null, List.of("Aventura", "Drama"),
            List.of(new AnimeEpisodeSummaryResponse("ep-1", 1, "Ep 1", "PUBLISHED", null, null)));
    }

    private WatchPlayerResponse player(String animeTitle, int number, String summary) {
        return player(animeTitle, number, summary, null);
    }

    private WatchPlayerResponse player(String animeTitle, int number, String summary, String thumbnailUrl) {
        return new WatchPlayerResponse("frieren", animeTitle, "ep-1", number, "Ep title", summary,
            thumbnailUrl, null, "https://player.test/embed/frieren/1", null, 1440);
    }

    @Test
    void homeIsIndexableWithDefaultTitle() {
        SeoMeta meta = service().metadataFor("/");
        assertThat(meta.noindex()).isFalse();
        assertThat(meta.title()).isEqualTo(SeoMetadataService.DEFAULT_TITLE);
        assertThat(meta.canonicalPath()).isEqualTo("/");
    }

    @Test
    void explorarIsIndexable() {
        SeoMeta meta = service().metadataFor("/explorar");
        assertThat(meta.noindex()).isFalse();
        assertThat(meta.title()).contains("Explorar");
    }

    @Test
    void animeUsesRealTitleAndSynopsis() {
        when(catalog.findPublishedAnimeBySlug("frieren"))
            .thenReturn(Optional.of(anime("frieren", "Frieren", "Apos derrotar o Rei Demonio, Frieren viaja.")));

        SeoMeta meta = service().metadataFor("/anime/frieren");

        assertThat(meta.noindex()).isFalse();
        assertThat(meta.title()).isEqualTo("Frieren | Nekoflow");
        assertThat(meta.description()).contains("Frieren viaja");
        assertThat(meta.canonicalPath()).isEqualTo("/anime/frieren");
        assertThat(meta.ogType()).isEqualTo("video.tv_show");
        assertThat(meta.structuredDataKind()).isEqualTo("anime");
        assertThat(meta.alternateNames()).contains("Sousou no Frieren");
        assertThat(meta.genres()).contains("Aventura", "Drama");
        assertThat(meta.numberOfEpisodes()).isEqualTo(1);
    }

    @Test
    void animeUsesBannerOrCoverForSocialImage() {
        when(catalog.findPublishedAnimeBySlug("frieren"))
            .thenReturn(Optional.of(anime("frieren", "Frieren", "x", "https://img.test/cover.jpg", "https://img.test/banner.jpg")));

        SeoMeta meta = service().metadataFor("/anime/frieren");

        assertThat(meta.imageUrl()).isEqualTo("https://img.test/banner.jpg");
        assertThat(meta.socialEnabled()).isTrue();
    }

    @Test
    void animeWithoutImageOrSynopsisUsesFallbacks() {
        when(catalog.findPublishedAnimeBySlug("frieren"))
            .thenReturn(Optional.of(anime("frieren", "Frieren", "", null, null)));

        SeoMeta meta = service().metadataFor("/anime/frieren");

        assertThat(meta.description()).isEqualTo(SeoMetadataService.DEFAULT_DESCRIPTION);
        assertThat(meta.imageUrl()).isEqualTo(SeoMetadataService.DEFAULT_IMAGE_PATH);
    }

    @Test
    void unknownAnimeIsNoindex() {
        when(catalog.findPublishedAnimeBySlug("naoexiste")).thenReturn(Optional.empty());
        assertThat(service().metadataFor("/anime/naoexiste").noindex()).isTrue();
    }

    @Test
    void watchUsesEpisodeDataAndIsIndexable() {
        when(catalog.findPublishedEpisodePlayer(eq("frieren"), eq(1)))
            .thenReturn(Optional.of(player("Frieren", 1, "Resumo do episodio 1.")));

        SeoMeta meta = service().metadataFor("/watch/frieren/1");

        assertThat(meta.noindex()).isFalse();
        assertThat(meta.title()).contains("Frieren").contains("Episodio 1");
        assertThat(meta.ogType()).isEqualTo("video.episode");
        assertThat(meta.structuredDataKind()).isEqualTo("episode");
        assertThat(meta.episodeNumber()).isEqualTo(1);
        assertThat(meta.embedUrl()).isEqualTo("https://player.test/embed/frieren/1");
        assertThat(meta.durationSeconds()).isEqualTo(1440);
    }

    @Test
    void watchUsesThumbnailAndAnimeFallbackDescription() {
        when(catalog.findPublishedEpisodePlayer(eq("frieren"), eq(1)))
            .thenReturn(Optional.of(player("Frieren", 1, "", "https://img.test/thumb.jpg")));
        when(catalog.findPublishedAnimeBySlug("frieren"))
            .thenReturn(Optional.of(anime("frieren", "Frieren", "Sinopse do anime.", "https://img.test/cover.jpg", null)));

        SeoMeta meta = service().metadataFor("/watch/frieren/1");

        assertThat(meta.description()).isEqualTo("Sinopse do anime.");
        assertThat(meta.imageUrl()).isEqualTo("https://img.test/thumb.jpg");
    }

    @Test
    void unknownEpisodeIsNoindex() {
        when(catalog.findPublishedEpisodePlayer(eq("frieren"), eq(99))).thenReturn(Optional.empty());
        assertThat(service().metadataFor("/watch/frieren/99").noindex()).isTrue();
    }

    @Test
    void privateRoutesAreNoindex() {
        SeoMetadataService s = service();
        assertThat(s.metadataFor("/perfil").noindex()).isTrue();
        assertThat(s.metadataFor("/entrar").noindex()).isTrue();
        assertThat(s.metadataFor("/notificacoes").noindex()).isTrue();
        assertThat(s.metadataFor("/admin/animes").noindex()).isTrue();
        assertThat(s.metadataFor("/entrar").socialEnabled()).isFalse();
    }

    @Test
    void unknownRouteIsNoindex() {
        assertThat(service().metadataFor("/rota-inexistente").noindex()).isTrue();
    }

    @Test
    void normalizesQueryTrailingSlashAndHash() {
        when(catalog.findPublishedAnimeBySlug("frieren"))
            .thenReturn(Optional.of(anime("frieren", "Frieren", "x")));

        assertThat(service().metadataFor("/anime/frieren/").title()).isEqualTo("Frieren | Nekoflow");
        assertThat(service().metadataFor("/anime/frieren?ref=x").title()).isEqualTo("Frieren | Nekoflow");
    }
}
