# CLAUDE.md

Guía para Claude Code al trabajar en este repositorio.

## Memoria del proyecto (`memorias/`)

El usuario trabaja este proyecto desde **dos computadoras**, así que la memoria persistente de Claude vive **dentro del repo**, en `memorias/`, y se sincroniza con git. **No uses la memoria local** (`~/.claude/projects/.../memory/`) para este proyecto.

- Índice (se carga automáticamente): @memorias/MEMORY.md
- Una memoria = un archivo `memorias/<slug>.md` con este frontmatter:
  ```markdown
  ---
  name: <slug-en-kebab-case>
  description: <resumen de una línea para decidir si es relevante>
  metadata:
    type: user | feedback | project | reference
  ---

  <el hecho; para feedback/project añade líneas **Why:** y **How to apply:**. Enlaza otras memorias con [[slug]].>
  ```
- Tras crear o editar un archivo, añade/actualiza su línea en `memorias/MEMORY.md` (`- [Título](archivo.md) — gancho`). Nunca pongas contenido en el índice.
- Antes de crear una memoria, revisa si ya existe una que lo cubra y actualízala; borra las que queden obsoletas. Convierte fechas relativas en absolutas.
- No guardes lo que ya está en el código, en git o en este `CLAUDE.md`. Nunca guardes secretos (URLs de `.env`, contraseñas, tokens).
- Al terminar de modificar memorias, recuerda al usuario que haga commit + push para que la otra computadora las reciba (y `git pull` al empezar en la otra).

## De qué va el proyecto

**Facturas S20** es una PWA móvil (en español, orientada a Perú) para escanear facturas/boletas con la cámara y extraer sus datos con un modelo **Gemma 4 afinado (LoRA)**, para apoyar las declaraciones mensuales. El proyecto tiene dos partes:

1. **Frontend** (raíz del repo): React 19 + TypeScript + Vite 8 + Tailwind CSS 4 + React Router 7. Se despliega en Vercel.
2. **Modelo / backend de inferencia** (`tunning_gemma4/`): notebooks de Google Colab que afinan `unsloth/gemma-4-E2B-it` con Unsloth + TRL y luego exponen un servidor FastAPI (`POST /extraer`) publicado mediante un túnel de Cloudflare (`*.trycloudflare.com`). No hay backend persistente: el túnel vive mientras corre el Colab.

## Comandos

```bash
npm install
npm run dev       # servidor de desarrollo Vite (usa el proxy /api -> túnel)
npm run build     # tsc -b && vite build  (el type-check forma parte del build)
npm run preview   # sirve el build con el mismo proxy
```

No hay linter, formateador ni tests configurados. Verifica cambios con `npm run build`.

## Configuración y conexión con Gemma

- Copia `.env.example` a `.env`:
  - `GEMMA_TUNNEL_ORIGIN` — URL del túnel Cloudflare actual (sin ruta). Solo la lee `vite.config.ts` para el proxy.
  - `VITE_GEMMA_ENDPOINT_URL=/api/extraer` — ruta fija que llama el frontend.
- **Dev/preview:** `vite.config.ts` reenvía `/api/*` a `GEMMA_TUNNEL_ORIGIN` quitando el prefijo `/api` (evita CORS).
- **Producción (Vercel):** `vercel.json` hace el mismo rewrite `/api/:path*` → URL del túnel, **hardcodeada**. Cada vez que se reinicia el Colab la URL de `trycloudflare.com` cambia y hay que actualizar `vercel.json` (y `.env` en local).

### Contrato del endpoint (`src/services/api.ts`)

- Petición: `POST /api/extraer`, `multipart/form-data` con el campo `imagen` (JPEG). Timeout de 30 s.
- Respuesta esperada:
  ```json
  { "ok": true,
    "campos": { "ruc": "...", "fecha_emision": "YYYY-MM-DD", "numero_factura": "...", "monto_total": "123.45" },
    "revisar": ["campo_con_baja_confianza"],
    "confiable": true,
    "error": "solo si ok=false" }
  ```
