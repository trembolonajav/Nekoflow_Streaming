import { useEffect, useMemo, useState, type ReactNode } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  Clock,
  Plus,
  Save,
  Search,
  X,
} from "lucide-react";
import { toast } from "sonner";

import { AdminCard, StatusPill } from "@/components/admin/AdminCard";
import { NewEpisodeDialog, type EpisodeFormValues } from "@/components/admin/NewEpisodeDialog";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { ScrollArea } from "@/components/ui/scroll-area";
import {
  createAdminEpisode,
  fetchAdminAnimes,
  fetchAdminEpisodes,
  updateAdminEpisode,
  type AdminAnimeDto,
  type AdminEpisodeDto,
} from "@/lib/backend-api";
import { cn } from "@/lib/utils";

type PlanEntry = {
  dateKey: string;
  time: string;
};

function startOfWeek(date: Date) {
  const copy = new Date(date);
  const day = copy.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  copy.setDate(copy.getDate() + diff);
  copy.setHours(0, 0, 0, 0);
  return copy;
}

function addDays(date: Date, days: number) {
  const copy = new Date(date);
  copy.setDate(copy.getDate() + days);
  return copy;
}

function toDateKey(date: Date) {
  return date.toISOString().slice(0, 10);
}

function currentSeasonLabel(date: Date) {
  const month = date.getMonth() + 1;
  const season = month <= 3 ? "Inverno" : month <= 6 ? "Primavera" : month <= 9 ? "Verão" : "Outono";
  return `${season} ${date.getFullYear()}`;
}

function toTime(value: string | null) {
  if (!value) return "12:00";
  return new Date(value).toLocaleTimeString("pt-BR", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });
}

function toLocalIso(dateKey: string, time: string) {
  return new Date(`${dateKey}T${time || "12:00"}`).toISOString();
}

function statusTone(value: string) {
  if (value === "PUBLISHED") return "success" as const;
  if (value === "SCHEDULED") return "info" as const;
  return "warning" as const;
}

function presentStatus(value: string) {
  if (value === "PUBLISHED") return "Publicado";
  if (value === "SCHEDULED") return "Agendado";
  return "Rascunho";
}

function toFormValues(episode: AdminEpisodeDto): EpisodeFormValues {
  return {
    id: episode.id,
    animeId: episode.animeId,
    number: episode.number,
    title: episode.title,
    summary: episode.summary,
    durationSeconds: episode.durationSeconds,
    thumbnailUrl: episode.thumbnailUrl,
    previewUrl: episode.previewUrl,
    status: episode.status,
    scheduledFor: episode.scheduledFor,
    provider: episode.provider ?? "SEEKSTREAMING",
    externalVideoId: episode.externalVideoId,
    embedUrl: episode.embedUrl,
    playerUrl: episode.playerUrl,
  };
}

function planFromEpisodes(episodes: AdminEpisodeDto[]) {
  const next: Record<string, PlanEntry> = {};
  for (const episode of episodes) {
    if (!episode.scheduledFor) continue;
    next[episode.id] = {
      dateKey: toDateKey(new Date(episode.scheduledFor)),
      time: toTime(episode.scheduledFor),
    };
  }
  return next;
}

function latestEpisodesByAnime(episodes: AdminEpisodeDto[], animes: AdminAnimeDto[], seasonLabel: string) {
  const seasonAnimeIds = new Set(
    animes
      .filter((anime) => anime.seasonLabel?.trim().toLowerCase() === seasonLabel.toLowerCase())
      .map((anime) => anime.id),
  );
  const byAnime = new Map<string, AdminEpisodeDto>();
  for (const episode of episodes) {
    if (!seasonAnimeIds.has(episode.animeId)) continue;
    const current = byAnime.get(episode.animeId);
    if (!current || episode.number > current.number) {
      byAnime.set(episode.animeId, episode);
    }
  }
  return Array.from(byAnime.values());
}

function samePlan(a: Record<string, PlanEntry>, b: Record<string, PlanEntry>) {
  const keys = new Set([...Object.keys(a), ...Object.keys(b)]);
  for (const key of keys) {
    if (a[key]?.dateKey !== b[key]?.dateKey || a[key]?.time !== b[key]?.time) return false;
  }
  return true;
}

