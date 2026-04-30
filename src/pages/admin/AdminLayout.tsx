import { Outlet, Link } from "react-router-dom";

import { AdminSidebar } from "@/components/admin/AdminSidebar";
import { AdminTopbar } from "@/components/admin/AdminTopbar";
import { useAuth } from "@/hooks/use-auth";

function AdminLayout() {
  const { user, isReady } = useAuth();

  if (!isReady) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-onyx px-4">
        <div className="size-8 animate-pulse rounded-full border border-gold/30 bg-gold/10" />
      </div>
    );
  }

  if (user?.role !== "admin") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-onyx px-4">
        <div className="max-w-md rounded-xl border border-border-subtle bg-surface/60 p-8 text-center backdrop-blur-sm">
          <p className="text-[10px] uppercase tracking-[0.32em] text-gold">Restrito</p>
          <h1 className="mt-3 font-serif text-2xl text-ivory">Painel administrativo</h1>
          <p className="mt-2 text-sm text-ivory-muted">
            É preciso entrar com uma conta administrativa para acessar esta área.
          </p>
          <div className="mt-6 flex items-center justify-center gap-3">
            <Link
              to="/entrar"
              className="inline-flex h-10 items-center rounded-md bg-gold px-5 text-sm font-medium text-onyx transition-colors hover:bg-gold/90"
            >
              Entrar
            </Link>
            <Link
              to="/"
              className="inline-flex h-10 items-center rounded-md border border-border-subtle px-5 text-sm font-medium text-ivory transition-colors hover:bg-surface-elevated"
            >
              Voltar
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen w-full bg-onyx text-ivory">
      <AdminSidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <AdminTopbar />
        <main className="flex-1 px-6 py-6 lg:px-8 lg:py-8">
          <Outlet />
        </main>
        <footer className="border-t border-border-subtle px-6 py-4 text-[11px] text-ivory-muted lg:px-8">
          <div className="flex items-center justify-between">
            <span>© 2026 NekoFlow Admin. Todos os direitos reservados.</span>
            <span>Versão 1.0.0</span>
          </div>
        </footer>
      </div>
    </div>
  );
}

export default AdminLayout;
