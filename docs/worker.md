# Worker de releases

Este documento registra o desenho atual do worker do NekoFlow Streaming.

## Objetivo

O worker existe para reduzir o trabalho manual de publicar episodios. Ele conecta tres fontes de informacao:

- videos sincronizados do SeekStreaming;
- metadados do AniList;
- banco local do NekoFlow.

O fluxo atual permite importar videos, tentar identificar anime/episodio, revisar a fila no admin e publicar o episodio no catalogo.

## Tabelas

A migration `backend/src/main/resources/db/migration/V12__create_worker_tables.sql` cria:

- `seek_videos`: espelho local dos videos encontrados no SeekStreaming.
- `release_queue`: fila de releases candidatos a publicacao.
- `anilist_cache`: cache de buscas e payloads do AniList.
- `rss_sources`: fontes RSS cadastradas para automacao futura.
- `seen_releases`: controle de releases RSS ja vistos.
- `webhook_log`: historico de tentativas de publicacao.

## Estados da fila

- `pending`: item criado, ainda aguardando processamento.
- `matched`: parser e AniList encontraram um match aceitavel.
- `needs_review`: precisa de ajuste manual no admin.
- `anilist_unavailable`: AniList falhou, rate limit ou indisponibilidade.
- `approved`: aprovado para publicacao.
- `published`: publicado no catalogo.
- `publish_failed`: tentativa de publicacao falhou.

## Fluxo operacional

1. O admin chama `POST /api/v1/admin/worker/seek/sync`.
2. O backend consulta o SeekStreaming e salva os videos em `seek_videos`.
3. O admin chama `POST /api/v1/admin/worker/queue/parse`.
4. O backend tenta extrair titulo, temporada e episodio pelo nome do arquivo.
5. O backend consulta/cacheia AniList e grava o resultado em `release_queue`.
6. O admin revisa itens `matched` ou `needs_review`.
7. O admin aprova um item com `POST /api/v1/admin/worker/queue/{id}/approve`.
8. O admin publica com `POST /api/v1/admin/worker/queue/{id}/publish`.
9. A publicacao cria ou atualiza anime, episodio e video source no catalogo principal.

## Webhook publico

Endpoint:

```text
POST /api/v1/worker/webhooks/releases
```

Payload minimo:

```json
{
  "event": "release.publish",
  "seek_video_id": "abc123",
  "title": "Nome do Anime",
  "season": 1,
  "episode": 1,
  "anilist_id": 12345,
  "thumbnail_url": "https://exemplo/imagem.jpg",
  "duration_seconds": 1440,
  "anilist": {}
}
```

Quando `APP_WORKER_WEBHOOK_SECRET` estiver definido, clientes externos devem enviar:

```text
X-Nekoflow-Signature: sha256=<hmac_sha256_do_body_raw>
```

O HMAC usa o body exatamente como enviado e o segredo definido em `APP_WORKER_WEBHOOK_SECRET`.

## Rotas administrativas

Base autenticada de admin:

```text
/api/v1/admin/worker
```

Rotas principais:

- `GET /dashboard`: contadores e resumo do worker.
- `POST /seek/ping`: testa integracao com SeekStreaming.
- `POST /seek/sync`: sincroniza videos do SeekStreaming.
- `GET /seek/videos`: lista videos locais sincronizados.
- `GET /import/options`: lista candidatos de importacao.
- `POST /import`: importa videos selecionados para a fila.
- `GET /queue`: lista fila de releases.
- `POST /queue/parse`: processa fila com parser e AniList.
- `PUT /queue/{id}`: ajusta dados parseados manualmente.
- `POST /queue/{id}/approve`: aprova um item.
- `POST /queue/approve`: aprova varios itens.
- `POST /queue/{id}/publish`: publica um item.
- `POST /queue/publish`: publica varios itens aprovados.
- `GET /logs`: consulta logs de publicacao.
- `GET /sources`: lista fontes RSS.
- `POST /sources`: cria fonte RSS.
- `PUT /sources/{id}`: atualiza fonte RSS.
- `DELETE /sources/{id}`: remove fonte RSS.
- `POST /sources/poll`: registra tentativa de poll das fontes.

## Limitacoes atuais

- O poll RSS ainda nao baixa feeds; as fontes ficam salvas para a proxima etapa.
- O parser cobre padroes comuns de filename, mas releases fora de padrao caem em `needs_review`.
- A publicacao interna pelo admin chama o mesmo servico do webhook, mas sem assinatura porque roda dentro do backend.
- O embed final usa `https://nekoflow.embedseek.com/#<seek_video_id>`.
- Se o AniList falhar, o item deve ser reprocessado depois ou revisado manualmente.

## Cuidados futuros

- Nao versionar tokens reais do SeekStreaming nem segredo do worker.
- Manter novas tabelas sempre via Flyway.
- Evitar alterar status diretamente no banco fora de manutencao controlada.
- Antes de automatizar publicacao total, validar criterios de confianca do match e regras de moderacao.
