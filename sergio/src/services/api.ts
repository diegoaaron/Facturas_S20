import type { InvoiceData } from "../types/Invoice";

const ENDPOINT_URL = import.meta.env.VITE_GEMMA_ENDPOINT_URL as string | undefined;
const TIMEOUT_MS = 30_000;

export type ExtractInvoiceErrorCode = "timeout" | "network" | "server" | "config" | "parse";

export class ExtractInvoiceError extends Error {
  code: ExtractInvoiceErrorCode;

  constructor(code: ExtractInvoiceErrorCode, message: string) {
    super(message);
    this.name = "ExtractInvoiceError";
    this.code = code;
  }
}

interface GemmaResponse {
  ok: boolean;
  campos?: {
    ruc?: string;
    fecha_emision?: string;
    numero_factura?: string;
    monto_total?: string | number;
  };
  revisar?: string[];
  confiable?: boolean;
  error?: string;
}

function isGemmaResponse(value: unknown): value is GemmaResponse {
  return typeof value === "object" && value !== null && "ok" in value;
}

/** "2025-12-26" -> "26/12/2025". Si no reconoce el formato, devuelve el texto tal cual llegó. */
function formatFechaEmision(raw: string | undefined): string {
  if (!raw) return "";
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(raw);
  if (!match) return raw;
  const [, year, month, day] = match;
  return `${day}/${month}/${year}`;
}

async function dataUrlToBlob(dataUrl: string): Promise<Blob> {
  const response = await fetch(dataUrl);
  return response.blob();
}

export async function extractInvoice(imageBase64: string): Promise<InvoiceData> {
  if (!ENDPOINT_URL) {
    throw new ExtractInvoiceError(
      "config",
      "No se configuró VITE_GEMMA_ENDPOINT_URL. Revisa tu archivo .env.",
    );
  }

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), TIMEOUT_MS);

  try {
    const imageBlob = await dataUrlToBlob(imageBase64);
    const formData = new FormData();
    formData.append("imagen", imageBlob, "factura.jpg");

    const response = await fetch(ENDPOINT_URL, {
      method: "POST",
      body: formData,
      signal: controller.signal,
    });

    if (!response.ok) {
      throw new ExtractInvoiceError(
        "server",
        `El servidor respondió con un error (${response.status}). Intenta nuevamente.`,
      );
    }

    let data: unknown;
    try {
      data = await response.json();
    } catch {
      throw new ExtractInvoiceError("parse", "No se pudo interpretar la respuesta del servidor.");
    }

    if (!isGemmaResponse(data)) {
      throw new ExtractInvoiceError("parse", "La respuesta del servidor no tiene el formato esperado.");
    }

    if (!data.ok) {
      throw new ExtractInvoiceError(
        "server",
        data.error ?? "No se pudo extraer la información de la factura.",
      );
    }

    const campos = data.campos ?? {};
    const montoTotal =
      typeof campos.monto_total === "number"
        ? campos.monto_total
        : parseFloat(campos.monto_total ?? "0") || 0;

    const invoiceData: InvoiceData = {
      tipoDocumento: "Factura",
      proveedor: "",
      ruc: campos.ruc ?? "",
      numeroDocumento: campos.numero_factura ?? "",
      fechaEmision: formatFechaEmision(campos.fecha_emision),
      moneda: "PEN",
      montoTotal,
      formaPago: null,
      diasCredito: 0,
      camposParaRevisar: data.revisar ?? [],
      confiable: data.confiable ?? true,
    };

    return invoiceData;
  } catch (error) {
    if (error instanceof ExtractInvoiceError) throw error;

    if (error instanceof DOMException && error.name === "AbortError") {
      throw new ExtractInvoiceError(
        "timeout",
        "El escaneo tardó demasiado tiempo. Verifica tu conexión e intenta de nuevo.",
      );
    }

    throw new ExtractInvoiceError(
      "network",
      "No se pudo conectar con el servicio de escaneo. Verifica tu conexión e intenta de nuevo.",
    );
  } finally {
    clearTimeout(timeoutId);
  }
}