- `extractInvoice()` lo mapea a `InvoiceData` (`src/types/Invoice.ts`): convierte la fecha a `DD/MM/YYYY`, fija `tipoDocumento: "Factura"` y `moneda: "PEN"`; `proveedor`, `formaPago` y `diasCredito` los completa el usuario. Los errores se lanzan como `ExtractInvoiceError` con `code` (`timeout | network | server | config | parse`) y mensajes para el usuario en español.
- Si se cambian los campos que devuelve el modelo, hay que actualizar a la vez el prompt `INSTRUCCION` del notebook, el servidor FastAPI y `GemmaResponse` en `api.ts`.

## Arquitectura del frontend (`src/`)

- `App.tsx` — `AuthProvider` > `InvoiceProvider` > `BrowserRouter`. Layout de ancho móvil (`max-w-md`). Todo excepto `/`, `/login` y `/register` va envuelto en `ProtectedRoute`.
- **Flujo de escaneo** (los datos pasan entre pantallas por `navigate(..., { state })`, no por contexto):
  `/scan/intro` → `/scan/camera` (input de archivo con `capture="environment"` o galería → data URL base64) → `/scan/processing` (llama a `extractInvoice`, permite reintentar) → `/scan/complete` → `/invoice/nueva` (borrador editable; al guardar llama `addInvoice`) → `/summary`.
  `/invoice/:id` muestra una factura guardada. Si se entra a una pantalla sin su `state`, se muestra un mensaje y se redirige.
- `context/AuthContext.tsx` — autenticación **simulada** en `localStorage` (`facturas-s20:users`, `facturas-s20:session`). Contraseñas en texto plano; usuarios demo `demo@facturas.com / demo1234` y `user / user`; “login con Google” es un mock. Solo para prototipo.
- `context/InvoiceContext.tsx` — facturas persistidas en `localStorage` (`facturas-s20:invoices`), incluida la imagen en base64 (ojo con el límite de ~5 MB de localStorage).
- `lib/format.ts` — moneda (`S/` o `$`), fechas relativas (`Hoy, 26SEP`), color/inicial de avatar.
- `components/` — `Button`, `Card`, `BottomNav`, `StatusBadge`, `ProtectedRoute`.
- Estilos: Tailwind 4 con tokens en `@theme` de `src/index.css` (`primary #1b4d45`, `accent #8bc34a`, `warning`, `success`, `surface`, `text-primary`, `text-secondary`). Usa estas clases (`bg-primary`, `text-text-secondary`…) en lugar de colores sueltos. Iconos con `lucide-react`.

## Convenciones

- Todo el texto de la UI, mensajes de error y comentarios van en **español**.
- Componentes funcionales con `export default function`, hooks de contexto `useAuth()` / `useInvoices()`.
- TypeScript estricto (`noUnusedLocals`, `noUnusedParameters`): el build falla con variables sin usar.
- Dominio peruano: RUC de 11 dígitos (el notebook valida prefijo 10/15/17/20 y dígito verificador módulo 11), moneda por defecto PEN.

## Trampas conocidas del repo

