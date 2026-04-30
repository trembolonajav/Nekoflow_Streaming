import { getStoredAccessToken } from "@/hooks/use-auth";

const API_BASE_URL = `${import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080"}/api/v1`;
const SEEKSTREAMING_EMBED_BASE = "https://nekoflow.embedseek.com/#";

export interface ApiPageResponse<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}

export interface AdminAnimeDto {
  id: string;
  anilistId: number | null;
  slug: string;
  titleDisplay: string;
  titleRomaji: string | null;
  titleNative: string | null;
  titleEnglish: string | null;
  synopsis: string | null;
  type: string;
  status: string;
  visibility: string;
  seasonLabel: string | null;
  year: number | null;
  coverUrl: string | null;
  bannerUrl: string | null;
  studio: string | null;
  genres: string[];
  episodesCount: number;
  updatedAt: string | null;
}

export interface AdminEpisodeDto {
  id: string;
  animeId: string;
  animeTitle: string;
  number: number;
  title: string;
  summary: string | null;
  durationSeconds: number | null;
  thumbnailUrl: string | null;
  previewUrl: string | null;
  status: string;
  scheduledFor: string | null;
  provider: string | null;
  externalVideoId: string | null;
  embedUrl: string | null;
  playerUrl: string | null;
  updatedAt: string | null;
}

export interface AdminHomeSectionDto {
  code: string;
  title: string;
  mode: string;
  active: boolean;
  sortOrder: number;
  manualItemIds: string[];
}

export interface AdminHomeDto {
  hero: {
    animeIds: string[];
    tag: string | null;
    ctaLabel: string | null;
  };
  sections: AdminHomeSectionDto[];
}

export interface HomeItemDto {
  id: string;
  animeId: string | null;
  episodeId: string | null;
  title: string;
  subtitle: string | null;
  coverUrl: string | null;
  bannerUrl: string | null;
  previewUrl: string | null;
  slug: string | null;
}

export interface PublicHomeDto {
  hero: {
    tag: string;
    ctaLabel: string;
    items: HomeItemDto[];
  };
  sections: Array<{
    code: string;
    title: string;
    mode: string;
    active: boolean;
    items: HomeItemDto[];
  }>;
}

export interface AnimeEpisodeSummaryDto {
  id: string;
  number: number;
  title: string;
  status: string;
  thumbnailUrl: string | null;
  durationSeconds: number | null;
}

export interface AnimeDetailDto {
  id: string;
  slug: string;
  anilistId: number | null;
  titleDisplay: string;
  titleRomaji: string | null;
  titleNative: string | null;
  titleEnglish: string | null;
  synopsis: string | null;
  type: string;
  status: string;
  visibility: string;
  seasonLabel: string | null;
  year: number | null;
  coverUrl: string | null;
  bannerUrl: string | null;
  studio: string | null;
  genres: string[];
  episodes: AnimeEpisodeSummaryDto[];
}

export interface AnimeSearchResultDto {
  id: string;
  slug: string;
  title: string;
  altTitle: string | null;
  poster: string | null;
  meta: string;
  genres: string[];
}

export interface WatchPlayerDto {
  animeSlug: string;
  animeTitle: string;
  episodeId: string;
  episodeNumber: number;
  episodeTitle: string;
  summary: string | null;
  thumbnailUrl: string | null;
  provider: string | null;
  embedUrl: string | null;
  playerUrl: string | null;
  durationSeconds: number | null;
}

export interface CommentAuthorDto {
  id: string;
  name: string;
  handle: string;
  avatarUrl: string | null;
  badge: string | null;
}

export interface CommentDto {
  id: string;
  animeId: string;
  episodeId: string;
  body: string;
  containsSpoiler: boolean;
  createdAt: string | null;
  user: CommentAuthorDto;
  replies: CommentDto[];
}

export interface ContinueWatchingDto {
  animeId: string;
  animeSlug: string;
  animeTitle: string;
  episodeId: string;
  episodeNumber: number;
  episodeTitle: string;
  thumbnailUrl: string | null;
  progressSeconds: number;
  progressPercent: number;
  remainingMinutes: number;
}

export interface ProfileDto {
  id: string;
  name: string;
  email: string;
  avatarUrl: string | null;
  stats: {
    continueWatchingCount: number;
    watchlistCount: number;
    historyCount: number;
    commentCount: number;
  };
  continueWatching: ContinueWatchingDto[];
}

