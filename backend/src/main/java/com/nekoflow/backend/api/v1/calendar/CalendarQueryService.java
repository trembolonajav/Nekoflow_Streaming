package com.nekoflow.backend.api.v1.calendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nekoflow.backend.api.v1.calendar.dto.CalendarDayResponse;
import com.nekoflow.backend.api.v1.calendar.dto.CalendarReleaseResponse;
import com.nekoflow.backend.api.v1.calendar.dto.CalendarWeekResponse;
import com.nekoflow.backend.domain.entity.EpisodeEntity;
import com.nekoflow.backend.domain.enums.EpisodeStatus;
import com.nekoflow.backend.domain.repository.EpisodeRepository;

@Service
public class CalendarQueryService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final EpisodeRepository episodeRepository;

    public CalendarQueryService(EpisodeRepository episodeRepository) {
        this.episodeRepository = episodeRepository;
    }

    @Transactional(readOnly = true)
    public CalendarWeekResponse getWeek(LocalDate referenceDate) {
        LocalDate monday = referenceDate.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        List<EpisodeEntity> episodes = episodeRepository.findAllByStatusInOrderByScheduledForAscPublishedAtAscNumberAsc(
            List.of(EpisodeStatus.PUBLISHED, EpisodeStatus.SCHEDULED)
        ).stream()
            .filter(episode -> episode.getAnime() != null && episode.getAnime().isShowInCalendar())
            .filter(this::isLatestEpisodeForAnime)
            .filter(episode -> {
                OffsetDateTime airDate = releaseDate(episode);
                if (airDate == null) {
                    return false;
                }
                LocalDate date = airDate.toLocalDate();
                return !date.isBefore(monday) && !date.isAfter(sunday);
            })
            .toList();

        List<CalendarDayResponse> days = monday.datesUntil(sunday.plusDays(1))
            .map(day -> {
                List<CalendarReleaseResponse> releases = episodes.stream()
                    .filter(episode -> releaseDate(episode) != null && releaseDate(episode).toLocalDate().equals(day))
                    .sorted(Comparator.comparing(this::releaseDate))
                    .map(episode -> toRelease(episode, now))
                    .toList();
                return new CalendarDayResponse(
                    day.getDayOfWeek().getValue() - 1,
                    capitalize(day.getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR)),
                    capitalize(day.getDayOfWeek().getDisplayName(TextStyle.SHORT, PT_BR)).replace(".", ""),
                    day.atStartOfDay().atOffset(ZoneOffset.UTC).toString(),
                    day.equals(now.toLocalDate()),
                    releases
                );
            })
            .toList();

        return new CalendarWeekResponse(
            monday.atStartOfDay().atOffset(ZoneOffset.UTC).toString(),
            sunday.atTime(23, 59, 59).atOffset(ZoneOffset.UTC).toString(),
            formatRangeLabel(monday, sunday),
            seasonName(monday),
            monday.getYear(),
            days,
            days.stream().mapToInt(day -> day.releases().size()).sum()
        );
    }

    private CalendarReleaseResponse toRelease(EpisodeEntity episode, OffsetDateTime now) {
        OffsetDateTime airDate = releaseDate(episode);
        return new CalendarReleaseResponse(
            episode.getId().toString(),
            episode.getAnime().getSlug(),
            episode.getAnime().getTitleDisplay(),
            episode.getNumber(),
            episode.getThumbnailUrl(),
            episode.getAnime().getCoverUrl(),
            episode.getSummary() != null ? episode.getSummary() : defaultSynopsis(episode.getAnime().getSynopsis()),
            List.of(episode.getAnime().getType().name()),
            airDate.toLocalTime().withSecond(0).withNano(0).toString(),
            airDate.toString(),
            "LEG",
            airDate.isAfter(now) ? "upcoming" : "aired",
            episode.getAnime().getStudio() != null ? episode.getAnime().getStudio() : "NekoFlow"
        );
    }

    private OffsetDateTime releaseDate(EpisodeEntity episode) {
        return episode.getScheduledFor();
    }

    private boolean isLatestEpisodeForAnime(EpisodeEntity episode) {
        if (episode.getAnime() == null || episode.getAnime().getEpisodes() == null) {
            return true;
        }

        Integer latestNumber = episode.getAnime().getEpisodes().stream()
            .filter(item -> item.getStatus() == EpisodeStatus.PUBLISHED || item.getStatus() == EpisodeStatus.SCHEDULED)
            .map(EpisodeEntity::getNumber)
            .filter(java.util.Objects::nonNull)
            .max(Integer::compareTo)
            .orElse(episode.getNumber());

        return java.util.Objects.equals(episode.getNumber(), latestNumber);
    }

    private String seasonName(LocalDate date) {
        int month = date.getMonthValue();
        if (month <= 3) return "Inverno";
        if (month <= 6) return "Primavera";
        if (month <= 9) return "Verão";
        return "Outono";
    }

    private String formatRangeLabel(LocalDate start, LocalDate end) {
        String startMonth = start.getMonth().getDisplayName(TextStyle.FULL, PT_BR);
        String endMonth = end.getMonth().getDisplayName(TextStyle.FULL, PT_BR);
        if (start.getMonth().equals(end.getMonth())) {
            return start.getDayOfMonth() + "–" + end.getDayOfMonth() + " de " + startMonth;
        }
        return start.getDayOfMonth() + " de " + startMonth.substring(0, 3) + " – "
            + end.getDayOfMonth() + " de " + endMonth.substring(0, 3);
    }

    private String capitalize(String value) {
        return value.substring(0, 1).toUpperCase(PT_BR) + value.substring(1);
    }

    private String defaultSynopsis(String synopsis) {
        if (synopsis == null || synopsis.isBlank()) {
            return "Novo episódio disponível no calendário da temporada.";
        }
        return synopsis.length() > 140 ? synopsis.substring(0, 140) + "..." : synopsis;
    }
}
