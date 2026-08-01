import { useCallback, useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { AlertTriangle } from "lucide-react";
import { extractInvoice, ExtractInvoiceError } from "../services/api";
import type { InvoiceData } from "../types/Invoice";

interface LocationState {
  imageBase64?: string;
}

export default function ScanProcessing() {
  const navigate = useNavigate();
  const location = useLocation();
  const { imageBase64 } = (location.state as LocationState | null) ?? {};

  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const runExtraction = useCallback(
    async (image: string) => {
      setLoading(true);
      setError(null);
      try {
        const data: InvoiceData = await extractInvoice(image);
        navigate("/scan/complete", { state: { imageBase64: image, data }, replace: true });
      } catch (err) {
        const message =
          err instanceof ExtractInvoiceError
            ? err.message
            : "Ocurrió un error inesperado al procesar la factura.";
        setError(message);
        setLoading(false);
      }
    },
    [navigate],
  );

  useEffect(() => {
    if (!imageBase64) return;
    runExtraction(imageBase64);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (!imageBase64) {
    return (
      <div className="flex h-dvh flex-col items-center justify-center gap-4 bg-white px-8 text-center">
        <p className="text-sm text-text-secondary">
          No se encontró ninguna foto capturada. Vuelve a tomar la foto de tu factura.
        </p>
        <button
          type="button"
          onClick={() => navigate("/scan/camera", { replace: true })}
          className="rounded-full bg-primary px-6 py-3 text-sm font-semibold text-white"
        >
          Ir a la cámara
        </button>
      </div>
    );
  }

  return (
    <div className="relative flex h-dvh flex-col items-center justify-center bg-black px-8">
      <img
        src={imageBase64}
        alt="Factura capturada"
        className="absolute inset-0 h-full w-full object-cover"
      />
      <div className="absolute inset-0 bg-black/60" />

      <div className="relative z-10 flex flex-col items-center gap-6 text-center text-white">
        {loading && (
          <>
            <div className="h-14 w-14 animate-spin rounded-full border-4 border-white/25 border-t-white" />
            <p className="text-base font-semibold">Escaneando...</p>
          </>
        )}

        {error && (
          <>
            <AlertTriangle size={40} className="text-warning" />
            <p className="max-w-xs text-sm text-white/90">{error}</p>
            <button
              type="button"
              onClick={() => runExtraction(imageBase64)}
              className="rounded-full bg-accent px-6 py-3 text-sm font-semibold text-white"
            >
              Reintentar
            </button>
            <button
              type="button"
              onClick={() => navigate("/scan/camera", { replace: true })}
              className="text-xs font-medium text-white/80 underline"
            >
              Tomar otra foto
            </button>
          </>
        )}
      </div>
    </div>
  );
}
