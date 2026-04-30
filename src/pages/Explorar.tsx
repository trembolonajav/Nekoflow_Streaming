import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { ChevronDown, Dices, SearchX, SlidersHorizontal, Star, X } from "lucide-react";
import { z } from "zod";

import { Header } from "@/components/layout/Header";
import { OrnamentDivider } from "@/components/layout/OrnamentDivider";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { fetchExploreAnimes, type AnimeSummaryDto } from "@/lib/backend-api";
import { cn } from "@/lib/utils";

const PAGE_SIZE = 30;
const STORAGE_KEY = "nekoflow:explorar:filtros";

const searchSchema = z.object({
  genero: z.string().optional().default("todos"),
  ano: z.string().optional().default("todos"),
  status: z.string().optional().default("todos"),
  formato: z.string().optional().default("todos"),
  idioma: z.string().optional().default("todos"),
  ordem: z.enum(["popular", "rating", "recentes", "az"]).optional().default("popular"),
  pagina: z.coerce.number().int().min(1).optional().default(1),
});

type ExploreParams = z.infer<typeof searchSchema>;

function Explorar() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [imageFailures, setImageFailures] = useState<Record<string, boolean>>({});
  const catalogQuery = useQuery({
    queryKey: ["explore-animes"],
    queryFn: fetchExploreAnimes,
  });

  useEffect(() => {
    if (searchParams.toString()) return;
    const saved = window.localStorage.getItem(STORAGE_KEY);
    if (saved) {
      setSearchParams(saved, { replace: true });
    }
  }, [searchParams, setSearchParams]);

  const params = parseSearchParams(searchParams);
  const animes = catalogQuery.data ?? [];
  const options = useMemo(() => buildOptions(animes), [animes]);
  const filtered = useMemo(() => filterAndSort(animes, params), [animes, params]);
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const currentPage = Math.min(params.pagina, totalPages);
  const pageItems = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  useEffect(() => {
    if (!searchParams.toString()) return;
    window.localStorage.setItem(STORAGE_KEY, searchParams.toString());
  }, [searchParams]);

  useEffect(() => {
    if (params.pagina > totalPages) {
      updateParams(setSearchParams, params, { pagina: String(totalPages) });
    }
  }, [params, setSearchParams, totalPages]);

  const setFilter = (key: keyof ExploreParams, value: string | number) => {
    updateParams(setSearchParams, params, {
      [key]: String(value),
      ...(key !== "pagina" ? { pagina: "1" } : {}),
    });
  };

  const clearFilter = (key: keyof ExploreParams) => {
    updateParams(setSearchParams, params, {
      [key]: key === "ordem" ? "popular" : key === "pagina" ? "1" : "todos",
      pagina: "1",
    });
  };

  const clearAll = () => {
    setSearchParams({}, { replace: false });
    window.localStorage.removeItem(STORAGE_KEY);
  };

  const surpriseMe = () => {
    if (filtered.length === 0) return;
    const chosen = filtered[Math.floor(Math.random() * filtered.length)];
    navigate(`/anime/${chosen.slug}`);
  };

  return (
    <div className="min-h-screen bg-background text-ivory">
      <Header />
      <main>
        <section className="mx-auto w-full max-w-[1400px] px-4 pb-8 pt-12 text-center md:px-10 md:pt-16">
          <span className="inline-flex items-center gap-2 font-mono text-[11px] uppercase tracking-[0.34em] text-gold">
            <SlidersHorizontal className="h-3.5 w-3.5" />
            Catálogo
          </span>
          <h1 className="mt-5 font-serif text-[44px] font-medium leading-tight text-ivory md:text-[72px]">
            Explorar o catálogo
          </h1>
          <p className="mt-4 text-base text-ivory-muted md:text-lg">
            {animes.length} títulos curados · atualizado hoje
          </p>
          <OrnamentDivider width="sm" className="mx-auto mt-8" />
        </section>

        <section className="sticky top-24 z-30 border-y border-border-subtle bg-background/92 backdrop-blur-xl md:top-28">
          <div className="mx-auto flex w-full max-w-[1400px] flex-col gap-4 px-4 py-4 md:px-10">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex flex-wrap items-center gap-3">
                <FilterMenu label="Gênero" value={params.genero} options={options.generos} onSelect={(value) => setFilter("genero", value)} />
                <FilterMenu label="Ano" value={params.ano} options={options.anos} onSelect={(value) => setFilter("ano", value)} />
                <FilterMenu label="Status" value={params.status} options={options.status} onSelect={(value) => setFilter("status", value)} />
                <FilterMenu label="Formato" value={params.formato} options={options.formatos} onSelect={(value) => setFilter("formato", value)} />
                <FilterMenu label="Idioma" value={params.idioma} options={["LEG"]} onSelect={(value) => setFilter("idioma", value)} />
              </div>

              <div className="flex flex-wrap items-center gap-3">
                <Button
                  type="button"
                  variant="outline"
                  onClick={surpriseMe}
                  disabled={filtered.length === 0}
                  className="h-12 rounded-full border-gold/40 bg-gold/5 px-5 font-mono text-[11px] uppercase tracking-[0.22em] text-gold hover:border-gold/70 hover:bg-gold/10 hover:text-gold"
                >
                  <Dices className="mr-2 h-4 w-4" />
                  Surpreenda-me
                </Button>
                <FilterMenu
                  label="Ordenar"
                  value={params.ordem}
                  options={[
                    { value: "popular", label: "Mais populares" },
                    { value: "rating", label: "Melhor nota" },
                    { value: "recentes", label: "Recentes" },
                    { value: "az", label: "A-Z" },
                  ]}
                  onSelect={(value) => setFilter("ordem", value)}
                />
              </div>
            </div>

            <div className="flex flex-wrap items-center justify-between gap-3">
              <ActiveChips params={params} onClear={clearFilter} onClearAll={clearAll} />
              <span className="font-mono text-xs uppercase tracking-[0.24em] text-ivory-muted">
                {filtered.length} títulos
              </span>
            </div>
          </div>
        </section>

        <section className="mx-auto w-full max-w-[1400px] px-4 py-10 md:px-10 md:py-14">
          {catalogQuery.isLoading ? (
            <ExploreSkeleton />
          ) : filtered.length === 0 ? (
            <EmptyState onClear={clearAll} />
          ) : (
            <>
              <div className="grid grid-cols-2 gap-x-4 gap-y-9 md:grid-cols-3 lg:grid-cols-5">
                {pageItems.map((anime) => (
                  <ExploreCard
                    key={anime.id}
                    anime={anime}
                    imageFailed={Boolean(imageFailures[anime.id])}
                    onImageError={() => setImageFailures((current) => ({ ...current, [anime.id]: true }))}
                  />
                ))}
              </div>
              <Pagination
                page={currentPage}
                totalPages={totalPages}
                onPage={(page) => setFilter("pagina", page)}
              />
            </>
          )}
        </section>
      </main>
    </div>
  );
}

