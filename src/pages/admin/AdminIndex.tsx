import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import {
  AlertTriangle,
  Calendar,
  ChevronRight,
  Clock,
  Image as ImageIcon,
  MessageSquare,
  Minus,
  PlayCircle,
  Plus,
  Sparkles,
  Star,
  TrendingDown,
  TrendingUp,
  Tv,
} from "lucide-react";

import { fetchAdminDashboard } from "@/lib/backend-api";
import { AdminCard, AdminCardHeader, StatusPill } from "@/components/admin/AdminCard";

const METRIC_ICONS: Record<string, typeof Tv> = {
  animes: Tv,
  episodes: PlayCircle,
  pending: Clock,
  reports: MessageSquare,
  featured: Star,
};

const SECTION_ICONS: Record<string, typeof Sparkles> = {
  hero: Star,
  continue: PlayCircle,
  season: Calendar,
  recent: Plus,
};

function publicationTone(status: string) {
  if (status === "PUBLISHED") return "success" as const;
  if (status === "DRAFT") return "muted" as const;
  if (status === "SCHEDULED") return "info" as const;
  if (status === "REVIEW") return "warning" as const;
  return "muted" as const;
}

function publicationLabel(status: string) {
  if (status === "PUBLISHED") return "Publicado";
  if (status === "DRAFT") return "Rascunho";
  if (status === "SCHEDULED") return "Agendado";
  if (status === "REVIEW") return "Revisão";
  return status;
}

function sectionModeLabel(mode: string) {
  if (mode === "MANUAL") return "Manual";
  if (mode === "AUTOMATIC") return "Automático";
  if (mode === "HYBRID") return "Híbrido";
  return mode;
}

function reportReasonLabel(reason: string) {
  return reason.replaceAll("_", " ").toLowerCase().replace(/^\w/, (char) => char.toUpperCase());
}

function suggestionLabel(status: string) {
  if (status === "APPROVED") return "Aprovado";
  if (status === "IN_REVIEW") return "Em análise";
  if (status === "REJECTED") return "Recusado";
  return "Novo";
}

