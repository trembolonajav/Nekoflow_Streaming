import { useEffect } from "react";
import { useLocation } from "react-router-dom";

import { applySeo } from "@/lib/seo";

const ROUTE_SEO: Record<string, { title: string; description: string; noindex?: boolean }> = {
  "/": {
    title: "Streaming de anime premium",
    description: "Assista animes, acompanhe lancamentos e descubra uma curadoria editorial na Nekoflow.",
  },
  "/explorar": {
    title: "Explorar animes",
    description: "Explore o catalogo de animes da Nekoflow por genero, ano, status e formato.",
  },
  "/calendario": {
    title: "Calendario de lancamentos",
    description: "Veja a agenda semanal de episodios e lancamentos publicados na Nekoflow.",
  },
  "/entrar": {
    title: "Entrar",
    description: "Entre na sua conta Nekoflow para acompanhar progresso, lista e preferencias.",
    noindex: true,
  },
  "/perfil": {
    title: "Meu perfil",
    description: "Gerencie progresso, historico, comentarios, lista e preferencias da sua conta Nekoflow.",
    noindex: true,
  },
  "/notificacoes": {
    title: "Notificacoes",
    description: "Acompanhe notificacoes da sua conta Nekoflow.",
    noindex: true,
  },
  "/termos-de-uso": {
    title: "Termos de uso",
    description: "Consulte os termos de uso da plataforma Nekoflow.",
  },
  "/politica-de-privacidade": {
    title: "Politica de privacidade",
    description: "Entenda como a Nekoflow trata dados e privacidade dos usuarios.",
  },
};

export function RouteSeo() {
  const location = useLocation();

  useEffect(() => {
    const route = ROUTE_SEO[location.pathname];
    if (route) {
      applySeo({ ...route, path: location.pathname });
      return;
    }

    if (location.pathname.startsWith("/admin")) {
      applySeo({
        title: "Painel administrativo",
        description: "Area administrativa da Nekoflow.",
        path: location.pathname,
        noindex: true,
      });
      return;
    }

    if (location.pathname.startsWith("/anime/")) {
      applySeo({
        title: "Anime",
        description: "Detalhes do anime no catalogo da Nekoflow.",
        path: location.pathname,
      });
      return;
    }

    if (location.pathname.startsWith("/watch/")) {
      applySeo({
        title: "Assistir episodio",
        description: "Player de episodio da Nekoflow.",
        path: location.pathname,
        noindex: true,
      });
      return;
    }

    applySeo({
      title: "Pagina nao encontrada",
      description: "A pagina solicitada nao foi encontrada na Nekoflow.",
      path: location.pathname,
      noindex: true,
    });
  }, [location.pathname]);

  return null;
}
