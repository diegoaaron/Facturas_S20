import type { ButtonHTMLAttributes, ReactNode } from "react";
import { twMerge } from "tailwind-merge";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "outline" | "ghost";
  fullWidth?: boolean;
  children: ReactNode;
}

export default function Button({
  variant = "primary",
  fullWidth = true,
  className,
  children,
  ...rest
}: ButtonProps) {
  const base =
    "inline-flex items-center justify-center gap-2 rounded-full px-6 py-3.5 text-sm font-semibold transition active:scale-[0.98] disabled:opacity-50 disabled:active:scale-100";

  const variants = {
    primary: "bg-primary text-white shadow-sm hover:bg-primary-light",
    outline: "border border-primary text-primary bg-white hover:bg-surface",
    ghost: "text-primary hover:bg-surface",
  };

  return (
    <button
      className={twMerge(base, variants[variant], fullWidth && "w-full", className)}
      {...rest}
    >
      {children}
    </button>
  );
}
