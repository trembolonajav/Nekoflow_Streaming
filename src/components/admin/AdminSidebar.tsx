import { Link, useLocation } from "react-router-dom";
import {
  LayoutDashboard,
  Film,
  PlayCircle,
  LayoutGrid,
  CalendarDays,
  MessageSquareWarning,
  Lightbulb,
  Settings,
  ArrowLeft,
  Workflow,
} from "lucide-react";

import logoUrl from "@/assets/nekoflow-logo.png";
import mascotUrl from "@/assets/nekoflow-mascot.png";
import { cn } from "@/lib/utils";

const NAV = [
  { label: "Dashboard", to: "/admin" as const, icon: LayoutDashboard, exact: true },
  { label: "Animes", to: "/admin/animes" as const, icon: Film },
  { label: "Episódios", to: "/admin/episodios" as const, icon: PlayCircle },
  { label: "Home / Curadoria", to: "/admin/home" as const, icon: LayoutGrid },
  { label: "Calendário", to: "/admin/calendario" as const, icon: CalendarDays },
  { label: "Comentários", to: "/admin/comentarios" as const, icon: MessageSquareWarning },
  { label: "Sugestões", to: "/admin/sugestoes" as const, icon: Lightbulb },
];

export function AdminSidebar() {
  const location = useLocation();

  return (
    <aside className="sticky top-0 hidden h-screen w-[260px] shrink-0 flex-col border-r border-gold/10 bg-onyx/90 backdrop-blur-xl lg:flex">
      {/* Logo */}
      <div className="flex h-24 items-center gap-2 border-b border-border-subtle px-5">
        <img src={logoUrl} alt="Nekoflow" className="h-16 w-auto" />
        <span className="ml-1 mt-1 text-[10px] font-medium uppercase tracking-[0.32em] text-gold/80">
          Admin
        </span>
      </div>

      {/* Nav */}
      <nav className="flex-1 overflow-y-auto px-3 py-5">
        <ul className="space-y-1">
          {NAV.map((item) => {
            const isActive = item.exact
              ? location.pathname === item.to
              : location.pathname === item.to || location.pathname.startsWith(item.to + "/");
            const Icon = item.icon;
            return (
              <li key={item.to}>
                <Link
                  to={item.to}
                  className={cn(
                    "group flex items-center gap-3 rounded-md px-3 py-2.5 text-sm font-medium transition-all",
                    isActive
                      ? "bg-gold/10 text-gold ring-1 ring-gold/20"
                      : "text-ivory-muted hover:bg-surface-elevated/60 hover:text-ivory",
                  )}
                >
                  <Icon
                    className={cn(
                      "size-4 transition-colors",
                      isActive ? "text-gold" : "text-ivory-muted/80 group-hover:text-ivory",
                    )}
                  />
                  {item.label}
                </Link>
              </li>
            );
          })}
        </ul>

        <div className="my-5 h-px bg-border-subtle" />

        <a
          href={import.meta.env.VITE_WORKER_URL ?? "http://localhost:5174"}
          target="_blank"
          rel="noreferrer"
          className="flex items-center gap-3 rounded-md px-3 py-2.5 text-sm text-ivory-muted transition-colors hover:bg-surface-elevated/60 hover:text-ivory"
        >
          <Workflow className="size-4" />
          Worker / Automacao
        </a>
        <Link
          to="/"
          className="flex items-center gap-3 rounded-md px-3 py-2.5 text-sm text-ivory-muted transition-colors hover:bg-surface-elevated/60 hover:text-ivory"
        >
          <ArrowLeft className="size-4" />
          Voltar ao site
        </Link>
        <button
          type="button"
          className="mt-1 flex w-full items-center gap-3 rounded-md px-3 py-2.5 text-sm text-ivory-muted transition-colors hover:bg-surface-elevated/60 hover:text-ivory"
        >
          <Settings className="size-4" />
          Configurações
        </button>
      </nav>

      {/* Mascote watermark */}
      <div className="relative h-40 overflow-hidden border-t border-border-subtle">
        <img
          src={mascotUrl}
          alt=""
          className="absolute inset-x-0 bottom-0 mx-auto h-40 w-auto opacity-70"
          draggable={false}
        />
        <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-onyx via-onyx/60 to-transparent" />
      </div>
    </aside>
  );
}
