import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import {
  ArrowLeft,
  Loader2,
  Maximize2,
} from "lucide-react";

import { Header } from "@/components/layout/Header";
import { Button } from "@/components/ui/button";
import { EpisodeComments } from "@/components/player/EpisodeComments";
import { useAuth } from "@/hooks/use-auth";
import { fetchAnimeDetail, fetchWatchPlayer, updateProgress } from "@/lib/backend-api";
import { cn } from "@/lib/utils";

const PROGRESS_SYNC_INTERVAL_SECONDS = 15;

function PlayerPage() {
  const { slug = "", episodeNumber = "" } = useParams() as { slug?: string; episodeNumber?: string };
  const watchQuery = useQuery({
    queryKey: ["watch-player", slug, episodeNumber],
    queryFn: () => fetchWatchPlayer(slug, episodeNumber),
    enabled: Boolean(slug && episodeNumber),
  });
  const animeQuery = useQuery({
    queryKey: ["anime-detail", slug],
    queryFn: () => fetchAnimeDetail(slug),
    enabled: Boolean(slug),
  });

  if (watchQuery.isLoading || animeQuery.isLoading) {
    return (
      <div className="relative flex min-h-screen flex-col bg-background">
        <Header />
        <main className="flex flex-1 items-center justify-center px-6 py-32 text-ivory-muted">
          Carregando episódio…
        </main>
      </div>
    );
  }

  if (!watchQuery.data || !animeQuery.data) return <PlayerNotFound />;
  return <PlayerInner data={watchQuery.data} anime={animeQuery.data} />;
}

