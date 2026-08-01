import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, CameraOff, ImageUp } from "lucide-react";

export default function ScanCamera() {
  const navigate = useNavigate();
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [error, setError] = useState<string | null>(null);
  const [ready, setReady] = useState(false);
  const [fileError, setFileError] = useState<string | null>(null);

  const startCamera = useCallback(async () => {
    setError(null);
    setReady(false);

    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: "environment" },
        audio: false,
      });
      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play();
      }
      setReady(true);
    } catch {
      setError(
        "No se pudo acceder a la cámara. Verifica que hayas dado permiso de cámara a este sitio.",
      );
    }
  }, []);

  useEffect(() => {
    startCamera();
    return () => {
      streamRef.current?.getTracks().forEach((track) => track.stop());
    };
  }, [startCamera]);

  function goToProcessing(imageBase64: string) {
    streamRef.current?.getTracks().forEach((track) => track.stop());
    navigate("/scan/processing", { state: { imageBase64 }, replace: true });
  }

  function handleCapture() {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas) return;

    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    goToProcessing(canvas.toDataURL("image/jpeg", 0.9));
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
        ref={fileInputRef}
        type="file"
        accept="image/*"
        className="hidden"
        onChange={handleFileSelected}
      />

      {error ? (
        <div className="flex h-full flex-col items-center justify-center gap-4 px-10 text-center text-white">
          <CameraOff size={48} className="text-white/70" />
          <p className="text-sm text-white/90">{error}</p>
          <button
            type="button"
            onClick={startCamera}
            className="rounded-full bg-accent px-6 py-3 text-sm font-semibold text-white"
          >
            Reintentar
          </button>
          <div className="mt-2 flex items-center gap-3 text-white/50">
            <div className="h-px w-10 bg-white/20" />
            <span className="text-xs">o</span>
            <div className="h-px w-10 bg-white/20" />
          </div>
          <button
            type="button"
            onClick={handlePickFromGallery}
            className="flex items-center gap-2 rounded-full border border-white/30 px-5 py-2.5 text-sm font-medium text-white"
          >
            <ImageUp size={16} />
            Subir foto de la galería
          </button>
          {fileError && <p className="text-xs text-warning">{fileError}</p>}
        </div>
      ) : (
        <>
          <video
            ref={videoRef}
            playsInline
            muted
            className="h-full w-full object-cover"
          />

          <div className="pointer-events-none absolute inset-x-8 top-1/2 aspect-[3/4] -translate-y-1/2 rounded-2xl border-2 border-white/60" />

          <div className="absolute inset-x-0 bottom-0 flex flex-col items-center gap-3 pb-[max(2rem,env(safe-area-inset-bottom))] pt-6">
            <p className="text-sm text-white/90">Encuadra la factura dentro del recuadro</p>
            {fileError && <p className="text-xs text-warning">{fileError}</p>}

            <div className="flex w-full items-center justify-center gap-10 px-10">
              <button
                type="button"
                onClick={handlePickFromGallery}
                className="flex h-12 w-12 items-center justify-center rounded-full bg-white/15 text-white active:scale-95"
                aria-label="Subir foto de la galería"
              >
                <ImageUp size={22} />
              </button>

              <button
                type="button"
                onClick={handleCapture}
                disabled={!ready}
                className="flex h-[72px] w-[72px] items-center justify-center rounded-full border-4 border-white/80 bg-white/20 disabled:opacity-50"
                aria-label="Capturar foto"
              >
                <span className="h-14 w-14 rounded-full bg-white" />
              </button>

              <div className="h-12 w-12" aria-hidden="true" />
            </div>
          </div>
        </>
      )}

      <canvas ref={canvasRef} className="hidden" />
    </div>
  );
}
