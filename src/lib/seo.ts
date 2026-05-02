const SITE_NAME = "Nekoflow";
const SITE_URL = "https://nekoflow.com.br";
const DEFAULT_TITLE = "Nekoflow - Streaming de anime premium";
const DEFAULT_DESCRIPTION =
  "Nekoflow e uma plataforma editorial de streaming de anime, com curadoria, lancamentos da temporada e catalogo organizado.";

interface SeoOptions {
  title?: string;
  description?: string;
  path?: string;
  noindex?: boolean;
}

export function applySeo({ title, description, path, noindex = false }: SeoOptions) {
  const nextTitle = title ? `${title} | ${SITE_NAME}` : DEFAULT_TITLE;
  const nextDescription = description ?? DEFAULT_DESCRIPTION;
  document.title = nextTitle;
  setMeta("description", nextDescription);
  setMeta("robots", noindex ? "noindex,nofollow" : "index,follow");
  setCanonical(path ? `${SITE_URL}${path}` : SITE_URL);
}

export function buildDescription(value: string | null | undefined, fallback = DEFAULT_DESCRIPTION) {
  if (!value) return fallback;
  return value.replace(/<[^>]*>/g, "").replace(/\s+/g, " ").trim().slice(0, 155);
}

function setMeta(name: string, content: string) {
  let element = document.querySelector<HTMLMetaElement>(`meta[name="${name}"]`);
  if (!element) {
    element = document.createElement("meta");
    element.name = name;
    document.head.appendChild(element);
  }
  element.content = content;
}

function setCanonical(href: string) {
  let element = document.querySelector<HTMLLinkElement>('link[rel="canonical"]');
  if (!element) {
    element = document.createElement("link");
    element.rel = "canonical";
    document.head.appendChild(element);
  }
  element.href = href;
}
