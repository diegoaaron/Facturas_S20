import { useNavigate } from "react-router-dom";
import { ScanSearch } from "lucide-react";
import Button from "../components/Button";

export default function Landing() {
  const navigate = useNavigate();

  return (
    <div className="flex h-dvh flex-col items-center justify-between bg-white px-8 py-16">
      <div />

      <div className="flex flex-col items-center gap-4">
        <div className="flex h-24 w-24 items-center justify-center rounded-3xl bg-primary">
          <ScanSearch size={48} className="text-white" strokeWidth={1.75} />
        </div>
        <p className="text-2xl font-extrabold tracking-tight text-primary">
          FACTURAS S20
        </p>
      </div>

      <div className="w-full max-w-sm">
        <Button onClick={() => navigate("/login")}>Comenzar</Button>
      </div>
    </div>
  );
}
