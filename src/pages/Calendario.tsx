import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import {
  Calendar as CalendarIcon,
  ChevronLeft,
  ChevronRight,
  Clock,
  Play,
  Sparkles,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { OrnamentDivider } from "@/components/layout/OrnamentDivider";
import { cn } from "@/lib/utils";
import { fetchCalendar, type CalendarDayDto, type CalendarReleaseDto } from "@/lib/backend-api";

function toIsoDate(date: Date) {
  return date.toISOString().slice(0, 10);
}

function startOfWeek(date: Date) {
  const copy = new Date(date);
  const day = copy.getUTCDay();
  const diff = day === 0 ? -6 : 1 - day;
  copy.setUTCDate(copy.getUTCDate() + diff);
  return copy;
}

function addDays(date: Date, days: number) {
  const copy = new Date(date);
  copy.setUTCDate(copy.getUTCDate() + days);
  return copy;
}

function CalendarPage() {
  const [referenceDate, setReferenceDate] = useState(() => startOfWeek(new Date()));
  const [selectedDayIso, setSelectedDayIso] = useState<string | null>(null);
  const weekStart = useMemo(() => toIsoDate(referenceDate), [referenceDate]);
  const calendarQuery = useQuery({
    queryKey: ["calendar-week", weekStart],
    queryFn: () => fetchCalendar(weekStart),
  });

  const week = calendarQuery.data;
  const activeDay = useMemo(() => {
    if (!week) return null;
    if (selectedDayIso) {
      const selected = week.days.find((day) => day.dateIso === selectedDayIso);
      if (selected) return selected;
    }
    return week.days.find((day) => day.isToday) ?? week.days[0] ?? null;
  }, [selectedDayIso, week]);

  const goPrevWeek = () => {
    setSelectedDayIso(null);
    setReferenceDate((current) => addDays(current, -7));
  };

  const goNextWeek = () => {
    setSelectedDayIso(null);
    setReferenceDate((current) => addDays(current, 7));
  };

  const goToday = () => {
    setSelectedDayIso(null);
    setReferenceDate(startOfWeek(new Date()));
  };

  return (
    <div className="relative flex min-h-screen flex-col bg-background">
      <Header />

      <main className="relative flex-1 pb-16 pt-10 md:pt-14">
        <div className="mx-auto w-full max-w-[1400px] px-4 md:px-10">
          {week ? (
            <>
              <CalendarHeader
                season={week.season}
                year={week.year}
                rangeLabel={week.rangeLabel}
                totalReleases={week.totalReleases}
                onPrev={goPrevWeek}
                onNext={goNextWeek}
                onToday={goToday}
              />

              <OrnamentDivider className="my-8" />

              <WeekTimeline
                days={week.days}
                activeDateIso={activeDay?.dateIso ?? ""}
                onSelect={setSelectedDayIso}
              />

              {activeDay ? <DayFocus day={activeDay} /> : null}
            </>
          ) : (
            <div className="py-24 text-center text-sm text-ivory-muted">
              {calendarQuery.isLoading ? "Carregando calendário..." : "Não foi possível carregar o calendário."}
            </div>
          )}
        </div>
      </main>

      <Footer />
    </div>
  );
}

function CalendarHeader({
  season,
  year,
  rangeLabel,
  totalReleases,
  onPrev,
  onNext,
  onToday,
}: {
  season: string;
  year: number;
  rangeLabel: string;
  totalReleases: number;
  onPrev: () => void;
  onNext: () => void;
  onToday: () => void;
}) {
  return (
    <header className="flex flex-col gap-6 md:flex-row md:items-end md:justify-between">
      <div>
        <span className="inline-flex items-center gap-2 rounded-full border border-gold/25 bg-gold/5 px-3 py-1 font-mono text-[10px] uppercase tracking-[0.22em] text-gold">
          <CalendarIcon className="h-3 w-3" />
          Temporada de {season} · {year}
        </span>
        <h1 className="mt-4 font-serif text-4xl font-medium leading-tight text-ivory md:text-5xl">
          Calendário da semana
        </h1>
        <p className="mt-2 max-w-xl text-sm leading-relaxed text-ivory-muted md:text-base">
          Estreias e episódios já publicados na grade real do catálogo.
        </p>
      </div>

      <div className="flex flex-col items-start gap-3 md:items-end">
        <span className="font-mono text-[11px] uppercase tracking-[0.22em] text-ivory-muted">
          {rangeLabel} · {String(totalReleases).padStart(2, "0")} {totalReleases === 1 ? "estreia" : "estreias"}
        </span>
        <div className="flex items-center gap-2">
          <WeekArrow direction="prev" onClick={onPrev} />
          <Button
            variant="outline"
            onClick={onToday}
            className="h-9 border-gold/30 bg-gold/5 px-4 font-mono text-[11px] uppercase tracking-[0.18em] text-gold shadow-none transition-all hover:border-gold/60 hover:bg-gold/10 hover:text-gold"
          >
            Hoje
          </Button>
          <WeekArrow direction="next" onClick={onNext} />
        </div>
      </div>
    </header>
  );
}

function WeekArrow({ direction, onClick }: { direction: "prev" | "next"; onClick: () => void }) {
  const Icon = direction === "prev" ? ChevronLeft : ChevronRight;
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={direction === "prev" ? "Semana anterior" : "Próxima semana"}
      className="inline-flex h-9 w-9 items-center justify-center rounded-full border border-border-subtle text-ivory transition-all duration-200 hover:border-gold/60 hover:text-gold"
    >
      <Icon className="h-4 w-4" />
    </button>
  );
}

