import { useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { ChevronLeft, ChevronRight, Clock, Play } from "lucide-react";
import { SectionHeader } from "@/components/home/SectionHeader";
import { cn } from "@/lib/utils";
import type { AnimeDetailView, AnimeWatchProgress } from "@/lib/anime-ui";

interface AnimeEpisodesProps {
  anime: AnimeDetailView;
  progress: AnimeWatchProgress | null;
}

const PAGE_SIZE = 20; // grid 5×4 no desktop
const TRANSITION_MS = 280;

const formatDate = (iso: string) =>
  new Date(iso).toLocaleDateString("pt-BR", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });

export function AnimeEpisodes({ anime, progress }: AnimeEpisodesProps) {
  const [activeSeasonId, setActiveSeasonId] = useState(anime.seasons[0]?.id ?? "");
  const [page, setPage] = useState(1);
  const [isPending, setIsPending] = useState(false);
  const sectionRef = useRef<HTMLElement>(null);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const activeSeason = useMemo(
    () => anime.seasons.find((s) => s.id === activeSeasonId) ?? anime.seasons[0],
    [anime.seasons, activeSeasonId],
  );

  const totalPages = Math.max(
    1,
    Math.ceil((activeSeason?.episodes.length ?? 0) / PAGE_SIZE),
  );

  const visible = useMemo(() => {
    if (!activeSeason) return [];
    const start = (page - 1) * PAGE_SIZE;
    return activeSeason.episodes.slice(start, start + PAGE_SIZE);
  }, [activeSeason, page]);

  const goTo = (p: number) => {
    const next = Math.min(Math.max(1, p), totalPages);
    if (next === page) return;
    setIsPending(true);
    if (timeoutRef.current) clearTimeout(timeoutRef.current);
    timeoutRef.current = setTimeout(() => {
      setPage(next);
      timeoutRef.current = setTimeout(() => {
        setIsPending(false);
        sectionRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
      }, 120);
    }, TRANSITION_MS);
  };

  const switchSeason = (id: string) => {
    if (id === activeSeasonId) return;
    setIsPending(true);
    if (timeoutRef.current) clearTimeout(timeoutRef.current);
    timeoutRef.current = setTimeout(() => {
      setActiveSeasonId(id);
      setPage(1);
      timeoutRef.current = setTimeout(() => setIsPending(false), 120);
    }, TRANSITION_MS);
  };

  useEffect(() => {
    return () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    };
  }, []);

  const pageWindow = useMemo(() => {
    const max = 5;
    if (totalPages <= max) return Array.from({ length: totalPages }, (_, i) => i + 1);
    const start = Math.max(1, Math.min(page - 2, totalPages - max + 1));
    return Array.from({ length: max }, (_, i) => start + i);
  }, [page, totalPages]);

  if (!activeSeason) return null;

  return (
    <section
      ref={sectionRef}
      aria-label="Episódios"
      className="mx-auto w-full max-w-[1400px] scroll-mt-28 px-6 py-12 md:px-10 md:py-16"
    >
      <SectionHeader
        title="Episódios"
        subtitle={`${activeSeason.label} · ${activeSeason.episodes.length} episódios`}
      />

      {/* Seletor de temporada */}
      {anime.seasons.length > 1 ? (
        <div
          role="tablist"
          aria-label="Selecionar temporada"
          className="mb-6 flex flex-wrap gap-2"
        >
          {anime.seasons.map((s) => (
            <button
              key={s.id}
              role="tab"
              aria-selected={activeSeasonId === s.id}
              onClick={() => switchSeason(s.id)}
              className={cn(
                "rounded-full border px-4 py-1.5 text-xs uppercase tracking-[0.2em] transition-all duration-200",
                activeSeasonId === s.id
                  ? "border-gold bg-gold/10 text-gold"
                  : "border-border-subtle text-ivory-muted hover:border-gold/40 hover:text-gold",
              )}
            >
              {s.label}
            </button>
          ))}
        </div>
      ) : null}

      {/* Grid de cards (mesma linguagem dos Episódios recentes da home) */}
      <div
        aria-busy={isPending}
        aria-live="polite"
        style={{ transitionDuration: `${TRANSITION_MS}ms` }}
        className={cn(
          "grid grid-cols-2 gap-3 transition-all ease-out sm:grid-cols-3 sm:gap-4 md:grid-cols-4 lg:grid-cols-5",
          isPending ? "translate-y-1 opacity-0" : "translate-y-0 opacity-100",
        )}
      >
        {isPending
          ? Array.from({ length: PAGE_SIZE }).map((_, i) => (
              <EpisodeSkeleton key={`sk-${i}`} delay={i * 25} />
            ))
          : visible.map((ep) => {
              const isCurrent =
                progress?.animeTitle === anime.title &&
                progress.episodeNumber === ep.number;
              return (
                <article
                  key={ep.id}
                  className={cn(
                    "group/ep flex flex-col overflow-hidden rounded-xl border bg-surface transition-all duration-300 hover:border-gold/40 hover:shadow-[0_8px_30px_-10px_var(--gold-glow)]",
                    isCurrent ? "border-gold/60" : "border-border-subtle",
                  )}
                >
                  <Link
                    to={`/watch/${anime.slug}/${String(ep.number)}`}
                    aria-label={`Assistir episódio ${ep.number}: ${ep.title}`}
                    className="relative block aspect-video w-full overflow-hidden"
                  >
                    <img
                      src={ep.thumbnail}
                      alt=""
                      loading="lazy"
                      className="h-full w-full object-cover transition-transform duration-500 group-hover/ep:scale-[1.04]"
                    />
                    <div className="absolute inset-0 bg-gradient-to-t from-onyx/85 via-onyx/15 to-transparent" />
                    <div className="absolute inset-0 bg-gold/0 transition-colors duration-300 group-hover/ep:bg-gold/10" />

                    <div className="absolute inset-0 flex items-center justify-center opacity-0 transition-opacity duration-300 group-hover/ep:opacity-100">
                      <span className="inline-flex h-12 w-12 items-center justify-center rounded-full border border-gold/60 bg-onyx/60 text-gold backdrop-blur">
                        <Play className="h-5 w-5 fill-gold" />
                      </span>
                    </div>

                    {ep.isNew ? (
                      <span className="absolute left-2.5 top-2.5 inline-flex items-center gap-1 rounded-sm bg-gold/95 px-1.5 py-0.5 font-mono text-[10px] font-bold tracking-wider text-onyx">
                        NOVO
                      </span>
                    ) : null}

                    <span className="absolute bottom-2.5 right-2.5 inline-flex items-center gap-1 rounded-full bg-onyx/70 px-2 py-0.5 font-mono text-[10px] tracking-wider text-ivory backdrop-blur">
                      <Clock className="h-2.5 w-2.5" />
                      {ep.durationMin} min
                    </span>

                    {/* Barra de progresso se for o episódio em andamento */}
                    {isCurrent && progress ? (
                      <div className="absolute inset-x-0 bottom-0 h-[3px] bg-ivory/10">
                        <div
                          className="h-full bg-gold"
                          style={{ width: `${progress.progressPercent}%` }}
                        />
                      </div>
                    ) : null}
                  </Link>
                  <div className="flex flex-col gap-1 px-3.5 py-3">
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-[11px] uppercase tracking-[0.2em] text-gold/90">
                        Ep. {ep.number}
                      </span>
                      <span className="text-[10px] uppercase tracking-[0.18em] text-ivory-muted">
                        {formatDate(ep.airDate)}
                      </span>
                    </div>
                    <h3 className="line-clamp-2 font-serif text-base font-medium leading-tight text-ivory">
                      {ep.title}
                    </h3>
                  </div>
                </article>
              );
            })}
      </div>

      {/* Paginação */}
      {totalPages > 1 ? (
        <nav
          aria-label="Paginação de episódios"
          className="mt-10 flex flex-col items-center justify-between gap-4 border-t border-border-subtle pt-6 sm:flex-row"
        >
          <p className="font-mono text-[11px] uppercase tracking-[0.22em] text-ivory-muted">
            Página {String(page).padStart(2, "0")} de {String(totalPages).padStart(2, "0")}
          </p>

          <div
            className={cn(
              "flex items-center gap-1.5 transition-opacity duration-200",
              isPending && "pointer-events-none opacity-60",
            )}
          >
            <PageArrow direction="prev" disabled={page === 1} onClick={() => goTo(page - 1)} />
            {pageWindow[0] > 1 ? (
              <>
                <PageNumber n={1} active={page === 1} onClick={() => goTo(1)} />
                {pageWindow[0] > 2 ? <Ellipsis /> : null}
              </>
            ) : null}
            {pageWindow.map((n) => (
              <PageNumber key={n} n={n} active={page === n} onClick={() => goTo(n)} />
            ))}
            {pageWindow[pageWindow.length - 1] < totalPages ? (
              <>
                {pageWindow[pageWindow.length - 1] < totalPages - 1 ? <Ellipsis /> : null}
                <PageNumber
                  n={totalPages}
                  active={page === totalPages}
                  onClick={() => goTo(totalPages)}
                />
              </>
            ) : null}
            <PageArrow
              direction="next"
              disabled={page === totalPages}
              onClick={() => goTo(page + 1)}
            />
          </div>
        </nav>
      ) : null}
    </section>
  );
}

