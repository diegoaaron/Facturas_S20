import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { BarChart3, Search } from "lucide-react";
import Card from "../components/Card";
import BottomNav from "../components/BottomNav";
import StatusBadge from "../components/StatusBadge";
import { useInvoices } from "../context/InvoiceContext";
import { avatarColor, formatCurrency, initial, relativeDate } from "../lib/format";

const MONTH_NAMES = [
  "ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO",
  "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE",
];

export default function Summary() {
  const navigate = useNavigate();
  const { invoices } = useInvoices();
  const [query, setQuery] = useState("");

  const now = new Date();
  const currentMonthLabel = MONTH_NAMES[now.getMonth()];

  const monthInvoices = useMemo(
    () =>
      invoices.filter((inv) => {
        const d = new Date(inv.createdAt);
        return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear();
      }),
    [invoices, now],
  );

  const totalInvertido = monthInvoices.reduce((sum, inv) => sum + inv.montoTotal, 0);
  const porPagarCredito = monthInvoices
    .filter((inv) => inv.formaPago === "credito")
    .reduce((sum, inv) => sum + inv.montoTotal, 0);
  const porPagarContado = monthInvoices
    .filter((inv) => inv.formaPago === "contado")
    .reduce((sum, inv) => sum + inv.montoTotal, 0);

  const filteredInvoices = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return invoices;
    return invoices.filter((inv) => inv.proveedor.toLowerCase().includes(q));
  }, [invoices, query]);

  return (
    <div className="min-h-dvh bg-white pb-28">
      <div className="bg-primary px-5 pb-6 pt-[max(1.5rem,env(safe-area-inset-top))]">
        <h1 className="text-lg font-bold text-white">Resumen de Compras</h1>
      </div>

      <div className="-mt-4 px-5">
        <Card className="bg-primary p-5 text-white">
          <div className="flex items-start justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-white/70">
                Total invertido ({currentMonthLabel})
              </p>
              <p className="mt-1 text-2xl font-extrabold text-accent">
                {formatCurrency(totalInvertido, "PEN")}
              </p>
              <p className="mt-1 text-xs text-white/70">
                {monthInvoices.length} {monthInvoices.length === 1 ? "factura registrada" : "facturas registradas"}
              </p>
            </div>
            <BarChart3 size={28} className="text-accent" />
          </div>

          <div className="mt-5 grid grid-cols-2 gap-3">
            <div className="rounded-xl bg-white/10 p-3">
              <p className="text-xs text-white/70">Por pago (Crédito)</p>
              <p className="text-base font-bold text-warning">
                {formatCurrency(porPagarCredito, "PEN")}
              </p>
            </div>
            <div className="rounded-xl bg-white/10 p-3">
              <p className="text-xs text-white/70">Por pago (Contado)</p>
              <p className="text-base font-bold text-white">
                {formatCurrency(porPagarContado, "PEN")}
              </p>
            </div>
          </div>
        </Card>

        <div className="relative mt-5">
          <Search size={17} className="absolute left-4 top-1/2 -translate-y-1/2 text-text-secondary" />
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Buscar por proveedor"
            className="w-full rounded-xl border border-black/10 bg-surface py-3 pl-11 pr-4 text-sm outline-none focus:border-primary"
          />
        </div>

        <div className="mt-5 space-y-3">
          {filteredInvoices.length === 0 && (
            <p className="py-10 text-center text-sm text-text-secondary">
              {invoices.length === 0
                ? "Aún no has registrado ninguna factura."
                : "No se encontraron facturas para ese proveedor."}
            </p>
          )}

          {filteredInvoices.map((invoice) => (
            <button
              key={invoice.id}
              type="button"
              onClick={() => navigate(`/invoice/${invoice.id}`)}
              className="flex w-full items-center gap-3 rounded-2xl bg-white p-3 text-left shadow-sm"
            >
              <div
                className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full text-sm font-bold text-white"
                style={{ backgroundColor: avatarColor(invoice.proveedor) }}
              >
                {initial(invoice.proveedor)}
              </div>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-semibold text-text-primary">
                  {invoice.proveedor}
                </p>
                <p className="text-xs text-text-secondary">{relativeDate(invoice.createdAt)}</p>
              </div>
              <div className="flex flex-col items-end gap-1">
                <p className="text-sm font-bold text-text-primary">
                  {formatCurrency(invoice.montoTotal, invoice.moneda)}
                </p>
                <StatusBadge status={invoice.formaPago === "credito" ? "pendiente" : "contado"} />
              </div>
            </button>
          ))}
        </div>
      </div>

      <BottomNav />
    </div>
  );
}
