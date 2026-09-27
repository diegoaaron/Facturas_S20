"""Prototipo de alta fidelidad de Facturas S20 (pantallas del teléfono, consola web y reporte PDF).

Cada pantalla es una función que dibuja sobre un lienzo de 360 × 760 px (tamaño lógico de un teléfono
Android). Se exportan una a una (pantalla_XX_*.svg/.png) y agrupadas en hojas de cuatro para el informe.
Los datos que aparecen son ilustrativos (caso simulado de la bodega «El Progreso»).
"""
import datetime
from svg import (Svg, guardar, envolver, esc, APP_PRIMARIO, APP_ACENTO, APP_ACENTO_OSC, APP_FONDO, APP_TEXTO,
                 APP_TEXTO2, APP_ALERTA, APP_ALERTA_CLARO, APP_ERROR, APP_ERROR_CLARO, APP_OK, APP_OK_CLARO, APP_BORDE,
                 AZUL, GRIS, GRIS_MEDIO, BORDE)

W, H = 360, 760
BLANCO = "#ffffff"

# ---------------- datos del caso simulado ----------------
RUC = "10456789124"
NEGOCIO = "Bodega «El Progreso»"
MES = "Septiembre 2026"
FACTURAS = [
    ("Distribuidora Andina S.A.C.", "20601234565", "F001-004821", "22/09/2026", "1 450,00", "IA"),
    ("Comercial Lima Norte S.A.C.", "20512345671", "F002-000915", "20/09/2026", "386,40", "IA"),
    ("Molinos del Sur S.A.", "20498765433", "E001-001284", "18/09/2026", "612,00", "IA"),
    ("Bebidas Rímac S.A.C.", "20610022333", "F003-007712", "15/09/2026", "540,10", "Manual"),
    ("Lácteos San Juan E.I.R.L.", "20455667781", "F001-002231", "12/09/2026", "298,00", "IA"),
]
VENCE = datetime.date(2026, 10, 15)
DIAS = ["lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"]


