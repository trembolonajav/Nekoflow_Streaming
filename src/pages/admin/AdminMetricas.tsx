import { useQuery } from "@tanstack/react-query";
import { BarChart3, ExternalLink, Heart, Loader2, PlayCircle, Users } from "lucide-react";

import { AdminCard } from "@/components/admin/AdminCard";
import { Button } from "@/components/ui/button";
import { fetchAdminMetrics } from "@/lib/backend-api";
import { cn } from "@/lib/utils";

const UMAMI_SRC = import.meta.env.VITE_UMAMI_SRC as string | undefined;
const UMAMI_SHARE_URL = import.meta.env.VITE_UMAMI_SHARE_URL as string | undefined;
// O dashboard do Umami mora na raiz do mesmo host do script.js.
const UMAMI_DASHBOARD = UMAMI_SRC ? UMAMI_SRC.replace(/\/script\.js$/, "") : null;

function StatTile({ label, value, icon }: { label: string; value: number; icon?: React.ReactNode }) {
  return (
    <AdminCard className="p-4">
      <div className="flex items-center gap-2 text-ivory-muted">
        {icon}
        <p className="text-[10px] uppercase tracking-[0.2em]">{label}</p>
      </div>
      <p className="mt-2 font-serif text-3xl text-ivory">{value.toLocaleString("pt-BR")}</p>
    </AdminCard>
  );
}

function MiniBars({ data, tone }: { data: { day: string; total: number }[]; tone: "gold" | "success" }) {
  const max = Math.max(1, ...data.map((d) => d.total));
  return (
    <div className="flex h-28 items-end gap-1.5">
      {data.map((d) => (
        <div key={d.day} className="group relative flex-1">
          <div
            className={cn(
              "w-full rounded-t transition-colors",
              tone === "gold" ? "bg-gold/60 group-hover:bg-gold" : "bg-emerald-500/50 group-hover:bg-emerald-400",
            )}
            style={{ height: `${Math.max(3, (d.total / max) * 100)}%` }}
          />
          <div className="pointer-events-none absolute bottom-full left-1/2 z-10 mb-1 hidden -translate-x-1/2 whitespace-nowrap rounded bg-onyx px-2 py-1 font-mono text-[10px] text-ivory group-hover:block">
            {d.day.slice(5)} · {d.total}
          </div>
        </div>
      ))}
    </div>
  );
}

