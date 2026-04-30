import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Check, FileEdit, Lightbulb, X } from "lucide-react";
import { toast } from "sonner";

import {
  convertAdminSuggestionToAnime,
  fetchAdminSuggestions,
  updateAdminSuggestionStatus,
} from "@/lib/backend-api";
import { AdminCard, AdminCardHeader, StatusPill } from "@/components/admin/AdminCard";
import { Button } from "@/components/ui/button";

function suggestionTone(status: string) {
  if (status === "APPROVED") return "success" as const;
  if (status === "IN_REVIEW") return "warning" as const;
  if (status === "REJECTED") return "danger" as const;
  return "info" as const;
}

function suggestionLabel(status: string) {
  if (status === "APPROVED") return "Aprovado";
  if (status === "IN_REVIEW") return "Em análise";
  if (status === "REJECTED") return "Recusado";
  return "Novo";
}

function AdminSuggestions() {
  const queryClient = useQueryClient();
  const suggestionsQuery = useQuery({
    queryKey: ["admin-suggestions"],
    queryFn: fetchAdminSuggestions,
  });

  const updateStatusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) => updateAdminSuggestionStatus(id, status),
    onSuccess: (_, variables) => {
      const messages: Record<string, string> = {
        APPROVED: "Sugestão aprovada.",
        IN_REVIEW: "Sugestão enviada para análise.",
        REJECTED: "Sugestão recusada.",
      };
      toast.success(messages[variables.status] ?? "Sugestão atualizada.");
      void queryClient.invalidateQueries({ queryKey: ["admin-suggestions"] });
      void queryClient.invalidateQueries({ queryKey: ["admin-dashboard"] });
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const convertMutation = useMutation({
    mutationFn: convertAdminSuggestionToAnime,
    onSuccess: () => {
      toast.success("Sugestão convertida em rascunho do catálogo.");
      void queryClient.invalidateQueries({ queryKey: ["admin-suggestions"] });
      void queryClient.invalidateQueries({ queryKey: ["admin-dashboard"] });
      void queryClient.invalidateQueries({ queryKey: ["admin-animes"] });
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const items = suggestionsQuery.data ?? [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-serif text-3xl text-ivory">Sugestões da comunidade</h1>
        <p className="mt-1 text-sm text-ivory-muted">
          Títulos pedidos pelos usuários com persistência real no backend.
        </p>
      </div>

      <AdminCard>
        <AdminCardHeader title={`${items.length} sugestão(ões)`} />
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border-subtle text-[11px] uppercase tracking-[0.12em] text-ivory-muted">
                <th className="px-5 py-3 text-left font-medium">#</th>
                <th className="px-3 py-3 text-left font-medium">Título sugerido</th>
                <th className="px-3 py-3 text-right font-medium">Votos</th>
                <th className="px-3 py-3 text-left font-medium">Recebido</th>
                <th className="px-3 py-3 text-left font-medium">Status</th>
                <th className="px-5 py-3 text-right font-medium">Ações</th>
              </tr>
            </thead>
            <tbody>
              {items.map((suggestion) => (
                <tr
                  key={suggestion.id}
                  className="border-b border-border-subtle/60 transition-colors last:border-0 hover:bg-surface-elevated/40"
                >
                  <td className="px-5 py-3">
                    <span className="flex size-8 items-center justify-center rounded-md bg-surface-elevated font-serif text-sm text-gold">
                      {suggestion.rank}
                    </span>
                  </td>
                  <td className="px-3 py-3">
                    <div className="flex items-center gap-2">
                      <Lightbulb className="size-4 text-gold/70" />
                      <div className="min-w-0">
                        <p className="truncate font-medium text-ivory">{suggestion.title}</p>
                        {suggestion.note ? (
                          <p className="truncate text-[11px] text-ivory-muted">{suggestion.note}</p>
                        ) : null}
                      </div>
                    </div>
                  </td>
                  <td className="px-3 py-3 text-right text-ivory">
                    {suggestion.votes.toLocaleString("pt-BR")}
                  </td>
                  <td className="px-3 py-3 text-[12px] text-ivory-muted">
                    {suggestion.createdAt ? new Date(suggestion.createdAt).toLocaleDateString("pt-BR") : "—"}
                  </td>
                  <td className="px-3 py-3">
                    <StatusPill label={suggestionLabel(suggestion.status)} tone={suggestionTone(suggestion.status)} />
                  </td>
                  <td className="px-5 py-3">
                    <div className="flex justify-end gap-2">
                      <Button
                        variant="outline"
                        onClick={() => updateStatusMutation.mutate({ id: suggestion.id, status: "APPROVED" })}
                        className="h-8 gap-1.5 border-emerald-500/30 bg-emerald-500/5 px-2.5 text-xs text-emerald-300 hover:border-emerald-500/60 hover:bg-emerald-500/10 hover:text-emerald-200"
                      >
                        <Check className="size-3.5" />
                        Aprovar
                      </Button>
                      <Button
                        variant="outline"
                        onClick={() => updateStatusMutation.mutate({ id: suggestion.id, status: "REJECTED" })}
                        className="h-8 gap-1.5 border-rose-500/30 bg-rose-500/5 px-2.5 text-xs text-rose-300 hover:border-rose-500/60 hover:bg-rose-500/10 hover:text-rose-200"
                      >
                        <X className="size-3.5" />
                        Recusar
                      </Button>
                      <Button
                        variant="outline"
                        onClick={() => convertMutation.mutate(suggestion.id)}
                        className="h-8 gap-1.5 border-gold/30 bg-gold/5 px-2.5 text-xs text-gold hover:border-gold/60 hover:bg-gold/10"
                      >
                        <FileEdit className="size-3.5" />
                        Rascunho
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
              {items.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-5 py-12 text-center text-sm text-ivory-muted">
                    Nenhuma sugestão cadastrada.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </AdminCard>
    </div>
  );
}

export default AdminSuggestions;
