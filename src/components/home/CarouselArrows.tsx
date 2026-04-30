import { ChevronLeft, ChevronRight } from "lucide-react";
import { cn } from "@/lib/utils";

interface CarouselArrowsProps {
  onPrev: () => void;
  onNext: () => void;
  className?: string;
  size?: "sm" | "md";
}

/**
 * Setas circulares dourado-sutis para carrosséis horizontais.
 */
export function CarouselArrows({
  onPrev,
  onNext,
  className,
  size = "md",
}: CarouselArrowsProps) {
  const dim = size === "sm" ? "h-8 w-8" : "h-9 w-9";
  return (
    <div className={cn("flex items-center gap-2", className)}>
      <button
        type="button"
        onClick={onPrev}
        aria-label="Anterior"
        className={cn(
          "inline-flex items-center justify-center rounded-full border border-gold/25 bg-surface/60 text-ivory backdrop-blur transition-all duration-200 hover:border-gold/60 hover:bg-surface-elevated hover:text-gold focus-visible:ring-2 focus-visible:ring-gold/40",
          dim,
        )}
      >
        <ChevronLeft className="h-4 w-4" />
      </button>
      <button
        type="button"
        onClick={onNext}
        aria-label="Próximo"
        className={cn(
          "inline-flex items-center justify-center rounded-full border border-gold/25 bg-surface/60 text-ivory backdrop-blur transition-all duration-200 hover:border-gold/60 hover:bg-surface-elevated hover:text-gold focus-visible:ring-2 focus-visible:ring-gold/40",
          dim,
        )}
      >
        <ChevronRight className="h-4 w-4" />
      </button>
    </div>
  );
}

/**
 * Hook utilitário para scrollar containers horizontais.
 */
export function useHorizontalScroll(
  ref: React.RefObject<HTMLDivElement | null>,
  amount = 0.85,
) {
  const scrollBy = (direction: 1 | -1) => {
    const el = ref.current;
    if (!el) return;
    el.scrollBy({ left: direction * el.clientWidth * amount, behavior: "smooth" });
  };
  return {
    prev: () => scrollBy(-1),
    next: () => scrollBy(1),
  };
}
