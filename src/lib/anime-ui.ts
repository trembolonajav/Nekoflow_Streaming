export interface AnimeEpisodeCard {
  id: string;
  number: number;
  title: string;
  durationMin: number;
  airDate: string;
  thumbnail: string;
  isNew?: boolean;
}

export interface AnimeSeasonCard {
  id: string;
  label: string;
  episodes: AnimeEpisodeCard[];
}

export interface AnimeDetailView {
  id: string;
  slug: string;
  title: string;
  altTitle?: string;
  year: number;
  status: "Em lançamento" | "Finalizado" | "Em hiato";
  studio: string;
  episodesCount: number;
  seasonsCount: number;
  averageDurationMin: number;
  ageRating: string;
  languages: ("DUB" | "LEG")[];
  genres: string[];
  rating: number;
  synopsisShort: string;
  synopsisLong: string;
  banner: string;
  poster: string;
  seasons: AnimeSeasonCard[];
}

export interface AnimeWatchProgress {
  episodeNumber: number;
  progressPercent: number;
  remainingMinutes: number;
}
