import { Link } from "react-router-dom";
import { Play, Plus, Share2, Heart, Star } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { AnimeDetailView, AnimeWatchProgress } from "@/lib/anime-ui";

interface AnimeHeroProps {
  anime: AnimeDetailView;
  progress: AnimeWatchProgress | null;
  isAuthenticated?: boolean;
  isInWatchlist?: boolean;
  watchlistBusy?: boolean;
  onToggleWatchlist?: () => void;
}

export function AnimeHero({
  anime,
  progress,
  isAuthenticated = false,
  isInWatchlist = false,
  watchlistBusy = false,
  onToggleWatchlist,
}: AnimeHeroProps) {
  return (
    <section
      aria-label={`Detalhes de ${anime.title}`}
      className="relative isolate overflow-hidden border-b border-border-subtle"
    >
      {/* Banner cinematográfico */}
      <div className="absolute inset-0 -z-10">
        <img
          src={anime.banner}
          alt=""
          className="h-full w-full object-cover object-center"
        />
        <div className="absolute inset-0 bg-gradient-to-r from-background via-background/85 to-background/30" />
        <div className="absolute inset-0 bg-gradient-to-t from-background via-background/30 to-background/70" />
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_bottom_left,_var(--gold-glow),_transparent_55%)] opacity-40" />
      </div>

      <div className="mx-auto flex max-w-[1400px] flex-col gap-8 px-6 pb-14 pt-32 md:flex-row md:gap-10 md:px-10 md:pb-20 md:pt-40">
        {/* Poster destaque */}
        <div className="relative mx-auto w-44 flex-shrink-0 md:mx-0 md:w-64">
          <div className="aspect-[2/3] overflow-hidden rounded-xl border border-gold/20 bg-surface shadow-[0_30px_80px_-20px_rgba(0,0,0,0.7)]">
            <img
              src={anime.poster}
              alt={`Poster de ${anime.title}`}
              className="h-full w-full object-cover"
            />
          </div>
          <div className="pointer-events-none absolute -inset-3 -z-10 rounded-2xl bg-gold-glow blur-2xl" />
        </div>

        {/* Conteúdo editorial */}
        <div className="flex flex-1 flex-col text-center md:text-left">
          <span className="inline-flex items-center gap-2 self-center text-[11px] font-medium uppercase tracking-[0.32em] text-gold md:self-start">
            <span className="h-px w-8 bg-gold/60" />
            {anime.status}
          </span>

          <h1 className="mt-4 font-serif text-[36px] font-medium leading-[1.05] tracking-tight text-ivory md:text-[56px]">
            {anime.title}
          </h1>
          {anime.altTitle ? (
            <p className="mt-1 font-serif text-base italic text-ivory-muted md:text-lg">
              {anime.altTitle}
            </p>
          ) : null}

          {/* Tags */}
          <div className="mt-5 flex flex-wrap items-center justify-center gap-2 md:justify-start">
            {anime.genres.map((g) => (
              <span
                key={g}
                className="inline-flex items-center rounded-full border border-border-subtle bg-surface/60 px-3 py-1 text-[11px] uppercase tracking-[0.18em] text-ivory backdrop-blur"
              >
                {g}
              </span>
            ))}
            <span className="inline-flex items-center gap-1 rounded-full border border-gold/30 bg-onyx/40 px-3 py-1 text-[11px] uppercase tracking-[0.18em] text-gold">
              {Array.from({ length: anime.rating }).map((_, i) => (
                <Star key={i} className="h-3 w-3 fill-gold" />
              ))}
            </span>
          </div>

          {/* Meta linha */}
          <div className="mt-4 flex flex-wrap items-center justify-center gap-x-3 gap-y-1 text-xs uppercase tracking-[0.2em] text-ivory-muted md:justify-start">
            <span>{anime.year}</span>
            <span className="h-1 w-1 rounded-full bg-ivory-muted/50" />
            <span>{anime.studio}</span>
            <span className="h-1 w-1 rounded-full bg-ivory-muted/50" />
            <span>{anime.languages.join(" · ")}</span>
            <span className="h-1 w-1 rounded-full bg-ivory-muted/50" />
            <span>{anime.ageRating}</span>
          </div>

          <p className="mt-6 max-w-2xl text-sm leading-relaxed text-ivory/85 md:text-base">
            {anime.synopsisShort}
          </p>

          {/* Ações */}
          <div className="mt-8 flex flex-wrap items-center justify-center gap-3 md:justify-start">
            <Button
              asChild
              size="lg"
              className="group/cta h-12 rounded-full bg-gold px-7 text-onyx hover:bg-gold/90"
            >
              <Link
                to={`/watch/${anime.slug}/${String(progress?.episodeNumber ?? 1)}`}
              >
                <Play className="mr-2 h-4 w-4 fill-onyx" />
                {progress ? `Continuar Ep. ${progress.episodeNumber}` : "Assistir agora"}
              </Link>
            </Button>
            <Button
              size="lg"
              variant="ghost"
              onClick={onToggleWatchlist}
              disabled={watchlistBusy}
              className="h-12 rounded-full border border-ivory/20 px-6 text-ivory hover:border-ivory/40 hover:bg-ivory/5 hover:text-ivory"
            >
              <Plus className="mr-2 h-4 w-4" />
              {isAuthenticated && isInWatchlist ? "Na minha lista" : "Minha lista"}
            </Button>
            <button
              type="button"
              aria-label="Favoritar"
              className="inline-flex h-12 w-12 items-center justify-center rounded-full border border-ivory/15 text-ivory transition-colors hover:border-gold/50 hover:text-gold"
            >
              <Heart className="h-4 w-4" />
            </button>
            <button
              type="button"
              aria-label="Compartilhar"
              className="inline-flex h-12 w-12 items-center justify-center rounded-full border border-ivory/15 text-ivory transition-colors hover:border-gold/50 hover:text-gold"
            >
              <Share2 className="h-4 w-4" />
            </button>
          </div>

          {/* Barra de progresso atual */}
          {progress ? (
            <div className="mt-6 w-full max-w-md self-center md:self-start">
              <div className="flex items-center justify-between text-[11px] uppercase tracking-[0.22em] text-ivory-muted">
                <span>Em andamento</span>
                <span>Faltam {progress.remainingMinutes} min</span>
              </div>
              <div className="mt-2 h-[3px] overflow-hidden rounded-full bg-ivory/10">
                <div
                  className={cn("h-full bg-gold transition-all duration-500")}
                  style={{ width: `${progress.progressPercent}%` }}
                />
              </div>
            </div>
          ) : null}
        </div>
      </div>
    </section>
  );
}
