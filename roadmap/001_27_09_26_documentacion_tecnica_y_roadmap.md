# Facturas S20 — Documentación técnica y roadmap del proyecto

> **Qué es este documento.** Es la base de todo lo que hay que hacer para completar el proyecto. Es la referencia para construir el código de Facturas S20: la app Android nativa en Java, de un solo usuario, con Gemma 4 E2B ejecutado en el teléfono y toda la información guardada en el propio celular (SQLite cifrada). Resume, en forma de contratos que se pueden implementar y probar, el diseño aprobado en el segundo avance (APF2) del curso Integrador I (UTP), y el roadmap para terminarlo (sección 13): entregas, iteraciones, responsables y mediciones comprometidas. Une la antigua `entregable2_documentacion_tecnica.md` con el antiguo `roadmap_cronograma_y_pendientes.md`.
>
> **Ajuste de alcance.** Respecto del primer avance se retiró el servidor de respaldo opcional: toda la información permanece en el teléfono y el usuario la exporta cuando la necesita (sección 9).
>
> **Cómo usarlo en una conversación nueva.** Este archivo es autocontenido. Si trabajas con un asistente de código, pégalo o referencia su ruta al inicio y pide trabajar por iteraciones (sección 13). La configuración del equipo de desarrollo (paso 0, ya hecho) está en `002_27_09_26_configuracion_equipo.md`. Cuando el código contradiga este documento, gana el documento, salvo que se decida cambiarlo: en ese caso, actualízalo en el mismo commit.
>
> **Versión:** 1.2 · 2026-09-28 · corte de la semana 8 del ciclo 2026-03, con el paso 0 completado.

---

## Índice

