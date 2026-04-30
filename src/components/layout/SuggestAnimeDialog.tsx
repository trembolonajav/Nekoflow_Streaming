import { useState, type ReactNode } from "react";
import { Plus } from "lucide-react";
import { toast } from "sonner";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { OrnamentDivider } from "./OrnamentDivider";

interface SuggestAnimeDialogProps {
  trigger: ReactNode;
}

export function SuggestAnimeDialog({ trigger }: SuggestAnimeDialogProps) {
  const [open, setOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting(true);

    // Mock — sem backend nesta camada
    setTimeout(() => {
      setSubmitting(false);
      setOpen(false);
      toast.success("Sugestão recebida", {
        description: "Obrigado por contribuir com o catálogo do Nekoflow.",
      });
    }, 450);
  };

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>{trigger}</DialogTrigger>
      <DialogContent className="border-gold/15 bg-surface/95 backdrop-blur-xl sm:max-w-[480px]">
        <DialogHeader className="space-y-3 text-left">
          <DialogTitle className="font-serif text-2xl font-medium tracking-tight text-ivory">
            Sugerir um anime
          </DialogTitle>
          <OrnamentDivider width="sm" className="!mx-0" />
          <DialogDescription className="text-sm leading-relaxed text-ivory-muted">
            Não encontrou um título no catálogo? Conte para a curadoria do Nekoflow.
            Avaliamos cada sugestão antes de adicionar.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="mt-2 space-y-4">
          <div className="space-y-2">
            <Label htmlFor="anime-name" className="text-xs uppercase tracking-[0.14em] text-ivory-muted">
              Nome do anime
            </Label>
            <Input
              id="anime-name"
              required
              placeholder="Ex.: Frieren: Beyond Journey's End"
              className="border-border-subtle bg-onyx/50 text-ivory placeholder:text-ivory-muted/60 focus-visible:ring-2 focus-visible:ring-gold/40"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-2">
              <Label htmlFor="anime-year" className="text-xs uppercase tracking-[0.14em] text-ivory-muted">
                Ano (opcional)
              </Label>
              <Input
                id="anime-year"
                type="number"
                min={1960}
                max={2099}
                placeholder="2024"
                className="border-border-subtle bg-onyx/50 text-ivory placeholder:text-ivory-muted/60 focus-visible:ring-2 focus-visible:ring-gold/40"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="anime-link" className="text-xs uppercase tracking-[0.14em] text-ivory-muted">
                Referência (opcional)
              </Label>
              <Input
                id="anime-link"
                type="url"
                placeholder="MAL, AniList, site oficial…"
                className="border-border-subtle bg-onyx/50 text-ivory placeholder:text-ivory-muted/60 focus-visible:ring-2 focus-visible:ring-gold/40"
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="anime-note" className="text-xs uppercase tracking-[0.14em] text-ivory-muted">
              Observação (opcional)
            </Label>
            <Textarea
              id="anime-note"
              rows={3}
              placeholder="Por que esse anime merece estar no Nekoflow?"
              className="resize-none border-border-subtle bg-onyx/50 text-ivory placeholder:text-ivory-muted/60 focus-visible:ring-2 focus-visible:ring-gold/40"
            />
          </div>

          <DialogFooter className="mt-2 gap-2 sm:gap-2">
            <Button
              type="button"
              variant="ghost"
              onClick={() => setOpen(false)}
              className="text-ivory-muted hover:bg-surface-elevated hover:text-ivory"
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              disabled={submitting}
              className="bg-gold text-onyx shadow-[0_0_24px_-8px_var(--gold-glow)] hover:bg-gold/90"
            >
              <Plus className="size-4" />
              {submitting ? "Enviando…" : "Enviar sugestão"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
