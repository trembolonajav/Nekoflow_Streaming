import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";

const STORAGE_KEY = "nekoflow:auth";
const API_BASE_URL = `${import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080"}/api/v1`;

export type AuthRole = "user" | "admin";

export interface AuthUser {
  id: string;
  name: string;
  email: string;
  initial: string;
  provider: "email" | "google";
  role: AuthRole;
}

interface StoredSession {
  accessToken: string;
  refreshToken: string;
  user: AuthUser;
}

interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  name: string;
  email: string;
  roles: string[];
}

interface AuthMeResponse {
  id: string;
  name: string;
  email: string;
  roles: string[];
}

interface AuthState {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isReady: boolean;
  signIn: (input: { email: string; password: string }) => Promise<AuthUser>;
  signUp: (input: { name: string; email: string; password: string }) => Promise<AuthUser>;
  signInWithGoogle: () => Promise<AuthUser>;
  signOut: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

function initialOf(name: string): string {
  return name.trim().charAt(0).toUpperCase() || "N";
}

function toRole(roles: string[]): AuthRole {
  return roles.some((role) => role === "ADMIN" || role === "EDITOR" || role === "MODERATOR")
    ? "admin"
    : "user";
}

function toAuthUser(input: { id: string; name: string; email: string; roles: string[] }): AuthUser {
  return {
    id: input.id,
    name: input.name,
    email: input.email,
    initial: initialOf(input.name),
    provider: "email",
    role: toRole(input.roles),
  };
}

function loadSession(): StoredSession | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    return JSON.parse(raw) as StoredSession;
  } catch {
    return null;
  }
}

function saveSession(session: StoredSession | null): void {
  if (typeof window === "undefined") return;
  if (session) {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  } else {
    window.localStorage.removeItem(STORAGE_KEY);
  }
}

async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
  });

  if (!response.ok) {
    let message = "Não foi possível concluir a operação.";
    try {
      const payload = (await response.json()) as { message?: string; error?: string };
      message = payload.message ?? payload.error ?? message;
    } catch {
      message = response.status === 401 || response.status === 403
        ? "Sua sessão expirou ou você não tem acesso."
        : message;
    }
    throw new Error(message);
  }

  return response.json() as Promise<T>;
}

export function getStoredAccessToken(): string | null {
  return loadSession()?.accessToken ?? null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<StoredSession | null>(null);
  const [isReady, setIsReady] = useState(false);

  const persistSession = useCallback((next: StoredSession | null) => {
    setSession(next);
    saveSession(next);
  }, []);

  const refreshSession = useCallback(async (refreshToken: string) => {
    const payload = await apiRequest<TokenResponse>("/auth/refresh", {
      method: "POST",
      body: JSON.stringify({ refreshToken }),
    });
    const next: StoredSession = {
      accessToken: payload.accessToken,
      refreshToken: payload.refreshToken,
      user: toAuthUser({
        id: payload.userId,
        name: payload.name,
        email: payload.email,
        roles: payload.roles,
      }),
    };
    persistSession(next);
    return next;
  }, [persistSession]);

  useEffect(() => {
    let active = true;

    async function hydrate() {
      const stored = loadSession();
      if (!stored) {
        if (active) setIsReady(true);
        return;
      }

      try {
        const me = await apiRequest<AuthMeResponse>("/auth/me", {
          headers: { Authorization: `Bearer ${stored.accessToken}` },
        });
        if (!active) return;
        persistSession({
          ...stored,
          user: toAuthUser(me),
        });
      } catch {
        try {
          if (!active) return;
          await refreshSession(stored.refreshToken);
        } catch {
          if (!active) return;
          persistSession(null);
        }
      } finally {
        if (active) setIsReady(true);
      }
    }

    hydrate();
    return () => {
      active = false;
    };
  }, [persistSession, refreshSession]);

  useEffect(() => {
    function onStorage(e: StorageEvent) {
      if (e.key !== STORAGE_KEY) return;
      setSession(e.newValue ? (JSON.parse(e.newValue) as StoredSession) : null);
    }
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
  }, []);

  const signIn = useCallback(async ({ email, password }: { email: string; password: string }) => {
    const payload = await apiRequest<TokenResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    });
    const next: StoredSession = {
      accessToken: payload.accessToken,
      refreshToken: payload.refreshToken,
      user: toAuthUser({
        id: payload.userId,
        name: payload.name,
        email: payload.email,
        roles: payload.roles,
      }),
    };
    persistSession(next);
    return next.user;
  }, [persistSession]);

  const signUp = useCallback(
    async ({ name, email, password }: { name: string; email: string; password: string }) => {
      const payload = await apiRequest<TokenResponse>("/auth/register", {
        method: "POST",
        body: JSON.stringify({ name, email, password }),
      });
      const next: StoredSession = {
        accessToken: payload.accessToken,
        refreshToken: payload.refreshToken,
        user: toAuthUser({
          id: payload.userId,
          name: payload.name,
          email: payload.email,
          roles: payload.roles,
        }),
      };
      persistSession(next);
      return next.user;
    },
    [persistSession],
  );

  const signInWithGoogle = useCallback(async () => {
    throw new Error("Login com Google ainda não está disponível.");
  }, []);

  const signOut = useCallback(() => {
    const current = loadSession();
    if (current?.refreshToken) {
      void apiRequest<{ message: string }>("/auth/logout", {
        method: "POST",
        body: JSON.stringify({ refreshToken: current.refreshToken }),
      }).catch(() => undefined);
    }
    persistSession(null);
  }, [persistSession]);

  const value = useMemo<AuthState>(
    () => ({
      user: session?.user ?? null,
      isAuthenticated: Boolean(session?.accessToken),
      isReady,
      signIn,
      signUp,
      signInWithGoogle,
      signOut,
    }),
    [session, isReady, signIn, signUp, signInWithGoogle, signOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth precisa estar dentro de <AuthProvider>.");
  return ctx;
}
