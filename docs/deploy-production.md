# Deploy de producao

Este projeto usa um fluxo simples:

```text
maquina local -> branch curta -> main no GitHub -> VPS puxa main -> Docker Compose de producao
```

Nao e necessario manter uma branch `dev` agora. A `main` deve representar codigo pronto para producao, e mudancas devem ser feitas em branches curtas.

## Arquivos de ambiente

Arquivos reais de ambiente nunca devem ir para o GitHub:

- `.env`
- `.env.local`
- `.env.production`
- dumps `.sql`
- backups `.dump`, `.backup`, `.bak`

Use `env.production.example` como modelo e crie o arquivo real diretamente na VPS:

```bash
cp env.production.example .env.production
```

Depois edite `.env.production` com segredos reais.

## Variaveis obrigatorias em producao

Com `SPRING_PROFILES_ACTIVE=prod`, o backend falha ao iniciar se configuracoes criticas estiverem ausentes ou fracas:

- `APP_JWT_SECRET`: minimo 48 caracteres e nao pode usar o valor de desenvolvimento.
- `APP_WORKER_WEBHOOK_SECRET`: minimo 32 caracteres.
- `DB_PASSWORD`: minimo 16 caracteres.
- `APP_CORS_ALLOWED_ORIGINS`: sem `*`, `localhost` ou `127.0.0.1`.
- `APP_BOOTSTRAP_ADMIN_PASSWORD`: minimo 16 caracteres se `APP_BOOTSTRAP_ENABLED=true`.

Recomendacao: depois de criar o admin inicial, manter `APP_BOOTSTRAP_ENABLED=false` na VPS.

## Subir na VPS

Na VPS, dentro da pasta do projeto:

```bash
git pull origin main
docker compose -f docker-compose.prod.yml --env-file .env.production up -d --build
```

Ver logs:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.production logs -f backend
docker compose -f docker-compose.prod.yml --env-file .env.production logs -f frontend
```

## Validar que nao e Vite dev

Em producao, o frontend deve servir `dist/` via Nginx.

No DevTools > Network, nao deve aparecer:

```text
/@vite/client
/@react-refresh
/src/main.tsx
/src/App.tsx
```

Deve aparecer algo como:

```text
/assets/index-xxxxx.js
/assets/index-xxxxx.css
```

## Validacao pos-deploy

Abrir e testar:

- `https://nekoflow.com.br/`
- `https://nekoflow.com.br/explorar`
- `https://nekoflow.com.br/calendario`
- uma pagina `/anime/:slug`
- uma pagina `/watch/:slug/:episodeNumber`
- `https://nekoflow.com.br/admin`
- `https://nekoflow.com.br/robots.txt`
- `https://nekoflow.com.br/sitemap.xml`

Tambem confirmar:

- usuario comum nao acessa `/admin`;
- admin acessa `/admin`;
- login/logout funcionam;
- player funciona;
- comentarios carregam;
- console do navegador sem erro critico.

## Cache e headers

O Nginx de producao em `nginx.frontend.conf` aplica:

- cache longo para `/assets/`, porque os arquivos tem hash;
- `no-cache` para `index.html` e rotas SPA;
- cache curto para `robots.txt` e `sitemap.xml`;
- gzip;
- headers basicos de seguranca.

Nao foi adicionada CSP rigida por enquanto para evitar quebrar imagens, embeds e player externo. CSP deve ser uma etapa futura controlada.
