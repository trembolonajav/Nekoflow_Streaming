package com.nekoflow.backend.api.v1.admin;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nekoflow.backend.api.v1.admin.dto.AdminCommentReportResponse;
import com.nekoflow.backend.api.v1.admin.dto.AdminDashboardHealthResponse;
import com.nekoflow.backend.api.v1.admin.dto.AdminDashboardMetricResponse;
import com.nekoflow.backend.api.v1.admin.dto.AdminDashboardPublicationResponse;
import com.nekoflow.backend.api.v1.admin.dto.AdminDashboardResponse;
import com.nekoflow.backend.api.v1.admin.dto.AdminDashboardSectionResponse;
import com.nekoflow.backend.api.v1.admin.dto.AdminSuggestionResponse;
import com.nekoflow.backend.domain.entity.AnimeEntity;
import com.nekoflow.backend.domain.entity.AnimeSuggestionEntity;
import com.nekoflow.backend.domain.entity.CommentEntity;
import com.nekoflow.backend.domain.entity.CommentReportEntity;
import com.nekoflow.backend.domain.entity.EpisodeEntity;
import com.nekoflow.backend.domain.entity.HomeSectionEntity;
import com.nekoflow.backend.domain.enums.EpisodeStatus;
import com.nekoflow.backend.domain.enums.VisibilityStatus;
import com.nekoflow.backend.domain.repository.AnimeRepository;
import com.nekoflow.backend.domain.repository.AnimeSuggestionRepository;
import com.nekoflow.backend.domain.repository.CommentReportRepository;
import com.nekoflow.backend.domain.repository.CommentRepository;
import com.nekoflow.backend.domain.repository.EpisodeRepository;
import com.nekoflow.backend.domain.repository.HomeSectionRepository;

