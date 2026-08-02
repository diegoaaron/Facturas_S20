import { useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, Camera, ImageUp } from "lucide-react";

export default function ScanCamera() {
  const navigate = useNavigate();
  const cameraInputRef = useRef<HTMLInputElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [fileError, setFileError] = useState<string | null>(null);

  function goToProcessing(imageBase64: string) {
    navigate("/scan/processing", { state: { imageBase64 }, replace: true });
  }

  function handleTakePhoto() {
    setFileError(null);
    cameraInputRef.current?.click();
  }

  function handlePickFromGallery() {
    setFileError(null);
    fileInputRef.current?.click();
  }

  function handleFileSelected(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;

    if (!file.type.startsWith("image/")) {
      setFileError("Elige un archivo de imagen (JPG o PNG).");
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === "string") {
        goToProcessing(reader.result);
      }
    };
    reader.onerror = () => {
      setFileError("No se pudo leer esa imagen. Intenta con otra.");
    };
    reader.readAsDataURL(file);
  }

  return (
    <div className="relative h-dvh w-full overflow-hidden bg-black">
      <button
        type="button"
        onClick={() => navigate(-1)}
        className="absolute left-4 top-[max(1rem,env(safe-area-inset-top))] z-20 flex h-10 w-10 items-center justify-center rounded-full bg-black/40 text-white"
        aria-label="Volver"
      >
        <ArrowLeft size={20} />
      </button>

      <input
        ref={cameraInputRef}
        type="file"
        accept="image/*"
        capture="environment"
        className="hidden"
        onChange={handleFileSelected}
      />
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        className="hidden"
        onChange={handleFileSelected}
      />

      <div className="flex h-full flex-col items-center justify-center gap-6 px-10 text-center text-white">
        <div className="pointer-events-none aspect-[3/4] w-full max-w-xs rounded-2xl border-2 border-dashed border-white/40" />
        <p className="text-sm text-white/90">
          Usa la cámara de tu celular para una foto nítida y bien enfocada de la factura
        </p>
        {fileError && <p className="text-xs text-warning">{fileError}</p>}

        <button
          type="button"
          onClick={handleTakePhoto}
          className="flex items-center gap-2 rounded-full bg-accent px-8 py-3.5 text-sm font-semibold text-white active:scale-95"
        >
          <Camera size={18} />
          Tomar foto
        </button>

        <button
          type="button"
          onClick={handlePickFromGallery}
          className="flex items-center gap-2 rounded-full border border-white/30 px-5 py-2.5 text-sm font-medium text-white active:scale-95"
        >
          <ImageUp size={16} />
          Subir foto de la galería
        </button>
      </div>
    </div>
  );
}
