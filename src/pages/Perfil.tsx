import { useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Bookmark,
  Clock,
  LogOut,
  MessageSquare,
  Play,
  Sparkles,
  Trash2,
} from "lucide-react";
import { toast } from "sonner";

import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/hooks/use-auth";
import {
  clearHistory,
  deleteHistoryItem,
  fetchHistory,
  fetchPreferences,
  fetchProfile,
  fetchWatchlist,
  removeFromWatchlist,
  updatePreferences,
} from "@/lib/backend-api";

function ProfilePage() {
  const { isAuthenticated, isReady, signOut } = useAuth();
  const navigate = useNavigate();

  if (isReady && !isAuthenticated) {
    return (
      <div className="flex min-h-screen flex-col bg-onyx">
        <Header />
        <main className="flex flex-1 items-center justify-center px-6 py-24">
          <div className="max-w-md text-center">
            <Sparkles className="mx-auto size-10 text-gold" />
            <h1 className="mt-6 font-serif text-3xl text-ivory">Entre para ver seu perfil</h1>
            <p className="mt-3 text-sm text-ivory-muted">
              Seu progresso, lista e preferências agora ficam persistidos no backend da NekoFlow.
            </p>
            <Button asChild className="mt-8 bg-gold text-onyx hover:bg-gold/90">
              <Link to="/entrar">Entrar agora</Link>
            </Button>
          </div>
        </main>
        <Footer />
      </div>
    );
  }

  const handleSignOut = () => {
    signOut();
    navigate("/");
  };

  return (
    <div className="flex min-h-screen flex-col bg-onyx">
      <Header />
      <main className="flex-1">
        <ProfileContent onSignOut={handleSignOut} />
      </main>
      <Footer />
    </div>
  );
}