class Pantalla(Svg):
    """Lienzo de una pantalla (sin marco)."""

    def __init__(self, fondo=APP_FONDO):
        super().__init__(W, H, fondo=fondo)

    def contenido(self):
        return "".join(self.partes)

    # ---------- estructura ----------
    def barra_estado(self, oscuro=True):
        col = BLANCO if oscuro else APP_TEXTO
        self.text(18, 17, "9:41", size=12, color=col, bold=True)
        # señal, wifi y batería
        for i in range(4):
            self.rect(292 + i * 5, 13 - i * 2.5, 3, 4 + i * 2.5, fill=col, stroke="none")
        self.rect(318, 7, 22, 11, fill="none", stroke=col, sw=1.2, rx=2.5)
        self.rect(320, 9, 15, 7, fill=col, stroke="none", rx=1)
        self.rect(341, 10, 2, 5, fill=col, stroke="none")

    def appbar(self, titulo, atras=True, sub=None, acciones=None, color=APP_PRIMARIO):
        self.rect(0, 0, W, 84, fill=color, stroke="none")
        self.barra_estado()
        x = 18
        if atras:
            self.path("M28,44 L19,53 L28,62", stroke=BLANCO, sw=2.4)
            x = 46
        self.text(x, 58 if not sub else 52, titulo, size=18, color=BLANCO, bold=True)
        if sub:
            self.text(x, 71, sub, size=11.5, color="#cfe3dd")
        for i, ic in enumerate(acciones or []):
            self.icono(ic, 322 - i * 36, 52, BLANCO, 20)

    def nav(self, activo=0):
        y = 700
        self.rect(0, y, W, 60, fill=BLANCO, stroke="none")
        self.line(0, y, W, y, stroke=APP_BORDE, sw=1)
        items = [("inicio", "Inicio"), ("lista", "Facturas"), (None, ""), ("grafico", "Mes"), ("ajustes", "Ajustes")]
        for i, (ic, t) in enumerate(items):
            cx = 36 + i * 72
            if ic is None:
                self.circle(cx, y + 6, 30, fill=APP_ACENTO, stroke=BLANCO, sw=4)
                self.icono("camara", cx, y + 6, BLANCO, 24)
                self.text(cx, y + 52, "Escanear", size=10.5, color=APP_PRIMARIO, bold=True, anchor="middle")
                continue
            col = APP_PRIMARIO if i == activo else "#8a9a96"
            self.icono(ic, cx, y + 22, col, 20)
            self.text(cx, y + 48, t, size=10.5, color=col, bold=(i == activo), anchor="middle")

    # ---------- componentes ----------
    def tarjeta(self, x, y, w, h, fill=BLANCO, stroke=APP_BORDE, rx=14):
        self.rect(x, y, w, h, fill=fill, stroke=stroke, sw=1, rx=rx)

    def boton(self, x, y, w, texto, tipo="primario", h=48, icono=None):
        estilos = {"primario": (APP_PRIMARIO, BLANCO, APP_PRIMARIO), "acento": (APP_ACENTO, BLANCO, APP_ACENTO),
                   "contorno": (BLANCO, APP_PRIMARIO, APP_PRIMARIO), "peligro": (BLANCO, APP_ERROR, APP_ERROR),
                   "texto": ("none", APP_PRIMARIO, "none"), "deshabilitado": ("#e3e8e6", "#9aa8a4", "#e3e8e6")}
        f, c, b = estilos[tipo]
        self.rect(x, y, w, h, fill=f, stroke=b, sw=1.5, rx=h / 2)
        tx = x + w / 2
        if icono:
            self.icono(icono, tx - len(texto) * 4.2 - 14, y + h / 2, c, 18)
            tx += 10
        self.text(tx, y + h / 2 + 5.5, texto, size=15, color=c, bold=True, anchor="middle")

    def campo(self, x, y, w, etiqueta, valor, estado="normal", ayuda=None, h=58):
        bordes = {"normal": (APP_BORDE, BLANCO), "ok": (APP_OK, BLANCO), "alerta": (APP_ALERTA, APP_ALERTA_CLARO),
                  "error": (APP_ERROR, APP_ERROR_CLARO), "foco": (APP_PRIMARIO, BLANCO), "lectura": (APP_BORDE, "#f7f9f8")}
        b, f = bordes[estado]
        self.rect(x, y, w, h, fill=f, stroke=b, sw=2 if estado != "normal" and estado != "lectura" else 1.2, rx=10)
        self.text(x + 14, y + 20, etiqueta, size=11.5, color=APP_TEXTO2)
        self.text(x + 14, y + 43, valor, size=16, color=APP_TEXTO, bold=True)
        if estado == "ok":
            self.icono("check", x + w - 22, y + h / 2, APP_OK, 18)
        elif estado == "alerta":
            self.icono("alerta", x + w - 22, y + h / 2, APP_ALERTA, 18)
        elif estado == "error":
            self.icono("alerta", x + w - 22, y + h / 2, APP_ERROR, 18)
        if ayuda:
            col = {"ok": APP_OK, "alerta": "#9a6a00", "error": APP_ERROR}.get(estado, APP_TEXTO2)
            self.text(x + 4, y + h + 15, ayuda, size=11, color=col, bold=estado in ("alerta", "error"))

    def insignia(self, x, y, texto, fill=APP_OK_CLARO, color=APP_OK, anchor="start"):
        w = len(texto) * 6.3 + 16
        if anchor == "end":
            x -= w
        self.rect(x, y, w, 20, fill=fill, stroke="none", rx=10)
        self.text(x + w / 2, y + 14, texto, size=10.5, color=color, bold=True, anchor="middle")
        return w

    def barra(self, x, y, w, frac, color=APP_ACENTO, h=10, marca=None):
        self.rect(x, y, w, h, fill="#e6ecea", stroke="none", rx=h / 2)
        self.rect(x, y, max(h, w * frac), h, fill=color, stroke="none", rx=h / 2)
        if marca:
            mx = x + w * marca
            self.line(mx, y - 4, mx, y + h + 4, stroke=APP_TEXTO, sw=1.5)

    def interruptor(self, x, y, on=True):
        self.rect(x, y, 40, 22, fill=APP_ACENTO if on else "#cfd8d5", stroke="none", rx=11)
        self.circle(x + (29 if on else 11), y + 11, 8.5, fill=BLANCO, stroke="none")

    def texto(self, x, y, t, size=13, color=APP_TEXTO, bold=False, anchor="start", ancho=None, lh=None):
        if ancho:
            ls = envolver(t, ancho)
            self.lines(x, y, ls, size=size, color=color, bold=bold, anchor=anchor, lh=lh or size * 1.4)
            return len(ls)
        self.text(x, y, t, size=size, color=color, bold=bold, anchor=anchor)
        return 1

    def logo(self, cx, cy, r=40):
        self.circle(cx, cy, r, fill=APP_PRIMARIO, stroke="none")
        self.rect(cx - r * 0.42, cy - r * 0.5, r * 0.8, r * 0.95, fill=BLANCO, stroke="none", rx=4)
        for k in range(3):
            self.rect(cx - r * 0.3, cy - r * 0.32 + k * r * 0.2, r * 0.5 - (k == 2) * r * 0.2, r * 0.07, fill=APP_PRIMARIO, stroke="none")
        self.circle(cx + r * 0.42, cy + r * 0.42, r * 0.3, fill=APP_ACENTO, stroke=BLANCO, sw=3)

    def factura_mini(self, x, y, w, h, datos=None, inclinada=False):
        """Dibujo esquemático de una factura en papel."""
        g = f' transform="rotate(-3 {x + w / 2} {y + h / 2})"' if inclinada else ""
        d = datos or FACTURAS[0]
        partes = [f'<g{g}>',
                  f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="#fffdf8" stroke="#c9c2b3" stroke-width="1.2"/>',
                  f'<text x="{x + 10}" y="{y + 18}" font-size="{w / 17:.1f}" font-weight="bold" fill="#333">{esc(d[0].upper())}</text>',
                  f'<text x="{x + 10}" y="{y + 32}" font-size="{w / 22:.1f}" fill="#555">RUC {d[1]}</text>',
                  f'<rect x="{x + w - w * 0.5}" y="{y + 40}" width="{w * 0.46}" height="{h * 0.16}" fill="none" stroke="#777"/>',
                  f'<text x="{x + w - w * 0.27}" y="{y + 40 + h * 0.07}" font-size="{w / 26:.1f}" fill="#333" text-anchor="middle">FACTURA ELECTRÓNICA</text>',
                  f'<text x="{x + w - w * 0.27}" y="{y + 40 + h * 0.13}" font-size="{w / 20:.1f}" fill="#333" font-weight="bold" text-anchor="middle">{d[2]}</text>',
                  f'<text x="{x + 10}" y="{y + 58}" font-size="{w / 22:.1f}" fill="#555">Fecha: {d[3]}</text>']
        for k in range(6):
            yy = y + h * 0.42 + k * h * 0.07
            partes.append(f'<line x1="{x + 10}" y1="{yy}" x2="{x + w - 60}" y2="{yy}" stroke="#d6d0c4" stroke-width="3"/>')
            partes.append(f'<line x1="{x + w - 50}" y1="{yy}" x2="{x + w - 12}" y2="{yy}" stroke="#d6d0c4" stroke-width="3"/>')
        partes.append(f'<text x="{x + w - 12}" y="{y + h - 14}" font-size="{w / 16:.1f}" fill="#222" font-weight="bold" text-anchor="end">TOTAL S/ {d[4]}</text>')
        partes.append('</g>')
        self.add("".join(partes))

    # ---------- íconos (trazos simples) ----------
    def icono(self, nombre, cx, cy, color, s=20):
        k = s / 20
        sw = 1.9 * max(k, 0.8)

        def P(d):
            self.path(d, stroke=color, sw=sw, fill="none")

        x, y = cx - 10 * k, cy - 10 * k
        if nombre == "camara":
            self.rect(x + 1 * k, y + 5 * k, 18 * k, 13 * k, fill="none", stroke=color, sw=sw, rx=3 * k)
            self.circle(cx, cy + 1.5 * k, 4 * k, fill="none", stroke=color, sw=sw)
            P(f"M{x + 6 * k},{y + 5 * k} l2,-3 h{4 * k} l2,3")
        elif nombre == "inicio":
            P(f"M{x + 2 * k},{y + 10 * k} L{cx},{y + 2 * k} L{x + 18 * k},{y + 10 * k} M{x + 4 * k},{y + 8 * k} V{y + 18 * k} H{x + 16 * k} V{y + 8 * k}")
        elif nombre == "lista":
            for i in range(3):
                P(f"M{x + 6 * k},{y + (4 + i * 6) * k} H{x + 18 * k}")
                self.circle(x + 2.5 * k, y + (4 + i * 6) * k, 1.4 * k, fill=color, stroke="none")
        elif nombre == "grafico":
            for i, hh in enumerate((6, 11, 16)):
                self.rect(x + (2 + i * 6) * k, y + (18 - hh) * k, 4 * k, hh * k, fill="none", stroke=color, sw=sw, rx=1)
        elif nombre == "ajustes":
            self.circle(cx, cy, 3.5 * k, fill="none", stroke=color, sw=sw)
            self.circle(cx, cy, 8 * k, fill="none", stroke=color, sw=sw, dash=f"{3 * k} {2.2 * k}")
        elif nombre == "check":
            P(f"M{x + 3 * k},{y + 10 * k} L{x + 8 * k},{y + 15 * k} L{x + 17 * k},{y + 5 * k}")
        elif nombre == "check_circulo":
            self.circle(cx, cy, 9 * k, fill="none", stroke=color, sw=sw)
            P(f"M{x + 5.5 * k},{y + 10 * k} L{x + 8.5 * k},{y + 13 * k} L{x + 14.5 * k},{y + 7 * k}")
        elif nombre == "alerta":
            self.path(f"M{cx},{y + 1.5 * k} L{x + 19 * k},{y + 18 * k} H{x + 1 * k} Z", stroke=color, sw=sw, fill="none")
            P(f"M{cx},{y + 7.5 * k} V{y + 12 * k}")
            self.circle(cx, y + 15 * k, 1.2 * k, fill=color, stroke="none")
        elif nombre == "candado":
            self.rect(x + 3 * k, y + 9 * k, 14 * k, 10 * k, fill="none", stroke=color, sw=sw, rx=2 * k)
            P(f"M{x + 6 * k},{y + 9 * k} V{y + 6 * k} a{4 * k},{4 * k} 0 0 1 {8 * k},0 V{y + 9 * k}")
        elif nombre == "huella":
            for r in (3, 6, 9):
                self.path(f"M{cx - r * k},{cy + 4 * k} a{r * k},{r * k} 0 1 1 {2 * r * k},0", stroke=color, sw=sw)
        elif nombre == "doc":
            P(f"M{x + 4 * k},{y + 1 * k} H{x + 12 * k} L{x + 17 * k},{y + 6 * k} V{y + 19 * k} H{x + 4 * k} Z M{x + 7 * k},{y + 10 * k} H{x + 14 * k} M{x + 7 * k},{y + 14 * k} H{x + 14 * k}")
        elif nombre == "compartir":
            for px, py in ((15, 4), (5, 10), (15, 16)):
                self.circle(x + px * k, y + py * k, 2.6 * k, fill="none", stroke=color, sw=sw)
            P(f"M{x + 7.3 * k},{y + 8.8 * k} L{x + 12.7 * k},{y + 5.2 * k} M{x + 7.3 * k},{y + 11.2 * k} L{x + 12.7 * k},{y + 14.8 * k}")
        elif nombre == "campana":
            P(f"M{x + 4 * k},{y + 15 * k} V{y + 9 * k} a{6 * k},{6 * k} 0 0 1 {12 * k},0 V{y + 15 * k} Z M{x + 8 * k},{y + 18 * k} H{x + 12 * k}")
        elif nombre == "calendario":
            self.rect(x + 2 * k, y + 4 * k, 16 * k, 14 * k, fill="none", stroke=color, sw=sw, rx=2 * k)
            P(f"M{x + 2 * k},{y + 8.5 * k} H{x + 18 * k} M{x + 6 * k},{y + 2 * k} V{y + 5 * k} M{x + 14 * k},{y + 2 * k} V{y + 5 * k}")
        elif nombre == "nube":
            P(f"M{x + 5 * k},{y + 16 * k} a{4 * k},{4 * k} 0 0 1 0,-8 a{5 * k},{5 * k} 0 0 1 {9.5 * k},-1 a{3.6 * k},{3.6 * k} 0 0 1 {1 * k},9 Z")
        elif nombre == "chip":
            self.rect(x + 4 * k, y + 4 * k, 12 * k, 12 * k, fill="none", stroke=color, sw=sw, rx=2 * k)
            for t in (7, 10, 13):
                P(f"M{x + t * k},{y + 1 * k} V{y + 4 * k} M{x + t * k},{y + 16 * k} V{y + 19 * k} M{x + 1 * k},{y + t * k} H{x + 4 * k} M{x + 16 * k},{y + t * k} H{x + 19 * k}")
        elif nombre == "sinred":
            P(f"M{x + 2 * k},{y + 8 * k} a{11 * k},{11 * k} 0 0 1 {16 * k},0 M{x + 5 * k},{y + 11.5 * k} a{7 * k},{7 * k} 0 0 1 {10 * k},0")
            self.circle(cx, y + 15.5 * k, 1.6 * k, fill=color, stroke="none")
            P(f"M{x + 2 * k},{y + 2 * k} L{x + 18 * k},{y + 18 * k}")
        elif nombre == "galeria":
            self.rect(x + 1 * k, y + 3 * k, 18 * k, 14 * k, fill="none", stroke=color, sw=sw, rx=2 * k)
            P(f"M{x + 3 * k},{y + 15 * k} L{x + 8 * k},{y + 9 * k} L{x + 12 * k},{y + 13 * k} L{x + 14 * k},{y + 11 * k} L{x + 17 * k},{y + 15 * k}")
            self.circle(x + 13.5 * k, y + 7 * k, 1.6 * k, fill=color, stroke="none")
        elif nombre == "rayo":
            self.path(f"M{x + 11 * k},{y + 1 * k} L{x + 4 * k},{y + 11 * k} H{x + 10 * k} L{x + 9 * k},{y + 19 * k} L{x + 16 * k},{y + 8 * k} H{x + 10 * k} Z",
                      stroke=color, sw=sw)
        elif nombre == "buscar":
            self.circle(x + 8.5 * k, y + 8.5 * k, 6 * k, fill="none", stroke=color, sw=sw)
            P(f"M{x + 13 * k},{y + 13 * k} L{x + 18 * k},{y + 18 * k}")
        elif nombre == "lapiz":
            P(f"M{x + 3 * k},{y + 17 * k} L{x + 4 * k},{y + 13 * k} L{x + 14 * k},{y + 3 * k} L{x + 17 * k},{y + 6 * k} L{x + 7 * k},{y + 16 * k} Z")
        elif nombre == "descarga":
            P(f"M{cx},{y + 2 * k} V{y + 13 * k} M{x + 5 * k},{y + 8 * k} L{cx},{y + 13 * k} L{x + 15 * k},{y + 8 * k} M{x + 3 * k},{y + 18 * k} H{x + 17 * k}")
        elif nombre == "zoom":
            self.circle(x + 8.5 * k, y + 8.5 * k, 6 * k, fill="none", stroke=color, sw=sw)
            P(f"M{x + 13 * k},{y + 13 * k} L{x + 18 * k},{y + 18 * k} M{x + 6 * k},{y + 8.5 * k} H{x + 11 * k} M{x + 8.5 * k},{y + 6 * k} V{y + 11 * k}")
        elif nombre == "papelera":
            P(f"M{x + 3 * k},{y + 5 * k} H{x + 17 * k} M{x + 5 * k},{y + 5 * k} L{x + 6 * k},{y + 18 * k} H{x + 14 * k} L{x + 15 * k},{y + 5 * k} M{x + 8 * k},{y + 5 * k} V{y + 2.5 * k} H{x + 12 * k} V{y + 5 * k}")
        elif nombre == "cerrar":
            P(f"M{x + 4 * k},{y + 4 * k} L{x + 16 * k},{y + 16 * k} M{x + 16 * k},{y + 4 * k} L{x + 4 * k},{y + 16 * k}")
        elif nombre == "flecha_der":
            P(f"M{x + 7 * k},{y + 4 * k} L{x + 13 * k},{y + 10 * k} L{x + 7 * k},{y + 16 * k}")
        elif nombre == "reloj":
            self.circle(cx, cy, 8.5 * k, fill="none", stroke=color, sw=sw)
            P(f"M{cx},{cy - 5 * k} V{cy} L{cx + 4 * k},{cy + 2.5 * k}")


# ======================================================================
#  PANTALLAS
# ======================================================================
def p01_configuracion():
    p = Pantalla(BLANCO)
    p.barra_estado(oscuro=False)
    p.logo(180, 110, 38)
    p.text(180, 185, "Configure su negocio", size=21, color=APP_PRIMARIO, bold=True, anchor="middle")
    p.text(180, 208, "Paso 1 de 3 · solo la primera vez", size=12.5, color=APP_TEXTO2, anchor="middle")
    for i in range(3):
        p.rect(128 + i * 36, 220, 28, 5, fill=APP_ACENTO if i == 0 else "#dfe7e4", stroke="none", rx=2.5)
    p.campo(24, 250, 312, "RUC del negocio", RUC, "ok", "RUC válido (11 dígitos y dígito verificador)")
    p.campo(24, 342, 312, "Nombre del negocio", "Bodega El Progreso", "foco")
    p.campo(24, 420, 312, "Nombre del titular", "Rosa Huamán Quispe")
    p.tarjeta(24, 500, 312, 92, fill="#f3f8f6", stroke="#d7e6e0")
    p.icono("candado", 50, 530, APP_PRIMARIO, 22)
    p.texto(72, 526, "Sus datos se guardan cifrados en este", size=12.5)
    p.texto(72, 544, "teléfono. No necesita crear una cuenta", size=12.5)
    p.texto(72, 562, "ni tener internet para usar la app.", size=12.5)
    p.boton(24, 620, 312, "Continuar")
    p.text(180, 700, "Al continuar acepta el tratamiento local de sus datos", size=11, color=APP_TEXTO2, anchor="middle")
    return p


