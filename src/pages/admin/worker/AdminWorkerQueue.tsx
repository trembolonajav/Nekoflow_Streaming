import { useEffect, useMemo, useState } from "react";
import { CheckCircle2, Clock, Inbox, Loader2, RefreshCw, AlertTriangle, WifiOff, Search, Play, Send, XCircle, Pencil } from "lucide-react";
import { StatCard } from "@/components/admin/WorkerStatCard";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { AniListSearchPanel } from "@/components/admin/AniListSearchPanel";
import type { AdminAniListSearchDto } from "@/lib/backend-api";
import { workerApi } from "@/lib/worker-api";
import { toast } from "sonner";
import { cn } from "@/lib/utils";

interface QueueRow {
  id: string;
  seek_video_id: string | null;
  parsed_title: string | null;
  parsed_season: number | null;
  parsed_episode: number | null;
  anilist_id: number | null;
  match_confidence: number | null;
  thumbnail_url: string | null;
  duration_seconds: number | null;
  status: string;
  status_reason: string | null;
  updated_at: string;
}

const STATUS_META: Record<string, { label: string; cls: string; icon: typeof Clock }> = {
  matched: { label: "matched", cls: "text-success bg-success/10 border-success/30", icon: CheckCircle2 },
  needs_review: { label: "review", cls: "text-warning bg-warning/10 border-warning/30", icon: AlertTriangle },
  anilist_unavailable: { label: "anilist off", cls: "text-muted-foreground bg-muted/40 border-border", icon: WifiOff },
  pending: { label: "pending", cls: "text-primary bg-primary/10 border-primary/30", icon: Clock },
  approved: { label: "approved", cls: "text-success bg-success/15 border-success/40", icon: CheckCircle2 },
  published: { label: "published", cls: "text-accent bg-accent/15 border-accent/40", icon: Send },
  publish_failed: { label: "failed", cls: "text-destructive bg-destructive/10 border-destructive/30", icon: XCircle },
};

const FILTERS = [
  { key: "all", label: "Todos" },
  { key: "matched", label: "Matched" },
  { key: "needs_review", label: "Review" },
  { key: "anilist_unavailable", label: "AniList off" },
  { key: "approved", label: "Aprovados" },
  { key: "published", label: "Publicados" },
  { key: "publish_failed", label: "Falhou" },
] as const;

const PAGE_SIZE = 30;

// Linhas que podem ser aprovadas+publicadas em lote (match confiável ou ja aprovadas).
const SELECTABLE = new Set(["matched", "approved", "publish_failed"]);
// Linhas cujo match pode ser corrigido manualmente.
const FIXABLE = new Set(["needs_review", "anilist_unavailable", "publish_failed", "matched", "pending"]);

interface PublishResult {
  ok?: boolean;
  status?: number | string;
  error?: string;
}

interface PublishResponse {
  results?: PublishResult[];
  placeholder?: boolean;
  published?: number;
  total?: number;
}

const GRID = "grid grid-cols-[36px_56px_1fr_110px_70px_120px_180px] gap-3";

