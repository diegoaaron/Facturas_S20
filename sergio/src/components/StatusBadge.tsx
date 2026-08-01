import { twMerge } from "tailwind-merge";

interface StatusBadgeProps {
  status: "contado" | "pendiente" | "escaneado";
  className?: string;
}

const LABELS: Record<StatusBadgeProps["status"], string> = {
  contado: "Contado",
  pendiente: "Pendiente",
  escaneado: "Escaneado",
};

const STYLES: Record<StatusBadgeProps["status"], string> = {
  contado: "bg-success/10 text-success",
  pendiente: "bg-warning/10 text-warning",
  escaneado: "bg-white/90 text-primary",
};

export default function StatusBadge({ status, className }: StatusBadgeProps) {
  return (
    <span
      className={twMerge(
        "inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold",
        STYLES[status],
        className,
      )}
    >
      {LABELS[status]}
    </span>
  );
}
