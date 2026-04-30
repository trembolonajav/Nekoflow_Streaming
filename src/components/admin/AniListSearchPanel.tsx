import { useEffect, useRef, useState } from "react";
import { Search, Sparkles, Loader2, X, Check } from "lucide-react";

import { Input } from "@/components/ui/input";
import {
  searchAdminAniList,
  type AdminAniListSearchDto,
} from "@/lib/backend-api";

interface AniListSearchPanelProps {
  onPick: (result: AdminAniListSearchDto) => void;
}

/**
 * Busca auxiliar AniList exibida no topo do modal "Novo anime".
 * Debounce 350ms, até 8 resultados, clique preenche o form.
 */
export function AniListSearchPanel({ onPick }: AniListSearchPanelProps) {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<AdminAniListSearchDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pickedId, setPickedId] = useState<number | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    const trimmed = query.trim();
    if (trimmed.length < 2) {
      setResults([]);
      setError(null);
      setLoading(false);
      abortRef.current?.abort();
      return;
    }

    const t = setTimeout(() => {
      abortRef.current?.abort();
      const ctrl = new AbortController();
      abortRef.current = ctrl;
      setLoading(true);
      setError(null);
      searchAdminAniList(trimmed, ctrl.signal)
        .then((r) => {
          setResults(r);
          setLoading(false);
        })
        .catch((e) => {
          if (e?.name === "AbortError") return;
          setError("Falha ao consultar AniList. Tente novamente.");
          setLoading(false);
        });
    }, 350);

    return () => {
      clearTimeout(t);
      abortRef.current?.abort();
    };
  }, [query]);

  const handlePick = (r: AniListSearchResult) => {
    setPickedId(r.id);
    onPick(r);
  };

  return (
    <section className="space-y-3 rounded-lg border border-gold/15 bg-gold/[0.04] p-4">
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <Sparkles className="size-4 text-gold" />
          <h3 className="text-[11px] font-medium uppercase tracking-[0.18em] text-gold/90">
            Auxiliar AniList
          </h3>
        </div>
        <span className="text-[11px] text-ivory-muted">
          Busca metadados e preenche o formulário
        </span>
      </div>

      <div className="relative">
        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-ivory-muted" />
        <Input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Buscar anime no AniList… (ex.: Shingeki no Kyojin)"
          className="h-10 border-border-subtle bg-surface-elevated/60 pl-10 pr-10 text-sm text-ivory placeholder:text-ivory-muted/70"
        />
        {query && (
          <button
            type="button"
            onClick={() => setQuery("")}
            className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-1 text-ivory-muted hover:bg-surface-elevated hover:text-ivory"
            aria-label="Limpar busca"
          >
            <X className="size-3.5" />
          </button>
        )}
      </div>

      {loading && (
        <div className="flex items-center gap-2 text-xs text-ivory-muted">
          <Loader2 className="size-3.5 animate-spin" />
          Buscando…
        </div>
      )}

      {error && <p className="text-xs text-rose-300">{error}</p>}

      {!loading && !error && results.length > 0 && (
        <ul className="max-h-72 space-y-1.5 overflow-y-auto pr-1">
          {results.map((r) => {
            const picked = pickedId === r.id;
            return (
              <li key={r.id}>
                <button
                  type="button"
                  onClick={() => handlePick(r)}
                  className={
                    "flex w-full items-start gap-3 rounded-md border p-2 text-left transition-colors " +
                    (picked
                      ? "border-gold/50 bg-gold/10"
                      : "border-border-subtle/60 bg-surface-elevated/40 hover:border-gold/30 hover:bg-surface-elevated/70")
                  }
                >
                  {r.coverImage ? (
                    <img
                      src={r.coverImage}
                      alt=""
                      className="h-14 w-10 shrink-0 rounded object-cover"
                      loading="lazy"
                    />
                  ) : (
                    <div className="h-14 w-10 shrink-0 rounded bg-onyx/60" />
                  )}
                  <div className="min-w-0 flex-1">
                    <div className="flex items-start justify-between gap-2">
                      <p className="truncate text-sm font-medium text-ivory">
                        {r.titleRomaji}
                      </p>
                      {picked && (
                        <Check className="size-4 shrink-0 text-gold" />
                      )}
                    </div>
                    {r.titleEnglish && r.titleEnglish !== r.titleRomaji && (
                      <p className="truncate text-[11px] text-ivory-muted">
                        {r.titleEnglish}
                      </p>
                    )}
                    <p className="mt-0.5 truncate text-[11px] text-ivory-muted/80">
                      {[
                        r.format,
                        r.seasonYear,
                        r.episodes ? `${r.episodes} ep` : null,
                        r.averageScore ? `★ ${(r.averageScore / 10).toFixed(1)}` : null,
                      ]
                        .filter(Boolean)
                        .join(" · ")}
                    </p>
                  </div>
                </button>
              </li>
            );
          })}
        </ul>
      )}

      {!loading && !error && query.trim().length >= 2 && results.length === 0 && (
        <p className="text-xs text-ivory-muted">Nenhum resultado.</p>
      )}

      <p className="text-[11px] text-ivory-muted/70">
        Dica: o título é importado em romaji (ex.: <em>Shingeki no Kyojin</em>). A
        sinopse vem em inglês — traduza para PT-BR se quiser publicar.
      </p>
    </section>
  );
}

// re-export para conveniência
export type AniListSearchResult = AdminAniListSearchDto;
