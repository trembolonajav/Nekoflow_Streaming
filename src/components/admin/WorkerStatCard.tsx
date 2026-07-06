import * as React from "react";
import { cn } from "@/lib/utils";

interface StatCardProps {
  label: string;
  value: string | number;
  hint?: string;
  tone?: "default" | "success" | "warning" | "destructive" | "accent";
  icon?: React.ReactNode;
}

const TONE: Record<NonNullable<StatCardProps["tone"]>, string> = {
  default: "text-foreground",
  success: "text-success",
  warning: "text-warning",
  destructive: "text-destructive",
  accent: "text-accent",
};

export const StatCard = React.forwardRef<HTMLDivElement, StatCardProps>(
  ({ label, value, hint, tone = "default", icon }, ref) => {
    return (
      <div ref={ref} className="panel p-5">
        <div className="flex items-start justify-between">
          <div className="mono text-[11px] uppercase tracking-widest text-muted-foreground">
            {label}
          </div>
          {icon && <div className="text-muted-foreground/60">{icon}</div>}
        </div>
        <div className={cn("mt-3 text-3xl font-semibold mono tabular-nums", TONE[tone])}>
          {value}
        </div>
        {hint && <div className="mt-1 text-xs text-muted-foreground">{hint}</div>}
      </div>
    );
  },
);
StatCard.displayName = "WorkerStatCard";