function AdminCalendario() {
  const queryClient = useQueryClient();
  const [weekStart, setWeekStart] = useState(() => startOfWeek(new Date()));
  const [activeDateKey, setActiveDateKey] = useState(() => toDateKey(startOfWeek(new Date())));
  const [query, setQuery] = useState("");
  const [plan, setPlan] = useState<Record<string, PlanEntry>>({});
  const [dialogOpen, setDialogOpen] = useState(false);

  const animesQuery = useQuery({ queryKey: ["admin-animes"], queryFn: fetchAdminAnimes });
  const episodesQuery = useQuery({ queryKey: ["admin-episodes"], queryFn: fetchAdminEpisodes });
  const currentSeason = currentSeasonLabel(weekStart);
  const episodes = episodesQuery.data ?? [];
  const latestEpisodes = useMemo(
    () => latestEpisodesByAnime(episodes, animesQuery.data ?? [], currentSeason),
    [animesQuery.data, currentSeason, episodes],
  );
  const initialPlan = useMemo(() => planFromEpisodes(latestEpisodes), [latestEpisodes]);

  useEffect(() => {
    setPlan(initialPlan);
  }, [initialPlan]);

  const days = useMemo(
    () => Array.from({ length: 7 }, (_, index) => addDays(weekStart, index)),
    [weekStart],
  );

  useEffect(() => {
    setActiveDateKey(toDateKey(weekStart));
  }, [weekStart]);

  const normalizedQuery = query.trim().toLowerCase();
  const filteredEpisodes = useMemo(
    () =>
      latestEpisodes
        .filter((episode) => {
          if (!normalizedQuery) return true;
          return (
            episode.animeTitle.toLowerCase().includes(normalizedQuery) ||
            episode.title.toLowerCase().includes(normalizedQuery) ||
            String(episode.number).includes(normalizedQuery)
          );
        })
        .sort((a, b) => a.animeTitle.localeCompare(b.animeTitle) || a.number - b.number),
    [latestEpisodes, normalizedQuery],
  );

  const selectedForDay = useMemo(
    () =>
      latestEpisodes
        .filter((episode) => plan[episode.id]?.dateKey === activeDateKey)
        .sort((a, b) => plan[a.id].time.localeCompare(plan[b.id].time) || a.animeTitle.localeCompare(b.animeTitle)),
    [activeDateKey, latestEpisodes, plan],
  );

  const isDirty = useMemo(() => !samePlan(plan, initialPlan), [initialPlan, plan]);

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ["admin-episodes"] });
    await queryClient.invalidateQueries({ queryKey: ["calendar-week"] });
  };

  const saveMutation = useMutation({
    mutationFn: async () => {
      const ids = Array.from(new Set([...Object.keys(initialPlan), ...Object.keys(plan)]));
      const changed = ids.filter((id) => {
        const before = initialPlan[id];
        const after = plan[id];
        return before?.dateKey !== after?.dateKey || before?.time !== after?.time;
      });

      await Promise.all(
        changed.map((id) => {
          const episode = episodes.find((item) => item.id === id);
          if (!episode) return Promise.resolve();
          const entry = plan[id];
          const payload = toFormValues(episode);
          return updateAdminEpisode(id, {
            ...payload,
            status: entry ? "SCHEDULED" : episode.status === "SCHEDULED" ? "DRAFT" : episode.status,
            scheduledFor: entry ? toLocalIso(entry.dateKey, entry.time) : null,
          });
        }),
      );
    },
    onSuccess: async () => {
      await invalidate();
      toast.success("Calendário atualizado.");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const createMutation = useMutation({
    mutationFn: async (payload: EpisodeFormValues) => createAdminEpisode(payload),
    onSuccess: async () => {
      await invalidate();
      toast.success("Episódio criado.");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const toggleEpisode = (episode: AdminEpisodeDto) => {
    setPlan((current) => {
      if (current[episode.id]?.dateKey === activeDateKey) {
        const next = { ...current };
        delete next[episode.id];
        return next;
      }
      return {
        ...current,
        [episode.id]: {
          dateKey: activeDateKey,
          time: current[episode.id]?.time ?? "12:00",
        },
      };
    });
  };

  const updateTime = (episodeId: string, time: string) => {
    setPlan((current) => ({
      ...current,
      [episodeId]: {
        dateKey: current[episodeId]?.dateKey ?? activeDateKey,
        time,
      },
    }));
  };

  const weekEnd = days[6];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-serif text-3xl text-ivory">Calendário</h1>
          <p className="mt-1 text-sm text-ivory-muted">
            Selecione o dia, marque os episódios e defina o horário de cada estreia.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            onClick={() => setDialogOpen(true)}
            className="h-11 gap-2 border-border-subtle bg-transparent px-5 text-sm text-ivory hover:bg-surface-elevated"
          >
            <Plus className="size-4" />
            Novo episódio
          </Button>
          <Button
            onClick={() => saveMutation.mutate()}
            disabled={!isDirty || saveMutation.isPending}
            className="h-11 gap-2 bg-gold px-5 text-sm font-medium text-onyx hover:bg-gold/90 disabled:opacity-50"
          >
            <Save className="size-4" />
            Salvar calendário
          </Button>
        </div>
      </div>

      <NewEpisodeDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        availableAnimes={(animesQuery.data ?? []).map((anime) => ({
          id: anime.id,
          title: anime.titleDisplay,
          seasonLabel: anime.seasonLabel,
        }))}
        onSubmit={async (payload) => {
          await createMutation.mutateAsync(payload);
        }}
      />

      <AdminCard className="p-4">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <span className="flex size-11 items-center justify-center rounded-lg border border-gold/20 bg-gold/10 text-gold">
              <CalendarDays className="size-5" />
            </span>
            <div>
              <p className="font-serif text-xl text-ivory">
                {weekStart.toLocaleDateString("pt-BR", { day: "2-digit", month: "long" })} -{" "}
                {weekEnd.toLocaleDateString("pt-BR", { day: "2-digit", month: "long" })}
              </p>
              <p className="text-xs uppercase tracking-[0.18em] text-ivory-muted">
                {isDirty ? "alterações pendentes" : "grade sincronizada"}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <WeekButton label="Semana anterior" onClick={() => setWeekStart((current) => addDays(current, -7))}>
              <ChevronLeft className="size-4" />
            </WeekButton>
            <Button
              variant="outline"
              onClick={() => setWeekStart(startOfWeek(new Date()))}
              className="h-9 border-gold/30 bg-gold/5 px-4 text-xs uppercase tracking-[0.16em] text-gold hover:bg-gold/10 hover:text-gold"
            >
              Hoje
            </Button>
            <WeekButton label="Próxima semana" onClick={() => setWeekStart((current) => addDays(current, 7))}>
              <ChevronRight className="size-4" />
            </WeekButton>
          </div>
        </div>

        <div className="mt-5 grid gap-2 md:grid-cols-7">
          {days.map((day) => {
            const key = toDateKey(day);
            const selectedCount = Object.values(plan).filter((entry) => entry.dateKey === key).length;
            const active = key === activeDateKey;
            return (
              <button
                key={key}
                type="button"
                onClick={() => setActiveDateKey(key)}
                className={cn(
                  "rounded-lg border px-3 py-3 text-left transition-colors",
                  active
                    ? "border-gold/50 bg-gold/10 text-gold"
                    : "border-border-subtle bg-surface-elevated/30 text-ivory hover:border-gold/30",
                )}
              >
                <p className="text-[10px] uppercase tracking-[0.2em]">
                  {day.toLocaleDateString("pt-BR", { weekday: "short" }).replace(".", "")}
                </p>
                <div className="mt-2 flex items-end justify-between gap-2">
                  <span className="font-serif text-2xl">{String(day.getDate()).padStart(2, "0")}</span>
                  <span className="text-[10px] uppercase tracking-[0.16em] text-ivory-muted">
                    {selectedCount} eps
                  </span>
                </div>
              </button>
            );
          })}
        </div>
      </AdminCard>

      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(380px,0.82fr)]">
        <AdminCard className="overflow-hidden">
          <div className="border-b border-border-subtle px-5 py-4">
            <h2 className="font-serif text-xl text-ivory">Episódios disponíveis</h2>
            <p className="mt-1 text-sm text-ivory-muted">
              Marque para colocar no dia selecionado. Se já estiver em outro dia, ele será movido.
            </p>
            <div className="relative mt-4">
              <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-ivory-muted" />
              <Input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Buscar anime, episódio ou número..."
                className="h-10 border-border-subtle bg-surface-elevated/50 pl-10 text-sm text-ivory placeholder:text-ivory-muted/70"
              />
            </div>
          </div>
          <ScrollArea className="h-[620px]">
            <ul className="divide-y divide-border-subtle">
              {filteredEpisodes.map((episode) => {
                const entry = plan[episode.id];
                const checked = entry?.dateKey === activeDateKey;
                return (
                  <li key={episode.id}>
                    <label
                      className={cn(
                        "flex cursor-pointer items-center gap-3 px-5 py-3 transition-colors hover:bg-surface-elevated/50",
                        checked && "bg-gold/5",
                      )}
                    >
                      <Checkbox checked={checked} onCheckedChange={() => toggleEpisode(episode)} />
                      {episode.thumbnailUrl ? (
                        <img src={episode.thumbnailUrl} alt="" className="h-12 w-20 rounded-md object-cover" />
                      ) : (
                        <div className="flex h-12 w-20 items-center justify-center rounded-md bg-surface-elevated text-xs text-ivory-muted">
                          EP {episode.number}
                        </div>
                      )}
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium text-ivory">{episode.animeTitle}</p>
                        <p className="truncate text-xs text-ivory-muted">
                          Episódio {episode.number} · {episode.title}
                        </p>
                      </div>
                      <div className="hidden items-end gap-2 md:flex md:flex-col">
                        <StatusPill label={presentStatus(episode.status)} tone={statusTone(episode.status)} />
                        {entry ? (
                          <span className="text-[10px] uppercase tracking-[0.16em] text-gold/80">
                            {new Date(`${entry.dateKey}T00:00:00`).toLocaleDateString("pt-BR", {
                              weekday: "short",
                              day: "2-digit",
                            })}
                          </span>
                        ) : null}
                      </div>
                    </label>
                  </li>
                );
              })}
            </ul>
          </ScrollArea>
        </AdminCard>

        <AdminCard className="overflow-hidden">
          <div className="border-b border-border-subtle px-5 py-4">
            <p className="text-[10px] uppercase tracking-[0.22em] text-gold">
              Dia selecionado
            </p>
            <h2 className="mt-1 font-serif text-2xl text-ivory">
              {new Date(`${activeDateKey}T00:00:00`).toLocaleDateString("pt-BR", {
                weekday: "long",
                day: "2-digit",
                month: "long",
              })}
            </h2>
          </div>
          <ScrollArea className="h-[620px]">
            <div className="space-y-3 p-4">
              {selectedForDay.length > 0 ? (
                selectedForDay.map((episode, index) => (
                  <div key={episode.id} className="rounded-lg border border-border-subtle bg-surface-elevated/35 p-3">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <span className="font-mono text-[10px] text-gold">
                          {String(index + 1).padStart(2, "0")}
                        </span>
                        <p className="truncate font-serif text-base text-ivory">{episode.animeTitle}</p>
                        <p className="text-xs text-ivory-muted">
                          Episódio {episode.number} · {episode.title}
                        </p>
                      </div>
                      <button
                        type="button"
                        onClick={() => toggleEpisode(episode)}
                        className="inline-flex size-8 shrink-0 items-center justify-center rounded-full text-ivory-muted transition-colors hover:bg-rose-500/10 hover:text-rose-300"
                        aria-label="Remover do dia"
                      >
                        <X className="size-4" />
                      </button>
                    </div>
                    <div className="mt-3 grid grid-cols-[1fr_auto] items-end gap-3">
                      <label className="space-y-1.5">
                        <span className="text-xs text-ivory-muted">Horário</span>
                        <Input
                          type="time"
                          value={plan[episode.id]?.time ?? "12:00"}
                          onChange={(event) => updateTime(episode.id, event.target.value)}
                          className="h-10 border-border-subtle bg-surface/70 text-sm text-ivory"
                        />
                      </label>
                      <span className="mb-2 inline-flex items-center gap-1 rounded-full border border-gold/20 px-2.5 py-1 text-[11px] text-gold/90">
                        <Clock className="size-3" />
                        {plan[episode.id]?.time ?? "12:00"}
                      </span>
                    </div>
                  </div>
                ))
              ) : (
                <div className="rounded-lg border border-dashed border-border-subtle px-6 py-20 text-center">
                  <p className="font-serif text-xl text-ivory">Nenhum episódio marcado</p>
                  <p className="mt-2 text-sm text-ivory-muted">
                    Selecione itens na lista à esquerda para montar este dia.
                  </p>
                </div>
              )}
            </div>
          </ScrollArea>
        </AdminCard>
      </div>
    </div>
  );
}

function WeekButton({
  label,
  onClick,
  children,
}: {
  label: string;
  onClick: () => void;
  children: ReactNode;
}) {
  return (
    <button
      type="button"
      aria-label={label}
      onClick={onClick}
      className="inline-flex size-9 items-center justify-center rounded-full border border-border-subtle text-ivory transition-colors hover:border-gold/60 hover:text-gold"
    >
      {children}
    </button>
  );
}

export default AdminCalendario;
