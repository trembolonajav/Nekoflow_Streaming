import { useEffect, useMemo, useRef, useState, type KeyboardEvent as ReactKeyboardEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Search, X, CornerDownLeft, ImageIcon } from "lucide-react";

import { Input } from "@/components/ui/input";
import { searchAnimeCatalog, type AnimeSearchResultDto } from "@/lib/backend-api";
import { cn } from "@/lib/utils";

interface HeaderSearchProps {
  /** Tamanho visual: "md" (desktop, h-10) ou "lg" (mobile, h-11). */
  size?: "md" | "lg";
  /** Foca o input ao montar (mobile expandido). */
  autoFocus?: boolean;
  /** Chamado quando o usuário escolhe um resultado / submete (para fechar mobile). */
  onSelect?: () => void;
  className?: string;
}

export function HeaderSearch({
  size = "md",
  autoFocus = false,
  onSelect,
  className,
}: HeaderSearchProps) {
  const navigate = useNavigate();
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(0);
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const [results, setResults] = useState<AnimeSearchResultDto[]>([]);
  const hasQuery = query.trim().length >= 2;

  useEffect(() => {
    setActiveIndex(0);
  }, [query]);

  useEffect(() => {
    let active = true;
    if (!hasQuery) {
      setResults([]);
      return;
    }

    const timeout = setTimeout(() => {
      void searchAnimeCatalog(query)
        .then((items) => {
          if (active) setResults(items.slice(0, 6));
        })
        .catch(() => {
          if (active) setResults([]);
        });
    }, 120);

    return () => {
      active = false;
      clearTimeout(timeout);
    };
  }, [hasQuery, query]);

  // Fecha ao clicar fora
  useEffect(() => {
    if (!open) return;
    const onDown = (e: MouseEvent) => {
      if (!containerRef.current?.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", onDown);
    return () => document.removeEventListener("mousedown", onDown);
  }, [open]);

  const goTo = (slug: string) => {
    setOpen(false);
    setQuery("");
    onSelect?.();
    navigate(`/anime/${slug}`);
  };

  const onKeyDown = (e: ReactKeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Escape") {
      if (query) {
        setQuery("");
      } else {
        setOpen(false);
        inputRef.current?.blur();
      }
      return;
    }
    if (!open || results.length === 0) return;
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setActiveIndex((i) => (i + 1) % results.length);
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setActiveIndex((i) => (i - 1 + results.length) % results.length);
    } else if (e.key === "Enter") {
      e.preventDefault();
      const target = results[activeIndex] ?? results[0];
      if (target) goTo(target.slug);
    }
  };

  const inputHeight = size === "lg" ? "h-11" : "h-10";

  return (
    <div ref={containerRef} className={cn("relative", className)}>
      <Search
        className={cn(
          "pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-ivory-muted transition-colors",
          open && "text-gold",
        )}
      />
      <Input
        ref={inputRef}
        type="search"
        autoFocus={autoFocus}
        value={query}
        onChange={(e) => {
          setQuery(e.target.value);
          setOpen(true);
        }}
        onFocus={() => setOpen(true)}
        onKeyDown={onKeyDown}
        placeholder="Buscar anime, gênero, estúdio…"
        autoComplete="off"
        spellCheck={false}
        className={cn(
          inputHeight,
          "border-border-subtle bg-surface-elevated/60 pl-10 pr-9 text-sm text-ivory placeholder:text-ivory-muted/70 transition-all focus-visible:border-gold/40 focus-visible:bg-surface-elevated focus-visible:ring-2 focus-visible:ring-gold/30",
        )}
      />

      {query && (
        <button
          type="button"
          aria-label="Limpar busca"
          onClick={() => {
            setQuery("");
            inputRef.current?.focus();
          }}
          className="absolute right-2 top-1/2 flex size-6 -translate-y-1/2 items-center justify-center rounded-full text-ivory-muted transition-colors hover:bg-surface-elevated hover:text-gold"
        >
          <X className="size-3.5" />
        </button>
      )}

      {/* Painel de resultados */}
      {open && hasQuery && (
        <div className="absolute left-0 right-0 top-full z-50 mt-2 overflow-hidden rounded-lg border border-gold/15 bg-surface/95 shadow-2xl backdrop-blur-xl">
          {results.length === 0 ? (
            <div className="px-4 py-6 text-center">
              <p className="text-sm text-ivory">Nenhum resultado para “{query}”.</p>
              <p className="mt-1 text-xs text-ivory-muted">
                Tente outro termo ou sugira o anime para o catálogo.
              </p>
            </div>
          ) : (
            <>
              <div className="border-b border-border-subtle px-3 py-2 text-[10px] uppercase tracking-[0.18em] text-ivory-muted/70">
                Resultados ({results.length})
              </div>
              <ul className="max-h-[420px] overflow-y-auto py-1">
                {results.map((r, i) => {
                  const active = i === activeIndex;
                  return (
                    <li key={r.slug}>
                      <Link
                        to={`/anime/${r.slug}`}
                        onClick={() => {
                          setOpen(false);
                          setQuery("");
                          onSelect?.();
                        }}
                        onMouseEnter={() => setActiveIndex(i)}
                        className={cn(
                          "flex items-center gap-3 px-3 py-2.5 transition-colors",
                          active
                            ? "bg-surface-elevated text-gold"
                            : "text-ivory hover:bg-surface-elevated/70",
                        )}
                      >
                        {r.poster ? (
                          <img
                            src={r.poster}
                            alt=""
                            loading="lazy"
                            className="h-14 w-10 shrink-0 rounded-md object-cover"
                          />
                        ) : (
                          <div className="flex h-14 w-10 shrink-0 items-center justify-center rounded-md border border-border-subtle bg-surface-elevated text-ivory-muted">
                            <ImageIcon className="size-4" />
                          </div>
                        )}
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-sm font-medium">{r.title}</p>
                          {r.altTitle && (
                            <p className="truncate text-[11px] text-ivory-muted">
                              {r.altTitle}
                            </p>
                          )}
                          <p className="mt-0.5 truncate text-[11px] text-ivory-muted/80">
                            {r.meta}
                          </p>
                        </div>
                        {active && (
                          <CornerDownLeft className="size-3.5 shrink-0 text-gold/80" />
                        )}
                      </Link>
                    </li>
                  );
                })}
              </ul>
              <div className="border-t border-border-subtle px-3 py-2 text-[10px] uppercase tracking-[0.16em] text-ivory-muted/60">
                ↑↓ navegar · ↵ abrir · esc fechar
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}