@Service
public class AdminOperationsService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AnimeRepository animeRepository;
    private final EpisodeRepository episodeRepository;
    private final HomeSectionRepository homeSectionRepository;
    private final AnimeSuggestionRepository animeSuggestionRepository;
    private final CommentReportRepository commentReportRepository;
    private final CommentRepository commentRepository;

    public AdminOperationsService(
        AnimeRepository animeRepository,
        EpisodeRepository episodeRepository,
        HomeSectionRepository homeSectionRepository,
        AnimeSuggestionRepository animeSuggestionRepository,
        CommentReportRepository commentReportRepository,
        CommentRepository commentRepository
    ) {
        this.animeRepository = animeRepository;
        this.episodeRepository = episodeRepository;
        this.homeSectionRepository = homeSectionRepository;
        this.animeSuggestionRepository = animeSuggestionRepository;
        this.commentReportRepository = commentReportRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        List<AnimeEntity> animes = animeRepository.findAllByOrderByTitleDisplayAsc();
        List<EpisodeEntity> episodes = episodeRepository.findAllByOrderByPublishedAtDescNumberDesc();
        List<HomeSectionEntity> sections = homeSectionRepository.findAllByOrderBySortOrderAsc();
        List<AdminCommentReportResponse> reports = listReports();
        List<AdminSuggestionResponse> suggestions = listSuggestions();

        List<AdminDashboardMetricResponse> metrics = List.of(
            new AdminDashboardMetricResponse("animes", "Animes publicados", String.valueOf(animes.stream().filter(anime -> anime.getVisibility() == VisibilityStatus.PUBLISHED).count()), "Base real", "up"),
            new AdminDashboardMetricResponse("episodes", "Episódios publicados", String.valueOf(episodes.stream().filter(episode -> episode.getStatus() == EpisodeStatus.PUBLISHED).count()), "Base real", "up"),
            new AdminDashboardMetricResponse("pending", "Pendentes de revisão", String.valueOf(episodes.stream().filter(episode -> episode.getStatus() == EpisodeStatus.REVIEW).count()), "Fila atual", "flat"),
            new AdminDashboardMetricResponse("reports", "Reports abertos", String.valueOf(commentReportRepository.countByStatus("PENDING")), "Comunidade", "down"),
            new AdminDashboardMetricResponse("featured", "Itens em destaque", String.valueOf(sections.stream().mapToInt(section -> section.getItems().size()).sum()), "Home ativa", "flat")
        );

        List<AdminDashboardPublicationResponse> recentPublications = buildRecentPublications(animes, episodes);
        List<AdminDashboardSectionResponse> homeSections = sections.stream()
            .map(section -> new AdminDashboardSectionResponse(
                section.getCode(),
                section.getTitle(),
                section.getMode().name(),
                section.getItems().size(),
                section.isActive()
            ))
            .toList();

        List<AdminDashboardHealthResponse> health = List.of(
            new AdminDashboardHealthResponse("h1", "Animes sem banner", "Recomendado adicionar banner", (int) animes.stream().filter(anime -> anime.getBannerUrl() == null || anime.getBannerUrl().isBlank()).count(), "warning"),
            new AdminDashboardHealthResponse("h2", "Episódios sem thumbnail", "Recomendado adicionar thumbnail", (int) episodes.stream().filter(episode -> episode.getThumbnailUrl() == null || episode.getThumbnailUrl().isBlank()).count(), "warning"),
            new AdminDashboardHealthResponse("h3", "Sugestões novas", "Itens aguardando triagem editorial", (int) animeSuggestionRepository.countByStatus("NEW"), "info"),
            new AdminDashboardHealthResponse("h4", "Reports pendentes", "Aguardando ação da equipe", (int) commentReportRepository.countByStatus("PENDING"), "info")
        );

        return new AdminDashboardResponse(
            metrics,
            recentPublications,
            homeSections,
            reports.stream().limit(4).toList(),
            suggestions.stream().limit(5).toList(),
            health
        );
    }

    @Transactional(readOnly = true)
    public List<AdminSuggestionResponse> listSuggestions() {
        List<AnimeSuggestionEntity> items = animeSuggestionRepository.findAllByOrderByVotesDescCreatedAtAsc();
        List<AdminSuggestionResponse> response = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            AnimeSuggestionEntity suggestion = items.get(index);
            response.add(new AdminSuggestionResponse(
                suggestion.getId().toString(),
                index + 1,
                suggestion.getTitle(),
                suggestion.getVotes(),
                suggestion.getStatus(),
                suggestion.getNote(),
                suggestion.getCreatedAt() != null ? suggestion.getCreatedAt().toString() : null
            ));
        }
        return response;
    }

    @Transactional
    public AdminSuggestionResponse updateSuggestionStatus(UUID id, String status) {
        AnimeSuggestionEntity suggestion = animeSuggestionRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Suggestion not found"));
        suggestion.setStatus(normalizeEnumValue(status));
        return toSuggestion(animeSuggestionRepository.save(suggestion), 0);
    }

    @Transactional
    public AdminSuggestionResponse convertSuggestionToAnime(UUID id) {
        AnimeSuggestionEntity suggestion = animeSuggestionRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Suggestion not found"));

        AnimeEntity anime = new AnimeEntity();
        anime.setId(UUID.randomUUID());
        anime.setSlug(slugify(suggestion.getTitle()));
        anime.setTitleDisplay(suggestion.getTitle());
        anime.setType(com.nekoflow.backend.domain.enums.AnimeType.SERIES);
        anime.setStatus(com.nekoflow.backend.domain.enums.AnimeStatus.RELEASING);
        anime.setVisibility(VisibilityStatus.DRAFT);
        animeRepository.save(anime);

        suggestion.setStatus("APPROVED");
        suggestion.setNote("Convertido em rascunho do catálogo.");
        animeSuggestionRepository.save(suggestion);

        return toSuggestion(suggestion, 0);
    }

    @Transactional(readOnly = true)
    public List<AdminCommentReportResponse> listReports() {
        return commentReportRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(this::toReport)
            .toList();
    }

    @Transactional
    public AdminCommentReportResponse updateReportStatus(UUID id, String status) {
        CommentReportEntity report = commentReportRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        report.setStatus(normalizeEnumValue(status));
        return toReport(commentReportRepository.save(report));
    }

    @Transactional
    public AdminCommentReportResponse updateCommentVisibility(UUID commentId, String visibility) {
        CommentEntity comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        comment.setStatus(normalizeEnumValue(visibility));
        commentRepository.save(comment);

        return commentReportRepository.findAllByOrderByCreatedAtDesc().stream()
            .filter(report -> report.getComment().getId().equals(commentId))
            .findFirst()
            .map(this::toReport)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found for comment"));
    }

    private AdminSuggestionResponse toSuggestion(AnimeSuggestionEntity suggestion, int rank) {
        return new AdminSuggestionResponse(
            suggestion.getId().toString(),
            rank,
            suggestion.getTitle(),
            suggestion.getVotes(),
            suggestion.getStatus(),
            suggestion.getNote(),
            suggestion.getCreatedAt() != null ? suggestion.getCreatedAt().toString() : null
        );
    }

    private List<AdminDashboardPublicationResponse> buildRecentPublications(List<AnimeEntity> animes, List<EpisodeEntity> episodes) {
        List<AdminDashboardPublicationResponse> publications = new ArrayList<>();

        publications.addAll(episodes.stream()
            .sorted(Comparator.comparing(this::episodeSortDate).reversed())
            .limit(4)
            .map(episode -> new AdminDashboardPublicationResponse(
                episode.getId().toString(),
                episode.getAnime().getTitleDisplay(),
                "Episódio " + episode.getNumber(),
                "EPISODE",
                episode.getStatus().name(),
                formatDate(episodeSortDate(episode)),
                episode.getThumbnailUrl()
            ))
            .toList());

        publications.addAll(animes.stream()
            .sorted(Comparator.comparing(this::animeSortDate).reversed())
            .limit(2)
            .map(anime -> new AdminDashboardPublicationResponse(
                anime.getId().toString(),
                anime.getTitleDisplay(),
                "Anime",
                "ANIME",
                anime.getVisibility().name(),
                formatDate(animeSortDate(anime)),
                anime.getCoverUrl()
            ))
            .toList());

        return publications.stream()
            .sorted(Comparator.comparing(AdminDashboardPublicationResponse::updatedAt).reversed())
            .limit(6)
            .toList();
    }

    private AdminCommentReportResponse toReport(CommentReportEntity report) {
        CommentEntity comment = report.getComment();
        String user = comment.getUser().getName();
        return new AdminCommentReportResponse(
            report.getId().toString(),
            comment.getId().toString(),
            user,
            user.substring(0, 1).toUpperCase(Locale.ROOT),
            comment.getEpisode().getAnime().getTitleDisplay() + " - Ep. " + comment.getEpisode().getNumber(),
            report.getReason(),
            comment.getBody(),
            report.getReportCount(),
            report.getCreatedAt() != null ? report.getCreatedAt().toString() : null,
            report.getStatus()
        );
    }

    private OffsetDateTime episodeSortDate(EpisodeEntity episode) {
        return episode.getPublishedAt() != null ? episode.getPublishedAt()
            : episode.getScheduledFor() != null ? episode.getScheduledFor()
            : OffsetDateTime.MIN;
    }

    private OffsetDateTime animeSortDate(AnimeEntity anime) {
        return anime.getPublishedAt() != null ? anime.getPublishedAt() : OffsetDateTime.MIN;
    }

    private String formatDate(OffsetDateTime value) {
        return value != null && !OffsetDateTime.MIN.equals(value) ? value.format(DATETIME_FORMATTER) : "Sem data";
    }

    private String normalizeEnumValue(String value) {
        return value.trim()
            .replace('-', '_')
            .replace(' ', '_')
            .toUpperCase(Locale.ROOT);
    }

    private String slugify(String value) {
        return value.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
    }
}
