import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Bell, Plus, Search, ChevronDown } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import avatarDefault from "@/assets/profile-avatar-default.png";
import { useAuth } from "@/hooks/use-auth";
import {
  createAdminAnime,
  createAdminEpisode,
  fetchAdminAnimes,
} from "@/lib/backend-api";
import { NewAnimeDialog, type AnimeFormValues } from "@/components/admin/NewAnimeDialog";
import { NewEpisodeDialog, type EpisodeFormValues } from "@/components/admin/NewEpisodeDialog";

export function AdminTopbar() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [animeOpen, setAnimeOpen] = useState(false);
  const [episodeOpen, setEpisodeOpen] = useState(false);

  const animesQuery = useQuery({
    queryKey: ["admin-animes"],
    queryFn: fetchAdminAnimes,
  });

  const invalidateCatalog = async () => {
    await queryClient.invalidateQueries({ queryKey: ["admin-animes"] });
    await queryClient.invalidateQueries({ queryKey: ["admin-episodes"] });
    await queryClient.invalidateQueries({ queryKey: ["admin-home"] });
    await queryClient.invalidateQueries({ queryKey: ["public-home"] });
  };

  const createAnimeMutation = useMutation({
    mutationFn: (payload: AnimeFormValues) => createAdminAnime(payload),
    onSuccess: async () => {
      await invalidateCatalog();
      setAnimeOpen(false);
      toast.success("Anime criado.");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const createEpisodeMutation = useMutation({
    mutationFn: (payload: EpisodeFormValues) => createAdminEpisode(payload),
    onSuccess: async () => {
      await invalidateCatalog();
      setEpisodeOpen(false);
      toast.success("Episódio criado.");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  return (
    <header className="sticky top-0 z-30 flex h-20 w-full items-center gap-4 border-b border-gold/10 bg-onyx/85 px-6 backdrop-blur-xl">
      {/* Busca global */}
      <div className="relative max-w-xl flex-1">
        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-ivory-muted" />
        <Input
          type="search"
          placeholder="Buscar animes, episódios, usuários…"
          className="h-11 border-border-subtle bg-surface-elevated/50 pl-10 text-sm text-ivory placeholder:text-ivory-muted/70 focus-visible:border-gold/40 focus-visible:bg-surface-elevated focus-visible:ring-2 focus-visible:ring-gold/30"
        />
      </div>

      {/* Ações */}
      <div className="ml-auto flex items-center gap-2">
        <Button
          variant="outline"
          onClick={() => setAnimeOpen(true)}
          className="h-11 gap-2 border-gold/30 bg-gold/5 px-4 text-sm font-medium text-gold transition-all hover:border-gold/60 hover:bg-gold/10 hover:text-gold"
        >
          <Plus className="size-4" />
          <span className="hidden md:inline">Novo anime</span>
        </Button>
        <Button
          variant="outline"
          onClick={() => setEpisodeOpen(true)}
          className="h-11 gap-2 border-gold/30 bg-gold/5 px-4 text-sm font-medium text-gold transition-all hover:border-gold/60 hover:bg-gold/10 hover:text-gold"
        >
          <Plus className="size-4" />
          <span className="hidden md:inline">Novo episódio</span>
        </Button>

        <Button
          variant="ghost"
          size="icon"
          className="relative h-11 w-11 text-ivory hover:bg-surface-elevated hover:text-gold"
          aria-label="Notificações"
        >
          <Bell className="size-5" />
          <span className="absolute right-2 top-2 flex size-4 items-center justify-center rounded-full bg-gold text-[10px] font-semibold leading-none text-onyx ring-2 ring-onyx">
            7
          </span>
        </Button>

        <button
          type="button"
          className="ml-1 flex items-center gap-3 rounded-md border border-border-subtle bg-surface-elevated/40 px-2 py-1.5 text-left transition-all hover:border-gold/30 hover:bg-surface-elevated"
        >
          <Avatar className="size-9 border border-gold/30 bg-onyx">
            <AvatarImage src={avatarDefault} alt={user?.name ?? "Admin"} className="object-cover" />
            <AvatarFallback className="bg-onyx font-serif text-sm text-gold">
              {user?.initial ?? "A"}
            </AvatarFallback>
          </Avatar>
          <div className="hidden flex-col leading-tight md:flex">
            <span className="text-sm font-medium text-ivory">{user?.name ?? "Administrador"}</span>
            <span className="text-[11px] text-ivory-muted">{user?.email ?? "admin@nekoflow.com"}</span>
          </div>
          <ChevronDown className="hidden size-4 text-ivory-muted md:block" />
        </button>
      </div>

      <NewAnimeDialog
        open={animeOpen}
        onOpenChange={setAnimeOpen}
        onSubmit={async (payload) => {
          await createAnimeMutation.mutateAsync(payload);
        }}
      />
      <NewEpisodeDialog
        open={episodeOpen}
        onOpenChange={setEpisodeOpen}
        availableAnimes={(animesQuery.data ?? []).map((anime) => ({
          id: anime.id,
          title: anime.titleDisplay,
          seasonLabel: anime.seasonLabel,
        }))}
        onSubmit={async (payload) => {
          await createEpisodeMutation.mutateAsync(payload);
        }}
      />
    </header>
  );
}
