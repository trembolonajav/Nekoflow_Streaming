// Analytics em duas camadas: Umami (self-hosted, fonte da verdade) e GA4
// (opcional, de carona pela integração com o Search Console). Cada script só é
// injetado se a env correspondente estiver preenchida — sem env, custo zero.

declare global {
  interface Window {
    umami?: { track: (event: string, data?: Record<string, unknown>) => void };
    dataLayer?: unknown[];
    gtag?: (...args: unknown[]) => void;
  }
}

const UMAMI_SRC = import.meta.env.VITE_UMAMI_SRC as string | undefined;
const UMAMI_WEBSITE_ID = import.meta.env.VITE_UMAMI_WEBSITE_ID as string | undefined;
const GA4_ID = import.meta.env.VITE_GA4_ID as string | undefined;

export function initAnalytics() {
  if (UMAMI_SRC && UMAMI_WEBSITE_ID) {
    const script = document.createElement("script");
    script.defer = true;
    script.src = UMAMI_SRC;
    script.setAttribute("data-website-id", UMAMI_WEBSITE_ID);
    document.head.appendChild(script);
  }

  if (GA4_ID) {
    const script = document.createElement("script");
    script.async = true;
    script.src = `https://www.googletagmanager.com/gtag/js?id=${GA4_ID}`;
    document.head.appendChild(script);
    window.dataLayer = window.dataLayer || [];
    window.gtag = (...args: unknown[]) => {
      window.dataLayer!.push(args);
    };
    window.gtag("js", new Date());
    window.gtag("config", GA4_ID);
  }
}

/** Evento customizado nas duas ferramentas (o Umami trata pageviews sozinho). */
export function trackEvent(name: string, data?: Record<string, unknown>) {
  try {
    window.umami?.track(name, data);
    window.gtag?.("event", name, data ?? {});
  } catch {
    // analytics nunca pode quebrar a navegação
  }
}
