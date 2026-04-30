import { useEffect, useMemo, useState } from "react";
import { ArrowDown, ArrowUp, Search, X } from "lucide-react";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Checkbox } from "@/components/ui/checkbox";
import { cn } from "@/lib/utils";

interface AnimeOption {
  id: string;
  title: string;
  coverUrl: string | null;
  seasonLabel: string | null;
  visibility: string;
}

interface HeroEditorDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  hero: {
    animeIds: string[];
    tag: string | null;
    ctaLabel: string | null;
  };
  animes: AnimeOption[];
  onSave: (payload: { animeIds: string[]; tag: string; ctaLabel: string }) => Promise<void>;
}

export function HeroEditorDialog({ open, onOpenChange, hero, animes, onSave }: HeroEditorDialogProps) {
  const [animeIds, setAnimeIds] = useState<string[]>(hero.animeIds);
  const [tag, setTag] = useState(hero.tag ?? "Destaque editorial");
  const [ctaLabel, setCtaLabel] = useState(hero.ctaLabel ?? "Assistir agora");
  const [query, setQuery] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setAnimeIds(hero.animeIds);
    setTag(hero.tag ?? "Destaque editorial");
    setCtaLabel(hero.ctaLabel ?? "Assistir agora");
    setQuery("");
  }, [open, hero]);

  const publishedAnimes = useMemo(
    () => animes.filter((anime) => anime.visibility === "PUBLISHED"),
    [animes],
  );

  const filtered = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    if (!normalized) return publishedAnimes;
    return publishedAnimes.filter((anime) => anime.title.toLowerCase().includes(normalized));
  }, [publishedAnimes, query]);

  const selectedAnimes = useMemo(
    () =>
      animeIds
        .map((id) => publishedAnimes.find((anime) => anime.id === id))
        .filter((anime): anime is AnimeOption => Boolean(anime)),
    [animeIds, publishedAnimes],
  );

  const toggle = (id: string) => {
    setAnimeIds((prev) => (prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]));
  };

  const move = (index: number, dir: -1 | 1) => {
    setAnimeIds((prev) => {
      const next = [...prev];
      const target = index + dir;
      if (target < 0 || target >= next.length) return prev;
      [next[index], next[target]] = [next[target], next[index]];
      return next;
    });
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      await onSave({
        animeIds,
        tag,
        ctaLabel,
      });
      onOpenChange(false);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] max-w-3xl overflow-hidden border-gold/15 bg-surface/95 text-ivory backdrop-blur-xl">
        <DialogHeader>
          <DialogTitle className="font-serif text-2xl text-ivory">Editar Hero</DialogTitle>
          <DialogDescription className="text-ivory-muted">
            Selecione os animes que entram no carrossel e ajuste tag e CTA compartilhados.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-5 pt-2">
          <div className="grid gap-4 md:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="hero-tag" className="text-xs text-ivory-muted">Tag editorial</Label>
              <Input
                id="hero-tag"
                value={tag}
                onChange={(e) => setTag(e.target.value)}
                className="h-10 border-border-subtle bg-surface-elevated/50 text-sm text-ivory"
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="hero-cta" className="text-xs text-ivory-muted">Texto do CTA</Label>
              <Input
                id="hero-cta"
                value={ctaLabel}
                onChange={(e) => setCtaLabel(e.target.value)}
                className="h-10 border-border-subtle bg-surface-elevated/50 text-sm text-ivory"
              />
            </div>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <div className="space-y-2">
              <Label className="text-xs text-ivory-muted">Animes publicados</Label>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-ivory-muted" />
                <Input
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  className="h-9 border-border-subtle bg-surface-elevated/50 pl-9 text-sm text-ivory"
                />
              </div>
              <ScrollArea className="h-64 rounded-md border border-border-subtle bg-surface-elevated/30">
                <ul className="divide-y divide-border-subtle">
                  {filtered.map((anime) => {
                    const checked = animeIds.includes(anime.id);
                    return (
                      <li key={anime.id}>
                        <label
                          className={cn(
                            "flex cursor-pointer items-center gap-3 px-3 py-2 transition-colors hover:bg-surface-elevated/60",
                            checked && "bg-gold/5",
                          )}
                        >
                          <Checkbox checked={checked} onCheckedChange={() => toggle(anime.id)} />
                          {anime.coverUrl ? (
                            <img src={anime.coverUrl} alt="" className="h-10 w-7 rounded-sm object-cover" />
                          ) : (
                            <div className="h-10 w-7 rounded-sm bg-surface" />
                          )}
                          <div className="min-w-0 flex-1">
                            <p className="truncate text-xs text-ivory">{anime.title}</p>
                            <p className="truncate text-[10px] text-ivory-muted">{anime.seasonLabel ?? "Sem temporada"}</p>
                          </div>
                        </label>
                      </li>
                    );
                  })}
                </ul>
              </ScrollArea>
            </div>

            <div className="space-y-2">
              <Label className="text-xs text-ivory-muted">Ordem no carrossel</Label>
              <ScrollArea className="h-64 rounded-md border border-border-subtle bg-surface-elevated/30">
                <ol className="divide-y divide-border-subtle">
                  {selectedAnimes.map((anime, index) => (
                    <li key={anime.id} className="flex items-center gap-2 px-2 py-2">
                      <span className="w-5 text-center font-mono text-[10px] text-gold">
                        {String(index + 1).padStart(2, "0")}
                      </span>
                      {anime.coverUrl ? (
                        <img src={anime.coverUrl} alt="" className="h-10 w-7 rounded-sm object-cover" />
                      ) : (
                        <div className="h-10 w-7 rounded-sm bg-surface" />
                      )}
                      <p className="min-w-0 flex-1 truncate text-xs text-ivory">{anime.title}</p>
                      <div className="flex items-center gap-0.5">
                        <button
                          type="button"
                          onClick={() => move(index, -1)}
                          disabled={index === 0}
                          className="inline-flex h-7 w-7 items-center justify-center rounded text-ivory-muted hover:bg-surface-elevated hover:text-ivory disabled:opacity-30"
                        >
                          <ArrowUp className="h-3.5 w-3.5" />
                        </button>
                        <button
                          type="button"
                          onClick={() => move(index, 1)}
                          disabled={index === selectedAnimes.length - 1}
                          className="inline-flex h-7 w-7 items-center justify-center rounded text-ivory-muted hover:bg-surface-elevated hover:text-ivory disabled:opacity-30"
                        >
                          <ArrowDown className="h-3.5 w-3.5" />
                        </button>
                        <button
                          type="button"
                          onClick={() => toggle(anime.id)}
                          className="inline-flex h-7 w-7 items-center justify-center rounded text-ivory-muted hover:bg-destructive/10 hover:text-destructive"
                        >
                          <X className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </li>
                  ))}
                </ol>
              </ScrollArea>
            </div>
          </div>
        </div>

        <DialogFooter className="gap-2 sm:gap-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => onOpenChange(false)}
            className="h-10 border-border-subtle bg-transparent text-sm text-ivory hover:bg-surface-elevated"
          >
            Cancelar
          </Button>
          <Button
            type="button"
            onClick={handleSave}
            disabled={saving}
            className="h-10 bg-gold px-5 text-sm font-medium text-onyx hover:bg-gold/90"
          >
            Salvar Hero
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