1. [Contexto y producto](#1-contexto-y-producto)
2. [Restricciones no negociables](#2-restricciones-no-negociables)
3. [Arquitectura](#3-arquitectura)
4. [Stack tecnológico](#4-stack-tecnológico)
5. [Dominio y reglas del NRUS](#5-dominio-y-reglas-del-nrus)
6. [Inferencia local con Gemma 4 (LiteRT-LM)](#6-inferencia-local-con-gemma-4-litert-lm)
7. [Datos: base local (SQLite)](#7-datos-base-local-sqlite)
8. [Interfaz: pantallas, navegación y diseño](#8-interfaz-pantallas-navegación-y-diseño)
9. [Exportación de datos](#9-exportación-de-datos)
10. [Seguridad](#10-seguridad)
11. [Requerimientos y criterios de aceptación](#11-requerimientos-y-criterios-de-aceptación)
12. [Pruebas](#12-pruebas)
13. [Roadmap: plan de construcción por iteraciones](#13-roadmap-plan-de-construcción-por-iteraciones)
14. [Convenciones](#14-convenciones)
15. [Artefactos de referencia](#15-artefactos-de-referencia)

---

## 1. Contexto y producto

**Problema.** Las bodegas acogidas al Nuevo Régimen Único Simplificado (NRUS) pagan una cuota mensual según su categoría, que se decide por el **mayor** de dos montos del mes: ingresos brutos o adquisiciones. Las facturas de compra se acumulan sin registrar y al cierre se suman a mano: se pierden facturas, el cálculo toma horas y, ante la duda, el titular declara la categoría superior (paga S/ 50 en vez de S/ 20).

**Solución.** Una app Android de un solo usuario (el bodeguero) que:

1. Fotografía la factura de compra al recibirla (guía de encuadre y nitidez).
2. La lee **dentro del teléfono, sin internet**, con Gemma 4 E2B ajustado (LoRA) sobre facturas peruanas y convertido a `.litertlm`.
3. Muestra los campos para verificarlos y guarda la factura con su imagen, cifrada, en la base local del teléfono.
4. Mantiene el acumulado del mes, determina categoría y cuota, avisa al 80 % y 100 % del límite y recuerda el vencimiento.
5. Genera el reporte mensual en PDF y **exporta los datos** (CSV, imágenes y PDF en un ZIP) para analizarlos en una PC.

**Toda la data vive en el celular.** No hay servidor propio, ni cuentas en la nube, ni respaldo remoto: ningún sistema externo recibe ni pide datos de la app. La única conexión de red es la descarga única del modelo desde su repositorio público, y no envía ningún dato del usuario.

**Usuarios (4 actores).**

| Actor | Tipo | Qué hace |
|---|---|---|
| Bodeguero | Persona (actor principal) | Registra facturas, ingresa ventas del mes, consulta, declara, instala el modelo y exporta sus datos. Sin formación contable; usa el teléfono con una mano en el mostrador. |
| Contador externo | Persona | Recibe el reporte PDF o el archivo exportado que le comparte el bodeguero. No usa la app. |
| Motor de extracción | Sistema | Gemma 4 E2B en el teléfono vía LiteRT-LM. |
| Motor de reglas NRUS | Sistema | Clase Java que aplica la norma. |

**Fuera del alcance:** presentar o pagar la declaración ante la SUNAT, registrar ventas diarias (solo el total mensual), emitir comprobantes, contabilidad completa, otros regímenes, integración con SUNAT, iOS, varios usuarios por negocio, sincronización o respaldo en la nube.

**Pérdida del teléfono.** El sustento son los reportes PDF y los archivos exportados que el usuario guardó fuera del teléfono; por eso la app sugiere exportar al cerrar cada mes (P17).

**Estado del repositorio `Facturas_S20`** (2026-09-28). El prototipo previo (PWA React + servidor FastAPI en Colab con túnel, usado en la competencia Build with Gemma) se retiró del repo; su último estado está en el tag `prototipo-pwa`. La raíz queda así:

```
android/              proyecto Gradle de la app (módulos :app y :dominio) — paso 0 hecho
tunning_gemma4/       tunning_model_gemma4_6.ipynb: entrenamiento y conversión del modelo
roadmap/              este documento (001) y la configuración del equipo (002)
documentos_teoricos/  entregables del curso (informes, sustentaciones y figuras)
memorias/             memoria de Claude Code compartida entre computadoras
```

- `tunning_gemma4/tunning_model_gemma4_6.ipynb`: cuaderno de ajuste fino (Unsloth + TRL, 896 px, 80/20). Hoy extrae 4 campos (`ruc`, `fecha_emision`, `numero_factura`, `monto_total`); debe pasar a los **7 campos** de la sección 6.2 y agregar la exportación a `.litertlm`.
- La referencia visual es el prototipo P01–P20 del APF2 (sección 15).

---

## 2. Restricciones no negociables

| # | Restricción | Motivo |
|---|---|---|
| R1 | **≥ 50 % del código en Java** (meta ≈ 85 %). Kotlin **solo** en `LiteRtLmPuente.kt`. | Exigencia del curso. La API de LiteRT-LM es Kotlin. |
| R2 | La **lectura de facturas funciona sin conexión**. Ningún flujo principal hace llamadas de red; la única conexión de la app es la descarga del modelo. | RNF-12, RNF-16. En el mostrador la señal es intermitente. |
| R3 | **La imagen de la factura nunca sale del teléfono** para ser leída; los datos solo salen si el usuario los exporta o comparte. | RNF-14. Privacidad tributaria. |
| R4 | Android **8.0 (API 26)** o superior; 6 GB de RAM recomendados para la lectura con IA; si no alcanza, **registro manual**. | RNF-07. |
| R5 | Montos con `BigDecimal` (escala 2, `RoundingMode.HALF_UP`). Nunca `double` para dinero. | Exactitud del total (RNF-03). |
| R6 | Parámetros del NRUS (límites, cuotas, cronograma) **fuera del código**, en un archivo versionado (`assets/parametros_nrus.json`). | RNF-17. Cambian por norma. |
| R7 | El paquete `dominio` **no importa** `android.*` ni LiteRT-LM. | RNF-18: se prueba con JUnit en la JVM. |
| R8 | Toda la interfaz, mensajes y comentarios **en español** (Perú). | Usuario final. |
| R9 | Base local cifrada (SQLCipher) y acceso con PIN o huella. | RNF-15. |

---

## 3. Arquitectura

### 3.1 Capas y regla de dependencias

Cuatro capas dentro del teléfono (presentación, dominio, inferencia local, datos). Los únicos elementos externos son el **repositorio público del modelo** (descarga única) y **Google Play** (distribución de la app). No hay nodo servidor.

```
┌──────────────────────────── Teléfono Android ────────────────────────────┐
│  ui (Presentación)      Activities/Fragments, ViewModels, CameraX         │
│        │ usa                                                              │
│        ▼                                                                  │
│  dominio (Java puro)    casos de uso · reglas NRUS · validadores · puertos│
│        ▲ implementan                          ▲ implementan               │
│  inferencia             ExtractorGemmaLocal ──┘   datos   Room + SQLCipher │
│        └─ litert/LiteRtLmPuente.kt (único Kotlin)   exportacion  ZIP/CSV  │
└───────────────────────────────────────────────────────────────────────────┘
         ▲ descarga única del modelo (HTTPS,          │ archivo ZIP elegido por el usuario
         │ sin datos del usuario)                     ▼ (SAF o intent de compartir)
  Hugging Face (repo público, .litertlm)       PC del usuario / contador (Excel, LibreOffice)
  Google Play: distribuye la app, assets/modelo.json y assets/parametros_nrus.json
```

- **Regla:** `ui → dominio ← inferencia, datos, exportacion`. El dominio define interfaces (**puertos**); inferencia, datos y exportación las implementan (**adaptadores**). La inyección se hace en un `ContenedorDependencias` manual (sin framework DI, para mantener Java simple) o con Hilt si el equipo lo prefiere (Hilt es Java-compatible).
- **Un solo hilo de UI.** Inferencia, base de datos, PDF y exportación en un `ExecutorService` de la app; resultados a la UI con `LiveData`.

### 3.2 Módulos Gradle

| Módulo | Tipo | Contenido | Depende de |
|---|---|---|---|
| `:dominio` | `java-library` | modelo, casos de uso, reglas, puertos | nada (solo JDK) |
| `:app` | `com.android.application` | ui, inferencia, datos, trabajos, exportación | `:dominio` |

No hay otros proyectos de producción: el único código fuera de la app es el cuaderno de entrenamiento (`tunning_gemma4/`).

### 3.3 Paquetes de la app

```
pe.facturass20
├── ui/
│   ├── acceso/         P01 ConfiguracionFragment · P02 AccesoFragment
│   ├── inicio/         P04 InicioFragment · InicioViewModel
│   ├── captura/        P05 GuiaCapturaFragment · P06 CapturaActivity · EvaluadorNitidez
│   ├── verificacion/   P07 LecturaFragment · P08 VerificacionFragment · P09 DuplicadoDialog · P10 GuardadaFragment
│   ├── facturas/       P11 ListaFacturasFragment · P12 DetalleFacturaFragment · AnulacionBottomSheet
│   ├── mes/            P13 VentasFragment · P14 CategoriaFragment · P15 VencimientoFragment
│   ├── reportes/       P16 ReporteFragment · P17 HistorialFragment
│   ├── ajustes/        P18 AjustesFragment · P03 PreparacionModeloFragment · P19 RegistroManualFragment
│   ├── exportacion/    P20 ExportarDatosFragment · ExportarDatosViewModel
│   └── comun/          MainActivity (NavHost + barra inferior), componentes y formateadores
├── inferencia/         ExtractorGemmaLocal · ParserRespuesta · GestorModeloLocal · Imagenes
│   └── litert/         LiteRtLmPuente.kt
├── datos/
│   ├── entidades/      13 entidades Room
│   ├── dao/            DAO por agregado
│   ├── BaseDatosFacturas.java (RoomDatabase + SQLCipher)
│   ├── cifrado/        GestorClaves (Keystore) · AlmacenImagenes (AES-GCM)
│   ├── parametros/     CargadorParametrosNrus (lee assets/parametros_nrus.json)
│   └── repositorios/   implementaciones de los puertos del dominio
├── exportacion/        ExportadorDatos (ZIP) · EscritorCsv
├── trabajos/           RecordatorioWorker · DescargaModeloWorker
└── App.java            Application: inicializa ContenedorDependencias y canales de notificación

(módulo :dominio)  pe.facturass20.dominio
├── modelo/     Contribuyente, PeriodoMensual, FacturaCompra, Emisor, ImagenFactura, ResultadoExtraccion,
│               CampoExtraido, Determinacion, CategoriaNRUS, CronogramaVencimiento, ParametrosNrus,
│               ModeloLocal, Aviso, ReporteMensual, SolicitudExportacion + enumeraciones
├── casosuso/   RegistrarFactura, VerificarFactura, RegistrarVentas, DeterminarCategoria,
│               CerrarPeriodo, AnularFactura, GenerarReporte (interfaz de generador), ConsultarHistorial,
│               ExportarDatos
├── reglas/     MotorReglasNRUS, ValidadorRuc, ValidadorFecha, DetectorDuplicados, Montos
└── puertos/    ExtractorFacturas, FacturaRepositorio, PeriodoRepositorio, ParametrosRepositorio,
                ContribuyenteRepositorio, AvisoProgramador, Reloj, ExportadorArchivos
```

### 3.4 Despliegue, costos y participación de Java

| Nodo | Qué aloja |
|---|---|
| Teléfono Android | App + SQLite cifrada (SQLCipher) + imágenes cifradas + modelo `.litertlm` |
| Hugging Face (repositorio público del proyecto) | Archivo `.litertlm` del modelo ajustado; descarga única |
| Google Play | Distribución de la app, con `assets/modelo.json` y `assets/parametros_nrus.json` |
| Google Colab | Solo para entrenar y convertir el modelo (fuera de producción) |

**Costos:** publicación en Google Play, pago único S/ 94. Costo mensual: S/ 0 (la inferencia es local y el modelo se aloja gratis en el repositorio público). Costo para la bodega: S/ 0.

**Participación por lenguaje (meta):** app Android (presentación, dominio, datos y exportación) — Java ≈ 85 % · puente LiteRT-LM — Kotlin ≈ 5 % · layouts, recursos y configuración — XML/Gradle ≈ 8 % · migraciones Room — SQL ≈ 2 %.

---

## 4. Stack tecnológico

> Usa la **última versión estable** de cada biblioteca al iniciar y fija las versiones en `libs.versions.toml`. Las versiones de abajo son la base de diseño; verifica compatibilidad antes de fijarlas. Las versiones ya fijadas en el paso 0 (Gradle, AGP, SDK, Java) están en `002_27_09_26_configuracion_equipo.md` §1.5; **AGP no puede pasar de 9.1.x** mientras IntelliJ 2026.2 no lo soporte.

| Capa | Tecnología |
|---|---|
| Lenguaje app | Java 17 (source/target), Kotlin solo para el puente |
| SDK | `minSdk 26`, `targetSdk`/`compileSdk` = el más reciente estable |
| UI | Material Components 3, AndroidX Navigation (Fragments), ViewBinding, ConstraintLayout |
| Cámara | CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`) |
| IA local | `com.google.ai.edge.litertlm:litertlm-android` + modelo Gemma 4 E2B ajustado (`.litertlm`) |
| Persistencia | Room (`room-runtime`, `room-compiler` con `annotationProcessor`) + SQLCipher (`net.zetetic:sqlcipher-android`) — única base de datos del sistema |
| Segundo plano | WorkManager |
| Seguridad | `androidx.biometric`, Android Keystore (`KeyGenParameterSpec`, AES/GCM) |
| Red (solo descarga del modelo) | `HttpsURLConnection` u OkHttp dentro de `DescargaModeloWorker` (reanudable con `Range`) |
| PDF | `android.graphics.pdf.PdfDocument` |
| Exportación | `java.util.zip.ZipOutputStream`, Storage Access Framework (`ACTION_CREATE_DOCUMENT`), `androidx.core.content.FileProvider` + `Intent.ACTION_SEND` |
| Pruebas app | JUnit 4/5 en `:dominio`, AndroidX Test + Espresso en `:app` |
| Alojamiento del modelo | Repositorio público del proyecto en Hugging Face |
| Distribución | Google Play |
| Entrenamiento | Python, Unsloth, TRL, Google Colab (GPU) |

---

## 5. Dominio y reglas del NRUS

### 5.1 Entidades del dominio

| Clase | Atributos | Operaciones / invariantes |
|---|---|---|
| `Contribuyente` | `ruc: String(11)`, `nombre: String`, `titular: String`, `ultimoDigitoRuc: int`, `fechaAlta: LocalDate` | `ruc` válido (5.3); `ultimoDigitoRuc = ruc.charAt(10)` |
| `PeriodoMensual` | `anio`, `mes`, `totalVentas: BigDecimal`, `estado: EstadoPeriodo` | `totalAdquisiciones()` = suma de facturas **VIGENTE**; `registrarVentas(m ≥ 0)` solo si ABIERTO; `cerrar()` exige ventas registradas |
| `FacturaCompra` | `emisor`, `serie(4)`, `numero(≤8)`, `fechaEmision`, `moneda`, `importeTotal > 0`, `estado`, `origen: OrigenRegistro`, `motivoAnulacion` | `claveUnica() = rucEmisor + serie + numero`; `anular(motivo)` solo si VIGENTE y período ABIERTO |
| `Emisor` | `ruc`, `razonSocial` | `rucValido()` |
| `ImagenFactura` | `ruta`, `nitidez: double`, `fechaCaptura` | `esLegible(umbral)` |
| `ResultadoExtraccion` | `versionModelo`, `tiempoMs`, `campos: List<CampoExtraido>` | `camposARevisar()` = confianza < 0,80 o validación fallida |
| `CampoExtraido` | `nombre: CampoFactura`, `valorLeido`, `valorFinal`, `confianza [0..1]`, `corregido` | `corregido = !valorLeido.equals(valorFinal)` |
| `Determinacion` | `totalAdquisiciones`, `montoDeterminante`, `categoria`, `cuota`, `fechaVencimiento`, `nivelAlerta` | una por período |
| `CategoriaNRUS` | `codigo`, `limiteMensual`, `cuota` | `admite(m) = m ≤ limiteMensual` |
| `CronogramaVencimiento` | `anio`, `mes`, `ultimoDigito`, `fechaLimite` | una fila por (año, mes, dígito) |
| `ParametrosNrus` | `version`, `vigenteDesde`, `umbralAviso (0,80)`, `topeAnual`, `categorias`, `cronograma` | `categoriasVigentes(fecha)` ordenadas por límite |
| `ModeloLocal` | `version`, `tamanoMb`, `sha256`, `estado: EstadoModelo`, `ruta` | `verificarIntegridad()` |
| `Aviso` | `tipo: TipoAviso`, `fechaProgramada`, `enviado` | |
| `ReporteMensual` | `fechaGeneracion`, `rutaPdf`, `sha256` | |
| `SolicitudExportacion` | `anio`, `mes` (nulo = año completo), `incluirImagenes`, `incluirReporte` | `mes` 1..12 o nulo; el período debe existir |

**Enumeraciones:** `EstadoPeriodo {ABIERTO, CERRADO}`, `EstadoFactura {VIGENTE, ANULADA}`, `OrigenRegistro {IA, MANUAL}`, `Moneda {PEN, USD}`, `NivelAlerta {NINGUNA, AVISO_80, LIMITE_100, FUERA_DE_REGIMEN}`, `EstadoModelo {NO_INSTALADO, DESCARGANDO, VERIFICADO, ACTIVO}`, `CampoFactura {RUC, RAZON_SOCIAL, SERIE, NUMERO, FECHA_EMISION, MONEDA, IMPORTE_TOTAL}`, `TipoAviso {TOPE_80, TOPE_100, VENCIMIENTO}`.

### 5.2 Parámetros del NRUS (versión 2026.1)

Viajan dentro de la app en el archivo versionado `assets/parametros_nrus.json`. Al iniciar, `CargadorParametrosNrus` compara su `version` con la registrada en la tabla local `parametro_version` y, si es nueva, la inserta (con sus categorías y cronograma) y la marca activa. **Se actualizan con una nueva versión de la app**; cambiarlos no exige modificar código Java (RNF-17). Una versión nueva aplica a períodos nuevos, no a los cerrados (RF-22). **Verificar los montos contra la norma vigente (D. Leg. 937 y modificatorias) antes de publicar.**

```json
{
  "version": "2026.1",
  "vigenteDesde": "2026-01-01",
  "umbralAviso": 0.80,
  "topeAnual": 96000.00,
  "categorias": [
    { "codigo": 1, "limiteMensual": 5000.00, "cuota": 20.00 },
    { "codigo": 2, "limiteMensual": 8000.00, "cuota": 50.00 }
  ],
  "cronograma": [
    { "anio": 2026, "mes": 9, "ultimoDigito": 4, "fechaLimite": "2026-10-15" }
  ]
}
```

> El cronograma de ejemplo es **ilustrativo**. El real lo publica la SUNAT cada año (12 meses × 10 dígitos); se carga completo en la versión de parámetros.

### 5.3 Validación del RUC (módulo 11)

- 11 dígitos, prefijo `10`, `15`, `17` o `20`.
- Pesos `5,4,3,2,7,6,5,4,3,2` sobre los 10 primeros dígitos; `d = 11 - (suma % 11)`; si `d == 10 → 0`, si `d == 11 → 1`; debe coincidir con el dígito 11.

```java
public final class ValidadorRuc {
    private static final int[] PESOS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
    private ValidadorRuc() { }
    public static boolean esValido(String ruc) {
        if (ruc == null || !ruc.matches("(10|15|17|20)\\d{9}")) return false;
        int suma = 0;
        for (int i = 0; i < 10; i++) suma += (ruc.charAt(i) - '0') * PESOS[i];
        int digito = 11 - (suma % 11);
        if (digito == 10) digito = 0; else if (digito == 11) digito = 1;
        return digito == ruc.charAt(10) - '0';
    }
}
```

Casos de prueba: válidos `10456789124`, `20601234565`, `20512345671`, `20498765433`, `20610022333`, `20455667781`; inválidos `10456789123` (dígito), `30601234565` (prefijo), `2060123456` (10 dígitos), `null`, `"20A01234565"`.

### 5.4 Motor de reglas (`MotorReglasNRUS.determinar(periodo)`)

1. `adquisiciones = periodo.totalAdquisiciones()` (solo facturas VIGENTE, en PEN; si hay USD, convertir con el tipo de cambio registrado en la factura — **fuera del alcance de esta etapa: se permite solo PEN y se valida**).
2. `determinante = max(adquisiciones, periodo.totalVentas)`.
3. `categoria` = primera categoría vigente (ordenada por límite) que `admite(determinante)`; si ninguna → `nivelAlerta = FUERA_DE_REGIMEN` y se lanza/retorna la alerta de cambio de régimen.
4. `cuota = categoria.cuota`.
5. `nivelAlerta`: `LIMITE_100` si `determinante > limite` de la categoría 1 y se pasó a la 2 (o está en el límite exacto de la última); `AVISO_80` si `determinante ≥ limite × umbralAviso`; si no `NINGUNA`. Además, si el **acumulado anual** (compras o ventas del año) ≥ `topeAnual × umbralAviso` → aviso de tope anual.
6. `fechaVencimiento = cronograma.fechaPara(anio, mes, ultimoDigitoRuc)`; si no existe → error de configuración (no inventar fechas).
7. Devuelve `Determinacion` inmutable. **No persiste**: el caso de uso guarda.

Casos de prueba mínimos (parámetros 2026.1):

| # | Compras | Ventas | Esperado |
|---|---|---|---|
| 1 | 4 120,50 | 3 900,00 | Cat. 1, cuota 20, AVISO_80 (4 120,50 ≥ 4 000) |
| 2 | 3 000,00 | 3 500,00 | Cat. 1, cuota 20, NINGUNA (determina ventas) |
| 3 | 5 000,00 | 0 | Cat. 1 (límite inclusivo), AVISO_80 |
| 4 | 5 000,01 | 0 | Cat. 2, cuota 50 |
| 5 | 8 000,01 | 0 | FUERA_DE_REGIMEN |
| 6 | 0 | 0 | Cat. 1, cuota 20, NINGUNA |
| 7 | 4 120,50 con una factura ANULADA de 386,40 | 0 | Acumulado 3 734,10 |

### 5.5 Otras reglas

- **Fecha de la factura:** debe pertenecer al período abierto (mismo año y mes) o al mes anterior mientras no esté cerrado; si no, se pide confirmar el período.
- **Duplicado:** existe otra factura VIGENTE con el mismo `ruc emisor + serie + numero` → P09, no se registra.
- **Anulación:** no borra; cambia estado, guarda motivo y recalcula.
- **Cierre del mes:** requiere ventas registradas; congela la determinación; genera el reporte si no existe y **sugiere exportar los datos del mes** (P20).

---

## 6. Inferencia local con Gemma 4 (LiteRT-LM)

### 6.1 Contratos

```java
/** Puerto del dominio. */
public interface ExtractorFacturas {
    ResultadoExtraccion extraer(byte[] imagenJpeg) throws ExtraccionException;
}

/** Contrato del motor (lo implementa el puente Kotlin). */
public interface MotorInferencia extends AutoCloseable {
    void inicializar(String rutaModelo) throws InferenciaException;   // en segundo plano; puede tardar
    String generar(File imagen, String instruccion, long timeoutMs) throws InferenciaException;
    @Override void close();
}
```

### 6.2 Instrucción y esquema de salida

Los **7 campos**: `ruc`, `razon_social`, `serie`, `numero`, `fecha_emision` (YYYY-MM-DD), `moneda` (PEN|USD), `importe_total` (punto decimal, dos decimales).

```java
static final String INSTRUCCION =
    "Extrae de esta factura peruana los campos ruc, razon_social, serie, numero, "
  + "fecha_emision (YYYY-MM-DD), moneda (PEN o USD) e importe_total (numero con punto decimal). "
  + "Responde solo un objeto JSON donde cada campo tenga 'valor' y 'confianza' entre 0 y 1. "
  + "Si un campo no se lee, usa valor null y confianza 0.";
```

```json
{
  "ruc":           { "valor": "20601234565", "confianza": 0.98 },
  "razon_social":  { "valor": "DISTRIBUIDORA ANDINA S.A.C.", "confianza": 0.95 },
  "serie":         { "valor": "F001", "confianza": 0.99 },
  "numero":        { "valor": "004821", "confianza": 0.97 },
  "fecha_emision": { "valor": "2026-09-22", "confianza": 0.96 },
  "moneda":        { "valor": "PEN", "confianza": 0.99 },
  "importe_total": { "valor": "1450.00", "confianza": 0.62 }
}
```

> **La misma instrucción y el mismo esquema** deben usarse en el cuaderno de ajuste fino (etiquetas de entrenamiento) y en la app. Si cambian los campos, se actualizan a la vez: cuaderno, `INSTRUCCION`, `ParserRespuesta`, `CampoFactura` y la tabla `campo_extraido`.

### 6.3 Reglas del parser (`ParserRespuesta`)

- Extraer el **primer objeto JSON** de la respuesta (el modelo puede envolverlo en texto o en ```json).
- Normalizar: `ruc` solo dígitos; `serie` en mayúsculas; `numero` sin ceros de relleno para comparar pero se guarda tal cual; `importe_total` acepta `1 450,00`, `1,450.00`, `1450` → `BigDecimal("1450.00")`; `fecha_emision` acepta `dd/MM/yyyy` y la convierte.
- Campo faltante o no parseable → `valor = null`, `confianza = 0`, requiere revisión.
- Si la **confianza global** (mínima de los campos clave RUC, serie, número, fecha, importe) < 0,5 → sugerir otra foto.
- Si no hay JSON válido → `ExtraccionException` con mensaje para el usuario: «No pudimos leer la factura. Tome otra foto con más luz.»

### 6.4 Preprocesado de la imagen

- Recorte al marco de encuadre, corrección de orientación EXIF, redimensionado al lado mayor de **896 px**, JPEG calidad 90.
- **Nitidez:** varianza del Laplaciano sobre la imagen en gris (umbral inicial 100, calibrar con el equipo de referencia). **Luz:** media de luminancia entre 60 y 200.

### 6.5 Puente Kotlin (único archivo Kotlin)

Basado en la guía oficial de LiteRT-LM para Android (`com.google.ai.edge.litertlm`). **Verifica los nombres exactos de la API en la versión que fijes**; la estructura es:

```kotlin
package pe.facturass20.inferencia.litert

import com.google.ai.edge.litertlm.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File

class LiteRtLmPuente : MotorInferencia {
    private var engine: Engine? = null

    override fun inicializar(rutaModelo: String) {
        val config = EngineConfig(
            modelPath = rutaModelo,
            backend = Backend.GPU(),        // si falla, reintentar con Backend.CPU()
            visionBackend = Backend.GPU()
        )
        engine = Engine(config).also { it.initialize() }   // llamar fuera del hilo de UI
    }

    override fun generar(imagen: File, instruccion: String, timeoutMs: Long): String = runBlocking {
        val conv = engine!!.createConversation()
        try {
            withTimeout(timeoutMs) {
                conv.sendMessageAsync(Contents.of(Content.ImageFile(imagen.absolutePath), Content.Text(instruccion)))
                    .toList().joinToString("") { it.toString() }
            }
        } finally { conv.close() }
    }

    override fun close() { engine?.close(); engine = null }
}
```

- En `AndroidManifest.xml`, dentro de `<application>`, declarar las bibliotecas nativas que pide la guía para GPU: `<uses-native-library android:name="libOpenCL.so" android:required="false"/>` y `<uses-native-library android:name="libvndksupport.so" android:required="false"/>`.
- Una sola instancia del motor en toda la app (`GestorModeloLocal`), inicializada al primer uso y cerrada en `onTrimMemory` crítico.
- `timeoutMs = 20_000` (RNF-04). Al vencer, ofrecer reintentar o registrar a mano.

### 6.6 Gestión del modelo (`GestorModeloLocal`)

El modelo Gemma 4 E2B ajustado (`.litertlm`, ≈ 2,6 GB) se descarga **una sola vez**, por Wi-Fi, desde el **repositorio público del proyecto en Hugging Face**. La versión, la URL, la huella SHA-256 y el tamaño vienen dentro de la app, en el manifiesto `assets/modelo.json`:

```json
{
  "version": "1.0.0",
  "url": "https://huggingface.co/<organizacion>/facturas-s20-gemma4-e2b/resolve/<revision>/facturas-s20-1.0.0.litertlm",
  "sha256": "<64 caracteres hexadecimales>",
  "tamanoBytes": 2791728742
}
```

1. **Comprobar el equipo:** `ActivityManager.MemoryInfo.totalMem ≥ 5,5 GB` (equipos de «6 GB» reportan algo menos) y espacio libre ≥ 3 GB (≥ 2 × tamaño mientras se conservan dos versiones). Si no, P19 (registro manual) y no ofrecer descarga.
2. **Manifiesto:** leer `assets/modelo.json` y compararlo con la fila activa de `modelo_local`. Si la versión del manifiesto es nueva (o no hay modelo), ofrecer la descarga en P03. La URL apunta a una revisión fija del repositorio para que el archivo no cambie bajo la misma versión.
3. **Descarga** con `DescargaModeloWorker` (solo Wi-Fi, reanudable con `Range`) a `filesDir/modelos/<version>.litertlm.part`. La petición no lleva ningún dato del usuario (ni RUC, ni identificadores): es un `GET` anónimo por HTTPS.
4. **Verificar tamaño y SHA-256** contra el manifiesto; si no coinciden, borrar y reintentar (máx. 3).
5. **Probar** con una factura de control empaquetada; si falla, conservar la versión anterior.
6. Renombrar a `.litertlm`, marcar ACTIVO, registrar en `modelo_local`; conservar la anterior hasta que la nueva funcione y luego borrarla (RNF-19).

**Actualización del modelo:** una versión nueva del modelo llega con una **actualización de la app en Google Play**, que trae el nuevo `assets/modelo.json`. Al abrir la app actualizada, `GestorModeloLocal` detecta la versión nueva y repite los pasos 3–6; mientras tanto, la lectura sigue con el modelo anterior. El APK no incluye el modelo (RNF-06), por eso cambiarlo no exige reinstalar la app (RNF-19).

### 6.7 Entrenamiento y conversión (fuera de la app)

Partir de `tunning_gemma4/tunning_model_gemma4_6.ipynb`: etiquetar los 7 campos con el esquema 6.2, LoRA sobre `unsloth/gemma-4-E2B-it` en 4 bits, 896 px, 80/20 + conjunto de prueba separado, evaluar por campo contra el modelo base, **fusionar → cuantizar → convertir a `.litertlm`** y **evaluar de nuevo después de convertir**. Publicar solo si mejora al base y logra ≥ 90 % por campo (RNF-01/02). Publicar significa: subir el `.litertlm` al repositorio público de Hugging Face, calcular su SHA-256, actualizar `assets/modelo.json` y lanzar una nueva versión de la app. Referencia de formato: `litert-community/gemma-4-E2B-it-litert-lm` en Hugging Face.

---

## 7. Datos: base local (SQLite)

La única base de datos del sistema es la SQLite del teléfono, cifrada con SQLCipher y accedida con Room. No hay base de datos remota ni réplica: si el usuario quiere llevar sus datos a una PC, los exporta (sección 9).

### 7.1 Base local (Room + SQLCipher) — 13 tablas

| Tabla | Columnas (PK **negrita**, FK *cursiva*) | Restricciones |
|---|---|---|
| `contribuyente` | **id_contribuyente**, ruc, nombre, titular, ultimo_digito, pin_hash, pin_sal, fecha_alta | ruc UNIQUE |
| `periodo` | **id_periodo**, *id_contribuyente*, anio, mes, total_ventas, estado | UQ(id_contribuyente, anio, mes); mes 1..12 |
| `emisor` | **id_emisor**, ruc, razon_social | ruc UNIQUE |
| `factura_compra` | **id_factura**, *id_periodo*, *id_emisor*, serie, numero, fecha_emision, moneda, importe_total, origen, estado, motivo_anulacion, fecha_registro | UQ(id_emisor, serie, numero); importe > 0; índice (id_periodo, estado) |
| `imagen_factura` | **id_imagen**, *id_factura* UNIQUE, ruta_cifrada, nitidez, fecha_captura | |
| `campo_extraido` | **id_campo**, *id_factura*, *id_modelo*, nombre_campo, valor_leido, valor_final, confianza, corregido | índice (id_factura) |
| `modelo_local` | **id_modelo**, version UNIQUE, tamano_mb, sha256, estado, ruta, fecha_instalacion | |
| `parametro_version` | **id_parametro**, version UNIQUE, umbral_aviso, tope_anual, vigente_desde, activo | se llena desde `assets/parametros_nrus.json` |
| `categoria_nrus` | **id_categoria**, *id_parametro*, codigo, limite_mensual, cuota | UQ(id_parametro, codigo) |
| `cronograma_venc` | **id_cronograma**, *id_parametro*, anio, mes, ultimo_digito, fecha_limite | UQ(id_parametro, anio, mes, ultimo_digito) |
| `determinacion` | **id_determinacion**, *id_periodo* UNIQUE, *id_categoria*, total_adquisiciones, monto_determinante, cuota, fecha_vencimiento, nivel_alerta, fecha_calculo | |
| `aviso` | **id_aviso**, *id_periodo*, tipo, fecha_programada, enviado | |
| `reporte_mensual` | **id_reporte**, *id_periodo*, ruta_pdf, sha256, fecha_generacion | |

- Fechas como `TEXT` ISO-8601. Montos: el informe los define como `NUMERIC` leídos como `BigDecimal`; en la implementación se recomienda guardarlos como **`INTEGER` en céntimos** con un `TypeConverter` a `BigDecimal`, porque SQLite trata `NUMERIC` con decimales como `REAL` y puede redondear.
- Columnas agregadas respecto del DER del informe, necesarias para implementar: `contribuyente.titular` y `pin_sal`, `factura_compra.fecha_registro`, `modelo_local.ruta` y `determinacion.fecha_calculo`.
- `@Transaction` para guardar factura + imagen + campos + recálculo (RNF-13).
- **Migraciones Room** explícitas desde la versión 1 (`exportSchema = true`, esquemas en `app/schemas/`).

**Apertura cifrada:**

```java
byte[] clave = GestorClaves.claveBaseDatos(context);          // 32 bytes, envuelta con una clave AES del Keystore
SupportOpenHelperFactory fabrica = new SupportOpenHelperFactory(clave);
BaseDatosFacturas db = Room.databaseBuilder(context, BaseDatosFacturas.class, "facturas.db")
        .openHelperFactory(fabrica)
        .build();
```

**Imágenes:** `AlmacenImagenes` cifra cada JPEG con AES-256-GCM (clave del Keystore, IV aleatorio de 12 bytes antepuesto) en `filesDir/imagenes/<uuid>.jpg.enc`. Nunca en la galería ni en almacenamiento compartido; solo se descifran en memoria para mostrarlas, para el PDF o para la exportación que pida el usuario (sección 9).

---

## 8. Interfaz: pantallas, navegación y diseño

### 8.1 Pantallas

Las 20 pantallas (P01–P20) son móviles; además está el reporte PDF (8.5).

| ID | Pantalla | Destino de navegación | Datos | Acciones | Validaciones / estados | RF |
|---|---|---|---|---|---|---|
| P01 | Configuración inicial | `configuracion` | RUC, nombre del negocio, titular | Continuar | RUC módulo 11; obligatorios | RF-19, RF-06 |
| P02 | Acceso | `acceso` | PIN 4 dígitos | Ingresar, huella, restablecer | 3 fallos → espera 30 s | RF-19 |
| P03 | Preparar lectura con IA | `preparacionModelo` | RAM, espacio, red, versión y tamaño del modelo, progreso | Descargar (desde el repositorio público), pausar, registrar a mano | Solo Wi-Fi; SHA-256 al final | RF-20 |
| P04 | Inicio (principal) | `inicio` | Compras, ventas, categoría, cuota, barra con marca 80 %, vencimiento, últimas 3 facturas | Escanear, editar ventas, ver todas | Barra ámbar ≥ 80 % | RF-08, RF-10–13 |
| P05 | Guía de captura | `guiaCaptura` | Consejos | Abrir cámara, galería, «no volver a mostrar» | — | RF-01, RF-03 |
| P06 | Cámara | `CapturaActivity` | Vista previa, nitidez, luz | Disparar, galería, flash | Disparador activo con nitidez y luz «buenas» | RF-01–03 |
| P07 | Lectura | `lectura` | Progreso por campo | Cancelar | Timeout 20 s | RF-04 |
| P08 | Verificación | `verificacion` | Imagen + 7 campos | Editar campo, Guardar, otra foto | Verde ✓ validado · ámbar ⚠ confianza < 0,80 · rojo ✗ inválido | RF-05, RF-06 |
| P09 | Duplicado | diálogo | Factura existente | Ver registrada, descartar | — | RF-07 |
| P10 | Guardada | `guardada` | Importe agregado, acumulado, barra | Escanear otra, Inicio | Aviso si ≥ 80 % | RF-08, RF-12 |
| P11 | Facturas del mes | `facturas` | Lista por semana, total | Filtrar (Todas, IA, Manuales, Anuladas), buscar | Vacío: «Aún no registra facturas este mes» | RF-08 |
| P12 | Detalle y anulación | `detalleFactura` | Datos, imagen | Anular con motivo (bottom sheet), compartir | Solo período abierto | RF-18 |
| P13 | Ventas del mes | `ventas` | Total de ventas | Guardar | ≥ 0 | RF-09 |
| P14 | Categoría y cuota | `categoria` | Determinante, tabla vigente, avisos | Interruptores de avisos | — | RF-10–12, RF-22 |
| P15 | Vencimiento | `vencimiento` | Fecha límite, calendario, dígito | Recordatorios (5 y 1 días antes) | — | RF-13, RF-14 |
| P16 | Reporte | `reporte` | Vista previa PDF, resumen | Compartir, guardar | Advierte si faltan ventas | RF-15, RF-16 |
| P17 | Historial y cierre | `historial` | Meses, acumulado anual vs. tope | Cerrar el mes, abrir mes, exportar datos (→ P20) | No cierra sin ventas; al cerrar sugiere exportar | RF-17, RF-12, RF-21 |
| P18 | Ajustes | `ajustes` | Negocio, seguridad, modelo (versión instalada), parámetros vigentes | Cambiar PIN, huella, ver modelo, **Exportar datos** (→ P20) | — | RF-19, RF-20, RF-22 |
| P19 | Registro manual | `registroManual` | RUC, proveedor (autocompleta si el emisor existe), serie-número, fecha, importe, foto opcional | Guardar | Mismas validaciones que P08; `origen = MANUAL` | RF-01, RF-06 |
| P20 | Exportar datos | `exportarDatos` | Selector de período (mes / año), casillas «Incluir imágenes» e «Incluir reporte PDF», tamaño estimado | «Guardar en el teléfono» (SAF), «Compartir» | Aviso «El archivo no va cifrado»; período con datos | RF-21 |

**Barra inferior** (en P04, P11, P14, P17, P18): Inicio · Facturas · **[Escanear]** (botón central) · Mes · Ajustes. **Flujo principal ≤ 3 toques:** Escanear → disparar → Guardar (RNF-09).

### 8.2 Grafo de navegación (resumen)

```
configuracion → acceso → (sin modelo) preparacionModelo → inicio
acceso → inicio
inicio → guiaCaptura → CapturaActivity → lectura → verificacion → guardada → inicio
verificacion → DuplicadoDialog (si existe) · CapturaActivity (otra foto)
CapturaActivity → registroManual (sin IA / memoria insuficiente) → guardada
inicio → facturas → detalleFactura
inicio → ventas → categoria → vencimiento → reporte
inicio → historial → exportarDatos
inicio → ajustes → exportarDatos · preparacionModelo
```

### 8.3 Tokens de diseño (`res/values/colors.xml`, `dimens.xml`)

| Token | Valor | Uso |
|---|---|---|
| `color_primario` | `#1B4D45` | Barras, botón primario, títulos |
| `color_acento` | `#8BC34A` | Escanear, progreso, éxito |
| `color_acento_oscuro` | `#5F8F2A` | Texto sobre fondos claros de acento |
| `color_fondo` | `#F5F7F6` | Fondo |
| `color_texto` / `color_texto2` | `#1D2B28` / `#5E6E6A` | Texto principal / secundario |
| `color_alerta` / `color_alerta_claro` | `#F0A202` / `#FFF4DB` | Revisar, 80 % |
| `color_error` / `color_error_claro` | `#C62828` / `#FDECEA` | Inválido, anular |
| `color_ok` / `color_ok_claro` | `#2E7D32` / `#E8F5E9` | Validado |
| `color_borde` | `#DFE5E3` | Bordes |
| `margen` | 16 dp | Margen lateral |
| `boton_alto` | 48 dp (mínimo táctil) | Botones y objetivos |
| `campo_alto` | 58 dp | Campos |
| `radio_tarjeta` / `radio_boton` | 14 dp / 24 dp | Forma |
| Texto | título 18–22 sp; monto 28–36 sp; dato 16 sp; ayuda 12 sp | Roboto |

**Reglas UX:** lenguaje cotidiano («compras», «ventas», «le corresponde»); nunca códigos de error; el color siempre acompañado de ícono y texto; contraste ≥ 4,5:1; acciones principales en la mitad inferior; formato de moneda `S/ 4 120,50` (espacio como separador de miles, coma decimal) y fechas `22/09/2026` o «jueves 15 de octubre».

### 8.4 Textos clave (strings.xml)

| Clave | Texto |
|---|---|
| `guia_titulo` | ¡Hora de la foto! |
| `camara_acercar` | Acérquese un poco más hasta que se lea el total |
| `lectura_sin_red` | IA en el teléfono · sin conexión |
| `verif_baja_confianza` | Confianza baja (%1$d %%): compárelo con el total impreso en la factura. |
| `duplicado_titulo` | Esta factura ya está registrada |
| `aviso_80` | Superó el 80 %% del límite de la categoría %1$d |
| `ventas_explicacion` | La categoría del NRUS se decide con el mayor monto del mes: compras o ventas. |
| `manual_sin_ia` | La lectura automática no está disponible en este equipo. Complete los datos; se validan igual que con IA. |
| `reporte_privacidad` | El PDF se genera en su teléfono y no se envía a ningún lado |
| `modelo_descarga` | Se descargará una sola vez (%1$s) por Wi-Fi. No se envía ningún dato suyo. |
| `exportar_titulo` | Exportar datos |
| `exportar_aviso_cifrado` | El archivo no va cifrado. Guárdelo en un lugar seguro y compártalo solo con personas de confianza. |
| `exportar_listo` | Listo: se exportaron %1$d facturas de %2$s |
| `cierre_sugerir_exportar` | Mes cerrado. Le recomendamos exportar sus datos por si pierde el teléfono. |

### 8.5 Reporte PDF (RF-15)

A4 vertical. Página 1: encabezado (título, período, fecha de generación), contribuyente (nombre, RUC, régimen), resumen (compras con n.º de facturas, ventas, monto determinante, categoría y cuota, vencimiento con dígito), tabla de facturas (n.º, fecha, proveedor, RUC, serie-número, importe), total. Páginas siguientes: una miniatura por factura con su serie-número. Pie: «Documento de apoyo para la declaración; no reemplaza la declaración ante la SUNAT» y SHA-256 del archivo. Nombre: `facturas-s20_AAAA-MM.pdf`. Compartir con `FileProvider` + `Intent.ACTION_SEND`.

---

## 9. Exportación de datos

Reemplaza al respaldo opcional del primer avance. La exportación es la única forma en que los datos salen del teléfono, y siempre por decisión del usuario (RF-21, RNF-14).

### 9.1 Pantalla P20 y flujo

Se llega a **P20 Exportar datos** desde Ajustes (P18) y desde Historial (P17); al cerrar un mes, P17 sugiere exportarlo.

1. El usuario elige el período: **un mes** o **el año** completo.
2. Marca «Incluir imágenes» (JPEG descifrados de las facturas) e «Incluir reporte PDF» (del mes o de cada mes del año que lo tenga). La pantalla muestra el **tamaño estimado** del ZIP.
3. Lee el aviso **«El archivo no va cifrado»**: una vez fuera de la app, el archivo queda bajo responsabilidad del usuario.
4. Pulsa **«Guardar en el teléfono»** → selector de Android (Storage Access Framework, `Intent.ACTION_CREATE_DOCUMENT`, tipo `application/zip`, nombre sugerido) y la app escribe el ZIP en el `Uri` elegido (Descargas, Drive, memoria USB…), **o** pulsa **«Compartir»** → la app genera el ZIP en `cacheDir/exportaciones/`, lo expone con `FileProvider` y abre `Intent.ACTION_SEND` (correo, WhatsApp, Drive) con `FLAG_GRANT_READ_URI_PERMISSION`. El archivo temporal se borra al volver o, a más tardar, al siguiente inicio.
5. Al terminar: «Listo: se exportaron N facturas de <período>».

### 9.2 Contenido del ZIP

Nombre: `facturas-s20_AAAA-MM.zip` (mes) o `facturas-s20_AAAA.zip` (año).

```
facturas-s20_2026-09.zip
├── facturas.csv
├── periodos.csv
├── determinaciones.csv
├── imagenes/                  (opcional) 2026-09_20601234565_F001-004821.jpg …
└── reportes/                  (opcional) facturas-s20_2026-09.pdf
```

**`facturas.csv`** — una fila por factura del período (VIGENTE y ANULADA; se filtra por `estado` en Excel). Sale de `factura_compra` + `emisor` + `periodo`:

| Columna | Origen | Ejemplo |
|---|---|---|
| `anio`, `mes` | `periodo` | `2026`, `9` |
| `fecha_emision` | `factura_compra.fecha_emision` (ISO) | `2026-09-22` |
| `ruc_emisor` | `emisor.ruc` (texto) | `20601234565` |
| `razon_social` | `emisor.razon_social` | `DISTRIBUIDORA ANDINA S.A.C.` |
| `serie`, `numero` | `factura_compra` | `F001`, `004821` |
| `moneda` | `factura_compra.moneda` | `PEN` |
| `importe_total` | `factura_compra.importe_total` | `1450.00` |
| `origen` | `IA` / `MANUAL` | `IA` |
| `estado` | `VIGENTE` / `ANULADA` | `VIGENTE` |
| `motivo_anulacion` | vacío si VIGENTE | |
| `fecha_registro` | `factura_compra.fecha_registro` | `2026-09-22T10:41:05` |
| `archivo_imagen` | ruta dentro del ZIP o vacío | `imagenes/2026-09_20601234565_F001-004821.jpg` |

**`periodos.csv`** — una fila por mes exportado (`periodo` + suma calculada): `anio`, `mes`, `estado` (ABIERTO/CERRADO), `total_ventas`, `total_adquisiciones` (suma de VIGENTE), `facturas_vigentes`, `facturas_anuladas`.

**`determinaciones.csv`** — una fila por período con determinación (`determinacion` + `categoria_nrus` + `parametro_version`): `anio`, `mes`, `total_adquisiciones`, `monto_determinante`, `categoria` (código), `cuota`, `fecha_vencimiento`, `nivel_alerta`, `version_parametros`, `fecha_calculo`.

No se exportan `contribuyente.pin_hash`/`pin_sal`, claves, ni `campo_extraido` (confianzas del modelo): el archivo es para análisis tributario, no para restaurar la app.

### 9.3 Formato de los CSV

- **Codificación UTF-8 con BOM** (`EF BB BF`) para que Excel muestre bien tildes y «ñ»; fin de línea `\r\n`.
- **Separador `;`**. Campos entre comillas dobles solo si contienen `;`, comillas o saltos de línea; las comillas internas se duplican (RFC 4180). Primera fila: nombres de columna en minúsculas y sin tildes.
- **Montos con punto decimal, sin separador de miles y siempre con dos decimales** (`1450.00`, `BigDecimal.setScale(2).toPlainString()`). Motivo: la configuración regional de Windows para Perú (`es-PE`) usa el **punto** como símbolo decimal, así que Excel reconoce `1450.00` como número y lo suma sin conversión; además es el mismo formato del modelo (6.2) y de `BigDecimal`, y no choca con ningún separador. Si una PC está configurada con coma decimal (p. ej. `es-ES`), se usa *Datos → Desde texto/CSV* y se elige la configuración regional «Español (Perú)» o «Inglés»; LibreOffice pregunta el separador y la configuración regional al abrir.
- Fechas en ISO (`AAAA-MM-DD`), que Excel y LibreOffice reconocen en cualquier configuración regional.
- El RUC y el número se escriben como texto; si Excel los muestra en notación científica, se importan con *Datos → Desde texto/CSV* y tipo «Texto». Para que el doble clic abra las columnas separadas en una PC cuyo separador de listas sea `,`, también se usa *Datos → Desde texto/CSV* (detecta `;`).
- Las imágenes se descifran con `AlmacenImagenes` en flujo (sin archivos temporales en claro) y se escriben tal cual como JPEG dentro del ZIP.

### 9.4 Clases

| Clase | Paquete | Responsabilidad |
|---|---|---|
| `ExportarDatos` | `dominio.casosuso` | Valida la `SolicitudExportacion`, reúne facturas, períodos y determinaciones de los repositorios y llama al puerto `ExportadorArchivos`. No conoce Android. |
| `ExportadorArchivos` | `dominio.puertos` | `ResumenExportacion exportar(DatosExportacion datos, OutputStream destino)` |
| `ExportadorDatos` | `exportacion` | Implementa el puerto: arma el ZIP (`ZipOutputStream`), escribe los CSV con `EscritorCsv` y agrega imágenes y PDF opcionales. |
| `EscritorCsv` | `exportacion` | BOM, separador `;`, escape RFC 4180, montos con punto decimal y fechas ISO. |
| `ExportarDatosFragment` / `ExportarDatosViewModel` | `ui.exportacion` | P20: opciones, tamaño estimado, aviso, lanzadores SAF (`registerForActivityResult(new ActivityResultContracts.CreateDocument("application/zip"))`) y compartir con `FileProvider`. |

```java
public final class EscritorCsv implements Closeable {
    private static final char SEPARADOR = ';';
    private final Writer salida;

    public EscritorCsv(OutputStream destino) throws IOException {
        destino.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});   // BOM para Excel
        this.salida = new OutputStreamWriter(destino, StandardCharsets.UTF_8);
    }

    public void fila(Object... valores) throws IOException {
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) salida.write(SEPARADOR);
            salida.write(formatear(valores[i]));
        }
        salida.write("\r\n");
    }

    private static String formatear(Object valor) {
        if (valor == null) return "";
        String texto = valor instanceof BigDecimal
                ? ((BigDecimal) valor).setScale(2, RoundingMode.HALF_UP).toPlainString()   // 1450.00
                : valor.toString();                                                     // LocalDate → ISO
        boolean requiereComillas = texto.indexOf(SEPARADOR) >= 0 || texto.indexOf('"') >= 0
                || texto.indexOf('\n') >= 0 || texto.indexOf('\r') >= 0;
        return requiereComillas ? '"' + texto.replace("\"", "\"\"") + '"' : texto;
    }

    @Override public void flush() throws IOException { salida.flush(); }
    @Override public void close() throws IOException { salida.flush(); }   // el ZIP cierra el flujo
}
```

```java
// En ExportadorDatos: el ZipOutputStream escribe directo en el Uri elegido con SAF
try (OutputStream destino = contentResolver.openOutputStream(uri);
     ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(destino))) {
    zip.putNextEntry(new ZipEntry("facturas.csv"));
    EscritorCsv csv = new EscritorCsv(zip);
    csv.fila("anio", "mes", "fecha_emision", "ruc_emisor", "razon_social", "serie", "numero",
             "moneda", "importe_total", "origen", "estado", "motivo_anulacion", "fecha_registro", "archivo_imagen");
    for (FilaFactura f : datos.facturas()) {
        csv.fila(f.anio(), f.mes(), f.fechaEmision(), f.rucEmisor(), f.razonSocial(), f.serie(), f.numero(),
                 f.moneda(), f.importeTotal(), f.origen(), f.estado(), f.motivoAnulacion(), f.fechaRegistro(),
                 f.archivoImagen());
    }
    csv.flush();
    zip.closeEntry();
    // … periodos.csv, determinaciones.csv, imagenes/ y reportes/ (opcionales)
}
```

### 9.5 Criterio de aceptación (RF-21)

- El ZIP generado para un mes y para el año **se abre en una PC** (Windows con Excel y con LibreOffice) sin errores ni caracteres raros.
- La **suma de `importe_total` de las filas VIGENTE de `facturas.csv` es igual al acumulado del período** (`periodos.csv.total_adquisiciones` y lo mostrado en P04/P17), céntimo a céntimo.
- Con «Incluir imágenes», cada `archivo_imagen` existe dentro del ZIP y abre como JPEG; sin la casilla, la columna va vacía y no hay carpeta `imagenes/`.
- Antes de generar se muestra el aviso «El archivo no va cifrado».
- La exportación funciona en modo avión («Guardar en el teléfono»).

---

## 10. Seguridad

No hay servidor, cuentas ni sincronización: la superficie de ataque es el teléfono y el archivo exportado. La única conexión de red es la descarga del modelo, que no envía datos del usuario. Lista mínima (alineada con OWASP MASVS):

- [ ] SQLCipher con clave aleatoria de 32 bytes envuelta por una clave AES del Android Keystore.
- [ ] Imágenes cifradas con AES-GCM en almacenamiento interno; `android:allowBackup="false"` y reglas de extracción de datos (`dataExtractionRules`) que excluyan base e imágenes de las copias automáticas de Android.
- [ ] PIN con hash (PBKDF2 + sal) y bloqueo temporal; huella con `BiometricPrompt` (`BIOMETRIC_STRONG`).
- [ ] **Permisos mínimos de Android:** `CAMERA`, `INTERNET` y `ACCESS_NETWORK_STATE` (solo para la descarga del modelo por Wi-Fi), `POST_NOTIFICATIONS` (Android 13+, recordatorios), `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` (descarga larga con WorkManager). **Sin** permisos de almacenamiento (la galería usa el Photo Picker y la exportación usa SAF/`FileProvider`), ubicación ni contactos.
- [ ] `INTERNET` solo lo usa `DescargaModeloWorker`; ningún otro flujo abre conexiones (verificar en modo avión y con un proxy que no salga ningún dato del usuario).
- [ ] TLS 1.2+, sin tráfico en claro (`usesCleartextTraffic="false"`, `network_security_config` que solo permita el dominio del repositorio del modelo).
- [ ] Verificación de tamaño y SHA-256 del modelo (contra `assets/modelo.json`) antes de usarlo.
- [ ] Exportación: aviso de archivo no cifrado; sin PIN, claves ni datos del modelo en el ZIP; temporales de «Compartir» en `cacheDir` y borrados después; `FileProvider` con rutas limitadas a `cacheDir/exportaciones/` y al PDF.
- [ ] No registrar en `Logcat` RUC completos, montos ni respuestas del modelo en compilaciones release.

---

## 11. Requerimientos y criterios de aceptación

### 11.1 Funcionales

| RF | Requerimiento | Prioridad | Criterio de aceptación verificable |
|---|---|---|---|
| RF-01 | Capturar con guía de encuadre | Alta | P06 muestra marco y permite capturar |
| RF-02 | Evaluar nitidez e iluminación | Alta | Foto con varianza < umbral o luz fuera de rango → «Tome otra foto» |
| RF-03 | Importar desde galería | Media | Imagen elegida pasa por el mismo flujo |
| RF-04 | Extraer 7 campos en el teléfono | Alta | En modo avión, P07 devuelve los campos |
| RF-05 | Mostrar campos y resaltar baja confianza | Alta | Campo con confianza < 0,80 aparece en ámbar |
| RF-06 | Validar RUC y fecha | Alta | RUC inválido bloquea Guardar; fecha fuera del período pide confirmación |
| RF-07 | Detectar duplicados | Media | Misma clave → P09 y no se inserta fila |
| RF-08 | Acumular total mensual | Alta | Total = suma de VIGENTE tras guardar o anular |
| RF-09 | Registrar ventas del mes | Alta | Un solo campo por período |
| RF-10 | Determinar categoría | Alta | Casos 5.4 pasan |
| RF-11 | Calcular cuota | Alta | Cuota según categoría vigente |
| RF-12 | Avisos 80 %, 100 % y topes anuales | Media | Notificación y banner al cruzar umbrales |
| RF-13 | Calcular vencimiento | Alta | Fecha del cronograma por último dígito |
| RF-14 | Notificar vencimiento | Media | Notificación 5 y 1 días antes (WorkManager) |
| RF-15 | Generar reporte PDF | Media | PDF con resumen, detalle e imágenes |
| RF-16 | Compartir reporte | Baja | Intent de compartir con el PDF |
| RF-17 | Cerrar mes e histórico | Media | Mes cerrado aparece en P17 y no se edita |
| RF-18 | Anular factura | Media | Estado ANULADA con motivo y recálculo |
| RF-19 | Configurar contribuyente y acceso | Alta | Sin configuración no se accede; PIN/huella requeridos |
| RF-20 | Descargar una sola vez, verificar (SHA-256) e instalar el modelo Gemma 4 ajustado desde el repositorio público indicado por la app, y actualizarlo cuando una nueva versión de la app traiga un modelo nuevo (CU-09) | Alta | Descarga verificada; versión anterior conservada |
| RF-21 | Exportar los registros de un mes o del año a archivos abiertos (CSV, imágenes y reporte PDF en un ZIP) que el usuario guarda o comparte para analizarlos en una PC (CU-11) | Media | El ZIP se abre en una PC y `facturas.csv` suma lo mismo que el acumulado del período (9.5) |
| RF-22 | Aplicar los parámetros del NRUS (límites, cuotas, cronograma) incluidos en la versión instalada de la app (CU-05) | Media | Una versión nueva aplica a períodos nuevos, no a los cerrados |

### 11.2 No funcionales (métricas)

| RNF | Métrica |
|---|---|
| RNF-01 / 02 | Exactitud ≥ 90 % por campo en el conjunto de prueba; mejor que el modelo base en todos los campos clave |
| RNF-03 | Diferencia del total mensual < 2 % frente al cálculo de control |
| RNF-04 | Lectura ≤ 20 s en el equipo de referencia (gama media, 6 GB) |
| RNF-05 | ≤ 2 GB de RAM durante la extracción |
| RNF-06 | APK ≤ 60 MB (modelo aparte, ≈ 2,6 GB) |
| RNF-07 | Android 8.0 (API 26)+ |
| RNF-08 | Reporte en PDF (objetivo PDF/A) |
| RNF-09 | ≤ 3 toques para registrar desde Inicio |
| RNF-10 | Objetivos ≥ 48 dp; texto de datos ≥ 16 sp |
| RNF-11 | Prueba con 3 titulares sin formación contable (≥ 5/6 tareas sin ayuda; SUS ≥ 70) |
| RNF-12 | 100 % de funciones principales sin red |
| RNF-13 | Escrituras transaccionales; ningún registro perdido ante cierre forzado |
| RNF-14 | (Seguridad) Las imágenes y los datos tributarios no salen del teléfono salvo que el usuario los exporte o comparta. Métrica: cero envíos de imágenes para inferencia |
| RNF-15 | SQLCipher + PIN o biometría |
| RNF-16 | (Seguridad) Ningún dato del usuario se envía por red; la única conexión es la descarga del modelo. Métrica: cero envíos de datos; descarga por TLS 1.2+ verificada con SHA-256 |
| RNF-17 | (Mantenibilidad) Parámetros del NRUS fuera del código, en un archivo versionado (`assets/parametros_nrus.json`). Métrica: cambiar los parámetros no modifica código Java |
| RNF-18 | Cobertura de pruebas del motor de reglas ≥ 80 % |
| RNF-19 | Cambiar el modelo sin reinstalar la app |

---

## 12. Pruebas

| Nivel | Qué | Herramienta | Mínimo |
|---|---|---|---|
| Unitarias `:dominio` | `MotorReglasNRUS` (casos 5.4), `ValidadorRuc` (5.3), `DetectorDuplicados`, `PeriodoMensual`, `ParserRespuesta` (JSON válido, envuelto, incompleto, montos con coma), `ExportarDatos` (mes, año, período sin datos) | JUnit | Cobertura ≥ 80 % en `reglas` |
| Unitarias exportación | `EscritorCsv`: BOM, separador `;`, escape de `;`, comillas y saltos de línea, montos `1450.00`, fechas ISO, tildes y «ñ» | JUnit (JVM) | Todos los casos de 9.3 |
| Instrumentadas `:app` | DAO y migraciones Room con SQLCipher; `AlmacenImagenes`; guardado transaccional; carga de `assets/parametros_nrus.json` y de `assets/modelo.json`; `ExportadorDatos` (ZIP con y sin imágenes/PDF, suma de `facturas.csv` = acumulado del período) | AndroidX Test | Todos los DAO y el criterio 9.5 |
| UI | Flujo P04→P10 con un `ExtractorFacturas` falso; P08 estados; P09; P19; P20 (aviso de no cifrado, SAF con `Intents` de Espresso, compartir) | Espresso | Flujo principal y excepciones |
| Inferencia | Factura de control empaquetada: 7 campos en ≤ 20 s en el teléfono de referencia | Prueba manual + registro de tiempos | Por versión del modelo |
| Red y privacidad | Modo avión: todas las funciones principales y la exportación funcionan; con proxy, la única petición es la descarga del modelo | Prueba manual | Por versión de la app |
| Aceptación | Mes simulado con las facturas reales del conjunto de prueba; exportar y abrir el ZIP en Excel y LibreOffice | Planilla de control | RNF-01, 03, 04; RF-21 |

Usa un **`ExtractorFacturas` falso** (devuelve el JSON de la sección 6.2) para desarrollar y probar toda la app sin el modelo real.

---

## 13. Roadmap: plan de construcción por iteraciones

Plan del proyecto para el ciclo 2026-03 (18 semanas). Estado al **corte de la semana 8** (APF2, 27–29/09/2026), más el paso 0, que se completó el 27–28/09/2026. Las tablas de entregas, responsables, Gantt y mediciones vienen del informe y la sustentación del APF2 (`documentos_teoricos/entregable2/entregable2_archivos/scripts/`). Si el plan cambia y el informe del siguiente avance debe reflejarlo, actualiza también esos scripts.

### 13.1 Entregas del curso

| Semana | Entrega |
|---|---|
| 4 | APF1 — entregado |
| 8 | APF2 — entrega hasta el 29/09/2026 18:00 |
| 12 | APF3 |
| 18 | Informe final y sustentación |

> Nota del curso: para el avance siguiente el docente pidió mostrar **alrededor de 20 % del back-end y 60–80 % del front-end** (interfaces construidas a partir del prototipo y la base de datos). Sin servidor, el "back-end" son las capas Java del teléfono: dominio, motor NRUS, Room/SQLite y exportación. Por eso la iteración I3 prioriza las pantallas navegables con datos locales.

### 13.2 Iteraciones

Son iteraciones de dos semanas, alineadas con las entregas del curso.

| Iteración | Semanas | Objetivo | Entregable | Estado |
|---|---|---|---|---|
| I1 | 1–4 | Análisis del contexto, alternativas y SRS | APF1 | ✅ completo |
| I2 | 5–8 | Diseño de procesos, datos, clases, prototipo y documentación técnica | APF2 | ✅ completo |
| Paso 0 | 8 | Configuración del equipo y proyecto Android base | App "Hello World!" en el teléfono | ✅ completo (ver 13.3) |
| I3 | 9–10 | Front-end navegable (P01–P20) sobre la base de datos cifrada, y dominio con motor de reglas probado | Incremento 1 | ⏳ en curso (pasos 1 y 2 hechos, ver 13.4) |
| I4 | 11–12 | Inferencia local con el modelo convertido, medición en el teléfono y prueba de usabilidad | APF3 | pendiente |
| I5 | 13–14 | Reporte PDF, exportación de datos e historial | Incremento 3 | pendiente |
| I6 | 15–18 | Pruebas, validación en mes simulado y sustentación | Informe final | pendiente |

### 13.3 Paso 0 · Configuración del equipo ✅ (hecho)

La primera parte del trabajo ya está hecha y documentada en **`002_27_09_26_configuracion_equipo.md`**. Su Parte 1 cuenta lo que se hizo en la primera computadora; su Parte 2, cómo levantar el proyecto en otra.

Quedó listo:
- IntelliJ IDEA 2026.2 con el plugin de Android, Android SDK (API 36.1) y JDK 17.
- El proyecto Gradle en `android/` con los módulos `:app` (Android, `viewBinding`) y `:dominio` (`java-library`, JUnit 5).
- Versiones en `gradle/libs.versions.toml`, Gradle Wrapper 9.8.0 y AGP 9.1.1 (máximo que soporta IntelliJ 2026.2).
- La estructura de paquetes de la sección 3.3, con un `package-info.java` por paquete. `MainActivity` está en `ui.comun`.
- Íconos provisionales, `.gitignore` y `.gitattributes`.
- La app instalada en el teléfono de pruebas (Android 15), mostrando "Hello World!".

El 2026-09-28 se completaron además los pasos 1 (base de la app) y 2 (dominio) de I3; ver 13.4.

### 13.4 Tareas por iteración

**I3 · semanas 9–10 — Front-end navegable y dominio**

Decidido el 2026-09-28: para el APF3 se muestra ~80 % de front-end (la app) y ~20 % de back-end, que es sobre todo la base de datos. Por eso **la base de datos se adelanta de I4 a I3** y las pantallas usan los repositorios reales desde el principio (no se hacen repositorios en memoria). Se sigue este orden:

1. ✅ **Base de la app (FE)** — hecho el 2026-09-28:
   - Navigation 2.10.2 y Fragment 1.9.1 en `libs.versions.toml`.
   - Tokens de §8.3 en `colors.xml` y `dimens.xml`, tema Material 3 solo claro con estilos de botón, tarjeta, barra y textos (`Texto.FacturasS20.Titulo/Monto/Dato/Ayuda`), y `strings.xml` con los títulos P01–P20 y los textos de §8.4.
   - `ui.comun.Formatos` (moneda `S/ 4 120,50` y `monedaCorta` `S/ 5 000`, fechas `22/09/2026`, `22/09`, «jueves 15 de octubre», «Septiembre 2026»), con `FormatosTest`.
   - `App` (crea el canal de notificaciones `avisos_nrus`) y `ContenedorDependencias` (por ahora solo el ejecutor de fondo).
   - `MainActivity` con NavHost, `nav_graph.xml` con los destinos y acciones de §8.2 y la barra inferior con el botón central Escanear (visible solo en P04, P11, P14, P17 y P18).
   - **Todos los destinos usan `PantallaPendienteFragment`** (muestra código y título); cada pantalla la reemplaza en su `android:name` al construirse. Faltan en el grafo `CapturaActivity` (P06) y `DuplicadoDialog` (P09). El destino inicial es `inicio` hasta que exista la base; entonces será `configuracion` o `acceso`.
   - CameraX no se agregó todavía: va con P06 (paso 4).
2. ✅ **Dominio mínimo (BE)** — hecho el 2026-09-28. `./gradlew :dominio:check` corre 118 pruebas y JaCoCo exige ≥ 80 % de líneas en `reglas` (RNF-18; hoy 99 %). Informe en `dominio/build/reports/jacoco/test/html/`.
   - `modelo`: entidades y enumeraciones de 5.1. `FacturaCompra` y `PeriodoMensual` son clases con invariantes; el resto, `record`s. `PeriodoMensual` es el agregado: `agregarFactura`, `anularFactura(id, motivo)` (la anulación pasa por el mes, que es quien sabe si está abierto), `registrarVentas` y `cerrar`. `totalVentas` nulo = «sin registrar» (el motor lo toma como 0; `cerrar()` lo rechaza). Además: `CampoFactura` (con la clave JSON de 6.2), `ReglaNegocioException` (mensaje en español para mostrar al usuario) y `ConfiguracionNrusException` (faltan categorías o fechas en los parámetros).
   - `reglas`: `ValidadorRuc`, `Montos` (incluye `interpretar("1 450,00")`, que reutilizará `ParserRespuesta`, y céntimos para Room), `ValidadorFecha` (acepta `2026-09-22` y `22/09/2026`; regla de 5.5), `ValidadorCampos` (mensaje por campo, igual en P08 y P19; USD se rechaza con «Por ahora solo se registran compras en soles (S/).»), `DetectorDuplicados` y `MotorReglasNRUS`.
   - Interpretación del motor (5.4, paso 5): `LIMITE_100` si la categoría ya no es la 1 o el monto está justo en el límite de la última. El aviso de tope anual es `Determinacion.avisoTopeAnual` (compras o ventas del año ≥ tope × umbral). Fuera de régimen: `categoria` y `cuota` nulas.
   - `puertos`: `ExtractorFacturas` (+ `ExtraccionException`), `FacturaRepositorio`, `PeriodoRepositorio`, `ContribuyenteRepositorio`, `ParametrosRepositorio` y `Reloj` (hora de Lima). **Cada escritura es una sola llamada que el repositorio hace en una transacción:** `FacturaRepositorio.registrar(periodo, factura, imagen, extraccion, determinacion)`, `FacturaRepositorio.anular(factura, determinacion)` y `PeriodoRepositorio.guardar(periodo, determinacion)`. `AvisoProgramador` y `ExportadorArchivos` quedan para I5.
   - `casosuso`: `VerificarFactura` (no guarda; devuelve errores por campo, si hay que confirmar el mes y el duplicado), `RegistrarFactura` (lanza `FacturaDuplicadaException` con la existente), `RegistrarVentas`, `DeterminarCategoria` (`calcular` no guarda y lo usan los demás; `ejecutar` devuelve la determinación congelada si el mes está cerrado, RF-22), `AnularFactura` y `CerrarPeriodo` (el PDF y la sugerencia de exportar, en I5).
   - Las pruebas de casos de uso usan `BaseEnMemoria`, que solo existe en `src/test` (la app no tendrá repositorios en memoria).
3. **Base de datos (BE, el 20 %)** — ojo con dos puntos que dejó el paso 2: (a) el duplicado compara el número **sin ceros de relleno** y **solo contra facturas VIGENTE**, así que el UQ(id_emisor, serie, numero) de 7.1 tal cual chocaría al volver a registrar una factura anulada o al escribir `4821` en vez de `004821`; conviene una columna `numero_normalizado` y quitar o cambiar ese UQ (decidirlo al hacer la entidad). (b) `total_ventas` debe admitir NULL.
   Contenido: Room (`annotationProcessor`) y SQLCipher en `libs.versions.toml`; las 13 entidades de 7.1 con UNIQUE, índices y FK; `TypeConverter`s (montos en céntimos `INTEGER` ↔ `BigDecimal`, fechas ISO-8601); DAO por agregado; `BaseDatosFacturas` versión 1 con `exportSchema = true`; `GestorClaves` (Keystore) y apertura cifrada; `assets/parametros_nrus.json` + `CargadorParametrosNrus`; repositorios que implementan los puertos con guardado `@Transaction`; `AlmacenImagenes` (AES-256-GCM); PIN con hash y sal; `allowBackup="false"` y reglas de extracción (lista de §10); pruebas instrumentadas de DAO y de la transacción.
4. **Pantallas en el orden del flujo (FE, el 80 %)**, cada una con su Fragment, layout y ViewModel sobre los repositorios reales:
   - Acceso: P01 Configuración, P02 Acceso (PIN, huella, 3 fallos → 30 s).
   - Principal: P04 Inicio.
   - Registro: P05 Guía → P06 Cámara (CameraX + `EvaluadorNitidez`) → P07 Lectura → P08 Verificación → P09 Duplicado → P10 Guardada · P19 Registro manual. P07 usa un `ExtractorFacturas` **falso** que devuelve una factura de ejemplo.
   - Control: P11 Facturas del mes (filtros), P12 Detalle + anulación.
   - NRUS: P13 Ventas, P14 Categoría y cuota, P15 Vencimiento.
   - Reportes: P16 Reporte (vista previa, sin PDF real), P17 Historial y cierre.
   - Ajustes: P18 Ajustes, P03 Preparar modelo y P20 Exportar datos (solo interfaz: sin descarga ni ZIP reales).
5. **Cierre del incremento**: prueba Espresso del flujo principal (Escanear → disparar → Guardar, ≤ 3 toques); instalar en el teléfono y tomar capturas para el APF3.

**I4 · semanas 11–12 — Persistencia e IA local (APF3)**
5. ~~Room + SQLCipher~~ (se adelantó a I3, paso 3).
6. `LiteRtLmPuente.kt`, `GestorModeloLocal` (comprobación, `assets/modelo.json`, descarga desde Hugging Face, SHA-256, prueba), P03.
7. Medir tiempo y memoria en el teléfono de referencia con el modelo convertido.
8. Prueba de usabilidad con 3 titulares (protocolo del informe, Anexo S). **Decidido (2026-09-27): semanas 11–12.** El informe del APF2 también la menciona en I3 (§3.7.6 y conclusión cuarta); se corrige en el informe del APF3.

**I5 · semanas 13–14 — Reportes, exportación de datos e historial**
9. Reporte PDF (8.5) y compartir; cierre de mes e historial (P17) con la sugerencia de exportar.
10. WorkManager: recordatorios y avisos.
11. Exportación de datos (sección 9): `ExportarDatos`, `ExportadorDatos`, `EscritorCsv`, P20 con SAF y compartir por `FileProvider`.
12. Pruebas de exportación y verificación del criterio 9.5 en Excel y LibreOffice.

**I6 · semanas 15–18 — Pruebas y validación**
13. Pruebas de integración y de seguridad (lista 10), modo avión, cierre forzado.
14. Validación en mes simulado y métricas finales; informe y sustentación.

### 13.5 Actividades de las semanas 9 a 18 y responsables

| Semanas | Actividad | Responsable | Entregable |
|---|---|---|---|
| 9–10 | Front-end navegable (P01–P20) y dominio con pruebas unitarias del motor NRUS | Coronel Obregón | Incremento 1 |
| 9–12 | Ajuste fino, evaluación, conversión a `.litertlm` y pruebas en el teléfono | Damián Valdivia | Modelo v1 convertido |
| 11–12 | Room con SQLCipher, repositorios e integración del puente LiteRT-LM | Coronel Obregón | APF3 |
| 11–12 | Prueba de usabilidad del prototipo con tres titulares | Gonzales Maco | Informe de usabilidad |
| 13–14 | Reporte PDF, exportación de datos e historial | Coronel Obregón, Mateo Velásquez | Incremento 3 |
| 12–16 | Pruebas de integración y validación en mes simulado | Gonzales Maco | Resultados de validación |
| 17–18 | Informe final y sustentación | Mateo Velásquez | Informe final |

### 13.6 Estado del Gantt al corte de la semana 8

| Paquete | Semanas | Estado |
|---|---|---|
| 1. Gestión del proyecto (acta, WBS, cronograma y riesgos: S1–3; seguimiento: S4–18) | 1–18 | en curso |
| 2. Análisis (contexto, canvas, entrevistas, SRS) | 1–4 | completo |
| 3. Diseño (BPMN, clases, DER, prototipo UX/UI, documentación técnica) | 3–8 | completo |
| 4. Modelo Gemma 4 ajustado — recopilación y etiquetado | 5–9 | en curso |
| 4. Modelo Gemma 4 ajustado — ajuste fino, conversión y pruebas | 9–12 | pendiente |
| 5. Construcción de la app — front-end (pantallas y navegación) | 7–12 | en curso |
| 5. Construcción de la app — back-end (dominio, Room y exportación) | 8–14 | en curso |
| 6. Pruebas y validación | 12–16 | pendiente |
| 7. Documentación y sustentación | 4–18 | en curso |

No hay desviaciones respecto de la línea base (Anexo D del informe). La construcción de la app se adelantó una semana para empezar el front-end mientras se cerraba el diseño.

### 13.7 Mediciones comprometidas (qué hay que demostrar y cuándo)

| RNF | Medición prevista | Cuándo |
|---|---|---|
| RNF-01, RNF-02 | Exactitud ≥ 90 % por campo (conjunto de prueba separado, comparado con el modelo base) | APF3 |
| RNF-03 | Diferencia < 2 % en el mes simulado | Final |
| RNF-04, RNF-05 | ≤ 20 s y ≤ 2 GB en el equipo de referencia (imagen a 896 px, GPU si existe) | APF3 |
| RNF-06, RNF-07 | APK ≤ 60 MB (modelo como descarga separada); pruebas en Android 8 y 13 (minSdk 26) | APF3 |
| RNF-09 a RNF-11 | Prueba de usabilidad con 3 titulares (Anexo S) | Semanas 11–12, I4 (resultados en APF3) |
| RNF-12, RNF-14 | Prueba en modo avión e inspección de tráfico | APF3 |
| RNF-13 | Prueba de cierre forzado durante el guardado | APF3 |
| RNF-15, RNF-16 | Revisión con OWASP MASVS e inspección de tráfico | Final |
| RNF-17 a RNF-19 | Cobertura de pruebas del motor ≥ 80 % | APF3 |

### 13.8 Próximos pasos hacia el APF3 (presentados en la sustentación)

1. Construir el front-end y el dominio a partir de este documento (I3).
2. Hacer el ajuste fino y la conversión a `.litertlm`.
3. Medir la exactitud y el tiempo de lectura en el teléfono de referencia.
4. Hacer la prueba de usabilidad con tres titulares. Los resultados se reportan en el APF3 y **no se inventan cifras**.

---

## 14. Convenciones

- **Idioma:** identificadores del dominio en español sin tildes (`FacturaCompra`, `importeTotal`, `registrarVentas`); sufijos técnicos en inglés cuando son convención (`UseCase`, `ViewModel`, `Dao`, `Entity`, `Fragment`, `Worker`).
- **Estilo:** Google Java Style; clases `final` cuando no se heredan; sin `null` en retornos de colecciones; `Optional` en búsquedas; inmutabilidad en objetos de valor (`Determinacion`, `ResultadoExtraccion`).
- **Dinero:** `BigDecimal` escala 2 (`Montos.de("1450.00")`); comparar con `compareTo`, nunca `equals`. En pantalla `S/ 4 120,50`; en los CSV exportados `4120.50` (9.3).
- **Fechas:** `java.time` (`LocalDate`, `YearMonth`); zona `America/Lima` para «hoy».
- **Archivos versionados en `assets/`:** `modelo.json` (versión, URL, SHA-256 y tamaño del modelo) y `parametros_nrus.json` (sección 5.2). Cambiarlos implica publicar una nueva versión de la app y anotarlo en sus notas de versión.
- **Git:** rama `main` protegida; ramas `feature/<rf>-<descripcion>`; commits en español en imperativo («Agrega validación de RUC»); cada PR indica los RF/RNF que cubre.
- **Definición de terminado:** compila, pruebas verdes, criterio de aceptación de 11.1 verificado, textos en español, sin datos sensibles en logs, este documento actualizado si cambió un contrato.

---

## 15. Artefactos de referencia

En `documentos_teoricos/` del repositorio `Facturas_S20`:

| Artefacto | Contenido |
|---|---|
| `entregable2_documento.docx` | Informe APF2 completo: BPMN, clases, DER, diseño de BD, prototipo, validación, cronograma y presupuesto |
| `entregable2_presentacion.pptx` | Sustentación APF2 |
| `entregable2_archivos/` | Todas las figuras en PNG y sus fuentes SVG editables (`fuentes/`); scripts que las regeneran (`scripts/`) |
| `entregable2_archivos/pantalla_XX_*.png` | Prototipo P01–P20, todas pantallas móviles (referencia visual exacta de cada pantalla) |
| `entregable2_archivos/bpmn_0X_*.png` | Procesos AS-IS, TO-BE, registro, cierre mensual, publicación del modelo |
| `entregable2_archivos/clases_*.png`, `der_local.png` | Diagramas de clases y entidad-relación de la base local |
| `../entregable1/entregable1_documento.docx` | Informe APF1 (contexto, SRS original) |
| `../tunning_gemma4/tunning_model_gemma4_6.ipynb` | Cuaderno de ajuste fino del prototipo previo |

Documentación externa: guía de LiteRT-LM para Android (developers.google.com/edge/litert-lm/android), Room, CameraX, WorkManager, SQLCipher for Android, Storage Access Framework y `FileProvider` (developer.android.com), repositorios de modelos de Hugging Face, OWASP MASVS.