function fmtDuration(sec: number | null): string {
  if (!sec) return "—";
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${m}m${String(s).padStart(2, "0")}s`;
}

export default function AdminWorkerQueue() {
  const [rows, setRows] = useState<QueueRow[]>([]);
  const [counts, setCounts] = useState({ total: 0, matched: 0, review: 0, unavailable: 0, approved: 0, published: 0, failed: 0 });
  const [filter, setFilter] = useState<(typeof FILTERS)[number]["key"]>("all");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [running, setRunning] = useState<null | "parse" | "retry" | "publish" | "bulk">(null);
  const [publishingId, setPublishingId] = useState<string | null>(null);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [fixingRow, setFixingRow] = useState<QueueRow | null>(null);
  const [applyingFix, setApplyingFix] = useState(false);

  async function loadCounts() {
    const data = await workerApi.queue({ status: filter, search, page, size: PAGE_SIZE });
    setCounts((data.counts as typeof counts) ?? counts);
  }

  async function loadRows() {
    setLoading(true);
    try {
      const data = await workerApi.queue({ status: filter, search, page, size: PAGE_SIZE });
      setRows((data.items as QueueRow[]) ?? []);
      setCounts((data.counts as typeof counts) ?? counts);
    } catch (err) {
      toast.error("Erro: " + (err instanceof Error ? err.message : String(err)));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadCounts();
  }, []);

  useEffect(() => {
    setSelected(new Set());
    loadRows();
  }, [filter, page]);

  async function runParse(reprocess = false) {
    setRunning(reprocess ? "retry" : "parse");
    try {
      const data = await workerApi.parseQueue({ reprocess });
      toast.success(`Parse: ${data?.scanned ?? 0} escaneados · ${data?.matched ?? 0} matched · ${data?.anilist_unavailable ?? 0} sem AniList`);
    } catch (err) {
      toast.error("Falha ao executar parse: " + (err instanceof Error ? err.message : String(err)));
    } finally {
      setRunning(null);
      loadCounts();
      loadRows();
    }
  }

  async function approve(id: string) {
    try {
      await workerApi.approve(id);
      const data = (await workerApi.publish(id)) as PublishResponse;
      const r = data?.results?.[0];
      if (r?.ok) {
        toast.success("Aprovado e publicado");
      } else {
        toast.error(`Aprovado, mas falhou ao publicar: ${r?.error ?? "erro desconhecido"}`);
      }
      loadCounts();
      loadRows();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : String(err));
    }
  }

  async function publishOne(id: string) {
    setPublishingId(id);
    try {
      const data = (await workerApi.publish(id)) as PublishResponse;
      const r = data?.results?.[0];
      if (r?.ok) {
        toast.success(data?.placeholder ? "Publicado (placeholder httpbin)" : "Publicado");
      } else {
        toast.error(`Falhou: status=${r?.status ?? "n/a"} ${r?.error ?? ""}`);
      }
    } catch (e) {
      toast.error("Erro ao publicar: " + (e instanceof Error ? e.message : String(e)));
    } finally {
      setPublishingId(null);
      loadCounts();
      loadRows();
    }
  }

  async function publishAllApproved() {
    setRunning("publish");
    try {
      const data = (await workerApi.publishMany()) as PublishResponse;
      toast.success(`Publicados: ${data?.published}/${data?.total}${data?.placeholder ? " (placeholder)" : ""}`);
    } catch (e) {
      toast.error("Erro: " + (e instanceof Error ? e.message : String(e)));
    } finally {
      setRunning(null);
      loadCounts();
      loadRows();
    }
  }

  // Aprova + publica todos os selecionados de uma vez.
  async function bulkApprovePublish() {
    const ids = [...selected];
    if (ids.length === 0) return;
    setRunning("bulk");
    try {
      await workerApi.approveMany(ids);
      const data = (await workerApi.publishMany(ids)) as PublishResponse;
      const published = data?.published ?? 0;
      const failed = (data?.total ?? ids.length) - published;
      if (failed > 0) toast.warning(`Publicados ${published}/${ids.length} · ${failed} falharam`);
      else toast.success(`Publicados ${published}/${ids.length}`);
      setSelected(new Set());
    } catch (e) {
      toast.error("Erro no lote: " + (e instanceof Error ? e.message : String(e)));
    } finally {
      setRunning(null);
      loadCounts();
      loadRows();
    }
  }

  // Aplica um match escolhido manualmente no AniList a um item em review.
  async function applyManualMatch(row: QueueRow, pick: AdminAniListSearchDto) {
    setApplyingFix(true);
    try {
      await workerApi.updateQueueItem(row.id, {
        parsed_title: pick.titleRomaji,
        parsed_season: row.parsed_season,
        parsed_episode: row.parsed_episode,
        anilist_id: pick.id,
        matched_anime_id: null,
        match_confidence: 1,
        thumbnail_url: pick.coverImage ?? row.thumbnail_url,
        duration_seconds: row.duration_seconds,
        status: "matched",
        status_reason: null,
      });
      toast.success(`Match corrigido: ${pick.titleRomaji} — agora é só aprovar`);
      setFixingRow(null);
      loadCounts();
      loadRows();
    } catch (e) {
      toast.error("Falha ao corrigir match: " + (e instanceof Error ? e.message : String(e)));
    } finally {
      setApplyingFix(false);
    }
  }

  const filtered = useMemo(() => {
    if (!search.trim()) return rows;
    return rows.filter((r) => (r.parsed_title ?? "").toLowerCase().includes(search.toLowerCase()));
  }, [rows, search]);

  const selectableRows = useMemo(() => filtered.filter((r) => SELECTABLE.has(r.status)), [filtered]);
  const matchedRows = useMemo(() => filtered.filter((r) => r.status === "matched"), [filtered]);
  const allSelectableSelected = selectableRows.length > 0 && selectableRows.every((r) => selected.has(r.id));

  function toggle(id: string) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }
  function selectSet(rowsToSelect: QueueRow[]) {
    setSelected(new Set(rowsToSelect.map((r) => r.id)));
  }

  return (
    <div className="max-w-[1400px]">
      <div className="flex items-start justify-between mb-7">
        <div>
          <div className="mono text-xs text-muted-foreground uppercase tracking-widest mb-1.5">worker · publish</div>
          <h1 className="text-2xl font-semibold tracking-tight">Fila de aprovação</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Aprove os matches do AniList e publique no Nekoflow.
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={() => runParse(true)} disabled={running !== null}>
            {running === "retry" ? <Loader2 className="w-4 h-4 animate-spin" /> : <RefreshCw className="w-4 h-4" />}
            Reprocessar
          </Button>
          <Button variant="outline" size="sm" onClick={() => runParse(false)} disabled={running !== null}>
            {running === "parse" ? <Loader2 className="w-4 h-4 animate-spin" /> : <Play className="w-4 h-4" />}
            Parse
          </Button>
          <Button size="sm" onClick={publishAllApproved} disabled={running !== null || counts.approved === 0}>
            {running === "publish" ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
            Publicar aprovados ({counts.approved})
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 xl:grid-cols-7 gap-3 mb-6">
        <StatCard label="Na fila" value={counts.total} icon={<Inbox className="w-4 h-4" />} />
        <StatCard label="Matched" value={counts.matched} icon={<CheckCircle2 className="w-4 h-4" />} tone="success" />
        <StatCard label="Review" value={counts.review} icon={<AlertTriangle className="w-4 h-4" />} tone="warning" />
        <StatCard label="AniList off" value={counts.unavailable} icon={<WifiOff className="w-4 h-4" />} />
        <StatCard label="Aprovados" value={counts.approved} icon={<CheckCircle2 className="w-4 h-4" />} tone="success" />
        <StatCard label="Publicados" value={counts.published} icon={<Send className="w-4 h-4" />} tone="accent" />
        <StatCard label="Falhou" value={counts.failed} icon={<XCircle className="w-4 h-4" />} tone="destructive" />
      </div>

      <div className="flex items-center gap-2 mb-4 flex-wrap">
        <div className="flex flex-wrap gap-1 bg-card border border-border rounded-md p-1">
          {FILTERS.map((f) => (
            <button
              key={f.key}
              onClick={() => {
                setPage(0);
                setFilter(f.key);
              }}
              className={cn(
                "px-3 py-1.5 text-xs rounded mono uppercase tracking-wider transition-colors",
                filter === f.key
                  ? "bg-primary text-primary-foreground"
                  : "text-muted-foreground hover:text-foreground",
              )}
            >
              {f.label}
            </button>
          ))}
        </div>
        <div className="relative ml-auto w-72">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-muted-foreground" />
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && loadRows()}
            placeholder="Buscar título…"
            className="pl-9 h-9 text-sm bg-card"
          />
        </div>
      </div>

      {/* Barra de seleção em lote */}
      <div className="flex items-center gap-3 mb-3 text-xs mono text-muted-foreground flex-wrap">
        <button className="hover:text-foreground transition-colors" onClick={() => selectSet(matchedRows)} disabled={matchedRows.length === 0}>
          selecionar matched ({matchedRows.length})
        </button>
        <span className="opacity-40">·</span>
        <button className="hover:text-foreground transition-colors" onClick={() => selectSet(selectableRows)} disabled={selectableRows.length === 0}>
          selecionar todos aprovaveis ({selectableRows.length})
        </button>
        <span className="opacity-40">·</span>
        <button className="hover:text-foreground transition-colors" onClick={() => setSelected(new Set())} disabled={selected.size === 0}>
          limpar seleção
        </button>
        {selected.size > 0 && (
          <div className="ml-auto flex items-center gap-2">
            <span className="text-foreground">{selected.size} selecionado(s)</span>
            <Button size="sm" onClick={bulkApprovePublish} disabled={running !== null}>
              {running === "bulk" ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Send className="w-3.5 h-3.5" />}
              Aprovar + publicar ({selected.size})
            </Button>
          </div>
        )}
      </div>

      <div className="border border-border rounded-lg bg-card overflow-hidden">
        <div className={cn(GRID, "px-4 py-2.5 border-b border-border bg-muted/30 mono text-[10px] uppercase tracking-widest text-muted-foreground items-center")}>
          <div>
            <Checkbox
              checked={allSelectableSelected}
              onCheckedChange={(v) => (v ? selectSet(selectableRows) : setSelected(new Set()))}
              aria-label="Selecionar todos"
            />
          </div>
          <div>thumb</div>
          <div>release</div>
          <div>ep</div>
          <div>conf.</div>
          <div>status</div>
          <div className="text-right">ação</div>
        </div>

        {loading ? (
          <div className="py-16 text-center text-muted-foreground text-sm">
            <Loader2 className="w-5 h-5 animate-spin inline mr-2" /> carregando…
          </div>
        ) : filtered.length === 0 ? (
          <div className="py-16 text-center text-muted-foreground text-sm">
            <Inbox className="w-6 h-6 inline mr-2 opacity-50" />
            Nenhum item.
          </div>
        ) : (
          filtered.map((r) => {
            const meta = STATUS_META[r.status] ?? STATUS_META.pending;
            const Icon = meta.icon;
            const canPublish = r.status === "approved" || r.status === "publish_failed";
            const canSelect = SELECTABLE.has(r.status);
            const canFix = FIXABLE.has(r.status);
            return (
              <div
                key={r.id}
                className={cn(GRID, "px-4 py-3 border-b border-border last:border-0 items-center hover:bg-muted/20 transition-colors", selected.has(r.id) && "bg-primary/[0.06]")}
              >
                <div>
                  {canSelect ? (
                    <Checkbox checked={selected.has(r.id)} onCheckedChange={() => toggle(r.id)} aria-label="Selecionar item" />
                  ) : null}
                </div>
                <div className="w-12 h-16 rounded bg-muted overflow-hidden border border-border">
                  {r.thumbnail_url ? (
                    <img src={r.thumbnail_url} alt="" className="w-full h-full object-cover" loading="lazy" />
                  ) : null}
                </div>
                <div className="min-w-0">
                  <div className="text-sm font-medium truncate">{r.parsed_title ?? <span className="text-muted-foreground">(sem parse)</span>}</div>
                  <div className="mono text-[11px] text-muted-foreground truncate">
                    {r.anilist_id ? `anilist:${r.anilist_id} · ` : ""}
                    {r.status_reason ?? "—"}
                  </div>
                </div>
                <div className="mono text-xs text-muted-foreground">
                  {r.parsed_season != null && r.parsed_episode != null
                    ? `S${String(r.parsed_season).padStart(2, "0")}E${String(r.parsed_episode).padStart(2, "0")}`
                    : "—"}
                  <div className="text-[10px]">{fmtDuration(r.duration_seconds)}</div>
                </div>
                <div className="mono text-xs">
                  {r.match_confidence != null ? `${Math.round(r.match_confidence * 100)}%` : "—"}
                </div>
                <div>
                  <span className={cn("inline-flex items-center gap-1.5 px-2 py-1 rounded border mono text-[10px] uppercase tracking-wider", meta.cls)}>
                    <Icon className="w-3 h-3" />
                    {meta.label}
                  </span>
                </div>
                <div className="flex items-center justify-end gap-1.5">
                  {canFix ? (
                    <Button size="sm" variant="ghost" className="h-8 px-2 text-muted-foreground hover:text-foreground" onClick={() => setFixingRow(r)} title="Corrigir match no AniList">
                      <Pencil className="w-3.5 h-3.5" />
                    </Button>
                  ) : null}
                  {r.status === "matched" ? (
                    <Button size="sm" variant="outline" onClick={() => approve(r.id)}>
                      Aprovar + publicar
                    </Button>
                  ) : canPublish ? (
                    <Button size="sm" onClick={() => publishOne(r.id)} disabled={publishingId === r.id}>
                      {publishingId === r.id ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Send className="w-3.5 h-3.5" />}
                      {r.status === "publish_failed" ? "Retry" : "Publicar"}
                    </Button>
                  ) : !canFix ? (
                    <span className="mono text-[10px] text-muted-foreground">—</span>
                  ) : null}
                </div>
              </div>
            );
          })
        )}
      </div>

      <div className="flex items-center justify-between mt-4">
        <div className="mono text-[11px] text-muted-foreground">
          página {page + 1} · {filtered.length} itens
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))}>
            ← anterior
          </Button>
          <Button variant="outline" size="sm" disabled={filtered.length < PAGE_SIZE} onClick={() => setPage((p) => p + 1)}>
            próxima →
          </Button>
        </div>
      </div>

      {/* Modal de correção manual do match (busca AniList ao vivo) */}
      <Dialog open={fixingRow !== null} onOpenChange={(o) => !o && setFixingRow(null)}>
        <DialogContent className="max-h-[90vh] max-w-2xl overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Corrigir match no AniList</DialogTitle>
            <DialogDescription>
              {fixingRow ? (
                <>
                  Item: <span className="text-foreground font-medium">{fixingRow.parsed_title ?? fixingRow.seek_video_id}</span>
                  {fixingRow.parsed_season != null && fixingRow.parsed_episode != null
                    ? ` · S${String(fixingRow.parsed_season).padStart(2, "0")}E${String(fixingRow.parsed_episode).padStart(2, "0")}`
                    : ""}
                  . Busque o anime correto e clique para vincular — o episódio é mantido.
                </>
              ) : null}
            </DialogDescription>
          </DialogHeader>
          {applyingFix ? (
            <div className="py-8 text-center text-sm text-muted-foreground">
              <Loader2 className="w-5 h-5 animate-spin inline mr-2" /> aplicando…
            </div>
          ) : (
            <AniListSearchPanel onPick={(pick) => { if (fixingRow) applyManualMatch(fixingRow, pick); }} />
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
