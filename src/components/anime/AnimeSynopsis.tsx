import { OrnamentDivider } from "@/components/layout/OrnamentDivider";
import type { AnimeDetailView } from "@/lib/anime-ui";

interface AnimeSynopsisProps {
  anime: AnimeDetailView;
}

/**
 * Bloco editorial de sinopse longa, em coluna única e respiro generoso.
 */
export function AnimeSynopsis({ anime }: AnimeSynopsisProps) {
  return (
    <section
      aria-label="Sinopse completa"
      className="mx-auto w-full max-w-[1400px] px-6 pb-12 md:px-10 md:pb-16"
    >
      <div className="mx-auto max-w-3xl">
        <span className="text-[11px] font-medium uppercase tracking-[0.32em] text-gold">
          Sinopse
        </span>
        <p className="mt-5 font-serif text-xl leading-[1.45] text-ivory md:text-2xl">
          {anime.synopsisLong}
        </p>
        <OrnamentDivider width="sm" className="mt-10 opacity-70" />
      </div>
    </section>
  );
}
