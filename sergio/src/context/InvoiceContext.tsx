import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import type { Invoice, InvoiceData } from "../types/Invoice";

interface InvoiceContextValue {
  invoices: Invoice[];
  addInvoice: (data: InvoiceData, imagenBase64: string) => Invoice;
  getInvoice: (id: string) => Invoice | undefined;
  deleteInvoice: (id: string) => void;
}

const STORAGE_KEY = "facturas-s20:invoices";

function readInvoices(): Invoice[] {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return [];
  try {
    return JSON.parse(raw) as Invoice[];
  } catch {
    return [];
  }
}

const InvoiceContext = createContext<InvoiceContextValue | undefined>(undefined);

export function InvoiceProvider({ children }: { children: ReactNode }) {
  const [invoices, setInvoices] = useState<Invoice[]>(() => readInvoices());

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(invoices));
  }, [invoices]);

  function addInvoice(data: InvoiceData, imagenBase64: string): Invoice {
    const invoice: Invoice = {
      ...data,
      id: crypto.randomUUID(),
      imagenBase64,
      createdAt: new Date().toISOString(),
    };
    setInvoices((prev) => [invoice, ...prev]);
    return invoice;
  }

  function getInvoice(id: string) {
    return invoices.find((inv) => inv.id === id);
  }

  function deleteInvoice(id: string) {
    setInvoices((prev) => prev.filter((inv) => inv.id !== id));
  }

  return (
    <InvoiceContext.Provider value={{ invoices, addInvoice, getInvoice, deleteInvoice }}>
      {children}
    </InvoiceContext.Provider>
  );
}

export function useInvoices(): InvoiceContextValue {
  const ctx = useContext(InvoiceContext);
  if (!ctx) throw new Error("useInvoices debe usarse dentro de InvoiceProvider");
  return ctx;
}
