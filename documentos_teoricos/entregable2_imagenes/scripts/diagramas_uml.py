"""Arquitectura, despliegue, clases, componentes Java, DER y Gantt del entregable 2."""
from svg import (Svg, guardar, envolver, GRIS, GRIS_MEDIO, GRIS_CLARO, AZUL, AZUL_CLARO, ROJO, ROJO_CLARO,
                 VERDE, VERDE_CLARO, NARANJA, NARANJA_CLARO, BORDE, APP_PRIMARIO, APP_ACENTO)

MORADO = "#5b3f8c"
MORADO_CLARO = "#efe9f7"


class Uml(Svg):
    # ---------- clase UML ----------
    def clase(self, x, y, w, nombre, atributos=(), metodos=(), estereotipo=None, color=AZUL, fs=12.5):
        lh = fs * 1.42
        hh = 30 + (16 if estereotipo else 0)
        ha = max(1, len(atributos)) * lh + 10 if atributos else 0
        hm = len(metodos) * lh + 10 if metodos else 0
        h = hh + ha + hm
        self.rect(x, y, w, h, fill="#fff", stroke=color, sw=1.6, rx=4)
        self.rect(x, y, w, hh, fill=color, stroke=color, sw=1.6, rx=4)
        cy = y + 20
        if estereotipo:
            self.text(x + w / 2, y + 16, f"«{estereotipo}»", size=11, color="#fff", anchor="middle", italic=True)
            cy = y + 35
        self.text(x + w / 2, cy, nombre, size=14, color="#fff", bold=True, anchor="middle")
        yy = y + hh
        if atributos:
            for i, a in enumerate(atributos):
                self.text(x + 10, yy + 17 + i * lh, a, size=fs, color="#262626")
            yy += ha
            if metodos:
                self.line(x, yy, x + w, yy, stroke=color, sw=1)
        for i, m in enumerate(metodos):
            self.text(x + 10, yy + 17 + i * lh, m, size=fs, color=AZUL if color == AZUL else color)
        return (x, y, w, h)

    def etiqueta(self, x, y, t, color=ROJO, size=12, bold=True, anchor="middle", italic=False):
        self.text(x, y, t, size=size, color=color, bold=bold, anchor=anchor, italic=italic)

    # ---------- tabla ER ----------
    def tabla(self, x, y, w, nombre, columnas, color=AZUL, fs=12):
        """columnas: [(texto, tipo)] tipo en {'pk','fk','pkfk','uq',''}"""
        lh = fs * 1.55
        h = 28 + len(columnas) * lh + 8
        self.rect(x, y, w, h, fill="#fff", stroke=color, sw=1.6, rx=4)
        self.rect(x, y, w, 28, fill=color, stroke=color, sw=1.6, rx=4)
        self.text(x + w / 2, y + 19, nombre, size=13.5, color="#fff", bold=True, anchor="middle")
        for i, (t, k) in enumerate(columnas):
            yy = y + 28 + 17 + i * lh
            col, b, pref = "#262626", False, ""
            if k == "pk":
                col, b, pref = ROJO, True, "PK "
            elif k == "fk":
                col, pref = NARANJA, "FK "
            elif k == "pkfk":
                col, b, pref = ROJO, True, "PK FK "
            elif k == "uq":
                col = "#262626"
            self.text(x + 10, yy, pref + t, size=fs, color=col, bold=b)
        return (x, y, w, h)

    # ---------- pata de gallo ----------
    def pata(self, x, y, lado, tipo):
        """Extremo de relación en (x,y) sobre el borde de una tabla. lado: 'izq','der','arr','aba' (hacia dónde
        está la tabla). tipo: '1' (uno y solo uno), '0..1', '1..N', '0..N'."""
        d = {"izq": (-1, 0), "der": (1, 0), "arr": (0, -1), "aba": (0, 1)}[lado]
        dx, dy = d
        px, py = -dy, dx  # perpendicular

        def barra(off):
            cx, cy = x - dx * off, y - dy * off
            self.line(cx + px * 7, cy + py * 7, cx - px * 7, cy - py * 7, stroke=GRIS, sw=1.6)

        def circ(off):
            self.circle(x - dx * off, y - dy * off, 4.5, fill="#fff", stroke=GRIS, sw=1.4)

        def gallo():
            bx, by = x - dx * 14, y - dy * 14
            for s in (-1, 0, 1):
                self.line(bx, by, x + px * 8 * s, y + py * 8 * s, stroke=GRIS, sw=1.5)

        if tipo == "1":
            barra(6); barra(11)
        elif tipo == "0..1":
            barra(6); circ(15)
        elif tipo == "1..N":
            gallo(); barra(18)
        elif tipo == "0..N":
            gallo(); circ(21)

    def rel(self, pts, a_lado, a_tipo, b_lado, b_tipo, etiqueta=None, pos=None, dash=None):
        self.poly(pts, stroke=GRIS, sw=1.5, dash=dash)
        self.pata(pts[0][0], pts[0][1], a_lado, a_tipo)
        self.pata(pts[-1][0], pts[-1][1], b_lado, b_tipo)
        if etiqueta:
            px, py = pos if pos else ((pts[0][0] + pts[-1][0]) / 2, (pts[0][1] + pts[-1][1]) / 2 - 6)
            self.text(px, py, etiqueta, size=11, color=GRIS_MEDIO, italic=True, anchor="middle")

    # ---------- nodos de despliegue ----------
    def nodo(self, x, y, w, h, titulo, estereotipo, fill="#fff", color=AZUL):
        d = 14
        self.polygon([(x, y), (x + d, y - d), (x + w + d, y - d), (x + w, y)], fill=AZUL_CLARO, stroke=color, sw=1.5)
        self.polygon([(x + w, y), (x + w + d, y - d), (x + w + d, y + h - d), (x + w, y + h)], fill="#c5d5ea", stroke=color, sw=1.5)
        self.rect(x, y, w, h, fill=fill, stroke=color, sw=1.6)
        self.text(x + 12, y + 20, f"«{estereotipo}»", size=11.5, color=GRIS_MEDIO, italic=True)
        self.text(x + 12, y + 38, titulo, size=14.5, color=color, bold=True)

    def artefacto(self, x, y, w, h, nombre, detalle=None, fill="#fff", color=GRIS, estereotipo="artefacto"):
        self.rect(x, y, w, h, fill=fill, stroke=color, sw=1.3, rx=3)
        self.path(f"M{x + w - 20},{y + 6} h9 l5,5 v11 h-14 z", stroke=GRIS_MEDIO, sw=1)
        self.text(x + 10, y + 17, f"«{estereotipo}»", size=10.5, color=GRIS_MEDIO, italic=True)
        self.text(x + 10, y + 34, nombre, size=12.5, color="#1a1a1a", bold=True)
        if detalle:
            for i, l in enumerate(envolver(detalle, int((w - 20) / 6.3))):
                self.text(x + 10, y + 50 + i * 14, l, size=11, color=GRIS)

    def componente(self, x, y, w, h, nombre, detalle=None, fill="#fff", color=AZUL, lenguaje=None):
        self.rect(x, y, w, h, fill=fill, stroke=color, sw=1.5, rx=6)
        # ícono de componente UML
        ix, iy = x + w - 26, y + 8
        self.rect(ix + 4, iy, 16, 18, fill="#fff", stroke=color, sw=1)
        self.rect(ix, iy + 3, 8, 4, fill="#fff", stroke=color, sw=1)
        self.rect(ix, iy + 11, 8, 4, fill="#fff", stroke=color, sw=1)
        self.text(x + 10, y + 21, nombre, size=13, color=color, bold=True)
        if detalle:
            for i, l in enumerate(envolver(detalle, int((w - 20) / 6.4))):
                self.text(x + 10, y + 39 + i * 14, l, size=11.2, color=GRIS)
        if lenguaje:
            col = {"Java": "#b07219", "Kotlin": MORADO, "SQL": NARANJA, "XML": GRIS_MEDIO}.get(lenguaje, GRIS)
            tw = len(lenguaje) * 7 + 12
            self.rect(x + w - tw - 8, y + h - 22, tw, 16, fill=col, stroke="none", rx=8)
            self.text(x + w - tw / 2 - 8, y + h - 10, lenguaje, size=10.5, color="#fff", bold=True, anchor="middle")


