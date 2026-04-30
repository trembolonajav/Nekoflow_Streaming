import { useState, useEffect, type FormEvent } from "react";
import { ImagePlus, Loader2, Video } from "lucide-react";
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
import type { AdminEpisodePayload } from "@/lib/backend-api";
import { SeekStreamingPanel, type SeekStreamingPick } from "./SeekStreamingPanel";

export interface EpisodeFormValues extends AdminEpisodePayload {
  id?: string;
}

interface AnimeOption {
  id: string;
  title: string;
  seasonLabel: string | null;
}

interface NewEpisodeDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  availableAnimes: AnimeOption[];
  editing?: EpisodeFormValues | null;
  onSubmit: (payload: EpisodeFormValues) => Promise<void>;
}

export function NewEpisodeDialog({
  open,
  onOpenChange,
  availableAnimes,
  editing,
  onSubmit,
}: NewEpisodeDialogProps) {
  const isEdit = Boolean(editing);
  const [animeId, setAnimeId] = useState("");
  const [number, setNumber] = useState("");
  const [title, setTitle] = useState("");
  const [summary, setSummary] = useState("");
  const [duration, setDuration] = useState("");
  const [embedUrl, setEmbedUrl] = useState("");
  const [playerUrl, setPlayerUrl] = useState("");
  const [externalVideoId, setExternalVideoId] = useState("");
  const [thumbUrl, setThumbUrl] = useState("");
  const [previewUrl, setPreviewUrl] = useState("");
  const [scheduleEnabled, setScheduleEnabled] = useState(false);
  const [scheduledFor, setScheduledFor] = useState("");
  const [publish, setPublish] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!open) return;
    if (editing) {
      setAnimeId(editing.animeId);
      setNumber(String(editing.number));
      setTitle(editing.title);
      setSummary(editing.summary ?? "");
      setDuration(editing.durationSeconds ? String(Math.round(editing.durationSeconds / 60)) : "");
      setEmbedUrl(editing.embedUrl ?? "");
      setPlayerUrl(editing.playerUrl ?? "");
      setExternalVideoId(editing.externalVideoId ?? "");
      setThumbUrl(editing.thumbnailUrl ?? "");
      setPreviewUrl(editing.previewUrl ?? "");
      setScheduleEnabled(editing.status === "SCHEDULED");
      setScheduledFor(editing.scheduledFor ? editing.scheduledFor.slice(0, 16) : "");
      setPublish(editing.status === "PUBLISHED");
    } else {
      setAnimeId(availableAnimes[0]?.id ?? "");
      setNumber("");
      setTitle("");
      setSummary("");
      setDuration("");
      setEmbedUrl("");
      setPlayerUrl("");
      setExternalVideoId("");
      setThumbUrl("");
      setPreviewUrl("");
      setScheduleEnabled(false);
      setScheduledFor("");
      setPublish(false);
    }
  }, [open, editing, availableAnimes]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!animeId) {
      toast.error("Selecione o anime vinculado.");
      return;
    }
    if (!number.trim() || !title.trim()) {
      toast.error("Informe número e título do episódio.");
      return;
    }
    if (publish && !embedUrl.trim() && !playerUrl.trim()) {
      toast.error("Episódios publicados precisam de URL de player ou embed.");
      return;
    }

    setSubmitting(true);
    try {
      await onSubmit({
        id: editing?.id,
        animeId,
        number: Number(number),
        title: title.trim(),
        summary: summary || null,
        durationSeconds: duration ? Number(duration) * 60 : null,
        thumbnailUrl: thumbUrl || null,
        previewUrl: previewUrl || null,
        status: scheduleEnabled ? "SCHEDULED" : publish ? "PUBLISHED" : "DRAFT",
        scheduledFor: scheduleEnabled && scheduledFor ? new Date(scheduledFor).toISOString() : null,
        provider: "SEEKSTREAMING",
        externalVideoId: externalVideoId || null,
        embedUrl: embedUrl || null,
        playerUrl: playerUrl || null,
      });
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
            {isEdit ? "Editar episódio" : "Novo episódio"}
          </DialogTitle>
          <DialogDescription className="text-ivory-muted">
            Vincule ao anime, configure o SeekStreaming e defina o status editorial.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-6 pt-2">
          {!isEdit ? (
            <SeekStreamingPanel
              open={open}
              onPick={(pick: SeekStreamingPick) => {
                setExternalVideoId(pick.externalVideoId);
                setEmbedUrl(pick.embedUrl);
                if (!thumbUrl && pick.thumbnailUrl) {
                  setThumbUrl(pick.thumbnailUrl);
                }
                if (!previewUrl && pick.previewUrl) {
                  setPreviewUrl(pick.previewUrl);
                }
                if (!duration && pick.durationMinutes) {
                  setDuration(pick.durationMinutes);
                }
                if (!number && pick.episodeNumber !== undefined) {
                  setNumber(String(pick.episodeNumber));
                }
                if (!title && pick.suggestedTitle) {
                  setTitle(pick.suggestedTitle);
                }
              }}
            />
          ) : null}

          <section className="space-y-4">
            <h3 className="text-[11px] font-medium uppercase tracking-[0.18em] text-gold/80">Vínculo</h3>
            <div className="space-y-1.5">
              <Label className="text-xs text-ivory-muted">Anime</Label>
              <Select value={animeId} onValueChange={setAnimeId}>
                <SelectTrigger className="h-10 border-border-subtle bg-surface-elevated/50 text-sm text-ivory">
                  <SelectValue placeholder="Selecione um anime…" />
                </SelectTrigger>
                <SelectContent className="max-h-72">
                  {availableAnimes.map((anime) => (
                    <SelectItem key={anime.id} value={anime.id}>
                      {anime.title} {anime.seasonLabel ? `— ${anime.seasonLabel}` : ""}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-4 md:grid-cols-3">
              <Field label="Nº do episódio" value={number} onChange={setNumber} type="number" />
              <Field label="Duração (min)" value={duration} onChange={setDuration} type="number" />
              <div className="space-y-1.5">
                <Label className="text-xs text-ivory-muted">Status</Label>
                <Select
                  value={scheduleEnabled ? "SCHEDULED" : publish ? "PUBLISHED" : "DRAFT"}
                  onValueChange={(value) => {
                    setScheduleEnabled(value === "SCHEDULED");
                    setPublish(value === "PUBLISHED");
                  }}
                >
                  <SelectTrigger className="h-10 border-border-subtle bg-surface-elevated/50 text-sm text-ivory">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="DRAFT">Rascunho</SelectItem>
                    <SelectItem value="PUBLISHED">Publicado</SelectItem>
                    <SelectItem value="SCHEDULED">Agendado</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
          </section>

          <section className="space-y-4">
            <h3 className="text-[11px] font-medium uppercase tracking-[0.18em] text-gold/80">Conteúdo</h3>
            <Field label="Título" value={title} onChange={setTitle} />
            <div className="space-y-1.5">
              <Label className="text-xs text-ivory-muted">Resumo</Label>
              <Textarea
                value={summary}
                onChange={(e) => setSummary(e.target.value)}
                rows={4}
                className="min-h-[100px] resize-y border-border-subtle bg-surface-elevated/50 text-sm text-ivory"
              />
            </div>
          </section>

          <section className="space-y-4">
            <h3 className="text-[11px] font-medium uppercase tracking-[0.18em] text-gold/80">SeekStreaming</h3>
            <Field label="External video ID" value={externalVideoId} onChange={setExternalVideoId} />
            <Field label="Embed URL" value={embedUrl} onChange={setEmbedUrl} icon={<Video className="size-4 text-ivory-muted" />} />
            <Field label="Player URL" value={playerUrl} onChange={setPlayerUrl} />
          </section>

          <section className="space-y-4">
            <h3 className="text-[11px] font-medium uppercase tracking-[0.18em] text-gold/80">Mídia</h3>
            <div className="space-y-1.5">
              <Label className="text-xs text-ivory-muted">Thumbnail (URL)</Label>
              <div className="flex gap-3">
                <div className="flex aspect-video w-24 shrink-0 items-center justify-center overflow-hidden rounded-md border border-dashed border-border-subtle bg-onyx/60">
                  {thumbUrl ? (
                    <img src={thumbUrl} alt="" className="size-full object-cover" />
                  ) : (
                    <ImagePlus className="size-4 text-ivory-muted" />
                  )}
                </div>
                <Input
                  value={thumbUrl}
                  onChange={(e) => setThumbUrl(e.target.value)}
                  className="h-10 border-border-subtle bg-surface-elevated/50 text-sm text-ivory"
                />
              </div>
            </div>
            <Field label="Preview (URL)" value={previewUrl} onChange={setPreviewUrl} />
          </section>

          <section className="space-y-3 rounded-lg border border-border-subtle bg-surface-elevated/40 p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-ivory">Agendar publicação</p>
                <p className="text-xs text-ivory-muted">Use apenas quando a data de estreia já estiver definida.</p>
              </div>
              <Switch
                checked={scheduleEnabled}
                onCheckedChange={(checked) => {
                  setScheduleEnabled(checked);
                  if (checked) setPublish(false);
                }}
              />
            </div>
            {scheduleEnabled ? (
              <Input
                type="datetime-local"
                value={scheduledFor}
                onChange={(e) => setScheduledFor(e.target.value)}
                className="h-10 border-border-subtle bg-surface-elevated/50 text-sm text-ivory"
              />
            ) : null}
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
              {isEdit ? "Salvar alterações" : "Salvar episódio"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function Field({
  label,
  value,
  onChange,
  type = "text",
  icon,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
  icon?: React.ReactNode;
}) {
  return (
    <div className="space-y-1.5">
      <Label className="text-xs text-ivory-muted">{label}</Label>
      <div className="relative">
        {icon ? <span className="absolute left-3 top-1/2 -translate-y-1/2">{icon}</span> : null}
        <Input
          type={type}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className={
            "h-10 border-border-subtle bg-surface-elevated/50 text-sm text-ivory " +
            (icon ? "pl-10" : "")
          }
        />
      </div>
    </div>
  );
}
