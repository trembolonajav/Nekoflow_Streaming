import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Bell, CheckCheck, ExternalLink, Loader2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import {
  fetchNotifications,
  fetchUnreadNotificationCount,
  markAllNotificationsRead,
  markNotificationRead,
  type NotificationDto,
} from "@/lib/backend-api";
import { cn } from "@/lib/utils";

function formatNotificationDate(value: string | null) {
  if (!value) return "Agora";
  return new Date(value).toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function categoryLabel(notification: NotificationDto) {
  if (notification.type === "ADMIN") return "Admin";
  if (notification.type === "WORKER") return "Worker";
  if (notification.type === "COMMUNITY") return "Comunidade";
  if (notification.type === "CONTENT") return "Conteudo";
  if (notification.type === "ACCOUNT") return "Conta";
  return "Sistema";
}

export function NotificationBell() {
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const unreadQuery = useQuery({
    queryKey: ["notifications-unread-count"],
    queryFn: fetchUnreadNotificationCount,
    staleTime: 30_000,
    refetchInterval: 60_000,
  });

  const notificationsQuery = useQuery({
    queryKey: ["notifications", "dropdown"],
    queryFn: () => fetchNotifications(0, 8),
    enabled: open,
    staleTime: 15_000,
  });

  useEffect(() => {
    if (!open) return;
    void queryClient.invalidateQueries({ queryKey: ["notifications", "dropdown"] });
    void queryClient.invalidateQueries({ queryKey: ["notifications-unread-count"] });
  }, [open, queryClient]);

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
      void queryClient.invalidateQueries({ queryKey: ["notifications"] });
      void queryClient.invalidateQueries({ queryKey: ["notifications-unread-count"] });
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const unreadCount = unreadQuery.data?.count ?? 0;
  const notifications = notificationsQuery.data?.items ?? [];

  const openNotification = (notification: NotificationDto) => {
    if (!notification.read) {
      markReadMutation.mutate(notification.id);
    }
    if (notification.actionUrl) {
      setOpen(false);
      navigate(notification.actionUrl);
    }
  };

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          aria-label={unreadCount > 0 ? `Notificacoes, ${unreadCount} nao lidas` : "Notificacoes"}
          className="relative text-ivory hover:bg-surface-elevated hover:text-gold"
        >
          <Bell className="size-5" />
          {unreadCount > 0 && (
            <span className="absolute right-1.5 top-1.5 flex min-w-4 items-center justify-center rounded-full bg-gold px-1 text-[10px] font-semibold leading-4 text-onyx ring-2 ring-onyx">
              {unreadCount > 99 ? "99+" : unreadCount}
            </span>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent
        align="end"
        className="w-[min(360px,calc(100vw-24px))] border-gold/15 bg-surface/95 p-0 backdrop-blur-xl"
      >
        <div className="flex items-center justify-between gap-3 border-b border-border-subtle px-4 py-3">
          <span className="font-serif text-base text-ivory">Notificacoes</span>
          {unreadCount > 0 ? (
            <button
              type="button"
              onClick={() => markAllMutation.mutate()}
              disabled={markAllMutation.isPending}
              className="inline-flex items-center gap-1.5 text-[10px] uppercase tracking-[0.16em] text-gold transition-colors hover:text-gold/80 disabled:opacity-60"
            >
              <CheckCheck className="size-3.5" />
              Ler todas
            </button>
          ) : null}
        </div>

        <div className="max-h-[380px] overflow-y-auto">
          {notificationsQuery.isLoading ? (
            <div className="flex items-center justify-center gap-2 px-4 py-10 text-sm text-ivory-muted">
              <Loader2 className="size-4 animate-spin" />
              Carregando...
            </div>
          ) : notificationsQuery.isError ? (
            <div className="px-4 py-10 text-center text-sm text-ivory-muted">
              Nao foi possivel carregar notificacoes.
            </div>
          ) : notifications.length === 0 ? (
            <div className="px-4 py-10 text-center text-sm text-ivory-muted">
              Nenhuma notificacao por enquanto.
            </div>
          ) : (
            <ul>
              {notifications.map((notification) => (
                <li key={notification.id} className="border-b border-border-subtle last:border-0">
                  <button
                    type="button"
                    onClick={() => openNotification(notification)}
                    className="group flex w-full items-start gap-3 px-4 py-3 text-left transition-colors hover:bg-surface-elevated/60"
                  >
                    <span
                      className={cn(
                        "mt-1.5 size-1.5 shrink-0 rounded-full",
                        notification.read ? "bg-transparent" : "bg-gold",
                      )}
                      aria-hidden
                    />
                    <span className="min-w-0 flex-1">
                      <span className="flex items-center gap-2">
                        <span className="truncate text-sm font-medium text-ivory">{notification.title}</span>
                        {notification.actionUrl ? <ExternalLink className="size-3 shrink-0 text-ivory-muted/70" /> : null}
                      </span>
                      <span className="mt-0.5 line-clamp-2 text-xs leading-relaxed text-ivory-muted">
                        {notification.message}
                      </span>
                      <span className="mt-2 flex items-center gap-2 text-[10px] uppercase tracking-[0.14em] text-ivory-muted/65">
                        <span>{categoryLabel(notification)}</span>
                        <span aria-hidden>·</span>
                        <span>{formatNotificationDate(notification.createdAt)}</span>
                      </span>
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="border-t border-border-subtle px-4 py-2.5 text-center">
          <Link
            to="/notificacoes"
            onClick={() => setOpen(false)}
            className="text-xs uppercase tracking-[0.14em] text-gold/80 transition-colors hover:text-gold"
          >
            Ver todas
          </Link>
        </div>
      </PopoverContent>
    </Popover>
  );
}
