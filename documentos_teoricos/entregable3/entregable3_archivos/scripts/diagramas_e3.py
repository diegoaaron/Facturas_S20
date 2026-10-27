"""Figuras nuevas del entregable 3: Gantt al corte de la semana 12 y diagramas lógico y físico de la base.

Reutiliza las utilidades de dibujo del entregable 2 (svg.py y diagramas_uml.py) y deja los PNG en
entregable3_archivos/ y los SVG editables en entregable3_archivos/fuentes/. Los diagramas de la base se arman
desde el esquema que exporta Room (android/app/schemas/.../1.json), así que coinciden con la base real de la app.
Las figuras no llevan título dentro: el título va en la diapositiva o en la leyenda APA.

Uso:  python diagramas_e3.py
"""
import json
import os
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
TEORICOS = os.path.normpath(os.path.join(AQUI, "..", "..", ".."))
sys.path.insert(0, os.path.join(TEORICOS, "entregable2", "entregable2_archivos", "scripts"))

import svg  # noqa: E402
from svg import AZUL, VERDE, NARANJA, ROJO, GRIS, GRIS_MEDIO, BORDE  # noqa: E402
from diagramas_uml import Uml, MORADO  # noqa: E402

svg.CARPETA = os.path.dirname(AQUI)
svg.FUENTES = os.path.join(svg.CARPETA, "fuentes")
ESQUEMA = os.path.join(TEORICOS, "..", "android", "app", "schemas", "pe.facturass20.datos.BaseDatosFacturas", "1.json")
CORTE = 12


def esquema():
    with open(ESQUEMA, encoding="utf-8") as f:
        entidades = json.load(f)["database"]["entities"]
    return {e["tableName"]: e for e in entidades}


def claves(e):
    """Devuelve (pk, fks, únicas) de una entidad del esquema de Room."""
    pk = set(e["primaryKey"]["columnNames"])
    fks = {c: k["table"] for k in e.get("foreignKeys", []) for c in k["columns"]}
    unicas = {i["columnNames"][0] for i in e.get("indices", []) if i.get("unique") and len(i["columnNames"]) == 1}
    return pk, fks, unicas


# ======================================================================
def gantt():
    s = Uml(1760, 830)
    x0, y0, cw, rh = 470, 20, 66, 34
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
        ("3. Diseño (BPMN, clases, BD, prototipo)", 3, 8, "ok", NARANJA, True),
        ("4. Modelo Gemma 4 ajustado", 5, 12, "curso", ROJO, True),
        ("   Recopilación y etiquetado (52 fotos)", 5, 9, "ok", ROJO, False),
        ("   Ajuste fino, conversión y evaluación", 9, 12, "curso", ROJO, False),
        ("5. Construcción de la app", 7, 14, "curso", "#2f5597", True),
        ("   Front-end: pantallas P01–P20", 7, 12, "ok", "#2f5597", False),
        ("   Dominio, Room y SQLCipher", 8, 10, "ok", "#2f5597", False),
        ("   Gemma en el teléfono (LiteRT-LM)", 11, 12, "curso", "#2f5597", False),
        ("   Reporte PDF, exportación y avisos", 13, 14, "ok", "#2f5597", False),
        ("6. Pruebas y validación", 12, 16, "curso", VERDE, True),
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
            hasta = min(b, CORTE)
            s.rect(bx, y + 3, max(0, (hasta - a + 1) * cw - 8), 22, fill=col, stroke=col, sw=1.4, rx=5, opacity=0.45)
        texto = textos[est] + (" (adelantado)" if est == "ok" and b > CORTE else "")
        fin = b < 17
        s.text(bx + bw + 6 if fin else bx + bw - 6, y + 19, texto, size=11, color=GRIS, anchor="start" if fin else "end")
    corte = x0 + CORTE * cw
    ytot = y0 + 40 + len(filas) * rh
    s.line(corte, y0 + 32, corte, ytot, stroke=ROJO, sw=2.2, dash="7 4")
    s.rect(corte - 75, ytot + 6, 150, 24, fill=ROJO, stroke="none", rx=12)
    s.text(corte, ytot + 23, f"Hoy: APF3 (sem. {CORTE})", size=12, color="#fff", bold=True, anchor="middle")
    ly = ytot + 60
    s.rect(40, ly, 30, 14, fill=AZUL, stroke=AZUL, rx=3); s.text(78, ly + 12, "completado", size=12, color=GRIS)
    s.rect(190, ly, 30, 14, fill=AZUL, stroke=AZUL, rx=3, opacity=0.45); s.text(228, ly + 12, "en curso (hasta el corte)", size=12, color=GRIS)
    s.rect(420, ly, 30, 14, fill="#fff", stroke=AZUL, rx=3); s.text(458, ly + 12, "planificado", size=12, color=GRIS)
    s.rect(580, ly, 30, 14, fill=ROJO, stroke=ROJO, rx=3); s.text(618, ly + 12, "semanas de entrega: APF1 (4), APF2 (8), APF3 (12), final (18)", size=12, color=GRIS)
    s.h = int(ly + 40)
    svg.guardar(s, "gantt_semana12")


