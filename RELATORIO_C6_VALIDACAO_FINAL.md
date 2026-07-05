# Relatorio C6 - Validacao final de SEO/IA, performance e publicacao

Data: 2026-07-05

## Resumo executivo

O ambiente local/producao-like validado em Docker passou nos pontos tecnicos de
SEO server-side, OG/Twitter, JSON-LD, sitemaps, robots.txt e llms.txt.

O dominio publico `https://nekoflow.com.br` ainda nao esta com o build/config do
C1-C5 aplicado. A validacao externa real fica bloqueada ate o deploy atualizar o
frontend/nginx/backend do dominio.

Status geral:

- Local/producao-like: APROVADO.
- Dominio publico atual: BLOQUEADO PARA LANCAMENTO.
- Divulgacao/indexacao publica: NAO RECOMENDADA antes de novo deploy e recheck.

## Ambiente validado

- Local/producao-like: `http://localhost:8097`
- Container frontend validado: `nekoflow-fe-c5b`
- Backend Docker: `nekoflow_streaming-main-backend-1`
- Backend status: healthy
- Dominio publico testado: `https://nekoflow.com.br`

## Comandos executados

```powershell
mvn -q test
npm run build
docker build -f Dockerfile.frontend.prod -t nekoflow-fe-prod:c6 .
docker exec nekoflow-fe-c5b nginx -t
Invoke-WebRequest -UseBasicParsing http://localhost:8097/
Invoke-WebRequest -UseBasicParsing http://localhost:8097/explorar
Invoke-WebRequest -UseBasicParsing http://localhost:8097/calendario
Invoke-WebRequest -UseBasicParsing http://localhost:8097/anime/frieren-beyond-journeys-end
Invoke-WebRequest -UseBasicParsing http://localhost:8097/watch/frieren-beyond-journeys-end/1
Invoke-WebRequest -UseBasicParsing http://localhost:8097/entrar
Invoke-WebRequest -UseBasicParsing http://localhost:8097/perfil
Invoke-WebRequest -UseBasicParsing http://localhost:8097/admin
Invoke-WebRequest -UseBasicParsing http://localhost:8097/robots.txt
Invoke-WebRequest -UseBasicParsing http://localhost:8097/llms.txt
Invoke-WebRequest -UseBasicParsing http://localhost:8097/sitemap.xml
Invoke-WebRequest -UseBasicParsing http://localhost:8097/sitemap-static.xml
Invoke-WebRequest -UseBasicParsing http://localhost:8097/sitemap-animes.xml
Invoke-WebRequest -UseBasicParsing http://localhost:8097/sitemap-episodes.xml
Invoke-WebRequest -UseBasicParsing http://localhost:8097/sitemap-video.xml
Invoke-WebRequest -UseBasicParsing https://nekoflow.com.br/
Invoke-WebRequest -UseBasicParsing https://nekoflow.com.br/robots.txt
Invoke-WebRequest -UseBasicParsing https://nekoflow.com.br/llms.txt
Invoke-WebRequest -UseBasicParsing https://nekoflow.com.br/sitemap.xml
```

## Resultados de build e testes

- `mvn -q test`: passou.
- Testes Maven: 128 testes, 0 failures, 0 errors, 0 skipped.
- `npm run build`: passou.
- `docker build -f Dockerfile.frontend.prod -t nekoflow-fe-prod:c6 .`: passou.
- `nginx -t`: passou.

## URLs locais testadas

| URL | Status | Resultado |
| --- | --- | --- |
| `/` | 200 | title, description, canonical, OG/Twitter e JSON-LD presentes |
| `/explorar` | 200 | title, description, canonical, OG/Twitter e JSON-LD presentes |
| `/calendario` | 200 | title, description, canonical, OG/Twitter e JSON-LD presentes |
| `/anime/frieren-beyond-journeys-end` | 200 | anime com titulo real, sinopse, OG/Twitter e JSON-LD |
| `/watch/frieren-beyond-journeys-end/1` | 200 | episodio publicado indexavel, OG/Twitter e JSON-LD |
| `/entrar` | 200 | `noindex,nofollow`, sem OG rico e sem JSON-LD rico |
| `/perfil` | 200 | `noindex,nofollow`, sem OG rico e sem JSON-LD rico |
| `/admin` | 200 | `noindex,nofollow`, sem OG rico e sem JSON-LD rico |
| `/robots.txt` | 200 | `text/plain`, aponta para sitemap, nao bloqueia `/watch` |
| `/llms.txt` | 200 | `text/plain`, sitemap e rotas publicas documentadas |
| `/sitemap.xml` | 200 | sitemap index XML valido |
| `/sitemap-static.xml` | 200 | XML valido, sem rotas privadas |
| `/sitemap-animes.xml` | 200 | XML valido, animes publicados |
| `/sitemap-episodes.xml` | 200 | XML valido, episodio publicado presente |
| `/sitemap-video.xml` | 200 | XML valido, video somente com dados minimos reais |

