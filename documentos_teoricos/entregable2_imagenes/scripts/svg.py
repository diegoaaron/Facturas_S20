"""Utilidades mínimas para dibujar SVG y convertirlos a PNG con Chrome headless.

Todas las figuras del entregable 2 se generan desde código para poder editarlas:
cambia el texto o las coordenadas en diagramas.py / pantallas.py y vuelve a ejecutar
`python generar_todo.py`. Los .svg quedan en ../fuentes (editables en Inkscape,
Figma o Penpot) y los .png en la carpeta ../ (los que se insertan en Word y PPT).
"""
import os
import subprocess
from html import escape

AQUI = os.path.dirname(os.path.abspath(__file__))
CARPETA = os.path.dirname(AQUI)
FUENTES = os.path.join(CARPETA, "fuentes")
CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"

FONT = "Arial, Helvetica, sans-serif"

# Paleta común de los diagramas (misma que el entregable 1)
AZUL = "#1f4e79"
AZUL_CLARO = "#dce6f2"
ROJO = "#c00000"
ROJO_CLARO = "#fbe9e9"
VERDE = "#1e7b4f"
VERDE_CLARO = "#e3f1e8"
NARANJA = "#c55a11"
NARANJA_CLARO = "#fdf0e6"
GRIS = "#404040"
GRIS_MEDIO = "#7f7f7f"
GRIS_CLARO = "#f2f2f2"
BORDE = "#a6a6a6"

# Paleta de la app (tokens del diseño final del equipo)
APP_PRIMARIO = "#1b4d45"
APP_ACENTO = "#8bc34a"
APP_ACENTO_OSC = "#5f8f2a"
APP_FONDO = "#f5f7f6"
APP_TEXTO = "#1d2b28"
APP_TEXTO2 = "#5e6e6a"
APP_ALERTA = "#f0a202"
APP_ALERTA_CLARO = "#fff4db"
APP_ERROR = "#c62828"
APP_ERROR_CLARO = "#fdecea"
APP_OK = "#2e7d32"
APP_OK_CLARO = "#e8f5e9"
APP_BORDE = "#dfe5e3"


def esc(t):
    return escape(str(t), quote=True)


