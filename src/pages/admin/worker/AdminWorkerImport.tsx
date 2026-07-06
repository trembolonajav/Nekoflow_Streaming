import { useEffect, useMemo, useState } from "react";
import { Layers, Loader2, RefreshCw, Search } from "lucide-react";
import { StatCard } from "@/components/admin/WorkerStatCard";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import { workerApi } from "@/lib/worker-api";
import { toast } from "sonner";

interface SeekRow {
  seek_video_id: string;
  filename: string | null;
  duration_seconds: number | null;
  size_bytes: number | null;
  seek_status: string | null;
  thumbnail_url: string | null;
}

type QueueStatus =
  | "pending"
  | "matched"
  | "needs_review"
  | "anilist_unavailable"
  | "approved"
  | "published"
  | "publish_failed";

interface ImportResult {
  ok: boolean;
  scanned?: number;
  parsed?: number;
  parse_failed?: number;
  matched?: number;
  needs_review?: number;
  anilist_unavailable?: number;
  errors?: number;
  latency_ms?: number;
  error?: string;
  error_details?: string[];
}

const STATUS_LABEL: Record<QueueStatus | "missing", string> = {
  missing: "não importado",
  pending: "pendente",
  matched: "matched",
  needs_review: "review",
  anilist_unavailable: "AL offline",
  approved: "aprovado",
  published: "publicado",
  publish_failed: "falhou",
};

const STATUS_TONE: Record<QueueStatus | "missing", string> = {
  missing: "bg-muted text-muted-foreground",
  pending: "bg-warning/15 text-warning",
  matched: "bg-success/15 text-success",
  needs_review: "bg-warning/15 text-warning",
  anilist_unavailable: "bg-destructive/15 text-destructive",
  approved: "bg-accent/20 text-accent",
  published: "bg-success/15 text-success",
  publish_failed: "bg-destructive/15 text-destructive",
};