export interface WatchlistItemDto {
  id: string;
  animeId: string;
  animeSlug: string;
  title: string;
  coverUrl: string | null;
  status: string;
  createdAt: string | null;
}

export interface HistoryItemDto {
  id: string;
  animeId: string;
  animeSlug: string;
  animeTitle: string;
  episodeId: string;
  episodeNumber: number;
  episodeTitle: string;
  thumbnailUrl: string | null;
  watchedAt: string;
}

export interface UserPreferencesDto {
  autoplay: boolean;
  autoNext: boolean;
  preferredAudio: string;
  preferredSubtitle: string;
  preferredQuality: string;
  notifyReleases: boolean;
  notifyNewEpisodes: boolean;
  notifyWatchlist: boolean;
  notifyMarketing: boolean;
}

export interface CalendarReleaseDto {
  id: string;
  slug: string;
  animeTitle: string;
  episodeNumber: number;
  thumbnail: string | null;
  poster: string | null;
  synopsisShort: string;
  genres: string[];
  time: string;
  airDateIso: string;
  language: string;
  status: "aired" | "upcoming";
  studio: string;
}

export interface CalendarDayDto {
  index: number;
  label: string;
  shortLabel: string;
  dateIso: string;
  isToday: boolean;
  releases: CalendarReleaseDto[];
}

export interface CalendarWeekDto {
  weekStartIso: string;
  weekEndIso: string;
  rangeLabel: string;
  season: string;
  year: number;
  days: CalendarDayDto[];
  totalReleases: number;
}

export interface AdminDashboardMetricDto {
  key: string;
  label: string;
  value: string;
  delta: string | null;
  trend: string | null;
}

export interface AdminDashboardPublicationDto {
  id: string;
  title: string;
  subtitle: string;
  type: string;
  status: string;
  updatedAt: string;
  thumb: string | null;
}

export interface AdminDashboardSectionDto {
  id: string;
  name: string;
  mode: string;
  items: number;
  active: boolean;
}

export interface AdminDashboardHealthDto {
  id: string;
  title: string;
  description: string;
  count: number;
  severity: string;
}

export interface AdminReportDto {
  id: string;
  commentId: string;
  user: string;
  userInitial: string;
  context: string;
  reason: string;
  body: string;
  reportCount: number;
  createdAt: string | null;
  status: string;
}

export interface AdminSuggestionDto {
  id: string;
  rank: number;
  title: string;
  votes: number;
  status: string;
  note: string | null;
  createdAt: string | null;
}

export interface AdminDashboardDto {
  metrics: AdminDashboardMetricDto[];
  recentPublications: AdminDashboardPublicationDto[];
  homeSections: AdminDashboardSectionDto[];
  reports: AdminReportDto[];
  suggestions: AdminSuggestionDto[];
  health: AdminDashboardHealthDto[];
}

export interface AdminAniListSearchDto {
  id: number;
  titleRomaji: string;
  titleEnglish: string | null;
  titleNative: string | null;
  format: string | null;
  status: string | null;
  episodes: number | null;
  seasonYear: number | null;
  season: string | null;
  coverImage: string | null;
  bannerImage: string | null;
  averageScore: number | null;
  description: string | null;
  genres: string[];
  studios: string[];
}

export interface AdminSeekStreamingFolderDto {
  id: string;
  name: string;
  parentId: string | null;
  folderCount: number | null;
  videoCount: number | null;
}

export interface AdminSeekStreamingVideoDto {
  id: string;
  name: string;
  status: string | null;
  width: number | null;
  height: number | null;
  size: number | null;
  duration: number | null;
  poster: string | null;
  preview: string | null;
  folderId: string | null;
}

export interface AdminAnimePayload {
  anilistId: number | null;
  slug: string;
  titleDisplay: string;
  titleRomaji: string | null;
  titleNative: string | null;
  titleEnglish: string | null;
  synopsis: string | null;
  type: string;
  status: string;
  visibility: string;
  seasonLabel: string | null;
  year: number | null;
  coverUrl: string | null;
  bannerUrl: string | null;
  studio: string | null;
  genres: string[];
}

