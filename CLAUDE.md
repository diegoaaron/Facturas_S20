# CLAUDE.md

Guía para Claude Code al trabajar en este repositorio.

## Memoria del proyecto (`memorias/`)

El usuario trabaja este proyecto desde **dos computadoras**, así que la memoria persistente de Claude vive **dentro del repo**, en `memorias/`, y se sincroniza con git. **No uses la memoria local** (`~/.claude/projects/.../memory/`) para este proyecto.

- Índice (se carga automáticamente): @memorias/MEMORY.md
- Una memoria = un archivo `memorias/<NNN>_<dd>_<mm>_<yy>_<nombre_descriptivo>.md` (p. ej. `006_03_10_26_proyecto_estructura_android.md`), para ver en qué orden y cuándo se fue registrando cada cosa:
  - `NNN`: correlativo de 3 dígitos; la siguiente memoria toma el número más alto existente + 1 (no se reutilizan números de memorias borradas).
  - `dd_mm_yy`: fecha de **creación**; no cambia aunque la memoria se edite después.
  - `nombre_descriptivo`: en snake_case, sin tildes.
- Frontmatter (el `name` es el nombre del archivo sin `.md`):
  ```markdown
  ---
  name: <NNN>_<dd>_<mm>_<yy>_<nombre_descriptivo>
  description: <resumen de una línea para decidir si es relevante>
  metadata:
    type: user | feedback | project | reference
  ---

  <el hecho; para feedback/project añade líneas **Why:** y **How to apply:**. Enlaza otras memorias con [[name]].>
  ```
- Tras crear o editar un archivo, añade/actualiza su línea en `memorias/MEMORY.md` (`- [Título](archivo.md) — gancho`), en orden de número. Nunca pongas contenido en el índice.
- Antes de crear una memoria, revisa si ya existe una que lo cubra y actualízala; borra las que queden obsoletas. Convierte fechas relativas en absolutas.
- No guardes lo que ya está en el código, en git o en este `CLAUDE.md`. Nunca guardes secretos (URLs de `.env`, contraseñas, tokens).
- Al terminar de modificar memorias, recuerda al usuario que haga commit + push para que la otra computadora las reciba (y `git pull` al empezar en la otra).

## De qué va el proyecto

**Facturas S20** es una **app Android nativa en Java** (en español, orientada a Perú) para escanear facturas y boletas con la cámara del teléfono. Sus datos se extraen con un modelo **Gemma 4 E2B afinado (LoRA)** que corre **dentro del teléfono** (formato `.litertlm` con LiteRT-LM), y la app ayuda al titular a controlar su categoría y a hacer su declaración mensual del NRUS.

Claves de la arquitectura (el detalle está en `roadmap/entregable2_documentacion_tecnica.md`):
- **Java en al menos el 50 % del código** (exigencia del curso; se apunta a ~85 %). Lo único en Kotlin es el puente con LiteRT-LM, porque su API es Kotlin.
- **Un solo usuario y sin servidor.** Toda la data vive en el teléfono, en SQLite con Room + SQLCipher. El usuario exporta sus datos (ZIP con CSV, imágenes y PDF) para analizarlos en una PC. No hay cuentas, sincronización, nube ni consola de administración.
- **Funciona sin conexión.** La única conexión de red es la descarga única del modelo desde Hugging Face. En teléfonos sin RAM suficiente hay un modo de registro manual.
- Se desarrolla con **IntelliJ IDEA** (plugin de Android + Gradle). El proyecto Android todavía no está creado en el repo.

### Prototipo previo (lo que hay hoy en el repo)

Antes de pasar a Android se hizo una PWA, que queda como prototipo de referencia (competencia Build with Gemma). **No es el producto final.** Tiene dos partes:

1. **Frontend** (raíz del repo): React 19 + TypeScript + Vite 8 + Tailwind CSS 4 + React Router 7. Se despliega en Vercel.
2. **Modelo / backend de inferencia** (`tunning_gemma4/`): notebooks de Google Colab que afinan `unsloth/gemma-4-E2B-it` con Unsloth + TRL y luego exponen un servidor FastAPI (`POST /extraer`) publicado mediante un túnel de Cloudflare (`*.trycloudflare.com`). No hay backend persistente: el túnel vive mientras corre el Colab. Los notebooks siguen sirviendo para el ajuste fino del modelo de la app Android.

