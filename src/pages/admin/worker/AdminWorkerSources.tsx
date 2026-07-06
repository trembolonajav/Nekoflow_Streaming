import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { workerApi } from "@/lib/worker-api";
import { toast } from "@/hooks/use-toast";
import { Plus, RefreshCw, Trash2, Pencil, Radio, AlertCircle, CheckCircle2 } from "lucide-react";
import { formatDistanceToNow } from "date-fns";

interface RssSource {
  id: string;
  name: string;
  url: string;
  release_group_filter: string | null;
  quality_filter: string | null;
  enabled: boolean;
  last_polled_at: string | null;
  last_poll_items: number | null;
  last_poll_error: string | null;
}

interface PollSummary {
  fetched?: number;
  new?: number;
  filtered?: number;
}

interface PollResponse {
  sources?: number;
  summary?: PollSummary[];
}

const empty = {
  id: "",
  name: "",
  url: "",
  release_group_filter: "",
  quality_filter: "",
  enabled: true,
};

export default function AdminWorkerSources() {
  const [sources, setSources] = useState<RssSource[]>([]);
  const [loading, setLoading] = useState(false);
  const [polling, setPolling] = useState<string | null>(null);
  const [pollingAll, setPollingAll] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState(empty);

  async function load() {
    setLoading(true);
    try {
      const data = await workerApi.sources();
      setSources((data as RssSource[]) ?? []);
    } catch (err) {
      toast({ title: "Erro", description: err instanceof Error ? err.message : String(err), variant: "destructive" });
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  function openNew() {
    setForm(empty);
    setDialogOpen(true);
  }

  function openEdit(s: RssSource) {
    setForm({
      id: s.id,
      name: s.name,
      url: s.url,
      release_group_filter: s.release_group_filter ?? "",
      quality_filter: s.quality_filter ?? "",
      enabled: s.enabled,
    });
    setDialogOpen(true);
  }

  async function save() {
    if (!form.name.trim() || !form.url.trim()) {
      toast({ title: "Preenche nome e URL", variant: "destructive" });
      return;
    }
    const payload = {
      name: form.name.trim(),
      url: form.url.trim(),
      release_group_filter: form.release_group_filter.trim() || null,
      quality_filter: form.quality_filter.trim() || null,
      enabled: form.enabled,
    };
    try {
      if (form.id) await workerApi.updateSource(form.id, payload);
      else await workerApi.createSource(payload);
    } catch (err) {
      toast({ title: "Erro ao salvar", description: err instanceof Error ? err.message : String(err), variant: "destructive" });
      return;
    }
    toast({ title: form.id ? "Fonte atualizada" : "Fonte criada" });
    setDialogOpen(false);
    load();
  }

  async function toggle(s: RssSource) {
    try {
      await workerApi.updateSource(s.id, { ...s, enabled: !s.enabled });
      load();
    } catch (err) {
      toast({ title: "Erro", description: err instanceof Error ? err.message : String(err), variant: "destructive" });
    }
  }

  async function remove(s: RssSource) {
    if (!confirm(`Remover "${s.name}"?`)) return;
    try {
      await workerApi.deleteSource(s.id);
      toast({ title: "Removida" });
      load();
    } catch (err) {
      toast({ title: "Erro", description: err instanceof Error ? err.message : String(err), variant: "destructive" });
    }
  }

  async function poll(s: RssSource) {
    setPolling(s.id);
    try {
      const data = (await workerApi.pollSources({ sourceId: s.id })) as PollResponse;
      const summary = data?.summary?.[0];
      toast({
        title: `Poll concluído: ${s.name}`,
        description: `Fetched ${summary?.fetched ?? 0} · novos ${summary?.new ?? 0} · filtrados ${summary?.filtered ?? 0}`,
      });
      load();
    } catch (err) {
      toast({ title: "Erro no poll", description: err instanceof Error ? err.message : String(err), variant: "destructive" });
    } finally {
      setPolling(null);
    }
  }

  async function pollAll() {
    setPollingAll(true);
    try {
      const data = (await workerApi.pollSources({})) as PollResponse;
      const totalNew = (data?.summary ?? []).reduce(
        (acc: number, s: { new?: number }) => acc + (s.new ?? 0),
        0,
      );
      toast({
        title: "Poll geral concluído",
        description: `${data?.sources ?? 0} fontes · ${totalNew} novos itens`,
      });
      load();
    } catch (err) {
      toast({ title: "Erro no poll geral", description: err instanceof Error ? err.message : String(err), variant: "destructive" });
    } finally {
      setPollingAll(false);
    }
  }

  return (
    <>
      <header className="border-b border-border pb-6 flex items-baseline justify-between">
        <div>
          <h1 className="text-xl font-semibold tracking-tight">Worker · Fontes RSS</h1>
          <p className="mono text-xs text-muted-foreground mt-1">
            feeds, filtros e poll manual
          </p>
        </div>
        <div className="flex gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={pollAll}
            disabled={pollingAll || sources.filter((s) => s.enabled).length === 0}
          >
            <RefreshCw className={`w-4 h-4 ${pollingAll ? "animate-spin" : ""}`} />
            poll todas
          </Button>
          <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
            <DialogTrigger asChild>
              <Button size="sm" onClick={openNew}>
                <Plus className="w-4 h-4" />
                nova fonte
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>{form.id ? "Editar fonte" : "Nova fonte RSS"}</DialogTitle>
              </DialogHeader>
              <div className="space-y-4 py-2">
                <div className="space-y-1.5">
                  <Label>Nome</Label>
                  <Input
                    value={form.name}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                    placeholder="Erai-raws 1080p"
                  />
                </div>
                <div className="space-y-1.5">
                  <Label>URL do feed</Label>
                  <Input
                    value={form.url}
                    onChange={(e) => setForm({ ...form, url: e.target.value })}
                    placeholder="https://www.erai-raws.info/rss-1080/?type=magnet"
                  />
                </div>
                <div className="space-y-1.5">
                  <Label>Filtro release group (separado por vírgula, opcional)</Label>
                  <Input
                    value={form.release_group_filter}
                    onChange={(e) =>
                      setForm({ ...form, release_group_filter: e.target.value })
                    }
                    placeholder="erai-raws,subsplease"
                  />
                </div>
                <div className="space-y-1.5">
                  <Label>Filtro quality (separado por vírgula, opcional)</Label>
                  <Input
                    value={form.quality_filter}
                    onChange={(e) => setForm({ ...form, quality_filter: e.target.value })}
                    placeholder="1080p,720p"
                  />
                </div>
                <div className="flex items-center gap-3">
                  <Switch
                    checked={form.enabled}
                    onCheckedChange={(c) => setForm({ ...form, enabled: c })}
                  />
                  <Label>Habilitada</Label>
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setDialogOpen(false)}>
                  Cancelar
                </Button>
                <Button onClick={save}>Salvar</Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>
      </header>

      <div className="py-8 space-y-3">
        {loading && (
          <div className="mono text-xs text-muted-foreground">carregando...</div>
        )}
        {!loading && sources.length === 0 && (
          <div className="panel p-12 grid-bg flex flex-col items-center text-center">
            <Radio className="w-10 h-10 text-muted-foreground mb-4" />
            <div className="mono text-xs uppercase tracking-widest text-muted-foreground">
              nenhuma fonte cadastrada
            </div>
            <div className="text-sm text-foreground/70 mt-2 max-w-md">
              Adiciona feeds RSS de fansubs (Erai-raws, SubsPlease, Nyaa) pra captura
              automática de novos episódios.
            </div>
          </div>
        )}
        {sources.map((s) => (
          <div key={s.id} className="panel p-4 flex items-start gap-4">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2">
                <span className="font-medium">{s.name}</span>
                {s.enabled ? (
                  <span className="mono text-[10px] uppercase tracking-widest text-success">
                    ● on
                  </span>
                ) : (
                  <span className="mono text-[10px] uppercase tracking-widest text-muted-foreground">
                    ○ off
                  </span>
                )}
              </div>
              <div className="mono text-xs text-muted-foreground truncate mt-1">
                {s.url}
              </div>
              <div className="flex flex-wrap gap-3 mt-2 mono text-[11px] text-muted-foreground">
                {s.release_group_filter && (
                  <span>group: {s.release_group_filter}</span>
                )}
                {s.quality_filter && <span>quality: {s.quality_filter}</span>}
                {s.last_polled_at ? (
                  <span className="flex items-center gap-1">
                    {s.last_poll_error ? (
                      <AlertCircle className="w-3 h-3 text-destructive" />
                    ) : (
                      <CheckCircle2 className="w-3 h-3 text-success" />
                    )}
                    último poll{" "}
                    {formatDistanceToNow(new Date(s.last_polled_at), { addSuffix: true })}
                    {s.last_poll_items != null && ` · ${s.last_poll_items} novos`}
                  </span>
                ) : (
                  <span>nunca polled</span>
                )}
              </div>
              {s.last_poll_error && (
                <div className="mono text-[11px] text-destructive mt-1">
                  erro: {s.last_poll_error}
                </div>
              )}
            </div>
            <div className="flex items-center gap-1.5 shrink-0">
              <Switch checked={s.enabled} onCheckedChange={() => toggle(s)} />
              <Button
                variant="outline"
                size="sm"
                onClick={() => poll(s)}
                disabled={polling === s.id || !s.enabled}
              >
                <RefreshCw
                  className={`w-3.5 h-3.5 ${polling === s.id ? "animate-spin" : ""}`}
                />
                poll
              </Button>
              <Button variant="ghost" size="icon" onClick={() => openEdit(s)}>
                <Pencil className="w-4 h-4" />
              </Button>
              <Button variant="ghost" size="icon" onClick={() => remove(s)}>
                <Trash2 className="w-4 h-4 text-destructive" />
              </Button>
            </div>
          </div>
        ))}
      </div>
    </>
  );
}
