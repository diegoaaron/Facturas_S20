import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { InvoiceProvider } from "./context/InvoiceContext";
import ProtectedRoute from "./components/ProtectedRoute";

import Landing from "./pages/Landing";
import Login from "./pages/Login";
import Register from "./pages/Register";
import ScanIntro from "./pages/ScanIntro";
import ScanCamera from "./pages/ScanCamera";
import ScanProcessing from "./pages/ScanProcessing";
import ScanComplete from "./pages/ScanComplete";
import InvoiceDetails from "./pages/InvoiceDetails";
import Summary from "./pages/Summary";
import Profile from "./pages/Profile";

export default function App() {
  return (
    <AuthProvider>
      <InvoiceProvider>
        <BrowserRouter>
          <div className="mx-auto min-h-dvh w-full max-w-md bg-white shadow-xl">
            <Routes>
              <Route path="/" element={<Landing />} />
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />

              <Route
                path="/scan/intro"
                element={
                  <ProtectedRoute>
                    <ScanIntro />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/scan/camera"
                element={
                  <ProtectedRoute>
                    <ScanCamera />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/scan/processing"
                element={
                  <ProtectedRoute>
                    <ScanProcessing />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/scan/complete"
                element={
                  <ProtectedRoute>
                    <ScanComplete />
                  </ProtectedRoute>
                }
              />

              <Route
                path="/invoice/:id"
                element={
                  <ProtectedRoute>
                    <InvoiceDetails />
                  </ProtectedRoute>
                }
              />

              <Route
                path="/summary"
                element={
                  <ProtectedRoute>
                    <Summary />
                  </ProtectedRoute>
                }
              />

              <Route
                path="/profile"
                element={
                  <ProtectedRoute>
                    <Profile />
                  </ProtectedRoute>
                }
              />

              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </div>
        </BrowserRouter>
      </InvoiceProvider>
    </AuthProvider>
  );
}