# ======================================================================
def bd_logico():
    """Modelo lógico: entidades, atributos, claves y relaciones con su cardinalidad (sin tipos de dato)."""
    E = esquema()
    s = Uml(1760, 1100)

    def ent(x, y, w, nombre):
        pk, fks, unicas = claves(E[nombre])
        cols = []
        for f in E[nombre]["fields"]:
            c = f["columnName"]
            tipo = "pk" if c in pk else "fk" if c in fks else ""
            cols.append((c + (" (UQ)" if c in unicas and tipo != "pk" else ""), tipo))
        return s.tabla(x, y, w, nombre, cols)

    T = {
        "contribuyente": ent(30, 20, 330, "contribuyente"),
        "periodo": ent(470, 20, 340, "periodo"),
        "determinacion": ent(930, 20, 360, "determinacion"),
        "categoria_nrus": ent(1410, 20, 320, "categoria_nrus"),
        "parametro_version": ent(1410, 330, 320, "parametro_version"),
        "cronograma_venc": ent(1410, 620, 320, "cronograma_venc"),
        "emisor": ent(30, 400, 330, "emisor"),
        "factura_compra": ent(470, 400, 340, "factura_compra"),
        "imagen_factura": ent(30, 600, 330, "imagen_factura"),
        "reporte_mensual": ent(930, 400, 360, "reporte_mensual"),
        "aviso": ent(930, 600, 360, "aviso"),
        "campo_extraido": ent(470, 800, 340, "campo_extraido"),
        "modelo_local": ent(930, 800, 360, "modelo_local"),
    }
    abajo = {k: v[1] + v[3] for k, v in T.items()}
    r = s.rel
    r([(360, 60), (470, 60)], "izq", "1", "der", "1..N", "declara", (415, 52))
    r([(810, 60), (930, 60)], "izq", "1", "der", "0..1", "resulta en", (870, 52))
    r([(1290, 80), (1410, 80)], "izq", "0..N", "der", "1", "se ubica en", (1350, 72))
    r([(1570, 330), (1570, abajo["categoria_nrus"])], "aba", "1", "arr", "1..N", "define", (1605, 305))
    r([(1570, abajo["parametro_version"]), (1570, 620)], "arr", "1", "aba", "1..N", "publica", (1605, 600))
    r([(640, abajo["periodo"]), (640, 400)], "arr", "1", "aba", "0..N", "contiene", (680, 300))
    r([(360, 440), (470, 440)], "izq", "1", "der", "0..N", "emite", (415, 432))
    r([(470, 640), (420, 640), (420, 650), (360, 650)], "der", "1", "izq", "0..1", "respalda", (430, 632))
    r([(640, abajo["factura_compra"]), (640, 800)], "arr", "1", "aba", "0..N", "produce", (680, 760))
    r([(930, 860), (810, 860)], "der", "1", "izq", "0..N", "genera", (870, 852))
    r([(810, 130), (870, 130), (870, 440), (930, 440)], "izq", "1", "der", "0..N", "resume", (900, 300))
    r([(810, 150), (890, 150), (890, 640), (930, 640)], "izq", "1", "der", "0..N", "programa", (918, 560))
    # leyenda
    s.rect(30, 800, 400, 270, fill="#fff", stroke=BORDE, sw=1, rx=8)
    s.text(50, 826, "Notación pata de gallo (Crow's Foot)", size=13.5, color=AZUL, bold=True)
    for i, (t, d) in enumerate([("1", "uno y solo uno"), ("0..1", "cero o uno"), ("1..N", "uno o muchos"), ("0..N", "cero o muchos")]):
        yy = 860 + i * 34
        s.line(60, yy, 150, yy, stroke=GRIS, sw=1.5)
        s.pata(150, yy, "der", t)
        s.text(170, yy + 4, d, size=12, color=GRIS)
    s.text(50, 1010, "PK clave primaria (rojo) · FK clave foránea (naranja)", size=11.5, color=GRIS)
    s.text(50, 1028, "(UQ) valor único · 13 entidades en 3FN", size=11.5, color=GRIS)
    s.text(50, 1046, "Fuente: esquema v1 que exporta Room", size=11.5, color=GRIS)
    svg.guardar(s, "bd_logico")


