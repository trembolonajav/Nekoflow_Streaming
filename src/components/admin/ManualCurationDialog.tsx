import { useEffect, useMemo, useState } from "react";
import { Search, Check } from "lucide-react";

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
import { cn } from "@/lib/utils";

interface ItemOption {
  id: string;
  title: string;
  subtitle: string | null;
  image: string | null;
}

interface ManualCurationDialogProps {
  title: string;
  description: string;
  items: ItemOption[];
  selectedIds: string[];
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSave: (ids: string[]) => Promise<void>;
}

export function ManualCurationDialog({
  title,
  description,
  items,
  selectedIds,
  open,
  onOpenChange,
  onSave,
}: ManualCurationDialogProps) {
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState<string[]>(selectedIds);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setSelected(selectedIds);
    setQuery("");
  }, [open, selectedIds]);

  const filtered = useMemo(
    () => items.filter((item) => item.title.toLowerCase().includes(query.toLowerCase())),
    [items, query],
  );

  const toggle = (id: string) => {
    setSelected((prev) => (prev.includes(id) ? prev.filter((value) => value !== id) : [...prev, id]));
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      await onSave(selected);
      onOpenChange(false);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] max-w-3xl overflow-hidden border-gold/15 bg-surface/95 text-ivory backdrop-blur-xl">
        <DialogHeader>
          <DialogTitle className="font-serif text-2xl text-ivory">{title}</DialogTitle>
          <DialogDescription className="text-ivory-muted">{description}</DialogDescription>
        </DialogHeader>

        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-ivory-muted" />
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="h-10 border-border-subtle bg-surface-elevated/50 pl-10 text-sm text-ivory"
          />
        </div>

        <div className="-mr-2 max-h-[50vh] overflow-y-auto pr-2">
          <ul className="grid grid-cols-1 gap-2 sm:grid-cols-2">
            {filtered.map((item) => {
              const active = selected.includes(item.id);
              return (
                <li key={item.id}>
                  <button
                    type="button"
                    onClick={() => toggle(item.id)}
                    className={cn(
                      "flex w-full items-center gap-3 rounded-lg border p-2.5 text-left transition-all",
                      active
                        ? "border-gold/50 bg-gold/10"
                        : "border-border-subtle bg-surface-elevated/40 hover:border-gold/30 hover:bg-surface-elevated",
                    )}
                  >
                    {item.image ? (
                      <img src={item.image} alt="" className="size-12 shrink-0 rounded-md object-cover" />
                    ) : (
                      <div className="size-12 shrink-0 rounded-md bg-surface" />
                    )}
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium text-ivory">{item.title}</p>
                      {item.subtitle ? <p className="truncate text-[11px] text-ivory-muted">{item.subtitle}</p> : null}
                    </div>
                    <span
                      className={cn(
                        "flex size-5 shrink-0 items-center justify-center rounded-full border transition-all",
                        active ? "border-gold bg-gold text-onyx" : "border-border-subtle text-transparent",
                      )}
                    >
                      <Check className="size-3" strokeWidth={3} />
                    </span>
                  </button>
                </li>
              );
            })}
          </ul>
        </div>

        <DialogFooter className="gap-2 sm:gap-2">
          <p className="mr-auto self-center text-xs text-ivory-muted">
            {selected.length} {selected.length === 1 ? "selecionado" : "selecionados"}
          </p>
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
            Salvar curadoria
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
