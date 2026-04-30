import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Play } from "lucide-react";

import { CarouselArrows, useHorizontalScroll } from "@/components/home/CarouselArrows";
import { Header } from "@/components/layout/Header";
import { OrnamentDivider } from "@/components/layout/OrnamentDivider";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/hooks/use-auth";
import { fetchProfile, fetchPublicHome, type ContinueWatchingDto } from "@/lib/backend-api";
import { cn } from "@/lib/utils";

const RECENT_PAGE_SIZE = 15;

function Home() {
  const [activeHeroIndex, setActiveHeroIndex] = useState(0);
  const { isAuthenticated, isReady } = useAuth();
  const homeQuery = useQuery({
    queryKey: ["public-home"],
    queryFn: fetchPublicHome,
  });
  const profileQuery = useQuery({
    queryKey: ["me-profile"],
    queryFn: fetchProfile,
    enabled: isReady && isAuthenticated,
  });

  const data = homeQuery.data;
  const sections = buildHomeSections(data?.sections ?? [], profileQuery.data?.continueWatching ?? [], isAuthenticated);
  const heroItems = data?.hero.items ?? [];
  const activeHero = heroItems[activeHeroIndex] ?? heroItems[0];

  useEffect(() => {
    if (activeHeroIndex >= heroItems.length) {
      setActiveHeroIndex(0);
    }
  }, [activeHeroIndex, heroItems.length]);

  const goToPrevHero = () => {
    setActiveHeroIndex((current) => (current - 1 + heroItems.length) % heroItems.length);
  };

  const goToNextHero = () => {
    setActiveHeroIndex((current) => (current + 1) % heroItems.length);
  };

  return (
    <div className="relative flex min-h-screen flex-col bg-background">
      <Header />

      <main className="relative flex-1">
        {activeHero ? (
          <section className="relative isolate overflow-hidden border-b border-border-subtle">
            <div className="relative h-[72vh] min-h-[520px] w-full">
              <HeroImage
                src={activeHero.bannerUrl ?? activeHero.coverUrl ?? ""}
                fallbackSrc={activeHero.coverUrl ?? activeHero.bannerUrl ?? ""}
              />
              <div className="absolute inset-0 bg-gradient-to-r from-background via-background/80 to-transparent md:via-background/60" />
              <div className="absolute inset-0 bg-gradient-to-t from-background via-transparent to-background/40" />
            </div>
            {heroItems.length > 1 ? (
              <CarouselArrows
                onPrev={goToPrevHero}
                onNext={goToNextHero}
                className="absolute bottom-8 right-6 z-20 md:right-10"
              />
            ) : null}
            <div className="absolute inset-0 z-10 flex items-center">
              <div className="mx-auto w-full max-w-[1400px] px-6 md:px-10">
                <div className="max-w-xl">
                  <span className="inline-flex items-center gap-2 text-[11px] font-medium uppercase tracking-[0.32em] text-gold">
                    <span className="h-px w-8 bg-gold/60" />
                    {data?.hero.tag ?? "Destaque editorial"}
                  </span>
                  <h1 className="mt-6 font-serif text-[40px] font-medium leading-[1.02] tracking-tight text-ivory md:text-[64px]">
                    {activeHero.title}
                  </h1>
                  <p className="mt-5 max-w-lg text-sm leading-relaxed text-ivory/85 md:text-base">
                    {activeHero.subtitle ?? "Curadoria principal da NekoFlow."}
                  </p>
                  <div className="mt-9 flex flex-wrap items-center gap-3">
                    <Button asChild size="lg" className="h-12 rounded-full bg-gold px-7 text-onyx hover:bg-gold/90">
                      <Link to={`/watch/${activeHero.slug}/1`}>
                        <Play className="mr-2 h-4 w-4 fill-onyx" />
                        {data?.hero.ctaLabel ?? "Assistir agora"}
                      </Link>
                    </Button>
                    <Button
                      asChild
                      size="lg"
                      variant="ghost"
                      className="h-12 rounded-full border border-ivory/20 px-7 text-ivory hover:border-ivory/40 hover:bg-ivory/5 hover:text-ivory"
                    >
                      <Link to={`/anime/${activeHero.slug}`}>Ver detalhes</Link>
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          </section>
        ) : null}

        {sections.map((section, index) => (
          <div key={section.code}>
            {index > 0 ? <OrnamentDivider width="md" className="my-2 opacity-60" /> : null}
            <HomeSection section={section} />
          </div>
        ))}

        <div className="h-16 md:h-24" />
      </main>
    </div>
  );
}

type HomeSectionData = Awaited<ReturnType<typeof fetchPublicHome>>["sections"][number];
type HomeSectionItem = HomeSectionData["items"][number];

