package com.nekoflow.backend.api.v1.catalog.seo;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nekoflow.backend.api.v1.catalog.CatalogCache;

/**
 * Render server-side do SEO (Bloco C1): o nginx encaminha as rotas de documento
 * para ca com o caminho original em X-Original-URI. Buscamos o shell da SPA
 * (index.html do build, via bypass /__spa_index.html - NUNCA pela rota renderizada,
 * para nao criar loop), injetamos o <head> real e devolvemos o mesmo HTML para
 * usuario e crawler (sem cloaking). Se algo falhar, lancamos erro e o nginx serve
 * o index.html estatico como fallback.
 */
@RestController
@RequestMapping("/api/v1/seo")
public class SeoRenderController {

    private final SeoMetadataService metadataService;
    private final CatalogCache catalogCache;
    private final RestTemplate restTemplate;
    private final String frontendInternalUrl;
    private final String publicBaseUrl;

    // Shell muda so em redeploy do frontend: TTL curto, 1 entrada.
    private final Cache<String, String> shellCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(60))
        .maximumSize(1)
        .build();

    public SeoRenderController(
        SeoMetadataService metadataService,
        CatalogCache catalogCache,
        RestTemplate restTemplate,
        @Value("${app.frontend.internal-url:http://frontend}") String frontendInternalUrl,
        @Value("${app.public-base-url:https://nekoflow.com.br}") String publicBaseUrl
    ) {
        this.metadataService = metadataService;
        this.catalogCache = catalogCache;
        this.restTemplate = restTemplate;
        this.frontendInternalUrl = frontendInternalUrl.replaceAll("/+$", "");
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    @GetMapping(value = "/render", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> render(@RequestHeader(value = "X-Original-URI", required = false) String originalUri) {
        String path = originalUri == null || originalUri.isBlank() ? "/" : originalUri;
        SeoMeta meta = metadataService.metadataFor(path);
        String html = catalogCache.render(meta.canonicalPath(), () -> {
            return SeoHtmlInjector.inject(loadShell(), meta, publicBaseUrl);
        });
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    private String loadShell() {
        String shell = shellCache.get("shell",
            ignored -> restTemplate.getForObject(frontendInternalUrl + "/__spa_index.html", String.class));
        if (shell == null || shell.isBlank()) {
            // Sem shell nao ha o que renderizar: falha -> nginx serve o estatico.
            throw new IllegalStateException("Shell da SPA indisponivel para render de SEO.");
        }
        return shell;
    }
}