function PlayerInner({
  data,
  anime,
}: {
  data: Awaited<ReturnType<typeof fetchWatchPlayer>>;
  anime: Awaited<ReturnType<typeof fetchAnimeDetail>>;
}) {
  const { isAuthenticated } = useAuth();
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const lastSyncedRef = useRef<number>(0);
  const [currentSeconds, setCurrentSeconds] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const totalSeconds = data.durationSeconds ?? 24 * 60;
  const playbackUrl = data.embedUrl ?? data.playerUrl ?? null;

  const playerKind = useMemo(() => detectPlayerKind(playbackUrl), [playbackUrl]);
  const hasRealPlayer = Boolean(playbackUrl);
  const canTrackProgress = playerKind === "video";
  const progressPercent = totalSeconds > 0 ? (currentSeconds / totalSeconds) * 100 : 0;

  useEffect(() => {
    setCurrentSeconds(0);
    setIsLoading(true);
    lastSyncedRef.current = 0;
  }, [data.episodeId]);

  useEffect(() => {
    if (!isAuthenticated || !anime.id || !data.episodeId || !playbackUrl) return;
    void updateProgress({
      animeId: anime.id,
      episodeId: data.episodeId,
      progressSeconds: 1,
      durationSeconds: totalSeconds,
    }).catch(() => undefined);
  }, [anime.id, data.episodeId, isAuthenticated, playbackUrl, totalSeconds]);

  useEffect(() => {
    if (!isAuthenticated || !canTrackProgress) return;
    if (!anime.id || !data.episodeId) return;
    if (currentSeconds === 0) return;
    if (currentSeconds - lastSyncedRef.current < PROGRESS_SYNC_INTERVAL_SECONDS && currentSeconds !== totalSeconds) {
      return;
    }

    lastSyncedRef.current = currentSeconds;
    void updateProgress({
      animeId: anime.id,
      episodeId: data.episodeId,
      progressSeconds: currentSeconds,
      durationSeconds: totalSeconds,
    }).catch(() => undefined);
  }, [anime.id, canTrackProgress, currentSeconds, data.episodeId, isAuthenticated, totalSeconds]);

  useEffect(
    () => () => {
      if (!isAuthenticated || !canTrackProgress || !anime.id || !data.episodeId || currentSeconds <= 0) return;
      void updateProgress({
        animeId: anime.id,
        episodeId: data.episodeId,
        progressSeconds: currentSeconds,
        durationSeconds: totalSeconds,
      }).catch(() => undefined);
    },
    [anime.id, canTrackProgress, currentSeconds, data.episodeId, isAuthenticated, totalSeconds],
  );

  const currentIndex = anime.episodes.findIndex((episode) => episode.number === data.episodeNumber);
  const prevEpisode = currentIndex > 0 ? anime.episodes[currentIndex - 1] : null;
  const nextEpisode = currentIndex >= 0 && currentIndex < anime.episodes.length - 1 ? anime.episodes[currentIndex + 1] : null;

  return (
    <div className="relative flex min-h-screen flex-col bg-background">
      <Header />

      <main className="relative flex-1">
        <div className="mx-auto w-full max-w-[1500px] px-4 py-6 md:px-8 md:py-8 lg:px-10 lg:py-10">
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-12 lg:gap-6">
            <div className="flex flex-col gap-6 lg:col-span-8">
              <section className="relative isolate w-full overflow-hidden rounded-xl border border-border-subtle bg-black">
                <div className="relative aspect-video w-full">
                  <Link
                    to={`/anime/${anime.slug}`}
                    className="absolute left-4 top-4 z-20 inline-flex items-center gap-2 rounded-full border border-ivory/20 bg-onyx/60 px-3 py-1.5 text-[11px] uppercase tracking-[0.22em] text-ivory backdrop-blur transition-all duration-200 hover:border-gold/60 hover:text-gold"
                  >
                    <ArrowLeft className="h-3 w-3" />
                    Detalhes
                  </Link>

                  {playbackUrl && playerKind === "iframe" ? (
                    <iframe
                      key={data.episodeId}
                      src={playbackUrl}
                      title={`${data.animeTitle} - ${data.episodeTitle}`}
                      allow="autoplay; encrypted-media; picture-in-picture; fullscreen"
                      allowFullScreen
                      referrerPolicy="strict-origin-when-cross-origin"
                      className="absolute inset-0 h-full w-full border-0"
                      onLoad={() => setIsLoading(false)}
                    />
                  ) : null}

                  {playbackUrl && playerKind === "video" ? (
                    <video
                      key={data.episodeId}
                      ref={videoRef}
                      src={playbackUrl}
                      poster={data.thumbnailUrl ?? anime.bannerUrl ?? anime.coverUrl ?? undefined}
                      controls
                      autoPlay
                      playsInline
                      preload="metadata"
                      className="absolute inset-0 h-full w-full bg-black object-contain"
                      onLoadedData={() => setIsLoading(false)}
                      onTimeUpdate={(event) => setCurrentSeconds(event.currentTarget.currentTime)}
                    />
                  ) : null}

                  {!playbackUrl ? (
                    <div className="absolute inset-0 flex flex-col items-center justify-center gap-4 bg-onyx/70 px-6 text-center">
                      <p className="font-serif text-2xl text-ivory">Player não configurado</p>
                      <p className="max-w-xl text-sm text-ivory-muted">
                        Este episódio ainda não recebeu `embed_url` ou `player_url` no admin.
                      </p>
                    </div>
                  ) : null}

                  {isLoading && hasRealPlayer ? (
                    <div className="absolute inset-0 z-10 flex flex-col items-center justify-center gap-3 bg-onyx/40 backdrop-blur-sm">
                      <Loader2 className="h-10 w-10 animate-spin text-gold" />
                      <p className="font-mono text-[11px] uppercase tracking-[0.3em] text-ivory-muted">
                        Carregando player…
                      </p>
                    </div>
                  ) : null}
                </div>
              </section>

              <section className="px-1">
                <span className="inline-flex items-center gap-2 text-[11px] font-medium uppercase tracking-[0.32em] text-gold">
                  <span className="h-px w-8 bg-gold/60" />
                  Episódio {String(data.episodeNumber).padStart(2, "0")} · {Math.round(totalSeconds / 60)} min
                </span>
                <h1 className="mt-3 font-serif text-[28px] font-medium leading-[1.05] tracking-tight text-ivory md:text-[36px]">
                  {data.episodeTitle}
                </h1>
                <p className="mt-2 font-serif text-base italic text-ivory-muted">
                  <Link to={`/anime/${anime.slug}`} className="underline-offset-4 transition-colors hover:text-gold hover:underline">
                    {anime.titleDisplay}
                  </Link>
                </p>
                <p className="mt-5 max-w-2xl text-sm leading-relaxed text-ivory/80">
                  {data.summary ?? anime.synopsis ?? "Resumo ainda não disponível."}
                </p>

                {canTrackProgress ? (
                  <div className="mt-6 max-w-xl">
                    <div className="mb-2 flex items-center justify-between text-[11px] uppercase tracking-[0.18em] text-ivory-muted">
                      <span>Progresso</span>
                      <span>{formatTime(currentSeconds)} / {formatTime(totalSeconds)}</span>
                    </div>
                    <div className="h-1.5 overflow-hidden rounded-full bg-ivory/10">
                      <div className="h-full bg-gold transition-all duration-150" style={{ width: `${progressPercent}%` }} />
                    </div>
                  </div>
                ) : null}

              </section>
            </div>

            <div className="lg:col-span-4">
              <aside className="flex h-full flex-col overflow-hidden rounded-xl border border-border-subtle bg-surface/60 backdrop-blur">
                <header className="flex flex-col gap-1 border-b border-border-subtle px-5 py-4">
                  <span className="font-mono text-[10px] uppercase tracking-[0.28em] text-gold">Episódios</span>
                  <h2 className="truncate font-serif text-lg font-medium leading-tight text-ivory">{anime.titleDisplay}</h2>
                </header>
                <div className="flex-1 overflow-y-auto px-3 py-3 [scrollbar-width:thin]">
                  <ul className="flex flex-col gap-1.5">
                    {anime.episodes.map((episode) => {
                      const active = episode.number === data.episodeNumber;
                      return (
                        <li key={episode.id}>
                          <Link
                            to={`/watch/${anime.slug}/${String(episode.number)}`}
                            className={cn(
                              "group/ep relative flex gap-3 rounded-lg border p-2 transition-all duration-200",
                              active ? "border-gold/60 bg-gold/[0.06]" : "border-transparent hover:border-gold/30 hover:bg-surface-elevated/60",
                            )}
                          >
                            <div className="relative aspect-video w-28 flex-shrink-0 overflow-hidden rounded-md bg-onyx">
                              <img src={episode.thumbnailUrl ?? anime.coverUrl ?? ""} alt="" className="h-full w-full object-cover" />
                            </div>
                            <div className="flex min-w-0 flex-1 flex-col justify-center gap-0.5">
                              <span className={cn("font-mono text-[10px] uppercase tracking-[0.2em]", active ? "text-gold" : "text-gold/80")}>
                                Ep. {String(episode.number).padStart(2, "0")}
                              </span>
                              <h3 className="line-clamp-2 font-serif text-sm leading-snug text-ivory">{episode.title}</h3>
                            </div>
                          </Link>
                        </li>
                      );
                    })}
                  </ul>
                </div>
              </aside>
            </div>
          </div>
        </div>
        <EpisodeComments episodeId={data.episodeId} episodeTitle={data.episodeTitle} />
      </main>
    </div>
  );
}

