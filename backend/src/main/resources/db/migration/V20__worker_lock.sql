-- Lock leve baseado em lease para o worker automatico (evita execucao duplicada
-- do poll RSS entre agendamentos concorrentes ou multiplas instancias).
-- O lease expira sozinho, entao um processo que morrer no meio nao deixa o lock
-- preso para sempre.
create table worker_lock (
    name text primary key,
    locked_until timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

insert into worker_lock (name, locked_until) values ('rss-poll', now())
    on conflict (name) do nothing;
