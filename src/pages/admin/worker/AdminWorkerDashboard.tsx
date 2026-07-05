import * as React from "react";
import { useEffect, useState } from "react";
import { Activity, CheckCircle2, CloudDownload, Inbox, Layers, Loader2, Radio, XCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { StatCard } from "@/components/admin/WorkerStatCard";
import { workerApi } from "@/lib/worker-api";
import { toast } from "sonner";

interface PingResult {
  ok: boolean;
  latency_ms?: number;
  status?: number;
  user?: { email?: string; username?: string; id?: string | number; [k: string]: unknown };
  error?: string;
  body?: unknown;
}

interface Stats {
  seek_videos: number;
  queue_pending: number;
  queue_published: number;
  queue_error: number;
}

export default function AdminWorkerDashboard() {
  const [pingLoading, setPingLoading] = useState(false);
  const [ping, setPing] = useState<PingResult | null>(null);
  const [stats, setStats] = useState<Stats>({
    seek_videos: 0,
    queue_pending: 0,
    queue_published: 0,
    queue_error: 0,
  });

  async function loadStats() {
    const data = await workerApi.dashboard();
    setStats({
      seek_videos: data.seek_videos ?? 0,
      queue_pending: data.queue_pending ?? 0,
      queue_published: data.queue_published ?? 0,
      queue_error: data.queue_error ?? 0,
    });
  }

  useEffect(() => {
    loadStats();
  }, []);

  async function testSeek() {
    setPingLoading(true);
    setPing(null);
    try {
      const data = (await workerApi.pingSeek()) as unknown as PingResult;
      setPing(data ?? { ok: false, error: "sem resposta" });
      if (data?.ok) {
        toast.success(`Seek conectado em ${data.latency_ms}ms`);
      } else {
        toast.error("Falha ao conectar no Seek");
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      setPing({ ok: false, error: message });
      toast.error(`Erro: ${message}`);
    } finally {
      setPingLoading(false);
    }
  }

  return (
    <>
      <header className="border-b border-border pb-6 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold tracking-tight">Worker · Dashboard</h1>
          <p className="mono text-xs text-muted-foreground mt-1">
            painel interno · automação seek + anilist + admin
          </p>
        </div>
        <div className="flex items-center gap-2 mono text-xs text-muted-foreground">
          <span className="status-dot bg-success" />
          worker online
        </div>
      </header>

      <div className="py-8 space-y-8 max-w-6xl">
        <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard
            label="Seek videos"
            value={stats.seek_videos}
            hint="cacheados localmente"
            icon={<CloudDownload className="w-4 h-4" />}
          />
          <StatCard
            label="Na fila"
            value={stats.queue_pending}
            hint="aguardando aprovação"
            tone="warning"
            icon={<Inbox className="w-4 h-4" />}
          />
          <StatCard
            label="Publicados"
            value={stats.queue_published}
            hint="enviados ao admin"
            tone="success"
            icon={<CheckCircle2 className="w-4 h-4" />}
          />
          <StatCard
            label="Com erro"
            value={stats.queue_error}
            hint="precisa atenção"
            tone={stats.queue_error > 0 ? "destructive" : "default"}
            icon={<XCircle className="w-4 h-4" />}
          />
        </section>

        <section className="panel p-6">
          <div className="flex items-start justify-between gap-4 mb-4">
            <div>
              <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
                Validação de conexão
              </h2>
              <p className="text-sm text-foreground/80 mt-2">
                Testa o token salvo do <span className="mono text-accent">SeekStreaming</span> chamando{" "}
                <span className="mono text-xs px-1.5 py-0.5 rounded bg-surface-3">GET /api/v1/user/information</span>.
                Confirma autenticação antes de avançar pra sync.
              </p>
            </div>
            <Button onClick={testSeek} disabled={pingLoading} size="sm">
              {pingLoading ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  Testando…
                </>
              ) : (
                <>
                  <Activity className="w-4 h-4 mr-2" />
                  Testar conexão Seek
                </>
              )}
            </Button>
          </div>

          {ping && (
            <div className="mt-4 rounded-md border border-border bg-surface-2 p-4">
              <div className="flex items-center gap-3 mb-3">
                {ping.ok ? (
                  <CheckCircle2 className="w-5 h-5 text-success" />
                ) : (
                  <XCircle className="w-5 h-5 text-destructive" />
                )}
                <span className={`font-medium ${ping.ok ? "text-success" : "text-destructive"}`}>
                  {ping.ok ? "Conectado" : "Falhou"}
                </span>
                {ping.latency_ms !== undefined && (
                  <span className="mono text-xs text-muted-foreground ml-auto">
                    {ping.latency_ms}ms · HTTP {ping.status ?? "—"}
                  </span>
                )}
              </div>

              {ping.ok && ping.user ? (
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 mono text-xs">
                  {Object.entries(ping.user)
                    .filter(([, v]) => typeof v !== "object" || v === null)
                    .slice(0, 6)
                    .map(([k, v]) => (
                      <div key={k} className="space-y-0.5">
                        <div className="text-muted-foreground uppercase tracking-wider text-[10px]">
                          {k}
                        </div>
                        <div className="truncate text-foreground">{String(v ?? "—")}</div>
                      </div>
                    ))}
                </div>
              ) : (
                <pre className="mono text-xs text-muted-foreground overflow-x-auto whitespace-pre-wrap break-all">
                  {ping.error ?? JSON.stringify(ping.body, null, 2)}
                </pre>
              )}
            </div>
          )}
        </section>

        <section className="panel p-6">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground mb-4">
            Roadmap
          </h2>
          <ol className="space-y-3 text-sm">
            <RoadmapItem n="0" status="active" title="Validar conexão SeekStreaming" desc="Token + ping endpoint" />
            <RoadmapItem n="1" status="next" title="Sync vídeos existentes do Seek" desc="Puxa os que já estão lá" icon={<CloudDownload className="w-4 h-4" />} />
            <RoadmapItem n="2" status="todo" title="Auto-match com AniList" desc="Parser regex + busca + cache" />
            <RoadmapItem n="3" status="todo" title="Fila + aprovação em massa" desc="Revisar e publicar lote" icon={<Inbox className="w-4 h-4" />} />
            <RoadmapItem n="4" status="todo" title="RSS poll automático" desc="Cron + captura contínua" icon={<Radio className="w-4 h-4" />} />
            <RoadmapItem n="5" status="todo" title="Modo backlog (import seleção)" desc="Marca eps, dispara em lote" icon={<Layers className="w-4 h-4" />} />
          </ol>
        </section>
      </div>
    </>
  );
}

const RoadmapItem = React.forwardRef<
  HTMLLIElement,
  {
    n: string;
    title: string;
    desc: string;
    status: "active" | "next" | "todo" | "done";
    icon?: React.ReactNode;
  }
>(({ n, title, desc, status, icon }, ref) => {
  const tone =
    status === "active"
      ? "border-primary/40 bg-primary/5"
      : status === "next"
      ? "border-accent/30 bg-accent/5"
      : "border-border bg-surface-2";
  const badge =
    status === "active"
      ? "bg-primary text-primary-foreground"
      : status === "next"
      ? "bg-accent text-accent-foreground"
      : "bg-surface-3 text-muted-foreground";
  return (
    <li ref={ref} className={`flex items-start gap-4 rounded-md border px-4 py-3 ${tone}`}>
      <div className={`mono text-xs font-bold w-7 h-7 rounded flex items-center justify-center shrink-0 ${badge}`}>
        {n}
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 font-medium text-foreground">
          {icon}
          {title}
        </div>
        <div className="text-xs text-muted-foreground mt-0.5">{desc}</div>
      </div>
      <span className="mono text-[10px] uppercase tracking-widest text-muted-foreground self-center">
        {status === "active" ? "agora" : status === "next" ? "próximo" : status}
      </span>
    </li>
  );
});
RoadmapItem.displayName = "RoadmapItem";
