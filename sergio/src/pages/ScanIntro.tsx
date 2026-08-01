import { useNavigate } from "react-router-dom";
import { Camera } from "lucide-react";

const TIPS = [
  "Evita sombras sobre el papel.",
  "Coloca la factura sobre una mesa o superficie plana.",
  "Asegúrate de que los números se vean claros.",
];

export default function ScanIntro() {
  const navigate = useNavigate();

  return (
    <div className="flex h-dvh flex-col bg-accent px-8 pb-10 pt-16 text-white">
      <div className="flex flex-1 flex-col items-center justify-center gap-8">
        <div className="flex h-48 w-48 items-center justify-center rounded-full bg-white/15">
          <div className="flex h-32 w-32 items-center justify-center rounded-3xl bg-white/90 shadow-lg">
            <Camera size={56} className="text-primary" strokeWidth={1.5} />
          </div>
        </div>

        <div className="text-center">
          <h1 className="text-2xl font-bold">¡Hora de la foto!</h1>
        </div>

        <ul className="w-full max-w-xs space-y-3">
          {TIPS.map((tip) => (
            <li key={tip} className="flex items-start gap-2 text-sm">
              <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-white" />
              <span>{tip}</span>
            </li>
          ))}
        </ul>
      </div>

      <button
        type="button"
        onClick={() => navigate("/scan/camera")}
        className="w-full rounded-full bg-white py-3.5 text-sm font-bold uppercase tracking-wide text-primary shadow-sm active:scale-[0.98]"
      >
        Siguiente
      </button>
    </div>
  );
}
