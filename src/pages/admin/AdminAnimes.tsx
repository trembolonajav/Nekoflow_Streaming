import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Archive, ArchiveRestore, Eye, MoreHorizontal, Pencil, Plus, Search, Trash2 } from "lucide-react";
import { toast } from "sonner";

import {
  deleteAdminAnime,
  fetchAdminAnimes,
  updateAdminAnimeVisibility,
  createAdminAnime,
  updateAdminAnime,
  type AdminAnimeDto,
} from "@/lib/backend-api";
import { AdminCard, StatusPill } from "@/components/admin/AdminCard";
import { NewAnimeDialog, type AnimeFormValues } from "@/components/admin/NewAnimeDialog";
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
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

function visibilityTone(value: string) {
  if (value === "PUBLISHED") return "success" as const;
  if (value === "DRAFT") return "warning" as const;
  return "muted" as const;
}

function statusTone(value: string) {
  if (value === "RELEASING") return "info" as const;
  if (value === "FINISHED") return "success" as const;
  return "warning" as const;
}

function presentVisibility(value: string) {
  if (value === "PUBLISHED") return "Publicado";
  if (value === "ARCHIVED") return "Arquivado";
  return "Rascunho";
}

function presentStatus(value: string) {
  if (value === "RELEASING") return "Em lançamento";
  if (value === "FINISHED") return "Finalizado";
  if (value === "HIATUS") return "Em hiato";
  return "Não lançado";
}

function toFormValues(anime: AdminAnimeDto): AnimeFormValues {
  return {
    id: anime.id,
    anilistId: anime.anilistId,
    slug: anime.slug,
    titleDisplay: anime.titleDisplay,
    titleRomaji: anime.titleRomaji,
    titleNative: anime.titleNative,
    titleEnglish: anime.titleEnglish,
    synopsis: anime.synopsis,
    type: anime.type,
    status: anime.status,
    visibility: anime.visibility,
    seasonLabel: anime.seasonLabel,
    year: anime.year,
    coverUrl: anime.coverUrl,
    bannerUrl: anime.bannerUrl,
    studio: anime.studio,
    genres: anime.genres,
  };
}

