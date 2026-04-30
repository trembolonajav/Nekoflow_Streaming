# NekoFlow Phase 2 - Domain Contract

Este documento congela o contrato inicial do dominio antes da integracao real do frontend.

## Fontes externas

### AniList

Usado para:

- `anilist_id`
- `title_romaji`
- `title_native`
- `title_english`
- `synopsis`
- `cover_url`
- `banner_url`
- `studio`
- `year`
- metadados editoriais basicos

Nao usado para:

- auth
- progresso
- comentarios
- home curada
- publicacao interna

### SeekStreaming

Usado para:

- `provider = SEEKSTREAMING`
- `external_video_id`
- `embed_url`
- `player_url`

Nao usado para:

- gestao editorial do catalogo
- usuarios
- progresso interno
- comentarios

## Fonte de verdade

O backend e a unica fonte de verdade para:

- anime publicado ou nao
- episodio publicado ou nao
- ordem e modo das secoes da home
- relacao entre anime e episodio
- qual video e o padrao do episodio
- estado do usuario
- regras de admin/moderacao

## Enums canonicos

### RoleCode

- `ADMIN`
- `EDITOR`
- `MODERATOR`
- `USER`

### AnimeType

- `SERIES`
- `MOVIE`
- `OVA`
- `SPECIAL`

### AnimeStatus

- `RELEASING`
- `FINISHED`
- `HIATUS`

### VisibilityStatus

- `DRAFT`
- `REVIEW`
- `SCHEDULED`
- `PUBLISHED`
- `ARCHIVED`

### EpisodeStatus

- `DRAFT`
- `REVIEW`
- `SCHEDULED`
- `PUBLISHED`
- `ARCHIVED`

### HomeSectionMode

- `MANUAL`
- `AUTOMATIC`
- `HYBRID`

### VideoProvider

- `SEEKSTREAMING`

## Endpoints v1 previstos nesta fase

### Health

- `GET /api/health`

### Auth

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`

### Catalogo publico

- `GET /api/v1/home`
- `GET /api/v1/animes`
- `GET /api/v1/animes/{slug}`
- `GET /api/v1/watch/{slug}/{episodeNumber}`

### Admin

- `GET /api/v1/admin/animes`
- `POST /api/v1/admin/animes`
- `PUT /api/v1/admin/animes/{id}`
- `GET /api/v1/admin/episodes`
- `POST /api/v1/admin/episodes`
- `PUT /api/v1/admin/episodes/{id}`
- `GET /api/v1/admin/home`
- `PUT /api/v1/admin/home/sections`
- `PUT /api/v1/admin/home/hero`

## DTOs base previstos

### AnimeSummaryResponse

- `id`
- `slug`
- `titleDisplay`
- `coverUrl`
- `bannerUrl`
- `type`
- `status`
- `visibility`
- `year`

### EpisodePlayerResponse

- `animeSlug`
- `animeTitle`
- `episodeId`
- `episodeNumber`
- `episodeTitle`
- `summary`
- `thumbnailUrl`
- `provider`
- `embedUrl`
- `playerUrl`

## Regra de mapeamento para o frontend

- frontend nao decide enum de negocio
- frontend nao decide permissao
- frontend nao persiste catalogo em `localStorage`
- frontend so consome contratos da API
