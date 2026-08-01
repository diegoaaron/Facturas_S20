import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Eye, EyeOff, ScanSearch } from "lucide-react";
import Button from "../components/Button";
import { useAuth } from "../context/AuthContext";

export default function Login() {
  const navigate = useNavigate();
  const { login, loginWithGoogle } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [infoMessage, setInfoMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(email, password);
      navigate("/summary", { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "No se pudo iniciar sesión.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleGoogleLogin() {
    setError(null);
    setSubmitting(true);
    try {
      await loginWithGoogle();
      navigate("/summary", { replace: true });
    } catch {
      setError("No se pudo iniciar sesión con Gmail.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-dvh flex-col bg-white px-8 py-10">
      <div className="flex flex-col items-center gap-2">
        <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-primary">
          <ScanSearch size={30} className="text-white" strokeWidth={1.75} />
        </div>
        <p className="text-lg font-extrabold tracking-tight text-primary">FACTURAS S20</p>
      </div>

      <div className="mt-10">
        <h1 className="text-2xl font-bold text-text-primary">Iniciar sesión</h1>
        <p className="mt-1 text-sm text-text-secondary">
          Lleve el control de sus facturas de manera fácil, rápida y segura.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="mt-8 flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <label htmlFor="email" className="text-sm font-medium text-text-primary">
            Correo de gmail
          </label>
          <input
            id="email"
            type="text"
            required
            autoComplete="username"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="nombre@gmail.com"
            className="rounded-xl border border-black/10 bg-surface px-4 py-3 text-sm outline-none focus:border-primary"
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="password" className="text-sm font-medium text-text-primary">
            Contraseña
          </label>
          <div className="relative">
            <input
              id="password"
              type={showPassword ? "text" : "password"}
              required
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              className="w-full rounded-xl border border-black/10 bg-surface px-4 py-3 pr-11 text-sm outline-none focus:border-primary"
            />
            <button
              type="button"
              onClick={() => setShowPassword((v) => !v)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-text-secondary"
              aria-label={showPassword ? "Ocultar contraseña" : "Mostrar contraseña"}
            >
              {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
            </button>
          </div>
          <button
            type="button"
            onClick={() =>
              setInfoMessage("Escribe a soporte@facturas-s20.com para restablecer tu contraseña.")
            }
            className="self-end text-xs font-medium text-primary"
          >
            ¿Olvidó su contraseña?
          </button>
        </div>

        {error && <p className="text-sm text-warning">{error}</p>}
        {infoMessage && <p className="text-xs text-text-secondary">{infoMessage}</p>}

        <Button type="submit" disabled={submitting} className="mt-2">
          {submitting ? "Ingresando..." : "Aceptar"}
        </Button>
      </form>

      <div className="my-6 flex items-center gap-3">
        <div className="h-px flex-1 bg-black/10" />
        <span className="text-xs text-text-secondary">o</span>
        <div className="h-px flex-1 bg-black/10" />
      </div>

      <Button variant="outline" onClick={handleGoogleLogin} disabled={submitting}>
        Iniciar sesión con Gmail
      </Button>

      <p className="mt-6 text-center text-xs text-text-secondary">
        Iniciar sesión con el{" "}
        <span className="font-semibold text-primary underline">Móvil</span>
      </p>

      <p className="mt-auto pt-10 text-center text-sm text-text-secondary">
        ¿No tienes una cuenta?{" "}
        <Link to="/register" className="font-semibold text-primary">
          Regístrate
        </Link>
      </p>
    </div>
  );
}
