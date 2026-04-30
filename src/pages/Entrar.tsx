import { useEffect, useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { Eye, EyeOff, Lock, Mail, Sparkles, User } from "lucide-react";
import { toast } from "sonner";

import authBg from "@/assets/auth-background.png";
import logoUrl from "@/assets/nekoflow-logo.png";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { OrnamentDivider } from "@/components/layout/OrnamentDivider";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { useAuth } from "@/hooks/use-auth";
import { cn } from "@/lib/utils";


function EntrarPage() {
  const { isAuthenticated, isReady } = useAuth();
  const navigate = useNavigate();
  const [tab, setTab] = useState<"signin" | "signup">("signin");

  // Se já está logado, manda pra home
  useEffect(() => {
    if (isReady && isAuthenticated) {
      navigate("/");
    }
  }, [isReady, isAuthenticated, navigate]);

  return (
    <div className="flex min-h-screen flex-col bg-onyx">
      <Header />

      <main className="relative flex flex-1 w-full">
        {/* Coluna visual (esquerda) */}
        <aside
          className="relative hidden overflow-hidden lg:block lg:w-[58%] xl:w-[60%]"
          aria-hidden
        >
          <img
            src={authBg}
            alt=""
            className="absolute inset-0 h-full w-full object-cover object-left"
          />
          {/* Gradient para fusão com a coluna do form */}
          <div className="absolute inset-0 bg-gradient-to-r from-onyx/0 via-onyx/20 to-onyx" />
        </aside>

        {/* Coluna do formulário (direita) */}
        <section className="relative flex w-full items-center justify-center px-4 py-10 sm:px-8 lg:w-[42%] lg:justify-start lg:px-10 xl:w-[40%] xl:px-14">
          {/* Background mobile (a imagem aparece atrás esmaecida) */}
          <img
            src={authBg}
            alt=""
            aria-hidden
            className="absolute inset-0 h-full w-full object-cover opacity-25 lg:hidden"
          />
          <div className="absolute inset-0 bg-gradient-to-b from-onyx via-onyx/95 to-onyx lg:hidden" />

          <div className="relative w-full max-w-md">
            <div className="rounded-2xl border border-gold/15 bg-surface/80 p-7 shadow-[0_30px_80px_-20px_rgba(0,0,0,0.6)] backdrop-blur-xl sm:p-9">
              {/* Logo + título */}
              <div className="flex flex-col items-center text-center">
                <img src={logoUrl} alt="Nekoflow" className="h-16 w-auto" draggable={false} />
                <OrnamentDivider width="sm" className="my-4 opacity-70" />
              </div>

              <Tabs value={tab} onValueChange={(v) => setTab(v as "signin" | "signup")}>
                <TabsList className="mb-6 grid h-auto w-full grid-cols-2 rounded-lg border border-border-subtle bg-onyx/60 p-1">
                  <TabsTrigger
                    value="signin"
                    className="rounded-md py-2 font-mono text-[11px] uppercase tracking-[0.18em] text-ivory-muted data-[state=active]:bg-gold/10 data-[state=active]:text-gold"
                  >
                    Entrar
                  </TabsTrigger>
                  <TabsTrigger
                    value="signup"
                    className="rounded-md py-2 font-mono text-[11px] uppercase tracking-[0.18em] text-ivory-muted data-[state=active]:bg-gold/10 data-[state=active]:text-gold"
                  >
                    Criar conta
                  </TabsTrigger>
                </TabsList>

                <TabsContent value="signin" className="mt-0">
                  <SignInForm />
                </TabsContent>
                <TabsContent value="signup" className="mt-0">
                  <SignUpForm onSuccess={() => setTab("signin")} />
                </TabsContent>
              </Tabs>
            </div>

            <p className="mt-6 text-center font-mono text-[10px] uppercase tracking-[0.22em] text-ivory-muted/60">
              Ao entrar você concorda com os termos da Nekoflow
            </p>
          </div>
        </section>
      </main>

      <Footer />
    </div>
  );
}

/* -------------------------------------------------------------------------- */
/*  Entrar                                                                    */
/* -------------------------------------------------------------------------- */

function SignInForm() {
  const { signIn, signInWithGoogle } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [remember, setRemember] = useState(true);
  const [loading, setLoading] = useState(false);
  const [googleLoading, setGoogleLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (loading) return;
    setLoading(true);
    try {
      const user = await signIn({ email, password });
      toast.success(`Bem-vindo(a) de volta, ${user.name}.`);
      navigate("/");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Não foi possível entrar.");
    } finally {
      setLoading(false);
    }
  };

  const handleGoogle = async () => {
    if (googleLoading) return;
    setGoogleLoading(true);
    try {
      const user = await signInWithGoogle();
      toast.success(`Conectado como ${user.name}.`);
      navigate("/");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Não foi possível conectar com Google.");
    } finally {
      setGoogleLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-5">
      <header className="text-center">
        <h1 className="font-serif text-3xl font-medium text-ivory">Entrar</h1>
        <p className="mt-1 text-sm text-ivory-muted">
          Acesse sua conta para continuar assistindo.
        </p>
      </header>

      <FieldEmail value={email} onChange={setEmail} />
      <FieldPassword
        value={password}
        onChange={setPassword}
        visible={showPassword}
        onToggleVisible={() => setShowPassword((v) => !v)}
      />

      <div className="flex items-center justify-between">
        <label className="flex cursor-pointer items-center gap-2 text-sm text-ivory-muted">
          <Checkbox
            checked={remember}
            onCheckedChange={(v) => setRemember(!!v)}
            className="border-border-subtle data-[state=checked]:border-gold data-[state=checked]:bg-gold data-[state=checked]:text-onyx"
          />
          Lembrar de mim
        </label>
        <button
          type="button"
          onClick={() => toast.info("Em breve: recuperação de senha.")}
          className="text-sm text-gold/80 transition-colors hover:text-gold"
        >
          Esqueci minha senha
        </button>
      </div>

      <PrimaryGoldButton type="submit" loading={loading}>
        Entrar
      </PrimaryGoldButton>

      <Divider />

      <GoogleButton onClick={handleGoogle} loading={googleLoading} />
    </form>
  );
}

/* -------------------------------------------------------------------------- */
/*  Criar conta                                                               */
/* -------------------------------------------------------------------------- */

function SignUpForm({ onSuccess }: { onSuccess: () => void }) {
  const { signUp, signInWithGoogle } = useAuth();
  const navigate = useNavigate();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [accept, setAccept] = useState(false);
  const [loading, setLoading] = useState(false);
  const [googleLoading, setGoogleLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (loading) return;
    if (!accept) {
      toast.error("Aceite os termos para criar sua conta.");
      return;
    }
    if (password.length < 6) {
      toast.error("A senha precisa ter pelo menos 6 caracteres.");
      return;
    }
    setLoading(true);
    try {
      const user = await signUp({ name, email, password });
      toast.success(`Conta criada. Bem-vindo(a), ${user.name}!`);
      onSuccess();
      navigate("/");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Não foi possível criar a conta.");
    } finally {
      setLoading(false);
    }
  };

  const handleGoogle = async () => {
    if (googleLoading) return;
    setGoogleLoading(true);
    try {
      const user = await signInWithGoogle();
      toast.success(`Conta vinculada ao Google de ${user.name}.`);
      navigate("/");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Não foi possível conectar com Google.");
    } finally {
      setGoogleLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-5">
      <header className="text-center">
        <h1 className="font-serif text-3xl font-medium text-ivory">Criar conta</h1>
        <p className="mt-1 text-sm text-ivory-muted">
          Comece sua jornada na curadoria Nekoflow.
        </p>
      </header>

      <FieldText
        label="Nome"
        icon={<User className="size-4" />}
        placeholder="Como podemos te chamar?"
        value={name}
        onChange={setName}
        autoComplete="name"
      />
      <FieldEmail value={email} onChange={setEmail} />
      <FieldPassword
        value={password}
        onChange={setPassword}
        visible={showPassword}
        onToggleVisible={() => setShowPassword((v) => !v)}
        autoComplete="new-password"
        helper="Mínimo de 6 caracteres."
      />

      <label className="flex cursor-pointer items-start gap-2 text-sm text-ivory-muted">
        <Checkbox
          checked={accept}
          onCheckedChange={(v) => setAccept(!!v)}
          className="mt-0.5 border-border-subtle data-[state=checked]:border-gold data-[state=checked]:bg-gold data-[state=checked]:text-onyx"
        />
        <span>
          Concordo com os <span className="text-gold">termos</span> e a{" "}
          <span className="text-gold">política de privacidade</span>.
        </span>
      </label>

      <PrimaryGoldButton type="submit" loading={loading}>
        Criar conta
      </PrimaryGoldButton>

      <Divider />

      <GoogleButton onClick={handleGoogle} loading={googleLoading} label="Cadastrar com Google" />
    </form>
  );
}

/* -------------------------------------------------------------------------- */
/*  Subcomponentes                                                            */
/* -------------------------------------------------------------------------- */

function FieldText({
  label,
  icon,
  placeholder,
  value,
  onChange,
  autoComplete,
  type = "text",
}: {
  label: string;
  icon: React.ReactNode;
  placeholder: string;
  value: string;
  onChange: (v: string) => void;
  autoComplete?: string;
  type?: string;
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <Label className="font-mono text-[10px] uppercase tracking-[0.22em] text-ivory-muted">
        {label}
      </Label>
      <div className="relative">
        <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ivory-muted">
          {icon}
        </span>
        <Input
          type={type}
          required
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          autoComplete={autoComplete}
          className="h-12 border-border-subtle bg-onyx/50 pl-10 text-sm text-ivory placeholder:text-ivory-muted/60 focus-visible:border-gold/40 focus-visible:bg-onyx/70 focus-visible:ring-2 focus-visible:ring-gold/30"
        />
      </div>
    </div>
  );
}

function FieldEmail({ value, onChange }: { value: string; onChange: (v: string) => void }) {
  return (
    <FieldText
      label="E-mail"
      icon={<Mail className="size-4" />}
      placeholder="seu@email.com"
      value={value}
      onChange={onChange}
      type="email"
      autoComplete="email"
    />
  );
}

function FieldPassword({
  value,
  onChange,
  visible,
  onToggleVisible,
  autoComplete = "current-password",
  helper,
}: {
  value: string;
  onChange: (v: string) => void;
  visible: boolean;
  onToggleVisible: () => void;
  autoComplete?: string;
  helper?: string;
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <Label className="font-mono text-[10px] uppercase tracking-[0.22em] text-ivory-muted">
        Senha
      </Label>
      <div className="relative">
        <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ivory-muted">
          <Lock className="size-4" />
        </span>
        <Input
          type={visible ? "text" : "password"}
          required
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder="••••••••••"
          autoComplete={autoComplete}
          className="h-12 border-border-subtle bg-onyx/50 pl-10 pr-11 text-sm text-ivory placeholder:text-ivory-muted/60 focus-visible:border-gold/40 focus-visible:bg-onyx/70 focus-visible:ring-2 focus-visible:ring-gold/30"
        />
        <button
          type="button"
          onClick={onToggleVisible}
          aria-label={visible ? "Ocultar senha" : "Mostrar senha"}
          className="absolute right-3 top-1/2 -translate-y-1/2 text-ivory-muted transition-colors hover:text-gold"
        >
          {visible ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
        </button>
      </div>
      {helper ? (
        <span className="text-[11px] text-ivory-muted/70">{helper}</span>
      ) : null}
    </div>
  );
}

function PrimaryGoldButton({
  children,
  loading,
  type = "button",
}: {
  children: React.ReactNode;
  loading?: boolean;
  type?: "button" | "submit";
}) {
  return (
    <Button
      type={type}
      disabled={loading}
      className={cn(
        "group/btn relative h-12 w-full overflow-hidden bg-gradient-to-b from-gold to-[oklch(0.66_0.13_75)] font-serif text-base font-medium text-onyx shadow-[0_10px_30px_-12px_var(--gold-glow)] transition-all duration-300 hover:from-gold hover:to-gold hover:shadow-[0_14px_40px_-12px_var(--gold-glow)]",
      )}
    >
      <Sparkles className="mr-2 size-4 opacity-80" />
      {loading ? "Aguarde…" : children}
      <Sparkles className="ml-2 size-4 opacity-80" />
    </Button>
  );
}

function Divider() {
  return (
    <div className="flex items-center gap-3">
      <span className="h-px flex-1 bg-border-subtle" />
      <span className="font-mono text-[10px] uppercase tracking-[0.22em] text-ivory-muted/70">
        ou
      </span>
      <span className="h-px flex-1 bg-border-subtle" />
    </div>
  );
}

function GoogleButton({
  onClick,
  loading,
  label = "Continuar com Google",
}: {
  onClick: () => void;
  loading?: boolean;
  label?: string;
}) {
  return (
    <Button
      type="button"
      variant="outline"
      onClick={onClick}
      disabled={loading}
      className="h-12 w-full gap-3 border-border-subtle bg-onyx/50 text-sm font-medium text-ivory transition-all duration-200 hover:border-gold/40 hover:bg-onyx/70 hover:text-ivory"
    >
      <GoogleGlyph />
      {loading ? "Conectando ao Google…" : label}
    </Button>
  );
}

function GoogleGlyph() {
  return (
    <svg className="size-5" viewBox="0 0 48 48" aria-hidden="true">
      <path
        fill="#FFC107"
        d="M43.611 20.083H42V20H24v8h11.303c-1.649 4.657-6.08 8-11.303 8-6.627 0-12-5.373-12-12s5.373-12 12-12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 12.955 4 4 12.955 4 24s8.955 20 20 20 20-8.955 20-20c0-1.341-.138-2.65-.389-3.917z"
      />
      <path
        fill="#FF3D00"
        d="m6.306 14.691 6.571 4.819C14.655 15.108 18.961 12 24 12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 16.318 4 9.656 8.337 6.306 14.691z"
      />
      <path
        fill="#4CAF50"
        d="M24 44c5.166 0 9.86-1.977 13.409-5.192l-6.19-5.238C29.211 35.091 26.715 36 24 36c-5.202 0-9.619-3.317-11.283-7.946l-6.522 5.025C9.505 39.556 16.227 44 24 44z"
      />
      <path
        fill="#1976D2"
        d="M43.611 20.083H42V20H24v8h11.303c-.792 2.237-2.231 4.166-4.087 5.571.001-.001.002-.001.003-.002l6.19 5.238C36.971 39.205 44 34 44 24c0-1.341-.138-2.65-.389-3.917z"
      />
    </svg>
  );
}

export default EntrarPage;
