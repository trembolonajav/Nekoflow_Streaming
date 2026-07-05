package com.nekoflow.backend.api.v1.catalog.seo;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.nekoflow.backend.api.v1.catalog.CatalogQueryService;
import com.nekoflow.backend.api.v1.catalog.dto.AnimeDetailResponse;
import com.nekoflow.backend.api.v1.catalog.dto.WatchPlayerResponse;

/**
 * Calcula o <head> (title/description/canonical/robots) de cada rota publica a
 * partir do conteudo REAL do catalogo. Rotas privadas -> noindex. Rotas nao
 * encontradas -> noindex. Sem inventar dados: usa titulo/sinopse reais.
 */
@Service
public class SeoMetadataService {

    static final String SITE_NAME = "Nekoflow";
    static final String DEFAULT_TITLE = "Nekoflow - Streaming de anime premium";
    static final String DEFAULT_IMAGE_PATH = "/favicon.png";
    static final String DEFAULT_DESCRIPTION =
        "Nekoflow e uma plataforma de streaming de anime com catalogo organizado, lancamentos da temporada e curadoria.";
    private static final int MAX_DESCRIPTION = 160;

    private final CatalogQueryService catalog;

    public SeoMetadataService(CatalogQueryService catalog) {
        this.catalog = catalog;
    }

    public SeoMeta metadataFor(String rawPath) {
        String path = normalizePath(rawPath);

        if (isPrivate(path)) {
            return noindex(titled(privateTitle(path)), DEFAULT_DESCRIPTION, path);
        }

        switch (path) {
            case "/":
                return index(DEFAULT_TITLE, DEFAULT_DESCRIPTION, "/", "website", DEFAULT_IMAGE_PATH, "home");
            case "/explorar":
                return index(titled("Explorar animes"),
                    "Explore o catalogo de animes da Nekoflow por titulo, genero, ano e status.",
                    path, "website", DEFAULT_IMAGE_PATH, "generic");
            case "/calendario":
                return index(titled("Calendario de lancamentos"),
                    "Agenda semanal de episodios e lancamentos publicados na Nekoflow.",
                    path, "website", DEFAULT_IMAGE_PATH, "generic");
            case "/termos-de-uso":
                return index(titled("Termos de uso"), "Termos de uso da plataforma Nekoflow.",
                    path, "website", DEFAULT_IMAGE_PATH, "generic");
            case "/politica-de-privacidade":
                return index(titled("Politica de privacidade"),
                    "Como a Nekoflow trata dados e privacidade dos usuarios.",
                    path, "website", DEFAULT_IMAGE_PATH, "generic");
            default:
                break;
        }

        if (path.startsWith("/anime/")) {
            return animeMeta(path);
        }
        if (path.startsWith("/watch/")) {
            return watchMeta(path);
        }

        // Rota desconhecida -> nao indexar.
        return noindex(titled("Pagina nao encontrada"), DEFAULT_DESCRIPTION, path);
    }

    private SeoMeta animeMeta(String path) {
        String slug = path.substring("/anime/".length());
        return catalog.findPublishedAnimeBySlug(slug)
            .map(anime -> new SeoMeta(
                titled(anime.titleDisplay()),
                description(anime.synopsis()),
                path,
                false,
                true,
                "video.tv_show",
                firstNonBlank(anime.bannerUrl(), anime.coverUrl(), DEFAULT_IMAGE_PATH),
                "anime",
                alternateNames(anime),
                safeList(anime.genres()),
                anime.episodes() == null ? null : anime.episodes().size(),
                null,
                null,
                null,
                null,
                null))
            .orElseGet(() -> noindex(titled("Anime"), DEFAULT_DESCRIPTION, path));
    }

