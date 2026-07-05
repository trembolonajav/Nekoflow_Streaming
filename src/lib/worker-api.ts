import { getStoredAccessToken, refreshStoredSession } from "@/hooks/use-auth";

/**
 * Cliente da automação do worker.
 *
 * As telas do worker foram absorvidas para dentro do admin do site, então este
 * cliente reaproveita a MESMA sessão do admin (getStoredAccessToken) em vez de
 * manter um login separado como fazia o console antigo. Todos os endpoints já
 * existem no backend em /api/v1/admin/worker/*.
 */

const API_BASE_URL = `${import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080"}/api/v1`;

export class WorkerApiError extends Error {
  status: number;
  body: unknown;

  constructor(message: string, status: number, body: unknown) {
    super(message);
    this.name = "WorkerApiError";
    this.status = status;
    this.body = body;
  }
}

async function request<T = unknown>(
  path: string,
  options: { method?: string; body?: unknown } = {},
): Promise<T> {
  const send = async (token: string | null) => {
    const headers = new Headers({ Accept: "application/json" });
    if (options.body !== undefined) headers.set("Content-Type", "application/json");
    if (token) headers.set("Authorization", `Bearer ${token}`);
    return fetch(`${API_BASE_URL}${path}`, {
      method: options.method ?? "GET",
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    });
  };

  let response = await send(getStoredAccessToken());
  if (response.status === 401) {
    const refreshed = await refreshStoredSession();
    if (refreshed) {
      response = await send(refreshed);
    }
  }

  const parsed = await readBody(response);
  if (!response.ok) {
    throw new WorkerApiError(bodyMessage(parsed) ?? `HTTP ${response.status}`, response.status, parsed);
  }
  return parsed as T;
}

async function readBody(response: Response) {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function bodyMessage(body: unknown) {
  if (!body) return null;
  if (typeof body === "string") return body;
  if (typeof body === "object" && "message" in body && typeof body.message === "string") {
    return body.message;
  }
  if (typeof body === "object" && "error" in body && typeof body.error === "string") {
    return body.error;
  }
  return null;
}

function query(params: Record<string, string | number | undefined>) {
  const sp = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== "") sp.set(key, String(value));
  }
  return sp.toString();
}

export const workerApi = {
  dashboard: () => request<Record<string, number>>("/admin/worker/dashboard"),
  pingSeek: () => request<Record<string, unknown>>("/admin/worker/seek/ping", { method: "POST" }),
  syncSeek: () => request<Record<string, unknown>>("/admin/worker/seek/sync", { method: "POST" }),
  seekVideos: (params: { search?: string; page?: number; size?: number } = {}) =>
    request<Record<string, unknown>>(`/admin/worker/seek/videos?${query(params)}`),
  importOptions: (params: { search?: string; status?: string } = {}) =>
    request<Record<string, unknown>>(`/admin/worker/import/options?${query(params)}`),
  importSelected: (ids: string[]) =>
    request<Record<string, unknown>>("/admin/worker/import", { method: "POST", body: { ids } }),
  queue: (params: { status?: string; search?: string; page?: number; size?: number } = {}) =>
    request<Record<string, unknown>>(`/admin/worker/queue?${query(params)}`),
  parseQueue: (body: Record<string, unknown> = {}) =>
    request<Record<string, unknown>>("/admin/worker/queue/parse", { method: "POST", body }),
  approve: (id: string) =>
    request<Record<string, unknown>>(`/admin/worker/queue/${id}/approve`, { method: "POST" }),
  approveMany: (ids: string[]) =>
    request<Record<string, unknown>>("/admin/worker/queue/approve", { method: "POST", body: { ids } }),
  updateQueueItem: (id: string, body: Record<string, unknown>) =>
    request<Record<string, unknown>>(`/admin/worker/queue/${id}`, { method: "PUT", body }),
  publish: (id: string) =>
    request<Record<string, unknown>>(`/admin/worker/queue/${id}/publish`, { method: "POST" }),
  publishMany: (ids?: string[]) =>
    request<Record<string, unknown>>("/admin/worker/queue/publish", { method: "POST", body: ids ? { ids } : {} }),
  logs: () => request<Record<string, unknown>>("/admin/worker/logs"),
  sources: () => request<unknown[]>("/admin/worker/sources"),
  createSource: (body: Record<string, unknown>) =>
    request<Record<string, unknown>>("/admin/worker/sources", { method: "POST", body }),
  updateSource: (id: string, body: Record<string, unknown>) =>
    request<Record<string, unknown>>(`/admin/worker/sources/${id}`, { method: "PUT", body }),
  deleteSource: (id: string) =>
    request<Record<string, unknown>>(`/admin/worker/sources/${id}`, { method: "DELETE" }),
  pollSources: (body: Record<string, unknown> = {}) =>
    request<Record<string, unknown>>("/admin/worker/sources/poll", { method: "POST", body }),
};