## Validacao local de metadados

Confirmado localmente:

- HTML publico contem `title`, `description`, `canonical` e `robots` corretos.
- Anime contem `Frieren: Beyond Journey's End` e sinopse/conteudo real.
- Episodio publicado `/watch/frieren-beyond-journeys-end/1` e indexavel.
- Rotas privadas/noindex usam `noindex,nofollow`.
- OG/Twitter existem nas rotas publicas.
- Rotas privadas nao recebem OG rico.
- JSON-LD existe nas rotas publicas corretas.
- `/entrar`, `/perfil` e `/admin` nao recebem JSON-LD rico.

## Validacao local de JSON-LD

Confirmado localmente:

- Home: `Organization`, `WebSite`, `SearchAction`.
- Anime: `BreadcrumbList`, `TVSeries`.
- Episodio: `BreadcrumbList`, `TVEpisode`, `VideoObject`.
- Nao ha `aggregateRating` inventado.
- Nao ha `review` inventado.
- `duration` aparece somente quando existe duracao real.
- `thumbnailUrl`, `embedUrl` e `isAccessibleForFree` aparecem no VideoObject do
  episodio publicado com dados reais.

## Validacao local de sitemaps

Confirmado localmente:

- `/sitemap.xml`: XML valido com root `sitemapindex`.
- `/sitemap-static.xml`: XML valido com root `urlset`.
- `/sitemap-animes.xml`: XML valido com root `urlset`.
- `/sitemap-episodes.xml`: XML valido com root `urlset`.
- `/sitemap-video.xml`: XML valido com root `urlset`.
- Nenhum sitemap local contem `/admin`, `/perfil`, `/notificacoes` ou `/entrar`.
- `/sitemap-episodes.xml` e `/sitemap-video.xml` contem
  `/watch/frieren-beyond-journeys-end/1`.
- Content-Type dos sitemaps: `application/xml;charset=UTF-8`.
- Cache-Control via nginx: `public, max-age=300`.

## Validacao local de robots/IA

Confirmado localmente:

- `robots.txt` bloqueia `/admin`, `/perfil`, `/notificacoes` e `/entrar`.
- `robots.txt` nao bloqueia `/watch`.
- `robots.txt` aponta para `/sitemap.xml`.
- `llms.txt` contem sitemap, rotas publicas e rotas privadas como "Nao indexar".
- Politica de bots de IA esta documentada em `docs/seo-launch-checklist.md`.

## Validacao de configuracao de producao

Confirmado nos arquivos:

- `docker-compose.prod.yml` exige `APP_PUBLIC_BASE_URL`.
- `env.production.example` usa `APP_PUBLIC_BASE_URL=https://nekoflow.com.br`.
- `docker-compose.prod.yml` exige `APP_CORS_ALLOWED_ORIGINS`.
- `env.production.example` usa CORS sem localhost:
  `https://nekoflow.com.br,https://www.nekoflow.com.br`.
- `docker-compose.prod.yml` exige `APP_JWT_SECRET`.
- `docker-compose.prod.yml` exige `APP_WORKER_WEBHOOK_SECRET`.
- `APP_WORKER_RSS_POLL_ENABLED` nao esta no compose prod, mas o default em
  `application.yml` e `false`.
- Backend Docker local ficou healthy, indicando boot ok com migracoes/Flyway.

## Validacao do dominio publico

Dominio testado: `https://nekoflow.com.br`

Resultado: BLOQUEADO. O dominio publico esta servindo uma versao anterior ao C1-C5.

Achados:

