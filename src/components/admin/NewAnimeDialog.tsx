import { useState, useEffect, type FormEvent } from "react";
import { ImagePlus, Loader2 } from "lucide-react";
import { toast } from "sonner";

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
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import {
  AniListSearchPanel,
  type AniListSearchResult,
} from "@/components/admin/AniListSearchPanel";
import {
  mapAniListFormatToType,
  mapAniListStatusToInternal,
  formatAniListSeason,
  mapAniListGenresToPreset,
  translateToPortuguese,
} from "@/lib/anilist";
import type { AdminAnimePayload } from "@/lib/backend-api";

export interface AnimeFormValues extends AdminAnimePayload {
  id?: string;
}

interface NewAnimeDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  editing?: AnimeFormValues | null;
  onSubmit: (payload: AnimeFormValues) => Promise<void>;
}

const GENRES = [
  "Ação",
  "Aventura",
  "Romance",
  "Drama",
  "Fantasia",
  "Sci-Fi",
  "Slice of Life",
  "Mistério",
  "Comédia",
  "Sobrenatural",
];

function slugify(value: string) {
  return value
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9\s-]/g, "")
    .trim()
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-");
}

export function NewAnimeDialog({ open, onOpenChange, editing, onSubmit }: NewAnimeDialogProps) {
  const isEdit = Boolean(editing);
  const [title, setTitle] = useState("");
  const [slug, setSlug] = useState("");
  const [type, setType] = useState("SERIES");
  const [season, setSeason] = useState("");
  const [status, setStatus] = useState("RELEASING");
  const [synopsis, setSynopsis] = useState("");
  const [studio, setStudio] = useState("");
  const [year, setYear] = useState("");
  const [coverUrl, setCoverUrl] = useState("");
  const [bannerUrl, setBannerUrl] = useState("");
  const [genres, setGenres] = useState<string[]>([]);
  const [publish, setPublish] = useState(false);
  const [anilistId, setAnilistId] = useState<number | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const reset = () => {
    setTitle("");
    setSlug("");
    setType("SERIES");
    setSeason("");
    setStatus("RELEASING");
    setSynopsis("");
    setStudio("");
    setYear("");
    setCoverUrl("");
    setBannerUrl("");
    setGenres([]);
    setPublish(false);
    setAnilistId(null);
  };

  useEffect(() => {
    if (!open) return;
    if (editing) {
      setTitle(editing.titleDisplay ?? "");
      setSlug(editing.slug ?? "");
      setType(editing.type ?? "SERIES");
      setSeason(editing.seasonLabel ?? "");
      setStatus(editing.status ?? "RELEASING");
      setSynopsis(editing.synopsis ?? "");
      setStudio(editing.studio ?? "");
      setYear(editing.year ? String(editing.year) : "");
      setCoverUrl(editing.coverUrl ?? "");
      setBannerUrl(editing.bannerUrl ?? "");
      setGenres(editing.genres ?? []);
      setPublish(editing.visibility === "PUBLISHED");
      setAnilistId(editing.anilistId ?? null);
    } else {
      reset();
    }
  }, [open, editing]);

  const toggleGenre = (genre: string) => {
    setGenres((prev) => (prev.includes(genre) ? prev.filter((item) => item !== genre) : [...prev, genre]));
  };

  const handleAniListPick = async (result: AniListSearchResult) => {
    const nextTitle = result.titleRomaji;
    setTitle(nextTitle);
    setAnilistId(result.id);
    if (!slug || slug === slugify(title)) setSlug(slugify(nextTitle));

    setType(mapAniListFormatToType(result.format).toUpperCase().replace("É", "E").replace(" ", "_"));
    setStatus(mapAniListStatusToInternal(result.status).toUpperCase().replace(" ", "_"));

    const seasonLabel = formatAniListSeason(result.season, result.seasonYear);
    if (seasonLabel) setSeason(seasonLabel);
    if (result.seasonYear) setYear(String(result.seasonYear));
    if (result.studios.length > 0) setStudio(result.studios[0]);
    if (result.coverImage) setCoverUrl(result.coverImage);
    if (result.bannerImage) setBannerUrl(result.bannerImage);

    if (result.genres.length > 0) {
      const mapped = mapAniListGenresToPreset(result.genres, GENRES);
      if (mapped.length > 0) {
        setGenres((prev) => Array.from(new Set([...prev, ...mapped])));
      }
    }

    if (result.description) {
      setSynopsis(result.description);
      const original = result.description;
      const toastId = toast.loading("Traduzindo sinopse para português…");
      try {
        const translated = await translateToPortuguese(original);
        setSynopsis((current) => (current === original ? translated : current));
        toast.success("Sinopse traduzida.", { id: toastId });
      } catch {
        toast.error("Não foi possível traduzir a sinopse. O texto original foi mantido.", {
          id: toastId,
        });
      }
    }

    toast.success(`“${nextTitle}” importado do AniList.`);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!title.trim()) {
      toast.error("Informe o título do anime.");
      return;
    }
    if (!synopsis.trim()) {
      toast.error("A sinopse é obrigatória.");
      return;
    }

    setSubmitting(true);
    try {
      await onSubmit({
        id: editing?.id,
        anilistId,
        slug: slug || slugify(title),
        titleDisplay: title.trim(),
        titleRomaji: title.trim(),
        titleNative: null,
        titleEnglish: null,
        synopsis: synopsis.trim(),
        type,
        status,
        visibility: publish ? "PUBLISHED" : editing?.visibility === "ARCHIVED" ? "ARCHIVED" : "DRAFT",
        seasonLabel: season || null,
        year: year ? Number(year) : null,
        coverUrl: coverUrl || null,
        bannerUrl: bannerUrl || coverUrl || null,
        studio: studio || null,
        genres,
      });
      reset();
      onOpenChange(false);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] max-w-3xl overflow-y-auto border-gold/15 bg-surface/95 text-ivory backdrop-blur-xl">
        <DialogHeader>
          <DialogTitle className="font-serif text-2xl text-ivory">
            {isEdit ? "Editar anime" : "Novo anime"}
          </DialogTitle>
          <DialogDescription className="text-ivory-muted">
            {isEdit
              ? "Atualize as informações e salve para refletir no catálogo real."
              : "Cadastre o anime e publique quando estiver pronto."}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-6 pt-2">
          <AniListSearchPanel onPick={handleAniListPick} />

          <section className="space-y-4">
            <h3 className="text-[11px] font-medium uppercase tracking-[0.18em] text-gold/80">
              Identidade
            </h3>
            <div className="grid gap-4 md:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="anime-title" className="text-xs text-ivory-muted">Título</Label>
                <Input
                  id="anime-title"
                  value={title}
                  onChange={(e) => {
                    const value = e.target.value;
                    setTitle(value);
                    if (!slug || slug === slugify(title)) setSlug(slugify(value));
                  }}
                  className="h-10 border-border-subtle bg-surface-elevated/50 text-sm text-ivory"
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="anime-slug" className="text-xs text-ivory-muted">Slug</Label>
                <Input
                  id="anime-slug"
                  value={slug}
                  onChange={(e) => setSlug(slugify(e.target.value))}
                  className="h-10 border-border-subtle bg-surface-elevated/50 text-sm text-ivory"
                />
              </div>
            </div>

            <div className="grid gap-4 md:grid-cols-3">
              <SelectField label="Tipo" value={type} onValueChange={setType} options={[
                ["SERIES", "Série"],
                ["MOVIE", "Filme"],
                ["OVA", "OVA"],
                ["SPECIAL", "Especial"],
              ]} />
              <SelectField label="Status" value={status} onValueChange={setStatus} options={[
                ["RELEASING", "Em lançamento"],
                ["FINISHED", "Finalizado"],
                ["HIATUS", "Em hiato"],
                ["NOT_YET_RELEASED", "Não lançado"],
              ]} />
              <div className="space-y-1.5">
                <Label htmlFor="anime-year" className="text-xs text-ivory-muted">Ano</Label>
                <Input
                  id="anime-year"
                  type="number"
                  value={year}
                  onChange={(e) => setYear(e.target.value)}
                  className="h-10 border-border-subtle bg-surface-elevated/50 text-sm text-ivory"
                />
              </div>
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <div className="space-y-1.5">
                <Label htmlFor="anime-season" className="text-xs text-ivory-muted">Temporada</Label>
                <Input
                  id="anime-season"
                  value={season}
                  onChange={(e) => setSeason(e.target.value)}
                  className="h-10 border-border-subtle bg-surface-elevated/50 text-sm text-ivory"
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="anime-studio" className="text-xs text-ivory-muted">Estúdio</Label>
                <Input
                  id="anime-studio"
                  value={studio}
                  onChange={(e) => setStudio(e.target.value)}
                  className="h-10 border-border-subtle bg-surface-elevated/50 text-sm text-ivory"
                />
              </div>
            </div>
          </section>

          <section className="space-y-4">
            <h3 className="text-[11px] font-medium uppercase tracking-[0.18em] text-gold/80">
              Sinopse e gêneros
            </h3>
            <div className="space-y-1.5">
              <Label htmlFor="anime-synopsis" className="text-xs text-ivory-muted">Sinopse</Label>
              <Textarea
                id="anime-synopsis"
                value={synopsis}
                onChange={(e) => setSynopsis(e.target.value)}
                rows={5}
                className="min-h-[120px] resize-y border-border-subtle bg-surface-elevated/50 text-sm text-ivory"
              />
            </div>
            <div className="flex flex-wrap gap-2">
              {GENRES.map((genre) => {
                const active = genres.includes(genre);
                return (
                  <button
                    key={genre}
                    type="button"
                    onClick={() => toggleGenre(genre)}
                    className={
                      "rounded-full border px-3 py-1 text-xs transition-all " +
                      (active
                        ? "border-gold/50 bg-gold/15 text-gold"
                        : "border-border-subtle bg-surface-elevated/40 text-ivory-muted hover:border-gold/30 hover:text-ivory")
                    }
                  >
                    {genre}
                  </button>
                );
              })}
            </div>
          </section>

          <section className="space-y-4">
            <h3 className="text-[11px] font-medium uppercase tracking-[0.18em] text-gold/80">
              Mídia
            </h3>
            <div className="grid gap-4 md:grid-cols-2">
              <MediaField id="anime-cover" label="Capa" value={coverUrl} onChange={setCoverUrl} aspect="aspect-[2/3]" />
              <MediaField id="anime-banner" label="Banner" value={bannerUrl} onChange={setBannerUrl} aspect="aspect-video" />
            </div>
          </section>

          <section className="flex items-center justify-between rounded-lg border border-border-subtle bg-surface-elevated/40 p-4">
            <div>
              <p className="text-sm font-medium text-ivory">Publicar imediatamente</p>
              <p className="text-xs text-ivory-muted">Se desativado, o anime será salvo como rascunho.</p>
            </div>
            <Switch checked={publish} onCheckedChange={setPublish} />
          </section>

          <DialogFooter className="gap-2 sm:gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={submitting}
              className="h-10 border-border-subtle bg-transparent text-sm text-ivory hover:bg-surface-elevated"
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              disabled={submitting}
              className="h-10 gap-2 bg-gold px-5 text-sm font-medium text-onyx hover:bg-gold/90"
            >
              {submitting ? <Loader2 className="size-4 animate-spin" /> : null}
              {isEdit ? "Salvar alterações" : publish ? "Criar e publicar" : "Salvar rascunho"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function SelectField({
  label,
  value,
  onValueChange,
  options,
}: {
  label: string;
  value: string;
  onValueChange: (value: string) => void;
  options: [string, string][];
}) {
  return (
    <div className="space-y-1.5">
      <Label className="text-xs text-ivory-muted">{label}</Label>
      <Select value={value} onValueChange={onValueChange}>
        <SelectTrigger className="h-10 border-border-subtle bg-surface-elevated/50 text-sm text-ivory">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {options.map(([key, text]) => (
            <SelectItem key={key} value={key}>{text}</SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}

function MediaField({
  id,
  label,
  value,
  onChange,
  aspect,
}: {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  aspect: string;
}) {
  return (
    <div className="space-y-2">
      <Label htmlFor={id} className="text-xs text-ivory-muted">{label}</Label>
      <div className="flex gap-3">
        <div
          className={
            "flex w-24 shrink-0 items-center justify-center overflow-hidden rounded-md border border-dashed border-border-subtle bg-onyx/60 " +
            aspect
          }
        >
          {value ? (
            <img src={value} alt="" className="size-full object-cover" />
          ) : (
            <ImagePlus className="size-5 text-ivory-muted" />
          )}
        </div>
        <Input
          id={id}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="h-10 border-border-subtle bg-surface-elevated/50 text-sm text-ivory"
        />
      </div>
    </div>
  );
}
