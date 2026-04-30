package com.nekoflow.backend.api.v1.catalog;

import java.util.List;

import com.nekoflow.backend.api.v1.catalog.dto.AnimeDetailResponse;
import com.nekoflow.backend.api.v1.catalog.dto.AnimeEpisodeSummaryResponse;
import com.nekoflow.backend.api.v1.catalog.dto.AnimeSummaryResponse;
import com.nekoflow.backend.api.v1.catalog.dto.HeroBlockResponse;
import com.nekoflow.backend.api.v1.catalog.dto.HomeResponse;
import com.nekoflow.backend.api.v1.catalog.dto.HomeSectionItemResponse;
import com.nekoflow.backend.api.v1.catalog.dto.HomeSectionResponse;
import com.nekoflow.backend.api.v1.catalog.dto.WatchPlayerResponse;

public final class CatalogStubData {

    private CatalogStubData() {
    }

    public static AnimeSummaryResponse animeSummary() {
        return new AnimeSummaryResponse(
            "a1",
            "frieren-beyond-journeys-end",
            154587L,
            "Frieren: Beyond Journey's End",
            "Sousou no Frieren",
            "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587.jpg",
            "https://s4.anilist.co/file/anilistcdn/media/anime/banner/154587.jpg",
            "SERIES",
            "RELEASING",
            "PUBLISHED",
            2024
        );
    }

    public static AnimeDetailResponse animeDetail() {
        return new AnimeDetailResponse(
            "a1",
            "frieren-beyond-journeys-end",
            154587L,
            "Frieren: Beyond Journey's End",
            "Sousou no Frieren",
            "葬送のフリーレン",
            "Frieren: Beyond Journey's End",
            "Apos derrotar o Rei Demonio, Frieren parte em uma jornada marcada por memoria, tempo e reencontro.",
            "SERIES",
            "RELEASING",
            "PUBLISHED",
            "Winter 2024",
            2024,
            "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587.jpg",
            "https://s4.anilist.co/file/anilistcdn/media/anime/banner/154587.jpg",
            "Madhouse",
            List.of("Fantasy", "Drama", "Adventure"),
            List.of(
                new AnimeEpisodeSummaryResponse(
                    "ep-1",
                    1,
                    "The Journey's End",
                    "PUBLISHED",
                    "https://img.anili.st/media/154587",
                    1440
                ),
                new AnimeEpisodeSummaryResponse(
                    "ep-2",
                    2,
                    "It Didn't Have to Be Magic",
                    "PUBLISHED",
                    "https://img.anili.st/media/154587?ep=2",
                    1440
                )
            )
        );
    }

    public static HomeResponse home() {
        HomeSectionItemResponse hero = new HomeSectionItemResponse(
            "hero-1",
            "a1",
            null,
            "Frieren: Beyond Journey's End",
            "Destaque editorial",
            "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587.jpg",
            "https://s4.anilist.co/file/anilistcdn/media/anime/banner/154587.jpg",
            null,
            "frieren-beyond-journeys-end"
        );

        return new HomeResponse(
            new HeroBlockResponse("Destaque editorial", "Assistir agora", List.of(hero)),
            List.of(
                new HomeSectionResponse(
                    "continue",
                    "Continuar assistindo",
                    "AUTOMATIC",
                    true,
                    List.of(hero)
                ),
                new HomeSectionResponse(
                    "season",
                    "Novidades da temporada",
                    "HYBRID",
                    true,
                    List.of(hero)
                )
            )
        );
    }

    public static WatchPlayerResponse watchPlayer() {
        return new WatchPlayerResponse(
            "frieren-beyond-journeys-end",
            "Frieren: Beyond Journey's End",
            "ep-1",
            1,
            "The Journey's End",
            "Primeiro episodio publicado no catalogo da plataforma.",
            "https://img.anili.st/media/154587",
            "SEEKSTREAMING",
            "https://seekstreaming.com/embed/frieren-ep-1",
            "https://seekstreaming.com/player/frieren-ep-1",
            1440
        );
    }
}
