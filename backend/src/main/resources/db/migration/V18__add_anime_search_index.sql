-- Busca de catalogo no banco (antes era filtro em memoria sobre findAll()).
-- Coluna normalizada + indice trigram para LIKE '%q%' rapido e acento-insensivel.

create extension if not exists unaccent;
create extension if not exists pg_trgm;

alter table anime add column if not exists search_index text;

-- Backfill dos animes existentes com a mesma normalizacao usada pelo app na
-- escrita (minusculo, sem acento, apenas alfanumerico separado por espaco).
-- Linhas novas/editadas sao mantidas pelo callback @PrePersist/@PreUpdate.
update anime
set search_index = trim(regexp_replace(
    lower(unaccent(concat_ws(' ',
        coalesce(title_display, ''),
        coalesce(title_romaji, ''),
        coalesce(title_english, ''),
        coalesce(title_native, ''),
        coalesce(slug, ''),
        coalesce(studio, ''),
        coalesce(season_label, ''),
        coalesce(genres, ''),
        coalesce(year::text, '')
    ))),
    '[^a-z0-9]+', ' ', 'g'
));

create index if not exists idx_anime_search_index_trgm
    on anime using gin (search_index gin_trgm_ops);
