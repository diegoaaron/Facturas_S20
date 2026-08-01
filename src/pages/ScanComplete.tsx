import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { AlertTriangle, Check } from "lucide-react";
import type { InvoiceData } from "../types/Invoice";

interface LocationState {
  imageBase64?: string;
  data?: InvoiceData;
}

const CHECKLIST_FIELDS: { key: keyof InvoiceData; label: string; backendKeys: string[] }[] = [
  { key: "proveedor", label: "Proveedor", backendKeys: ["proveedor"] },
  { key: "ruc", label: "RUC", backendKeys: ["ruc"] },
  { key: "numeroDocumento", label: "Nº de Factura/Boleta", backendKeys: ["numero_factura"] },
  { key: "fechaEmision", label: "Fecha Emisión", backendKeys: ["fecha_emision"] },
  { key: "montoTotal", label: "Monto Total", backendKeys: ["monto_total"] },
];

function isFieldDetected(
  data: InvoiceData,
  field: { key: keyof InvoiceData; backendKeys: string[] },
): boolean {
  const value = data[field.key];
  const hasValue = typeof value === "number" ? value > 0 : Boolean(value);
  const flagged = (data.camposParaRevisar ?? []).some((k) => field.backendKeys.includes(k));
  return hasValue && !flagged;
}

const RADIUS = 54;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

export default function ScanComplete() {
  const navigate = useNavigate();
  const location = useLocation();
  const { imageBase64, data } = (location.state as LocationState | null) ?? {};

  const [progress, setProgress] = useState(0);

  useEffect(() => {
    if (!data) return;
    const id = requestAnimationFrame(() => setProgress(100));
    return () => cancelAnimationFrame(id);
  }, [data]);

  if (!data) {
    return (
      <div className="flex h-dvh flex-col items-center justify-center gap-4 bg-white px-8 text-center">
        <p className="text-sm text-text-secondary">
          No hay datos de factura para mostrar. Empieza escaneando una nueva factura.
        </p>
        <button
          type="button"
          onClick={() => navigate("/scan/intro", { replace: true })}
          className="rounded-full bg-primary px-6 py-3 text-sm font-semibold text-white"
        >
          Escanear factura
        </button>
      </div>
    );
  }

  const offset = CIRCUMFERENCE - (progress / 100) * CIRCUMFERENCE;

  return (
    <div className="flex h-dvh flex-col items-center bg-white px-8 py-16">
      <div className="relative flex h-40 w-40 items-center justify-center">
        <svg viewBox="0 0 120 120" className="h-full w-full -rotate-90">
          <circle
            cx="60"
            cy="60"
            r={RADIUS}
            fill="none"
            stroke="#F7F7F5"
            strokeWidth="10"
          />
          <circle
            cx="60"
            cy="60"
            r={RADIUS}
            fill="none"
            stroke="#2E7D32"
            strokeWidth="10"
            strokeLinecap="round"
            strokeDasharray={CIRCUMFERENCE}
            strokeDashoffset={offset}
            style={{ transition: "stroke-dashoffset 1.1s ease-out" }}
          />
        </svg>
        <div className="absolute flex flex-col items-center">
          {progress === 100 ? (
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-success">
              <Check size={26} className="text-white" strokeWidth={3} />
            </div>
          ) : (
            <span className="text-2xl font-bold text-primary">{progress}%</span>
          )}
        </div>
      </div>

      <p className="mt-4 text-lg font-bold text-text-primary">Completo</p>

      <div className="mt-8 w-full max-w-sm space-y-3">
        {CHECKLIST_FIELDS.map((field) => {
          const detected = isFieldDetected(data, field);
          return (
            <div
              key={field.key}
              className="flex items-center justify-between rounded-xl bg-surface px-4 py-3"
            >
              <span className="text-sm text-text-primary">{field.label}</span>
              <div
                className={`flex h-5 w-5 items-center justify-center rounded-full ${
                  detected ? "bg-success" : "bg-warning"
                }`}
              >
                {detected ? (
                  <Check size={13} className="text-white" strokeWidth={3} />
                ) : (
                  <AlertTriangle size={12} className="text-white" strokeWidth={2.5} />
                )}
              </div>
            </div>
          );
        })}
      </div>
      <p className="mt-3 text-center text-xs text-text-secondary">
        Los campos marcados en naranja no se detectaron con certeza; podrás completarlos en el
        siguiente paso.
      </p>

      <button
        type="button"
        onClick={() => navigate("/invoice/nueva", { state: { imageBase64, data }, replace: true })}
        className="mt-auto w-full max-w-sm rounded-full bg-primary py-3.5 text-sm font-bold uppercase tracking-wide text-white shadow-sm active:scale-[0.98]"
      >
        Siguiente
      </button>
    </div>
  );
}