export default function AdminWorkerImport() {
  const [seek, setSeek] = useState<SeekRow[]>([]);
  const [queue, setQueue] = useState<Map<string, QueueStatus>>(new Map());
  const [loading, setLoading] = useState(false);
  const [importing, setImporting] = useState(false);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<"all" | "missing" | "imported">("missing");
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [lastResult, setLastResult] = useState<ImportResult | null>(null);

  async function loadAll() {
    setLoading(true);
    try {
      const data = await workerApi.importOptions({ search: "", status: "all" });
      const items = (data.items ?? []) as Array<SeekRow & { queue_status?: QueueStatus | null }>;
      const map = new Map<string, QueueStatus>();
      items.forEach((r) => {
        if (r.seek_video_id && r.queue_status) map.set(r.seek_video_id, r.queue_status);
      });
      setSeek(items);
      setQueue(map);
      setSelected(new Set());
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadAll();
  }, []);

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase();
    return seek.filter((v) => {
      const isImported = queue.has(v.seek_video_id);
      if (statusFilter === "missing" && isImported) return false;
      if (statusFilter === "imported" && !isImported) return false;
      if (term && !(v.filename ?? "").toLowerCase().includes(term)) return false;
      return true;
    });
  }, [seek, queue, search, statusFilter]);

  const totals = useMemo(() => {
    let imported = 0;
    let missing = 0;
    seek.forEach((v) => {
      if (queue.has(v.seek_video_id)) imported++;
      else missing++;
    });
    return { total: seek.length, imported, missing };
  }, [seek, queue]);

  function toggle(id: string) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function selectAllVisible() {
    setSelected(new Set(filtered.map((v) => v.seek_video_id)));
  }

  function selectAllMissing() {
    const missingIds = seek
      .filter((v) => !queue.has(v.seek_video_id))
      .map((v) => v.seek_video_id);
    setSelected(new Set(missingIds));
    setStatusFilter("missing");
    toast.success(`${missingIds.length} faltantes selecionados`);
  }

  function clearSelection() {
    setSelected(new Set());
  }

  async function runImport() {
    if (selected.size === 0) {
      toast.error("Nenhum vídeo selecionado");
      return;
    }
    setImporting(true);
    setLastResult(null);
    try {
      const ids = Array.from(selected);
      const data = (await workerApi.importSelected(ids)) as unknown as ImportResult;
      setLastResult(data ?? { ok: false, error: "sem resposta" });
      if (data?.ok) {
        toast.success(
          `${data.scanned ?? 0} processados · ${data.matched ?? 0} match · ${
            data.needs_review ?? 0
          } review · ${data.anilist_unavailable ?? 0} AL offline`,
        );
        await loadAll();
      } else {
        toast.error(data?.error ?? "Falha no import");
      }
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      setLastResult({ ok: false, error: msg });
      toast.error(msg);
    } finally {
      setImporting(false);
    }
  }

  const allVisibleSelected =
    filtered.length > 0 && filtered.every((v) => selected.has(v.seek_video_id));

  return (
    <>
      <header className="border-b border-border pb-6 flex items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold tracking-tight">Worker · Import em lote</h1>
          <p className="mono text-xs text-muted-foreground mt-1">
            seleciona vídeos do cache do Seek e enfileira em release_queue
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="sm" onClick={loadAll} disabled={loading}>
            <RefreshCw className={`w-4 h-4 mr-2 ${loading ? "animate-spin" : ""}`} />
            recarregar
          </Button>
          <Button onClick={runImport} disabled={importing || selected.size === 0}>
            {importing ? (
              <>
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                Importando…
              </>
            ) : (
              <>
                <Layers className="w-4 h-4 mr-2" />
                Importar {selected.size > 0 ? `(${selected.size})` : ""}
              </>
            )}
          </Button>
        </div>
      </header>

      <div className="py-8 space-y-6">
        <section className="grid grid-cols-1 sm:grid-cols-4 gap-4">
          <StatCard label="Total no cache" value={totals.total} hint="seek_videos" />
          <StatCard label="Já importados" value={totals.imported} tone="success" hint="na release_queue" />
          <StatCard label="Faltantes" value={totals.missing} tone="accent" hint="prontos pra importar" />
          <StatCard
            label="Selecionados"
            value={selected.size}
            tone={selected.size > 0 ? "accent" : "default"}
            hint={lastResult ? `último: ${lastResult.scanned ?? 0} em ${lastResult.latency_ms}ms` : "aguardando ação"}
          />
        </section>

        {lastResult && !lastResult.ok && (
          <div className="panel p-4 border-destructive/40 bg-destructive/10">
            <div className="text-destructive text-sm font-medium mb-1">Erro no import</div>
            <pre className="mono text-xs text-foreground/80 whitespace-pre-wrap break-all">
              {lastResult.error ?? lastResult.error_details?.join("\n")}
            </pre>
          </div>
        )}

        <section className="panel">
          <div className="px-5 py-4 border-b border-border flex flex-wrap items-center gap-3">
            <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
              Vídeos no Seek
            </h2>

            <div className="flex items-center gap-1 ml-2">
              {(["missing", "all", "imported"] as const).map((f) => (
                <button
                  key={f}
                  type="button"
                  onClick={() => setStatusFilter(f)}
                  className={`mono text-[11px] uppercase tracking-widest px-2.5 py-1 rounded transition-colors ${
                    statusFilter === f
                      ? "bg-foreground text-background"
                      : "text-muted-foreground hover:text-foreground hover:bg-surface-2"
                  }`}
                >
                  {f === "missing" ? "faltantes" : f === "imported" ? "importados" : "todos"}
                </button>
              ))}
            </div>

            <div className="ml-auto flex items-center gap-2">
              <div className="relative">
                <Search className="w-3.5 h-3.5 absolute left-2.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
                <Input
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="filtrar por nome…"
                  className="h-8 pl-8 pr-2 w-64 mono text-xs"
                />
              </div>
            </div>
          </div>

          <div className="px-5 py-2 border-b border-border bg-surface-2/40 flex flex-wrap items-center gap-2 mono text-[11px] text-muted-foreground">
            <span>{filtered.length} visíveis</span>
            <span className="text-border">·</span>
            <button
              type="button"
              onClick={selectAllVisible}
              className="hover:text-foreground transition-colors"
            >
              selecionar visíveis
            </button>
            <span className="text-border">·</span>
            <button
              type="button"
              onClick={selectAllMissing}
              className="hover:text-accent transition-colors text-accent/80"
            >
              selecionar todos os faltantes ({totals.missing})
            </button>
            {selected.size > 0 && (
              <>
                <span className="text-border">·</span>
                <button
                  type="button"
                  onClick={clearSelection}
                  className="hover:text-destructive transition-colors"
                >
                  limpar seleção
                </button>
              </>
            )}
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-surface-2 text-muted-foreground mono text-[10px] uppercase tracking-widest">
                <tr>
                  <th className="px-4 py-2 w-10">
                    <Checkbox
                      checked={allVisibleSelected}
                      onCheckedChange={(v) => {
                        if (v) selectAllVisible();
                        else clearSelection();
                      }}
                    />
                  </th>
                  <th className="text-left px-4 py-2 font-medium">Nome do arquivo</th>
                  <th className="text-left px-4 py-2 font-medium w-32">Status fila</th>
                  <th className="text-right px-4 py-2 font-medium w-20">Dur</th>
                </tr>
              </thead>
              <tbody>
                {filtered.length === 0 && !loading && (
                  <tr>
                    <td colSpan={4} className="px-4 py-12 text-center text-muted-foreground text-sm">
                      Nada por aqui. Tente outro filtro ou rode <span className="text-foreground">Sync Seek</span> primeiro.
                    </td>
                  </tr>
                )}
                {filtered.map((v) => {
                  const status = (queue.get(v.seek_video_id) ?? "missing") as QueueStatus | "missing";
                  const isSelected = selected.has(v.seek_video_id);
                  return (
                    <tr
                      key={v.seek_video_id}
                      className={`border-t border-border hover:bg-surface-2/50 cursor-pointer ${
                        isSelected ? "bg-accent/5" : ""
                      }`}
                      onClick={() => toggle(v.seek_video_id)}
                    >
                      <td className="px-4 py-2" onClick={(e) => e.stopPropagation()}>
                        <Checkbox
                          checked={isSelected}
                          onCheckedChange={() => toggle(v.seek_video_id)}
                        />
                      </td>
                      <td className="px-4 py-2 mono text-xs text-foreground/90 truncate max-w-xl">
                        {v.filename ?? "—"}
                      </td>
                      <td className="px-4 py-2">
                        <span
                          className={`mono text-[10px] uppercase tracking-wider px-2 py-0.5 rounded ${STATUS_TONE[status]}`}
                        >
                          {STATUS_LABEL[status]}
                        </span>
                      </td>
                      <td className="px-4 py-2 mono text-xs text-right text-muted-foreground">
                        {fmtDuration(v.duration_seconds)}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
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