class Svg:
    def __init__(self, w, h, fondo="#ffffff"):
        self.w, self.h = w, h
        self.partes = []
        self.defs = []
        if fondo:
            self.rect(0, 0, w, h, fill=fondo, stroke="none")
        self._marcadores()

    # ---------- infraestructura ----------
    def _marcadores(self):
        for nombre, color in (("flecha", GRIS), ("flecha_azul", AZUL), ("flecha_verde", VERDE),
                              ("flecha_roja", ROJO), ("flecha_gris", GRIS_MEDIO)):
            self.defs.append(
                f'<marker id="{nombre}" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" '
                f'markerHeight="7" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" fill="{color}"/></marker>')
        # flecha abierta (mensaje BPMN / dependencia UML)
        self.defs.append('<marker id="abierta" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" '
                         'orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10" fill="none" stroke="#404040" stroke-width="1.4"/></marker>')
        # triángulo hueco (herencia / realización UML)
        self.defs.append('<marker id="triangulo" viewBox="0 0 12 12" refX="11" refY="6" markerWidth="12" markerHeight="12" '
                         'orient="auto"><path d="M0,0 L12,6 L0,12 z" fill="#fff" stroke="#404040" stroke-width="1.2"/></marker>')
        # rombo lleno (composición) y hueco (agregación)
        self.defs.append('<marker id="rombo" viewBox="0 0 16 10" refX="1" refY="5" markerWidth="16" markerHeight="10" '
                         'orient="auto"><path d="M1,5 L8,1 L15,5 L8,9 z" fill="#404040"/></marker>')
        self.defs.append('<marker id="rombo_hueco" viewBox="0 0 16 10" refX="1" refY="5" markerWidth="16" markerHeight="10" '
                         'orient="auto"><path d="M1,5 L8,1 L15,5 L8,9 z" fill="#fff" stroke="#404040"/></marker>')
        # círculo de inicio de flujo de mensaje BPMN
        self.defs.append('<marker id="circulo" viewBox="0 0 10 10" refX="5" refY="5" markerWidth="7" markerHeight="7">'
                         '<circle cx="5" cy="5" r="4" fill="#fff" stroke="#404040" stroke-width="1.2"/></marker>')

    def add(self, s):
        self.partes.append(s)
        return self

    def to_string(self):
        return (f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" '
                f'width="{self.w}" height="{self.h}" viewBox="0 0 {self.w} {self.h}" font-family="{FONT}">'
                f'<defs>{"".join(self.defs)}</defs>{"".join(self.partes)}</svg>')

    # ---------- primitivas ----------
    def rect(self, x, y, w, h, fill="#fff", stroke=GRIS, sw=1.5, rx=0, dash=None, opacity=None):
        d = f' stroke-dasharray="{dash}"' if dash else ""
        o = f' opacity="{opacity}"' if opacity is not None else ""
        return self.add(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" fill="{fill}" '
                        f'stroke="{stroke}" stroke-width="{sw}"{d}{o}/>')

    def circle(self, cx, cy, r, fill="#fff", stroke=GRIS, sw=1.5, dash=None):
        d = f' stroke-dasharray="{dash}"' if dash else ""
        return self.add(f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{fill}" stroke="{stroke}" stroke-width="{sw}"{d}/>')

    def line(self, x1, y1, x2, y2, stroke=GRIS, sw=1.5, dash=None, end=None, start=None):
        d = f' stroke-dasharray="{dash}"' if dash else ""
        me = f' marker-end="url(#{end})"' if end else ""
        ms = f' marker-start="url(#{start})"' if start else ""
        return self.add(f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{stroke}" stroke-width="{sw}"{d}{me}{ms}/>')

    def path(self, d, stroke=GRIS, sw=1.5, fill="none", dash=None, end=None, start=None):
        ds = f' stroke-dasharray="{dash}"' if dash else ""
        me = f' marker-end="url(#{end})"' if end else ""
        ms = f' marker-start="url(#{start})"' if start else ""
        return self.add(f'<path d="{d}" fill="{fill}" stroke="{stroke}" stroke-width="{sw}"{ds}{me}{ms}/>')

    def poly(self, pts, stroke=GRIS, sw=1.5, end=None, dash=None, start=None):
        d = "M" + " L".join(f"{x},{y}" for x, y in pts)
        return self.path(d, stroke=stroke, sw=sw, end=end, dash=dash, start=start)

    def polygon(self, pts, fill="#fff", stroke=GRIS, sw=1.5):
        p = " ".join(f"{x},{y}" for x, y in pts)
        return self.add(f'<polygon points="{p}" fill="{fill}" stroke="{stroke}" stroke-width="{sw}"/>')

    def text(self, x, y, t, size=14, color=GRIS, bold=False, italic=False, anchor="start", family=None, weight=None):
        fw = weight or ("bold" if bold else "normal")
        fs = ' font-style="italic"' if italic else ""
        ff = f' font-family="{family}"' if family else ""
        return self.add(f'<text x="{x}" y="{y}" font-size="{size}" fill="{color}" font-weight="{fw}"{fs}{ff} '
                        f'text-anchor="{anchor}">{esc(t)}</text>')

    def lines(self, x, y, textos, size=14, color=GRIS, bold=False, anchor="start", lh=None, italic=False):
        lh = lh or size * 1.3
        for i, t in enumerate(textos):
            self.text(x, y + i * lh, t, size=size, color=color, bold=bold, anchor=anchor, italic=italic)
        return self

    def image(self, x, y, w, h, href):
        return self.add(f'<image x="{x}" y="{y}" width="{w}" height="{h}" href="{esc(href)}"/>')

    def group(self, contenido, tx=0, ty=0, scale=1):
        return self.add(f'<g transform="translate({tx},{ty}) scale({scale})">{contenido}</g>')


def envolver(texto, max_chars):
    """Parte un texto en líneas de como máximo max_chars caracteres (por palabras)."""
    palabras, lineas, actual = str(texto).split(), [], ""
    for p in palabras:
        if len(actual) + len(p) + (1 if actual else 0) <= max_chars:
            actual = f"{actual} {p}" if actual else p
        else:
            if actual:
                lineas.append(actual)
            actual = p
    if actual:
        lineas.append(actual)
    return lineas


def guardar(svg, nombre, escala=2):
    """Escribe fuentes/<nombre>.svg y ../<nombre>.png (a escala `escala`)."""
    os.makedirs(FUENTES, exist_ok=True)
    ruta_svg = os.path.join(FUENTES, nombre + ".svg")
    with open(ruta_svg, "w", encoding="utf-8") as f:
        f.write(svg.to_string())
    ruta_png = os.path.join(CARPETA, nombre + ".png")
    url = "file:///" + ruta_svg.replace("\\", "/")
    subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars",
                    f"--force-device-scale-factor={escala}", f"--window-size={svg.w},{svg.h}",
                    f"--screenshot={ruta_png}", url], check=True, capture_output=True)
    print("  ok", nombre, f"{svg.w}x{svg.h}")
    return ruta_png