function AdminDashboard() {
  const dashboardQuery = useQuery({
    queryKey: ["admin-dashboard"],
    queryFn: fetchAdminDashboard,
  });

  const dashboard = dashboardQuery.data;

  if (dashboardQuery.isLoading) {
    return <div className="text-sm text-ivory-muted">Carregando dashboard...</div>;
  }

  if (!dashboard) {
    return <div className="text-sm text-ivory-muted">Não foi possível carregar o dashboard.</div>;
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-serif text-3xl text-ivory">Dashboard</h1>
        <p className="mt-1 text-sm text-ivory-muted">Visão geral operacional do catálogo e curadoria.</p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
        {dashboard.metrics.map((metric) => {
          const Icon = METRIC_ICONS[metric.key] ?? Tv;
          const TrendIcon = metric.trend === "up" ? TrendingUp : metric.trend === "down" ? TrendingDown : Minus;
          const trendClass =
            metric.trend === "up"
              ? "text-emerald-400"
              : metric.trend === "down"
                ? "text-rose-400"
                : "text-ivory-muted";

          return (
            <AdminCard key={metric.key} className="p-5">
              <div className="flex items-start gap-4">
                <div className="flex size-11 shrink-0 items-center justify-center rounded-lg border border-gold/20 bg-gold/5 text-gold">
                  <Icon className="size-5" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-[11px] uppercase tracking-[0.16em] text-ivory-muted">{metric.label}</p>
                  <p className="mt-1 font-serif text-3xl text-ivory">{metric.value}</p>
                  {metric.delta ? (
                    <p className={`mt-1 flex items-center gap-1 text-[11px] ${trendClass}`}>
                      <TrendIcon className="size-3" />
                      {metric.delta}
                    </p>
                  ) : null}
                </div>
              </div>
            </AdminCard>
          );
        })}
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
        <AdminCard>
          <AdminCardHeader
            title="Publicações recentes"
            action={
              <Link to="/admin/episodios" className="text-[11px] uppercase tracking-[0.14em] text-gold/80 transition-colors hover:text-gold">
                Ver tudo →
              </Link>
            }
          />
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border-subtle text-[11px] uppercase tracking-[0.12em] text-ivory-muted">
                  <th className="px-5 py-3 text-left font-medium">Título</th>
                  <th className="px-3 py-3 text-left font-medium">Tipo</th>
                  <th className="px-3 py-3 text-left font-medium">Status</th>
                  <th className="px-5 py-3 text-left font-medium">Atualizado</th>
                </tr>
              </thead>
              <tbody>
                {dashboard.recentPublications.map((publication) => (
                  <tr key={publication.id} className="border-b border-border-subtle/60 transition-colors last:border-0 hover:bg-surface-elevated/40">
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-3">
                        {publication.thumb ? (
                          <img src={publication.thumb} alt="" className="size-10 rounded-md object-cover" loading="lazy" />
                        ) : (
                          <div className="size-10 rounded-md bg-surface-elevated" />
                        )}
                        <div className="min-w-0">
                          <p className="truncate font-medium text-ivory">{publication.title}</p>
                          <p className="truncate text-[11px] text-ivory-muted">{publication.subtitle}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-3 py-3 text-ivory-muted">{publication.type === "EPISODE" ? "Episódio" : "Anime"}</td>
                    <td className="px-3 py-3">
                      <StatusPill label={publicationLabel(publication.status)} tone={publicationTone(publication.status)} />
                    </td>
                    <td className="px-5 py-3 text-[12px] text-ivory-muted">{publication.updatedAt}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </AdminCard>

        <AdminCard>
          <AdminCardHeader
            title="Home / Curadoria"
            action={
              <Link to="/admin/home" className="text-[11px] uppercase tracking-[0.14em] text-gold/80 transition-colors hover:text-gold">
                Gerenciar →
              </Link>
            }
          />
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border-subtle text-[11px] uppercase tracking-[0.12em] text-ivory-muted">
                  <th className="px-5 py-3 text-left font-medium">Seção</th>
                  <th className="px-3 py-3 text-left font-medium">Modo</th>
                  <th className="px-3 py-3 text-right font-medium">Itens</th>
                  <th className="px-5 py-3 text-left font-medium">Status</th>
                </tr>
              </thead>
              <tbody>
                {dashboard.homeSections.map((section) => {
                  const Icon = SECTION_ICONS[section.id] ?? Sparkles;
                  const modeTone =
                    section.mode === "MANUAL" ? "warning" : section.mode === "AUTOMATIC" ? "info" : "success";

                  return (
                    <tr key={section.id} className="border-b border-border-subtle/60 transition-colors last:border-0 hover:bg-surface-elevated/40">
                      <td className="px-5 py-3">
                        <div className="flex items-center gap-2.5">
                          <Icon className="size-4 text-gold/70" />
                          <span className="font-medium text-ivory">{section.name}</span>
                        </div>
                      </td>
                      <td className="px-3 py-3">
                        <StatusPill label={sectionModeLabel(section.mode)} tone={modeTone} />
                      </td>
                      <td className="px-3 py-3 text-right text-ivory-muted">{section.items}</td>
                      <td className="px-5 py-3">
                        <StatusPill label={section.active ? "Ativo" : "Inativo"} tone={section.active ? "success" : "muted"} />
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </AdminCard>
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
        <AdminCard>
          <AdminCardHeader title="Comentários e reports" />
          <ul className="divide-y divide-border-subtle/60">
            {dashboard.reports.map((report) => (
              <li key={report.id} className="px-5 py-3.5">
                <div className="flex items-start gap-3">
                  <div className="flex size-8 shrink-0 items-center justify-center rounded-full border border-gold/20 bg-onyx text-[12px] font-medium text-gold">
                    {report.userInitial}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-ivory">{report.user}</p>
                    <p className="truncate text-[11px] text-ivory-muted">{report.context}</p>
                    <div className="mt-1 flex items-center gap-2">
                      <StatusPill label={reportReasonLabel(report.reason)} tone="warning" />
                      <span className="text-[10px] text-ivory-muted">
                        {report.createdAt ? new Date(report.createdAt).toLocaleString("pt-BR") : "—"}
                      </span>
                    </div>
                  </div>
                </div>
              </li>
            ))}
          </ul>
          <div className="border-t border-border-subtle px-5 py-3 text-center">
            <Link to="/admin/comentarios" className="text-[11px] uppercase tracking-[0.14em] text-gold/80 transition-colors hover:text-gold">
              Ir para moderação →
            </Link>
          </div>
        </AdminCard>

        <AdminCard>
          <AdminCardHeader title="Sugestões da comunidade" />
          <ul className="divide-y divide-border-subtle/60">
            {dashboard.suggestions.map((suggestion) => {
              const tone =
                suggestion.status === "APPROVED"
                  ? ("success" as const)
                  : suggestion.status === "IN_REVIEW"
                    ? ("warning" as const)
                    : suggestion.status === "REJECTED"
                      ? ("danger" as const)
                      : ("info" as const);

              return (
                <li key={suggestion.id} className="flex items-center gap-3 px-5 py-3">
                  <span className="flex size-7 shrink-0 items-center justify-center rounded-md bg-surface-elevated font-serif text-sm text-gold">
                    {suggestion.rank}
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-ivory">{suggestion.title}</p>
                    <p className="text-[11px] text-ivory-muted">{suggestion.votes.toLocaleString("pt-BR")} votos</p>
                  </div>
                  <StatusPill label={suggestionLabel(suggestion.status)} tone={tone} />
                </li>
              );
            })}
          </ul>
          <div className="border-t border-border-subtle px-5 py-3 text-center">
            <Link to="/admin/sugestoes" className="text-[11px] uppercase tracking-[0.14em] text-gold/80 transition-colors hover:text-gold">
              Gerenciar sugestões →
            </Link>
          </div>
        </AdminCard>

        <AdminCard>
          <AdminCardHeader title="Saúde do catálogo" />
          <ul className="divide-y divide-border-subtle/60">
            {dashboard.health.map((item) => {
              const Icon =
                item.id === "h1"
                  ? AlertTriangle
                  : item.id === "h2"
                    ? ImageIcon
                    : item.id === "h3"
                      ? Clock
                      : MessageSquare;
              const iconClass = item.severity === "warning" ? "text-amber-300" : "text-sky-300";

              return (
                <li key={item.id} className="flex items-center gap-3 px-5 py-3.5">
                  <div className={`flex size-9 shrink-0 items-center justify-center rounded-lg bg-surface-elevated ${iconClass}`}>
                    <Icon className="size-4" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-ivory">{item.title}</p>
                    <p className="truncate text-[11px] text-ivory-muted">{item.description}</p>
                  </div>
                  <span className="font-serif text-xl text-ivory">{item.count}</span>
                  <ChevronRight className="size-4 text-ivory-muted" />
                </li>
              );
            })}
          </ul>
        </AdminCard>
      </div>
    </div>
  );
}

export default AdminDashboard;
