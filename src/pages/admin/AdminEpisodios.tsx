import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Calendar, Image as ImageIcon, MoreHorizontal, Pencil, Plus, Search, Trash2, Video } from "lucide-react";
import { toast } from "sonner";

import {
  createAdminEpisode,
  deleteAdminEpisode,
  fetchAdminAnimes,
  fetchAdminEpisodes,
  updateAdminEpisode,
  type AdminEpisodeDto,
} from "@/lib/backend-api";
import { AdminCard, StatusPill } from "@/components/admin/AdminCard";
import { NewEpisodeDialog, type EpisodeFormValues } from "@/components/admin/NewEpisodeDialog";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

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

function AdminEpisodes() {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("todos");
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<EpisodeFormValues | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<AdminEpisodeDto | null>(null);

  const animesQuery = useQuery({ queryKey: ["admin-animes"], queryFn: fetchAdminAnimes });
  const episodesQuery = useQuery({ queryKey: ["admin-episodes"], queryFn: fetchAdminEpisodes });

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ["admin-episodes"] });
    await queryClient.invalidateQueries({ queryKey: ["public-home"] });
    await queryClient.invalidateQueries({ queryKey: ["admin-home"] });
    await queryClient.invalidateQueries({ queryKey: ["calendar-week"] });
  };

  const saveMutation = useMutation({
    mutationFn: async (payload: EpisodeFormValues) => {
      if (payload.id) {
        return updateAdminEpisode(payload.id, payload);
      }
      return createAdminEpisode(payload);
    },
    onSuccess: async (_, payload) => {
      await invalidate();
      toast.success(payload.id ? "Episódio atualizado." : "Episódio criado.");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const deleteMutation = useMutation({
    mutationFn: deleteAdminEpisode,
    onSuccess: async () => {
      await invalidate();
      toast.success("Episódio excluído.");
      setConfirmDelete(null);
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const filtered = useMemo(
    () =>
      (episodesQuery.data ?? []).filter((episode) => {
        const normalized = query.toLowerCase();
        const matches =
          episode.title.toLowerCase().includes(normalized) ||
          episode.animeTitle.toLowerCase().includes(normalized);
        const matchesStatus = statusFilter === "todos" || episode.status === statusFilter;
        return matches && matchesStatus;
      }),
    [episodesQuery.data, query, statusFilter],
  );

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-serif text-3xl text-ivory">Episódios</h1>
          <p className="mt-1 text-sm text-ivory-muted">
            Cadastro e publicação de episódios conectados ao SeekStreaming.
          </p>
        </div>
        <Button
          onClick={() => {
            setEditing(null);
            setDialogOpen(true);
          }}
          className="h-11 gap-2 bg-gold px-5 text-sm font-medium text-onyx hover:bg-gold/90"
        >
          <Plus className="size-4" />
          Novo episódio
        </Button>
      </div>

      <NewEpisodeDialog
        open={dialogOpen}
        onOpenChange={(open) => {
          setDialogOpen(open);
          if (!open) setEditing(null);
        }}
        editing={editing}
        availableAnimes={(animesQuery.data ?? []).map((anime) => ({
          id: anime.id,
          title: anime.titleDisplay,
          seasonLabel: anime.seasonLabel,
        }))}
        onSubmit={async (payload) => {
          await saveMutation.mutateAsync(payload);
        }}
      />

      <AlertDialog open={Boolean(confirmDelete)} onOpenChange={(open) => !open && setConfirmDelete(null)}>
        <AlertDialogContent className="border-gold/15 bg-surface/95 text-ivory backdrop-blur-xl">
          <AlertDialogHeader>
            <AlertDialogTitle className="font-serif text-xl text-ivory">Excluir episódio?</AlertDialogTitle>
            <AlertDialogDescription className="text-ivory-muted">
              Esta ação remove o episódio {confirmDelete?.number} de “{confirmDelete?.animeTitle}”.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel className="border-border-subtle bg-transparent text-ivory hover:bg-surface-elevated hover:text-ivory">
              Cancelar
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={() => confirmDelete && deleteMutation.mutate(confirmDelete.id)}
              className="bg-rose-500/90 text-ivory hover:bg-rose-500"
            >
              Excluir
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AdminCard>
        <div className="flex flex-wrap items-center gap-3 border-b border-border-subtle p-4">
          <div className="relative min-w-[240px] flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-ivory-muted" />
            <Input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Buscar por episódio ou anime…"
              className="h-10 border-border-subtle bg-surface-elevated/50 pl-10 text-sm text-ivory placeholder:text-ivory-muted/70"
            />
          </div>
          <Select value={statusFilter} onValueChange={setStatusFilter}>
            <SelectTrigger className="h-10 w-[180px] border-border-subtle bg-surface-elevated/50 text-sm text-ivory">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="todos">Todos os status</SelectItem>
              <SelectItem value="PUBLISHED">Publicado</SelectItem>
              <SelectItem value="DRAFT">Rascunho</SelectItem>
              <SelectItem value="SCHEDULED">Agendado</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border-subtle text-[11px] uppercase tracking-[0.12em] text-ivory-muted">
                <th className="px-5 py-3 text-left font-medium">Episódio</th>
                <th className="px-3 py-3 text-left font-medium">Anime</th>
                <th className="px-3 py-3 text-left font-medium">Duração</th>
                <th className="px-3 py-3 text-left font-medium">Mídia</th>
                <th className="px-3 py-3 text-left font-medium">Status</th>
                <th className="px-3 py-3 text-left font-medium">Atualizado</th>
                <th className="px-5 py-3 text-right font-medium">Ações</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((episode) => (
                <tr key={episode.id} className="border-b border-border-subtle/60 transition-colors last:border-0 hover:bg-surface-elevated/40">
                  <td className="px-5 py-3">
                    <div className="flex items-center gap-3">
                      <span className="flex size-10 shrink-0 items-center justify-center rounded-md border border-gold/20 bg-onyx font-serif text-base text-gold">
                        {episode.number}
                      </span>
                      <div className="min-w-0">
                        <p className="truncate font-medium text-ivory">{episode.title}</p>
                        <p className="text-[11px] text-ivory-muted">Episódio {episode.number}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-3 py-3 text-ivory-muted">{episode.animeTitle}</td>
                  <td className="px-3 py-3 text-ivory-muted">
                    {episode.durationSeconds ? `${Math.round(episode.durationSeconds / 60)} min` : "—"}
                  </td>
                  <td className="px-3 py-3">
                    <div className="flex items-center gap-1.5">
                      <Video className={"size-4 " + (episode.embedUrl || episode.playerUrl ? "text-emerald-400" : "text-rose-400/80")} />
                      <ImageIcon className={"size-4 " + (episode.thumbnailUrl ? "text-emerald-400" : "text-rose-400/80")} />
                    </div>
                  </td>
                  <td className="px-3 py-3">
                    <div className="flex flex-col gap-1">
                      <StatusPill label={presentStatus(episode.status)} tone={statusTone(episode.status)} />
                      {episode.scheduledFor ? (
                        <span className="inline-flex items-center gap-1 text-[10px] text-ivory-muted">
                          <Calendar className="size-3" />
                          {new Date(episode.scheduledFor).toLocaleString("pt-BR")}
                        </span>
                      ) : null}
                    </div>
                  </td>
                  <td className="px-3 py-3 text-[12px] text-ivory-muted">
                    {episode.updatedAt ? new Date(episode.updatedAt).toLocaleString("pt-BR") : "—"}
                  </td>
                  <td className="px-5 py-3 text-right">
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon" className="h-8 w-8 text-ivory-muted hover:bg-surface-elevated hover:text-gold">
                          <MoreHorizontal className="size-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end" className="w-44 border-gold/15 bg-surface/95 backdrop-blur-xl">
                        <DropdownMenuItem
                          onClick={() => {
                            setEditing(toFormValues(episode));
                            setDialogOpen(true);
                          }}
                          className="text-ivory focus:bg-surface-elevated focus:text-gold"
                        >
                          <Pencil className="size-4" /> Editar
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          onClick={() => setConfirmDelete(episode)}
                          className="text-rose-300 focus:bg-rose-500/10 focus:text-rose-200"
                        >
                          <Trash2 className="size-4" /> Excluir
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminCard>
    </div>
  );
}

export default AdminEpisodes;
