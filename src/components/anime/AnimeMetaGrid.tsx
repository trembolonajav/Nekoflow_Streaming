import {
  Calendar,
  Clock,
  Film,
  Languages,
  Layers,
  ShieldAlert,
  Tv,
} from "lucide-react";
import type { AnimeDetailView } from "@/lib/anime-ui";

interface AnimeMetaGridProps {
  anime: AnimeDetailView;
}

/**
 * Bloco de metadados estruturado: 7 fichas em grid editorial.
 * Evita que o usuário tenha que "caçar informação".
 */
export function AnimeMetaGrid({ anime }: AnimeMetaGridProps) {
  const items = [
    { icon: Tv, label: "Episódios", value: String(anime.episodesCount) },
    { icon: Layers, label: "Temporadas", value: String(anime.seasonsCount) },
    { icon: Calendar, label: "Status", value: anime.status },
    { icon: Film, label: "Estúdio", value: anime.studio },
    {
      icon: Clock,
      label: "Duração média",
      value: `${anime.averageDurationMin} min`,
    },
    {
      icon: ShieldAlert,
      label: "Classificação",
      value: anime.ageRating,
    },
    {
      icon: Languages,
      label: "Idiomas",
      value: anime.languages.join(" · "),
    },
  ];

  return (
    <section
      aria-label="Informações do anime"
      className="mx-auto w-full max-w-[1400px] px-6 py-10 md:px-10 md:py-12"
    >
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-7">
        {items.map(({ icon: Icon, label, value }) => (
          <div
            key={label}
            className="group/meta flex flex-col gap-2 rounded-xl border border-border-subtle bg-surface/60 px-4 py-4 backdrop-blur transition-colors duration-300 hover:border-gold/30"
          >
            <span className="inline-flex items-center gap-2 text-[10px] uppercase tracking-[0.22em] text-ivory-muted">
              <Icon className="h-3.5 w-3.5 text-gold/80" />
              {label}
            </span>
            <span className="font-serif text-base font-medium leading-tight text-ivory">
              {value}
            </span>
          </div>
        ))}
      </div>
    </section>
  );
}
