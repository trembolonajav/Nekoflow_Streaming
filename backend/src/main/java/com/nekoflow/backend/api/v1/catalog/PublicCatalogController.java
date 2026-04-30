package com.nekoflow.backend.api.v1.catalog;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nekoflow.backend.api.v1.catalog.dto.AnimeDetailResponse;
import com.nekoflow.backend.api.v1.catalog.dto.AnimeSummaryResponse;
import com.nekoflow.backend.api.v1.catalog.dto.HomeResponse;
import com.nekoflow.backend.api.v1.catalog.dto.WatchPlayerResponse;
import com.nekoflow.backend.api.v1.common.dto.ApiPageResponse;

@RestController
@RequestMapping("/api/v1")
public class PublicCatalogController {

    private final CatalogQueryService catalogQueryService;

    public PublicCatalogController(CatalogQueryService catalogQueryService) {
        this.catalogQueryService = catalogQueryService;
    }

    @GetMapping("/home")
    public ResponseEntity<HomeResponse> home() {
        return ResponseEntity.ok(catalogQueryService.getHome());
    }

    @GetMapping("/animes")
    public ResponseEntity<ApiPageResponse<AnimeSummaryResponse>> listAnimes(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "") String q
    ) {
        List<AnimeSummaryResponse> items = catalogQueryService.listPublishedAnimes(q, size);
        return ResponseEntity.ok(new ApiPageResponse<>(items, items.size(), page, size));
    }

    @GetMapping("/animes/{slug}")
    public ResponseEntity<AnimeDetailResponse> animeBySlug(@PathVariable String slug) {
        return catalogQueryService.findPublishedAnimeBySlug(slug)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/watch/{slug}/{episodeNumber}")
    public ResponseEntity<WatchPlayerResponse> watch(
        @PathVariable String slug,
        @PathVariable Integer episodeNumber
    ) {
        return catalogQueryService.findPublishedEpisodePlayer(slug, episodeNumber)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
