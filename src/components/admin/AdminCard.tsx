import type { HTMLAttributes, ReactNode } from "react";
import { cn } from "@/lib/utils";

interface AdminCardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  className?: string;
}

export function AdminCard({ children, className, ...rest }: AdminCardProps) {
  return (
    <div
      className={cn(
        "rounded-xl border border-border-subtle bg-surface/60 backdrop-blur-sm",
        className,
      )}
      {...rest}
    >
      {children}
    </div>
  );
}

export function AdminCardHeader({
  title,
  action,
}: {
  title: string;
  action?: ReactNode;
}) {
  return (
    <div className="flex items-center justify-between border-b border-border-subtle px-5 py-4">
      <h3 className="font-serif text-lg text-ivory">{title}</h3>
      {action}
    </div>
  );
}

export function StatusPill({
  label,
  tone,
}: {
  label: string;
  tone: "success" | "warning" | "info" | "danger" | "muted";
}) {
  const tones: Record<string, string> = {
    success: "border-emerald-500/30 bg-emerald-500/10 text-emerald-300",
    warning: "border-amber-500/30 bg-amber-500/10 text-amber-300",
    info: "border-sky-500/30 bg-sky-500/10 text-sky-300",
    danger: "border-rose-500/30 bg-rose-500/10 text-rose-300",
    muted: "border-border-subtle bg-surface-elevated text-ivory-muted",
  };
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full border px-2.5 py-0.5 text-[11px] font-medium",
        tones[tone],
      )}
    >
      {label}
    </span>
  );
}
