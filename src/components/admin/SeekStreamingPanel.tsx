import { useEffect, useMemo, useState, type ReactNode } from "react";
import { ArrowLeft, Folder, Loader2, RefreshCw, Search, Video } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  buildSeekStreamingEmbedUrl,
  fetchAdminSeekStreamingFolders,
  fetchAdminSeekStreamingVideos,
  type AdminSeekStreamingFolderDto,
  type AdminSeekStreamingVideoDto,
} from "@/lib/backend-api";

export interface SeekStreamingPick {
  externalVideoId: string;
  embedUrl: string;
  thumbnailUrl?: string;
  previewUrl?: string;
  durationMinutes?: string;
  episodeNumber?: number;
  suggestedTitle?: string;
}

interface SeekStreamingPanelProps {
  open: boolean;
  onPick: (pick: SeekStreamingPick) => void;
}

export function SeekStreamingPanel({ open, onPick }: SeekStreamingPanelProps) {
  const [folders, setFolders] = useState<AdminSeekStreamingFolderDto[]>([]);
  const [videos, setVideos] = useState<AdminSeekStreamingVideoDto[]>([]);
  const [path, setPath] = useState<AdminSeekStreamingFolderDto[]>([]);
  const [query, setQuery] = useState("");
  const [loadingFolders, setLoadingFolders] = useState(false);
  const [loadingVideos, setLoadingVideos] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reloadTick, setReloadTick] = useState(0);

  const currentFolder = path[path.length - 1] ?? null;

  const loadFolders = async () => {
    setLoadingFolders(true);
    setError(null);
    try {
      const response = await fetchAdminSeekStreamingFolders();
      setFolders(response);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Falha ao carregar pastas da SeekStreaming.");
    } finally {
      setLoadingFolders(false);
    }
  };

  useEffect(() => {
    if (!open) return;
    setPath([]);
    setQuery("");
    setVideos([]);
    setError(null);
    void loadFolders();
  }, [open]);

  useEffect(() => {
    if (!open || !currentFolder) {
      setVideos([]);
      return;
    }

    const controller = new AbortController();
    setLoadingVideos(true);
    setError(null);

    void fetchAdminSeekStreamingVideos(currentFolder.id, controller.signal)
      .then(setVideos)
      .catch((loadError) => {
        if (controller.signal.aborted) return;
        setError(loadError instanceof Error ? loadError.message : "Falha ao carregar vídeos da pasta.");
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoadingVideos(false);
        }
      });

    return () => controller.abort();
  }, [currentFolder, open, reloadTick]);

  const childFolders = useMemo(() => {
    const parentId = currentFolder?.id ?? null;
    return folders
      .filter((folder) => (folder.parentId ?? null) === parentId)
      .sort((left, right) => left.name.localeCompare(right.name, "pt-BR"));
  }, [folders, currentFolder]);

  const filteredFolders = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    if (!normalizedQuery) return childFolders;
    return childFolders.filter((folder) => folder.name.toLowerCase().includes(normalizedQuery));
  }, [childFolders, query]);

  const filteredVideos = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    if (!normalizedQuery) return videos;
    return videos.filter((video) => video.name.toLowerCase().includes(normalizedQuery));
  }, [videos, query]);

  const handlePick = (video: AdminSeekStreamingVideoDto) => {
    onPick({
      externalVideoId: video.id,
      embedUrl: buildSeekStreamingEmbedUrl(video.id),
      thumbnailUrl: video.poster ?? undefined,
      previewUrl: video.preview ?? undefined,
      durationMinutes: toDurationMinutes(video.duration),
      episodeNumber: extractEpisodeNumber(video.name),
      suggestedTitle: suggestTitleFromFilename(video.name),
    });
  };

  const handleRefresh = async () => {
    if (currentFolder) {
      setReloadTick((value) => value + 1);
      return;
    }
    await loadFolders();
  };

  return (
    <section className="space-y-3 rounded-lg border border-gold/20 bg-gold/[0.03] p-4">
      <div className="flex items-center justify-between gap-3">
        <div className="flex min-w-0 items-center gap-2">
          {path.length > 0 ? (
            <Button
              type="button"
              variant="ghost"
              size="icon"
              onClick={() => setPath((previous) => previous.slice(0, -1))}
              className="size-7 shrink-0 text-ivory-muted hover:bg-surface-elevated hover:text-gold"
            >
              <ArrowLeft className="size-4" />
            </Button>
          ) : null}
          <h3 className="truncate text-[11px] font-medium uppercase tracking-[0.18em] text-gold/80">
            SeekStreaming
            {path.length === 0 ? " · Raiz" : path.map((folder) => ` · ${folder.name}`).join("")}
          </h3>
        </div>
        <div className="flex items-center gap-1">
          {path.length > 1 ? (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => setPath([])}
              className="h-7 px-2 text-[11px] text-ivory-muted hover:bg-surface-elevated hover:text-gold"
            >
              Raiz
            </Button>
          ) : null}
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => void handleRefresh()}
            disabled={loadingFolders || loadingVideos}
            className="h-7 gap-1.5 px-2 text-[11px] text-ivory-muted hover:bg-surface-elevated hover:text-gold"
          >
            <RefreshCw className={`size-3 ${loadingFolders || loadingVideos ? "animate-spin" : ""}`} />
            Atualizar
          </Button>
        </div>
      </div>

      <div className="relative">
        <Search className="pointer-events-none absolute left-3 top-1/2 size-3.5 -translate-y-1/2 text-ivory-muted" />
        <Input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder={currentFolder ? "Filtrar pastas e vídeos..." : "Filtrar pastas..."}
          className="h-9 border-border-subtle bg-surface-elevated/50 pl-9 text-sm text-ivory placeholder:text-ivory-muted/70"
        />
      </div>

      {error ? (
        <p className="rounded-md border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-xs text-rose-200">
          {error}
        </p>
      ) : null}

      <div className="max-h-72 overflow-y-auto rounded-md border border-border-subtle bg-onyx/40">
        {loadingFolders && folders.length === 0 ? (
          <EmptyRow icon={<Loader2 className="size-4 animate-spin" />} text="Carregando pastas..." />
        ) : (
          <ul className="divide-y divide-border-subtle/60">
            {filteredFolders.map((folder) => (
              <li key={folder.id}>
                <button
                  type="button"
                  onClick={() => setPath((previous) => [...previous, folder])}
                  className="flex w-full items-center gap-3 px-3 py-2 text-left text-sm text-ivory transition-colors hover:bg-surface-elevated/60"
                >
                  <Folder className="size-4 text-gold/80" />
                  <span className="flex-1 truncate">{folder.name}</span>
                  <span className="text-[11px] text-ivory-muted">
                    {describeFolder(folder)}
                  </span>
                </button>
              </li>
            ))}

            {currentFolder && loadingVideos ? (
              <li>
                <EmptyRow icon={<Loader2 className="size-4 animate-spin" />} text="Carregando vídeos..." />
              </li>
            ) : null}

            {filteredVideos.map((video) => (
              <li key={video.id}>
                <button
                  type="button"
                  onClick={() => handlePick(video)}
                  className="flex w-full items-center gap-3 px-3 py-2 text-left transition-colors hover:bg-surface-elevated/60"
                >
                  <div className="flex aspect-video w-16 shrink-0 items-center justify-center overflow-hidden rounded border border-border-subtle bg-onyx/60">
                    {video.poster ? (
                      <img src={video.poster} alt="" className="size-full object-cover" loading="lazy" />
                    ) : (
                      <Video className="size-4 text-ivory-muted" />
                    )}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-xs font-medium text-ivory">{video.name}</p>
                    <p className="text-[10px] text-ivory-muted">
                      {[formatDurationLabel(video.duration), formatResolution(video.width, video.height), video.id]
                        .filter(Boolean)
                        .join(" · ")}
                    </p>
                  </div>
                </button>
              </li>
            ))}

            {!loadingFolders && !loadingVideos && filteredFolders.length === 0 && filteredVideos.length === 0 ? (
              <li>
                <EmptyRow text={currentFolder ? "Nenhum item encontrado nesta pasta." : "Nenhuma pasta encontrada."} />
              </li>
            ) : null}
          </ul>
        )}
      </div>

      <p className="text-[10px] text-ivory-muted">
        Navegue até a temporada e clique em um vídeo para preencher ID, embed, thumb, preview, duração e sugestão de episódio.
      </p>
    </section>
  );
}