function ProfileContent({ onSignOut }: { onSignOut: () => void }) {
  const queryClient = useQueryClient();
  const profileQuery = useQuery({
    queryKey: ["me-profile"],
    queryFn: fetchProfile,
  });
  const watchlistQuery = useQuery({
    queryKey: ["me-watchlist"],
    queryFn: fetchWatchlist,
  });
  const historyQuery = useQuery({
    queryKey: ["me-history"],
    queryFn: fetchHistory,
  });
  const preferencesQuery = useQuery({
    queryKey: ["me-preferences"],
    queryFn: fetchPreferences,
  });

  const savePreferencesMutation = useMutation({
    mutationFn: updatePreferences,
    onSuccess: () => toast.success("Preferências salvas."),
    onError: (error: Error) => toast.error(error.message),
  });

  const removeWatchlistMutation = useMutation({
    mutationFn: removeFromWatchlist,
    onSuccess: () => {
      toast.success("Anime removido da sua lista.");
      void queryClient.invalidateQueries({ queryKey: ["me-watchlist"] });
      void queryClient.invalidateQueries({ queryKey: ["me-profile"] });
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const deleteHistoryMutation = useMutation({
    mutationFn: deleteHistoryItem,
    onSuccess: () => {
      toast.success("Item removido do histórico.");
      void queryClient.invalidateQueries({ queryKey: ["me-history"] });
      void queryClient.invalidateQueries({ queryKey: ["me-profile"] });
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const clearHistoryMutation = useMutation({
    mutationFn: clearHistory,
    onSuccess: () => {
      toast.success("Histórico limpo.");
      void queryClient.invalidateQueries({ queryKey: ["me-history"] });
      void queryClient.invalidateQueries({ queryKey: ["me-profile"] });
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const [localPreferences, setLocalPreferences] = useState<null | Awaited<ReturnType<typeof fetchPreferences>>>(null);
  const preferences = localPreferences ?? preferencesQuery.data ?? null;

  const profile = profileQuery.data;
  const watchlist = watchlistQuery.data ?? [];
  const history = historyQuery.data ?? [];

  const stats = useMemo(() => {
    if (!profile) return [];
    return [
      { icon: Play, label: "Continuando", value: profile.stats.continueWatchingCount },
      { icon: Bookmark, label: "Na lista", value: profile.stats.watchlistCount },
      { icon: Clock, label: "Histórico", value: profile.stats.historyCount },
      { icon: MessageSquare, label: "Comentários", value: profile.stats.commentCount },
    ];
  }, [profile]);

  if (profileQuery.isLoading) {
    return (
      <div className="mx-auto flex max-w-[1400px] items-center justify-center px-6 py-24 text-ivory-muted">
        Carregando perfil...
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="mx-auto flex max-w-[1400px] items-center justify-center px-6 py-24 text-ivory-muted">
        Não foi possível carregar seu perfil.
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-[1400px] flex-col gap-10 px-4 py-8 md:px-8 md:py-10">
      <section className="rounded-2xl border border-border-subtle bg-surface/40 p-6 md:p-8">
        <div className="flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
          <div className="flex items-center gap-4">
            <span className="flex size-20 items-center justify-center rounded-full border border-gold/30 bg-onyx font-serif text-3xl text-gold">
              {profile.name.charAt(0).toUpperCase()}
            </span>
            <div>
              <h1 className="font-serif text-3xl text-ivory md:text-4xl">{profile.name}</h1>
              <p className="mt-1 text-sm text-gold/80">@{profile.email.split("@")[0]}</p>
              <p className="text-xs text-ivory-muted">{profile.email}</p>
            </div>
          </div>
          <Button
            variant="outline"
            onClick={onSignOut}
            className="gap-2 border-border-subtle bg-surface-elevated/30 text-ivory hover:border-destructive/40 hover:bg-destructive/10 hover:text-destructive"
          >
            <LogOut className="size-4" />
            Sair
          </Button>
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-4">
        {stats.map((stat) => {
          const Icon = stat.icon;
          return (
            <article key={stat.label} className="rounded-xl border border-border-subtle bg-surface/40 p-5">
              <div className="flex items-start justify-between">
                <div>
                  <p className="text-[11px] uppercase tracking-[0.16em] text-ivory-muted">{stat.label}</p>
                  <p className="mt-2 font-serif text-4xl text-ivory">{stat.value}</p>
                </div>
                <span className="flex size-10 items-center justify-center rounded-lg border border-gold/20 bg-gold/5 text-gold">
                  <Icon className="size-5" />
                </span>
              </div>
            </article>
          );
        })}
      </section>

      <section className="grid gap-8 lg:grid-cols-[1.4fr_1fr]">
        <div className="space-y-8">
          <Panel title="Continuar assistindo">
            {profile.continueWatching.length === 0 ? (
              <EmptyState label="Você ainda não começou nenhum episódio." />
            ) : (
              <div className="grid gap-4 sm:grid-cols-2">
                {profile.continueWatching.map((item) => (
                  <Link
                    key={item.episodeId}
                    to={`/watch/${item.animeSlug}/${item.episodeNumber}`}
                    className="overflow-hidden rounded-xl border border-border-subtle bg-surface-elevated/30 transition-colors hover:border-gold/30"
                  >
                    <div className="aspect-video overflow-hidden bg-onyx">
                      {item.thumbnailUrl ? (
                        <img src={item.thumbnailUrl} alt="" className="h-full w-full object-cover" />
                      ) : null}
                    </div>
                    <div className="space-y-2 p-4">
                      <p className="line-clamp-1 font-medium text-ivory">{item.animeTitle}</p>
                      <p className="text-xs text-ivory-muted">
                        Ep. {item.episodeNumber} · {item.episodeTitle}
                      </p>
                      <div className="h-1 overflow-hidden rounded-full bg-surface">
                        <span
                          className="block h-full rounded-full bg-gradient-to-r from-gold/60 to-gold"
                          style={{ width: `${item.progressPercent}%` }}
                        />
                      </div>
                      <p className="text-[11px] uppercase tracking-[0.14em] text-ivory-muted">
                        {item.remainingMinutes} min restantes
                      </p>
                    </div>
                  </Link>
                ))}
              </div>
            )}
          </Panel>

          <Panel
            title="Histórico"
            action={
              history.length > 0
                ? {
                    label: clearHistoryMutation.isPending ? "Limpando..." : "Limpar",
                    onClick: () => clearHistoryMutation.mutate(),
                  }
                : undefined
            }
          >
            {history.length === 0 ? (
              <EmptyState label="Seu histórico está vazio." />
            ) : (
              <div className="space-y-3">
                {history.map((item) => (
                  <article
                    key={item.id}
                    className="flex flex-col gap-4 rounded-xl border border-border-subtle bg-surface-elevated/30 p-4 sm:flex-row sm:items-center"
                  >
                    <div className="aspect-video w-full overflow-hidden rounded-lg bg-onyx sm:w-44">
                      {item.thumbnailUrl ? (
                        <img src={item.thumbnailUrl} alt="" className="h-full w-full object-cover" />
                      ) : null}
                    </div>
                    <div className="min-w-0 flex-1">
                      <Link to={`/anime/${item.animeSlug}`} className="truncate font-medium text-ivory hover:text-gold">
                        {item.animeTitle}
                      </Link>
                      <p className="mt-1 text-xs text-ivory-muted">
                        Ep. {item.episodeNumber} · {item.episodeTitle}
                      </p>
                      <p className="mt-2 text-[11px] uppercase tracking-[0.14em] text-ivory-muted">
                        {new Date(item.watchedAt).toLocaleString("pt-BR")}
                      </p>
                    </div>
                    <div className="flex items-center gap-2">
                      <Button asChild size="sm" className="bg-gold text-onyx hover:bg-gold/90">
                        <Link to={`/watch/${item.animeSlug}/${item.episodeNumber}`}>Retomar</Link>
                      </Button>
                      <Button
                        size="icon"
                        variant="ghost"
                        onClick={() => deleteHistoryMutation.mutate(item.id)}
                        className="text-ivory-muted hover:bg-surface hover:text-destructive"
                      >
                        <Trash2 className="size-4" />
                      </Button>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </Panel>
        </div>

        <div className="space-y-8">
          <Panel title="Minha lista">
            {watchlist.length === 0 ? (
              <EmptyState label="Sua lista ainda está vazia." />
            ) : (
              <div className="space-y-3">
                {watchlist.map((item) => (
                  <article key={item.id} className="flex items-center gap-3 rounded-xl border border-border-subtle bg-surface-elevated/30 p-3">
                    <div className="h-20 w-14 overflow-hidden rounded-md bg-onyx">
                      {item.coverUrl ? <img src={item.coverUrl} alt="" className="h-full w-full object-cover" /> : null}
                    </div>
                    <div className="min-w-0 flex-1">
                      <Link to={`/anime/${item.animeSlug}`} className="line-clamp-2 font-medium text-ivory hover:text-gold">
                        {item.title}
                      </Link>
                      <p className="mt-1 text-[11px] uppercase tracking-[0.14em] text-ivory-muted">{item.status}</p>
                    </div>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => removeWatchlistMutation.mutate(item.animeId)}
                      className="text-ivory-muted hover:bg-surface hover:text-destructive"
                    >
                      Remover
                    </Button>
                  </article>
                ))}
              </div>
            )}
          </Panel>

          <Panel title="Preferências">
            {preferences ? (
              <div className="space-y-4">
                <PreferenceRow
                  label="Autoplay"
                  checked={preferences.autoplay}
                  onChange={(checked) => setLocalPreferences({ ...preferences, autoplay: checked })}
                />
                <PreferenceRow
                  label="Próximo episódio automático"
                  checked={preferences.autoNext}
                  onChange={(checked) => setLocalPreferences({ ...preferences, autoNext: checked })}
                />
                <PreferenceRow
                  label="Notificar novos episódios"
                  checked={preferences.notifyNewEpisodes}
                  onChange={(checked) => setLocalPreferences({ ...preferences, notifyNewEpisodes: checked })}
                />
                <PreferenceRow
                  label="Notificar watchlist"
                  checked={preferences.notifyWatchlist}
                  onChange={(checked) => setLocalPreferences({ ...preferences, notifyWatchlist: checked })}
                />
                <Button
                  onClick={() => preferences && savePreferencesMutation.mutate(preferences)}
                  className="w-full bg-gold text-onyx hover:bg-gold/90"
                >
                  Salvar preferências
                </Button>
              </div>
            ) : (
              <EmptyState label="Carregando preferências..." />
            )}
          </Panel>
        </div>
      </section>
    </div>
  );
}

function Panel({
  title,
  children,
  action,
}: {
  title: string;
  children: React.ReactNode;
  action?: { label: string; onClick: () => void };
}) {
  return (
    <section className="rounded-2xl border border-border-subtle bg-surface/40 p-5 md:p-6">
      <div className="mb-5 flex items-center justify-between gap-4">
        <h2 className="font-serif text-2xl text-ivory">{title}</h2>
        {action ? (
          <button
            type="button"
            onClick={action.onClick}
            className="text-[11px] uppercase tracking-[0.18em] text-gold transition-opacity hover:opacity-80"
          >
            {action.label}
          </button>
        ) : null}
      </div>
      {children}
    </section>
  );
}

function PreferenceRow({
  label,
  checked,
  onChange,
}: {
  label: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
}) {
  return (
    <div className="flex items-center justify-between rounded-xl border border-border-subtle bg-surface-elevated/30 px-4 py-3">
      <Label className="text-sm text-ivory">{label}</Label>
      <Switch checked={checked} onCheckedChange={onChange} />
    </div>
  );
}

function EmptyState({ label }: { label: string }) {
  return (
    <div className="rounded-xl border border-dashed border-border-subtle bg-surface/20 py-12 text-center text-sm text-ivory-muted">
      {label}
    </div>
  );
}

export default ProfilePage;
