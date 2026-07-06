import { useEffect, useState } from "react";
import { CheckCircle2, Loader2, RefreshCw, Send, Terminal, XCircle } from "lucide-react";
import { StatCard } from "@/components/admin/WorkerStatCard";
import { Button } from "@/components/ui/button";
import { workerApi } from "@/lib/worker-api";
import { cn } from "@/lib/utils";

interface LogRow {
  id: string;
  release_queue_id: string | null;
  endpoint: string;
  response_status: number | null;
  response_body: string | null;
  error: string | null;
  attempt: number;
  created_at: string;
  release?: { parsed_title: string | null; parsed_episode: number | null } | null;
}

const PAGE_SIZE = 50;

function fmtTime(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "medium" });
}

function statusKind(row: LogRow): "ok" | "fail" {
  if (row.error) return "fail";
  if (row.response_status && row.response_status >= 200 && row.response_status < 300) return "ok";
  return "fail";
}

export default function AdminWorkerLogs() {
  const [rows, setRows] = useState<LogRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [counts, setCounts] = useState({ total: 0, ok: 0, fail: 0 });
  const [expanded, setExpanded] = useState<string | null>(null);

  async function load() {
    setLoading(true);
    try {
      const data = await workerApi.logs();
      const items = ((data.items ?? []) as Array<LogRow & { parsed_title?: string | null; parsed_episode?: number | null }>).map((row) => ({
        ...row,
        release: {
          parsed_title: row.parsed_title ?? null,
          parsed_episode: row.parsed_episode ?? null,
        },
      }));
      setRows(items);
      setCounts({
        total: Number(data.total ?? 0),
        ok: Number(data.ok ?? 0),
        fail: Number(data.fail ?? 0),
      });
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  return (
    <div className="max-w-[1400px]">
      <div className="flex items-start justify-between mb-7">
        <div>
          <div className="mono text-xs text-muted-foreground uppercase tracking-widest mb-1.5">worker · webhooks</div>
          <h1 className="text-2xl font-semibold tracking-tight">Webhook logs</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Tentativas de publicação no Nekoflow — últimas {PAGE_SIZE} entradas.
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={load} disabled={loading}>
          {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <RefreshCw className="w-4 h-4" />}
          Atualizar
        </Button>
      </div>

      <div className="grid grid-cols-3 gap-3 mb-6">
        <StatCard label="Total tentativas" value={counts.total} icon={<Terminal className="w-4 h-4" />} />
        <StatCard label="Sucesso (2xx)" value={counts.ok} icon={<CheckCircle2 className="w-4 h-4" />} tone="success" />
        <StatCard label="Falhas" value={counts.fail} icon={<XCircle className="w-4 h-4" />} tone="destructive" />
      </div>

      <div className="border border-border rounded-lg bg-card overflow-hidden">
        <div className="grid grid-cols-[160px_1fr_80px_70px_120px_80px] gap-3 px-4 py-2.5 border-b border-border bg-muted/30 mono text-[10px] uppercase tracking-widest text-muted-foreground">
          <div>quando</div>
          <div>release / endpoint</div>
          <div>status</div>
          <div>tent.</div>
          <div>resultado</div>
          <div className="text-right">detalhes</div>
        </div>

        {loading ? (
          <div className="py-16 text-center text-muted-foreground text-sm">
            <Loader2 className="w-5 h-5 animate-spin inline mr-2" /> carregando…
          </div>
        ) : rows.length === 0 ? (
          <div className="py-16 text-center text-muted-foreground text-sm">
            <Send className="w-6 h-6 inline mr-2 opacity-50" />
            Nenhuma tentativa de webhook ainda. Publique um item da fila.
          </div>
        ) : (
          rows.map((r) => {
            const kind = statusKind(r);
            const isOpen = expanded === r.id;
            return (
              <div key={r.id} className="border-b border-border last:border-0">
                <div className="grid grid-cols-[160px_1fr_80px_70px_120px_80px] gap-3 px-4 py-2.5 items-center hover:bg-muted/20">
                  <div className="mono text-[11px] text-muted-foreground">{fmtTime(r.created_at)}</div>
                  <div className="min-w-0">
                    <div className="text-sm truncate">
                      {r.release?.parsed_title ?? <span className="text-muted-foreground italic">release removida</span>}
                      {r.release?.parsed_episode != null && (
                        <span className="mono text-[11px] text-muted-foreground ml-2">ep{String(r.release.parsed_episode).padStart(2, "0")}</span>
                      )}
                    </div>
                    <div className="mono text-[10px] text-muted-foreground truncate">{r.endpoint}</div>
                  </div>
                  <div className="mono text-xs">
                    {r.response_status ?? <span className="text-muted-foreground">—</span>}
                  </div>
                  <div className="mono text-xs text-muted-foreground">#{r.attempt}</div>
                  <div>
                    <span
                      className={cn(
                        "inline-flex items-center gap-1.5 px-2 py-1 rounded border mono text-[10px] uppercase tracking-wider",
                        kind === "ok"
                          ? "text-success bg-success/10 border-success/30"
                          : "text-destructive bg-destructive/10 border-destructive/30",
                      )}
                    >
                      {kind === "ok" ? <CheckCircle2 className="w-3 h-3" /> : <XCircle className="w-3 h-3" />}
                      {kind === "ok" ? "ok" : "fail"}
                    </span>
                  </div>
                  <div className="text-right">
                    <Button size="sm" variant="ghost" onClick={() => setExpanded(isOpen ? null : r.id)}>
                      {isOpen ? "fechar" : "abrir"}
                    </Button>
                  </div>
                </div>
                {isOpen && (
                  <div className="px-4 py-3 bg-muted/30 border-t border-border mono text-[11px] space-y-2">
                    {r.error && (
                      <div>
                        <div className="text-muted-foreground uppercase tracking-widest text-[9px] mb-1">error</div>
                        <pre className="text-destructive whitespace-pre-wrap break-all">{r.error}</pre>
                      </div>
                    )}
                    {r.response_body && (
                      <div>
                        <div className="text-muted-foreground uppercase tracking-widest text-[9px] mb-1">response body</div>
                        <pre className="text-foreground/80 whitespace-pre-wrap break-all max-h-64 overflow-auto">{r.response_body}</pre>
                      </div>
                    )}
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
