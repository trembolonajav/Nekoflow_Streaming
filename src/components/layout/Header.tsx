import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import {
  Bell,
  LogIn,
  Menu,
  Plus,
  Search,
  Settings,
  LogOut,
  User,
  Bookmark,
  ShieldCheck,
  X,
} from "lucide-react";
import { toast } from "sonner";

import logoUrl from "@/assets/nekoflow-logo.png";
import { cn } from "@/lib/utils";
import { useAuth } from "@/hooks/use-auth";
import { Button } from "@/components/ui/button";
import { HeaderSearch } from "./HeaderSearch";
import {
  Avatar,
  AvatarFallback,
  AvatarImage,
} from "@/components/ui/avatar";
import avatarDefault from "@/assets/profile-avatar-default.png";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { NotificationBell } from "@/components/notifications/NotificationBell";
import { SuggestAnimeDialog } from "./SuggestAnimeDialog";

const NAV_ITEMS = [
  { label: "Início", to: "/" as const },
  { label: "Calendário", to: "/calendario" as const },
  { label: "Explorar", to: "/explorar" as const },
];

export function Header() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, isAuthenticated, signOut } = useAuth();
  const [scrolled, setScrolled] = useState(false);
  const [mobileSearchOpen, setMobileSearchOpen] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  const handleSignOut = () => {
    signOut();
    toast.success("Você saiu da sua conta.");
    navigate("/");
  };

  return (
    <header
      className={cn(
        "sticky top-0 z-50 w-full border-b transition-all duration-300",
        scrolled
          ? "border-gold/10 bg-onyx/80 backdrop-blur-xl"
          : "border-transparent bg-onyx/40 backdrop-blur-md",
      )}
    >
      <div className="mx-auto flex h-24 w-full max-w-[1400px] items-center gap-3 px-4 md:h-28 md:gap-6 md:px-8">
        {/* Mobile: hamburger */}
        <Sheet open={mobileMenuOpen} onOpenChange={setMobileMenuOpen}>
          <SheetTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              className="-ml-2 text-ivory hover:bg-surface-elevated hover:text-gold md:hidden"
              aria-label="Abrir menu"
            >
              <Menu className="size-5" />
            </Button>
          </SheetTrigger>
          <SheetContent
            side="left"
            className="w-[300px] border-r border-gold/10 bg-onyx/95 p-0 backdrop-blur-xl"
          >
            <SheetHeader className="border-b border-border-subtle px-5 py-5 text-left">
              <SheetTitle className="flex items-center gap-2">
                <img src={logoUrl} alt="Nekoflow" className="h-12 w-auto" />
              </SheetTitle>
            </SheetHeader>
            <nav className="flex flex-col px-3 py-4">
              {NAV_ITEMS.map((item) => (
                <Link
                  key={item.label}
                  to={item.to}
                  onClick={() => setMobileMenuOpen(false)}
                  className="rounded-md px-3 py-3 text-sm font-medium text-ivory transition-colors hover:bg-surface-elevated hover:text-gold"
                >
                  {item.label}
                </Link>
              ))}

              <div className="my-3 h-px bg-border-subtle" />

              <SuggestAnimeDialog
                trigger={
                  <button
                    type="button"
                    className="group flex items-center gap-2 rounded-md border border-gold/30 bg-gold/5 px-3 py-3 text-sm font-medium text-gold transition-all duration-200 hover:border-gold/60 hover:bg-gold/10"
                  >
                    <Plus className="size-4" />
                    Sugerir anime
                  </button>
                }
              />

              <div className="my-3 h-px bg-border-subtle" />

              {isAuthenticated ? (
                <>
                  <Link
                    to="/notificacoes"
                    onClick={() => setMobileMenuOpen(false)}
                    className="flex items-center gap-3 rounded-md px-3 py-3 text-sm text-ivory transition-colors hover:bg-surface-elevated hover:text-gold"
                  >
                    <Bell className="size-4" />
                    Notificações
                  </Link>
                  <Link
                    to="/perfil"
                    onClick={() => setMobileMenuOpen(false)}
                    className="flex items-center gap-3 rounded-md px-3 py-3 text-sm text-ivory transition-colors hover:bg-surface-elevated hover:text-gold"
                  >
                    <User className="size-4" />
                    Meu perfil
                  </Link>
                  <button
                    type="button"
                    onClick={() => {
                      setMobileMenuOpen(false);
                      handleSignOut();
                    }}
                    className="flex items-center gap-3 rounded-md px-3 py-3 text-left text-sm text-ivory-muted transition-colors hover:bg-surface-elevated hover:text-ivory"
                  >
                    <LogOut className="size-4" />
                    Sair
                  </button>
                </>
              ) : (
                <Link
                  to="/entrar"
                  onClick={() => setMobileMenuOpen(false)}
                  className="flex items-center gap-3 rounded-md border border-gold/30 bg-gold/5 px-3 py-3 text-sm font-medium text-gold transition-all hover:border-gold/60 hover:bg-gold/10"
                >
                  <LogIn className="size-4" />
                  Entrar
                </Link>
              )}
            </nav>
          </SheetContent>
        </Sheet>

        {/* Logo */}
        <Link to="/" className="flex shrink-0 items-center outline-none" aria-label="Nekoflow — Início">
          <img
            src={logoUrl}
            alt="Nekoflow"
            width={512}
            height={288}
            className="h-20 w-auto md:h-24"
            draggable={false}
          />
        </Link>

        {/* Desktop nav */}
        <nav className="ml-4 hidden items-center gap-1 md:flex">
          {NAV_ITEMS.map((item) => {
            const isActive = item.to === "/" ? location.pathname === "/" : location.pathname.startsWith(item.to);
            return (
              <Link
                key={item.label}
                to={item.to}
                className={cn(
                  "group relative px-3 py-2 text-sm font-medium tracking-wide transition-colors duration-200",
                  isActive
                    ? "text-gold"
                    : "text-ivory hover:text-gold",
                )}
              >
                {item.label}
                <span
                  className={cn(
                    "pointer-events-none absolute inset-x-3 -bottom-0.5 h-px bg-gradient-to-r from-transparent via-gold to-transparent transition-opacity duration-300",
                    isActive ? "opacity-100" : "opacity-0 group-hover:opacity-60",
                  )}
                />
              </Link>
            );
          })}
        </nav>

        {/* Desktop search */}
        <div className="ml-auto hidden max-w-md flex-1 md:block">
          <HeaderSearch size="md" />
        </div>

        {/* Mobile actions */}
        <div className="ml-auto flex items-center gap-1 md:gap-2">
          {/* Mobile search trigger */}
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setMobileSearchOpen((v) => !v)}
            aria-label="Buscar"
            className="text-ivory hover:bg-surface-elevated hover:text-gold md:hidden"
          >
            {mobileSearchOpen ? <X className="size-5" /> : <Search className="size-5" />}
          </Button>

          {/* Suggest anime — desktop only as button-with-text */}
          <div className="hidden md:block">
            <SuggestAnimeDialog
              trigger={
                <Button
                  variant="outline"
                  className="h-10 gap-2 border-gold/30 bg-gold/5 px-3 text-sm font-medium text-gold shadow-none transition-all duration-200 hover:border-gold/60 hover:bg-gold/10 hover:text-gold"
                >
                  <Plus className="size-4" />
                  <span className="hidden lg:inline">Sugerir anime</span>
                  <span className="lg:hidden">Sugerir</span>
                </Button>
              }
            />
          </div>

          {isAuthenticated ? <NotificationBell /> : null}

          {/* Avatar / Entrar */}
          {isAuthenticated && user ? (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <button
                  aria-label="Conta"
                  className="ml-1 rounded-full outline-none ring-offset-2 ring-offset-onyx transition-all hover:ring-2 hover:ring-gold/50 focus-visible:ring-2 focus-visible:ring-gold/60"
                >
                  <Avatar className="size-9 border border-gold/30 bg-onyx">
                    <AvatarImage src={avatarDefault} alt={user.name} className="object-cover" />
                    <AvatarFallback className="bg-onyx font-serif text-base font-medium text-gold">
                      {user.initial}
                    </AvatarFallback>
                  </Avatar>
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent
                align="end"
                className="w-60 border-gold/15 bg-surface/95 backdrop-blur-xl"
              >
                <DropdownMenuLabel className="font-normal">
                  <div className="flex flex-col gap-0.5">
                    <span className="font-serif text-sm text-ivory">{user.name}</span>
                    <span className="truncate text-[11px] text-ivory-muted">{user.email}</span>
                  </div>
                </DropdownMenuLabel>
                <DropdownMenuSeparator className="bg-border-subtle" />
                <DropdownMenuItem
                  onClick={() => navigate("/perfil")}
                  className="text-ivory focus:bg-surface-elevated focus:text-gold"
                >
                  <User className="size-4" />
                  Meu perfil
                </DropdownMenuItem>
                <DropdownMenuItem
                  onClick={() => navigate("/perfil")}
                  className="text-ivory focus:bg-surface-elevated focus:text-gold"
                >
                  <Bookmark className="size-4" />
                  Minha lista
                </DropdownMenuItem>
                {user.role === "admin" && (
                  <DropdownMenuItem
                    onClick={() => navigate("/admin")}
                    className="text-gold focus:bg-gold/10 focus:text-gold"
                  >
                    <ShieldCheck className="size-4" />
                    Painel admin
                  </DropdownMenuItem>
                )}
                <DropdownMenuItem
                  onClick={() => navigate("/perfil")}
                  className="text-ivory focus:bg-surface-elevated focus:text-gold"
                >
                  <Settings className="size-4" />
                  Configurações
                </DropdownMenuItem>
                <DropdownMenuSeparator className="bg-border-subtle" />
                <DropdownMenuItem
                  onClick={handleSignOut}
                  className="text-ivory-muted focus:bg-surface-elevated focus:text-ivory"
                >
                  <LogOut className="size-4" />
                  Sair
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          ) : (
            <Link
              to="/entrar"
              className="ml-1 inline-flex h-10 items-center gap-2 rounded-md border border-gold/30 bg-gold/5 px-3 text-sm font-medium text-gold transition-all duration-200 hover:border-gold/60 hover:bg-gold/10"
            >
              <LogIn className="size-4" />
              <span className="hidden sm:inline">Entrar</span>
            </Link>
          )}
        </div>
      </div>

      {/* Mobile expanded search */}
      {mobileSearchOpen && (
        <div className="border-t border-border-subtle bg-onyx/90 px-4 py-3 backdrop-blur-xl md:hidden">
          <HeaderSearch
            size="lg"
            autoFocus
            onSelect={() => setMobileSearchOpen(false)}
          />
        </div>
      )}
    </header>
  );
}
