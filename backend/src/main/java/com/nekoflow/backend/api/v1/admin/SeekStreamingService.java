package com.nekoflow.backend.api.v1.admin;

import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.nekoflow.backend.api.v1.admin.dto.AdminSeekStreamingFolderResponse;
import com.nekoflow.backend.api.v1.admin.dto.AdminSeekStreamingVideoResponse;
import com.nekoflow.backend.config.AppProperties;

@Service
public class SeekStreamingService {

    private static final String ASSET_BASE_URL = "https://asset.seekstreaming.info";

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public SeekStreamingService(RestTemplate restTemplate, AppProperties appProperties) {
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
    }

    public List<AdminSeekStreamingFolderResponse> listFolders() {
        ResponseEntity<List<Map<String, Object>>> response = exchange(
            "/api/v1/video/folder",
            new ParameterizedTypeReference<>() {
            }
        );

        List<Map<String, Object>> body = response.getBody();
        if (body == null) {
            return List.of();
        }

        return body.stream()
            .map(this::mapFolder)
            .toList();
    }

    @SuppressWarnings("unchecked")
    public List<AdminSeekStreamingVideoResponse> listVideos(String folderId) {
        ResponseEntity<Map<String, Object>> response = exchange(
            "/api/v1/video/folder/" + folderId,
            new ParameterizedTypeReference<>() {
            }
        );

        Map<String, Object> body = response.getBody();
        if (body == null) {
            return List.of();
        }

        Object data = body.get("data");
        if (!(data instanceof List<?> rawVideos)) {
            return List.of();
        }

        return rawVideos.stream()
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .map(item -> (Map<String, Object>) item)
            .map(this::mapVideo)
            .toList();
    }

    private <T> ResponseEntity<T> exchange(String path, ParameterizedTypeReference<T> responseType) {
        String endpoint = resolveEndpoint();
        String apiToken = resolveApiToken();

        if (isBlank(apiToken)) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "SeekStreaming não configurado. Defina APP_SEEKSTREAMING_API_TOKEN."
            );
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("api-token", apiToken);

        try {
            return restTemplate.exchange(
                normalizeBaseUrl(endpoint) + path,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                responseType
            );
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao consultar SeekStreaming.", exception);
        }
    }

    private String resolveEndpoint() {
        AppProperties.Integrations integrations = appProperties.integrations();
        if (integrations != null && integrations.seekstreaming() != null && !isBlank(integrations.seekstreaming().endpoint())) {
            return integrations.seekstreaming().endpoint();
        }
        String envValue = System.getenv("APP_SEEKSTREAMING_ENDPOINT");
        return isBlank(envValue) ? "https://seekstreaming.com" : envValue;
    }

    private String resolveApiToken() {
        AppProperties.Integrations integrations = appProperties.integrations();
        if (integrations != null && integrations.seekstreaming() != null && !isBlank(integrations.seekstreaming().apiToken())) {
            return integrations.seekstreaming().apiToken();
        }
        String envValue = System.getenv("APP_SEEKSTREAMING_API_TOKEN");
        return isBlank(envValue) ? null : envValue;
    }

    private AdminSeekStreamingFolderResponse mapFolder(Map<String, Object> folder) {
        return new AdminSeekStreamingFolderResponse(
            asString(folder.get("id")),
            asString(folder.get("name")),
            asString(folder.get("parentId")),
            asInteger(folder.get("folderCount")),
            asInteger(folder.get("videoCount"))
        );
    }

    private AdminSeekStreamingVideoResponse mapVideo(Map<String, Object> video) {
        return new AdminSeekStreamingVideoResponse(
            asString(video.get("id")),
            asString(video.get("name")),
            asString(video.get("status")),
            asInteger(video.get("width")),
            asInteger(video.get("height")),
            asLong(video.get("size")),
            asInteger(video.get("duration")),
            normalizeAssetUrl(asString(video.get("poster"))),
            normalizeAssetUrl(asString(video.get("preview"))),
            asString(video.get("folderId"))
        );
    }

    private String normalizeBaseUrl(String value) {
        if (isBlank(value)) {
            return "https://seekstreaming.com";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String normalizeAssetUrl(String value) {
        if (isBlank(value)) {
            return null;
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        return ASSET_BASE_URL + (value.startsWith("/") ? value : "/" + value);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