export interface AdminEpisodePayload {
  animeId: string;
  number: number;
  title: string;
  summary: string | null;
  durationSeconds: number | null;
  thumbnailUrl: string | null;
  previewUrl: string | null;
  status: string;
  scheduledFor: string | null;
  provider: string;
  externalVideoId: string | null;
  embedUrl: string | null;
  playerUrl: string | null;
}

async function apiRequest<T>(path: string, init?: RequestInit, authenticated = false): Promise<T> {
  const token = authenticated ? getStoredAccessToken() : null;
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init?.headers ?? {}),
    },
  });

  if (!response.ok) {
    let message = "Falha na comunicação com o backend.";
    try {
      const payload = await response.json() as { message?: string; error?: string; detail?: string; title?: string };
      message = payload.message ?? payload.error ?? payload.detail ?? payload.title ?? message;
    } catch {
      message = response.statusText || message;
    }
    throw new Error(message);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export function fetchPublicHome() {
  return apiRequest<PublicHomeDto>("/home");
}

export function fetchAnimeDetail(slug: string) {
  return apiRequest<AnimeDetailDto>(`/animes/${slug}`);
}

export async function searchAnimeCatalog(query: string) {
  const response = await apiRequest<ApiPageResponse<AnimeSearchResultDto>>(`/animes?q=${encodeURIComponent(query)}&size=6`);
  return response.items;
}

export function fetchWatchPlayer(slug: string, episodeNumber: string) {
  return apiRequest<WatchPlayerDto>(`/watch/${slug}/${episodeNumber}`);
}

export function fetchAnimeComments(slug: string) {
  return apiRequest<CommentDto[]>(`/animes/${slug}/comments`);
}

export function fetchEpisodeComments(episodeId: string) {
  return apiRequest<CommentDto[]>(`/episodes/${episodeId}/comments`);
}

export function createEpisodeComment(episodeId: string, payload: { body: string; containsSpoiler?: boolean }) {
  return apiRequest<CommentDto>(`/episodes/${episodeId}/comments`, {
    method: "POST",
    body: JSON.stringify(payload),
  }, true);
}

export function createCommentReply(commentId: string, payload: { body: string; containsSpoiler?: boolean }) {
  return apiRequest<CommentDto>(`/comments/${commentId}/replies`, {
    method: "POST",
    body: JSON.stringify(payload),
  }, true);
}

export function fetchProfile() {
  return apiRequest<ProfileDto>("/me/profile", undefined, true);
}

export function fetchWatchlist() {
  return apiRequest<WatchlistItemDto[]>("/me/watchlist", undefined, true);
}

export function addToWatchlist(animeId: string) {
  return apiRequest<WatchlistItemDto>(`/me/watchlist/${animeId}`, { method: "POST" }, true);
}

export function removeFromWatchlist(animeId: string) {
  return apiRequest<{ message: string }>(`/me/watchlist/${animeId}`, { method: "DELETE" }, true);
}

export function fetchHistory() {
  return apiRequest<HistoryItemDto[]>("/me/history", undefined, true);
}

export function deleteHistoryItem(historyId: string) {
  return apiRequest<{ message: string }>(`/me/history/${historyId}`, { method: "DELETE" }, true);
}

export function clearHistory() {
  return apiRequest<{ message: string }>("/me/history", { method: "DELETE" }, true);
}

export function fetchPreferences() {
  return apiRequest<UserPreferencesDto>("/me/preferences", undefined, true);
}

export function updatePreferences(payload: UserPreferencesDto) {
  return apiRequest<UserPreferencesDto>("/me/preferences", {
    method: "PUT",
    body: JSON.stringify(payload),
  }, true);
}

export function updateProgress(payload: {
  animeId: string;
  episodeId: string;
  progressSeconds: number;
  durationSeconds: number;
}) {
  return apiRequest<{ message: string }>("/me/progress", {
    method: "PUT",
    body: JSON.stringify(payload),
  }, true);
}

export function fetchCalendar(weekStart?: string) {
  const suffix = weekStart ? `?weekStart=${weekStart}` : "";
  return apiRequest<CalendarWeekDto>(`/calendar${suffix}`);
}

export function fetchAdminDashboard() {
  return apiRequest<AdminDashboardDto>("/admin/dashboard", undefined, true);
}

export function searchAdminAniList(query: string, signal?: AbortSignal) {
  return apiRequest<AdminAniListSearchDto[]>(
    `/admin/anilist/search?q=${encodeURIComponent(query)}`,
    signal ? { signal } : undefined,
    true,
  );
}

export function fetchAdminSeekStreamingFolders(signal?: AbortSignal) {
  return apiRequest<AdminSeekStreamingFolderDto[]>(
    "/admin/seekstreaming/folders",
    signal ? { signal } : undefined,
    true,
  );
}

export function fetchAdminSeekStreamingVideos(folderId: string, signal?: AbortSignal) {
  return apiRequest<AdminSeekStreamingVideoDto[]>(
    `/admin/seekstreaming/folders/${folderId}/videos`,
    signal ? { signal } : undefined,
    true,
  );
}

export function buildSeekStreamingEmbedUrl(videoId: string) {
  return `${SEEKSTREAMING_EMBED_BASE}${videoId}`;
}

export function fetchAdminSuggestions() {
  return apiRequest<AdminSuggestionDto[]>("/admin/suggestions", undefined, true);
}

export function updateAdminSuggestionStatus(id: string, status: string) {
  return apiRequest<AdminSuggestionDto>(`/admin/suggestions/${id}/status/${status}`, {
    method: "PATCH",
  }, true);
}

export function convertAdminSuggestionToAnime(id: string) {
  return apiRequest<AdminSuggestionDto>(`/admin/suggestions/${id}/convert-to-anime`, {
    method: "POST",
  }, true);
}

export function fetchAdminReports() {
  return apiRequest<AdminReportDto[]>("/admin/reports", undefined, true);
}

export function updateAdminReportStatus(id: string, status: string) {
  return apiRequest<AdminReportDto>(`/admin/reports/${id}/status/${status}`, {
    method: "PATCH",
  }, true);
}

export function updateAdminCommentVisibility(commentId: string, visibility: string) {
  return apiRequest<AdminReportDto>(`/admin/comments/${commentId}/visibility/${visibility}`, {
    method: "PATCH",
  }, true);
}

export async function fetchAdminAnimes() {
  const response = await apiRequest<ApiPageResponse<AdminAnimeDto>>("/admin/animes", undefined, true);
  return response.items;
}

export async function createAdminAnime(payload: AdminAnimePayload) {
  return apiRequest<AdminAnimeDto>("/admin/animes", {
    method: "POST",
    body: JSON.stringify(payload),
  }, true);
}

export async function updateAdminAnime(id: string, payload: AdminAnimePayload) {
  return apiRequest<AdminAnimeDto>(`/admin/animes/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  }, true);
}

export async function updateAdminAnimeVisibility(id: string, visibility: string) {
  return apiRequest<AdminAnimeDto>(`/admin/animes/${id}/visibility/${visibility}`, {
    method: "PATCH",
  }, true);
}

export async function deleteAdminAnime(id: string) {
  return apiRequest<{ message: string }>(`/admin/animes/${id}`, { method: "DELETE" }, true);
}

export async function fetchAdminEpisodes() {
  const response = await apiRequest<ApiPageResponse<AdminEpisodeDto>>("/admin/episodes", undefined, true);
  return response.items;
}

export async function createAdminEpisode(payload: AdminEpisodePayload) {
  return apiRequest<AdminEpisodeDto>("/admin/episodes", {
    method: "POST",
    body: JSON.stringify(payload),
  }, true);
}

export async function updateAdminEpisode(id: string, payload: AdminEpisodePayload) {
  return apiRequest<AdminEpisodeDto>(`/admin/episodes/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  }, true);
}

export async function deleteAdminEpisode(id: string) {
  return apiRequest<{ message: string }>(`/admin/episodes/${id}`, { method: "DELETE" }, true);
}

export function fetchAdminHome() {
  return apiRequest<AdminHomeDto>("/admin/home", undefined, true);
}

export function updateAdminHomeSections(sections: AdminHomeSectionDto[]) {
  return apiRequest<AdminHomeDto>("/admin/home/sections", {
    method: "PUT",
    body: JSON.stringify(sections),
  }, true);
}

export function updateAdminHero(payload: { animeIds: string[]; tag: string; ctaLabel: string }) {
  return apiRequest<AdminHomeDto["hero"]>("/admin/home/hero", {
    method: "PUT",
    body: JSON.stringify(payload),
  }, true);
}