    private SeoMeta watchMeta(String path) {
        // /watch/{slug}/{episodeNumber}
        String rest = path.substring("/watch/".length());
        int slash = rest.indexOf('/');
        if (slash <= 0 || slash == rest.length() - 1) {
            return noindex(titled("Assistir episodio"), DEFAULT_DESCRIPTION, path);
        }
        String slug = rest.substring(0, slash);
        Integer episodeNumber = parseInt(rest.substring(slash + 1));
        if (episodeNumber == null) {
            return noindex(titled("Assistir episodio"), DEFAULT_DESCRIPTION, path);
        }
        return catalog.findPublishedEpisodePlayer(slug, episodeNumber)
            .map(player -> watchMetaWithPlayer(path, slug, player))
            .orElseGet(() -> noindex(titled("Assistir episodio"), DEFAULT_DESCRIPTION, path));
    }

    private SeoMeta watchMetaWithPlayer(String path, String slug, WatchPlayerResponse player) {
        var anime = catalog.findPublishedAnimeBySlug(slug);
        String fallbackDescription = anime.map(value -> value.synopsis()).orElse("Assista " + player.animeTitle()
            + " episodio " + player.episodeNumber() + " na Nekoflow.");
        String image = firstNonBlank(
            player.thumbnailUrl(),
            anime.map(value -> value.bannerUrl()).orElse(null),
            anime.map(value -> value.coverUrl()).orElse(null),
            DEFAULT_IMAGE_PATH);

        return new SeoMeta(
            titled(player.animeTitle() + " - Episodio " + player.episodeNumber()),
            description(firstNonBlank(player.summary(), fallbackDescription)),
            path,
            false,
            true,
            "video.episode",
            image,
            "episode",
            anime.map(SeoMetadataService::alternateNames).orElse(List.of()),
            anime.map(value -> safeList(value.genres())).orElse(List.of()),
            null,
            player.episodeNumber(),
            player.animeTitle(),
            "/anime/" + slug,
            validPublicUrl(player.embedUrl()) ? player.embedUrl() : null,
            player.durationSeconds());
    }

    private static boolean isPrivate(String path) {
        return path.equals("/entrar")
            || path.equals("/perfil")
            || path.equals("/notificacoes")
            || path.startsWith("/admin");
    }

    private static String privateTitle(String path) {
        if (path.startsWith("/admin")) return "Painel administrativo";
        if (path.equals("/entrar")) return "Entrar";
        if (path.equals("/perfil")) return "Meu perfil";
        return "Notificacoes";
    }

    private static String titled(String value) {
        return value == null || value.isBlank() ? DEFAULT_TITLE : value + " | " + SITE_NAME;
    }

    private static SeoMeta index(String title, String description, String path, String ogType, String imageUrl) {
        return index(title, description, path, ogType, imageUrl, "generic");
    }

    private static SeoMeta index(String title, String description, String path, String ogType, String imageUrl, String kind) {
        return new SeoMeta(title, description, path, false, true, ogType, imageUrl,
            kind, List.of(), List.of(), null, null, null, null, null, null);
    }

    private static SeoMeta noindex(String title, String description, String path) {
        return new SeoMeta(title, description, path, true, false, "website", null);
    }

    private static List<String> alternateNames(AnimeDetailResponse anime) {
        return Stream.of(anime.titleRomaji(), anime.titleEnglish(), anime.titleNative())
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .filter(value -> !value.equalsIgnoreCase(anime.titleDisplay()))
            .distinct()
            .toList();
    }

    private static List<String> safeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .distinct()
            .toList();
    }

    private static boolean validPublicUrl(String value) {
        return value != null && (value.startsWith("https://") || value.startsWith("http://"));
    }

    private static String description(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_DESCRIPTION;
        }
        String clean = value.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        return clean.length() > MAX_DESCRIPTION ? clean.substring(0, MAX_DESCRIPTION - 3).trim() + "..." : clean;
    }

    private static String normalizePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) return "/";
        String path = rawPath;
        int q = path.indexOf('?');
        if (q >= 0) path = path.substring(0, q);
        int h = path.indexOf('#');
        if (h >= 0) path = path.substring(0, h);
        if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
        return path.isBlank() ? "/" : path;
    }

    private static Integer parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