# ======================================================================
def arquitectura():
    s = Uml(1700, 980)
    s.text(30, 40, "Arquitectura de la solución: la lectura ocurre dentro del teléfono; el servidor es opcional",
           size=20, color=AZUL, bold=True)
    # teléfono
    X, Y, W, H = 30, 70, 1000, 880
    s.rect(X, Y, W, H, fill="#fbfcfd", stroke=AZUL, sw=2.2, rx=16)
    s.text(X + 20, Y + 30, "TELÉFONO ANDROID (8.0+ · 6 GB RAM recomendados) — funciona sin conexión", size=15, color=AZUL, bold=True)
    capas = [
        ("1. Presentación", "Java · Android SDK", AZUL, [
            ("Activities y Fragments", "Acceso, Inicio, Captura, Verificación, Facturas, Mes, Reporte, Ajustes"),
            ("ViewModels + LiveData", "Estado de cada pantalla; sin lógica de negocio"),
            ("CameraX", "Vista previa, guía de encuadre, captura y medición de nitidez"),
        ]),
        ("2. Dominio", "Java puro · sin dependencias de Android", VERDE, [
            ("Casos de uso", "RegistrarFactura, DeterminarCategoria, CerrarPeriodo, GenerarReporte"),
            ("MotorReglasNRUS", "Categoría, cuota, avisos 80 %/100 %, topes anuales y vencimiento"),
            ("Validadores", "ValidadorRuc (módulo 11), DetectorDuplicados, ValidadorFecha"),
        ]),
        ("3. Inferencia local", "Java + puente Kotlin", MORADO, [
            ("ExtractorGemmaLocal", "Instrucción, redimensionado a 896 px, parseo del JSON y confianza por campo"),
            ("LiteRtLmPuente (Kotlin)", "Único archivo Kotlin: envuelve Engine y Conversation de LiteRT-LM"),
            ("gemma-4-e2b-facturas.litertlm", "Modelo ajustado y cuantizado (≈ 2,6 GB) · CPU/GPU del teléfono"),
        ]),
        ("4. Datos", "Java · Room", NARANJA, [
            ("Room + SQLCipher", "13 tablas; base cifrada con clave en Android Keystore"),
            ("Almacén de imágenes", "Archivos JPEG cifrados en almacenamiento interno de la app"),
            ("Repositorios", "Interfaces del dominio implementadas sobre DAO de Room"),
        ]),
    ]
    cy = Y + 50
    for nombre, tec, col, comps in capas:
        s.rect(X + 20, cy, 740, 190, fill="#fff", stroke=col, sw=1.6, rx=10)
        s.rect(X + 20, cy, 150, 190, fill=col, stroke=col, sw=1.6, rx=10)
        s.rect(X + 150, cy, 20, 190, fill=col, stroke="none")
        s.lines(X + 34, cy + 80, envolver(nombre, 14), size=15, color="#fff", bold=True, lh=19)
        s.lines(X + 34, cy + 125, envolver(tec, 17), size=11, color="#fff", lh=14)
        for i, (cn, cd) in enumerate(comps):
            s.componente(X + 185, cy + 12 + i * 59, 560, 52, cn, cd, color=col)
        cy += 205
    # servicios android
    sx = X + 780
    s.rect(sx, Y + 50, 200, 805, fill=GRIS_CLARO, stroke=GRIS_MEDIO, sw=1.3, rx=10)
    s.text(sx + 100, Y + 76, "Servicios de Android", size=13.5, color=GRIS, bold=True, anchor="middle")
    servicios = [("WorkManager", "Recordatorios, descarga reanudable del modelo, respaldo"),
                 ("Notificaciones", "Aviso de tope y de vencimiento"),
                 ("BiometricPrompt", "Acceso con huella o PIN"),
                 ("Android Keystore", "Clave de SQLCipher y de las imágenes"),
                 ("DataStore", "Parámetros NRUS versionados y preferencias"),
                 ("PdfDocument", "Reporte mensual en PDF"),
                 ("Intent de compartir", "Envío del reporte por correo o mensajería")]
    for i, (n, d) in enumerate(servicios):
        yy = Y + 95 + i * 108
        s.rect(sx + 12, yy, 176, 96, fill="#fff", stroke=BORDE, sw=1, rx=6)
        s.text(sx + 22, yy + 22, n, size=12.5, color="#1a1a1a", bold=True)
        s.lines(sx + 22, yy + 42, envolver(d, 24), size=11, color=GRIS, lh=14)
    # servidor
    SX = 1080
    s.rect(SX, 70, 590, 470, fill="#fbfcfd", stroke=GRIS, sw=2, rx=16, dash="8 5")
    s.text(SX + 20, 100, "SERVIDOR DE RESPALDO (opcional) — Java 21 · Spring Boot 3", size=15, color=GRIS, bold=True)
    comps = [("API REST /api/v1", "Respaldos, manifiesto del modelo y parámetros NRUS (JSON sobre HTTPS)"),
             ("Spring Security + JWT", "Cuenta por RUC; tokens de corta duración; roles BODEGUERO y ADMIN"),
             ("Servicios y JPA", "Reglas de respaldo, versionado de modelos y de parámetros"),
             ("Consola de administración", "Thymeleaf: publicar versión del modelo y parámetros"),
             ("Oracle Database / SQL Server", "9 tablas; respaldo cifrado de extremo a extremo (BLOB)")]
    for i, (n, d) in enumerate(comps):
        s.componente(SX + 20, 120 + i * 82, 550, 70, n, d, color=GRIS, lenguaje="SQL" if i == 4 else "Java")
    # entrenamiento
    TY = 580
    s.rect(SX, TY, 590, 370, fill="#fbfcfd", stroke=MORADO, sw=2, rx=16, dash="8 5")
    s.text(SX + 20, TY + 30, "ENTRENAMIENTO (fuera del producto) — Google Colab con GPU", size=15, color=MORADO, bold=True)
    pasos = ["Conjunto de facturas etiquetadas (Drive)", "Ajuste fino LoRA · Unsloth + TRL · 896 px",
             "Evaluación por campo contra el modelo base", "Fusión, cuantización mixta 2/4/8 bits",
             "Conversión a .litertlm y evaluación final", "Publicación: archivo + SHA-256 + notas"]
    for i, p in enumerate(pasos):
        yy = TY + 50 + i * 52
        s.rect(SX + 20, yy, 550, 40, fill=MORADO_CLARO, stroke=MORADO, sw=1.2, rx=6)
        s.circle(SX + 44, yy + 20, 12, fill=MORADO, stroke="none")
        s.text(SX + 44, yy + 25, str(i + 1), size=12, color="#fff", bold=True, anchor="middle")
        s.text(SX + 66, yy + 25, p, size=12.5, color="#1a1a1a")
        if i < len(pasos) - 1:
            s.line(SX + 44, yy + 32, SX + 44, yy + 52, stroke=MORADO, sw=1.2)
    # conexiones
    s.path(f"M{X + W},{300} C{X + W + 30},300 {SX - 30},300 {SX},300", stroke=GRIS, sw=1.8, dash="6 4", end="flecha", start="flecha")
    s.rect(X + W + 2, 250, 46, 38, fill="#fff", stroke="none")
    s.lines(X + W + 25, 262, ["HTTPS", "TLS 1.2+"], size=10.5, color=GRIS, anchor="middle", lh=12)
    s.lines(X + W + 25, 330, ["sin", "imágenes"], size=10.5, color=ROJO, bold=True, anchor="middle", lh=12)
    s.path(f"M{SX},{TY + 330} C{SX - 30},{TY + 330} {X + W + 30},{700} {X + W},{700}", stroke=MORADO, sw=1.8, dash="6 4", end="flecha_gris")
    s.lines(X + W + 25, 650, ["descarga", "única del", "modelo"], size=10.5, color=MORADO, anchor="middle", lh=12)
    guardar(s, "arquitectura_capas")


