import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft } from "lucide-react";

import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { OrnamentDivider } from "@/components/layout/OrnamentDivider";
import { AnimeHero } from "@/components/anime/AnimeHero";
import { AnimeMetaGrid } from "@/components/anime/AnimeMetaGrid";
import { AnimeSynopsis } from "@/components/anime/AnimeSynopsis";
import { AnimeEpisodes } from "@/components/anime/AnimeEpisodes";
import { Button } from "@/components/ui/button";
import { fetchAnimeDetail, type AnimeDetailDto } from "@/lib/backend-api";
import type { AnimeDetailView } from "@/lib/anime-ui";

function toAnimeDetail(dto: AnimeDetailDto): AnimeDetailView {
  const durationMin = dto.episodes[0]?.durationSeconds ? Math.round(dto.episodes[0].durationSeconds / 60) : 24;
  return {
    slug: dto.slug,
    title: dto.titleDisplay,
    altTitle: dto.titleRomaji ?? dto.titleNative ?? dto.titleEnglish ?? undefined,
    year: dto.year ?? new Date().getFullYear(),
    status:
      dto.status === "FINISHED"
        ? "Finalizado"
        : dto.status === "HIATUS"
          ? "Em hiato"
          : "Em lançamento",
    studio: dto.studio ?? "—",
    episodesCount: dto.episodes.length,
    seasonsCount: 1,
    averageDurationMin: durationMin,
    ageRating: "13+",
    languages: ["LEG"],
    genres: dto.genres.length > 0 ? dto.genres : [dto.type],
    rating: 5,
    synopsisShort: dto.synopsis ?? "Sinopse ainda não disponível.",
    synopsisLong: dto.synopsis ?? "Sinopse ainda não disponível.",
    banner: dto.bannerUrl ?? dto.coverUrl ?? "",
    poster: dto.coverUrl ?? dto.bannerUrl ?? "",
    seasons: [
      {
        id: `${dto.id}-season-1`,
        label: dto.seasonLabel ?? "Temporada 1",
        episodes: dto.episodes.map((episode) => ({
          id: episode.id,
          number: episode.number,
          title: episode.title,
          durationMin: episode.durationSeconds ? Math.round(episode.durationSeconds / 60) : 24,
          airDate: new Date().toISOString(),
          thumbnail: episode.thumbnailUrl ?? dto.coverUrl ?? "",
          isNew: false,
        })),
      },
    ],
  };
}

function AnimeDetailPage() {
  const { slug = "" } = useParams() as { slug?: string };
  const animeQuery = useQuery({
    queryKey: ["anime-detail", slug],
    queryFn: () => fetchAnimeDetail(slug),
    enabled: Boolean(slug),
  });

  if (animeQuery.isLoading) {
    return (
      <div className="relative flex min-h-screen flex-col bg-background">
        <Header />
        <main className="flex flex-1 items-center justify-center px-6 py-32 text-ivory-muted">
          Carregando anime…
        </main>
      </div>
    );
  }

  if (!animeQuery.data) return <AnimeNotFound slug={slug} />;
  const anime = toAnimeDetail(animeQuery.data);

  return (
    <div className="relative flex min-h-screen flex-col bg-background">
      <Header />

      <main className="relative flex-1">
        <AnimeHero anime={anime} progress={null} />
        <AnimeMetaGrid anime={anime} />
        <AnimeSynopsis anime={anime} />
        <OrnamentDivider width="md" className="my-2 opacity-60" />
        <AnimeEpisodes anime={anime} progress={null} />
      </main>

      <Footer />
    </div>
  );
}

function AnimeNotFound({ slug }: { slug: string }) {
  return (
    <div className="relative flex min-h-screen flex-col bg-background">
      <Header />
      <main className="flex flex-1 items-center justify-center px-6 py-32">
        <div className="max-w-lg text-center">
          <span className="text-[11px] font-medium uppercase tracking-[0.32em] text-gold">
            Acervo
          </span>
          <h1 className="mt-5 font-serif text-5xl font-medium leading-[1.05] tracking-tight text-ivory md:text-6xl">
            Não encontramos
            <br />
            <span className="italic text-gold">esse título.</span>
          </h1>
          <OrnamentDivider width="sm" className="mt-8" />
          <p className="mt-8 text-sm leading-relaxed text-ivory-muted">
            O slug <span className="font-mono text-ivory">"{slug}"</span> não está
            no nosso catálogo.
          </p>
          <div className="mt-8">
            <Button asChild size="lg" className="rounded-full bg-gold px-7 text-onyx hover:bg-gold/90">
              <Link to="/">
                <ArrowLeft className="mr-2 h-4 w-4" />
                Voltar ao início
              </Link>
            </Button>
          </div>
        </div>
      </main>
    </div>
  );
}

export default AnimeDetailPage;
