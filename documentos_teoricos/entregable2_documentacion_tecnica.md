# Facturas S20 — Documentación técnica para desarrolladores

> **Qué es este documento.** Es la referencia para construir el código de Facturas S20: la app Android nativa en Java con Gemma 4 E2B ejecutado en el teléfono, y el servidor opcional de respaldo en Spring Boot. Resume, en forma de contratos que se pueden implementar y probar, el diseño aprobado en el segundo avance (APF2) del curso Integrador I (UTP).
>
> **Cómo usarlo en una conversación nueva.** Este archivo es autocontenido. Si trabajas con un asistente de código, pégalo o referencia su ruta al inicio y pide trabajar por iteraciones (sección 13). Cuando el código contradiga este documento, gana el documento, salvo que se decida cambiarlo: en ese caso, actualízalo en el mismo commit.
>
> **Versión:** 1.0 · 2026-09-27 · corte de la semana 8 del ciclo 2026-03.

---

## Índice

1. [Contexto y producto](#1-contexto-y-producto)
2. [Restricciones no negociables](#2-restricciones-no-negociables)
3. [Arquitectura](#3-arquitectura)
4. [Stack tecnológico](#4-stack-tecnológico)
5. [Dominio y reglas del NRUS](#5-dominio-y-reglas-del-nrus)
6. [Inferencia local con Gemma 4 (LiteRT-LM)](#6-inferencia-local-con-gemma-4-litert-lm)
7. [Datos: base local y base del servidor](#7-datos-base-local-y-base-del-servidor)
8. [Interfaz: pantallas, navegación y diseño](#8-interfaz-pantallas-navegación-y-diseño)
9. [Servidor de respaldo: API REST](#9-servidor-de-respaldo-api-rest)
10. [Seguridad](#10-seguridad)
11. [Requerimientos y criterios de aceptación](#11-requerimientos-y-criterios-de-aceptación)
12. [Pruebas](#12-pruebas)
13. [Plan de construcción por iteraciones](#13-plan-de-construcción-por-iteraciones)
14. [Convenciones](#14-convenciones)
15. [Artefactos de referencia](#15-artefactos-de-referencia)

---

## 1. Contexto y producto

**Problema.** Las bodegas acogidas al Nuevo Régimen Único Simplificado (NRUS) pagan una cuota mensual según su categoría, que se decide por el **mayor** de dos montos del mes: ingresos brutos o adquisiciones. Las facturas de compra se acumulan sin registrar y al cierre se suman a mano: se pierden facturas, el cálculo toma horas y, ante la duda, el titular declara la categoría superior (paga S/ 50 en vez de S/ 20).

**Solución.** Una app Android que:

1. Fotografía la factura de compra al recibirla (guía de encuadre y nitidez).
2. La lee **dentro del teléfono, sin internet**, con Gemma 4 E2B ajustado (LoRA) sobre facturas peruanas y convertido a `.litertlm`.
3. Muestra los campos para verificarlos y guarda la factura con su imagen, cifrada.
4. Mantiene el acumulado del mes, determina categoría y cuota, avisa al 80 % y 100 % del límite y recuerda el vencimiento.
5. Genera el reporte mensual en PDF y, opcionalmente, respalda los datos cifrados en un servidor propio.

**Usuarios.**

| Actor | Tipo | Qué hace |
|---|---|---|
| Bodeguero | Persona | Registra facturas, ingresa ventas del mes, consulta y declara. Sin formación contable; usa el teléfono con una mano en el mostrador. |
| Contador externo | Persona | Recibe el reporte PDF compartido por el bodeguero. No usa la app. |
| Administrador | Persona (equipo) | Publica versiones del modelo y de los parámetros del NRUS desde la consola web. |
| Motor de extracción | Sistema | Gemma 4 E2B en el teléfono vía LiteRT-LM. |
| Motor de reglas NRUS | Sistema | Clase Java que aplica la norma. |

**Fuera del alcance:** presentar o pagar la declaración ante la SUNAT, registrar ventas diarias (solo el total mensual), emitir comprobantes, contabilidad completa, otros regímenes, integración con SUNAT, iOS.

**Estado del repositorio `Facturas_S20`.** La raíz contiene un **prototipo previo** (PWA React + servidor FastAPI en Colab con túnel) usado en la competencia Build with Gemma. **No es la base del producto final.** Lo reutilizable es:

- `tunning_gemma4/tunning_model_gemma4_6.ipynb`: cuaderno de ajuste fino (Unsloth + TRL, 896 px, 80/20). Hoy extrae 4 campos (`ruc`, `fecha_emision`, `numero_factura`, `monto_total`); debe pasar a los **7 campos** de la sección 6.2 y agregar la exportación a `.litertlm`.
- `design_factu/*.pdf` (diseño 8 final) y el prototipo de APF2 (sección 15) como referencia visual.

Se recomienda crear el producto en carpetas nuevas del mismo repositorio (o en un repositorio aparte):

```
facturas-s20-android/     proyecto Gradle de la app (módulos :app y :dominio)
facturas-s20-respaldo/    proyecto Spring Boot del servidor (respaldo-api)
tunning_gemma4/           (existente) entrenamiento y conversión del modelo
```

---

## 2. Restricciones no negociables

| # | Restricción | Motivo |
|---|---|---|
| R1 | **≥ 50 % del código en Java** (meta ≈ 85 %). Kotlin **solo** en `LiteRtLmPuente.kt`. | Exigencia del curso. La API de LiteRT-LM es Kotlin. |
| R2 | La **lectura de facturas funciona sin conexión**. Ningún flujo principal hace llamadas de red. | RNF-12. En el mostrador la señal es intermitente. |
| R3 | **La imagen de la factura nunca sale del teléfono** para ser leída. | RNF-14. Privacidad tributaria. |
| R4 | Android **8.0 (API 26)** o superior; 6 GB de RAM recomendados para la lectura con IA; si no alcanza, **registro manual**. | RNF-07. |
| R5 | Montos con `BigDecimal` (escala 2, `RoundingMode.HALF_UP`). Nunca `double` para dinero. | Exactitud del total (RNF-03). |
| R6 | Parámetros del NRUS (límites, cuotas, cronograma) **fuera del código**, versionados. | RNF-17. Cambian por norma. |
| R7 | El paquete `dominio` **no importa** `android.*` ni LiteRT-LM. | RNF-18: se prueba con JUnit en la JVM. |
| R8 | Toda la interfaz, mensajes y comentarios **en español** (Perú). | Usuario final. |
| R9 | Base local cifrada (SQLCipher) y acceso con PIN o huella. | RNF-15. |

---

## 3. Arquitectura

### 3.1 Capas y regla de dependencias

```
┌──────────────────────────── Teléfono Android ────────────────────────────┐
│  ui (Presentación)      Activities/Fragments, ViewModels, CameraX         │
│        │ usa                                                              │
│        ▼                                                                  │
│  dominio (Java puro)    casos de uso · reglas NRUS · validadores · puertos│
│        ▲ implementan                          ▲ implementan               │
│  inferencia             ExtractorGemmaLocal ──┘   datos   Room + SQLCipher │
│        └─ litert/LiteRtLmPuente.kt (único Kotlin)                        │
└───────────────────────────────────────────────────────────────────────────┘
         │ HTTPS (opcional, nunca imágenes)            ▲ descarga única del modelo
         ▼                                             │
  respaldo-api (Spring Boot) ── JDBC/TLS ── Oracle / SQL Server      Almacén de objetos
```

- **Regla:** `ui → dominio ← inferencia, datos`. El dominio define interfaces (**puertos**); inferencia y datos las implementan (**adaptadores**). La inyección se hace en un `ContenedorDependencias` manual (sin framework DI, para mantener Java simple) o con Hilt si el equipo lo prefiere (Hilt es Java-compatible).
- **Un solo hilo de UI.** Inferencia, base de datos y PDF en un `ExecutorService` de la app; resultados a la UI con `LiveData`.

### 3.2 Módulos Gradle

| Módulo | Tipo | Contenido | Depende de |
|---|---|---|---|
| `:dominio` | `java-library` | modelo, casos de uso, reglas, puertos | nada (solo JDK) |
| `:app` | `com.android.application` | ui, inferencia, datos, trabajos, respaldo | `:dominio` |

`respaldo-api` es un proyecto independiente (Gradle o Maven) con Spring Boot.

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
│   └── comun/          MainActivity (NavHost + barra inferior), componentes y formateadores
├── inferencia/         ExtractorGemmaLocal · ParserRespuesta · GestorModeloLocal · Imagenes
│   └── litert/         LiteRtLmPuente.kt
├── datos/
│   ├── entidades/      13 entidades Room
│   ├── dao/            DAO por agregado
│   ├── BaseDatosFacturas.java (RoomDatabase + SQLCipher)
│   ├── cifrado/        GestorClaves (Keystore) · AlmacenImagenes (AES-GCM)
│   └── repositorios/   implementaciones de los puertos del dominio
├── trabajos/           RecordatorioWorker · DescargaModeloWorker · RespaldoWorker
├── respaldo/           ClienteRespaldo (Retrofit) · CifradorRespaldo
└── App.java            Application: inicializa ContenedorDependencias y canales de notificación

(módulo :dominio)  pe.facturass20.dominio
├── modelo/     Contribuyente, PeriodoMensual, FacturaCompra, Emisor, ImagenFactura, ResultadoExtraccion,
│               CampoExtraido, Determinacion, CategoriaNRUS, CronogramaVencimiento, ParametrosNrus,
│               ModeloLocal, Aviso, ReporteMensual + enumeraciones
├── casosuso/   RegistrarFactura, VerificarFactura, RegistrarVentas, DeterminarCategoria,
│               CerrarPeriodo, AnularFactura, GenerarReporte (interfaz de generador), ConsultarHistorial
├── reglas/     MotorReglasNRUS, ValidadorRuc, ValidadorFecha, DetectorDuplicados, Montos
└── puertos/    ExtractorFacturas, FacturaRepositorio, PeriodoRepositorio, ParametrosRepositorio,
                ContribuyenteRepositorio, AvisoProgramador, Reloj
```

### 3.4 Servidor `respaldo-api`

```
pe.facturass20.respaldo
├── api/          AuthController, RespaldoController, ModeloController, ParametrosController, AdminController (+ DTO)
├── servicio/     RespaldoService, VersionModeloService, ParametrosService, AuditoriaService
├── seguridad/    SecurityConfig, JwtService, CuentaAutenticada
├── repositorio/  interfaces Spring Data JPA
├── dominio/      entidades JPA
└── admin/        vistas Thymeleaf de la consola (P20)
resources/db/migration/   V1__esquema.sql, V2__datos_parametros_2026.sql …
```

---

## 4. Stack tecnológico

> Usa la **última versión estable** de cada biblioteca al iniciar y fija las versiones en `libs.versions.toml`. Las versiones de abajo son la base de diseño; verifica compatibilidad antes de fijarlas.

| Capa | Tecnología |
|---|---|
| Lenguaje app | Java 17 (source/target), Kotlin solo para el puente |
| SDK | `minSdk 26`, `targetSdk`/`compileSdk` = el más reciente estable |
| UI | Material Components 3, AndroidX Navigation (Fragments), ViewBinding, ConstraintLayout |
| Cámara | CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`) |
| IA local | `com.google.ai.edge.litertlm:litertlm-android` + modelo Gemma 4 E2B ajustado (`.litertlm`) |
| Persistencia | Room (`room-runtime`, `room-compiler` con `annotationProcessor`) + SQLCipher (`net.zetetic:sqlcipher-android`) |
| Segundo plano | WorkManager |
| Seguridad | `androidx.biometric`, Android Keystore (`KeyGenParameterSpec`, AES/GCM) |
| Red (solo respaldo y modelo) | Retrofit + OkHttp |
| PDF | `android.graphics.pdf.PdfDocument` |
| Pruebas app | JUnit 4/5 en `:dominio`, AndroidX Test + Espresso en `:app` |
| Servidor | Java 21, Spring Boot 3.x (Web, Security, Data JPA, Validation, Thymeleaf), Flyway, JJWT o Spring Security OAuth2 Resource Server |
| BD servidor | Oracle Database 21c XE (principal) o SQL Server 2022 Express |
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

**Enumeraciones:** `EstadoPeriodo {ABIERTO, CERRADO}`, `EstadoFactura {VIGENTE, ANULADA}`, `OrigenRegistro {IA, MANUAL}`, `Moneda {PEN, USD}`, `NivelAlerta {NINGUNA, AVISO_80, LIMITE_100, FUERA_DE_REGIMEN}`, `EstadoModelo {NO_INSTALADO, DESCARGANDO, VERIFICADO, ACTIVO}`, `CampoFactura {RUC, RAZON_SOCIAL, SERIE, NUMERO, FECHA_EMISION, MONEDA, IMPORTE_TOTAL}`, `TipoAviso {TOPE_80, TOPE_100, VENCIMIENTO}`.

### 5.2 Parámetros del NRUS (versión 2026.1)

Se empaquetan en `assets/parametros_nrus_2026.1.json` y luego se actualizan desde `GET /api/v1/parametros/vigente`. **Verificar los montos contra la norma vigente (D. Leg. 937 y modificatorias) antes de publicar.**

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
- **Cierre del mes:** requiere ventas registradas; congela la determinación; genera el reporte si no existe.

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

1. **Comprobar el equipo:** `ActivityManager.MemoryInfo.totalMem ≥ 5,5 GB` (equipos de «6 GB» reportan algo menos) y espacio libre ≥ 3 GB. Si no, P19 (registro manual) y no ofrecer descarga.
2. **Manifiesto:** `GET /api/v1/modelos/manifiesto` → `{version, url, sha256, tamanoBytes}`. Mientras no exista servidor, usar un manifiesto empaquetado en `assets/`.
3. **Descarga** con `DescargaModeloWorker` (solo Wi-Fi, reanudable con `Range`) a `filesDir/modelos/<version>.litertlm.part`.
4. **Verificar SHA-256**; si no coincide, borrar y reintentar (máx. 3).
5. **Probar** con una factura de control empaquetada; si falla, conservar la versión anterior.
6. Renombrar a `.litertlm`, marcar ACTIVO, registrar en `modelo_local`; conservar la anterior hasta que la nueva funcione (RNF-19).

### 6.7 Entrenamiento y conversión (fuera de la app)

Partir de `tunning_gemma4/tunning_model_gemma4_6.ipynb`: etiquetar los 7 campos con el esquema 6.2, LoRA sobre `unsloth/gemma-4-E2B-it` en 4 bits, 896 px, 80/20 + conjunto de prueba separado, evaluar por campo contra el modelo base, **fusionar → cuantizar → convertir a `.litertlm`** y **evaluar de nuevo después de convertir**. Publicar solo si mejora al base y logra ≥ 90 % por campo (RNF-01/02). Referencia de formato: `litert-community/gemma-4-E2B-it-litert-lm` en Hugging Face.

---

## 7. Datos: base local y base del servidor

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
| `parametro_version` | **id_parametro**, version UNIQUE, umbral_aviso, tope_anual, vigente_desde, activo | |
| `categoria_nrus` | **id_categoria**, *id_parametro*, codigo, limite_mensual, cuota | UQ(id_parametro, codigo) |
| `cronograma_venc` | **id_cronograma**, *id_parametro*, anio, mes, ultimo_digito, fecha_limite | UQ(id_parametro, anio, mes, ultimo_digito) |
| `determinacion` | **id_determinacion**, *id_periodo* UNIQUE, *id_categoria*, total_adquisiciones, monto_determinante, cuota, fecha_vencimiento, nivel_alerta, fecha_calculo | |
| `aviso` | **id_aviso**, *id_periodo*, tipo, fecha_programada, enviado | |
| `reporte_mensual` | **id_reporte**, *id_periodo*, ruta_pdf, sha256, fecha_generacion | |

- Fechas como `TEXT` ISO-8601. Montos: el informe los define como `NUMERIC` leídos como `BigDecimal`; en la implementación se recomienda guardarlos como **`INTEGER` en céntimos** con un `TypeConverter` a `BigDecimal`, porque SQLite trata `NUMERIC` con decimales como `REAL` y puede redondear.
- Columnas agregadas respecto del DER del informe, necesarias para implementar: `contribuyente.titular` y `pin_sal`, `factura_compra.fecha_registro`, `modelo_local.ruta`, `determinacion.fecha_calculo` y, en el servidor, `version_modelo.notas`.
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

**Imágenes:** `AlmacenImagenes` cifra cada JPEG con AES-256-GCM (clave del Keystore, IV aleatorio de 12 bytes antepuesto) en `filesDir/imagenes/<uuid>.jpg.enc`. Nunca en la galería ni en almacenamiento compartido.

### 7.2 Base del servidor (Oracle; variante SQL Server) — 9 tablas

`cuenta`, `dispositivo`, `respaldo`, `administrador`, `version_modelo`, `version_parametros`, `categoria_nrus`, `cronograma_venc`, `auditoria`. El servidor **no replica facturas**: `respaldo.contenido_cifrado` es un BLOB cifrado en el teléfono.

```sql
-- V1__esquema.sql (Oracle)
CREATE TABLE cuenta (
  id_cuenta      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  ruc            CHAR(11) NOT NULL UNIQUE,
  correo         VARCHAR2(120) NOT NULL UNIQUE,
  hash_clave     VARCHAR2(100) NOT NULL,
  estado         VARCHAR2(10) DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA','BLOQUEADA')),
  fecha_alta     TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL);
CREATE TABLE dispositivo (
  id_dispositivo NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_cuenta      NUMBER NOT NULL REFERENCES cuenta,
  id_instalacion CHAR(36) NOT NULL UNIQUE,
  modelo_equipo  VARCHAR2(60), version_app VARCHAR2(15), ultima_sync TIMESTAMP);
CREATE TABLE administrador (
  id_admin       NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  usuario        VARCHAR2(40) NOT NULL UNIQUE,
  hash_clave     VARCHAR2(100) NOT NULL,
  rol            VARCHAR2(10) DEFAULT 'ADMIN' NOT NULL,
  activo         NUMBER(1) DEFAULT 1 CHECK (activo IN (0,1)));
CREATE TABLE respaldo (
  id_respaldo       NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_cuenta         NUMBER NOT NULL REFERENCES cuenta,
  id_dispositivo    NUMBER NOT NULL REFERENCES dispositivo,
  anio              NUMBER(4) NOT NULL,
  mes               NUMBER(2) NOT NULL CHECK (mes BETWEEN 1 AND 12),
  contenido_cifrado BLOB NOT NULL,
  sha256            CHAR(64) NOT NULL,
  version_esquema   NUMBER(3) NOT NULL,
  fecha_respaldo    TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT uq_respaldo UNIQUE (id_cuenta, anio, mes));
CREATE INDEX ix_respaldo_cuenta ON respaldo (id_cuenta, anio DESC, mes DESC);
CREATE TABLE version_modelo (
  id_version        NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_admin          NUMBER NOT NULL REFERENCES administrador,
  version           VARCHAR2(15) NOT NULL UNIQUE,
  url_descarga      VARCHAR2(300) NOT NULL,
  sha256            CHAR(64) NOT NULL,
  tamano_mb         NUMBER(6) NOT NULL,
  exactitud_campos  NUMBER(5,2),
  notas             VARCHAR2(500),
  estado            VARCHAR2(12) DEFAULT 'BORRADOR'
                    CHECK (estado IN ('BORRADOR','ACTIVA','ANTERIOR','RETIRADA')),
  fecha_publicacion TIMESTAMP);
CREATE TABLE version_parametros (
  id_parametro   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_admin       NUMBER NOT NULL REFERENCES administrador,
  version        VARCHAR2(15) NOT NULL UNIQUE,
  umbral_aviso   NUMBER(3,2) DEFAULT 0.80 NOT NULL,
  tope_anual     NUMBER(12,2) NOT NULL,
  vigente_desde  DATE NOT NULL,
  publicado      NUMBER(1) DEFAULT 0 CHECK (publicado IN (0,1)));
CREATE TABLE categoria_nrus (
  id_categoria   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_parametro   NUMBER NOT NULL REFERENCES version_parametros,
  codigo         NUMBER(1) NOT NULL,
  limite_mensual NUMBER(10,2) NOT NULL,
  cuota          NUMBER(8,2) NOT NULL,
  CONSTRAINT uq_categoria UNIQUE (id_parametro, codigo));
CREATE TABLE cronograma_venc (
  id_cronograma  NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_parametro   NUMBER NOT NULL REFERENCES version_parametros,
  anio NUMBER(4) NOT NULL, mes NUMBER(2) NOT NULL, ultimo_digito NUMBER(1) NOT NULL,
  fecha_limite   DATE NOT NULL,
  CONSTRAINT uq_cronograma UNIQUE (id_parametro, anio, mes, ultimo_digito));
CREATE TABLE auditoria (
  id_auditoria   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  actor          VARCHAR2(60) NOT NULL,
  accion         VARCHAR2(40) NOT NULL,
  entidad        VARCHAR2(40) NOT NULL,
  fecha          TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  ip_hash        CHAR(64));
```

**SQL Server:** `NUMBER GENERATED ALWAYS AS IDENTITY` → `INT IDENTITY(1,1)`, `NUMBER(p,s)` → `DECIMAL(p,s)`, `VARCHAR2` → `NVARCHAR`, `BLOB` → `VARBINARY(MAX)`, `SYSTIMESTAMP` → `SYSDATETIME()`. Mantener scripts separados por motor (`db/migration/oracle`, `db/migration/sqlserver`).

**Privilegios:** Flyway con usuario propietario; la app con `app_respaldo` (SELECT/INSERT/UPDATE en `respaldo`, `dispositivo`, `cuenta`; SELECT en versiones y parámetros; INSERT en `auditoria`). Credenciales solo por variables de entorno.

### 7.3 Formato del respaldo (cifrado en el teléfono)

- Contenido: JSON del período (período, determinación, facturas, campos; **sin imágenes** en esta etapa) comprimido con GZIP.
- Cifrado: AES-256-GCM con una **clave de respaldo** aleatoria de 32 bytes generada en el teléfono. Esa clave se envuelve con una clave derivada de una **frase de respaldo** que elige el usuario (≥ 8 caracteres; PBKDF2-HMAC-SHA256 con 310 000 iteraciones y sal aleatoria). El PIN de 4 dígitos **no** se usa para esto.
- Se envía `{version_esquema, sal, iv, clave_envuelta, contenido, sha256}`; el servidor verifica el SHA-256 y guarda, sin poder descifrar.

---

## 8. Interfaz: pantallas, navegación y diseño

### 8.1 Pantallas

| ID | Pantalla | Destino de navegación | Datos | Acciones | Validaciones / estados | RF |
|---|---|---|---|---|---|---|
| P01 | Configuración inicial | `configuracion` | RUC, nombre del negocio, titular | Continuar | RUC módulo 11; obligatorios | RF-19, RF-06 |
| P02 | Acceso | `acceso` | PIN 4 dígitos | Ingresar, huella, restablecer | 3 fallos → espera 30 s | RF-19 |
| P03 | Preparar lectura con IA | `preparacionModelo` | RAM, espacio, red, progreso | Pausar, registrar a mano | SHA-256 al final | RF-20 |
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
| P17 | Historial y cierre | `historial` | Meses, acumulado anual vs. tope | Cerrar el mes, abrir mes | No cierra sin ventas | RF-17, RF-12 |
| P18 | Ajustes | `ajustes` | Negocio, seguridad, modelo, parámetros, respaldo | Cambiar PIN, huella, buscar actualización, respaldo | Respaldo **desactivado** por defecto | RF-19–22 |
| P19 | Registro manual | `registroManual` | RUC, proveedor (autocompleta si el emisor existe), serie-número, fecha, importe, foto opcional | Guardar | Mismas validaciones que P08; `origen = MANUAL` | RF-01, RF-06 |
| P20 | Consola admin (web) | `/admin/modelos` | Versiones del modelo, parámetros | Publicar versión, nueva versión de parámetros | Solo rol ADMIN; SHA-256 obligatorio | RF-20, RF-22 |

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
inicio → historial → ajustes
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
| `reporte_privacidad` | El PDF no se envía a ningún servidor |

### 8.5 Reporte PDF (RF-15)

A4 vertical. Página 1: encabezado (título, período, fecha de generación), contribuyente (nombre, RUC, régimen), resumen (compras con n.º de facturas, ventas, monto determinante, categoría y cuota, vencimiento con dígito), tabla de facturas (n.º, fecha, proveedor, RUC, serie-número, importe), total. Páginas siguientes: una miniatura por factura con su serie-número. Pie: «Documento de apoyo para la declaración; no reemplaza la declaración ante la SUNAT» y SHA-256 del archivo. Nombre: `facturas-s20_AAAA-MM.pdf`. Compartir con `FileProvider` + `Intent.ACTION_SEND`.

---

## 9. Servidor de respaldo: API REST

Base `/api/v1`, JSON, HTTPS obligatorio, errores en formato `application/problem+json` (RFC 7807) con `title` en español.

| Método | Ruta | Rol | Cuerpo / respuesta | RF |
|---|---|---|---|---|
| POST | `/auth/registro` | público | `{ruc, correo, clave}` → 201 | RF-21 |
| POST | `/auth/token` | público | `{correo, clave}` → `{token, expiraEn}` (JWT 15 min) | RF-21 |
| PUT | `/respaldos/{anio}/{mes}` | BODEGUERO | `{versionEsquema, sal, iv, claveEnvuelta, contenido (Base64), sha256}` → 204 | RF-21 |
| GET | `/respaldos` | BODEGUERO | `[{anio, mes, sha256, fechaRespaldo}]` | RF-21 |
| GET | `/respaldos/{anio}/{mes}` | BODEGUERO | respaldo completo para restaurar | RF-21 |
| GET | `/modelos/manifiesto` | público | `{version, url, sha256, tamanoBytes}` (URL firmada) | RF-20 |
| GET | `/parametros/vigente` | público | JSON de la sección 5.2 | RF-22 |
| POST | `/admin/modelos` | ADMIN | `{version, urlDescarga, sha256, tamanoMb, exactitudCampos, notas}` | RF-20 |
| PATCH | `/admin/modelos/{version}/estado` | ADMIN | `{estado: ACTIVA|RETIRADA}`; al activar, la anterior pasa a ANTERIOR | RF-20 |
| POST | `/admin/parametros` | ADMIN | versión completa de parámetros | RF-22 |

- El RUC y la cuenta **salen del token**, nunca de la URL.
- Tamaño máximo del respaldo: 5 MB. Límite de peticiones por cuenta.
- Cada operación de escritura registra una fila en `auditoria`.

```java
@RestController
@RequestMapping("/api/v1/respaldos")
public class RespaldoController {
    private final RespaldoService servicio;
    public RespaldoController(RespaldoService servicio) { this.servicio = servicio; }

    @PutMapping("/{anio}/{mes}")
    public ResponseEntity<Void> respaldar(@AuthenticationPrincipal CuentaAutenticada cuenta,
                                          @PathVariable int anio, @PathVariable int mes,
                                          @Valid @RequestBody RespaldoCifradoDto respaldo) {
        servicio.guardar(cuenta.id(), anio, mes, respaldo);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<ResumenRespaldoDto> listar(@AuthenticationPrincipal CuentaAutenticada cuenta) {
        return servicio.listar(cuenta.id());
    }
}
```

**Configuración:** perfiles `dev` (Oracle XE en contenedor), `test`, `prod`. Secretos (`DB_URL`, `DB_USUARIO`, `DB_CLAVE`, `JWT_SECRETO`, credenciales del almacén de objetos) solo en variables de entorno. Consola admin (Thymeleaf) bajo `/admin/**` con sesión y CSRF.

---

## 10. Seguridad

Lista mínima (alineada con OWASP MASVS):

- [ ] SQLCipher con clave aleatoria de 32 bytes envuelta por una clave AES del Android Keystore.
- [ ] Imágenes cifradas con AES-GCM en almacenamiento interno; `android:allowBackup="false"` y reglas de extracción de datos que excluyan base e imágenes.
- [ ] PIN con hash (PBKDF2 + sal) y bloqueo temporal; huella con `BiometricPrompt` (`BIOMETRIC_STRONG`).
- [ ] Sin permisos de red en el flujo principal; `INTERNET` solo lo usan respaldo y descarga (verificar en modo avión).
- [ ] TLS 1.2+, sin tráfico en claro (`usesCleartextTraffic="false"`); considerar *certificate pinning* para el servidor propio.
- [ ] Verificación SHA-256 del modelo y del respaldo antes de usarlos.
- [ ] Servidor: BCrypt, JWT corto, validación de DTO, consultas parametrizadas (JPA), mínimo privilegio en la BD, auditoría sin datos personales, sin imágenes.
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
| RF-20 | Instalar y actualizar el modelo | Alta | Descarga verificada; versión anterior conservada |
| RF-21 | Respaldo cifrado y restauración | Baja | Restaurar en otro teléfono reproduce el período |
| RF-22 | Actualizar parámetros del NRUS | Media | Nueva versión aplica a períodos nuevos, no a cerrados |

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
| RNF-14 | Cero envíos de imágenes para inferencia |
| RNF-15 | SQLCipher + PIN o biometría |
| RNF-16 | TLS 1.2+ en tránsito; AES-256 en reposo |
| RNF-17 | Parámetros versionados, sin recompilar |
| RNF-18 | Cobertura de pruebas del motor de reglas ≥ 80 % |
| RNF-19 | Cambiar el modelo sin reinstalar la app |

---

## 12. Pruebas

| Nivel | Qué | Herramienta | Mínimo |
|---|---|---|---|
| Unitarias `:dominio` | `MotorReglasNRUS` (casos 5.4), `ValidadorRuc` (5.3), `DetectorDuplicados`, `PeriodoMensual`, `ParserRespuesta` (JSON válido, envuelto, incompleto, montos con coma) | JUnit | Cobertura ≥ 80 % en `reglas` |
| Instrumentadas `:app` | DAO y migraciones Room con SQLCipher; `AlmacenImagenes`; guardado transaccional | AndroidX Test | Todos los DAO |
| UI | Flujo P04→P10 con un `ExtractorFacturas` falso; P08 estados; P09; P19 | Espresso | Flujo principal y excepciones |
| Inferencia | Factura de control empaquetada: 7 campos en ≤ 20 s en el teléfono de referencia | Prueba manual + registro de tiempos | Por versión del modelo |
| API | Controladores y seguridad (401/403), validación de DTO | MockMvc, Testcontainers (Oracle XE) | Todas las rutas |
| Aceptación | Mes simulado con las facturas reales del conjunto de prueba | Planilla de control | RNF-01, 03, 04 |

Usa un **`ExtractorFacturas` falso** (devuelve el JSON de la sección 6.2) para desarrollar y probar toda la app sin el modelo real.

---

## 13. Plan de construcción por iteraciones

> Nota del curso: para el avance siguiente el docente pidió mostrar **alrededor de 20 % del back-end y 60–80 % del front-end** (interfaces construidas a partir del prototipo y la base de datos). Por eso la iteración I3 prioriza las pantallas navegables con datos locales.

**I3 · semanas 9–10 — Front-end navegable y dominio**
1. Crear el proyecto Android (`:app`, `:dominio`), `libs.versions.toml`, tema y tokens (8.3), `MainActivity` con NavHost y barra inferior.
2. `:dominio` completo con pruebas: modelo, `ValidadorRuc`, `MotorReglasNRUS`, `DetectorDuplicados`, `ParserRespuesta`.
3. Pantallas P01–P19 con ViewModels y **repositorios en memoria** + `ExtractorFacturas` falso.
4. CameraX en P06 con evaluación de nitidez y luz.

**I4 · semanas 11–12 — Persistencia e IA local (APF3)**
5. Room + SQLCipher (13 tablas, migraciones), `AlmacenImagenes`, repositorios reales, guardado transaccional.
6. `LiteRtLmPuente.kt`, `GestorModeloLocal` (comprobación, descarga, SHA-256, prueba), P03.
7. Medir tiempo y memoria en el teléfono de referencia con el modelo convertido.
8. Prueba de usabilidad con 3 titulares (protocolo del informe, Anexo S).

**I5 · semanas 13–14 — Reportes, respaldo y administración**
9. Reporte PDF (8.5) y compartir; cierre de mes e historial.
10. WorkManager: recordatorios y avisos.
11. `respaldo-api`: esquema Flyway, auth JWT, respaldos, manifiesto, parámetros, consola P20.
12. Cliente de respaldo con cifrado 7.3 y restauración.

**I6 · semanas 15–18 — Pruebas y validación**
13. Pruebas de integración y de seguridad (lista 10), modo avión, cierre forzado.
14. Validación en mes simulado y métricas finales; informe y sustentación.

---

## 14. Convenciones

- **Idioma:** identificadores del dominio en español sin tildes (`FacturaCompra`, `importeTotal`, `registrarVentas`); sufijos técnicos en inglés cuando son convención (`UseCase`, `ViewModel`, `Dao`, `Entity`, `Controller`).
- **Estilo:** Google Java Style; clases `final` cuando no se heredan; sin `null` en retornos de colecciones; `Optional` en búsquedas; inmutabilidad en objetos de valor (`Determinacion`, `ResultadoExtraccion`).
- **Dinero:** `BigDecimal` escala 2 (`Montos.de("1450.00")`); comparar con `compareTo`, nunca `equals`.
- **Fechas:** `java.time` (`LocalDate`, `YearMonth`); zona `America/Lima` para «hoy».
- **Git:** rama `main` protegida; ramas `feature/<rf>-<descripcion>`; commits en español en imperativo («Agrega validación de RUC»); cada PR indica los RF/RNF que cubre.
- **Definición de terminado:** compila, pruebas verdes, criterio de aceptación de 11.1 verificado, textos en español, sin datos sensibles en logs, este documento actualizado si cambió un contrato.

---

## 15. Artefactos de referencia

En `documentos_teoricos/` del repositorio `Facturas_S20`:

| Artefacto | Contenido |
|---|---|
| `entregable2_documento.docx` | Informe APF2 completo: BPMN, clases, DER, diseño de BD, prototipo, validación, cronograma y presupuesto |
| `entregable2_presentacion.pptx` | Sustentación APF2 |
| `entregable2_imagenes/` | Todas las figuras en PNG y sus fuentes SVG editables (`fuentes/`); scripts que las regeneran (`scripts/`) |
| `entregable2_imagenes/pantalla_XX_*.png` | Prototipo P01–P20 (referencia visual exacta de cada pantalla) |
| `entregable2_imagenes/bpmn_0X_*.png` | Procesos AS-IS, TO-BE, registro, cierre mensual, publicación del modelo |
| `entregable2_imagenes/clases_*.png`, `der_*.png` | Diagramas de clases y entidad-relación |
| `entregable1_documento.docx` | Informe APF1 (contexto, SRS original) |
| `../tunning_gemma4/tunning_model_gemma4_6.ipynb` | Cuaderno de ajuste fino del prototipo previo |

Documentación externa: guía de LiteRT-LM para Android (developers.google.com/edge/litert-lm/android), Room, CameraX, WorkManager, SQLCipher for Android, Spring Boot, OWASP MASVS.
