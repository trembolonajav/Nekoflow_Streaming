-- Garante a tabela crawl_jobs para instalacoes NOVAS (do zero).
--
-- Nota de reconciliacao: a producao ja criou crawl_jobs por uma migration
-- propria (V16__create_worker_catalog_jobs.sql) que nunca foi para o main. Para
-- nao colidir com aquela V16, esta migration entra no fim (V21) e e idempotente
-- (create ... if not exists), entao em bases que ja tem a tabela vira no-op.
create table if not exists crawl_jobs (
    id uuid primary key default gen_random_uuid(),
    url text not null,
    source text not null default 'nyaa',
    status text not null default 'pending',
    status_reason text,
    pages_total integer,
    pages_done integer not null default 0,
    items_found integer not null default 0,
    items_new integer not null default 0,
    items_filtered integer not null default 0,
    items_failed integer not null default 0,
    release_group_filter text,
    quality_filter text,
    started_at timestamptz,
    finished_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists crawl_jobs_status_idx on crawl_jobs(status);
create index if not exists crawl_jobs_created_at_idx on crawl_jobs(created_at desc);