Las secciones siguientes (comandos, configuración, arquitectura del frontend y trampas) describen **este prototipo**.

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
- `sergio/` es una copia casi idéntica del proyecto (con su propio `dist/` y una carpeta `diseno_app/` que solo tiene un `test.txt`); en el código solo difiere `src/pages/ScanCamera.tsx` (allí se usa `getUserMedia` + `<video>`/`<canvas>`). La app activa es la de la raíz; no edites `sergio/` salvo que se pida.
- `design_factu/*.pdf` — diseños de referencia de la UI (iteraciones 1 a 8; “Diseño 8 Final” es la versión final).
- Archivos de prueba sin uso: `Test`, `mitest.txt`, `prueba.txt`.
- `node_modules` se llegó a commitear y luego se eliminó; está en `.gitignore`, no volver a añadirlo.

## Roadmap (`roadmap/`)

Plan de trabajo del desarrollo. Todo lo que sea roadmap, cronograma, pendientes o requerimientos por avanzar va aquí:

- `entregable2_documentacion_tecnica.md` — referencia técnica para construir la app Android: arquitectura, contratos, esquema SQLite, pantallas, RF/RNF (§11) y plan por iteraciones (§13). Si el código la contradice, gana el documento, salvo que se decida cambiarlo.
- `roadmap_cronograma_y_pendientes.md` — entregas del curso, iteraciones I1–I6, responsables, estado del Gantt y mediciones comprometidas por entrega. Si se cambia el plan, conviene reflejarlo también en los scripts del entregable correspondiente.

## Documentos teóricos (`documentos_teoricos/`)

Entregables del curso (UTP, Curso Integrador I). Cada entregable tiene su subcarpeta `documentos_teoricos/entregable<N>/`, y dentro los archivos siguen la convención `entregable<N>_<tipo>.<ext>`:

- `entregable<N>_baseX.*` — material base que entrega el curso/usuario (enunciado, rúbrica, descripción del proyecto). En el entregable 2 los de apoyo tienen nombre descriptivo: `entregable2_puntos_evaluacion.pdf` (consigna y rúbrica, el principal), `entregable2_diferencias_con_entregable1.pdf` (estructura oficial del informe y anexos A–S) y `entregable2_consideraciones.docx` (notas del docente).
- `entregable<N>_documento.docx` — informe generado para ese entregable.
- `entregable<N>_presentacion.pptx` — sustentación generada para ese entregable.
- `entregable<N>_documentacion_tecnica.md` — (desde el 2) referencia para desarrolladores; el usuario la mueve a `roadmap/` para la conversación de código.
- `entregable<N>_archivos/` — (desde el 2) figuras PNG, fuentes SVG editables y `scripts/` que regeneran figuras, Word y PPT (ver su `README.md`).

El entregable 1 corresponde al APF1 y el 2 al APF2. Al añadir nuevos entregables, crea su subcarpeta, sigue la misma convención y referencia los archivos por esa ruta.

**Entregable 2:** el Word **no se edita a mano**: `entregable2/entregable2_archivos/scripts/construir_documento.py` lo arma sobre `entregable1/entregable1_documento.docx` (reutiliza sus bloques por índice de elemento del cuerpo y añade el contenido de `contenido_documento.py`), y `actualizar_indice.ps1` regenera el índice con Word (`-Docx` elige el archivo). `construir_documento_solo.py` genera `solo_entregable2_documento.docx` (solo los apartados del APF2) a partir del informe completo; `generar_todo.py` corre todo. Requiere Python de Windows (`C:\Users\diego\AppData\Local\Python\bin\python.exe`, con `lxml`, `Pillow`, `python-pptx`, `pymupdf`), Chrome (SVG → PNG) y Word/PowerPoint (COM) para revisar en PDF; LibreOffice y pandoc no están instalados.

## Notebooks de fine-tuning (`tunning_gemma4/`)

Son iteraciones incrementales (`_1` … `_6`); **`tunning_model_gemma4_6.ipynb` es la más completa**. Pasos: instalar Unsloth/TRL/transformers 5.5 → montar Google Drive (`/MyDrive/dataset_facturas/` con `data.json` + `imagenes/`) → cargar el LoRA existente o crear uno nuevo sobre el modelo base en 4 bits → entrenar con `SFTTrainer` (resolución 896, 3 épocas, 80/20 train/val) → guardar el LoRA en Drive → evaluar por campo con validación de RUC/fecha → levantar FastAPI en el puerto 8000 → exponerlo con `cloudflared tunnel`. Requieren GPU de Colab; no se pueden ejecutar localmente.
