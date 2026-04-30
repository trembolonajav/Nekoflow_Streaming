import { type ReactNode } from "react";
import { cn } from "@/lib/utils";

interface SectionHeaderProps {
  title: string;
  subtitle?: string;
  icon?: ReactNode;
  action?: ReactNode;
  className?: string;
}

/**
 * Cabeçalho editorial de seção: serif title + meta sutil + ação opcional à direita.
 */
export function SectionHeader({
  title,
  subtitle,
  icon,
  action,
  className,
}: SectionHeaderProps) {
  return (
    <header
      className={cn(
        "mb-6 flex flex-wrap items-end justify-between gap-3 px-4 md:px-0",
        className,
      )}
    >
      <div className="flex flex-col gap-1">
        <div className="flex items-center gap-2.5">
          {icon ? <span className="text-gold/80">{icon}</span> : null}
          <h2 className="font-serif text-2xl font-medium leading-none tracking-tight text-ivory md:text-3xl">
            {title}
          </h2>
        </div>
        {subtitle ? (
          <p className="text-xs uppercase tracking-[0.22em] text-ivory-muted md:text-[11px]">
            {subtitle}
          </p>
        ) : null}
      </div>
      {action ? <div className="flex items-center gap-2">{action}</div> : null}
    </header>
  );
}
