import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Eye, EyeOff, Flag } from "lucide-react";
import { toast } from "sonner";

import {
  fetchAdminReports,
  updateAdminCommentVisibility,
  updateAdminReportStatus,
} from "@/lib/backend-api";
import { AdminCard, AdminCardHeader, StatusPill } from "@/components/admin/AdminCard";
import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";

function statusTone(status: string) {
  if (status === "PENDING") return "warning" as const;
  if (status === "APPROVED") return "success" as const;
  if (status === "RESOLVED") return "info" as const;
  return "muted" as const;
}

function statusLabel(status: string) {
  if (status === "PENDING") return "Pendente";
  if (status === "APPROVED") return "Aprovado";
  if (status === "RESOLVED") return "Resolvido";
  return "Oculto";
}

function reasonLabel(reason: string) {
  return reason.replaceAll("_", " ").toLowerCase().replace(/^\w/, (char) => char.toUpperCase());
}

function AdminComments() {
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<"todos" | "PENDING" | "RESOLVED" | "HIDDEN">("PENDING");
  const reportsQuery = useQuery({
    queryKey: ["admin-reports"],
    queryFn: fetchAdminReports,
  });

  const updateReportMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) => updateAdminReportStatus(id, status),
    onSuccess: () => {
      toast.success("Report atualizado.");
      void queryClient.invalidateQueries({ queryKey: ["admin-reports"] });
      void queryClient.invalidateQueries({ queryKey: ["admin-dashboard"] });
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const updateVisibilityMutation = useMutation({
    mutationFn: ({ commentId, visibility }: { commentId: string; visibility: string }) =>
      updateAdminCommentVisibility(commentId, visibility),
    onSuccess: () => {
      toast.success("Visibilidade do comentário atualizada.");
      void queryClient.invalidateQueries({ queryKey: ["admin-reports"] });
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const reports = reportsQuery.data ?? [];
  const filtered = reports.filter((report) => (tab === "todos" ? true : report.status === tab));

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-serif text-3xl text-ivory">Comentários e reports</h1>
        <p className="mt-1 text-sm text-ivory-muted">Modere comentários reportados pela comunidade.</p>
      </div>

      <Tabs value={tab} onValueChange={(value) => setTab(value as typeof tab)}>
        <TabsList className="h-11 border border-border-subtle bg-surface/60 p-1">
          <TabsTrigger value="PENDING" className="px-4 text-sm data-[state=active]:bg-gold/10 data-[state=active]:text-gold">
            Pendentes
          </TabsTrigger>
          <TabsTrigger value="RESOLVED" className="px-4 text-sm data-[state=active]:bg-gold/10 data-[state=active]:text-gold">
            Resolvidos
          </TabsTrigger>
          <TabsTrigger value="HIDDEN" className="px-4 text-sm data-[state=active]:bg-gold/10 data-[state=active]:text-gold">
            Ocultos
          </TabsTrigger>
          <TabsTrigger value="todos" className="px-4 text-sm data-[state=active]:bg-gold/10 data-[state=active]:text-gold">
            Todos
          </TabsTrigger>
        </TabsList>
      </Tabs>

      <AdminCard>
        <AdminCardHeader title={`${filtered.length} comentário(s)`} />
        <ul className="divide-y divide-border-subtle/60">
          {filtered.map((report) => (
            <li key={report.id} className="px-5 py-4">
              <div className="flex flex-wrap items-start gap-4">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-full border border-gold/20 bg-onyx font-serif text-base text-gold">
                  {report.userInitial}
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-sm font-medium text-ivory">{report.user}</span>
                    <span className="text-[11px] text-ivory-muted">em</span>
                    <span className="text-[12px] text-ivory">{report.context}</span>
                    <span className="ml-2 text-[11px] text-ivory-muted">
                      {report.createdAt ? new Date(report.createdAt).toLocaleString("pt-BR") : "—"}
                    </span>
                  </div>
                  <p className="mt-2 rounded-md border border-border-subtle bg-surface-elevated/40 p-3 text-[13px] leading-relaxed text-ivory">
                    “{report.body}”
                  </p>
                  <div className="mt-2 flex flex-wrap items-center gap-2">
                    <StatusPill label={reasonLabel(report.reason)} tone="warning" />
                    <span className="inline-flex items-center gap-1 text-[11px] text-ivory-muted">
                      <Flag className="size-3 text-rose-400" />
                      {report.reportCount} report(s)
                    </span>
                    <StatusPill label={statusLabel(report.status)} tone={statusTone(report.status)} />
                  </div>
                </div>

                <div className="flex shrink-0 flex-wrap items-center gap-2">
                  <Button
                    variant="outline"
                    onClick={() => updateReportMutation.mutate({ id: report.id, status: "APPROVED" })}
                    className="h-9 gap-1.5 border-emerald-500/30 bg-emerald-500/5 px-3 text-xs text-emerald-300 hover:border-emerald-500/60 hover:bg-emerald-500/10 hover:text-emerald-200"
                  >
                    <Eye className="size-3.5" />
                    Aprovar
                  </Button>
                  <Button
                    variant="outline"
                    onClick={() => updateVisibilityMutation.mutate({ commentId: report.commentId, visibility: "HIDDEN" })}
                    className="h-9 gap-1.5 border-amber-500/30 bg-amber-500/5 px-3 text-xs text-amber-300 hover:border-amber-500/60 hover:bg-amber-500/10 hover:text-amber-200"
                  >
                    <EyeOff className="size-3.5" />
                    Ocultar
                  </Button>
                  <Button
                    variant="outline"
                    onClick={() => updateReportMutation.mutate({ id: report.id, status: "RESOLVED" })}
                    className="h-9 gap-1.5 border-gold/30 bg-gold/5 px-3 text-xs text-gold hover:border-gold/60 hover:bg-gold/10"
                  >
                    <CheckCircle2 className="size-3.5" />
                    Resolver
                  </Button>
                </div>
              </div>
            </li>
          ))}
          {filtered.length === 0 ? (
            <li className="px-5 py-12 text-center text-sm text-ivory-muted">Nenhum comentário neste filtro.</li>
          ) : null}
        </ul>
      </AdminCard>
    </div>
  );
}

export default AdminComments;
