/**
 * Utilitários de mapeamento do AniList para o schema interno do admin.
 * A consulta HTTP ao AniList agora acontece no backend.
 */

// ---------- mapeamentos para o schema interno ----------

export function mapAniListFormatToType(
  format: string | null,
): "SERIES" | "MOVIE" | "OVA" | "SPECIAL" {
  switch (format) {
    case "MOVIE":
      return "MOVIE";
    case "OVA":
    case "ONA":
      return "OVA";
    case "SPECIAL":
      return "SPECIAL";
    default:
      return "SERIES"; // TV, TV_SHORT, MUSIC, etc.
  }
}

export function mapAniListStatusToInternal(
  status: string | null,
): "RELEASING" | "FINISHED" | "HIATUS" | "NOT_YET_RELEASED" {
  switch (status) {
    case "FINISHED":
      return "FINISHED";
    case "HIATUS":
      return "HIATUS";
    case "NOT_YET_RELEASED":
      return "NOT_YET_RELEASED";
    case "RELEASING":
    case "CANCELLED":
    default:
      return "RELEASING";
  }
}

const SEASON_PT: Record<string, string> = {
  WINTER: "Inverno",
  SPRING: "Primavera",
  SUMMER: "Verão",
  FALL: "Outono",
};

export function formatAniListSeason(
  season: string | null,
  year: number | null,
): string {
  if (season && year) return `${SEASON_PT[season] ?? season} ${year}`;
  if (year) return String(year);
  return "";
}

/**
 * Mapa EN→PT de gêneros do AniList para o preset interno.
 * AniList sempre retorna gêneros em inglês — esse mapa evita que sejam ignorados.
 */
const GENRE_EN_TO_PT: Record<string, string> = {
  action: "Ação",
  adventure: "Aventura",
  comedy: "Comédia",
  drama: "Drama",
  fantasy: "Fantasia",
  romance: "Romance",
  "sci-fi": "Sci-Fi",
  "slice of life": "Slice of Life",
  mystery: "Mistério",
  supernatural: "Sobrenatural",
  // sinônimos comuns
  thriller: "Mistério",
  horror: "Sobrenatural",
};

export function mapAniListGenresToPreset(
  genres: string[],
  preset: string[],
): string[] {
  const presetByLower = new Map(preset.map((g) => [g.toLowerCase(), g]));
  const mapped = new Set<string>();
  for (const g of genres) {
    const key = g.toLowerCase().trim();
    // 1) match direto (ex: "Romance", "Drama", "Sci-Fi")
    const direct = presetByLower.get(key);
    if (direct) {
      mapped.add(direct);
      continue;
    }
    // 2) match via tabela EN→PT
    const translated = GENRE_EN_TO_PT[key];
    if (translated && preset.includes(translated)) {
      mapped.add(translated);
    }
  }
  return Array.from(mapped);
}

/**
 * Traduz texto EN→PT usando o endpoint público do Google Translate.
 * Sem chave, sem auth — pensado pra uso pontual no admin.
 * Retorna o texto original em caso de falha.
 */
export async function translateToPortuguese(
  text: string,
  signal?: AbortSignal,
): Promise<string> {
  const trimmed = text.trim();
  if (!trimmed) return "";

  // Quebra em pedaços de ~4500 chars (limite prático da URL do endpoint)
  const chunks: string[] = [];
  const MAX = 4500;
  let remaining = trimmed;
  while (remaining.length > MAX) {
    // Tenta cortar em quebra de parágrafo/sentença
    let cut = remaining.lastIndexOf("\n\n", MAX);
    if (cut < MAX / 2) cut = remaining.lastIndexOf(". ", MAX);
    if (cut < MAX / 2) cut = MAX;
    chunks.push(remaining.slice(0, cut));
    remaining = remaining.slice(cut).trimStart();
  }
  if (remaining) chunks.push(remaining);

  const translated: string[] = [];
  for (const chunk of chunks) {
    const url =
      "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=pt&dt=t&q=" +
      encodeURIComponent(chunk);
    const res = await fetch(url, { signal });
    if (!res.ok) throw new Error(`Translate HTTP ${res.status}`);
    const json = await res.json();
    // Resposta: [[[ "trecho traduzido", "trecho original", ...], ...], ...]
    const sentences: string = Array.isArray(json?.[0])
      ? json[0]
          .map((s: unknown[]) => (Array.isArray(s) ? (s[0] as string) : ""))
          .join("")
      : "";
    translated.push(sentences || chunk);
  }
  return translated.join("\n\n").trim();
}