# ======================================================================
def bd_fisico(nombre_figura, tablas, columnas=4):
    """Modelo físico: tablas de SQLite con tipo, nulidad, claves e índices tal como los crea Room."""
    E = esquema()
    w, gx, fs = 420, 25, 11.5
    lh = fs * 1.55
    alto = {}
    for t in tablas:
        alto[t] = 28 + (len(E[t]["fields"]) + len(E[t].get("indices", []))) * lh + 22
    filas = [tablas[i:i + columnas] for i in range(0, len(tablas), columnas)]
    s = Uml(columnas * (w + gx) + 30, 10)
    y = 20
    for fila in filas:
        for j, t in enumerate(fila):
            x = 30 + j * (w + gx)
            e = E[t]
            pk, fks, _ = claves(e)
            h = alto[t]
            s.rect(x, y, w, h, fill="#fff", stroke=AZUL, sw=1.6, rx=4)
            s.rect(x, y, w, 28, fill=AZUL, stroke=AZUL, sw=1.6, rx=4)
            s.text(x + w / 2, y + 19, t, size=13.5, color="#fff", bold=True, anchor="middle")
            yy = y + 28 + 16
            for f in e["fields"]:
                c = f["columnName"]
                tipo = f["affinity"] + (" NOT NULL" if f.get("notNull") else "")
                if c in pk:
                    tipo = "INTEGER PK AUTOINCREMENT" if e["primaryKey"].get("autoGenerate") else tipo + " PK"
                col = ROJO if c in pk else NARANJA if c in fks else "#262626"
                s.text(x + 10, yy, c, size=fs, color=col, bold=c in pk)
                extra = f"→ {fks[c]}" if c in fks else ""
                s.text(x + 190, yy, (tipo + "  " + extra).strip(), size=fs - 1, color=GRIS_MEDIO if c not in fks else NARANJA)
                yy += lh
            s.line(x + 8, yy - 10, x + w - 8, yy - 10, stroke=BORDE, sw=1)
            yy += 6
            for i in e.get("indices", []):
                etiqueta = ("UNIQUE " if i.get("unique") else "INDEX ") + "(" + ", ".join(i["columnNames"]) + ")"
                s.text(x + 10, yy, etiqueta, size=fs - 1, color=MORADO, italic=True)
                yy += lh
        y += max(alto[t] for t in fila) + 25
    s.h = int(y)
    svg.guardar(s, nombre_figura)


def generar():
    gantt()
    bd_logico()
    bd_fisico("bd_fisico_registro", ["contribuyente", "periodo", "emisor", "factura_compra",
                                     "imagen_factura", "campo_extraido", "modelo_local"])
    bd_fisico("bd_fisico_nrus", ["parametro_version", "categoria_nrus", "cronograma_venc",
                                 "determinacion", "aviso", "reporte_mensual"], columnas=3)


if __name__ == "__main__":
    generar()
