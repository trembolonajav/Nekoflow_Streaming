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
const GOOGLE_CLIENT_ID = (import.meta.env.VITE_GOOGLE_CLIENT_ID ?? "").trim();

type GoogleProviderApi = {
  accounts: {
    id: {
      initialize: (options: {
        client_id: string;
        callback: (response: { credential?: string }) => void;
        auto_select?: boolean;
        cancel_on_tap_outside?: boolean;
      }) => void;
      prompt: (callback?: (notification: {
        isNotDisplayed?: () => boolean;
        isSkippedMoment?: () => boolean;
        isDismissedMoment?: () => boolean;
      }) => void) => void;
    };
  };
};

declare global {
  interface Window {
    google?: GoogleProviderApi;
  }
}

let googleScriptPromise: Promise<void> | null = null;

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
  provider: string;
}

interface AuthMeResponse {
  id: string;
  name: string;
  email: string;
  roles: string[];
  provider: string;
}

interface AuthState {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isReady: boolean;
  signIn: (input: { email: string; password: string; remember?: boolean }) => Promise<AuthUser>;
  signUp: (input: { name: string; email: string; password: string; confirmPassword: string; acceptTerms: boolean }) => Promise<AuthUser>;
  signInWithGoogle: (input?: { acceptTerms?: boolean }) => Promise<AuthUser>;
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

function toAuthProvider(provider: string): AuthUser["provider"] {
  return provider.includes("GOOGLE") ? "google" : "email";
}

function toAuthUser(input: { id: string; name: string; email: string; roles: string[]; provider: string }): AuthUser {
  return {
    id: input.id,
    name: input.name,
    email: input.email,
    initial: initialOf(input.name),
    provider: toAuthProvider(input.provider),
    role: toRole(input.roles),
  };
}

function loadSession(): StoredSession | null {
  if (typeof window === "undefined") return null;
  try {
    // Sessao efemera desta aba (sessionStorage) tem precedencia sobre a persistente (localStorage).
    const raw = window.sessionStorage.getItem(STORAGE_KEY) ?? window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    return JSON.parse(raw) as StoredSession;
  } catch {
    return null;
  }
}

// remember === true      -> localStorage   (sobrevive fechar o navegador)
// remember === false     -> sessionStorage (some ao fechar a aba/navegador)
// remember === undefined -> preserva onde a sessao ja esta (usado por refresh/hydrate)
function saveSession(session: StoredSession | null, remember?: boolean): void {
  if (typeof window === "undefined") return;
  if (!session) {
    window.localStorage.removeItem(STORAGE_KEY);
    window.sessionStorage.removeItem(STORAGE_KEY);
    return;
  }
  let target: Storage;
  if (remember === undefined) {
    target = window.sessionStorage.getItem(STORAGE_KEY) ? window.sessionStorage : window.localStorage;
  } else {
    target = remember ? window.localStorage : window.sessionStorage;
    (remember ? window.sessionStorage : window.localStorage).removeItem(STORAGE_KEY);
  }
  target.setItem(STORAGE_KEY, JSON.stringify(session));
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

async function loadGoogleScript() {
  if (typeof window === "undefined") return;
  if (window.google?.accounts?.id) return;

  if (!googleScriptPromise) {
    googleScriptPromise = new Promise<void>((resolve, reject) => {
      const script = document.createElement("script");
      script.src = "https://accounts.google.com/gsi/client";
      script.async = true;
      script.defer = true;
      script.dataset.googleIdentity = "true";
      script.onload = () => resolve();
      script.onerror = () => reject(new Error("Não foi possível carregar o login do Google."));
      document.head.appendChild(script);
    });
  }

  return googleScriptPromise;
}

async function requestGoogleCredential(): Promise<string> {
  if (!GOOGLE_CLIENT_ID) {
    throw new Error("Login com Google ainda não foi configurado.");
  }

  await loadGoogleScript();

  return new Promise<string>((resolve, reject) => {
    if (!window.google?.accounts?.id) {
      reject(new Error("Google Identity Services não está disponível neste navegador."));
      return;
    }

    let settled = false;
    const timeoutId = window.setTimeout(() => {
      if (!settled) {
        settled = true;
        reject(new Error("Não foi possível concluir o login com Google."));
      }
    }, 15000);

    const finish = (resolver: typeof resolve | typeof reject, value: string | Error) => {
      if (settled) return;
      settled = true;
      window.clearTimeout(timeoutId);
      resolver(value as never);
    };

    window.google.accounts.id.initialize({
      client_id: GOOGLE_CLIENT_ID,
      auto_select: false,
      cancel_on_tap_outside: true,
      callback: (response) => {
        if (!response.credential) {
          finish(reject, new Error("O Google não retornou uma credencial válida."));
          return;
        }
        finish(resolve, response.credential);
      },
    });

    window.google.accounts.id.prompt((notification) => {
      if (notification.isNotDisplayed?.() || notification.isSkippedMoment?.() || notification.isDismissedMoment?.()) {
        finish(reject, new Error("O prompt do Google não pôde ser exibido. Tente novamente ou use login por e-mail."));
      }
    });
  });
}

export function getStoredAccessToken(): string | null {
  return loadSession()?.accessToken ?? null;
}

export async function refreshStoredSession(): Promise<string | null> {
  const stored = loadSession();
  if (!stored?.refreshToken) return null;

  try {
    const payload = await apiRequest<TokenResponse>("/auth/refresh", {
      method: "POST",
      body: JSON.stringify({ refreshToken: stored.refreshToken }),
    });
    const next: StoredSession = {
      accessToken: payload.accessToken,
      refreshToken: payload.refreshToken,
      user: toAuthUser({
        id: payload.userId,
        name: payload.name,
        email: payload.email,
        roles: payload.roles,
        provider: payload.provider,
      }),
    };
    saveSession(next);
    return next.accessToken;
  } catch {
    // Multi-aba: se outra aba ja renovou (refresh token diferente no storage), reaproveita.
    const latest = loadSession();
    if (latest?.refreshToken && latest.refreshToken !== stored.refreshToken) {
      return latest.accessToken;
    }
    saveSession(null);
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<StoredSession | null>(null);
  const [isReady, setIsReady] = useState(false);

  const persistSession = useCallback((next: StoredSession | null, remember?: boolean) => {
    setSession(next);
    saveSession(next, remember);
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
        provider: payload.provider,
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
        if (!active) return;
        try {
          await refreshSession(stored.refreshToken);
        } catch {
          if (!active) return;
          // Multi-aba: outra aba pode ter rotacionado o refresh token e salvo uma sessao nova.
          // Antes de deslogar, tenta adotar a sessao mais recente do storage.
          const latest = loadSession();
          if (latest && latest.refreshToken !== stored.refreshToken) {
            try {
              const me = await apiRequest<AuthMeResponse>("/auth/me", {
                headers: { Authorization: `Bearer ${latest.accessToken}` },
              });
              if (!active) return;
              persistSession({ ...latest, user: toAuthUser(me) });
              return;
            } catch {
              // sessao mais recente tambem invalida -> desloga abaixo
            }
          }
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

  const signIn = useCallback(async ({ email, password, remember = true }: { email: string; password: string; remember?: boolean }) => {
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
        provider: payload.provider,
      }),
    };
    persistSession(next, remember);
    return next.user;
  }, [persistSession]);

  const signUp = useCallback(
    async ({ name, email, password, confirmPassword, acceptTerms }: { name: string; email: string; password: string; confirmPassword: string; acceptTerms: boolean }) => {
      const payload = await apiRequest<TokenResponse>("/auth/register", {
        method: "POST",
        body: JSON.stringify({ name, email, password, confirmPassword, acceptTerms }),
      });
      const next: StoredSession = {
        accessToken: payload.accessToken,
        refreshToken: payload.refreshToken,
        user: toAuthUser({
          id: payload.userId,
          name: payload.name,
          email: payload.email,
          roles: payload.roles,
          provider: payload.provider,
        }),
      };
      persistSession(next);
      return next.user;
    },
    [persistSession],
  );

  const signInWithGoogle = useCallback(async ({ acceptTerms = false }: { acceptTerms?: boolean } = {}) => {
    const idToken = await requestGoogleCredential();
    const payload = await apiRequest<TokenResponse>("/auth/google", {
      method: "POST",
      body: JSON.stringify({ idToken, acceptTerms }),
    });
    const next: StoredSession = {
      accessToken: payload.accessToken,
      refreshToken: payload.refreshToken,
      user: toAuthUser({
        id: payload.userId,
        name: payload.name,
        email: payload.email,
        roles: payload.roles,
        provider: payload.provider,
      }),
    };
    persistSession(next);
    return next.user;
  }, [persistSession]);

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