function formatTime(sec: number) {
  const minutes = Math.floor(sec / 60);
  const seconds = Math.floor(sec % 60);
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}

function detectPlayerKind(url: string | null) {
  if (!url) return "none";
  const normalized = url.toLowerCase();
  if (
    normalized.endsWith(".mp4") ||
    normalized.endsWith(".webm") ||
    normalized.endsWith(".ogg") ||
    normalized.includes(".mp4?") ||
    normalized.includes(".webm?") ||
    normalized.includes(".ogg?")
  ) {
    return "video";
  }
  return "iframe";
}

function PlayerNotFound() {
  const { slug, episodeNumber } = useParams() as { slug?: string; episodeNumber?: string };
  return (
    <div className="relative flex min-h-screen flex-col bg-background">
      <Header />
      <main className="flex flex-1 items-center justify-center px-6 py-32">
        <div className="max-w-lg text-center">
          <span className="text-[11px] font-medium uppercase tracking-[0.32em] text-gold">Player</span>
          <h1 className="mt-5 font-serif text-5xl font-medium leading-[1.05] tracking-tight text-ivory md:text-6xl">
            Episódio
            <br />
            <span className="italic text-gold">não encontrado.</span>
          </h1>
          <p className="mt-8 text-sm leading-relaxed text-ivory-muted">
            Não localizamos o episódio <span className="font-mono text-ivory">{episodeNumber}</span> de{" "}
            <span className="font-mono text-ivory">"{slug}"</span>.
          </p>
          <div className="mt-8 flex items-center justify-center gap-3">
            <Button asChild size="lg" className="rounded-full bg-gold px-7 text-onyx hover:bg-gold/90">
              <Link to={`/anime/${slug}`}>
                <ArrowLeft className="mr-2 h-4 w-4" />
                Voltar à ficha
              </Link>
            </Button>
          </div>
        </div>
      </main>
    </div>
  );
}

export default PlayerPage;