function parseSearchParams(searchParams: URLSearchParams): ExploreParams {
  const raw = Object.fromEntries(searchParams.entries());
  return searchSchema.catch({
    genero: "todos",
    ano: "todos",
    status: "todos",
    formato: "todos",
    idioma: "todos",
    ordem: "popular",
    pagina: 1,
  }).parse(raw);
}

function updateParams(
  setSearchParams: ReturnType<typeof useSearchParams>[1],
  current: ExploreParams,
  next: Record<string, string>,
) {
  const merged = { ...current, ...next };
  const clean = new URLSearchParams();

  Object.entries(merged).forEach(([key, value]) => {
    const stringValue = String(value);
    if (key === "pagina" && stringValue === "1") return;
    if (key !== "ordem" && stringValue === "todos") return;
    if (key === "ordem" && stringValue === "popular") return;
    clean.set(key, stringValue);
  });

  setSearchParams(clean);
}

function buildOptions(animes: AnimeSummaryDto[]) {
  const generos = unique(animes.flatMap((anime) => normalizedGenres(anime)));
  const anos = unique(animes.map((anime) => anime.year?.toString()).filter(Boolean) as string[]).sort((a, b) => Number(b) - Number(a));
  const status = unique(animes.map((anime) => anime.status));
  const formatos = unique(animes.map((anime) => anime.type));
  return { generos, anos, status, formatos };
}

function filterAndSort(animes: AnimeSummaryDto[], params: ExploreParams) {
  return animes
    .filter((anime) => params.genero === "todos" || normalizedGenres(anime).includes(params.genero))
    .filter((anime) => params.ano === "todos" || anime.year?.toString() === params.ano)
    .filter((anime) => params.status === "todos" || anime.status === params.status)
    .filter((anime) => params.formato === "todos" || anime.type === params.formato)
    .filter(() => params.idioma === "todos" || params.idioma === "LEG")
    .sort((a, b) => {
      if (params.ordem === "az") return a.titleDisplay.localeCompare(b.titleDisplay, "pt-BR");
      if (params.ordem === "recentes") return (b.year ?? 0) - (a.year ?? 0) || a.titleDisplay.localeCompare(b.titleDisplay, "pt-BR");
      return (b.averageScore ?? 0) - (a.averageScore ?? 0) || (b.year ?? 0) - (a.year ?? 0);
    });
}

