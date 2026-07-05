import { useEffect, useState } from "react";
import { CloudDownload, Loader2, RefreshCw, Search } from "lucide-react";
import { StatCard } from "@/components/admin/WorkerStatCard";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { workerApi } from "@/lib/worker-api";
import { toast } from "sonner";

interface SyncResult {
  ok: boolean;
  latency_ms?: number;
  total_in_seek?: number;
  fetched?: number;
  upserted?: number;
  pages_scanned?: number;
  errors?: number;
  error_details?: string[];
  error?: string;
}

interface SeekVideoRow {
  seek_video_id: string;
  filename: string | null;
  duration_seconds: number | null;
  size_bytes: number | null;
  seek_status: string | null;
  thumbnail_url: string | null;
  last_synced_at: string;
}

const PAGE_SIZE = 50;

export default function AdminWorkerSync() {
  const [syncing, setSyncing] = useState(false);
  const [lastResult, setLastResult] = useState<SyncResult | null>(null);
  const [stats, setStats] = useState({ total: 0, active: 0 });
  const [videos, setVideos] = useState<SeekVideoRow[]>([]);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [loadingList, setLoadingList] = useState(false);

  async function loadStats() {
    const data = await workerApi.seekVideos({ search, page, size: PAGE_SIZE });
    setStats({ total: Number(data.total ?? 0), active: Number(data.active ?? 0) });
  }

  async function loadList() {
    setLoadingList(true);
    try {
      const data = await workerApi.seekVideos({ search, page, size: PAGE_SIZE });
      setVideos((data.items ?? []) as SeekVideoRow[]);
      setStats({ total: Number(data.total ?? 0), active: Number(data.active ?? 0) });
    } catch (err) {
      toast.error(err instanceof Error ? err.message : String(err));
    } finally {
      setLoadingList(false);
    }
  }

  useEffect(() => {
    loadStats();
  }, []);

  useEffect(() => {
    loadList();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  async function runSync() {
    setSyncing(true);
    setLastResult(null);
    try {
      const data = (await workerApi.syncSeek()) as unknown as SyncResult;
      setLastResult(data ?? { ok: false, error: "sem resposta" });
      if (data?.ok) {
        toast.success(`${data.upserted ?? 0} vídeos sincronizados em ${data.latency_ms}ms`);
        await Promise.all([loadStats(), loadList()]);
      } else {
        toast.error(data?.error ?? "Falha na sync");
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      setLastResult({ ok: false, error: message });
      toast.error(message);
    } finally {
      setSyncing(false);
    }
  }

  function onSearch(e: React.FormEvent) {
    e.preventDefault();
    setPage(0);
    loadList();
  }

  return (
    <>
      <header className="border-b border-border pb-6 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold tracking-tight">Worker · Sync Seek</h1>
          <p className="mono text-xs text-muted-foreground mt-1">
            puxa todos os vídeos da sua conta no SeekStreaming
          </p>
        </div>
        <Button onClick={runSync} disabled={syncing}>
          {syncing ? (
            <>
              <Loader2 className="w-4 h-4 mr-2 animate-spin" />
              Sincronizando…
            </>
          ) : (
            <>
              <CloudDownload className="w-4 h-4 mr-2" />
              Puxar tudo do Seek
            </>
          )}
        </Button>
      </header>

      <div className="py-8 space-y-6 max-w-6xl">
        <section className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <StatCard label="Cacheados" value={stats.total} hint="seek_videos" />
          <StatCard label="Active no Seek" value={stats.active} tone="success" />
          <StatCard
            label="Última sync"
            value={lastResult ? `${lastResult.upserted ?? 0}` : "—"}
            hint={lastResult ? `${lastResult.latency_ms}ms · ${lastResult.pages_scanned ?? 0} pgs` : "nunca rodou nesta sessão"}
            tone={lastResult?.ok ? "accent" : "default"}
          />
        </section>

        {lastResult && !lastResult.ok && (
          <div className="panel p-4 border-destructive/40 bg-destructive/10">
            <div className="text-destructive text-sm font-medium mb-1">Erro na sincronização</div>
            <pre className="mono text-xs text-foreground/80 whitespace-pre-wrap break-all">
              {lastResult.error ?? lastResult.error_details?.join("\n")}
            </pre>
          </div>
        )}

        <section className="panel">
          <div className="px-5 py-4 border-b border-border flex items-center gap-3">
            <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
              Vídeos no cache
            </h2>
            <form onSubmit={onSearch} className="ml-auto flex items-center gap-2">
              <div className="relative">
                <Search className="w-3.5 h-3.5 absolute left-2.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
                <Input
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="filtrar por nome…"
                  className="h-8 pl-8 pr-2 w-64 mono text-xs"
                />
              </div>
              <Button type="button" size="sm" variant="ghost" onClick={loadList} disabled={loadingList}>
                <RefreshCw className={`w-3.5 h-3.5 ${loadingList ? "animate-spin" : ""}`} />
              </Button>
            </form>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-surface-2 text-muted-foreground mono text-[10px] uppercase tracking-widest">
                <tr>
                  <th className="text-left px-4 py-2 font-medium">ID</th>
                  <th className="text-left px-4 py-2 font-medium">Nome do arquivo</th>
                  <th className="text-right px-4 py-2 font-medium">Dur</th>
                  <th className="text-right px-4 py-2 font-medium">Tamanho</th>
                  <th className="text-left px-4 py-2 font-medium">Status</th>
                </tr>
              </thead>
              <tbody>
                {videos.length === 0 && !loadingList && (
                  <tr>
                    <td colSpan={5} className="px-4 py-12 text-center text-muted-foreground text-sm">
                      Nada aqui ainda. Clique em <span className="text-foreground">Puxar tudo do Seek</span>.
                    </td>
                  </tr>
                )}
                {videos.map((v) => (
                  <tr key={v.seek_video_id} className="border-t border-border hover:bg-surface-2/50">
                    <td className="px-4 py-2 mono text-[11px] text-muted-foreground">{v.seek_video_id}</td>
                    <td className="px-4 py-2 mono text-xs text-foreground/90 truncate max-w-md">{v.filename ?? "—"}</td>
                    <td className="px-4 py-2 mono text-xs text-right text-muted-foreground">{fmtDuration(v.duration_seconds)}</td>
                    <td className="px-4 py-2 mono text-xs text-right text-muted-foreground">{fmtBytes(v.size_bytes)}</td>
                    <td className="px-4 py-2">
                      <span className={`mono text-[10px] uppercase tracking-wider px-2 py-0.5 rounded ${
                        v.seek_status === "Active" ? "bg-success/15 text-success" : "bg-muted text-muted-foreground"
                      }`}>
                        {v.seek_status ?? "—"}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="px-5 py-3 border-t border-border flex items-center justify-between mono text-xs text-muted-foreground">
            <span>página {page + 1}</span>
            <div className="flex gap-2">
              <Button size="sm" variant="ghost" onClick={() => setPage(Math.max(0, page - 1))} disabled={page === 0}>
                ← anterior
              </Button>
              <Button size="sm" variant="ghost" onClick={() => setPage(page + 1)} disabled={videos.length < PAGE_SIZE}>
                próxima →
              </Button>
            </div>
          </div>
        </section>
      </div>
    </>
  );
}

function fmtDuration(s: number | null): string {
  if (!s) return "—";
  const m = Math.floor(s / 60);
  const sec = s % 60;
  return `${m}:${String(sec).padStart(2, "0")}`;
}

function fmtBytes(b: number | null): string {
  if (!b) return "—";
  const mb = b / 1024 / 1024;
  if (mb >= 1024) return `${(mb / 1024).toFixed(2)} GB`;
  return `${mb.toFixed(0)} MB`;
}
