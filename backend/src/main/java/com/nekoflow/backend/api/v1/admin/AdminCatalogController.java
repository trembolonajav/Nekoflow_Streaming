package com.nekoflow.backend.api.v1.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.nekoflow.backend.api.v1.admin.dto.AdminAnimeResponse;
import com.nekoflow.backend.api.v1.admin.dto.AdminAnimeUpsertRequest;
import com.nekoflow.backend.api.v1.admin.dto.AdminAniListSearchResponse;
import com.nekoflow.backend.api.v1.admin.dto.AdminEpisodeResponse;
import com.nekoflow.backend.api.v1.admin.dto.AdminEpisodeUpsertRequest;
import com.nekoflow.backend.api.v1.admin.dto.AdminHeroRequest;
import com.nekoflow.backend.api.v1.admin.dto.AdminHeroResponse;
import com.nekoflow.backend.api.v1.admin.dto.AdminHomeResponse;
import com.nekoflow.backend.api.v1.admin.dto.AdminHomeSectionRequest;
import com.nekoflow.backend.api.v1.common.dto.ApiMessageResponse;
import com.nekoflow.backend.api.v1.common.dto.ApiPageResponse;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/v1/admin")
public class AdminCatalogController {

    private final AdminCatalogService adminCatalogService;
    private final AniListIntegrationService aniListIntegrationService;

    public AdminCatalogController(
        AdminCatalogService adminCatalogService,
        AniListIntegrationService aniListIntegrationService
    ) {
        this.adminCatalogService = adminCatalogService;
        this.aniListIntegrationService = aniListIntegrationService;
    }

    @GetMapping("/animes")
    public ResponseEntity<ApiPageResponse<AdminAnimeResponse>> listAnimes() {
        java.util.List<AdminAnimeResponse> items = adminCatalogService.listAnimes();
        return ResponseEntity.ok(new ApiPageResponse<>(items, items.size(), 0, 20));
    }

    @GetMapping("/anilist/search")
    public ResponseEntity<?> searchAniList(
        @org.springframework.web.bind.annotation.RequestParam(name = "q") String query
    ) {
        try {
            return ResponseEntity.ok(aniListIntegrationService.search(query));
        } catch (ResponseStatusException exception) {
            return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(java.util.Map.of("message", exception.getReason() == null ? "Falha ao consultar AniList." : exception.getReason()));
        }
    }

    @PostMapping("/animes")
    public ResponseEntity<AdminAnimeResponse> createAnime(@Valid @RequestBody AdminAnimeUpsertRequest request) {
        return ResponseEntity.status(201).body(adminCatalogService.createAnime(request));
    }

    @PutMapping("/animes/{id}")
    public ResponseEntity<AdminAnimeResponse> updateAnime(
        @PathVariable String id,
        @Valid @RequestBody AdminAnimeUpsertRequest request
    ) {
        return ResponseEntity.ok(adminCatalogService.updateAnime(java.util.UUID.fromString(id), request));
    }

    @PatchMapping("/animes/{id}/visibility/{visibility}")
    public ResponseEntity<AdminAnimeResponse> updateAnimeVisibility(
        @PathVariable String id,
        @PathVariable String visibility
    ) {
        return ResponseEntity.ok(adminCatalogService.updateAnimeVisibility(java.util.UUID.fromString(id), visibility));
    }

    @DeleteMapping("/animes/{id}")
    public ResponseEntity<ApiMessageResponse> deleteAnime(@PathVariable String id) {
        adminCatalogService.deleteAnime(java.util.UUID.fromString(id));
        return ResponseEntity.ok(new ApiMessageResponse("Anime deleted successfully."));
    }

    @GetMapping("/episodes")
    public ResponseEntity<ApiPageResponse<AdminEpisodeResponse>> listEpisodes() {
        java.util.List<AdminEpisodeResponse> items = adminCatalogService.listEpisodes();
        return ResponseEntity.ok(new ApiPageResponse<>(items, items.size(), 0, 20));
    }

    @PostMapping("/episodes")
    public ResponseEntity<AdminEpisodeResponse> createEpisode(@Valid @RequestBody AdminEpisodeUpsertRequest request) {
        return ResponseEntity.status(201).body(adminCatalogService.createEpisode(request));
    }

    @PutMapping("/episodes/{id}")
    public ResponseEntity<AdminEpisodeResponse> updateEpisode(
        @PathVariable String id,
        @Valid @RequestBody AdminEpisodeUpsertRequest request
    ) {
        return ResponseEntity.ok(adminCatalogService.updateEpisode(java.util.UUID.fromString(id), request));
    }

    @DeleteMapping("/episodes/{id}")
    public ResponseEntity<ApiMessageResponse> deleteEpisode(@PathVariable String id) {
        adminCatalogService.deleteEpisode(java.util.UUID.fromString(id));
        return ResponseEntity.ok(new ApiMessageResponse("Episode deleted successfully."));
    }

    @GetMapping("/home")
    public ResponseEntity<AdminHomeResponse> getHomeConfig() {
        return ResponseEntity.ok(adminCatalogService.getHomeConfig());
    }

    @PutMapping("/home/sections")
    public ResponseEntity<AdminHomeResponse> updateSections(
        @Valid @RequestBody java.util.List<AdminHomeSectionRequest> request
    ) {
        return ResponseEntity.ok(adminCatalogService.updateSections(request));
    }

    @PutMapping("/home/hero")
    public ResponseEntity<AdminHeroResponse> updateHero(@Valid @RequestBody AdminHeroRequest request) {
        return ResponseEntity.ok(adminCatalogService.updateHero(request));
    }
}