# ======================================================================
def despliegue():
    s = Uml(1700, 820)
    s.text(30, 40, "Arquitectura tecnológica: diagrama de despliegue UML", size=20, color=AZUL, bold=True)
    # teléfono
    s.nodo(40, 90, 620, 690, "Teléfono Android del bodeguero", "dispositivo")
    s.text(52, 150, "Android 8.0+ (API 26) · 6 GB de RAM · 3 GB libres", size=12, color=GRIS)
    s.nodo(70, 190, 560, 250, "Android Runtime (ART)", "entorno de ejecución", fill="#fcfdff")
    s.artefacto(95, 250, 250, 80, "facturas-s20.apk", "Java ≈ 85 % · APK ≤ 60 MB")
    s.artefacto(360, 250, 245, 80, "litertlm-android", "Motor LiteRT-LM (CPU/GPU)", estereotipo="biblioteca")
    s.artefacto(95, 345, 510, 72, "WorkManager · CameraX · Room · BiometricPrompt", "Bibliotecas de Jetpack", estereotipo="bibliotecas")
    s.nodo(70, 480, 560, 270, "Almacenamiento interno de la app", "almacenamiento", fill="#fcfdff")
    s.artefacto(95, 540, 250, 88, "gemma-4-e2b-facturas.litertlm", "≈ 2,6 GB · verificado con SHA-256", fill=MORADO_CLARO)
    s.artefacto(360, 540, 245, 88, "facturas.db", "SQLite cifrada con SQLCipher", fill=NARANJA_CLARO, estereotipo="base de datos")
    s.artefacto(95, 643, 510, 80, "imagenes/*.jpg.enc", "Imágenes de facturas cifradas con clave del Keystore", estereotipo="archivos")
    # servidor
    s.nodo(760, 90, 460, 330, "Servidor de aplicaciones", "servidor")
    s.text(772, 150, "Linux · JDK 21 · 2 vCPU · 4 GB RAM", size=12, color=GRIS)
    s.artefacto(785, 180, 410, 100, "respaldo-api.jar", "Spring Boot 3: API REST, Spring Security (JWT), JPA y consola Thymeleaf")
    s.artefacto(785, 295, 410, 100, "application.yml", "Perfiles dev/prod; secretos en variables de entorno, nunca en el repositorio",
                estereotipo="configuración")
    s.nodo(760, 480, 460, 170, "Servidor de base de datos", "servidor")
    s.artefacto(785, 545, 410, 80, "facturas_respaldo", "Oracle Database 21c XE o SQL Server 2022", fill=NARANJA_CLARO, estereotipo="esquema")
    s.nodo(1300, 90, 370, 200, "Almacén de objetos", "servicio en la nube")
    s.artefacto(1325, 160, 320, 100, "modelos/v1.3/*.litertlm", "Descarga por HTTPS con URL firmada; manifiesto con SHA-256")
    s.nodo(1300, 350, 370, 190, "Google Colab (GPU)", "entorno de entrenamiento", color=MORADO)
    s.artefacto(1325, 420, 320, 90, "tunning_model_gemma4.ipynb", "Unsloth + TRL; exporta .litertlm", fill=MORADO_CLARO, estereotipo="cuaderno")
    s.nodo(1300, 600, 370, 170, "PC del administrador", "dispositivo")
    s.artefacto(1325, 665, 320, 80, "Navegador web", "Consola de administración (HTTPS)", estereotipo="cliente")
    # enlaces
    s.line(660, 250, 760, 250, stroke=GRIS, sw=2)
    s.text(710, 238, "HTTPS 443", size=11, color=GRIS, anchor="middle", bold=True)
    s.text(710, 270, "JSON · JWT", size=10.5, color=GRIS, anchor="middle")
    s.text(710, 286, "opcional", size=10.5, color=ROJO, anchor="middle")
    s.line(990, 420, 990, 466, stroke=GRIS, sw=2)
    s.text(1000, 448, "JDBC · TLS · 1521 / 1433", size=11, color=GRIS, bold=True)
    s.poly([(675, 600), (720, 600), (720, 795), (1280, 795), (1280, 215), (1300, 215)], stroke=MORADO, sw=2, dash="6 4")
    s.text(1000, 787, "descarga del modelo por Wi-Fi (HTTPS)", size=11, color=MORADO, bold=True, anchor="middle")
    s.line(1485, 336, 1485, 290, stroke=MORADO, sw=2, dash="6 4")
    s.text(1495, 318, "publica", size=11, color=MORADO, bold=True)
    s.poly([(1300, 700), (1245, 700), (1245, 380), (1234, 380)], stroke=GRIS, sw=2)
    s.text(1296, 692, "HTTPS", size=11, color=GRIS, bold=True, anchor="end")
    guardar(s, "arquitectura_despliegue")


