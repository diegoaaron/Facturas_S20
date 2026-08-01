import { useMemo, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { AlertTriangle, ArrowLeft, MoreVertical, Save, Trash2 } from "lucide-react";
import Card from "../components/Card";
import { useInvoices } from "../context/InvoiceContext";
import type { FormaPago, InvoiceData } from "../types/Invoice";
import { formatCurrency } from "../lib/format";

interface LocationState {
  imageBase64?: string;
  data?: InvoiceData;
}

interface DraftFields {
  tipoDocumento: string;
  proveedor: string;
  ruc: string;
  numeroDocumento: string;
  fechaEmision: string;
  montoTotal: string;
}

const FIELD_ROWS: { key: keyof DraftFields; label: string }[] = [
  { key: "proveedor", label: "Proveedor" },
  { key: "ruc", label: "RUC" },
  { key: "numeroDocumento", label: "Nº de Factura/Boleta" },
  { key: "fechaEmision", label: "Fecha Emisión" },
];

export default function InvoiceDetails() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const { getInvoice, addInvoice, deleteInvoice } = useInvoices();

  const isNew = id === "nueva";
  const draftState = (location.state as LocationState | null) ?? {};
  const savedInvoice = !isNew && id ? getInvoice(id) : undefined;

  const data: InvoiceData | undefined = isNew ? draftState.data : savedInvoice;
  const imageBase64 = isNew ? draftState.imageBase64 : savedInvoice?.imagenBase64;

  const [formaPago, setFormaPago] = useState<FormaPago>(data?.formaPago ?? "contado");
  const [menuOpen, setMenuOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [draft, setDraft] = useState<DraftFields>({
    tipoDocumento: data?.tipoDocumento ?? "Factura",
    proveedor: data?.proveedor ?? "",
    ruc: data?.ruc ?? "",
    numeroDocumento: data?.numeroDocumento ?? "",
    fechaEmision: data?.fechaEmision ?? "",
    montoTotal: data?.montoTotal ? String(data.montoTotal) : "",
  });

  const banner = useMemo(() => imageBase64, [imageBase64]);
  const camposParaRevisar = data?.camposParaRevisar ?? [];
  const proveedorValido = draft.proveedor.trim().length > 0;

  if (!data) {
    return (
      <div className="flex h-dvh flex-col items-center justify-center gap-4 bg-white px-8 text-center">
        <p className="text-sm text-text-secondary">No se encontró esta factura.</p>
        <button
          type="button"
          onClick={() => navigate("/summary", { replace: true })}
          className="rounded-full bg-primary px-6 py-3 text-sm font-semibold text-white"
        >
          Ir al resumen
        </button>
      </div>
    );
  }

  function updateDraft<K extends keyof DraftFields>(key: K, value: DraftFields[K]) {
    setDraft((prev) => ({ ...prev, [key]: value }));
  }

  function handleSave() {
    if (!isNew || !data || !proveedorValido) return;
    setSaving(true);
    const finalData: InvoiceData = {
      tipoDocumento: draft.tipoDocumento.trim() || "Factura",
      proveedor: draft.proveedor.trim(),
      ruc: draft.ruc.trim(),
      numeroDocumento: draft.numeroDocumento.trim(),
      fechaEmision: draft.fechaEmision.trim(),
      moneda: data.moneda,
      montoTotal: parseFloat(draft.montoTotal) || 0,
      formaPago,
      diasCredito: formaPago === "credito" ? 7 : 0,
    };
    addInvoice(finalData, imageBase64 ?? "");
    navigate("/summary", { replace: true });
  }

  function handleDelete() {
    if (!savedInvoice) return;
    deleteInvoice(savedInvoice.id);
    navigate("/summary", { replace: true });
  }

  return (
    <div className="min-h-dvh bg-white pb-28">
      <div className="relative h-48 w-full overflow-hidden bg-primary">
        {banner && (
          <img src={banner} alt="Factura escaneada" className="h-full w-full object-cover" />
        )}
        <div className="absolute inset-0 bg-black/25" />

        <button
          type="button"
          onClick={() => navigate(-1)}
          className="absolute left-4 top-[max(1rem,env(safe-area-inset-top))] flex h-9 w-9 items-center justify-center rounded-full bg-black/40 text-white"
          aria-label="Volver"
        >
          <ArrowLeft size={18} />
        </button>

        <p className="absolute left-16 top-[max(1.1rem,env(safe-area-inset-top))] text-base font-semibold text-white">
          Detalles de la factura
        </p>

        {!isNew && savedInvoice && (
          <div className="absolute right-4 top-[max(1rem,env(safe-area-inset-top))]">
            <button
              type="button"
              onClick={() => setMenuOpen((v) => !v)}
              className="flex h-9 w-9 items-center justify-center rounded-full bg-black/40 text-white"
              aria-label="Más opciones"
            >
              <MoreVertical size={18} />
            </button>
            {menuOpen && (
              <div className="absolute right-0 top-11 w-44 overflow-hidden rounded-xl bg-white shadow-lg">
                <button
                  type="button"
                  onClick={handleDelete}
                  className="flex w-full items-center gap-2 px-4 py-3 text-sm font-medium text-warning"
                >
                  <Trash2 size={16} />
                  Eliminar factura
                </button>
              </div>
            )}
          </div>
        )}

        <span className="absolute bottom-4 right-4 rounded-full bg-white/90 px-3 py-1 text-xs font-semibold text-primary">
          Escaneado
        </span>
      </div>

      <div className="-mt-6 px-5">
        {isNew && (data.confiable === false || camposParaRevisar.length > 0) && (
          <div className="mb-4 flex items-start gap-2 rounded-xl bg-warning/10 px-4 py-3 text-xs text-warning">
            <AlertTriangle size={16} className="mt-0.5 shrink-0" />
            <span>
              El escaneo no detectó todos los datos con certeza. Revisa y completa los campos
              antes de guardar
              {camposParaRevisar.length > 0 && (
                <> (especialmente: {camposParaRevisar.join(", ")})</>
              )}
              .
            </span>
          </div>
        )}

        <Card className="p-5">
          {isNew ? (
            <div className="flex flex-col gap-4">
              {FIELD_ROWS.map((field) => (
                <div key={field.key} className="flex flex-col gap-1">
                  <label htmlFor={field.key} className="text-xs font-medium text-text-secondary">
                    {field.label}
                    {field.key === "proveedor" && <span className="text-warning"> *</span>}
                  </label>
                  <input
                    id={field.key}
                    value={draft[field.key]}
                    onChange={(e) => updateDraft(field.key, e.target.value)}
                    placeholder={field.key === "proveedor" ? "Nombre del proveedor" : ""}
                    className={`rounded-lg border px-3 py-2 text-sm outline-none focus:border-primary ${
                      field.key === "proveedor" && !proveedorValido
                        ? "border-warning"
                        : "border-black/10"
                    }`}
                  />
                </div>
              ))}
              <div className="flex flex-col gap-1">
                <label htmlFor="montoTotal" className="text-xs font-medium text-text-secondary">
                  Monto Total
                </label>
                <input
                  id="montoTotal"
                  type="number"
                  step="0.01"
                  min="0"
                  value={draft.montoTotal}
                  onChange={(e) => updateDraft("montoTotal", e.target.value)}
                  className="rounded-lg border border-black/10 px-3 py-2 text-sm outline-none focus:border-primary"
                />
              </div>
            </div>
          ) : (
            <dl className="divide-y divide-black/5">
              {FIELD_ROWS.map((field) => (
                <div key={field.key} className="flex items-center justify-between py-3 first:pt-0">
                  <dt className="text-sm text-text-secondary">{field.label}</dt>
                  <dd className="text-sm font-semibold text-text-primary">{data[field.key]}</dd>
                </div>
              ))}
              <div className="flex items-center justify-between py-3 last:pb-0">
                <dt className="text-sm text-text-secondary">Monto Total</dt>
                <dd className="text-base font-bold text-primary">
                  {formatCurrency(data.montoTotal, data.moneda)}
                </dd>
              </div>
            </dl>
          )}
        </Card>

        <div className="mt-6">
          <p className="mb-3 text-sm font-semibold text-text-primary">Formas de Pago</p>
          <div className="flex gap-3">
            <button
              type="button"
              disabled={!isNew}
              onClick={() => setFormaPago("contado")}
              className={`flex-1 rounded-full border py-3 text-sm font-semibold transition ${
                formaPago === "contado"
                  ? "border-primary bg-primary text-white"
                  : "border-black/10 bg-white text-text-secondary"
              } disabled:opacity-70`}
            >
              Contado
            </button>
            <button
              type="button"
              disabled={!isNew}
              onClick={() => setFormaPago("credito")}
              className={`flex-1 rounded-full border py-3 text-sm font-semibold transition ${
                formaPago === "credito"
                  ? "border-primary bg-primary text-white"
                  : "border-black/10 bg-white text-text-secondary"
              } disabled:opacity-70`}
            >
              Crédito (7 días)
            </button>
          </div>
        </div>

        {isNew && (
          <button
            type="button"
            onClick={handleSave}
            disabled={saving || !proveedorValido}
            className="mt-8 flex w-full items-center justify-center gap-2 rounded-full bg-primary py-3.5 text-sm font-bold text-white shadow-sm active:scale-[0.98] disabled:opacity-60"
          >
            <Save size={17} />
            Guardar Factura
          </button>
        )}
      </div>
    </div>
  );
}
