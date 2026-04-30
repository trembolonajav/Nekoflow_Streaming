import { useEffect, useRef, useState } from "react";
import { useAuth } from "@/hooks/use-auth";
import { cn } from "@/lib/utils";

interface CommentComposerProps {
  initialValue?: string;
  variant?: "top" | "inline";
  autoFocus?: boolean;
  onSubmit: (body: string) => void | Promise<void>;
  onCancel?: () => void;
}

const MAX = 500;

export function CommentComposer({
  initialValue = "",
  variant = "top",
  autoFocus = false,
  onSubmit,
  onCancel,
}: CommentComposerProps) {
  const { user, isAuthenticated } = useAuth();
  const [value, setValue] = useState(initialValue);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const ref = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (autoFocus) {
      ref.current?.focus();
      const len = ref.current?.value.length ?? 0;
      ref.current?.setSelectionRange(len, len);
    }
  }, [autoFocus]);

  useEffect(() => {
    if (!ref.current) return;
    ref.current.style.height = "auto";
    ref.current.style.height = `${ref.current.scrollHeight}px`;
  }, [value]);

  const isInline = variant === "inline";
  const trimmed = value.trim();
  const tooLong = value.length > MAX;
  const canSubmit = isAuthenticated && trimmed.length > 0 && !tooLong && !isSubmitting;

  const handleSubmit = async () => {
    if (!canSubmit) return;
    setIsSubmitting(true);
    try {
      await onSubmit(trimmed);
      setValue("");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      className={cn(
        "flex gap-3 rounded-xl border border-border-subtle bg-surface/60 p-3 transition-colors focus-within:border-gold/50",
        isInline ? "mt-2" : "p-4",
      )}
    >
      <span
        className={cn(
          "flex flex-shrink-0 items-center justify-center rounded-full border border-border-subtle bg-onyx font-serif text-gold",
          isInline ? "h-8 w-8 text-sm" : "h-10 w-10 text-base",
        )}
      >
        {user?.initial ?? "N"}
      </span>

      <div className="flex flex-1 flex-col gap-2">
        <textarea
          ref={ref}
          value={value}
          onChange={(e) => setValue(e.target.value)}
          placeholder={
            isAuthenticated
              ? isInline
                ? "Escreva sua resposta..."
                : "O que você achou deste episódio?"
              : "Entre na sua conta para comentar."
          }
          rows={isInline ? 1 : 2}
          disabled={!isAuthenticated || isSubmitting}
          className={cn(
            "w-full resize-none border-0 bg-transparent text-sm leading-relaxed text-ivory outline-none placeholder:text-ivory-muted/70",
            isInline ? "min-h-[36px]" : "min-h-[56px]",
          )}
        />

        <div className="flex items-center justify-between gap-2">
          <span
            className={cn(
              "font-mono text-[10px] uppercase tracking-[0.22em]",
              tooLong ? "text-destructive" : "text-ivory-muted/70",
            )}
          >
            {value.length} / {MAX}
          </span>

          <div className="flex items-center gap-2">
            {onCancel ? (
              <button
                type="button"
                onClick={onCancel}
                className="rounded-full px-3 py-1.5 text-[11px] uppercase tracking-[0.2em] text-ivory-muted transition-colors hover:text-ivory"
              >
                Cancelar
              </button>
            ) : null}
            <button
              type="button"
              onClick={() => void handleSubmit()}
              disabled={!canSubmit}
              className={cn(
                "rounded-full px-4 py-1.5 text-[11px] uppercase tracking-[0.22em] transition-all duration-200",
                canSubmit
                  ? "bg-gold text-onyx hover:bg-gold/90"
                  : "cursor-not-allowed bg-ivory/10 text-ivory-muted/60",
              )}
            >
              {isSubmitting ? "Enviando" : isInline ? "Responder" : "Comentar"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