function AdminMetricas() {
  const metricsQuery = useQuery({ queryKey: ["admin-metrics"], queryFn: fetchAdminMetrics });
  const data = metricsQuery.data;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-serif text-3xl text-ivory">Métricas</h1>
          <p className="mt-1 text-sm text-ivory-muted">
            Audiência do catálogo (dados do próprio banco). Tráfego anônimo, origens e tempo de visita ficam no
            Umami.
          </p>
        </div>
        {UMAMI_DASHBOARD ? (
          <Button
            asChild
            variant="outline"
            className="h-11 gap-2 border-border-subtle bg-transparent px-5 text-sm text-ivory hover:bg-surface-elevated"
          >
            <a href={UMAMI_DASHBOARD} target="_blank" rel="noreferrer">
              <ExternalLink className="size-4" />
              Abrir Umami (tráfego)
            </a>
          </Button>
        ) : null}
      </div>

      {metricsQuery.isLoading ? (
        <div className="py-24 text-center text-ivory-muted">
          <Loader2 className="mr-2 inline size-5 animate-spin" /> carregando métricas…
        </div>
      ) : !data ? (
        <div className="py-24 text-center text-ivory-muted">Não foi possível carregar as métricas.</div>
      ) : (
        <>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4 xl:grid-cols-7">
            <StatTile label="Usuários" value={data.totals.users} icon={<Users className="size-4" />} />
            <StatTile label="Animes" value={data.totals.animes} />
            <StatTile label="Eps publicados" value={data.totals.episodes_published} />
            <StatTile label="Plays 7d" value={data.totals.plays_7d} icon={<PlayCircle className="size-4" />} />
            <StatTile label="Plays 30d" value={data.totals.plays_30d} />
            <StatTile label="Na lista" value={data.totals.watchlist_entries} icon={<Heart className="size-4" />} />
            <StatTile label="Comentários" value={data.totals.comments} />
          </div>

          <div className="grid gap-4 lg:grid-cols-2">
            <AdminCard className="p-5">
              <h2 className="font-serif text-xl text-ivory">Plays por dia</h2>
              <p className="mb-4 text-xs text-ivory-muted">Episódios assistidos por usuários logados · últimos 14 dias</p>
              <MiniBars data={data.plays_by_day} tone="gold" />
            </AdminCard>
            <AdminCard className="p-5">
              <h2 className="font-serif text-xl text-ivory">Novos usuários por dia</h2>
              <p className="mb-4 text-xs text-ivory-muted">Cadastros · últimos 14 dias</p>
              <MiniBars data={data.new_users_by_day} tone="success" />
            </AdminCard>
          </div>

          <div className="grid gap-4 xl:grid-cols-3">
            <AdminCard className="overflow-hidden">
              <div className="border-b border-border-subtle px-5 py-4">
                <h2 className="font-serif text-xl text-ivory">Animes mais assistidos</h2>
                <p className="text-xs text-ivory-muted">últimos 7 dias</p>
              </div>
              <ul className="divide-y divide-border-subtle">
                {data.top_animes_7d.length === 0 ? (
                  <li className="px-5 py-8 text-center text-sm text-ivory-muted">Sem plays nos últimos 7 dias.</li>
                ) : (
                  data.top_animes_7d.map((anime, index) => (
                    <li key={anime.slug} className="flex items-center gap-3 px-5 py-3">
                      <span className="font-mono text-[10px] text-gold">{String(index + 1).padStart(2, "0")}</span>
                      {anime.cover_url ? (
                        <img src={anime.cover_url} alt="" className="h-12 w-9 rounded object-cover" loading="lazy" />
                      ) : (
                        <div className="h-12 w-9 rounded bg-surface-elevated" />
                      )}
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm text-ivory">{anime.title}</p>
                        <p className="text-xs text-ivory-muted">
                          {anime.plays} plays · {anime.viewers} pessoa{anime.viewers === 1 ? "" : "s"}
                        </p>
                      </div>
                    </li>
                  ))
                )}
              </ul>
            </AdminCard>

            <AdminCard className="overflow-hidden">
              <div className="border-b border-border-subtle px-5 py-4">
                <h2 className="font-serif text-xl text-ivory">Episódios mais vistos</h2>
                <p className="text-xs text-ivory-muted">últimos 7 dias</p>
              </div>
              <ul className="divide-y divide-border-subtle">
                {data.top_episodes_7d.length === 0 ? (
                  <li className="px-5 py-8 text-center text-sm text-ivory-muted">Sem plays nos últimos 7 dias.</li>
                ) : (
                  data.top_episodes_7d.map((ep, index) => (
                    <li key={`${ep.title}-${ep.number}`} className="flex items-center gap-3 px-5 py-3">
                      <span className="font-mono text-[10px] text-gold">{String(index + 1).padStart(2, "0")}</span>
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm text-ivory">{ep.title}</p>
                        <p className="text-xs text-ivory-muted">Episódio {ep.number}</p>
                      </div>
                      <span className="font-mono text-xs text-gold">{ep.plays}</span>
                    </li>
                  ))
                )}
              </ul>
            </AdminCard>

            <AdminCard className="overflow-hidden">
              <div className="border-b border-border-subtle px-5 py-4">
                <h2 className="font-serif text-xl text-ivory">Mais favoritados</h2>
                <p className="text-xs text-ivory-muted">Minha Lista · geral</p>
              </div>
              <ul className="divide-y divide-border-subtle">
                {data.most_favorited.length === 0 ? (
                  <li className="px-5 py-8 text-center text-sm text-ivory-muted">Nenhum favorito ainda.</li>
                ) : (
                  data.most_favorited.map((anime, index) => (
                    <li key={anime.slug} className="flex items-center gap-3 px-5 py-3">
                      <span className="font-mono text-[10px] text-gold">{String(index + 1).padStart(2, "0")}</span>
                      <p className="min-w-0 flex-1 truncate text-sm text-ivory">{anime.title}</p>
                      <span className="inline-flex items-center gap-1 font-mono text-xs text-gold">
                        <Heart className="size-3" /> {anime.favorites}
                      </span>
                    </li>
                  ))
                )}
              </ul>
            </AdminCard>
          </div>

          {UMAMI_SHARE_URL ? (
            <AdminCard className="overflow-hidden">
              <div className="border-b border-border-subtle px-5 py-4">
                <h2 className="flex items-center gap-2 font-serif text-xl text-ivory">
                  <BarChart3 className="size-5 text-gold" /> Tráfego (Umami)
                </h2>
              </div>
              <iframe src={UMAMI_SHARE_URL} title="Umami" className="h-[720px] w-full border-0" />
            </AdminCard>
          ) : null}
        </>
      )}
    </div>
  );
}

export default AdminMetricas;
