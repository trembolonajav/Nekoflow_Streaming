import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Calendar, Clock, GripVertical, PlayCircle, Sparkles, Star } from "lucide-react";
import { toast } from "sonner";

import {
  fetchAdminAnimes,
  fetchAdminEpisodes,
  fetchAdminHome,
  updateAdminHero,
  updateAdminHomeSections,
  type AdminHomeSectionDto,
} from "@/lib/backend-api";
import { AdminCard, StatusPill } from "@/components/admin/AdminCard";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ManualCurationDialog } from "@/components/admin/ManualCurationDialog";
import { HeroEditorDialog } from "@/components/admin/HeroEditorDialog";

const ICON_FOR: Record<string, typeof Star> = {
  hero: Star,
  continue: PlayCircle,
  season: Calendar,
  recent: Clock,
};

function modeTone(mode: string) {
  if (mode === "MANUAL") return "warning" as const;
  if (mode === "AUTOMATIC") return "info" as const;
  return "success" as const;
}

function presentMode(mode: string) {
  if (mode === "MANUAL") return "Manual";
  if (mode === "AUTOMATIC") return "Automático";
  return "Híbrido";
}

function AdminHome() {
  const queryClient = useQueryClient();
  const homeQuery = useQuery({ queryKey: ["admin-home"], queryFn: fetchAdminHome });
  const animesQuery = useQuery({ queryKey: ["admin-animes"], queryFn: fetchAdminAnimes });
  const episodesQuery = useQuery({ queryKey: ["admin-episodes"], queryFn: fetchAdminEpisodes });

  const [sections, setSections] = useState<AdminHomeSectionDto[]>([]);
  const [editingSectionCode, setEditingSectionCode] = useState<string | null>(null);
  const [heroOpen, setHeroOpen] = useState(false);

  useEffect(() => {
    if (homeQuery.data) {
      setSections(homeQuery.data.sections);
    }
  }, [homeQuery.data]);

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ["admin-home"] });
    await queryClient.invalidateQueries({ queryKey: ["public-home"] });
  };

  const sectionsMutation = useMutation({
    mutationFn: updateAdminHomeSections,
    onSuccess: async () => {
      await invalidate();
      toast.success("Curadoria da home atualizada.");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const heroMutation = useMutation({
    mutationFn: updateAdminHero,
    onSuccess: async () => {
      await invalidate();
      toast.success("Hero atualizado.");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const updateSection = (code: string, patch: Partial<AdminHomeSectionDto>) => {
    setSections((prev) => prev.map((section) => (section.code === code ? { ...section, ...patch } : section)));
  };

  const persistSections = async (next: AdminHomeSectionDto[]) => {
    setSections(next);
    await sectionsMutation.mutateAsync(next);
  };

  const editingSection = sections.find((section) => section.code === editingSectionCode) ?? null;

  const manualItems = useMemo(() => {
    if (!editingSection) return [];
    if (editingSection.code === "recent") {
      return (episodesQuery.data ?? [])
        .filter((episode) => episode.status === "PUBLISHED")
        .map((episode) => ({
          id: episode.id,
          title: `${episode.animeTitle} — Ep. ${episode.number}`,
          subtitle: episode.title,
          image: episode.thumbnailUrl,
        }));
    }
    return (animesQuery.data ?? [])
      .filter((anime) => anime.visibility === "PUBLISHED")
      .map((anime) => ({
        id: anime.id,
        title: anime.titleDisplay,
        subtitle: anime.seasonLabel,
        image: anime.coverUrl,
      }));
  }, [editingSection, animesQuery.data, episodesQuery.data]);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-serif text-3xl text-ivory">Home / Curadoria</h1>
          <p className="mt-1 text-sm text-ivory-muted">
            Ordem, blocos ativos e seleção manual já persistidos no backend.
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        {sections.map((section, index) => {
          const Icon = ICON_FOR[section.code] ?? Sparkles;
          const isHero = section.code === "hero";
          return (
            <AdminCard key={section.code} className="p-5">
              <div className="flex items-start gap-4">
                <button
                  type="button"
                  onClick={async () => {
                    if (index === 0) return;
                    const next = [...sections];
                    [next[index - 1], next[index]] = [next[index], next[index - 1]];
                    next.forEach((item, itemIndex) => {
                      item.sortOrder = itemIndex + 1;
                    });
                    await persistSections(next);
                  }}
                  className="mt-1 text-ivory-muted/60 transition-colors hover:text-ivory-muted"
                  aria-label="Reordenar"
                >
                  <GripVertical className="size-4" />
                </button>
                <div className="flex size-11 shrink-0 items-center justify-center rounded-lg border border-gold/20 bg-gold/5 text-gold">
                  <Icon className="size-5" />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <h3 className="font-serif text-lg text-ivory">{section.title}</h3>
                    {!isHero ? <StatusPill label={presentMode(section.mode)} tone={modeTone(section.mode)} /> : null}
                    <span className="ml-auto text-[10px] uppercase tracking-[0.18em] text-ivory-muted/60">
                      Posição {String(index + 1).padStart(2, "0")}
                    </span>
                  </div>
                  <p className="mt-1 text-[12px] text-ivory-muted">
                    {isHero
                      ? `${homeQuery.data?.hero.animeIds.length ?? 0} animes no carrossel principal`
                      : `${section.manualItemIds.length} itens manuais configurados`}
                  </p>

                  <div className="mt-4 flex flex-wrap items-center gap-3">
                    {!isHero ? (
                      <Select
                        value={section.mode}
                        onValueChange={async (value) => {
                          const next = sections.map((item) => item.code === section.code ? { ...item, mode: value } : item);
                          await persistSections(next);
                        }}
                      >
                        <SelectTrigger className="h-9 w-[160px] border-border-subtle bg-surface-elevated/50 text-xs text-ivory">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="MANUAL">Manual</SelectItem>
                          <SelectItem value="AUTOMATIC">Automático</SelectItem>
                          <SelectItem value="HYBRID">Híbrido</SelectItem>
                        </SelectContent>
                      </Select>
                    ) : null}

                    {isHero ? (
                      <Button
                        variant="outline"
                        onClick={() => setHeroOpen(true)}
                        className="h-9 border-border-subtle bg-surface-elevated/40 px-3 text-xs text-ivory hover:border-gold/30 hover:bg-surface-elevated"
                      >
                        Editar Hero
                      </Button>
                    ) : (
                      <Button
                        variant="outline"
                        onClick={() => setEditingSectionCode(section.code)}
                        disabled={section.mode === "AUTOMATIC"}
                        className="h-9 border-border-subtle bg-surface-elevated/40 px-3 text-xs text-ivory hover:border-gold/30 hover:bg-surface-elevated disabled:opacity-50"
                      >
                        Editar itens
                      </Button>
                    )}

                    <div className="ml-auto flex items-center gap-2">
                      <span className="text-[11px] text-ivory-muted">Ativo</span>
                      <Switch
                        checked={section.active}
                        onCheckedChange={async (checked) => {
                          const next = sections.map((item) => item.code === section.code ? { ...item, active: checked } : item);
                          await persistSections(next);
                        }}
                      />
                    </div>
                  </div>
                </div>
              </div>
            </AdminCard>
          );
        })}
      </div>

      {editingSection ? (
        <ManualCurationDialog
          title={`Curadoria — ${editingSection.title}`}
          description="Selecione os itens manuais que devem aparecer nesse bloco."
          items={manualItems}
          selectedIds={editingSection.manualItemIds}
          open={Boolean(editingSection)}
          onOpenChange={(open) => !open && setEditingSectionCode(null)}
          onSave={async (ids) => {
            const next = sections.map((section) =>
              section.code === editingSection.code ? { ...section, manualItemIds: ids } : section,
            );
            await persistSections(next);
          }}
        />
      ) : null}

      {homeQuery.data ? (
        <HeroEditorDialog
          open={heroOpen}
          onOpenChange={setHeroOpen}
          hero={homeQuery.data.hero}
          animes={(animesQuery.data ?? []).map((anime) => ({
            id: anime.id,
            title: anime.titleDisplay,
            coverUrl: anime.coverUrl,
            seasonLabel: anime.seasonLabel,
            visibility: anime.visibility,
          }))}
          onSave={async (payload) => {
            await heroMutation.mutateAsync(payload);
          }}
        />
      ) : null}
    </div>
  );
}

export default AdminHome;
