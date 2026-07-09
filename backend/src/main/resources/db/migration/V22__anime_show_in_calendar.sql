-- Curadoria do calendario: o admin oculta animes do calendario sem apagar nada.
-- A sincronizacao com o AniList respeita a escolha e nunca readiciona ocultos.
alter table anime add column if not exists show_in_calendar boolean not null default true;
