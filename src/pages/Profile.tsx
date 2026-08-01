import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  ArrowLeft,
  ChevronRight,
  FileText,
  KeyRound,
  LogOut,
  Settings,
  User as UserIcon,
  UserPlus,
} from "lucide-react";
import BottomNav from "../components/BottomNav";
import Card from "../components/Card";
import Button from "../components/Button";
import { useAuth } from "../context/AuthContext";
import { useInvoices } from "../context/InvoiceContext";

export default function Profile() {
  const navigate = useNavigate();
  const { user, logout, updateProfile } = useAuth();
  const { invoices } = useInvoices();

  const [editing, setEditing] = useState(false);
  const [nombre, setNombre] = useState(user?.nombre ?? "");
  const [password, setPassword] = useState("");
  const [showHistory, setShowHistory] = useState(false);

  function handleSaveProfile() {
    updateProfile(nombre, password || undefined);
    setPassword("");
    setEditing(false);
  }

  function handleLogout() {
    logout();
    navigate("/login", { replace: true });
  }

  return (
    <div className="min-h-dvh bg-surface pb-28">
      <div className="flex items-center gap-3 bg-white px-5 pb-4 pt-[max(1.25rem,env(safe-area-inset-top))] shadow-sm">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="flex h-9 w-9 items-center justify-center rounded-full bg-surface text-text-primary"
          aria-label="Volver"
        >
          <ArrowLeft size={18} />
        </button>
        <h1 className="text-base font-bold text-text-primary">Mi Perfil</h1>
      </div>

      <div className="px-5 pt-6">
        <div className="flex flex-col items-center">
          <div className="relative">
            <div className="flex h-24 w-24 items-center justify-center rounded-full bg-primary/10">
              <UserIcon size={44} className="text-primary" />
            </div>
            <div className="absolute bottom-0 right-0 flex h-8 w-8 items-center justify-center rounded-full bg-primary text-white shadow-sm">
              <UserPlus size={16} />
            </div>
          </div>
        </div>

        <Card className="mt-6 p-5">
          {editing ? (
            <div className="flex flex-col gap-4">
              <div className="flex flex-col gap-1.5">
                <label htmlFor="nombre" className="text-xs font-medium text-text-secondary">
                  Nombres y Apellidos
                </label>
                <input
                  id="nombre"
                  value={nombre}
                  onChange={(e) => setNombre(e.target.value)}
                  className="rounded-xl border border-black/10 bg-surface px-3 py-2.5 text-sm outline-none focus:border-primary"
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <label htmlFor="password" className="text-xs font-medium text-text-secondary">
                  Nueva contraseña (opcional)
                </label>
                <input
                  id="password"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Dejar en blanco para no cambiar"
                  className="rounded-xl border border-black/10 bg-surface px-3 py-2.5 text-sm outline-none focus:border-primary"
                />
              </div>
              <div className="flex gap-3">
                <Button variant="outline" onClick={() => setEditing(false)}>
                  Cancelar
                </Button>
                <Button onClick={handleSaveProfile}>Guardar</Button>
              </div>
            </div>
          ) : (
            <div className="flex flex-col gap-4">
              <div className="flex items-center gap-3">
                <UserIcon size={18} className="text-primary" />
                <div className="flex-1">
                  <p className="text-xs text-text-secondary">Nombres y Apellidos</p>
                  <p className="text-sm font-semibold text-text-primary">{user?.nombre}</p>
                </div>
              </div>
              <div className="flex items-center gap-3">
                <KeyRound size={18} className="text-primary" />
                <div className="flex-1">
                  <p className="text-xs text-text-secondary">Contraseña</p>
                  <p className="text-sm font-semibold text-text-primary">••••••••</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setEditing(true)}
                className="self-end text-sm font-semibold text-primary"
              >
                Editar &gt;
              </button>
            </div>
          )}
        </Card>

        <Card className="mt-4 divide-y divide-black/5 p-2">
          <button
            type="button"
            onClick={() => setShowHistory((v) => !v)}
            className="flex w-full items-center gap-3 px-3 py-3.5 text-left"
          >
            <FileText size={18} className="text-text-secondary" />
            <span className="flex-1 text-sm font-medium text-text-primary">Historial</span>
            <span className="text-xs text-text-secondary">{invoices.length}</span>
            <ChevronRight size={16} className="text-text-secondary" />
          </button>

          <button
            type="button"
            onClick={() => navigate("/summary")}
            className="flex w-full items-center gap-3 px-3 py-3.5 text-left"
          >
            <Settings size={18} className="text-text-secondary" />
            <span className="flex-1 text-sm font-medium text-text-primary">Ajustes</span>
            <ChevronRight size={16} className="text-text-secondary" />
          </button>

          <button
            type="button"
            onClick={handleLogout}
            className="flex w-full items-center gap-3 px-3 py-3.5 text-left"
          >
            <LogOut size={18} className="text-warning" />
            <span className="flex-1 text-sm font-medium text-warning">Cerrar sesión</span>
          </button>
        </Card>

        {showHistory && (
          <Card className="mt-4 p-4">
            <p className="mb-3 text-sm font-semibold text-text-primary">
              Historial de facturas ({invoices.length})
            </p>
            {invoices.length === 0 ? (
              <p className="text-sm text-text-secondary">Aún no registras facturas.</p>
            ) : (
              <ul className="space-y-2">
                {invoices.map((inv) => (
                  <li key={inv.id}>
                    <button
                      type="button"
                      onClick={() => navigate(`/invoice/${inv.id}`)}
                      className="flex w-full items-center justify-between rounded-lg px-2 py-2 text-left text-sm hover:bg-surface"
                    >
                      <span className="truncate text-text-primary">{inv.proveedor}</span>
                      <span className="text-text-secondary">S/ {inv.montoTotal.toFixed(2)}</span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </Card>
        )}
      </div>

      <BottomNav />
    </div>
  );
}