function EmptyRow({ text, icon }: { text: string; icon?: ReactNode }) {
  return (
    <div className="flex items-center justify-center gap-2 px-3 py-8 text-xs text-ivory-muted">
      {icon}
      {text}
    </div>
  );
}

function describeFolder(folder: AdminSeekStreamingFolderDto) {
  const parts = [
    folder.folderCount ? `${folder.folderCount} pasta${folder.folderCount > 1 ? "s" : ""}` : null,
    folder.videoCount ? `${folder.videoCount} vídeo${folder.videoCount > 1 ? "s" : ""}` : null,
  ].filter(Boolean);
  return parts.length > 0 ? parts.join(" · ") : "vazia";
}

function toDurationMinutes(seconds: number | null) {
  if (!seconds || seconds <= 0) return "";
  return String(Math.max(1, Math.round(seconds / 60)));
}

function formatDurationLabel(seconds: number | null) {
  const minutes = toDurationMinutes(seconds);
  return minutes ? `${minutes} min` : "";
}

function formatResolution(width: number | null, height: number | null) {
  if (!width || !height) return "";
  return `${width}x${height}`;
}

function extractEpisodeNumber(name: string) {
  const cleaned = name.replace(/\.[a-z0-9]{2,5}$/i, "");
  const patterns = [
    /\b(?:ep|episode|episodio|episódio)[\s._-]*(\d{1,3})\b/i,
    /S\d{1,2}E(\d{1,3})/i,
    /-\s*(\d{1,3})(?:\s|$)/,
  ];

  for (const pattern of patterns) {
    const match = cleaned.match(pattern);
    if (match) {
      return Number(match[1]);
    }
  }

  const isolatedNumbers = cleaned.match(/(?:^|[\s._-])(\d{1,3})(?=[\s._-]|$)/g);
  if (!isolatedNumbers?.length) return undefined;
  const last = isolatedNumbers[isolatedNumbers.length - 1].match(/(\d{1,3})/);
  return last ? Number(last[1]) : undefined;
}

function suggestTitleFromFilename(name: string) {
  return name
    .replace(/\.[a-z0-9]{2,5}$/i, "")
    .replace(/\[[^\]]*]/g, " ")
    .replace(/\([^)]*\)/g, " ")
    .replace(/\b(1080p|720p|480p|2160p|4k|hevc|x264|x265|hdr|hi10p|10bit|web-?dl|bluray|bdrip|webrip)\b/gi, " ")
    .replace(/\b[a-f0-9]{8}\b/gi, " ")
    .replace(/[._]+/g, " ")
    .replace(/\s*-\s*\d{1,3}\s*$/, "")
    .replace(/\s{2,}/g, " ")
    .trim();
}
