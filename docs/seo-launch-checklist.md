# SEO launch checklist

Checklist operacional para publicar e validar a descoberta do Nekoflow em Google,
Bing, crawlers de busca e Cloudflare.

## Base de producao

- Configure `APP_PUBLIC_BASE_URL=https://nekoflow.com.br` antes do deploy.
- Confirme que `https://nekoflow.com.br/robots.txt` aponta para
  `https://nekoflow.com.br/sitemap.xml`.
- Confirme que os sitemaps nao contem `localhost`.
- Confirme que `/watch/*` nao esta bloqueado no `robots.txt`.
- Confirme que `/admin`, `/perfil`, `/notificacoes` e `/entrar` seguem bloqueados
  no `robots.txt` e com `noindex,nofollow` no HTML server-side.

## Politica para bots de IA

Decisao atual: permitir descoberta publica e nao bloquear crawlers de busca/IA no
`robots.txt`. O arquivo bloqueia apenas rotas privadas ou de conta.

Base revisada em 2026-07-05:

- OpenAI documenta `OAI-SearchBot` para busca, `GPTBot` para treinamento e
  `ChatGPT-User` como acesso iniciado por usuario.
- Google Search Central descreve `robots.txt` como controle de crawling, nao como
  mecanismo seguro para remover paginas do indice.
- Por isso, o Nekoflow nao mistura bloqueio de treinamento com bloqueio de busca
  nesta fase. Se a politica de uso de conteudo para treinamento mudar, revisar
  `GPTBot`, Google-Extended e outros agentes com documentacao oficial atualizada.

Pendencia intencional: nao adicionar nomes de bots sem fonte oficial confirmada.

## Google Search Console

- Adicionar propriedade de dominio para `nekoflow.com.br`.
- Verificar a propriedade via DNS.
- Enviar `https://nekoflow.com.br/sitemap.xml`.
- Inspecionar URLs principais:
  - `https://nekoflow.com.br/`
  - `https://nekoflow.com.br/explorar`
  - `https://nekoflow.com.br/anime/frieren-beyond-journeys-end`
  - `https://nekoflow.com.br/watch/frieren-beyond-journeys-end/1`
- Checar cobertura/indexacao depois do crawl.
- Checar relatorios de paginas com video se aparecerem.
- Checar Core Web Vitals depois de haver trafego real suficiente.

## Bing Webmaster Tools

- Adicionar `nekoflow.com.br` no Bing Webmaster Tools.
- Opcionalmente importar a propriedade do Google Search Console.
- Enviar `https://nekoflow.com.br/sitemap.xml`.
- Testar URLs publicas principais.
- Avaliar IndexNow depois que o fluxo de publicacao estiver estavel; nao ativar
  sem um gatilho confiavel de publicacao/atualizacao.

## Cloudflare

- DNS apontando para a VM de producao.
- SSL/TLS em Full Strict quando houver certificado valido na origem.
- Ativar Always Use HTTPS.
- Ativar Brotli.
- Cachear assets estaticos com hash respeitando os headers do Nginx.
- Nao cachear `/api/*` autenticado.
- Cachear `/robots.txt`, `/llms.txt` e `/sitemap*.xml` com TTL moderado.
- Nao criar regra que bloqueie `/watch/*`, `/anime/*`, `/assets/*` ou os sitemaps.
- Se houver websocket/API em tempo real no futuro, validar regras de proxy antes
  de ativar cache agressivo.

## Validacao manual

- `curl -I https://nekoflow.com.br/robots.txt`
- `curl https://nekoflow.com.br/robots.txt`
- `curl -I https://nekoflow.com.br/llms.txt`
- `curl https://nekoflow.com.br/llms.txt`
- `curl https://nekoflow.com.br/sitemap.xml`
- `curl https://nekoflow.com.br/sitemap-static.xml`
- `curl https://nekoflow.com.br/sitemap-animes.xml`
- `curl https://nekoflow.com.br/sitemap-episodes.xml`
- `curl https://nekoflow.com.br/sitemap-video.xml`

Cada sitemap deve ser XML valido e nao deve conter rotas privadas.
