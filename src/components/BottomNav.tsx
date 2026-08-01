import { Heart, FileText, Home, Plus, User } from "lucide-react";
import { useNavigate, useLocation } from "react-router-dom";
import { twMerge } from "tailwind-merge";

export default function BottomNav() {
  const navigate = useNavigate();
  const location = useLocation();

  const isActive = (path: string) => location.pathname === path;

  return (
    <nav className="fixed inset-x-0 bottom-0 z-30 border-t border-black/5 bg-white pb-[max(0.5rem,env(safe-area-inset-bottom))] pt-2">
      <div className="relative mx-auto flex max-w-md items-center justify-between px-6">
        <button
          type="button"
          onClick={() => navigate("/summary")}
          className={twMerge(
            "flex flex-col items-center gap-1 p-2 text-text-secondary",
            isActive("/summary") && "text-primary",
          )}
          aria-label="Inicio"
        >
          <Home size={22} strokeWidth={isActive("/summary") ? 2.5 : 2} />
        </button>

        <button
          type="button"
          disabled
          className="flex flex-col items-center gap-1 p-2 text-text-secondary/40"
          aria-label="Favoritos"
          aria-disabled="true"
        >
          <Heart size={22} />
        </button>

        <button
          type="button"
          onClick={() => navigate("/scan/intro")}
          className="-mt-7 flex h-14 w-14 items-center justify-center rounded-full bg-accent text-white shadow-md active:scale-95"
          aria-label="Nueva factura"
        >
          <Plus size={26} strokeWidth={2.5} />
        </button>

        <button
          type="button"
          disabled
          className="flex flex-col items-center gap-1 p-2 text-text-secondary/40"
          aria-label="Historial"
          aria-disabled="true"
        >
          <FileText size={22} />
        </button>

        <button
          type="button"
          onClick={() => navigate("/profile")}
          className={twMerge(
            "flex flex-col items-center gap-1 p-2 text-text-secondary",
            isActive("/profile") && "text-primary",
          )}
          aria-label="Perfil"
        >
          <User size={22} strokeWidth={isActive("/profile") ? 2.5 : 2} />
        </button>
      </div>
    </nav>
  );
}