function buildHomeSections(
  sections: HomeSectionData[],
  continueWatching: ContinueWatchingDto[],
  isAuthenticated: boolean,
): HomeSectionData[] {
  if (!isAuthenticated) {
    return sections.filter((section) => section.code !== "continue");
  }

  const continueItems = continueWatching.map(toContinueHomeItem);

  return sections
    .map((section) => (
      section.code === "continue"
        ? { ...section, mode: "PERSONALIZED", items: continueItems }
        : section
    ))
    .filter((section) => section.code !== "continue" || section.items.length > 0);
}

function toContinueHomeItem(item: ContinueWatchingDto): HomeSectionItem {
  return {
    id: item.episodeId,
    animeId: item.animeId,
    episodeId: item.episodeId,
    title: item.animeTitle,
    subtitle: `Ep. ${item.episodeNumber} - ${item.episodeTitle}`,
    coverUrl: item.coverUrl ?? item.thumbnailUrl,
    bannerUrl: item.thumbnailUrl,
    previewUrl: null,
    slug: item.animeSlug,
  };
}

function HomeSection({ section }: { section: HomeSectionData }) {
  const [page, setPage] = useState(1);
  const carouselRef = useRef<HTMLDivElement | null>(null);
  const carousel = useHorizontalScroll(carouselRef);
  const isRecent = section.code === "recent";
  const isSeason = section.code === "season";
  const totalPages = Math.max(1, Math.ceil(section.items.length / RECENT_PAGE_SIZE));
  const visibleItems = isRecent
    ? section.items.slice((page - 1) * RECENT_PAGE_SIZE, page * RECENT_PAGE_SIZE)
    : section.items;

  useEffect(() => {
    if (page > totalPages) {
      setPage(totalPages);
    }
  }, [page, totalPages]);

  const goToPrevPage = () => setPage((current) => Math.max(1, current - 1));
  const goToNextPage = () => setPage((current) => Math.min(totalPages, current + 1));

  return (
    <section className="mx-auto w-full max-w-[1400px] px-4 py-10 md:px-10 md:py-14">
      <div className="mb-6 flex items-end justify-between gap-4">
        <div>
          <h2 className="font-serif text-3xl text-ivory">{section.title}</h2>
          <p className="mt-1 text-sm text-ivory-muted">
            {section.code === "continue"
              ? getContinueDescription(section)
              : section.mode === "AUTOMATIC"
                ? "Atualizado automaticamente a partir do catálogo."
                : "Curadoria editorial conectada ao painel admin."}
          </p>
        </div>
        {isSeason && section.items.length > 5 ? (
          <CarouselArrows onPrev={carousel.prev} onNext={carousel.next} />
        ) : null}
      </div>

      {isSeason ? (
        <div
          ref={carouselRef}
          className="-mx-4 flex snap-x gap-4 overflow-x-auto px-4 pb-2 [scrollbar-width:none] md:mx-0 md:px-0 [&::-webkit-scrollbar]:hidden"
        >
          {section.items.map((item) => (
            <HomeCard
              key={item.id}
              item={item}
              sectionCode={section.code}
              className="w-[calc((100%_-_1rem)/2)] shrink-0 snap-start md:w-[calc((100%_-_3rem)/4)] lg:w-[calc((100%_-_4rem)/5)]"
            />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-4 md:grid-cols-4 lg:grid-cols-5">
          {visibleItems.map((item) => (
            <HomeCard key={item.id} item={item} sectionCode={section.code} />
          ))}
        </div>
      )}

      {isRecent && totalPages > 1 ? (
        <div className="mt-6 flex items-center justify-center gap-3">
          <button
            type="button"
            onClick={goToPrevPage}
            disabled={page === 1}
            className="h-9 rounded-full border border-gold/25 px-4 text-sm text-ivory transition-colors hover:border-gold/60 hover:text-gold disabled:cursor-not-allowed disabled:opacity-40"
          >
            Anterior
          </button>
          <span className="text-xs uppercase tracking-[0.18em] text-ivory-muted">
            {page} / {totalPages}
          </span>
          <button
            type="button"
            onClick={goToNextPage}
            disabled={page === totalPages}
            className="h-9 rounded-full border border-gold/25 px-4 text-sm text-ivory transition-colors hover:border-gold/60 hover:text-gold disabled:cursor-not-allowed disabled:opacity-40"
          >
            Próxima
          </button>
        </div>
      ) : null}
    </section>
  );
}

function HomeCard({
  item,
  sectionCode,
  className,
}: {
  item: HomeSectionItem;
  sectionCode: string;
  className?: string;
}) {
  return (
    <article className={cn("group overflow-hidden rounded-xl border border-border-subtle bg-surface transition-all duration-300 hover:border-gold/40", className)}>
      <Link
        to={item.episodeId ? `/watch/${item.slug}/${getEpisodeNumberFromSubtitle(item.subtitle)}` : `/anime/${item.slug}`}
        className="block"
      >
        <HomeCardMedia item={item} sectionCode={sectionCode} />
        <div className="px-3.5 py-3">
          <h3 className="truncate font-serif text-base font-medium text-ivory">{item.title}</h3>
          <p className="truncate text-[11px] uppercase tracking-[0.18em] text-gold/90">
            {item.subtitle ?? "Catálogo"}
          </p>
        </div>
      </Link>
    </article>
  );
}

function getContinueDescription(section: HomeSectionData) {
  if (section.mode === "PERSONALIZED") {
    return "Baseado nos episódios que você começou.";
  }
  return section.items.length > 0
    ? "Sugestões editoriais para retomar uma sessão."
    : "Entre na sua conta para acompanhar seu progresso.";
}

function getEpisodeNumberFromSubtitle(subtitle: string | null) {
  const match = subtitle?.match(/Ep\.\s*(\d+)/i);
  return match?.[1] ?? "1";
}

function HomeCardMedia({
  item,
  sectionCode,
}: {
  item: HomeSectionItem;
  sectionCode: string;
}) {
  const [useFallback, setUseFallback] = useState(false);
  const [hidePreview, setHidePreview] = useState(false);
  const isContinue = sectionCode === "continue";
  const isEpisode = Boolean(item.episodeId) && !isContinue;
  const showPlayHover = sectionCode === "recent" || sectionCode === "continue";
  const enablePreviewHover = Boolean(item.previewUrl) && !showPlayHover;
  const primaryImage = isEpisode
    ? (!useFallback ? item.bannerUrl ?? item.coverUrl ?? "" : item.coverUrl ?? item.bannerUrl ?? "")
    : (!useFallback ? item.coverUrl ?? item.bannerUrl ?? "" : item.bannerUrl ?? item.coverUrl ?? "");
  const fallbackImage = isEpisode ? item.coverUrl ?? item.bannerUrl ?? "" : item.bannerUrl ?? item.coverUrl ?? "";
  const previewImage = enablePreviewHover && !hidePreview ? item.previewUrl : null;

  useEffect(() => {
    setUseFallback(false);
    setHidePreview(false);
  }, [item.id, item.bannerUrl, item.coverUrl, item.previewUrl]);

  return (
    <div className={cn("relative overflow-hidden bg-surface-elevated", isEpisode ? "aspect-video" : "aspect-[2/3]")}>
      {primaryImage ? (
        <img
          src={primaryImage}
          alt=""
          loading="lazy"
          onError={() => setUseFallback(true)}
          className={cn(
            "h-full w-full object-cover transition-all duration-500",
            previewImage ? "group-hover:scale-[1.04] group-hover:opacity-0" : "group-hover:scale-[1.04]",
          )}
        />
      ) : null}
      {previewImage ? (
        <img
          src={previewImage}
          alt=""
          loading="lazy"
          onError={() => setHidePreview(true)}
          className="pointer-events-none absolute inset-0 h-full w-full object-cover opacity-0 transition-opacity duration-300 group-hover:opacity-100"
        />
      ) : null}
      {!primaryImage && fallbackImage ? (
        <img
          src={fallbackImage}
          alt=""
          loading="lazy"
          className={cn("h-full w-full object-cover", showPlayHover ? "transition-transform duration-500 group-hover:scale-[1.04]" : "")}
        />
      ) : null}
      {showPlayHover ? (
        <>
          <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-onyx/85 via-onyx/15 to-transparent" />
          <div className="pointer-events-none absolute inset-0 bg-gold/0 transition-colors duration-300 group-hover:bg-gold/10" />
          <div className="pointer-events-none absolute inset-0 flex items-center justify-center opacity-0 transition-opacity duration-300 group-hover:opacity-100">
            <span className="inline-flex h-12 w-12 items-center justify-center rounded-full border border-gold/60 bg-onyx/60 text-gold backdrop-blur">
              <Play className="h-5 w-5 fill-gold" />
            </span>
          </div>
        </>
      ) : null}
    </div>
  );
}

function HeroImage({ src, fallbackSrc }: { src: string; fallbackSrc: string }) {
  const [currentSrc, setCurrentSrc] = useState(src);

  useEffect(() => {
    setCurrentSrc(src);
  }, [src]);

  return (
    <img
      src={currentSrc}
      alt=""
      onError={() => {
        if (currentSrc !== fallbackSrc) {
          setCurrentSrc(fallbackSrc);
        }
      }}
      className="h-full w-full object-cover object-center"
    />
  );
}

export default Home;