function normalizedGenres(anime: AnimeSummaryDto) {
  const values = anime.genres?.length ? anime.genres : [anime.type];
  return values.map(formatLabel);
}

function unique(values: string[]) {
  return Array.from(new Set(values.filter(Boolean))).sort((a, b) => a.localeCompare(b, "pt-BR"));
}

function FilterMenu({
  label,
  value,
  options,
  onSelect,
}: {
  label: string;
  value: string;
  options: Array<string | { value: string; label: string }>;
  onSelect: (value: string) => void;
}) {
  const currentLabel = value === "todos"
    ? label
    : typeof options.find((option) => optionValue(option) === value) === "object"
      ? (options.find((option) => optionValue(option) === value) as { label: string }).label
      : formatLabel(value);

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          className={cn(
            "inline-flex h-12 items-center gap-3 rounded-full border px-5 font-mono text-[11px] uppercase tracking-[0.22em] transition-colors",
            value === "todos" || value === "popular"
              ? "border-border-subtle bg-surface/40 text-ivory-muted hover:border-gold/35 hover:text-gold"
              : "border-gold/35 bg-gold/10 text-gold",
          )}
        >
          {currentLabel}
          <ChevronDown className="h-4 w-4" />
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" className="max-h-[320px] min-w-56 overflow-y-auto border-gold/15 bg-surface/95 p-1 backdrop-blur-xl">
        <DropdownMenuItem onClick={() => onSelect(label === "Ordenar" ? "popular" : "todos")} className="text-ivory focus:bg-surface-elevated focus:text-gold">
          {label === "Ordenar" ? "Mais populares" : `Todos`}
        </DropdownMenuItem>
        {options.map((option) => (
          <DropdownMenuItem
            key={optionValue(option)}
            onClick={() => onSelect(optionValue(option))}
            className="text-ivory focus:bg-surface-elevated focus:text-gold"
          >
            {optionLabel(option)}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

function ActiveChips({
  params,
  onClear,
  onClearAll,
}: {
  params: ExploreParams;
  onClear: (key: keyof ExploreParams) => void;
  onClearAll: () => void;
}) {
  const chips = [
    ["genero", params.genero],
    ["ano", params.ano],
    ["status", params.status],
    ["formato", params.formato],
    ["idioma", params.idioma],
  ] as Array<[keyof ExploreParams, string]>;
  const active = chips.filter(([, value]) => value !== "todos");

  if (active.length === 0) {
    return <span className="text-sm text-ivory-muted">Use filtros para refinar o catálogo.</span>;
  }

  return (
    <div className="flex flex-wrap items-center gap-2">
      {active.map(([key, value]) => (
        <button
          key={key}
          type="button"
          onClick={() => onClear(key)}
          className="inline-flex items-center gap-2 rounded-full border border-gold/25 bg-gold/10 px-3 py-1.5 text-xs text-gold"
        >
          {formatLabel(value)}
          <X className="h-3 w-3" />
        </button>
      ))}
      <button type="button" onClick={onClearAll} className="text-xs uppercase tracking-[0.18em] text-ivory-muted hover:text-gold">
        Limpar tudo
      </button>
    </div>
  );
}

function ExploreCard({
  anime,
  imageFailed,
  onImageError,
}: {
  anime: AnimeSummaryDto;
  imageFailed: boolean;
  onImageError: () => void;
}) {
  const rating = anime.averageScore ? (anime.averageScore / 20).toFixed(1) : null;
  const genre = normalizedGenres(anime)[0] ?? formatLabel(anime.type);

  return (
    <Link to={`/anime/${anime.slug}`} className="group block">
      <article className="overflow-hidden">
        <div className="relative aspect-[2/3] overflow-hidden rounded-lg border border-border-subtle bg-surface transition-all duration-300 group-hover:-translate-y-1 group-hover:border-gold/50 group-hover:shadow-2xl group-hover:shadow-gold/10">
          {!imageFailed && (anime.coverUrl || anime.bannerUrl) ? (
            <img
              src={anime.coverUrl ?? anime.bannerUrl ?? ""}
              alt=""
              loading="lazy"
              onError={onImageError}
              className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-[1.04]"
            />
          ) : (
            <div className="flex h-full w-full items-center justify-center bg-surface-elevated px-6 text-center font-serif text-lg text-ivory-muted">
              {anime.titleDisplay}
            </div>
          )}
          <div className="absolute inset-0 bg-gradient-to-t from-onyx/90 via-onyx/10 to-transparent" />
          <div className="absolute left-3 top-3 flex flex-col items-start gap-2">
            <span className="rounded border border-gold/40 bg-onyx/70 px-2 py-1 font-mono text-[10px] uppercase tracking-[0.14em] text-gold backdrop-blur">
              LEG
            </span>
            {anime.status === "RELEASING" ? (
              <span className="rounded bg-ivory/15 px-2 py-1 font-mono text-[10px] uppercase tracking-[0.14em] text-ivory backdrop-blur">
                No ar
              </span>
            ) : null}
          </div>
          {rating ? (
            <span className="absolute bottom-3 right-3 inline-flex items-center gap-1 rounded-full border border-gold/35 bg-onyx/75 px-2.5 py-1 font-mono text-xs text-gold backdrop-blur">
              <Star className="h-3.5 w-3.5 fill-gold" />
              {rating}
            </span>
          ) : null}
          <div className="absolute inset-x-0 bottom-0 translate-y-full bg-onyx/88 p-4 opacity-0 backdrop-blur transition-all duration-300 group-hover:translate-y-0 group-hover:opacity-100">
            <p className="line-clamp-3 text-sm leading-relaxed text-ivory/90">
              {anime.synopsis ?? "Sinopse ainda não disponível."}
            </p>
          </div>
        </div>
        <h2 className="mt-4 truncate font-serif text-xl text-ivory transition-colors group-hover:text-gold">
          {anime.titleDisplay}
        </h2>
        <p className="mt-1 truncate font-mono text-xs uppercase tracking-[0.22em] text-gold/80">
          {[anime.year, genre].filter(Boolean).join(" · ")}
        </p>
      </article>
    </Link>
  );
}

function Pagination({ page, totalPages, onPage }: { page: number; totalPages: number; onPage: (page: number) => void }) {
  if (totalPages <= 1) return null;
  return (
    <div className="mt-12 flex items-center justify-center gap-3">
      <Button
        type="button"
        variant="outline"
        disabled={page === 1}
        onClick={() => onPage(page - 1)}
        className="rounded-full border-gold/25 bg-transparent px-5 text-ivory hover:border-gold/60 hover:bg-gold/10 hover:text-gold disabled:opacity-40"
      >
        Anterior
      </Button>
      <span className="font-mono text-xs uppercase tracking-[0.22em] text-ivory-muted">
        {page} / {totalPages}
      </span>
      <Button
        type="button"
        variant="outline"
        disabled={page === totalPages}
        onClick={() => onPage(page + 1)}
        className="rounded-full border-gold/25 bg-transparent px-5 text-ivory hover:border-gold/60 hover:bg-gold/10 hover:text-gold disabled:opacity-40"
      >
        Próxima
      </Button>
    </div>
  );
}

function EmptyState({ onClear }: { onClear: () => void }) {
  return (
    <div className="mx-auto flex min-h-[360px] max-w-xl flex-col items-center justify-center rounded-lg border border-dashed border-gold/20 bg-surface/30 px-8 text-center">
      <SearchX className="h-10 w-10 text-gold" />
      <h2 className="mt-5 font-serif text-3xl text-ivory">Nenhum título encontrado</h2>
      <p className="mt-3 text-sm leading-relaxed text-ivory-muted">
        Nenhum anime combina com esses filtros. Limpe o recorte para voltar aos destaques do catálogo.
      </p>
      <Button onClick={onClear} className="mt-6 rounded-full bg-gold px-6 text-onyx hover:bg-gold/90">
        Limpar filtros
      </Button>
    </div>
  );
}

function ExploreSkeleton() {
  return (
    <div className="grid grid-cols-2 gap-x-4 gap-y-9 md:grid-cols-3 lg:grid-cols-5">
      {Array.from({ length: 10 }).map((_, index) => (
        <div key={index} className="space-y-4">
          <div className="aspect-[2/3] animate-pulse rounded-lg bg-surface-elevated" />
          <div className="h-5 w-4/5 animate-pulse rounded bg-surface-elevated" />
          <div className="h-3 w-1/2 animate-pulse rounded bg-surface-elevated" />
        </div>
      ))}
    </div>
  );
}

function optionValue(option: string | { value: string; label: string }) {
  return typeof option === "string" ? option : option.value;
}

function optionLabel(option: string | { value: string; label: string }) {
  return typeof option === "string" ? formatLabel(option) : option.label;
}

function formatLabel(value: string) {
  return value
    .replace(/_/g, " ")
    .toLowerCase()
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export default Explorar;
