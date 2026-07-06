package com.nekoflow.backend.api.v1.catalog;

import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Cache em memoria (Caffeine) para as respostas publicas que mudam pouco.
 * Alivia a VM sob trafego anonimo/repetido sem infra externa.
 *
 * TTLs (expire-after-write):
 *   - home     : 60s  (conteudo editorial + recentes)
 *   - catalog  : 60s  (listagem/busca; chave = query|page|size)
 *   - sitemap  : 300s (5min; muda pouco)
 *
 * Eviction: alem do TTL, o admin/worker chama invalidateAll() ao criar/editar/
 * publicar anime ou episodio, para o conteudo novo aparecer imediatamente.
 */
@Component
public class CatalogCache {

    private final Cache<String, Object> home = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(60))
        .maximumSize(8)
        .build();

    private final Cache<String, Object> catalog = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(60))
        .maximumSize(500)
        .build();

    private final Cache<String, Object> sitemap = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(300))
        .maximumSize(8)
        .build();

    // HTML renderizado por rota (SEO server-side). TTL curto; evictado nas escritas.
    private final Cache<String, Object> render = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(60))
        .maximumSize(2000)
        .build();

    @SuppressWarnings("unchecked")
    public <T> T home(String key, Supplier<T> loader) {
        return (T) home.get(key, ignored -> loader.get());
    }

    @SuppressWarnings("unchecked")
    public <T> T catalog(String key, Supplier<T> loader) {
        return (T) catalog.get(key, ignored -> loader.get());
    }

    @SuppressWarnings("unchecked")
    public <T> T sitemap(String key, Supplier<T> loader) {
        return (T) sitemap.get(key, ignored -> loader.get());
    }

    @SuppressWarnings("unchecked")
    public <T> T render(String key, Supplier<T> loader) {
        return (T) render.get(key, ignored -> loader.get());
    }

    /** Invalida tudo. Chamado nas escritas de catalogo (admin/worker). */
    public void invalidateAll() {
        home.invalidateAll();
        catalog.invalidateAll();
        sitemap.invalidateAll();
        render.invalidateAll();
    }
}