- **`index.html` de la raíz es un build, no el fuente.** Carga `/assets/index-D-068JsU.js` (bundle compilado y commiteado en `assets/`) en lugar de `/src/main.tsx`. Ese bundle es antiguo (usa `getUserMedia`, la versión anterior de la cámara), así que los cambios en `src/` **no se reflejan** en `npm run dev` ni en `npm run build` mientras sea así. La versión correcta del fuente está en `sergio/index.html` (`<script type="module" src="/src/main.tsx">`). Confirma con el usuario antes de corregirlo, porque puede afectar al despliegue en Vercel.
- `icon.svg` y `manifest.json` están duplicados en la raíz y en `public/` (los de `public/` son los que usa Vite).
- `sergio/` es una copia casi idéntica del proyecto (con su propio `dist/`); solo difiere `src/pages/ScanCamera.tsx` (allí se usa `getUserMedia` + `<video>`/`<canvas>`). La app activa es la de la raíz; no edites `sergio/` salvo que se pida.
- `Angie/Index.html` es un prototipo HTML estático independiente (“Wasky App”, otra paleta). No forma parte de la app React.
- `design_factu/*.pdf` — diseños de referencia de la UI (iteraciones 1 a 8; “Diseño 8 Final” es la versión final).
- Archivos de prueba sin uso: `Test`, `mitest.txt`, `prueba.txt`.
- `node_modules` se llegó a commitear y luego se eliminó; está en `.gitignore`, no volver a añadirlo.

## Documentos teóricos (`documentos_teoricos/`)

Entregables del curso (UTP, Curso Integrador I). Cada entregable tiene su subcarpeta `documentos_teoricos/entregable<N>/`, y dentro los archivos siguen la convención `entregable<N>_<tipo>.<ext>`:

- `entregable<N>_baseX.*` — material base que entrega el curso/usuario (enunciado, rúbrica, descripción del proyecto). En el entregable 2 los de apoyo tienen nombre descriptivo: `entregable2_puntos_evaluacion.pdf` (consigna y rúbrica, el principal), `entregable2_diferencias_con_entregable1.pdf` (estructura oficial del informe y anexos A–S) y `entregable2_consideraciones.docx` (notas del docente).
- `entregable<N>_documento.docx` — informe generado para ese entregable.
- `entregable<N>_presentacion.pptx` — sustentación generada para ese entregable.
- `entregable<N>_documentacion_tecnica.md` — (desde el 2) referencia para desarrolladores; el usuario la mueve a otra ruta para la conversación de código.
- `entregable<N>_archivos/` — (desde el 2) figuras PNG, fuentes SVG editables y `scripts/` que regeneran figuras, Word y PPT (ver su `README.md`).

El entregable 1 corresponde al APF1 y el 2 al APF2. Al añadir nuevos entregables, crea su subcarpeta, sigue la misma convención y referencia los archivos por esa ruta.

**Entregable 2:** el Word **no se edita a mano**: `entregable2/entregable2_archivos/scripts/construir_documento.py` lo arma sobre `entregable1/entregable1_documento.docx` (reutiliza sus bloques por índice de elemento del cuerpo y añade el contenido de `contenido_documento.py`), y `actualizar_indice.ps1` regenera el índice con Word (`-Docx` elige el archivo). `construir_documento_solo.py` genera `solo_entregable2_documento.docx` (solo los apartados del APF2) a partir del informe completo; `generar_todo.py` corre todo. Requiere Python de Windows (`C:\Users\diego\AppData\Local\Python\bin\python.exe`, con `lxml`, `Pillow`, `python-pptx`, `pymupdf`), Chrome (SVG → PNG) y Word/PowerPoint (COM) para revisar en PDF; LibreOffice y pandoc no están instalados.

## Notebooks de fine-tuning (`tunning_gemma4/`)

Son iteraciones incrementales (`_1` … `_6`); **`tunning_model_gemma4_6.ipynb` es la más completa**. Pasos: instalar Unsloth/TRL/transformers 5.5 → montar Google Drive (`/MyDrive/dataset_facturas/` con `data.json` + `imagenes/`) → cargar el LoRA existente o crear uno nuevo sobre el modelo base en 4 bits → entrenar con `SFTTrainer` (resolución 896, 3 épocas, 80/20 train/val) → guardar el LoRA en Drive → evaluar por campo con validación de RUC/fecha → levantar FastAPI en el puerto 8000 → exponerlo con `cloudflared tunnel`. Requieren GPU de Colab; no se pueden ejecutar localmente.
