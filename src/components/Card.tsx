import type { HTMLAttributes, ReactNode } from "react";
import { twMerge } from "tailwind-merge";

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
}

export default function Card({ className, children, ...rest }: CardProps) {
  return (
    <div
      className={twMerge("rounded-2xl bg-white shadow-sm", className)}
      {...rest}
    >
      {children}
    </div>
  );
}