def p02_acceso():
    p = Pantalla(APP_PRIMARIO)
    p.barra_estado()
    p.logo(180, 120, 40)
    p.circle(180, 120, 40, fill="none", stroke=BLANCO, sw=2)
    p.text(180, 200, "Hola, Bodega El Progreso", size=19, color=BLANCO, bold=True, anchor="middle")
    p.text(180, 224, "Ingrese su PIN de 4 dígitos", size=13, color="#cfe3dd", anchor="middle")
    for i in range(4):
        p.circle(132 + i * 32, 262, 8, fill=BLANCO if i < 2 else "none", stroke=BLANCO, sw=2)
    teclas = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫"]
    for i, t in enumerate(teclas):
        cx, cy = 90 + (i % 3) * 90, 330 + (i // 3) * 76
        if t:
            p.circle(cx, cy, 30, fill="#24615a", stroke="none")
            p.text(cx, cy + 9, t, size=24, color=BLANCO, anchor="middle")
    p.rect(60, 646, 240, 50, fill="none", stroke=APP_ACENTO, sw=1.8, rx=25)
    p.icono("huella", 110, 671, APP_ACENTO, 22)
    p.text(192, 677, "Ingresar con huella", size=15, color=APP_ACENTO, bold=True, anchor="middle")
    p.text(180, 730, "¿Olvidó su PIN?  Restablecer con su RUC", size=12, color="#cfe3dd", anchor="middle")
    return p


def p03_modelo():
    p = Pantalla()
    p.appbar("Lectura sin internet", atras=False, sub="Preparación única del lector de facturas")
    p.tarjeta(16, 100, 328, 214)
    p.text(32, 128, "Comprobación del equipo", size=15, color=APP_TEXTO, bold=True)
    filas = [("Memoria del teléfono", "6 GB", True), ("Espacio libre", "5,1 GB de 2,6 GB necesarios", True),
             ("Conexión", "Wi-Fi", True), ("Versión de Android", "13", True)]
    for i, (a, b, ok) in enumerate(filas):
        yy = 160 + i * 38
        p.icono("check_circulo", 42, yy - 5, APP_OK, 20)
        p.text(62, yy, a, size=13, color=APP_TEXTO)
        p.text(330, yy, b, size=12.5, color=APP_TEXTO2, anchor="end")
    p.tarjeta(16, 328, 328, 214)
    p.icono("chip", 44, 360, APP_PRIMARIO, 24)
    p.text(66, 358, "Gemma 4 E2B · facturas v1.3", size=15, color=APP_TEXTO, bold=True)
    p.text(66, 377, "Modelo de IA que lee sus facturas en el teléfono", size=11.5, color=APP_TEXTO2)
    p.text(32, 420, "Descargando…", size=13, color=APP_TEXTO, bold=True)
    p.text(328, 420, "64 %", size=13, color=APP_PRIMARIO, bold=True, anchor="end")
    p.barra(32, 432, 296, 0.64, color=APP_ACENTO, h=12)
    p.text(32, 466, "1,7 GB de 2,6 GB · faltan ≈ 6 min", size=12, color=APP_TEXTO2)
    p.icono("candado", 42, 502, APP_OK, 16)
    p.texto(58, 499, "Al terminar se verifica la huella SHA-256", size=11.5, color=APP_OK)
    p.texto(58, 515, "del archivo antes de activarlo.", size=11.5, color=APP_OK)
    p.boton(16, 566, 328, "Pausar descarga", "contorno")
    p.boton(16, 626, 328, "Registrar a mano mientras tanto", "texto")
    p.text(180, 712, "La descarga se reanuda si se corta la conexión", size=11, color=APP_TEXTO2, anchor="middle")
    return p


def p04_inicio():
    p = Pantalla()
    p.rect(0, 0, W, 190, fill=APP_PRIMARIO, stroke="none")
    p.barra_estado()
    p.text(18, 56, "Hola, Rosa", size=14, color="#cfe3dd")
    p.text(18, 80, MES, size=21, color=BLANCO, bold=True)
    p.icono("campana", 318, 64, BLANCO, 22)
    p.circle(328, 55, 5, fill=APP_ALERTA, stroke="none")
    p.tarjeta(16, 100, 328, 170)
    p.text(32, 128, "Compras del mes", size=13, color=APP_TEXTO2)
    p.text(32, 164, "S/ 4 120,50", size=30, color=APP_PRIMARIO, bold=True)
    p.insignia(328, 114, "12 facturas", anchor="end")
    p.barra(32, 188, 296, 0.824, color=APP_ALERTA, h=12, marca=0.8)
    p.text(32, 222, "82 % del límite de la categoría 1 (S/ 5 000)", size=12, color="#9a6a00", bold=True)
    p.text(32, 244, "Le quedan S/ 879,50 antes de pasar a la categoría 2", size=11.5, color=APP_TEXTO2)
    p.tarjeta(16, 282, 158, 96)
    p.text(30, 306, "Ventas del mes", size=12, color=APP_TEXTO2)
    p.text(30, 336, "S/ 3 900,00", size=17, color=APP_TEXTO, bold=True)
    p.icono("lapiz", 30 + 6, 360, APP_PRIMARIO, 14)
    p.text(46, 364, "Editar", size=12, color=APP_PRIMARIO, bold=True)
    p.tarjeta(186, 282, 158, 96, fill="#eef6e4", stroke="#cfe3b3")
    p.text(200, 306, "Le corresponde", size=12, color=APP_TEXTO2)
    p.text(200, 336, "Categoría 1", size=17, color=APP_PRIMARIO, bold=True)
    p.text(200, 362, "Cuota S/ 20", size=13, color=APP_ACENTO_OSC, bold=True)
    p.tarjeta(16, 390, 328, 64, fill="#fff8e8", stroke="#f3dca6")
    p.icono("calendario", 42, 422, "#9a6a00", 22)
    p.text(66, 416, f"Declare hasta el {DIAS[VENCE.weekday()]} 15 de octubre", size=13.5, color=APP_TEXTO, bold=True)
    p.text(66, 436, "Faltan 18 días · recordatorio activado", size=12, color=APP_TEXTO2)
    p.text(16, 482, "Últimas facturas", size=14, color=APP_TEXTO, bold=True)
    p.text(344, 482, "Ver todas", size=12.5, color=APP_PRIMARIO, bold=True, anchor="end")
    for i, f in enumerate(FACTURAS[:3]):
        yy = 496 + i * 66
        p.tarjeta(16, yy, 328, 58, rx=12)
        p.circle(44, yy + 29, 16, fill="#e3efe9", stroke="none")
        p.text(44, yy + 34, f[0][0], size=14, color=APP_PRIMARIO, bold=True, anchor="middle")
        p.text(70, yy + 24, f[0], size=13, color=APP_TEXTO, bold=True)
        p.text(70, yy + 42, f"{f[2]} · {f[3][:5]}", size=11.5, color=APP_TEXTO2)
        p.text(332, yy + 34, f"S/ {f[4]}", size=13.5, color=APP_TEXTO, bold=True, anchor="end")
    p.nav(0)
    return p


def p05_guia():
    p = Pantalla(BLANCO)
    p.appbar("Registrar factura", atras=True)
    p.rect(0, 84, W, 220, fill="#eef6e4", stroke="none")
    p.factura_mini(95, 104, 170, 180, inclinada=True)
    p.circle(290, 120, 22, fill=APP_ACENTO, stroke=BLANCO, sw=3)
    p.icono("camara", 290, 120, BLANCO, 20)
    p.text(24, 342, "¡Hora de la foto!", size=22, color=APP_PRIMARIO, bold=True)
    tips = [("rayo", "Busque buena luz y evite sombras sobre el papel."), ("doc", "Ponga la factura sobre una superficie plana."),
            ("zoom", "Acérquese hasta que el total se lea con claridad."), ("sinred", "No necesita internet: la lectura se hace aquí.")]
    for i, (ic, t) in enumerate(tips):
        yy = 378 + i * 58
        p.circle(42, yy + 8, 17, fill="#eef6e4", stroke="none")
        p.icono(ic, 42, yy + 8, APP_PRIMARIO, 18)
        p.texto(70, yy + 4, t, size=13.5, ancho=34, lh=18)
    p.boton(24, 620, 312, "Abrir cámara", "acento", icono="camara")
    p.boton(24, 680, 312, "Elegir de la galería", "texto")
    return p


def p06_camara():
    p = Pantalla("#1e2422")
    p.barra_estado()
    p.icono("cerrar", 30, 56, BLANCO, 20)
    p.text(180, 62, "Encuadre la factura", size=16, color=BLANCO, bold=True, anchor="middle")
    p.icono("rayo", 330, 56, BLANCO, 20)
    p.rect(0, 84, W, 520, fill="#3a3530", stroke="none")
    p.rect(0, 84, W, 520, fill="#5b534a", stroke="none", opacity=0.5)
    p.factura_mini(62, 128, 236, 420)
    p.rect(50, 116, 260, 444, fill="none", stroke=APP_ACENTO, sw=3, dash="18 10")
    for cx, cy, dx, dy in ((50, 116, 1, 1), (310, 116, -1, 1), (50, 560, 1, -1), (310, 560, -1, -1)):
        p.path(f"M{cx},{cy + dy * 30} V{cy} H{cx + dx * 30}", stroke=APP_ACENTO, sw=6)
    p.rect(98, 574, 164, 26, fill=APP_OK, stroke="none", rx=13)
    p.text(180, 591, "Nitidez: buena · Luz: buena", size=12, color=BLANCO, bold=True, anchor="middle")
    p.rect(0, 604, W, 156, fill="#1e2422", stroke="none")
    p.text(180, 630, "Acérquese un poco más hasta que se lea el total", size=12, color="#cfd8d5", anchor="middle")
    p.circle(180, 690, 36, fill=BLANCO, stroke=APP_ACENTO, sw=6)
    p.rect(52, 670, 40, 40, fill="#3a3a3a", stroke=BLANCO, sw=1.5, rx=8)
    p.icono("galeria", 72, 690, BLANCO, 20)
    p.text(72, 730, "Galería", size=11, color=BLANCO, anchor="middle")
    return p


def p07_leyendo():
    p = Pantalla(BLANCO)
    p.appbar("Leyendo factura", atras=True)
    cx, cy, r = 180, 220, 82
    p.circle(cx, cy, r, fill="none", stroke="#e6ecea", sw=18)
    import math
    ang = 0.71 * 2 * math.pi
    x2, y2 = cx + r * math.sin(ang), cy - r * math.cos(ang)
    p.path(f"M{cx},{cy - r} A{r},{r} 0 1 1 {x2:.1f},{y2:.1f}", stroke=APP_PRIMARIO, sw=18)
    p.text(cx, cy + 4, "71 %", size=34, color=APP_PRIMARIO, bold=True, anchor="middle")
    p.text(cx, cy + 30, "≈ 4 s restantes", size=12.5, color=APP_TEXTO2, anchor="middle")
    campos = [("Proveedor y RUC", True), ("Serie y número", True), ("Fecha de emisión", True), ("Moneda", True),
              ("Importe total", False)]
    for i, (c, ok) in enumerate(campos):
        yy = 356 + i * 44
        if ok:
            p.icono("check_circulo", 60, yy - 5, APP_OK, 22)
        else:
            p.circle(60, yy - 5, 10, fill="none", stroke="#c3cfcb", sw=2, dash="3 3")
        p.text(84, yy, c, size=15, color=APP_TEXTO if ok else APP_TEXTO2, bold=ok)
    p.tarjeta(24, 590, 312, 70, fill="#eef6e4", stroke="#cfe3b3")
    p.icono("chip", 52, 625, APP_PRIMARIO, 24)
    p.text(76, 618, "IA en el teléfono · sin conexión", size=13.5, color=APP_PRIMARIO, bold=True)
    p.text(76, 638, "Gemma 4 E2B facturas v1.3 · la foto no sale", size=11.5, color=APP_TEXTO2)
    p.boton(24, 684, 312, "Cancelar", "texto")
    return p


def _encabezado_verificacion(p):
    p.appbar("Verifique los datos", atras=True, sub="Toque un campo para corregirlo")
    p.rect(16, 96, 328, 118, fill="#3a3530", stroke="none", rx=12)
    p.add('<clipPath id="cf"><rect x="16" y="96" width="328" height="118" rx="12"/></clipPath>')
    p.add('<g clip-path="url(#cf)">')
    p.factura_mini(40, 104, 280, 300)
    p.add('</g>')
    p.rect(300, 176, 32, 30, fill="#000", stroke="none", rx=8, opacity=0.55)
    p.icono("zoom", 316, 191, BLANCO, 18)


def p08_verificar():
    p = Pantalla()
    _encabezado_verificacion(p)
    y = 228
    p.campo(16, y, 328, "Proveedor", "Distribuidora Andina S.A.C.", "ok")
    p.campo(16, y + 68, 328, "RUC del proveedor", "20601234565", "ok")
    p.campo(16, y + 136, 158, "Serie y número", "F001-004821", "ok")
    p.campo(186, y + 136, 158, "Fecha de emisión", "22/09/2026", "ok")
    p.campo(16, y + 204, 100, "Moneda", "PEN", "ok")
    p.campo(128, y + 204, 216, "Importe total", "S/ 1 450,00", "alerta")
    p.rect(128, y + 270, 216, 22, fill="none", stroke="none")
    p.texto(130, y + 280, "Confianza baja (62 %): compárelo con", size=11, color="#9a6a00", bold=True)
    p.texto(130, y + 294, "el total impreso en la factura.", size=11, color="#9a6a00", bold=True)
    p.insignia(16, y + 274, "7 campos leídos", fill="#e3efe9", color=APP_PRIMARIO)
    p.boton(16, 614, 328, "Guardar factura")
    p.boton(16, 672, 328, "Tomar otra foto", "texto")
    return p


def p09_duplicada():
    p = p08_verificar()
    p.rect(0, 0, W, H, fill="#000", stroke="none", opacity=0.55)
    p.tarjeta(24, 210, 312, 330, rx=20, stroke="none")
    p.circle(180, 262, 30, fill=APP_ALERTA_CLARO, stroke="none")
    p.icono("alerta", 180, 262, APP_ALERTA, 30)
    p.text(180, 322, "Esta factura ya está", size=18, color=APP_TEXTO, bold=True, anchor="middle")
    p.text(180, 346, "registrada", size=18, color=APP_TEXTO, bold=True, anchor="middle")
    p.tarjeta(44, 366, 272, 70, fill="#f7f9f8")
    p.text(58, 390, "F001-004821 · Distribuidora Andina", size=12.5, color=APP_TEXTO, bold=True)
    p.text(58, 410, "Guardada el 22/09/2026 · S/ 1 450,00", size=12, color=APP_TEXTO2)
    p.text(58, 426, "Se detectó por RUC + serie + número", size=11, color=APP_TEXTO2)
    p.boton(44, 452, 272, "Ver la registrada", h=44)
    p.boton(44, 500, 272, "Descartar esta foto", "texto", h=34)
    return p


def p10_guardada():
    p = Pantalla(BLANCO)
    p.barra_estado(oscuro=False)
    p.circle(180, 170, 64, fill=APP_OK_CLARO, stroke="none")
    p.circle(180, 170, 44, fill=APP_OK, stroke="none")
    p.icono("check", 180, 170, BLANCO, 44)
    p.text(180, 276, "Factura guardada", size=24, color=APP_PRIMARIO, bold=True, anchor="middle")
    p.text(180, 302, "Distribuidora Andina · F001-004821", size=13, color=APP_TEXTO2, anchor="middle")
    p.text(180, 348, "+ S/ 1 450,00", size=30, color=APP_ACENTO_OSC, bold=True, anchor="middle")
    p.tarjeta(24, 380, 312, 150)
    p.text(40, 410, "Compras de septiembre", size=13, color=APP_TEXTO2)
    p.text(40, 442, "S/ 4 120,50", size=24, color=APP_TEXTO, bold=True)
    p.barra(40, 462, 280, 0.824, color=APP_ALERTA, h=12, marca=0.8)
    p.icono("alerta", 50, 504, APP_ALERTA, 18)
    p.text(68, 509, "Superó el 80 % del límite de la categoría 1", size=12, color="#9a6a00", bold=True)
    p.boton(24, 580, 312, "Escanear otra factura", "acento", icono="camara")
    p.boton(24, 640, 312, "Ir al inicio", "contorno")
    return p


def p11_lista():
    p = Pantalla()
    p.appbar("Facturas de septiembre", atras=False, acciones=["buscar"])
    chips = [("Todas (12)", True), ("Leídas por IA", False), ("Manuales", False), ("Anuladas", False)]
    x = 16
    for t, on in chips:
        w = len(t) * 6.6 + 22
        p.rect(x, 96, w, 30, fill=APP_PRIMARIO if on else BLANCO, stroke=APP_PRIMARIO if on else APP_BORDE, sw=1.2, rx=15)
        p.text(x + w / 2, 115, t, size=11.5, color=BLANCO if on else APP_TEXTO, bold=on, anchor="middle")
        x += w + 8
    p.text(16, 152, "Semana del 21 al 27", size=12, color=APP_TEXTO2, bold=True)
    orden = FACTURAS + [("Distribuidora Andina S.A.C.", "20601234565", "F001-004655", "08/09/2026", "214,00", "IA")]
    for i, f in enumerate(orden):
        yy = 162 + i * 76 + (18 if i >= 2 else 0)
        if i == 2:
            p.text(16, yy - 6, "Semana del 14 al 20", size=12, color=APP_TEXTO2, bold=True)
        p.tarjeta(16, yy, 328, 68, rx=12)
        p.circle(44, yy + 34, 17, fill="#e3efe9", stroke="none")
        p.text(44, yy + 39, f[0][0], size=14, color=APP_PRIMARIO, bold=True, anchor="middle")
        p.text(72, yy + 26, f[0], size=13, color=APP_TEXTO, bold=True)
        p.text(72, yy + 45, f"{f[2]} · {f[3]}", size=11.5, color=APP_TEXTO2)
        p.text(332, yy + 28, f"S/ {f[4]}", size=13.5, color=APP_TEXTO, bold=True, anchor="end")
        if f[5] == "Manual":
            p.insignia(332, yy + 38, "manual", fill="#eef1f0", color=APP_TEXTO2, anchor="end")
        else:
            p.insignia(332, yy + 38, "IA", fill="#e3efe9", color=APP_PRIMARIO, anchor="end")
    p.rect(0, 652, W, 48, fill="#eef6e4", stroke="none")
    p.text(16, 682, "Total del mes (12 facturas)", size=13, color=APP_TEXTO)
    p.text(344, 682, "S/ 4 120,50", size=15, color=APP_PRIMARIO, bold=True, anchor="end")
    p.nav(1)
    return p


def p12_detalle():
    p = Pantalla()
    p.appbar("Detalle de factura", atras=True, acciones=["compartir"])
    p.rect(16, 96, 328, 100, fill="#3a3530", stroke="none", rx=12)
    p.add('<clipPath id="cd"><rect x="16" y="96" width="328" height="100" rx="12"/></clipPath><g clip-path="url(#cd)">')
    p.factura_mini(40, 104, 280, 300, datos=FACTURAS[1])
    p.add('</g>')
    f = FACTURAS[1]
    filas = [("Proveedor", f[0]), ("RUC", f[1]), ("Serie y número", f[2]), ("Fecha de emisión", f[3]),
             ("Importe total", "S/ " + f[4]), ("Registro", "Leída por IA · 1 campo corregido")]
    p.tarjeta(16, 208, 328, 200)
    for i, (a, b) in enumerate(filas):
        yy = 236 + i * 30
        p.text(30, yy, a, size=12, color=APP_TEXTO2)
        p.text(330, yy, b, size=12.5, color=APP_TEXTO, bold=True, anchor="end")
    # hoja inferior de anulación
    p.rect(0, 0, W, H, fill="#000", stroke="none", opacity=0.45)
    p.rect(0, 418, W, 342, fill=BLANCO, stroke="none", rx=22)
    p.rect(160, 428, 40, 5, fill="#cfd8d5", stroke="none", rx=2.5)
    p.icono("papelera", 34, 462, APP_ERROR, 20)
    p.text(56, 468, "Anular esta factura", size=17, color=APP_TEXTO, bold=True)
    p.text(24, 494, "Se descontará S/ 386,40 del total del mes.", size=12.5, color=APP_TEXTO2)
    motivos = [("Registrada por error", True), ("Es un duplicado", False), ("Mercadería devuelta al proveedor", False)]
    for i, (m, on) in enumerate(motivos):
        yy = 528 + i * 42
        p.circle(38, yy, 10, fill="none", stroke=APP_PRIMARIO if on else "#9aa8a4", sw=2)
        if on:
            p.circle(38, yy, 5, fill=APP_PRIMARIO, stroke="none")
        p.text(58, yy + 5, m, size=14, color=APP_TEXTO)
    p.boton(24, 650, 312, "Anular y recalcular el mes", "peligro")
    p.boton(24, 704, 312, "Cancelar", "texto", h=40)
    return p


def p13_ventas():
    p = Pantalla()
    p.appbar("Ventas de septiembre", atras=True)
    p.tarjeta(16, 100, 328, 96, fill="#f3f8f6", stroke="#d7e6e0")
    p.icono("grafico", 42, 130, APP_PRIMARIO, 22)
    p.texto(66, 126, "La categoría del NRUS se decide con el", size=12.5)
    p.texto(66, 144, "mayor monto del mes: compras o ventas.", size=12.5)
    p.texto(66, 170, "Ingrese solo el total de ventas del mes.", size=12.5, bold=True)
    p.text(180, 250, "Total de ventas del mes", size=13, color=APP_TEXTO2, anchor="middle")
    p.text(180, 296, "S/ 3 900,00", size=36, color=APP_PRIMARIO, bold=True, anchor="middle")
    p.line(70, 312, 290, 312, stroke=APP_ACENTO, sw=3)
    p.text(180, 336, "Compras registradas: S/ 4 120,50", size=12, color=APP_TEXTO2, anchor="middle")
    teclas = ["1", "2", "3", "4", "5", "6", "7", "8", "9", ",", "0", "⌫"]
    for i, t in enumerate(teclas):
        x, y = 16 + (i % 3) * 112, 356 + (i // 3) * 60
        p.rect(x, y, 104, 52, fill=BLANCO, stroke=APP_BORDE, sw=1, rx=10)
        p.text(x + 52, y + 34, t, size=22, color=APP_TEXTO, anchor="middle")
    p.boton(16, 612, 328, "Guardar ventas")
    p.text(180, 690, "Puede cambiarlo hasta cerrar el mes", size=11.5, color=APP_TEXTO2, anchor="middle")
    return p


def p14_categoria():
    p = Pantalla()
    p.appbar("Categoría y cuota", atras=False, sub=MES)
    p.tarjeta(16, 98, 328, 118, fill=APP_PRIMARIO, stroke="none")
    p.text(34, 128, "Le corresponde", size=12.5, color="#cfe3dd")
    p.text(34, 164, "Categoría 1", size=28, color=BLANCO, bold=True)
    p.text(34, 194, "Cuota mensual: S/ 20,00", size=15, color=APP_ACENTO, bold=True)
    p.circle(300, 156, 34, fill="#24615a", stroke="none")
    p.text(300, 168, "1", size=34, color=BLANCO, bold=True, anchor="middle")
    p.tarjeta(16, 228, 328, 176)
    p.text(32, 254, "¿Qué monto decide la categoría?", size=13.5, color=APP_TEXTO, bold=True)
    p.text(32, 284, "Compras", size=12.5, color=APP_TEXTO2)
    p.text(328, 284, "S/ 4 120,50  ← mayor", size=12.5, color=APP_PRIMARIO, bold=True, anchor="end")
    p.barra(32, 292, 296, 0.824, color=APP_ALERTA, h=12, marca=0.8)
    p.text(32, 334, "Ventas", size=12.5, color=APP_TEXTO2)
    p.text(328, 334, "S/ 3 900,00", size=12.5, color=APP_TEXTO, bold=True, anchor="end")
    p.barra(32, 342, 296, 0.78, color=APP_ACENTO, h=12, marca=0.8)
    p.text(32, 384, "Línea negra: aviso al 80 % del límite (S/ 4 000)", size=11, color=APP_TEXTO2)
    p.tarjeta(16, 416, 328, 118)
    p.text(32, 442, "Tabla del NRUS vigente (v2026.1)", size=13.5, color=APP_TEXTO, bold=True)
    for i, (c, lim, cu) in enumerate((("Cat. 1", "hasta S/ 5 000", "S/ 20"), ("Cat. 2", "hasta S/ 8 000", "S/ 50"))):
        yy = 474 + i * 34
        if i == 0:
            p.rect(24, yy - 20, 312, 30, fill="#eef6e4", stroke="none", rx=8)
        p.text(36, yy, c, size=13, color=APP_TEXTO, bold=True)
        p.text(120, yy, lim, size=13, color=APP_TEXTO)
        p.text(324, yy, cu, size=13, color=APP_TEXTO, bold=True, anchor="end")
    p.tarjeta(16, 546, 328, 140)
    p.text(32, 572, "Avisos", size=13.5, color=APP_TEXTO, bold=True)
    for i, (t, on) in enumerate((("Al llegar al 80 % del límite", True), ("Al superar el límite de la categoría", True),
                                  ("Al acercarse a los topes anuales", True))):
        yy = 602 + i * 30
        p.text(32, yy + 5, t, size=12.5, color=APP_TEXTO)
        p.interruptor(290, yy - 8, on)
    p.nav(3)
    return p


def p15_vencimiento():
    p = Pantalla()
    p.appbar("Vencimiento", atras=True)
    p.tarjeta(16, 98, 328, 104, fill="#fff8e8", stroke="#f3dca6")
    p.icono("calendario", 46, 136, "#9a6a00", 28)
    p.text(76, 130, "Declare y pague hasta el", size=13, color=APP_TEXTO2)
    p.text(76, 156, f"{DIAS[VENCE.weekday()].capitalize()} 15 de octubre", size=19, color=APP_TEXTO, bold=True)
    p.text(76, 180, "Faltan 18 días", size=12.5, color="#9a6a00", bold=True)
    p.tarjeta(16, 214, 328, 268)
    p.text(32, 242, "Octubre 2026", size=14, color=APP_TEXTO, bold=True)
    dias = ["L", "M", "M", "J", "V", "S", "D"]
    for i, d in enumerate(dias):
        p.text(46 + i * 44, 270, d, size=11.5, color=APP_TEXTO2, bold=True, anchor="middle")
    inicio = datetime.date(2026, 10, 1).weekday()
    for dia in range(1, 32):
        pos = inicio + dia - 1
        cx, cy = 46 + (pos % 7) * 44, 298 + (pos // 7) * 34
        if dia == 15:
            p.circle(cx, cy - 4, 15, fill=APP_ALERTA, stroke="none")
        p.text(cx, cy + 1, str(dia), size=13, color=BLANCO if dia == 15 else APP_TEXTO, bold=dia == 15, anchor="middle")
    p.tarjeta(16, 494, 328, 70, fill="#f3f8f6", stroke="#d7e6e0")
    p.texto(32, 520, "La fecha depende del último dígito de su", size=12.5)
    p.texto(32, 538, "RUC (4) según el cronograma de la SUNAT.", size=12.5)
    p.text(16, 594, "Recordatorios", size=13.5, color=APP_TEXTO, bold=True)
    for i, (t, on) in enumerate((("5 días antes", True), ("1 día antes", True))):
        yy = 616 + i * 34
        p.icono("campana", 28, yy + 1, APP_PRIMARIO, 16)
        p.text(46, yy + 6, t, size=13, color=APP_TEXTO)
        p.interruptor(300, yy - 8, on)
    p.boton(16, 690, 328, "Ver reporte para declarar", "contorno", h=48)
    return p


def p16_reporte():
    p = Pantalla()
    p.appbar("Reporte del mes", atras=True, sub=MES)
    p.tarjeta(60, 100, 240, 320, fill=BLANCO)
    p.rect(60, 100, 240, 36, fill=APP_PRIMARIO, stroke="none", rx=14)
    p.rect(60, 120, 240, 16, fill=APP_PRIMARIO, stroke="none")
    p.text(74, 123, "Reporte mensual de compras", size=11, color=BLANCO, bold=True)
    for i in range(3):
        p.rect(74, 150 + i * 16, 110 - i * 20, 6, fill="#d9e2df", stroke="none", rx=3)
    p.rect(74, 204, 212, 52, fill="#eef6e4", stroke="none", rx=6)
    p.text(84, 224, "Compras S/ 4 120,50 · Cat. 1", size=10.5, color=APP_PRIMARIO, bold=True)
    p.text(84, 242, "Cuota S/ 20 · vence 15/10/2026", size=10.5, color=APP_TEXTO)
    for i in range(8):
        yy = 272 + i * 17
        p.rect(74, yy, 150, 6, fill="#e6ecea", stroke="none", rx=3)
        p.rect(240, yy, 46, 6, fill="#e6ecea", stroke="none", rx=3)
    p.text(180, 440, "facturas-s20_2026-09.pdf · 3 páginas", size=12, color=APP_TEXTO2, anchor="middle")
    p.tarjeta(16, 458, 328, 116)
    filas = [("Facturas incluidas", "12 (con imagen)"), ("Monto determinante", "S/ 4 120,50"), ("Categoría y cuota", "1 · S/ 20,00")]
    for i, (a, b) in enumerate(filas):
        yy = 488 + i * 30
        p.text(32, yy, a, size=12.5, color=APP_TEXTO2)
        p.text(330, yy, b, size=13, color=APP_TEXTO, bold=True, anchor="end")
    p.boton(16, 592, 328, "Compartir con mi contador", "primario", icono="compartir")
    p.boton(16, 650, 328, "Guardar PDF en el teléfono", "contorno", icono="descarga")
    p.text(180, 728, "El PDF no se envía a ningún servidor", size=11, color=APP_TEXTO2, anchor="middle")
    return p


def p17_historico():
    p = Pantalla()
    p.appbar("Historial de meses", atras=False)
    p.tarjeta(16, 98, 328, 150, fill="#eef6e4", stroke="#cfe3b3")
    p.text(32, 124, "Septiembre 2026", size=15, color=APP_TEXTO, bold=True)
    p.insignia(328, 110, "abierto", fill=BLANCO, color=APP_ACENTO_OSC, anchor="end")
    p.text(32, 150, "Compras S/ 4 120,50 · Ventas S/ 3 900,00", size=12.5, color=APP_TEXTO2)
    p.text(32, 172, "Categoría 1 · cuota S/ 20", size=12.5, color=APP_TEXTO2)
    p.boton(32, 186, 296, "Cerrar el mes", "primario", h=44)
    meses = [("Agosto 2026", "S/ 4 480,20", "Cat. 1 · S/ 20", "cerrado"), ("Julio 2026", "S/ 5 310,00", "Cat. 2 · S/ 50", "cerrado"),
             ("Junio 2026", "S/ 3 960,80", "Cat. 1 · S/ 20", "cerrado"), ("Mayo 2026", "S/ 4 105,40", "Cat. 1 · S/ 20", "cerrado")]
    for i, (m, c, cat, e) in enumerate(meses):
        yy = 262 + i * 80
        p.tarjeta(16, yy, 328, 70, rx=12)
        p.text(32, yy + 28, m, size=14, color=APP_TEXTO, bold=True)
        p.text(32, yy + 50, f"Compras {c}", size=12, color=APP_TEXTO2)
        p.text(300, yy + 28, cat, size=13, color=APP_PRIMARIO, bold=True, anchor="end")
        p.insignia(300, yy + 38, e, fill="#eef1f0", color=APP_TEXTO2, anchor="end")
        p.icono("flecha_der", 326, yy + 35, APP_TEXTO2, 16)
    p.tarjeta(16, 588, 328, 96, fill=BLANCO)
    p.text(32, 614, "Acumulado del año", size=13, color=APP_TEXTO, bold=True)
    p.text(32, 640, "Compras S/ 34 212,60 de S/ 96 000 (tope anual)", size=12, color=APP_TEXTO2)
    p.barra(32, 656, 296, 0.356, color=APP_ACENTO, h=10)
    p.nav(3)
    return p


def p18_ajustes():
    p = Pantalla()
    p.appbar("Ajustes", atras=False)
    secciones = [
        ("Mi negocio", [("RUC", RUC, None), ("Nombre", "Bodega El Progreso", None)]),
        ("Seguridad", [("Cambiar PIN", "", None), ("Ingresar con huella", "", True)]),
        ("Lector de facturas (IA)", [("Gemma 4 E2B facturas", "v1.3 · 2,6 GB · verificado", None),
                                     ("Buscar actualización por Wi-Fi", "", "boton")]),
        ("Parámetros del NRUS", [("Versión", "2026.1 · vigente desde 01/01/2026", None)]),
        ("Respaldo opcional", [("Respaldo cifrado en el servidor", "Desactivado", False)]),
    ]
    y = 98
    for titulo, filas in secciones:
        p.text(20, y + 14, titulo.upper(), size=11, color=APP_TEXTO2, bold=True)
        h = len(filas) * 48
        p.tarjeta(16, y + 22, 328, h, rx=12)
        for i, (a, b, extra) in enumerate(filas):
            yy = y + 22 + i * 48
            if i:
                p.line(28, yy, 332, yy, stroke=APP_BORDE, sw=1)
            if b:
                p.text(30, yy + 21, a, size=13, color=APP_TEXTO, bold=True)
                p.text(30, yy + 38, b, size=11.5, color=APP_TEXTO2)
            else:
                p.text(30, yy + 29, a, size=13.5, color=APP_TEXTO)
            if extra is True or extra is False:
                p.interruptor(292, yy + 13, extra)
            elif extra == "boton":
                p.icono("descarga", 318, yy + 24, APP_PRIMARIO, 18)
            else:
                p.icono("flecha_der", 322, yy + 24, "#9aa8a4", 16)
        y += 22 + h + 12
    p.nav(4)
    return p


def p19_manual():
    p = Pantalla()
    p.appbar("Registro manual", atras=True)
    p.tarjeta(16, 98, 328, 76, fill=APP_ALERTA_CLARO, stroke="#f3dca6")
    p.icono("alerta", 42, 128, APP_ALERTA, 22)
    p.texto(66, 124, "La lectura automática no está disponible", size=12.5, bold=True)
    p.texto(66, 142, "en este equipo (4 GB de RAM). Complete", size=12.5)
    p.texto(66, 160, "los datos; se validan igual que con IA.", size=12.5)
    p.campo(16, 190, 328, "RUC del proveedor", "20610022333", "ok", "RUC válido · Bebidas Rímac S.A.C.")
    p.campo(16, 276, 328, "Proveedor", "Bebidas Rímac S.A.C.", "lectura")
    p.campo(16, 346, 158, "Serie y número", "F003-007712", "normal")
    p.campo(186, 346, 158, "Fecha de emisión", "15/09/2026", "normal")
    p.campo(16, 416, 328, "Importe total", "S/ 540,10", "foco")
    p.tarjeta(16, 492, 328, 64)
    p.icono("camara", 42, 524, APP_PRIMARIO, 22)
    p.text(66, 520, "Adjuntar foto de la factura", size=13.5, color=APP_TEXTO, bold=True)
    p.text(66, 538, "Recomendado para sustentar la compra", size=11.5, color=APP_TEXTO2)
    p.boton(16, 612, 328, "Guardar factura")
    p.boton(16, 668, 328, "Cancelar", "texto")
    return p


PANTALLAS = [
    ("01", "Configuración inicial", p01_configuracion), ("02", "Acceso con PIN o huella", p02_acceso),
    ("03", "Preparar lectura con IA", p03_modelo), ("04", "Inicio: resumen del mes", p04_inicio),
    ("05", "Guía de captura", p05_guia), ("06", "Cámara con encuadre", p06_camara),
    ("07", "Lectura en el teléfono", p07_leyendo), ("08", "Verificación de datos", p08_verificar),
    ("09", "Aviso de duplicado", p09_duplicada), ("10", "Factura guardada", p10_guardada),
    ("11", "Facturas del mes", p11_lista), ("12", "Detalle y anulación", p12_detalle),
    ("13", "Ventas del mes", p13_ventas), ("14", "Categoría y cuota", p14_categoria),
    ("15", "Vencimiento y recordatorio", p15_vencimiento), ("16", "Reporte mensual", p16_reporte),
    ("17", "Historial y cierre de mes", p17_historico), ("18", "Ajustes, modelo y respaldo", p18_ajustes),
    ("19", "Registro manual (sin IA)", p19_manual),
]

HOJAS = [
    ("prototipo_hoja_1_acceso", "Módulo de acceso y configuración", ["01", "02", "03", "04"]),
    ("prototipo_hoja_2_registro", "Módulo de registro de facturas", ["05", "06", "07", "08"]),
    ("prototipo_hoja_3_control", "Módulo de control de facturas", ["09", "10", "11", "12"]),
    ("prototipo_hoja_4_nrus", "Módulo de determinación del NRUS", ["13", "14", "15", "16"]),
    ("prototipo_hoja_5_historial", "Módulos de historial, ajustes y registro manual", ["17", "18", "19"]),
]


def marco(contenido, x, y, escala=1.0, numero=None, titulo=None):
    """Devuelve el SVG de un teléfono (marco + pantalla) en (x, y)."""
    fw, fh = W + 24, H + 24
    s = [f'<g transform="translate({x},{y}) scale({escala})">',
         f'<rect x="0" y="0" width="{fw}" height="{fh}" rx="40" fill="#15191a"/>',
         f'<svg x="12" y="12" width="{W}" height="{H}" viewBox="0 0 {W} {H}">',
         f'<clipPath id="cp{numero}"><rect width="{W}" height="{H}" rx="30"/></clipPath>',
         f'<g clip-path="url(#cp{numero})">{contenido}</g></svg>',
         f'<rect x="{fw / 2 - 40}" y="16" width="80" height="6" rx="3" fill="#15191a"/>', '</g>']
    if numero:
        cy = y + fh * escala + 34
        s.append(f'<circle cx="{x + 20}" cy="{cy}" r="17" fill="{AZUL}"/>'
                 f'<text x="{x + 20}" y="{cy + 5.5}" font-size="15" font-weight="bold" fill="#fff" text-anchor="middle">P{numero}</text>'
                 f'<text x="{x + 46}" y="{cy + 6}" font-size="17" font-weight="bold" fill="#262626">{esc(titulo)}</text>')
    return "".join(s)


def generar_pantallas():
    cache = {}
    for num, titulo, fn in PANTALLAS:
        p = fn()
        cache[num] = (titulo, p.contenido())
        s = Svg(W + 24, H + 24, fondo=None)
        s.add(marco(cache[num][1], 0, 0, 1, None))
        slug = titulo.lower()
        for a, b in (("á", "a"), ("é", "e"), ("í", "i"), ("ó", "o"), ("ú", "u"), ("ñ", "n"), (":", ""), (",", ""), ("(", ""), (")", "")):
            slug = slug.replace(a, b)
        guardar(s, f"pantalla_{num}_" + "_".join(slug.split()[:3]))
    for nombre, titulo_hoja, nums in HOJAS:
        esc_ = 0.92
        ancho = 60 + len(nums) * ((W + 24) * esc_ + 50)
        s = Svg(int(ancho), 900)
        s.text(30, 40, f"Prototipo de Facturas S20 — {titulo_hoja}", size=22, color=AZUL, bold=True)
        for i, n in enumerate(nums):
            s.add(marco(cache[n][1], 30 + i * ((W + 24) * esc_ + 50), 62, esc_, n, cache[n][0]))
        guardar(s, nombre)
    return cache


# ======================================================================
def consola_admin():
    s = Svg(1280, 800, fondo="#f4f6f8")
    s.rect(0, 0, 1280, 800, fill="#f4f6f8", stroke="none")
    s.rect(20, 20, 1240, 760, fill="#fff", stroke=BORDE, sw=1, rx=10)
    s.rect(20, 20, 1240, 36, fill="#e9edf1", stroke="none", rx=10)
    for i, c in enumerate(("#ff5f57", "#febc2e", "#28c840")):
        s.circle(44 + i * 20, 38, 6, fill=c, stroke="none")
    s.rect(120, 28, 600, 20, fill="#fff", stroke="none", rx=10)
    s.text(134, 42, "https://respaldo.facturass20.pe/admin/modelos", size=12, color=GRIS)
    s.rect(20, 56, 230, 724, fill=APP_PRIMARIO, stroke="none")
    s.text(40, 96, "Facturas S20", size=18, color="#fff", bold=True)
    s.text(40, 116, "Consola de administración", size=12, color="#cfe3dd")
    menu = ["Modelos de IA", "Parámetros NRUS", "Cronograma SUNAT", "Cuentas y respaldos", "Auditoría"]
    for i, m in enumerate(menu):
        yy = 160 + i * 44
        if i == 0:
            s.rect(28, yy - 24, 214, 38, fill="#24615a", stroke="none", rx=8)
        s.text(48, yy, m, size=14, color="#fff", bold=(i == 0))
    s.text(40, 750, "admin.equipo · rol ADMIN", size=12, color="#cfe3dd")
    s.text(280, 100, "Versiones del modelo de IA", size=22, color=APP_TEXTO, bold=True)
    s.text(280, 124, "Archivos .litertlm publicados para descarga en los teléfonos", size=13, color=APP_TEXTO2)
    s.rect(1080, 80, 160, 40, fill=APP_ACENTO, stroke="none", rx=20)
    s.text(1160, 106, "+ Publicar versión", size=14, color="#fff", bold=True, anchor="middle")
    cols = [("Versión", 280), ("Publicada", 380), ("Tamaño", 500), ("Exactitud (campos)", 600), ("SHA-256", 760), ("Estado", 1010), ("", 1150)]
    s.rect(270, 146, 970, 40, fill="#eef2f1", stroke="none", rx=6)
    for t, x in cols:
        s.text(x, 171, t, size=13, color=APP_TEXTO2, bold=True)
    filas = [("v1.3", "20/09/2026", "2,61 GB", "93,4 %", "9f2c…e81a", "ACTIVA"), ("v1.2", "30/08/2026", "2,61 GB", "91,0 %", "41ab…07cd", "ANTERIOR"),
             ("v1.1", "10/08/2026", "2,58 GB", "86,7 %", "c3d9…5f10", "RETIRADA")]
    for i, f in enumerate(filas):
        yy = 186 + i * 52
        s.line(270, yy + 52, 1240, yy + 52, stroke="#e5e9e8", sw=1)
        for (t, x), v in zip(cols, f):
            if t == "Estado":
                col = {"ACTIVA": (APP_OK_CLARO, APP_OK), "ANTERIOR": ("#eef1f0", APP_TEXTO2), "RETIRADA": (APP_ERROR_CLARO, APP_ERROR)}[v]
                s.rect(x, yy + 14, len(v) * 8 + 20, 24, fill=col[0], stroke="none", rx=12)
                s.text(x + (len(v) * 8 + 20) / 2, yy + 31, v, size=11.5, color=col[1], bold=True, anchor="middle")
            else:
                s.text(x, yy + 32, v, size=13.5, color=APP_TEXTO, bold=(t == "Versión"), family="Consolas, monospace" if t == "SHA-256" else None)
        s.text(1150, yy + 32, "Ver notas", size=13, color=APP_PRIMARIO, bold=True)
    # formulario
    s.rect(280, 360, 560, 390, fill="#fff", stroke=BORDE, sw=1, rx=10)
    s.text(300, 394, "Publicar nueva versión", size=17, color=APP_TEXTO, bold=True)
    campos = [("Archivo .litertlm", "gemma-4-e2b-facturas-v1.4.litertlm (2,61 GB)"), ("Huella SHA-256 (calculada)", "5be0 7a13 … 9c44 d2e8"),
              ("Exactitud tras la conversión", "94,1 % en 7 campos · conjunto de prueba v3"), ("Notas de la versión", "Mejora en facturas matriciales y fotos con poca luz")]
    for i, (a, b) in enumerate(campos):
        yy = 416 + i * 70
        s.text(300, yy + 14, a, size=12.5, color=APP_TEXTO2)
        s.rect(300, yy + 22, 520, 36, fill="#fafbfb", stroke=BORDE, sw=1, rx=6)
        s.text(312, yy + 45, b, size=13.5, color=APP_TEXTO)
    s.rect(300, 702, 200, 36, fill=APP_PRIMARIO, stroke="none", rx=18)
    s.text(400, 725, "Publicar como ACTIVA", size=13.5, color="#fff", bold=True, anchor="middle")
    s.text(520, 725, "La versión anterior se conserva para volver atrás", size=12, color=APP_TEXTO2)
    s.rect(860, 360, 380, 390, fill="#f3f8f6", stroke="#d7e6e0", sw=1, rx=10)
    s.text(880, 394, "Parámetros NRUS publicados", size=17, color=APP_TEXTO, bold=True)
    s.text(880, 420, "Versión 2026.1 · vigente desde 01/01/2026", size=13, color=APP_TEXTO2)
    for i, (a, b) in enumerate((("Categoría 1", "hasta S/ 5 000 · cuota S/ 20"), ("Categoría 2", "hasta S/ 8 000 · cuota S/ 50"),
                                ("Umbral de aviso", "80 %"), ("Tope anual", "S/ 96 000"), ("Cronograma", "12 meses × 10 dígitos cargado"))):
        yy = 460 + i * 44
        s.text(880, yy, a, size=13.5, color=APP_TEXTO, bold=True)
        s.text(1224, yy, b, size=13, color=APP_TEXTO2, anchor="end")
    s.rect(880, 690, 200, 36, fill="#fff", stroke=APP_PRIMARIO, sw=1.5, rx=18)
    s.text(980, 713, "Nueva versión", size=13.5, color=APP_PRIMARIO, bold=True, anchor="middle")
    guardar(s, "pantalla_20_consola_admin")


def reporte_pdf():
    s = Svg(900, 1200, fondo="#e9ecef")
    s.rect(50, 30, 800, 1140, fill="#fff", stroke=BORDE, sw=1)
    s.rect(50, 30, 800, 110, fill=APP_PRIMARIO, stroke="none")
    s.text(80, 78, "Reporte mensual de compras", size=26, color="#fff", bold=True)
    s.text(80, 108, "Período: septiembre 2026 · Generado por Facturas S20 el 26/09/2026 18:42", size=14, color="#cfe3dd")
    s.circle(790, 85, 30, fill="#fff", stroke="none")
    s.text(790, 93, "S20", size=18, color=APP_PRIMARIO, bold=True, anchor="middle")
    s.text(80, 178, "Contribuyente", size=13, color=APP_TEXTO2)
    s.text(80, 200, "Rosa Huamán Quispe — Bodega El Progreso", size=16, color=APP_TEXTO, bold=True)
    s.text(80, 222, f"RUC {RUC} · Nuevo Régimen Único Simplificado", size=14, color=APP_TEXTO)
    s.rect(80, 246, 740, 140, fill="#f3f8f6", stroke="#d7e6e0", sw=1, rx=8)
    datos = [("Compras del mes (12 facturas)", "S/ 4 120,50"), ("Ventas del mes (declaradas por el titular)", "S/ 3 900,00"),
             ("Monto determinante (el mayor)", "S/ 4 120,50"), ("Categoría y cuota", "Categoría 1 · S/ 20,00"),
             ("Vencimiento (último dígito del RUC: 4)", "15/10/2026")]
    for i, (a, b) in enumerate(datos):
        yy = 274 + i * 24
        s.text(100, yy, a, size=14, color=APP_TEXTO)
        s.text(800, yy, b, size=14, color=APP_TEXTO, bold=True, anchor="end")
    s.text(80, 420, "Detalle de facturas de compra", size=17, color=APP_TEXTO, bold=True)
    cols = [("N.º", 90), ("Fecha", 130), ("Proveedor", 230), ("RUC", 480), ("Serie-número", 600), ("Importe", 810)]
    s.rect(80, 436, 740, 32, fill=APP_PRIMARIO, stroke="none")
    for t, x in cols:
        s.text(x, 457, t, size=12.5, color="#fff", bold=True, anchor="end" if t == "Importe" else "start")
    tabla = FACTURAS + [("Distribuidora Andina S.A.C.", "20601234565", "F001-004655", "08/09/2026", "214,00", "IA")]
    for i, f in enumerate(tabla):
        yy = 468 + i * 30
        if i % 2:
            s.rect(80, yy, 740, 30, fill="#f6f8f7", stroke="none")
        vals = [str(i + 1), f[3], f[0], f[1], f[2], "S/ " + f[4]]
        for (t, x), v in zip(cols, vals):
            s.text(x, yy + 20, v, size=12.5, color=APP_TEXTO, anchor="end" if t == "Importe" else "start")
    yy = 468 + len(tabla) * 30
    s.text(90, yy + 22, "… 6 facturas más (páginas 2 y 3, con la imagen de cada comprobante)", size=12.5, color=APP_TEXTO2)
    s.rect(80, yy + 36, 740, 34, fill="#eef6e4", stroke="none")
    s.text(90, yy + 58, "Total de adquisiciones del mes", size=14, color=APP_TEXTO, bold=True)
    s.text(810, yy + 58, "S/ 4 120,50", size=14, color=APP_PRIMARIO, bold=True, anchor="end")
    s.text(80, yy + 110, "Imágenes de los comprobantes (muestra)", size=15, color=APP_TEXTO, bold=True)
    for i in range(4):
        x = 80 + i * 188
        s.rect(x, yy + 124, 172, 200, fill="#fafafa", stroke=BORDE, sw=1)
        s.rect(x + 12, yy + 136, 148, 150, fill="#fffdf8", stroke="#d6d0c4", sw=1)
        for k in range(6):
            s.rect(x + 22, yy + 150 + k * 20, 100 - (k % 2) * 30, 6, fill="#e2ddd2", stroke="none")
        s.text(x + 86, yy + 308, FACTURAS[i][2], size=11.5, color=APP_TEXTO2, anchor="middle")
    s.line(80, 1110, 820, 1110, stroke=BORDE, sw=1)
    s.text(80, 1134, "Documento de apoyo para la declaración; no reemplaza la declaración ante la SUNAT.", size=11.5, color=APP_TEXTO2)
    s.text(80, 1152, "Integridad del archivo (SHA-256): 3e7a 91c0 5d2b … 0f64 · Página 1 de 3", size=11.5, color=APP_TEXTO2)
    guardar(s, "reporte_mensual_pdf")


# ======================================================================
def sistema_diseno():
    s = Svg(1600, 900)
    s.text(30, 42, "Sistema de diseño de Facturas S20 (guía de estilo para desarrolladores)", size=22, color=AZUL, bold=True)
    colores = [("primario", APP_PRIMARIO, "Barras, botones principales"), ("acento", APP_ACENTO, "Escanear, progreso, éxito"),
               ("fondo", APP_FONDO, "Fondo de pantallas"), ("texto", APP_TEXTO, "Texto principal"), ("texto2", APP_TEXTO2, "Texto secundario"),
               ("alerta", APP_ALERTA, "Confianza baja, 80 % del límite"), ("error", APP_ERROR, "RUC inválido, anular"), ("ok", APP_OK, "Campo validado")]
    s.text(30, 86, "Colores (tokens)", size=16, color=APP_TEXTO, bold=True)
    for i, (n, c, u) in enumerate(colores):
        x, y = 30 + (i % 4) * 190, 100 + (i // 4) * 120
        s.rect(x, y, 170, 60, fill=c, stroke=BORDE, sw=1, rx=10)
        s.text(x, y + 80, f"{n}  {c}", size=13, color=APP_TEXTO, bold=True, family="Consolas, monospace")
        s.text(x, y + 98, u, size=11.5, color=APP_TEXTO2)
    s.text(30, 370, "Tipografía (Roboto en Android; tamaños en sp)", size=16, color=APP_TEXTO, bold=True)
    tipos = [("Título de pantalla", 22, True, "18–22 sp · negrita"), ("Monto destacado", 30, True, "28–36 sp · negrita"),
             ("Texto de campo", 16, True, "16 sp · valor editable"), ("Texto normal", 14, False, "14–16 sp · mínimo 16 en datos"),
             ("Ayuda y notas", 12, False, "12 sp · solo texto auxiliar")]
    for i, (t, sz, b, d) in enumerate(tipos):
        y = 410 + i * 46
        s.text(30, y, t, size=sz, color=APP_PRIMARIO if i < 2 else APP_TEXTO, bold=b)
        s.text(420, y, d, size=12.5, color=APP_TEXTO2)
    # componentes
    s.text(820, 86, "Componentes", size=16, color=APP_TEXTO, bold=True)
    c = Pantalla()
    c.partes = []
    c.boton(0, 0, 300, "Guardar factura")
    c.boton(0, 60, 300, "Abrir cámara", "acento", icono="camara")
    c.boton(0, 120, 300, "Pausar descarga", "contorno")
    c.boton(0, 180, 300, "Anular factura", "peligro")
    c.campo(360, 0, 330, "Campo validado", "20601234565", "ok")
    c.campo(360, 76, 330, "Confianza baja", "S/ 1 450,00", "alerta", "Revise este valor")
    c.campo(360, 172, 330, "Error de validación", "2060123456", "error", "El RUC debe tener 11 dígitos")
    c.insignia(0, 260, "IA")
    c.insignia(40, 260, "manual", fill="#eef1f0", color=APP_TEXTO2)
    c.insignia(118, 260, "abierto", fill="#eef6e4", color=APP_ACENTO_OSC)
    c.interruptor(210, 259, True)
    c.interruptor(262, 259, False)
    c.barra(0, 310, 300, 0.824, color=APP_ALERTA, h=12, marca=0.8)
    c.text(0, 344, "Barra de límite con marca del 80 %", size=11.5, color=APP_TEXTO2)
    iconos = ["camara", "inicio", "lista", "grafico", "ajustes", "check_circulo", "alerta", "candado", "huella", "doc", "compartir",
              "campana", "calendario", "chip", "sinred", "galeria", "descarga", "papelera"]
    for i, ic in enumerate(iconos):
        x, y = 372 + (i % 9) * 36, 290 + (i // 9) * 40
        c.icono(ic, x, y, APP_PRIMARIO, 22)
    c.text(360, 356, "Íconos lineales de 24 dp (Material Symbols en la app)", size=11.5, color=APP_TEXTO2)
    s.add(f'<g transform="translate(820,110)">{"".join(c.partes)}</g>')
    # reglas UX
    s.rect(820, 490, 750, 380, fill="#f3f8f6", stroke="#d7e6e0", sw=1, rx=12)
    s.text(840, 522, "Reglas de UX derivadas de los requerimientos no funcionales", size=16, color=APP_TEXTO, bold=True)
    reglas = ["Objetivos táctiles ≥ 48 dp y texto de datos ≥ 16 sp: se opera con una mano en el mostrador (RNF-10).",
              "Registrar una factura en ≤ 3 toques desde Inicio: botón central Escanear → foto → Guardar (RNF-09).",
              "Lenguaje sin términos contables: «compras», «ventas», «le corresponde» (RNF-11).",
              "Colores con significado fijo: ámbar = revisar, rojo = error, verde = correcto; siempre con ícono y texto.",
              "Contraste mínimo 4,5:1 (WCAG 2.1 AA); ningún dato depende solo del color.",
              "La foto y el campo leído se ven juntos para comparar antes de guardar (RF-05).",
              "Mensajes que dicen qué hacer: «Acérquese hasta que se lea el total», no «Error de lectura».",
              "Todo funciona sin conexión; la app solo pide Wi-Fi para descargar el modelo (RNF-12)."]
    for i, r in enumerate(reglas):
        ls = envolver(r, 92)
        y = 556 + i * 38
        s.circle(848, y - 4, 4, fill=APP_ACENTO, stroke="none")
        s.lines(862, y, ls, size=12.5, color=APP_TEXTO, lh=16)
    s.text(30, 680, "Espaciado y forma", size=16, color=APP_TEXTO, bold=True)
    for i, (t, v) in enumerate((("Margen lateral", "16 dp"), ("Separación entre bloques", "12 dp"), ("Radio de tarjetas", "14 dp"),
                                ("Radio de botones", "24 dp (píldora)"), ("Altura de botón / campo", "48 dp / 58 dp"))):
        y = 716 + i * 32
        s.text(30, y, t, size=13.5, color=APP_TEXTO)
        s.text(420, y, v, size=13.5, color=APP_PRIMARIO, bold=True)
    guardar(s, "ui_sistema_diseno")


def mapa_navegacion():
    s = Svg(1700, 900)
    s.text(30, 42, "Mapa de navegación del prototipo (P01–P19) y flujo principal de registro", size=22, color=AZUL, bold=True)
    nodos = {
        "01": (60, 110), "02": (60, 250), "03": (60, 390), "04": (330, 250),
        "05": (600, 110), "06": (840, 110), "07": (1080, 110), "08": (1320, 110), "09": (1320, 250), "10": (1080, 250), "19": (840, 250),
        "11": (600, 420), "12": (840, 420), "13": (330, 560), "14": (600, 560), "15": (840, 560), "16": (1080, 560),
        "17": (330, 700), "18": (600, 700),
    }
    nombres = {n: t for n, t, _ in PANTALLAS}
    colores = {"01": "#5b3f8c", "02": "#5b3f8c", "03": "#5b3f8c", "04": APP_PRIMARIO}
    for n in ("05", "06", "07", "08", "09", "10", "19"):
        colores[n] = APP_ACENTO_OSC
    for n in ("11", "12"):
        colores[n] = "#2f5597"
    for n in ("13", "14", "15", "16"):
        colores[n] = "#c55a11"
    for n in ("17", "18"):
        colores[n] = GRIS
    BW, BH = 200, 64

    def centro(n, lado):
        x, y = nodos[n]
        return {"izq": (x, y + BH / 2), "der": (x + BW, y + BH / 2), "arr": (x + BW / 2, y), "aba": (x + BW / 2, y + BH)}[lado]

    def arco(a, la, b, lb, etq=None, color=GRIS, dash=None, pts=None):
        p1, p2 = centro(a, la), centro(b, lb)
        camino = [p1] + (pts or []) + [p2]
        s.poly(camino, stroke=color, sw=1.8, end="flecha", dash=dash)
        if etq:
            mx = (camino[0][0] + camino[1][0]) / 2
            my = (camino[0][1] + camino[1][1]) / 2
            s.text(mx, my - 6, etq, size=11, color=GRIS_MEDIO, italic=True, anchor="middle")

    verde = APP_ACENTO_OSC
    ROJO_FLUJO = "#c00000"

    def ruta(pts, color=GRIS, dash=None, etq=None, pos=None):
        s.poly(pts, stroke=color, sw=1.8, end="flecha", dash=dash)
        if etq:
            s.text(pos[0], pos[1], etq, size=11, color=GRIS_MEDIO, italic=True, anchor="middle")

    arco("01", "aba", "02", "arr"); s.text(170, 214, "1.ª vez", size=11, color=GRIS_MEDIO, italic=True, anchor="end")
    arco("02", "der", "04", "izq"); s.text(295, 274, "PIN o huella", size=11, color=GRIS_MEDIO, italic=True, anchor="middle")
    arco("02", "aba", "03", "arr"); s.text(170, 356, "sin modelo", size=11, color=GRIS_MEDIO, italic=True, anchor="end")
    ruta([(260, 422), (295, 422), (295, 300), (330, 300)], etq="listo", pos=(278, 440))
    ruta([(430, 250), (430, 142), (600, 142)], verde, etq="Escanear", pos=(515, 134))
    arco("05", "der", "06", "izq", None, verde)
    arco("06", "der", "07", "izq", None, verde)
    arco("07", "der", "08", "izq", None, verde)
    ruta([(1470, 174), (1470, 250)], ROJO_FLUJO, etq="duplicada", pos=(1510, 216))
    ruta([(1380, 174), (1380, 214), (1300, 214), (1300, 282), (1280, 282)], verde, etq="Guardar", pos=(1340, 206))
    ruta([(1180, 314), (1180, 350), (560, 350), (560, 290), (530, 290)], etq="Ir al inicio", pos=(870, 344))
    ruta([(940, 174), (940, 250)], ROJO_FLUJO, dash="6 4", etq="sin IA (4 GB)", pos=(985, 216))
    arco("19", "der", "10", "izq")
    ruta([(480, 314), (480, 452), (600, 452)], etq="Facturas", pos=(540, 444))
    ruta([(430, 314), (430, 560)], etq="Editar ventas", pos=(398, 470))
    ruta([(360, 314), (360, 520), (310, 520), (310, 732), (330, 732)], etq="Mes", pos=(335, 512))
    arco("11", "der", "12", "izq")
    arco("13", "der", "14", "izq")
    arco("14", "der", "15", "izq")
    arco("15", "der", "16", "izq")
    arco("17", "der", "18", "izq")
    for n, (x, y) in nodos.items():
        c = colores[n]
        s.rect(x, y, BW, BH, fill="#fff", stroke=c, sw=2, rx=12)
        s.rect(x, y, 54, BH, fill=c, stroke=c, sw=2, rx=12)
        s.rect(x + 40, y, 14, BH, fill=c, stroke="none")
        s.text(x + 27, y + 38, f"P{n}", size=15, color="#fff", bold=True, anchor="middle")
        ls = envolver(nombres[n], 18)
        s.lines(x + 66, y + 28 - (len(ls) - 1) * 8 + 6, ls, size=12.5, color="#1a1a1a", bold=True, lh=16)
    # leyenda
    ley = [("#5b3f8c", "Acceso y preparación"), (APP_PRIMARIO, "Pantalla principal"), (APP_ACENTO_OSC, "Registro de facturas"),
           ("#2f5597", "Control de facturas"), ("#c55a11", "Determinación NRUS"), (GRIS, "Historial y ajustes")]
    s.rect(1320, 420, 350, 290, fill="#fff", stroke=BORDE, sw=1, rx=10)
    s.text(1340, 450, "Módulos", size=15, color=APP_TEXTO, bold=True)
    for i, (c, t) in enumerate(ley):
        s.rect(1340, 468 + i * 30, 18, 18, fill=c, stroke="none", rx=4)
        s.text(1368, 482 + i * 30, t, size=13, color=APP_TEXTO)
    s.line(1340, 660, 1390, 660, stroke=APP_ACENTO_OSC, sw=2, end="flecha")
    s.text(1400, 665, "flujo principal (≤ 3 toques)", size=12.5, color=APP_TEXTO)
    s.line(1340, 688, 1390, 688, stroke="#c00000", sw=2, dash="6 4", end="flecha")
    s.text(1400, 693, "flujo alterno / excepción", size=12.5, color=APP_TEXTO)
    s.rect(30, 800, 1640, 70, fill="#f3f8f6", stroke="#d7e6e0", sw=1, rx=10)
    s.text(50, 828, "Barra inferior siempre visible en P04, P11, P14, P17 y P18: Inicio · Facturas · [Escanear] · Mes · Ajustes.",
           size=13.5, color=APP_TEXTO, bold=True)
    s.text(50, 852, "El botón central Escanear abre P05 desde cualquiera de ellas; «atrás» del sistema vuelve siempre a la pantalla anterior.",
           size=13, color=APP_TEXTO2)
    guardar(s, "ui_mapa_navegacion")


def generar():
    generar_pantallas()
    consola_admin()
    reporte_pdf()
    sistema_diseno()
    mapa_navegacion()


if __name__ == "__main__":
    generar()
