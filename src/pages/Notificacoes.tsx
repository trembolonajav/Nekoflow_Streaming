import { useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Bell, CheckCheck, ChevronLeft, ChevronRight, Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Footer } from "@/components/layout/Footer";
import { Header } from "@/components/layout/Header";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/hooks/use-auth";
import {
  fetchNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  type NotificationDto,
} from "@/lib/backend-api";
import { cn } from "@/lib/utils";

const PAGE_SIZE = 12;

function formatDate(value: string | null) {
  if (!value) return "Agora";
  return new Date(value).toLocaleString("pt-BR");
}

function typeLabel(type: string) {
  const labels: Record<string, string> = {
    ACCOUNT: "Conta",
    CONTENT: "Conteudo",
    COMMUNITY: "Comunidade",
    MODERATION: "Moderacao",
    WORKER: "Worker",
    ADMIN: "Admin",
    SYSTEM: "Sistema",
  };
  return labels[type] ?? type;
}

function severityClass(severity: string) {
  if (severity === "ERROR") return "border-destructive/35 text-destructive";
  if (severity === "WARNING") return "border-gold/35 text-gold";
  if (severity === "SUCCESS") return "border-emerald-400/35 text-emerald-300";
  return "border-ivory-muted/25 text-ivory-muted";
}

export default function Notificacoes() {
  const { isAuthenticated, isReady } = useAuth();
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();

  const notificationsQuery = useQuery({
    queryKey: ["notifications", "page", page],
    queryFn: () => fetchNotifications(page, PAGE_SIZE),
    enabled: isReady && isAuthenticated,
  });

  const markReadMutation = useMutation({
    mutationFn: markNotificationRead,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["notifications"] });
      void queryClient.invalidateQueries({ queryKey: ["notifications-unread-count"] });
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const markAllMutation = useMutation({
    mutationFn: markAllNotificationsRead,
    onSuccess: () => {
      toast.success("Notificacoes marcadas como lidas.");
      void queryClient.invalidateQueries({ queryKey: ["notifications"] });
      void queryClient.invalidateQueries({ queryKey: ["notifications-unread-count"] });
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const data = notificationsQuery.data;
  const totalPages = Math.max(1, Math.ceil((data?.total ?? 0) / PAGE_SIZE));

  if (isReady && !isAuthenticated) {
    return (
      <div className="flex min-h-screen flex-col bg-onyx">
        <Header />
        <main className="flex flex-1 items-center justify-center px-6 py-24">
          <div className="max-w-md text-center">
            <Bell className="mx-auto size-10 text-gold" />
            <h1 className="mt-6 font-serif text-3xl text-ivory">Entre para ver notificacoes</h1>
            <p className="mt-3 text-sm text-ivory-muted">
              Alertas de conta, comunidade e catalogo ficam ligados a sua sessao.
            </p>
            <Button asChild className="mt-8 bg-gold text-onyx hover:bg-gold/90">
              <Link to="/entrar">Entrar agora</Link>
            </Button>
          </div>
        </main>
        <Footer />
      </div>
    );
  }

  return (
    <div className="flex min-h-screen flex-col bg-onyx">
      <Header />
      <main className="flex-1">
        <section className="mx-auto w-full max-w-[1100px] px-4 py-8 md:px-8 md:py-12">
          <div className="mb-8 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
            <div>
              <p className="text-[11px] uppercase tracking-[0.24em] text-gold">Central pessoal</p>
              <h1 className="mt-2 font-serif text-4xl text-ivory">Notificacoes</h1>
              <p className="mt-2 max-w-2xl text-sm text-ivory-muted">
                Acompanhe avisos da sua conta, comunidade, catalogo e operacao quando sua role permitir.
              </p>
            </div>
            <Button
              type="button"
              variant="outline"
              onClick={() => markAllMutation.mutate()}
              disabled={markAllMutation.isPending || !data?.items.length}
              className="gap-2 border-gold/25 bg-gold/5 text-gold hover:border-gold/50 hover:bg-gold/10 hover:text-gold"
            >
              <CheckCheck className="size-4" />
              {markAllMutation.isPending ? "Marcando..." : "Marcar todas como lidas"}
            </Button>
          </div>

          {notificationsQuery.isLoading ? (
            <StateBox icon={<Loader2 className="size-5 animate-spin" />} label="Carregando notificacoes..." />
          ) : notificationsQuery.isError ? (
            <StateBox label="Nao foi possivel carregar suas notificacoes." />
          ) : !data || data.items.length === 0 ? (
            <StateBox label="Nenhuma notificacao por enquanto." />
          ) : (
            <>
              <div className="overflow-hidden rounded-xl border border-border-subtle bg-surface/40">
                {data.items.map((notification) => (
                  <NotificationRow
                    key={notification.id}
                    notification={notification}
                    onMarkRead={() => markReadMutation.mutate(notification.id)}
                  />
                ))}
              </div>

              <div className="mt-6 flex items-center justify-between gap-4">
                <span className="text-xs uppercase tracking-[0.16em] text-ivory-muted">
                  Pagina {page + 1} de {totalPages}
                </span>
                <div className="flex items-center gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => setPage((current) => Math.max(0, current - 1))}
                    disabled={page === 0}
                    className="gap-2 border-border-subtle bg-transparent text-ivory hover:bg-surface"
                  >
                    <ChevronLeft className="size-4" />
                    Anterior
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => setPage((current) => Math.min(totalPages - 1, current + 1))}
                    disabled={page >= totalPages - 1}
                    className="gap-2 border-border-subtle bg-transparent text-ivory hover:bg-surface"
                  >
                    Proxima
                    <ChevronRight className="size-4" />
                  </Button>
                </div>
              </div>
            </>
          )}
        </section>
      </main>
      <Footer />
    </div>
  );
}

function NotificationRow({
  notification,
  onMarkRead,
}: {
  notification: NotificationDto;
  onMarkRead: () => void;
}) {
  const content = (
    <article className="flex gap-4 border-b border-border-subtle px-4 py-4 last:border-0 transition-colors hover:bg-surface-elevated/40 md:px-5">
      <span
        className={cn("mt-2 size-2 shrink-0 rounded-full", notification.read ? "bg-transparent" : "bg-gold")}
        aria-hidden
      />
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <span className={cn("rounded-full border px-2 py-0.5 text-[10px] uppercase tracking-[0.14em]", severityClass(notification.severity))}>
            {typeLabel(notification.type)}
          </span>
          {!notification.read ? (
            <span className="rounded-full border border-gold/25 px-2 py-0.5 text-[10px] uppercase tracking-[0.14em] text-gold">
              Nova
            </span>
          ) : null}
        </div>
        <h2 className="mt-2 font-serif text-xl text-ivory">{notification.title}</h2>
        <p className="mt-1 text-sm leading-relaxed text-ivory-muted">{notification.message}</p>
        <div className="mt-3 flex flex-wrap items-center gap-3 text-[11px] uppercase tracking-[0.14em] text-ivory-muted/70">
          <span>{formatDate(notification.createdAt)}</span>
          {notification.relatedEntityType ? <span>{notification.relatedEntityType}</span> : null}
        </div>
      </div>
      {!notification.read ? (
        <button
          type="button"
          onClick={(event) => {
            event.preventDefault();
            onMarkRead();
          }}
          className="self-start text-[11px] uppercase tracking-[0.14em] text-gold transition-colors hover:text-gold/80"
        >
          Marcar lida
        </button>
      ) : null}
    </article>
  );

  if (notification.actionUrl) {
    return (
      <Link to={notification.actionUrl} onClick={!notification.read ? onMarkRead : undefined} className="block">
        {content}
      </Link>
    );
  }

  return content;
}

function StateBox({ label, icon }: { label: string; icon?: React.ReactNode }) {
  return (
    <div className="flex min-h-64 items-center justify-center gap-2 rounded-xl border border-dashed border-border-subtle bg-surface/30 px-6 py-16 text-center text-sm text-ivory-muted">
      {icon}
      <span>{label}</span>
    </div>
  );
}