function WeekTimeline({
  days,
  activeDateIso,
  onSelect,
}: {
  days: CalendarDayDto[];
  activeDateIso: string;
  onSelect: (dateIso: string) => void;
}) {
  return (
    <nav aria-label="Selecionar dia da semana" className="-mx-4 overflow-x-auto px-4 pb-1 md:mx-0 md:overflow-visible md:px-0">
      <ul className="flex min-w-max gap-2 md:grid md:min-w-0 md:grid-cols-7 md:gap-3">
        {days.map((day) => {
          const isActive = day.dateIso === activeDateIso;
          const dateNumber = new Date(day.dateIso).getUTCDate();
          const count = day.releases.length;
          return (
            <li key={day.dateIso} className="flex">
              <button
                type="button"
                onClick={() => onSelect(day.dateIso)}
                aria-pressed={isActive}
                className={cn(
                  "group/day relative flex w-[88px] flex-col items-center gap-1 rounded-xl border px-3 py-3 transition-all duration-200 md:w-full",
                  isActive
                    ? "border-gold/50 bg-gold/10 shadow-[0_8px_30px_-14px_var(--gold-glow)]"
                    : day.isToday
                      ? "border-gold/25 bg-surface/60 hover:border-gold/45"
                      : "border-border-subtle bg-surface/40 hover:border-gold/30 hover:bg-surface/70",
                )}
              >
                <span className={cn("font-mono text-[10px] uppercase tracking-[0.22em]", isActive ? "text-gold" : "text-ivory-muted")}>
                  {day.shortLabel}
                </span>
                <span className={cn("font-serif text-2xl leading-none", isActive || day.isToday ? "text-gold" : "text-ivory")}>
                  {String(dateNumber).padStart(2, "0")}
                </span>
                <span className={cn("mt-1 inline-flex items-center gap-1 font-mono text-[9px] uppercase tracking-[0.18em]", count === 0 ? "text-ivory-muted/50" : isActive ? "text-gold/90" : "text-ivory-muted/80")}>
                  {count === 0 ? "—" : `${String(count).padStart(2, "0")} ${count === 1 ? "ep" : "eps"}`}
                </span>
                {day.isToday ? (
                  <span className={cn("absolute -top-2 right-3 rounded-full px-1.5 py-px font-mono text-[8px] font-bold uppercase tracking-[0.2em]", isActive ? "bg-gold text-onyx" : "bg-onyx text-gold ring-1 ring-gold/40")}>
                    hoje
                  </span>
                ) : null}
              </button>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}

function DayFocus({ day }: { day: CalendarDayDto }) {
  const date = new Date(day.dateIso);
  const longDate = date.toLocaleDateString("pt-BR", { day: "2-digit", month: "long", timeZone: "UTC" });

  return (
    <section aria-label={`Lançamentos de ${day.label}`} className="mt-10">
      <header className="mb-6 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <span className="font-mono text-[10px] uppercase tracking-[0.22em] text-gold">
            {day.isToday ? "Hoje" : day.shortLabel}
          </span>
          <h2 className="mt-1 font-serif text-3xl font-medium leading-tight text-ivory md:text-4xl">
            {day.label}
            <span className="ml-3 text-ivory-muted/70">{longDate}</span>
          </h2>
        </div>
        <span className="font-mono text-[11px] uppercase tracking-[0.22em] text-ivory-muted">
          {day.releases.length === 0 ? "sem estreias" : `${String(day.releases.length).padStart(2, "0")} ${day.releases.length === 1 ? "estreia" : "estreias"}`}
        </span>
      </header>

      {day.releases.length === 0 ? (
        <div className="flex flex-col items-center justify-center gap-3 rounded-2xl border border-dashed border-border-subtle bg-surface/40 px-6 py-20 text-center">
          <Sparkles className="h-6 w-6 text-gold/60" />
          <p className="font-serif text-xl text-ivory">Dia silencioso</p>
          <p className="max-w-sm text-sm leading-relaxed text-ivory-muted">
            Nenhuma estreia agendada para este dia.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
          {day.releases.map((release) => (
            <ReleaseCard key={release.id} release={release} />
          ))}
        </div>
      )}
    </section>
  );
}

function ReleaseCard({ release }: { release: CalendarReleaseDto }) {
  const isAired = release.status === "aired";
  const target = isAired ? `/watch/${release.slug}/${String(release.episodeNumber)}` : `/anime/${release.slug}`;

  return (
    <Link
      to={target}
      aria-label={`Abrir ${release.animeTitle} episódio ${release.episodeNumber}`}
      className="group/release flex flex-col overflow-hidden rounded-2xl border border-border-subtle bg-surface/60 transition-all duration-300 hover:border-gold/40 hover:shadow-[0_14px_40px_-18px_var(--gold-glow)]"
    >
      <div className="relative aspect-video w-full overflow-hidden">
        {release.thumbnail ? (
          <img src={release.thumbnail} alt="" loading="lazy" className="h-full w-full object-cover transition-transform duration-500 group-hover/release:scale-[1.04]" />
        ) : null}
        <div className="absolute inset-0 bg-gradient-to-t from-onyx/95 via-onyx/30 to-transparent" />

        <div className="absolute inset-0 flex items-center justify-center opacity-0 transition-opacity duration-300 group-hover/release:opacity-100">
          <span className="inline-flex h-14 w-14 items-center justify-center rounded-full border border-gold/60 bg-onyx/60 text-gold backdrop-blur">
            <Play className="h-5 w-5 fill-gold" />
          </span>
        </div>

        <span className={cn("absolute left-3 top-3 inline-flex items-center gap-1.5 rounded-md px-2 py-1 font-mono text-[11px] font-semibold tracking-wider backdrop-blur", isAired ? "bg-onyx/75 text-ivory-muted" : "bg-gold/95 text-onyx")}>
          <Clock className="h-3 w-3" />
          {release.time.slice(0, 5)}
          <span className={cn("ml-1 border-l pl-1.5 text-[9px] uppercase tracking-[0.18em]", isAired ? "border-ivory-muted/30 text-ivory-muted/80" : "border-onyx/40 text-onyx")}>
            {isAired ? "já saiu" : "em breve"}
          </span>
        </span>

        <span className="absolute right-3 top-3 rounded-md bg-onyx/75 px-2 py-1 font-mono text-[10px] font-semibold tracking-wider text-ivory backdrop-blur">
          {release.language}
        </span>

        <span className="absolute bottom-3 left-3 inline-flex items-center gap-1 rounded-sm bg-gold/95 px-2 py-0.5 font-mono text-[10px] font-bold tracking-[0.18em] text-onyx">
          EP {String(release.episodeNumber).padStart(2, "0")}
        </span>
      </div>

      <div className="flex flex-1 flex-col gap-3 p-4">
        <div className="flex flex-col gap-1">
          <h3 className="font-serif text-xl font-medium leading-tight text-ivory transition-colors group-hover/release:text-gold">
            {release.animeTitle}
          </h3>
          <span className="font-mono text-[10px] uppercase tracking-[0.2em] text-ivory-muted/80">
            {release.studio}
          </span>
        </div>

        <p className="line-clamp-2 text-sm leading-relaxed text-ivory-muted">{release.synopsisShort}</p>

        <div className="mt-auto flex flex-wrap items-center gap-1.5 pt-1">
          {release.genres.map((genre) => (
            <span key={genre} className="rounded-full border border-border-subtle bg-onyx/40 px-2 py-0.5 font-mono text-[9px] uppercase tracking-[0.16em] text-ivory-muted">
              {genre}
            </span>
          ))}
        </div>
      </div>
    </Link>
  );
}

export default CalendarPage;