function PageNumber({
  n,
  active,
  onClick,
}: {
  n: number;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-current={active ? "page" : undefined}
      className={cn(
        "inline-flex h-9 min-w-9 items-center justify-center rounded-full border px-3 font-mono text-xs tracking-wider transition-all duration-200",
        active
          ? "border-gold bg-gold/10 text-gold"
          : "border-border-subtle text-ivory-muted hover:border-gold/50 hover:text-gold",
      )}
    >
      {String(n).padStart(2, "0")}
    </button>
  );
}

function PageArrow({
  direction,
  disabled,
  onClick,
}: {
  direction: "prev" | "next";
  disabled: boolean;
  onClick: () => void;
}) {
  const Icon = direction === "prev" ? ChevronLeft : ChevronRight;
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={direction === "prev" ? "Página anterior" : "Próxima página"}
      className={cn(
        "inline-flex h-9 w-9 items-center justify-center rounded-full border border-border-subtle text-ivory transition-all duration-200",
        disabled ? "cursor-not-allowed opacity-30" : "hover:border-gold/60 hover:text-gold",
      )}
    >
      <Icon className="h-4 w-4" />
    </button>
  );
}

function Ellipsis() {
  return (
    <span className="px-1 font-mono text-xs text-ivory-muted/60" aria-hidden>
      ···
    </span>
  );
}

function EpisodeSkeleton({ delay = 0 }: { delay?: number }) {
  return (
    <div
      className="flex flex-col overflow-hidden rounded-xl border border-border-subtle bg-surface"
      aria-hidden
    >
      <div
        className="aspect-video w-full animate-pulse bg-surface-elevated"
        style={{ animationDelay: `${delay}ms` }}
      />
      <div className="flex flex-col gap-2 px-3.5 py-3">
        <div
          className="h-3.5 w-4/5 animate-pulse rounded-sm bg-surface-elevated"
          style={{ animationDelay: `${delay + 60}ms` }}
        />
        <div
          className="h-2.5 w-1/3 animate-pulse rounded-sm bg-surface-elevated/70"
          style={{ animationDelay: `${delay + 120}ms` }}
        />
      </div>
    </div>
  );
}
