package com.nekoflow.backend.api.v1.admin;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Metricas de audiencia a partir do proprio banco (watch_history, watchlist,
 * users, comments). Trafego anonimo (visitas, origem) fica no Umami — aqui e o
 * que so o Nekoflow sabe: o que os usuarios logados assistem e guardam.
 */
@RestController
@RequestMapping("/api/v1/admin/metrics")
public class AdminMetricsController {

    private final JdbcTemplate jdbc;

    public AdminMetricsController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> metrics() {
        Map<String, Object> out = new LinkedHashMap<>();

        out.put("totals", Map.of(
            "users", count("select count(*) from users"),
            "animes", count("select count(*) from anime"),
            "episodes_published", count("select count(*) from episodes where status = 'PUBLISHED'"),
            "watchlist_entries", count("select count(*) from watchlist"),
            "comments", count("select count(*) from comments"),
            "plays_7d", count("select count(*) from watch_history where watched_at > now() - interval '7 days'"),
            "plays_30d", count("select count(*) from watch_history where watched_at > now() - interval '30 days'")
        ));

        out.put("top_animes_7d", jdbc.queryForList("""
            select a.title_display as title, a.slug, a.cover_url, count(*) as plays,
                   count(distinct h.user_id) as viewers
            from watch_history h
            join anime a on a.id = h.anime_id
            where h.watched_at > now() - interval '7 days'
            group by a.id
            order by plays desc
            limit 10
            """));

        out.put("top_episodes_7d", jdbc.queryForList("""
            select a.title_display as title, e.number, count(*) as plays
            from watch_history h
            join episodes e on e.id = h.episode_id
            join anime a on a.id = h.anime_id
            where h.watched_at > now() - interval '7 days'
            group by a.id, e.id
            order by plays desc
            limit 10
            """));

        out.put("most_favorited", jdbc.queryForList("""
            select a.title_display as title, a.slug, count(*) as favorites
            from watchlist w
            join anime a on a.id = w.anime_id
            group by a.id
            order by favorites desc
            limit 10
            """));

        out.put("new_users_by_day", jdbc.queryForList("""
            select to_char(d.day, 'YYYY-MM-DD') as day, coalesce(u.total, 0) as total
            from generate_series(current_date - interval '13 days', current_date, interval '1 day') d(day)
            left join (
                select created_at::date as day, count(*) as total
                from users
                where created_at > current_date - interval '14 days'
                group by 1
            ) u on u.day = d.day::date
            order by d.day
            """));

        out.put("plays_by_day", jdbc.queryForList("""
            select to_char(d.day, 'YYYY-MM-DD') as day, coalesce(h.total, 0) as total
            from generate_series(current_date - interval '13 days', current_date, interval '1 day') d(day)
            left join (
                select watched_at::date as day, count(*) as total
                from watch_history
                where watched_at > current_date - interval '14 days'
                group by 1
            ) h on h.day = d.day::date
            order by d.day
            """));

        return ResponseEntity.ok(out);
    }

    private int count(String sql) {
        Integer value = jdbc.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }
}