# ======================================================================
def clases_dominio():
    s = Uml(1760, 1180)
    s.text(30, 38, "Diagrama de clases del dominio (paquete pe.facturass20.dominio.modelo)", size=20, color=AZUL, bold=True)
    C = {}
    C["contrib"] = s.clase(30, 70, 330, "Contribuyente",
                           ["- ruc: String {11 dígitos}", "- nombre: String", "- ultimoDigitoRuc: int", "- fechaAlta: LocalDate"],
                           ["+ periodoActual(): PeriodoMensual"])
    C["periodo"] = s.clase(470, 70, 360, "PeriodoMensual",
                           ["- anio: int", "- mes: int", "- totalVentas: BigDecimal", "- estado: EstadoPeriodo"],
                           ["+ totalAdquisiciones(): BigDecimal", "+ registrarVentas(monto: BigDecimal)", "+ cerrar(): void"])
    C["det"] = s.clase(950, 70, 380, "Determinacion",
                       ["- totalAdquisiciones: BigDecimal", "- montoDeterminante: BigDecimal", "- cuota: BigDecimal",
                        "- fechaVencimiento: LocalDate", "- nivelAlerta: NivelAlerta"],
                       ["+ categoria(): CategoriaNRUS"])
    C["cat"] = s.clase(1420, 70, 320, "CategoriaNRUS",
                       ["- codigo: int", "- limiteMensual: BigDecimal", "- cuota: BigDecimal", "- vigenteDesde: LocalDate",
                        "- vigenteHasta: LocalDate"], ["+ admite(monto): boolean"])
    C["param"] = s.clase(1420, 330, 320, "ParametrosNrus",
                         ["- version: String", "- umbralAviso: BigDecimal {0,80}", "- topeAnual: BigDecimal"],
                         ["+ categoriasVigentes(f): List", "+ cronograma(): Cronograma"])
    C["cron"] = s.clase(1420, 560, 320, "CronogramaVencimiento",
                        ["- anio: int", "- mes: int", "- ultimoDigito: int", "- fechaLimite: LocalDate"],
                        ["+ fechaPara(periodo, digito): LocalDate"])
    C["fact"] = s.clase(470, 400, 360, "FacturaCompra",
                        ["- serie: String", "- numero: String", "- fechaEmision: LocalDate", "- moneda: Moneda",
                         "- importeTotal: BigDecimal", "- estado: EstadoFactura", "- origen: OrigenRegistro", "- motivoAnulacion: String"],
                        ["+ anular(motivo: String): void", "+ claveUnica(): String"])
    C["emisor"] = s.clase(30, 400, 330, "Emisor", ["- ruc: String", "- razonSocial: String"], ["+ rucValido(): boolean"])
    C["img"] = s.clase(30, 640, 330, "ImagenFactura", ["- ruta: String", "- nitidez: double", "- fechaCaptura: LocalDateTime"],
                       ["+ esLegible(umbral): boolean"])
    C["res"] = s.clase(470, 820, 360, "ResultadoExtraccion", ["- versionModelo: String", "- tiempoMs: long"],
                       ["+ camposARevisar(): List<CampoExtraido>", "+ confianzaMinima(): double"])
    C["campo"] = s.clase(950, 820, 380, "CampoExtraido",
                         ["- nombre: CampoFactura", "- valorLeido: String", "- valorFinal: String", "- confianza: double",
                          "- corregido: boolean"], ["+ requiereRevision(): boolean"])
    C["modelo"] = s.clase(1420, 820, 320, "ModeloLocal",
                          ["- version: String", "- tamanoMb: int", "- sha256: String", "- estado: EstadoModelo"],
                          ["+ verificarIntegridad(): boolean"])
    C["rep"] = s.clase(950, 400, 380, "ReporteMensual", ["- fechaGeneracion: LocalDateTime", "- rutaPdf: String", "- sha256: String"],
                       ["+ exportar(): File"])
    C["aviso"] = s.clase(950, 610, 380, "Aviso", ["- tipo: TipoAviso", "- fechaProgramada: LocalDateTime", "- enviado: boolean"],
                         ["+ programar(): void"])
    # enumeraciones
    ex = 30
    enums = [("EstadoPeriodo", ["ABIERTO", "CERRADO"]), ("EstadoFactura", ["VIGENTE", "ANULADA"]),
             ("OrigenRegistro", ["IA", "MANUAL"]), ("NivelAlerta", ["NINGUNA", "AVISO_80", "LIMITE_100", "FUERA_DE_REGIMEN"])]
    for i, (n, vals) in enumerate(enums):
        s.clase(ex + (i % 2) * 205, 850 + (i // 2) * 150, 195, n, vals, estereotipo="enumeration", color=GRIS_MEDIO, fs=11.5)
    # relaciones
    L = s.line

    def mult(x, y, t, anchor="middle"):
        s.etiqueta(x, y, t, anchor=anchor)

    L(360, 110, 470, 110); mult(372, 104, "1", "start"); mult(462, 104, "1..*", "end")
    s.text(415, 128, "declara", size=11.5, color=GRIS_MEDIO, italic=True, anchor="middle")
    L(830, 110, 950, 110); mult(842, 104, "1", "start"); mult(942, 104, "0..1", "end")
    s.text(890, 128, "determina", size=11.5, color=GRIS_MEDIO, italic=True, anchor="middle")
    L(1330, 110, 1420, 110); mult(1342, 104, "*", "start"); mult(1412, 104, "1", "end")
    s.text(1375, 128, "ubica en", size=11.5, color=GRIS_MEDIO, italic=True, anchor="middle")
    # periodo ◆— factura
    s.line(650, 244, 650, 400, stroke=GRIS, sw=1.5, start="rombo")
    mult(662, 272, "1", "start"); mult(662, 392, "0..*", "start")
    s.text(700, 350, "contiene", size=11.5, color=GRIS_MEDIO, italic=True)
    L(360, 440, 470, 440); mult(372, 434, "1", "start"); mult(462, 434, "0..*", "end")
    s.text(415, 458, "emite", size=11.5, color=GRIS_MEDIO, italic=True, anchor="middle")
    s.poly([(470, 600), (420, 600), (420, 690), (360, 690)], stroke=GRIS, sw=1.5)
    mult(462, 594, "1", "end"); mult(368, 684, "1", "start")
    s.text(424, 650, "respalda", size=11.5, color=GRIS_MEDIO, italic=True)
    s.line(650, 820, 650, 628, stroke=GRIS, sw=1.5)
    mult(662, 648, "1", "start"); mult(662, 812, "0..1", "start")
    s.text(700, 760, "produce", size=11.5, color=GRIS_MEDIO, italic=True)
    s.line(830, 870, 950, 870, stroke=GRIS, sw=1.5, start="rombo")
    mult(842, 864, "1", "start"); mult(942, 864, "1..*", "end")
    s.line(1330, 890, 1420, 890, stroke=GRIS, sw=1.5, dash="6 4", end="abierta")
    s.text(1375, 880, "generado por", size=11, color=GRIS_MEDIO, italic=True, anchor="middle")
    s.poly([(830, 180), (890, 180), (890, 440), (950, 440)], stroke=GRIS, sw=1.5)
    mult(842, 174, "1", "start"); mult(942, 434, "0..1", "end")
    s.text(896, 330, "resume", size=11.5, color=GRIS_MEDIO, italic=True)
    s.poly([(1330, 160), (1356, 160), (1356, 660), (1330, 660)], stroke=GRIS, sw=1.5)
    mult(1338, 154, "1", "start"); mult(1338, 654, "0..*", "start")
    s.text(1350, 560, "origina", size=11, color=GRIS_MEDIO, italic=True, anchor="end")
    s.line(1580, 330, 1580, 227, stroke=GRIS, sw=1.5, start="rombo_hueco")
    mult(1590, 316, "1", "start"); mult(1590, 246, "1..*", "start")
    s.line(1580, 470, 1580, 560, stroke=GRIS, sw=1.5, start="rombo_hueco")
    mult(1590, 552, "1..*", "start")
    s.poly([(1330, 205), (1390, 205), (1390, 620), (1420, 620)], stroke=GRIS, sw=1.5, dash="6 4", end="abierta")
    s.text(1344, 252, "vence", size=10.5, color=GRIS_MEDIO, italic=True)
    s.text(1344, 265, "según", size=10.5, color=GRIS_MEDIO, italic=True)
    s.rect(470, 1010, 1270, 64, fill="#f7f7f7", stroke=BORDE, sw=1, rx=6)
    s.text(486, 1034, "Otras enumeraciones:", size=12, color=GRIS, bold=True)
    s.text(660, 1034, "Moneda {PEN, USD} · TipoAviso {TOPE_80, TOPE_100, VENCIMIENTO} · "
           "EstadoModelo {NO_INSTALADO, DESCARGANDO, VERIFICADO, ACTIVO}", size=11.5, color=GRIS)
    s.text(486, 1058, "CampoFactura {RUC, RAZON_SOCIAL, SERIE, NUMERO, FECHA_EMISION, MONEDA, IMPORTE_TOTAL}", size=11.5, color=GRIS)
    guardar(s, "clases_dominio")


# ======================================================================
def clases_diseno():
    s = Uml(1760, 1000)
    s.text(30, 38, "Diagrama de clases de diseño: casos de uso, inferencia y persistencia (flujo CU-01)", size=20, color=AZUL, bold=True)
    cols = [(30, "ui", AZUL), (470, "dominio", VERDE), (910, "inferencia", MORADO), (1350, "datos", NARANJA)]
    for x, n, c in cols:
        s.rect(x, 60, 390, 920, fill="#fcfcfc", stroke=c, sw=1.2, rx=8, dash="5 4")
        s.rect(x, 60, len(n) * 7.5 + 120, 26, fill=c, stroke=c, sw=1.2, rx=4)
        s.text(x + 10, 78, f"pe.facturass20.{n}", size=12, color="#fff", bold=True)
    s.clase(55, 110, 340, "CapturaActivity", ["- previewView: PreviewView", "- evaluador: EvaluadorNitidez"],
            ["# onCreate(b: Bundle)", "- tomarFoto(): void", "- abrirGaleria(): void"])
    s.clase(55, 330, 340, "VerificacionViewModel", ["- estado: MutableLiveData<EstadoVerif>", "- registrar: RegistrarFacturaUseCase"],
            ["+ procesar(imagen: Bitmap): void", "+ corregir(campo, valor): void", "+ confirmar(): void"])
    s.clase(55, 580, 340, "ResumenMesViewModel", ["- determinar: DeterminarCategoriaUseCase"],
            ["+ resumen(): LiveData<ResumenMes>", "+ registrarVentas(monto): void"])
    s.clase(55, 780, 340, "EvaluadorNitidez", ["- umbralLaplaciano: double"], ["+ evaluar(img: Bitmap): double"])
    s.clase(495, 110, 340, "RegistrarFacturaUseCase", ["- extractor: ExtractorFacturas", "- facturas: FacturaRepositorio",
                                                       "- validador: ValidadorRuc", "- motor: MotorReglasNRUS"],
            ["+ extraer(img): ResultadoExtraccion", "+ guardar(borrador): FacturaCompra"])
    s.clase(495, 390, 340, "MotorReglasNRUS", ["- parametros: ParametrosNrus"], ["+ determinar(p: PeriodoMensual): Determinacion"])
    s.clase(495, 540, 340, "DeterminarCategoriaUseCase", ["- periodos: PeriodoRepositorio", "- motor: MotorReglasNRUS"],
            ["+ ejecutar(periodoId): Determinacion"])
    s.clase(495, 740, 340, "ValidadorRuc", [], ["+ esValido(ruc: String): boolean {static}"])
    s.clase(495, 850, 340, "ExtractorFacturas", [], ["+ extraer(img: Bitmap): ResultadoExtraccion"], estereotipo="interface", color=VERDE)
    s.clase(935, 110, 340, "ExtractorGemmaLocal", ["- motor: MotorInferencia", "- INSTRUCCION: String {final}"],
            ["+ extraer(img): ResultadoExtraccion", "- parsear(json): ResultadoExtraccion"])
    s.clase(935, 360, 340, "MotorInferencia", [], ["+ generar(img, instruccion): String", "+ cerrar(): void"], estereotipo="interface", color=MORADO)
    s.clase(935, 540, 340, "LiteRtLmPuente", ["- engine: Engine", "- rutaModelo: String"],
            ["+ generar(img, instruccion): String", "+ cerrar(): void"], estereotipo="Kotlin", color=MORADO)
    s.clase(935, 770, 340, "GestorModeloLocal", ["- modelo: ModeloLocal"], ["+ memoriaSuficiente(): boolean", "+ verificarSha256(): boolean"])
    s.clase(1375, 110, 340, "FacturaRepositorio", [], ["+ guardar(f): long", "+ existe(ruc, serie, num): boolean",
                                                     "+ delPeriodo(id): List<FacturaCompra>"], estereotipo="interface", color=NARANJA)
    s.clase(1375, 330, 340, "FacturaRepositorioRoom", ["- dao: FacturaDao", "- db: BaseDatosFacturas"],
            ["+ guardar(f): long", "+ existe(...): boolean"])
    s.clase(1375, 540, 340, "FacturaDao", [], ["@Insert insertar(e): long", "@Query buscarDuplicado(...)", "@Transaction guardarCompleta(...)"],
            estereotipo="Dao", color=NARANJA)
    s.clase(1375, 770, 340, "BaseDatosFacturas", ["- instancia: BaseDatosFacturas {static}"],
            ["+ get(ctx, clave): BaseDatosFacturas", "+ facturaDao(): FacturaDao"], estereotipo="RoomDatabase", color=NARANJA)
    # dependencias (línea discontinua con flecha abierta)
    dep = lambda pts: s.poly(pts, stroke=GRIS, sw=1.4, dash="6 4", end="abierta")
    real = lambda pts: s.poly(pts, stroke=GRIS, sw=1.4, dash="6 4", end="triangulo")
    dep([(225, 262), (225, 330)])
    dep([(395, 430), (440, 430), (440, 170), (495, 170)])
    dep([(395, 610), (495, 610)])
    dep([(55, 820), (40, 820), (40, 180), (55, 180)])
    dep([(665, 266), (665, 390)])
    dep([(665, 540), (665, 470)])
    dep([(835, 150), (870, 150), (870, 900), (835, 900)])
    dep([(495, 200), (482, 200), (482, 770), (495, 770)])
    real([(935, 190), (890, 190), (890, 870), (835, 870)])
    dep([(1105, 231), (1105, 360)])
    real([(1105, 540), (1105, 452)])
    dep([(835, 125), (855, 125), (855, 98), (1360, 98), (1360, 140), (1375, 140)])
    real([(1545, 330), (1545, 219)])
    dep([(1545, 451), (1545, 540)])
    dep([(1545, 649), (1545, 770)])
    s.text(1112, 318, "usa", size=11, color=GRIS_MEDIO, italic=True)
    s.text(1112, 500, "implementa", size=11, color=GRIS_MEDIO, italic=True)
    s.text(1552, 300, "implementa", size=11, color=GRIS_MEDIO, italic=True)
    s.text(896, 520, "implementa", size=10.5, color=GRIS_MEDIO, italic=True)
    # leyenda
    s.rect(935, 900, 390, 70, fill="#fff", stroke=BORDE, sw=1, rx=6)
    s.line(950, 922, 1000, 922, stroke=GRIS, sw=1.4, dash="6 4", end="abierta")
    s.text(1010, 926, "dependencia (usa)", size=11.5, color=GRIS)
    s.line(950, 950, 1000, 950, stroke=GRIS, sw=1.4, dash="6 4", end="triangulo")
    s.text(1010, 954, "realización (implementa una interfaz)", size=11.5, color=GRIS)
    guardar(s, "clases_diseno")


# ======================================================================
def componentes_java():
    s = Uml(1700, 860)
    s.text(30, 38, "Componentes desarrollados en Java y su participación en el código del producto", size=20, color=AZUL, bold=True)
    s.rect(30, 60, 1010, 770, fill="#fbfcfd", stroke=AZUL, sw=2, rx=14)
    s.text(50, 90, "Módulo :app — aplicación Android (paquete pe.facturass20)", size=15, color=AZUL, bold=True)
    comps = [
        (50, 110, "ui.acceso", "Configuración inicial, PIN y huella (P01–P02)", "Java"),
        (380, 110, "ui.captura", "CameraX, guía de encuadre y nitidez (P05–P06)", "Java"),
        (710, 110, "ui.verificacion", "Lectura, verificación y duplicados (P07–P10)", "Java"),
        (50, 220, "ui.facturas", "Lista, detalle y anulación (P11–P12)", "Java"),
        (380, 220, "ui.mes", "Inicio, ventas, categoría y vencimiento (P04, P13–P15)", "Java"),
        (710, 220, "ui.reportes / ui.ajustes", "Reporte, histórico, modelo y respaldo (P16–P19)", "Java"),
        (50, 350, "dominio.casosuso", "Registrar, verificar, determinar, cerrar, reportar", "Java"),
        (380, 350, "dominio.reglas", "MotorReglasNRUS, ValidadorRuc, DetectorDuplicados", "Java"),
        (710, 350, "dominio.modelo", "Entidades del dominio y enumeraciones", "Java"),
        (50, 480, "inferencia", "ExtractorGemmaLocal, parser JSON, GestorModeloLocal", "Java"),
        (380, 480, "inferencia.litert", "LiteRtLmPuente.kt: única clase Kotlin", "Kotlin"),
        (710, 480, "datos", "Room: entidades, DAO, BaseDatosFacturas (SQLCipher)", "Java"),
        (50, 610, "trabajos", "WorkManager: recordatorios, descarga del modelo, respaldo", "Java"),
        (380, 610, "respaldo.cliente", "Cliente HTTP (Retrofit) del servidor de respaldo", "Java"),
        (710, 610, "res/ + build.gradle", "Layouts, cadenas en español, temas y configuración", "XML"),
    ]
    for x, y, n, d, lang in comps:
        col = MORADO if lang == "Kotlin" else (GRIS_MEDIO if lang == "XML" else AZUL)
        s.componente(x, y, 310, 92, n, d, color=col, lenguaje=lang)
    for yy, t in ((330, "Dominio (sin Android)"), (460, "Inferencia y datos"), (590, "Soporte")):
        s.text(52, yy + 4, t, size=11.5, color=GRIS_MEDIO, italic=True)
    s.text(50, 740, "Regla de dependencias: ui → dominio ← inferencia / datos. El dominio no importa clases de Android ni de LiteRT-LM,",
           size=12, color=GRIS)
    s.text(50, 758, "por eso MotorReglasNRUS y ValidadorRuc se prueban con JUnit en la JVM, sin emulador.", size=12, color=GRIS)
    # servidor
    s.rect(1070, 60, 600, 430, fill="#fbfcfd", stroke=GRIS, sw=2, rx=14, dash="8 5")
    s.text(1090, 90, "Módulo respaldo-api — Spring Boot 3", size=15, color=GRIS, bold=True)
    sc = [("api", "Controladores REST y DTO validados", "Java"), ("servicio", "Respaldos, versiones de modelo y parámetros", "Java"),
          ("seguridad", "Spring Security, JWT, BCrypt", "Java"), ("repositorio", "Spring Data JPA", "Java"),
          ("admin", "Consola Thymeleaf del administrador", "Java"), ("db/migration", "Scripts Flyway (Oracle / SQL Server)", "SQL")]
    for i, (n, d, lang) in enumerate(sc):
        s.componente(1090 + (i % 2) * 290, 110 + (i // 2) * 120, 275, 100, n, d, color=GRIS, lenguaje=lang)
    # barra de participación
    s.rect(1070, 520, 600, 310, fill="#fff", stroke=BORDE, sw=1.2, rx=14)
    s.text(1090, 552, "Participación estimada por lenguaje (producto desplegado)", size=14, color=AZUL, bold=True)
    datos = [("Java — app Android", 60, "#b07219"), ("Java — servicio de respaldo", 25, "#d9a05b"),
             ("XML / Gradle", 7, GRIS_MEDIO), ("Kotlin — puente LiteRT-LM", 5, MORADO), ("SQL", 3, NARANJA)]
    x0, y0, anchoTotal = 1090, 575, 560
    acc = 0
    for n, p, c in datos:
        w = anchoTotal * p / 100
        s.rect(x0 + acc, y0, w, 36, fill=c, stroke="#fff", sw=1)
        if p >= 7:
            s.text(x0 + acc + w / 2, y0 + 23, f"{p} %", size=12, color="#fff", bold=True, anchor="middle")
        acc += w
    for i, (n, p, c) in enumerate(datos):
        yy = 640 + i * 28
        s.rect(1090, yy - 12, 14, 14, fill=c, stroke="none", rx=2)
        s.text(1112, yy, f"{n}: {p} %", size=12.5, color=GRIS)
    s.rect(1420, 640, 230, 120, fill=VERDE_CLARO, stroke=VERDE, sw=1.3, rx=10)
    s.text(1535, 690, "≈ 85 %", size=34, color=VERDE, bold=True, anchor="middle")
    s.text(1535, 720, "del código en Java", size=13, color=VERDE, bold=True, anchor="middle")
    s.text(1535, 740, "(mínimo exigido: 50 %)", size=11.5, color=GRIS, anchor="middle")
    guardar(s, "componentes_java")


# ======================================================================
def der_local():
    s = Uml(1760, 1160)
    s.text(30, 38, "Diagrama entidad-relación de la base local (Room sobre SQLite cifrada con SQLCipher)", size=20, color=AZUL, bold=True)
    T = {}
    T["contrib"] = s.tabla(30, 70, 330, "contribuyente", [("id_contribuyente INTEGER", "pk"), ("ruc TEXT(11) UNIQUE", ""),
                                                          ("nombre TEXT", ""), ("ultimo_digito INTEGER", ""), ("pin_hash TEXT", ""),
                                                          ("fecha_alta TEXT (ISO-8601)", "")])
    T["periodo"] = s.tabla(470, 70, 340, "periodo", [("id_periodo INTEGER", "pk"), ("id_contribuyente", "fk"), ("anio INTEGER", ""),
                                                     ("mes INTEGER CHECK 1..12", ""), ("total_ventas NUMERIC", ""),
                                                     ("estado TEXT ABIERTO|CERRADO", ""), ("UQ (id_contribuyente, anio, mes)", "")])
    T["det"] = s.tabla(930, 70, 360, "determinacion", [("id_determinacion INTEGER", "pk"), ("id_periodo UNIQUE", "fk"),
                                                        ("id_categoria", "fk"), ("total_adquisiciones NUMERIC", ""),
                                                        ("monto_determinante NUMERIC", ""), ("cuota NUMERIC", ""),
                                                        ("fecha_vencimiento TEXT", ""), ("nivel_alerta TEXT", "")])
    T["cat"] = s.tabla(1410, 70, 320, "categoria_nrus", [("id_categoria INTEGER", "pk"), ("id_parametro", "fk"), ("codigo INTEGER", ""),
                                                         ("limite_mensual NUMERIC", ""), ("cuota NUMERIC", ""),
                                                         ("vigente_desde TEXT", ""), ("vigente_hasta TEXT", "")])
    T["param"] = s.tabla(1410, 380, 320, "parametro_version", [("id_parametro INTEGER", "pk"), ("version TEXT UNIQUE", ""),
                                                               ("umbral_aviso NUMERIC", ""), ("tope_anual NUMERIC", ""),
                                                               ("vigente_desde TEXT", ""), ("activo INTEGER 0|1", "")])
    T["cron"] = s.tabla(1410, 650, 320, "cronograma_venc", [("id_cronograma INTEGER", "pk"), ("id_parametro", "fk"), ("anio INTEGER", ""),
                                                            ("mes INTEGER", ""), ("ultimo_digito INTEGER", ""), ("fecha_limite TEXT", ""),
                                                            ("UQ (id_parametro, anio, mes, digito)", "")])
    T["emisor"] = s.tabla(30, 400, 330, "emisor", [("id_emisor INTEGER", "pk"), ("ruc TEXT(11) UNIQUE", ""), ("razon_social TEXT", "")])
    T["fact"] = s.tabla(470, 400, 340, "factura_compra", [("id_factura INTEGER", "pk"), ("id_periodo", "fk"), ("id_emisor", "fk"),
                                                           ("serie TEXT(4)", ""), ("numero TEXT(8)", ""), ("fecha_emision TEXT", ""),
                                                           ("moneda TEXT DEFAULT 'PEN'", ""), ("importe_total NUMERIC > 0", ""),
                                                           ("origen TEXT IA|MANUAL", ""), ("estado TEXT VIGENTE|ANULADA", ""),
                                                           ("motivo_anulacion TEXT", ""), ("UQ (id_emisor, serie, numero)", "")])
    T["img"] = s.tabla(30, 610, 330, "imagen_factura", [("id_imagen INTEGER", "pk"), ("id_factura UNIQUE", "fk"),
                                                         ("ruta_cifrada TEXT", ""), ("nitidez REAL", ""), ("fecha_captura TEXT", "")])
    T["campo"] = s.tabla(470, 830, 340, "campo_extraido", [("id_campo INTEGER", "pk"), ("id_factura", "fk"), ("id_modelo", "fk"),
                                                            ("nombre_campo TEXT", ""), ("valor_leido TEXT", ""), ("valor_final TEXT", ""),
                                                            ("confianza REAL 0..1", ""), ("corregido INTEGER 0|1", "")])
    T["modelo"] = s.tabla(930, 830, 360, "modelo_local", [("id_modelo INTEGER", "pk"), ("version TEXT UNIQUE", ""),
                                                          ("tamano_mb INTEGER", ""), ("sha256 TEXT(64)", ""),
                                                          ("estado TEXT", ""), ("fecha_instalacion TEXT", "")])
    T["rep"] = s.tabla(930, 400, 360, "reporte_mensual", [("id_reporte INTEGER", "pk"), ("id_periodo", "fk"),
                                                          ("ruta_pdf TEXT", ""), ("sha256 TEXT(64)", ""), ("fecha_generacion TEXT", "")])
    T["aviso"] = s.tabla(930, 610, 360, "aviso", [("id_aviso INTEGER", "pk"), ("id_periodo", "fk"), ("tipo TEXT", ""),
                                                  ("fecha_programada TEXT", ""), ("enviado INTEGER 0|1", "")])
    r = s.rel
    r([(360, 110), (470, 110)], "izq", "1", "der", "1..N", "declara", (415, 102))
    r([(810, 110), (930, 110)], "izq", "1", "der", "0..1", "resulta en", (870, 102))
    r([(1290, 130), (1410, 130)], "izq", "0..N", "der", "1", "se ubica en", (1350, 122))
    r([(1570, 380), (1570, 237)], "aba", "1", "arr", "1..N", "define", (1600, 320))
    r([(1570, 528), (1570, 650)], "arr", "1", "aba", "1..N", "publica", (1600, 600))
    r([(640, 237), (640, 400)], "arr", "1", "aba", "0..N", "contiene", (680, 330))
    r([(360, 440), (470, 440)], "izq", "1", "der", "0..N", "emite", (415, 432))
    r([(470, 640), (420, 640), (420, 660), (360, 660)], "der", "1", "izq", "0..1", "respalda", (430, 632))
    r([(640, 659), (640, 830)], "arr", "1", "aba", "0..N", "produce", (680, 750))
    r([(930, 900), (810, 900)], "der", "1", "izq", "0..N", "genera", (870, 892))
    r([(810, 180), (870, 180), (870, 440), (930, 440)], "izq", "1", "der", "0..N", "resume", (900, 330))
    r([(810, 200), (890, 200), (890, 650), (930, 650)], "izq", "1", "der", "0..N", "programa", (918, 560))
    # leyenda
    s.rect(30, 830, 400, 300, fill="#fff", stroke=BORDE, sw=1, rx=8)
    s.text(50, 856, "Notación pata de gallo (Crow's Foot)", size=13.5, color=AZUL, bold=True)
    ej = [("1", "uno y solo uno"), ("0..1", "cero o uno"), ("1..N", "uno o muchos"), ("0..N", "cero o muchos")]
    for i, (t, d) in enumerate(ej):
        yy = 890 + i * 34
        s.line(60, yy, 150, yy, stroke=GRIS, sw=1.5)
        s.pata(150, yy, "der", t)
        s.text(170, yy + 4, d, size=12, color=GRIS)
    s.text(50, 1040, "PK clave primaria (rojo) · FK clave foránea (naranja)", size=11.5, color=GRIS)
    s.text(50, 1058, "UQ restricción de unicidad · 13 tablas en 3FN", size=11.5, color=GRIS)
    s.text(50, 1076, "Montos NUMERIC mapeados a BigDecimal en Java", size=11.5, color=GRIS)
    s.text(50, 1094, "Toda la base se cifra con SQLCipher (AES-256)", size=11.5, color=GRIS)
    guardar(s, "der_local")


# ======================================================================
def der_servidor():
    s = Uml(1760, 900)
    s.text(30, 38, "Diagrama entidad-relación del servidor de respaldo (Oracle Database / SQL Server)", size=20, color=AZUL, bold=True)
    s.tabla(30, 80, 360, "cuenta", [("id_cuenta NUMBER", "pk"), ("ruc CHAR(11) UNIQUE", ""), ("correo VARCHAR2(120) UNIQUE", ""),
                                    ("hash_clave VARCHAR2(100)", ""), ("estado VARCHAR2(10)", ""), ("fecha_alta TIMESTAMP", "")])
    s.tabla(500, 80, 380, "dispositivo", [("id_dispositivo NUMBER", "pk"), ("id_cuenta", "fk"), ("id_instalacion CHAR(36) UNIQUE", ""),
                                          ("modelo_equipo VARCHAR2(60)", ""), ("version_app VARCHAR2(15)", ""), ("ultima_sync TIMESTAMP", "")])
    s.tabla(990, 80, 380, "respaldo", [("id_respaldo NUMBER", "pk"), ("id_cuenta", "fk"), ("id_dispositivo", "fk"), ("anio NUMBER(4)", ""),
                                       ("mes NUMBER(2)", ""), ("contenido_cifrado BLOB", ""), ("sha256 CHAR(64)", ""),
                                       ("version_esquema NUMBER(3)", ""), ("fecha_respaldo TIMESTAMP", ""), ("UQ (id_cuenta, anio, mes)", "")])
    s.tabla(30, 440, 360, "administrador", [("id_admin NUMBER", "pk"), ("usuario VARCHAR2(40) UNIQUE", ""), ("hash_clave VARCHAR2(100)", ""),
                                            ("rol VARCHAR2(10)", ""), ("activo NUMBER(1)", "")])
    s.tabla(500, 440, 380, "version_modelo", [("id_version NUMBER", "pk"), ("id_admin", "fk"), ("version VARCHAR2(15) UNIQUE", ""),
                                              ("url_descarga VARCHAR2(300)", ""), ("sha256 CHAR(64)", ""), ("tamano_mb NUMBER(6)", ""),
                                              ("exactitud_campos NUMBER(5,2)", ""), ("estado VARCHAR2(12)", ""), ("fecha_publicacion TIMESTAMP", "")])
    s.tabla(990, 440, 380, "version_parametros", [("id_parametro NUMBER", "pk"), ("id_admin", "fk"), ("version VARCHAR2(15) UNIQUE", ""),
                                                  ("umbral_aviso NUMBER(3,2)", ""), ("tope_anual NUMBER(12,2)", ""),
                                                  ("vigente_desde DATE", ""), ("publicado NUMBER(1)", "")])
    s.tabla(1450, 100, 290, "categoria_nrus", [("id_categoria NUMBER", "pk"), ("id_parametro", "fk"), ("codigo NUMBER(1)", ""),
                                               ("limite_mensual NUMBER(10,2)", ""), ("cuota NUMBER(8,2)", "")])
    s.tabla(1450, 300, 290, "cronograma_venc", [("id_cronograma NUMBER", "pk"), ("id_parametro", "fk"), ("anio NUMBER(4)", ""),
                                                ("mes NUMBER(2)", ""), ("ultimo_digito NUMBER(1)", ""), ("fecha_limite DATE", "")])
    s.tabla(30, 700, 360, "auditoria", [("id_auditoria NUMBER", "pk"), ("actor VARCHAR2(60)", ""), ("accion VARCHAR2(40)", ""),
                                        ("entidad VARCHAR2(40)", ""), ("fecha TIMESTAMP", ""), ("ip_hash CHAR(64)", "")])
    r = s.rel
    r([(390, 120), (500, 120)], "izq", "1", "der", "0..N", "registra", (445, 112))
    r([(880, 120), (990, 120)], "izq", "1", "der", "0..N", "sube", (935, 112))
    r([(210, 80), (210, 62), (1180, 62), (1180, 80)], "aba", "1", "aba", "0..N", "posee", (700, 56))
    r([(390, 480), (500, 480)], "izq", "1", "der", "0..N", "publica", (445, 472))
    r([(210, 440), (210, 420), (1180, 420), (1180, 440)], "aba", "1", "aba", "0..N", "publica", (940, 414))
    r([(1370, 500), (1410, 500), (1410, 160), (1450, 160)], "izq", "1", "der", "1..N", "define", (1440, 152))
    r([(1370, 540), (1420, 540), (1420, 360), (1450, 360)], "izq", "1", "der", "1..N", "", None)
    s.rect(500, 700, 1240, 180, fill=VERDE_CLARO, stroke=VERDE, sw=1.2, rx=10)
    s.text(520, 728, "Consideraciones de seguridad del esquema", size=14, color=VERDE, bold=True)
    notas = ["El respaldo llega cifrado desde el teléfono (AES-256-GCM con clave derivada de una frase de respaldo del usuario): el servidor guarda un BLOB que no puede leer.",
             "Las contraseñas se guardan con BCrypt (hash_clave); ninguna tabla guarda claves en texto plano ni imágenes de facturas.",
             "Usuario de aplicación con privilegios mínimos (SELECT/INSERT/UPDATE); el DDL lo ejecuta solo Flyway con otro usuario.",
             "auditoria registra accesos y publicaciones sin datos personales (IP con hash). TDE/Always Encrypted en reposo; TLS en tránsito.",
             "sha256 permite verificar la integridad de cada respaldo y de cada versión del modelo antes de usarla."]
    for i, n in enumerate(notas):
        s.text(520, 756 + i * 24, "• " + n, size=12, color="#1a1a1a")
    guardar(s, "der_servidor")


# ======================================================================
def gantt():
    s = Uml(1760, 820)
    s.text(30, 38, "Cronograma actualizado del proyecto (ciclo 2026-03, 18 semanas) — corte: semana 8", size=20, color=AZUL, bold=True)
    x0, y0, cw, rh = 470, 70, 66, 34
    for k in range(18):
        x = x0 + k * cw
        hito = k + 1 in (4, 8, 12, 18)
        s.rect(x, y0, cw, 30, fill=ROJO if hito else AZUL, stroke="#fff", sw=1)
        s.text(x + cw / 2, y0 + 20, f"S{k + 1}", size=12, color="#fff", bold=True, anchor="middle")
    # estado: "ok" completado, "curso" en curso (se rellena hasta el corte), "pend" pendiente
    filas = [
        ("1. Gestión del proyecto", 1, 18, "curso", AZUL, True),
        ("   Acta, WBS, cronograma y riesgos", 1, 3, "ok", AZUL, False),
        ("   Seguimiento semanal y cierre", 4, 18, "curso", AZUL, False),
        ("2. Análisis", 1, 4, "ok", VERDE, True),
        ("   Contexto, canvas, entrevistas, SRS", 1, 4, "ok", VERDE, False),
        ("3. Diseño", 3, 8, "ok", NARANJA, True),
        ("   Procesos BPMN AS-IS / TO-BE", 5, 7, "ok", NARANJA, False),
        ("   Clases, DER y diseño físico de BD", 5, 8, "ok", NARANJA, False),
        ("   Prototipo UX/UI y reportes", 6, 8, "ok", NARANJA, False),
        ("   Documentación técnica (Markdown)", 7, 8, "ok", NARANJA, False),
        ("4. Modelo Gemma 4 ajustado", 5, 12, "curso", ROJO, True),
        ("   Recopilación y etiquetado", 5, 9, "curso", ROJO, False),
        ("   Ajuste fino, conversión y pruebas", 9, 12, "pend", ROJO, False),
        ("5. Construcción de la app", 7, 14, "curso", "#2f5597", True),
        ("   Front-end: pantallas y navegación", 7, 12, "curso", "#2f5597", False),
        ("   Back-end: dominio, Room y respaldo", 8, 14, "curso", "#2f5597", False),
        ("6. Pruebas y validación", 12, 16, "pend", VERDE, True),
        ("7. Documentación y sustentación", 4, 18, "curso", GRIS, True),
    ]
    textos = {"ok": "completado", "curso": "en curso", "pend": "pendiente"}
    for i, (n, a, b, est, col, bold) in enumerate(filas):
        y = y0 + 40 + i * rh
        if i % 2 == 0:
            s.rect(30, y - 4, x0 - 30 + 18 * cw, rh, fill="#f6f8fa", stroke="none")
        s.text(40, y + 17, n, size=13 if bold else 12, color="#1a1a1a", bold=bold)
        bx, bw = x0 + (a - 1) * cw + 4, (b - a + 1) * cw - 8
        s.rect(bx, y + 3, bw, 22, fill="#fff", stroke=col, sw=1.4, rx=5)
        if est == "ok":
            s.rect(bx, y + 3, bw, 22, fill=col, stroke=col, sw=1.4, rx=5)
        elif est == "curso":
            hasta = min(b, 8)
            s.rect(bx, y + 3, max(0, (hasta - a + 1) * cw - 8), 22, fill=col, stroke=col, sw=1.4, rx=5, opacity=0.45)
        fin = b < 17
        s.text(bx + bw + 6 if fin else bx + bw - 6, y + 19, textos[est], size=11, color=GRIS, anchor="start" if fin else "end")
    corte = x0 + 8 * cw
    ytot = y0 + 40 + len(filas) * rh
    s.line(corte, y0 + 32, corte, ytot, stroke=ROJO, sw=2.2, dash="7 4")
    s.rect(corte - 70, ytot + 6, 140, 24, fill=ROJO, stroke="none", rx=12)
    s.text(corte, ytot + 23, "Hoy: APF2 (sem. 8)", size=12, color="#fff", bold=True, anchor="middle")
    ly = ytot + 60
    s.rect(40, ly, 30, 14, fill=AZUL, stroke=AZUL, rx=3); s.text(78, ly + 12, "completado", size=12, color=GRIS)
    s.rect(190, ly, 30, 14, fill=AZUL, stroke=AZUL, rx=3, opacity=0.45); s.text(228, ly + 12, "en curso (hasta el corte)", size=12, color=GRIS)
    s.rect(420, ly, 30, 14, fill="#fff", stroke=AZUL, rx=3); s.text(458, ly + 12, "planificado", size=12, color=GRIS)
    s.rect(580, ly, 30, 14, fill=ROJO, stroke=ROJO, rx=3); s.text(618, ly + 12, "semanas de entrega: APF1 (4), APF2 (8), APF3 (12), final (18)", size=12, color=GRIS)
    guardar(s, "gantt_actualizado")


def generar():
    arquitectura(); despliegue(); clases_dominio(); clases_diseno(); componentes_java(); der_local(); der_servidor(); gantt()


if __name__ == "__main__":
    generar()
