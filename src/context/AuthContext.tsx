import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";

interface User {
  email: string;
  nombre: string;
}

interface StoredUser extends User {
  password: string;
}

interface AuthContextValue {
  user: User | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (nombre: string, email: string, password: string) => Promise<void>;
  loginWithGoogle: () => Promise<void>;
  updateProfile: (nombre: string, newPassword?: string) => void;
  logout: () => void;
}

const USERS_KEY = "facturas-s20:users";
const SESSION_KEY = "facturas-s20:session";
const DEMO_USERS: StoredUser[] = [
  { email: "demo@facturas.com", password: "demo1234", nombre: "Usuario Demo" },
  { email: "user", password: "user", nombre: "Usuario Prueba" },
];

function readUsers(): StoredUser[] {
  const raw = localStorage.getItem(USERS_KEY);
  if (!raw) {
    localStorage.setItem(USERS_KEY, JSON.stringify(DEMO_USERS));
    return DEMO_USERS;
  }
  try {
    const parsed = JSON.parse(raw) as StoredUser[];
    const missing = DEMO_USERS.filter(
      (demo) => !parsed.some((u) => u.email === demo.email),
    );
    if (missing.length > 0) {
      parsed.push(...missing);
      localStorage.setItem(USERS_KEY, JSON.stringify(parsed));
    }
    return parsed;
  } catch {
    localStorage.setItem(USERS_KEY, JSON.stringify(DEMO_USERS));
    return DEMO_USERS;
  }
}

function writeUsers(users: StoredUser[]) {
  localStorage.setItem(USERS_KEY, JSON.stringify(users));
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const raw = localStorage.getItem(SESSION_KEY);
    if (raw) {
      try {
        setUser(JSON.parse(raw) as User);
      } catch {
        localStorage.removeItem(SESSION_KEY);
      }
    }
    setLoading(false);
  }, []);

  async function login(email: string, password: string) {
    const users = readUsers();
    const match = users.find(
      (u) => u.email.toLowerCase() === email.trim().toLowerCase() && u.password === password,
    );
    if (!match) {
      throw new Error("Correo o contraseña incorrectos.");
    }
    const session: User = { email: match.email, nombre: match.nombre };
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    setUser(session);
  }

  async function register(nombre: string, email: string, password: string) {
    const users = readUsers();
    if (users.some((u) => u.email.toLowerCase() === email.trim().toLowerCase())) {
      throw new Error("Ya existe una cuenta con ese correo.");
    }
    const newUser: StoredUser = { email: email.trim(), password, nombre: nombre.trim() };
    writeUsers([...users, newUser]);
    const session: User = { email: newUser.email, nombre: newUser.nombre };
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    setUser(session);
  }

  async function loginWithGoogle() {
    const googleEmail = "google.demo@facturas.com";
    const users = readUsers();
    let match = users.find((u) => u.email === googleEmail);
    if (!match) {
      match = { email: googleEmail, password: crypto.randomUUID(), nombre: "Usuario Gmail" };
      writeUsers([...users, match]);
    }
    const session: User = { email: match.email, nombre: match.nombre };
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    setUser(session);
  }

  function updateProfile(nombre: string, newPassword?: string) {
    if (!user) return;
    const users = readUsers();
    const updated = users.map((u) =>
      u.email === user.email
        ? { ...u, nombre: nombre.trim(), password: newPassword ? newPassword : u.password }
        : u,
    );
    writeUsers(updated);
    const session: User = { email: user.email, nombre: nombre.trim() };
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    setUser(session);
  }

  function logout() {
    localStorage.removeItem(SESSION_KEY);
    setUser(null);
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: user !== null,
        loading,
        login,
        register,
        loginWithGoogle,
        updateProfile,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth debe usarse dentro de AuthProvider");
  return ctx;
}
