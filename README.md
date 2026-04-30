# NekoFlow Streaming

Plataforma de streaming e curadoria de anime com frontend em React/Vite, backend em Spring Boot e persistencia em PostgreSQL.

O projeto possui catalogo, player, autenticacao, area do usuario, painel administrativo, integracoes com AniList/SeekStreaming e uma base de worker para automatizar publicacao de episodios.

## Stack

- Frontend: React 18, Vite, TypeScript, Tailwind CSS, shadcn/ui, TanStack Query e React Router.
- Backend: Java 17, Spring Boot 3, Spring Security, Spring Data JPA, Flyway e Maven.
- Banco: PostgreSQL 16.
- Infra local: Docker Compose.

## Estrutura

```text
.
|-- backend/
|   |-- src/main/java/com/nekoflow/backend
|   |-- src/main/resources/db/migration
|   |-- src/test/java/com/nekoflow/backend
|   `-- pom.xml
|-- docs/
|   |-- phase-2-domain-contract.md
|   `-- worker.md
|-- public/
|-- src/
|-- docker-compose.yml
|-- Dockerfile.frontend
|-- package.json
`-- README.md
```

## Funcionalidades

- Login, registro, logout, refresh token e endpoint `me`.
- Rotas administrativas protegidas por role.
- CRUD de animes e episodios.
- Home editorial persistida no banco.
- Player conectado a provider externo.
- Comentarios, replies, reports e moderacao.
- Perfil, preferencias, watchlist, historico e progresso.
- Calendario semanal baseado em episodios publicados/agendados.
- Dashboard admin, sugestoes e busca integrada ao AniList.
- Worker administrativo para sincronizar videos, montar fila de releases e publicar episodios.

## Integracoes

### AniList

Usado para metadados editoriais: titulos, capas, banners, generos, sinopses, ano, temporada e estudios.

Variavel:

```text
APP_ANILIST_ENDPOINT=https://graphql.anilist.co
```

### SeekStreaming

Usado como fonte de videos, thumbnails, previews e embed do player.

Variaveis:

```text
APP_SEEKSTREAMING_ENDPOINT=https://seekstreaming.com
APP_SEEKSTREAMING_API_TOKEN=coloque_seu_token_aqui
```

### Worker

O worker fica no backend e permite:

- sincronizar videos do SeekStreaming para `seek_videos`;
- parsear nomes de arquivos e montar `release_queue`;
- consultar/cachear AniList em `anilist_cache`;
- revisar, aprovar e publicar releases no admin;
- receber webhooks externos em `/api/v1/worker/webhooks/releases`;
- registrar tentativas em `webhook_log`.

Detalhes completos estao em [docs/worker.md](docs/worker.md).

## Variaveis de ambiente

Copie o exemplo:

```bash
cp .env.example .env
```

No PowerShell:

```powershell
Copy-Item .env.example .env
```

Principais variaveis:

- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`: banco criado pelo Docker.
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`: conexao do backend.
- `SERVER_PORT`: porta interna do backend.
- `APP_CORS_ALLOWED_ORIGINS`: origens liberadas para o frontend.
- `APP_JWT_SECRET`: segredo JWT. Trocar em producao.
- `APP_JWT_ACCESS_EXPIRATION_SECONDS`: expiracao do access token.
- `APP_JWT_REFRESH_EXPIRATION_SECONDS`: expiracao do refresh token.
- `APP_ANILIST_ENDPOINT`: endpoint GraphQL do AniList.
- `APP_SEEKSTREAMING_ENDPOINT`: endpoint do SeekStreaming.
- `APP_SEEKSTREAMING_API_TOKEN`: token do provider externo.
- `APP_WORKER_WEBHOOK_SECRET`: segredo HMAC para webhooks externos.
- `APP_BOOTSTRAP_ADMIN_*`: usuario admin inicial.
- `APP_BOOTSTRAP_USER_*`: usuario comum inicial.
- `FRONTEND_PORT`, `BACKEND_PORT`, `POSTGRES_PORT`: portas expostas pelo Docker.

Nunca versionar o `.env` real.

## Como subir com Docker

```bash
docker compose up -d --build
```

Servicos:

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- Postgres: `localhost:5432`

Health checks:

- `http://localhost:8080/api/health`
- `http://localhost:8080/actuator/health`

## Desenvolvimento local

### Frontend

```bash
npm install
npm run dev
```

Build:

```bash
npm run build
```

Testes:

```bash
npm run test
```

Lint:

```bash
npm run lint
```

### Backend

O backend precisa de PostgreSQL acessivel pelas variaveis `DB_*`.

```bash
cd backend
mvn spring-boot:run
```

Testes:

```bash
cd backend
mvn test
```

## Credenciais iniciais

Definidas no `.env.example`:

- Admin: `admin@nekoflow.app` / `12345678`
- Usuario comum: `hikari@nekoflow.app` / `12345678`

Trocar essas credenciais em qualquer ambiente compartilhado ou publico.

## Rotas principais

Frontend:

- `/`
- `/entrar`
- `/calendario`
- `/perfil`
- `/anime/:slug`
- `/watch/:slug/:episodeNumber`
- `/admin`
- `/admin/animes`
- `/admin/episodios`
- `/admin/home`
- `/admin/comentarios`
- `/admin/sugestoes`

Backend base:

```text
http://localhost:8080/api/v1
```

Grupos principais:

- `/auth`
- `/home`
- `/animes`
- `/watch`
- `/episodes`
- `/comments`
- `/me`
- `/calendar`
- `/admin`
- `/admin/worker`
- `/worker/webhooks`

## Banco e migrations

As migrations Flyway ficam em:

```text
backend/src/main/resources/db/migration
```

Elas cobrem fundacao da base, roles, seeds, catalogo, refresh tokens, comunidade, reports, sugestoes, ajustes de midia e tabelas do worker.

Regra de manutencao: toda alteracao de schema deve entrar como nova migration `V<N>__descricao.sql`. Nao editar migrations antigas depois que ja foram aplicadas em algum ambiente.

## Publicacao de episodio pelo worker

Fluxo resumido:

1. Sincronizar SeekStreaming: `POST /api/v1/admin/worker/seek/sync`.
2. Processar fila: `POST /api/v1/admin/worker/queue/parse`.
3. Revisar itens em `matched`, `needs_review` ou `anilist_unavailable`.
4. Aprovar: `POST /api/v1/admin/worker/queue/{id}/approve`.
5. Publicar: `POST /api/v1/admin/worker/queue/{id}/publish`.

Ao publicar, o backend cria ou atualiza:

- anime;
- episodio;
- fonte de video padrao com provider `SEEKSTREAMING`;
- URL de embed baseada em `https://nekoflow.embedseek.com/#<seek_video_id>`.

## Checklist antes de subir alteracoes

```bash
npm run build
npm run test
cd backend
mvn test
```

Tambem conferir:

- `.env` nao foi adicionado ao Git;
- `backend/target`, `dist` e `node_modules` nao foram versionados;
- novas variaveis foram refletidas em `.env.example`;
- novas tabelas foram criadas via Flyway;
- novas rotas administrativas exigem autenticacao/role adequada.

## Arquivos que nao devem ir para o Git

- `.env`
- `node_modules/`
- `dist/`
- `backend/target/`
- logs locais
- credenciais, tokens e dumps de banco

## Estado atual

Este snapshot representa o site de streaming com frontend, backend, banco, Docker e worker administrativo documentados para continuidade do projeto.
