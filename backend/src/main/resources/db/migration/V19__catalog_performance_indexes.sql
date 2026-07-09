-- Indices para as queries publicas do catalogo/home (Fase B).
-- Ja existiam: anime(visibility), anime(anilist_id), episodes(anime_id),
-- episodes(status) e o unique de anime(slug). Aqui completamos os usados
-- na ordenacao/filtro das novas queries.

-- Ordenacao da listagem/busca por titulo.
create index if not exists idx_anime_title_display on anime (title_display);

-- Filtro por status de anime (badges/segmentacoes).
create index if not exists idx_anime_status on anime (status);

-- Home "recentes": ordenacao por data de publicacao do episodio.
create index if not exists idx_episodes_published_at on episodes (published_at desc);

-- Busca/ordenacao de episodio dentro de um anime (numero) e unicidade logica.
create index if not exists idx_episodes_anime_id_number on episodes (anime_id, number);
