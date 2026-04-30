import { cn } from "@/lib/utils";

interface OrnamentDividerProps {
  className?: string;
  width?: "sm" | "md" | "lg" | "full";
}

const widthMap = {
  sm: "max-w-[120px]",
  md: "max-w-[200px]",
  lg: "max-w-[320px]",
  full: "max-w-full",
};

/**
 * Divisor ornamental fino: linha dourada com losango central.
 * Usado como acabamento editorial entre seções, login, empty states, etc.
 */
export function OrnamentDivider({ className, width = "md" }: OrnamentDividerProps) {
  return (
    <div
      className={cn(
        "mx-auto flex w-full items-center gap-3 text-gold",
        widthMap[width],
        className,
      )}
      aria-hidden="true"
    >
      <span className="h-px flex-1 bg-gradient-to-r from-transparent via-gold/40 to-gold/60" />
      <span className="block h-1.5 w-1.5 rotate-45 border border-gold/70 bg-transparent" />
      <span className="h-px flex-1 bg-gradient-to-l from-transparent via-gold/40 to-gold/60" />
    </div>
  );
}