| Severidade | Problema | Evidencia |
| --- | --- | --- |
| Alta | `/llms.txt` publico serve HTML da SPA, nao `text/plain` | `Content-Type: text/html`, conteudo inicia com `<!doctype html>` |
| Alta | Sitemaps segmentados publicos nao estao ativos | `/sitemap-static.xml`, `/sitemap-animes.xml`, `/sitemap-episodes.xml`, `/sitemap-video.xml` retornam HTML |
| Alta | Rotas privadas publicas nao tem `noindex,nofollow` | `/entrar`, `/perfil`, `/admin` sem noindex |
| Alta | Rotas privadas publicas recebem OG generico da SPA | `/entrar`, `/perfil`, `/admin` com `og:title` |
| Alta | JSON-LD server-side nao aparece no dominio publico | home/anime/watch sem `application/ld+json` |
| Media | `/sitemap.xml` publico ainda e `urlset`, nao sitemap index | root XML `urlset`, sem sitemap index |
| Media | `/sitemap.xml` publico nao contem episodios `/watch/*` | regex `/watch/` ausente |
| Media | `/sitemap.xml` publico nao tem namespace de video | namespace de video ausente |
| Media | `robots.txt` publico ainda nao bloqueia `/entrar` | `Disallow: /entrar` ausente |
| Media | Cache-Control publico aparece duplicado em robots/sitemap antigo | `no-cache...` junto com `public, max-age=300` |

Ponto positivo no dominio:

- Nao foram encontrados URLs `localhost` nas rotas testadas do dominio publico.
- `/robots.txt` e `/sitemap.xml` respondem 200.

## Ferramentas externas

Nao foram executadas como validacao final conclusiva porque o dominio publico esta
com deploy antigo. Rodar agora PageSpeed/Rich Results/Search Console/Bing sobre o
dominio produziria resultados da versao errada.

Pendentes apos deploy C1-C5 no dominio:

- Google Search Console: adicionar propriedade, verificar DNS, enviar
  `/sitemap.xml`, inspecionar `/`, `/explorar`, anime e episodio.
- Bing Webmaster Tools: adicionar dominio e enviar `/sitemap.xml`.
- Rich Results Test para anime e episodio.
- Schema Markup Validator para JSON-LD.
- PageSpeed Insights para `/`, `/explorar`, `/anime/...`, `/watch/...`.
- SecurityHeaders.com.
- Mozilla Observatory.
- SSL Labs.
- WebPageTest ou GTmetrix.
- OWASP ZAP baseline somente em staging publico autorizado.

## Correcoes aplicadas no C6

Nenhuma correcao de codigo foi aplicada no C6. A etapa encontrou um problema de
deploy/configuracao no dominio publico, nao um problema reproduzido no ambiente
local/producao-like.

## Pendencias para dominio real

Antes de divulgacao/indexacao:

- Fazer deploy do backend atualizado.
- Fazer deploy do frontend/nginx atualizado.
- Garantir `APP_PUBLIC_BASE_URL=https://nekoflow.com.br`.
- Garantir CORS de producao sem localhost.
- Confirmar que `/llms.txt` publico responde `text/plain`.
- Confirmar que `/sitemap.xml` publico virou sitemap index.
- Confirmar que sitemaps segmentados publicos respondem XML.
- Confirmar que `/entrar`, `/perfil`, `/admin` tem `noindex,nofollow`.
- Confirmar que JSON-LD aparece no dominio publico.
- Reexecutar toda a bateria C6 no dominio publico.
- Depois disso, rodar ferramentas externas.

## Checklist final de lancamento

Nao lancar ainda.

Para liberar pre-lancamento/staging publico:

- [ ] Deploy C1-C5 aplicado no dominio real.
- [ ] Backend healthy em producao.
- [ ] `nginx -t` ok em producao.
- [ ] `/robots.txt` publico atualizado.
- [ ] `/llms.txt` publico em `text/plain`.
- [ ] `/sitemap.xml` publico como sitemap index.
- [ ] Sitemaps segmentados publicos em XML.
- [ ] Rotas privadas com `noindex,nofollow`.
- [ ] Rotas publicas com OG/Twitter/JSON-LD.
- [ ] Nenhum `localhost` em HTML, robots, llms ou sitemaps.
- [ ] Search Console configurado.
- [ ] Bing Webmaster configurado.
- [ ] Rich Results e Schema Validator aprovados.
- [ ] PageSpeed analisado.
- [ ] SecurityHeaders/Observatory/SSL Labs avaliados.
