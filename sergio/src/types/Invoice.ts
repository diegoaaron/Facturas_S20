export type FormaPago = "contado" | "credito" | null;

export interface InvoiceData {
  tipoDocumento: string;
  proveedor: string;
  ruc: string;
  numeroDocumento: string;
  fechaEmision: string;
  moneda: string;
  montoTotal: number;
  formaPago: FormaPago;
  diasCredito: number;
  /** Nombres de campos que el backend marcó con baja confianza; el usuario debe revisarlos. */
  camposParaRevisar?: string[];
  /** false cuando el backend marcó la extracción completa como poco confiable. */
  confiable?: boolean;
}

export interface Invoice extends InvoiceData {
  id: string;
  imagenBase64: string;
  createdAt: string;
}