function AdminAnimes() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("todos");
  const [visibilityFilter, setVisibilityFilter] = useState<string>("todos");
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<AnimeFormValues | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<AdminAnimeDto | null>(null);

  const animesQuery = useQuery({
    queryKey: ["admin-animes"],
    queryFn: fetchAdminAnimes,
  });

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ["admin-animes"] });
    await queryClient.invalidateQueries({ queryKey: ["admin-home"] });
    await queryClient.invalidateQueries({ queryKey: ["public-home"] });
  };

  const saveMutation = useMutation({
    mutationFn: async (payload: AnimeFormValues) => {
      if (payload.id) {
        return updateAdminAnime(payload.id, payload);
      }
      return createAdminAnime(payload);
    },
    onSuccess: async (_, payload) => {
      await invalidate();
      toast.success(payload.id ? "Anime atualizado." : "Anime criado.");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const visibilityMutation = useMutation({
    mutationFn: ({ id, visibility }: { id: string; visibility: string }) =>
      updateAdminAnimeVisibility(id, visibility),
    onSuccess: async (_, variables) => {
      await invalidate();
      toast.success(variables.visibility === "ARCHIVED" ? "Anime arquivado." : "Anime restaurado.");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const deleteMutation = useMutation({
    mutationFn: deleteAdminAnime,
    onSuccess: async () => {
      await invalidate();
      toast.success("Anime excluído.");
      setConfirmDelete(null);
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const filtered = useMemo(() => {
    return (animesQuery.data ?? []).filter((anime) => {
      const matchesQuery = anime.titleDisplay.toLowerCase().includes(query.toLowerCase());
      const matchesStatus = statusFilter === "todos" || anime.status === statusFilter;
      const matchesVisibility = visibilityFilter === "todos" || anime.visibility === visibilityFilter;
      return matchesQuery && matchesStatus && matchesVisibility;
    });
  }, [animesQuery.data, query, statusFilter, visibilityFilter]);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-serif text-3xl text-ivory">Animes</h1>
          <p className="mt-1 text-sm text-ivory-muted">
            Catálogo principal persistido no backend.
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
          Novo anime
        </Button>
      </div>

      <NewAnimeDialog
        open={dialogOpen}
        onOpenChange={(open) => {
          setDialogOpen(open);
          if (!open) setEditing(null);
        }}
        editing={editing}
        onSubmit={async (payload) => {
          await saveMutation.mutateAsync(payload);
        }}
      />

      <AlertDialog open={Boolean(confirmDelete)} onOpenChange={(open) => !open && setConfirmDelete(null)}>
        <AlertDialogContent className="border-gold/15 bg-surface/95 text-ivory backdrop-blur-xl">
          <AlertDialogHeader>
            <AlertDialogTitle className="font-serif text-xl text-ivory">Excluir anime?</AlertDialogTitle>
            <AlertDialogDescription className="text-ivory-muted">
              Esta ação remove “{confirmDelete?.titleDisplay}” e os episódios vinculados.
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
              placeholder="Buscar por título…"
              className="h-10 border-border-subtle bg-surface-elevated/50 pl-10 text-sm text-ivory placeholder:text-ivory-muted/70"
            />
          </div>
          <Select value={statusFilter} onValueChange={setStatusFilter}>
            <SelectTrigger className="h-10 w-[180px] border-border-subtle bg-surface-elevated/50 text-sm text-ivory">
              <SelectValue placeholder="Status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="todos">Todos os status</SelectItem>
              <SelectItem value="RELEASING">Em lançamento</SelectItem>
              <SelectItem value="FINISHED">Finalizado</SelectItem>
              <SelectItem value="HIATUS">Em hiato</SelectItem>
            </SelectContent>
          </Select>
          <Select value={visibilityFilter} onValueChange={setVisibilityFilter}>
            <SelectTrigger className="h-10 w-[180px] border-border-subtle bg-surface-elevated/50 text-sm text-ivory">
              <SelectValue placeholder="Visibilidade" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="todos">Toda a visibilidade</SelectItem>
              <SelectItem value="PUBLISHED">Publicado</SelectItem>
              <SelectItem value="DRAFT">Rascunho</SelectItem>
              <SelectItem value="ARCHIVED">Arquivado</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border-subtle text-[11px] uppercase tracking-[0.12em] text-ivory-muted">
                <th className="px-5 py-3 text-left font-medium">Título</th>
                <th className="px-3 py-3 text-left font-medium">Tipo</th>
                <th className="px-3 py-3 text-left font-medium">Temporada</th>
                <th className="px-3 py-3 text-left font-medium">Status</th>
                <th className="px-3 py-3 text-right font-medium">Episódios</th>
                <th className="px-3 py-3 text-left font-medium">Visibilidade</th>
                <th className="px-3 py-3 text-left font-medium">Atualizado</th>
                <th className="px-5 py-3 text-right font-medium">Ações</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((anime) => (
                <tr key={anime.id} className="border-b border-border-subtle/60 transition-colors last:border-0 hover:bg-surface-elevated/40">
                  <td className="px-5 py-3">
                    <div className="flex items-center gap-3">
                      {anime.coverUrl ? (
                        <img src={anime.coverUrl} alt="" className="size-11 rounded-md object-cover" loading="lazy" />
                      ) : (
                        <div className="size-11 rounded-md bg-surface-elevated" />
                      )}
                      <div className="min-w-0">
                        <p className="truncate font-medium text-ivory">{anime.titleDisplay}</p>
                        <p className="truncate text-[11px] text-ivory-muted">/{anime.slug}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-3 py-3 text-ivory-muted">{anime.type}</td>
                  <td className="px-3 py-3 text-ivory-muted">{anime.seasonLabel ?? "—"}</td>
                  <td className="px-3 py-3">
                    <StatusPill label={presentStatus(anime.status)} tone={statusTone(anime.status)} />
                  </td>
                  <td className="px-3 py-3 text-right text-ivory">{anime.episodesCount}</td>
                  <td className="px-3 py-3">
                    <StatusPill label={presentVisibility(anime.visibility)} tone={visibilityTone(anime.visibility)} />
                  </td>
                  <td className="px-3 py-3 text-[12px] text-ivory-muted">
                    {anime.updatedAt ? new Date(anime.updatedAt).toLocaleString("pt-BR") : "—"}
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
                          onClick={() => navigate(`/anime/${anime.slug}`)}
                          className="text-ivory focus:bg-surface-elevated focus:text-gold"
                        >
                          <Eye className="size-4" /> Visualizar
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          onClick={() => {
                            setEditing(toFormValues(anime));
                            setDialogOpen(true);
                          }}
                          className="text-ivory focus:bg-surface-elevated focus:text-gold"
                        >
                          <Pencil className="size-4" /> Editar
                        </DropdownMenuItem>
                        {anime.visibility === "ARCHIVED" ? (
                          <DropdownMenuItem
                            onClick={() => visibilityMutation.mutate({ id: anime.id, visibility: "DRAFT" })}
                            className="text-ivory focus:bg-surface-elevated focus:text-gold"
                          >
                            <ArchiveRestore className="size-4" /> Desarquivar
                          </DropdownMenuItem>
                        ) : (
                          <DropdownMenuItem
                            onClick={() => visibilityMutation.mutate({ id: anime.id, visibility: "ARCHIVED" })}
                            className="text-ivory focus:bg-surface-elevated focus:text-gold"
                          >
                            <Archive className="size-4" /> Arquivar
                          </DropdownMenuItem>
                        )}
                        <DropdownMenuSeparator className="bg-border-subtle" />
                        <DropdownMenuItem
                          onClick={() => setConfirmDelete(anime)}
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

        <div className="flex items-center justify-between border-t border-border-subtle px-5 py-3 text-[12px] text-ivory-muted">
          <span>{filtered.length} de {animesQuery.data?.length ?? 0} animes</span>
          <span>Página 1</span>
        </div>
      </AdminCard>
    </div>
  );
}

export default AdminAnimes;
