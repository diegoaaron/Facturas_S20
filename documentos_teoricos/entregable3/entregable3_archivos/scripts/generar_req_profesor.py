"""Arma documentos_teoricos/entregable3/req_profesor/ con lo que pide el docente para el avance 3.

La estructura sigue entregable2/observaciones_profesor_entregable/apuntes_correciones_integrador.txt:

    informe/                 Word, PDF del Word y PPT
    base_de_datos/           BD física (imágenes + script SQL), BD lógica y diagrama entidad-relación
    anexo/                   Lean Canvas, Charter, FODA y otros/ (BMC, WBS, casos de uso, arquitectura, riesgos)
    proyecto/                APK y ZIP del proyecto de IntelliJ
    github/                  enlace al repositorio
    diccionario_de_datos/    PDF (4.ª unidad)
    manual_de_usuario/       PDF (4.ª unidad)
    fuentes/                 PDF de investigación de la variable dependiente y de la independiente
    prototipos/              imágenes del prototipo

Las figuras se dibujan en HTML o SVG y se convierten a PNG/PDF con Chrome headless; el Word se pasa a PDF con
Word (COM). Antes hay que correr construir_presentacion.py, diagramas_e3.py y corregir_apf2.py, y compilar la app
(./gradlew :app:assembleDebug). Uso:  python generar_req_profesor.py
"""
import html
import json
import os
import shutil
import subprocess
import sys
import tempfile
import urllib.request
import zipfile

AQUI = os.path.dirname(os.path.abspath(__file__))
E3_ARCHIVOS = os.path.dirname(AQUI)
E3 = os.path.dirname(E3_ARCHIVOS)
TEORICOS = os.path.dirname(E3)
RAIZ = os.path.dirname(TEORICOS)
E2_ARCHIVOS = os.path.join(TEORICOS, "entregable2", "entregable2_archivos")
CAPTURAS = os.path.join(E3_ARCHIVOS, "capturas")
DESTINO = os.path.join(E3, "req_profesor")
ESQUEMA = os.path.join(RAIZ, "android", "app", "schemas", "pe.facturass20.datos.BaseDatosFacturas", "1.json")
APK = os.path.join(RAIZ, "android", "app", "build", "outputs", "apk", "debug", "app-debug.apk")
REPOSITORIO = "https://github.com/diegoaaron/Facturas_S20"
CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"

sys.path.insert(0, os.path.join(E2_ARCHIVOS, "scripts"))
from contenido_documento import TITULO  # noqa: E402

# PDF de investigación (acceso abierto) que sustentan las variables del título.
FUENTES = {
    "variables_dependientes": [  # exactitud de la declaración mensual del NRUS
        ("orihuela_2023_cultura_tributaria_cumplimiento_nrus.pdf",
         "https://rivc.upeu.edu.pe/index.php/ri_vc/article/download/1784/1992"),
        ("silva_2024_analisis_nrus_2015_2023.pdf",
         "https://repositorio.upn.edu.pe/backend/api/core/bitstreams/6a57bd2c-6ed5-40be-b7fd-c8210282f3b4/content"),
        ("unsaac_2023_cumplimiento_tributario_abarrotes_nrus.pdf",
         "https://repositorio.unsaac.edu.pe/bitstreams/622f5885-d8aa-4f82-8c98-ba9d436fddf1/download"),
    ],
    "variables_independientes": [  # aplicación móvil con IA multimodal (Gemma 4)
        ("gemma_team_2025_gemma3_technical_report.pdf", "https://arxiv.org/pdf/2503.19786"),
        ("kim_2022_donut_ocr_free_document_understanding.pdf", "https://arxiv.org/pdf/2111.15664"),
        ("hu_2021_lora_low_rank_adaptation.pdf", "https://arxiv.org/pdf/2106.09685"),
    ],
}

ESTILO = """
* { box-sizing: border-box; }
body { margin: 0; font-family: Calibri, Arial, sans-serif; color: #262626; background: #fff; }
h1 { color: #1f4e79; font-size: 30px; margin: 0 0 4px; }
h2 { color: #1f4e79; font-size: 22px; margin: 26px 0 8px; border-bottom: 3px solid #c00000; padding-bottom: 4px; }
h3 { color: #c00000; font-size: 15px; margin: 0 0 6px; text-transform: uppercase; letter-spacing: .5px; }
.sub { color: #7f7f7f; font-size: 15px; margin-bottom: 18px; }
table { border-collapse: collapse; width: 100%; font-size: 13px; }
th { background: #1f4e79; color: #fff; padding: 6px 8px; text-align: left; }
td { border-bottom: 1px solid #d9d9d9; padding: 5px 8px; vertical-align: top; }
tr:nth-child(even) td { background: #f6f8fa; }
.caja { border: 1.5px solid #a6a6a6; border-radius: 8px; padding: 12px 14px; background: #fff; }
.caja ul { margin: 0; padding-left: 18px; } .caja li { margin: 3px 0; font-size: 14px; }
.pie { color: #7f7f7f; font-size: 12px; margin-top: 10px; }
"""


# ---------------------------------------------------------------- utilidades
def carpeta(*partes):
    ruta = os.path.join(DESTINO, *partes)
    os.makedirs(ruta, exist_ok=True)
    return ruta


def copiar(origen, *destino):
    ruta = os.path.join(DESTINO, *destino)
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    shutil.copyfile(origen, ruta)
    print("  copiado", os.path.relpath(ruta, DESTINO))


def chrome(cuerpo, salida, ancho=None, alto=None):
    """Convierte HTML a PNG (si se da el tamaño) o a PDF (A4, con paginación)."""
    with tempfile.TemporaryDirectory() as tmp:
        pagina = os.path.join(tmp, "pagina.html")
        with open(pagina, "w", encoding="utf-8") as f:
            f.write(f"<!doctype html><html lang='es'><head><meta charset='utf-8'><style>{ESTILO}</style></head>"
                    f"<body>{cuerpo}</body></html>")
        url = "file:///" + pagina.replace("\\", "/")
        if salida.endswith(".png"):
            args = ["--force-device-scale-factor=2", f"--window-size={ancho},{alto}", f"--screenshot={salida}"]
        else:
            args = ["--no-pdf-header-footer", f"--print-to-pdf={salida}"]
        subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars",
                        "--allow-file-access-from-files", *args, url], check=True, capture_output=True)
    print("  generado", os.path.relpath(salida, DESTINO))


def lista(items):
    return "<ul>" + "".join(f"<li>{html.escape(i)}</li>" for i in items) + "</ul>"


def esquema():
    with open(ESQUEMA, encoding="utf-8") as f:
        return json.load(f)["database"]["entities"]


# ---------------------------------------------------------------- informe
def informe():
    print("Informe")
    docx = os.path.join(E3, "entregable3_apf2_corregido.docx")
    copiar(docx, "informe", "entregable3_documento.docx")
    copiar(os.path.join(E3, "entregable3_presentacion.pptx"), "informe", "entregable3_presentacion.pptx")
    pdf = os.path.join(carpeta("informe"), "entregable3_documento.pdf")
    comando = (f"$w = New-Object -ComObject Word.Application; $w.Visible = $false; $w.DisplayAlerts = 0; "
               f"try {{ $d = $w.Documents.Open('{docx}', $false, $true); $d.ExportAsFixedFormat('{pdf}', 17); "
               f"$d.Close(0) }} finally {{ $w.Quit() }}")
    subprocess.run(["powershell", "-NoProfile", "-Command", comando], check=True)
    print("  generado informe\\entregable3_documento.pdf")


# ---------------------------------------------------------------- base de datos
def entidad_relacion():
    """Diagrama entidad-relación conceptual (notación de Chen): entidades, relaciones y cardinalidades."""
    import svg
    from diagramas_uml import Uml
    svg.CARPETA = carpeta("base_de_datos")
    svg.FUENTES = os.path.join(E3_ARCHIVOS, "fuentes")
    s = Uml(1700, 900)
    pos = {"CONTRIBUYENTE": (60, 80), "PERIODO": (520, 80), "DETERMINACION": (980, 80), "CATEGORIA NRUS": (1440, 80),
           "EMISOR": (60, 420), "FACTURA COMPRA": (520, 420), "REPORTE MENSUAL": (60, 270), "AVISO": (980, 270),
           "PARAMETRO VERSION": (1440, 420), "IMAGEN FACTURA": (60, 760), "CAMPO EXTRAIDO": (520, 760),
           "MODELO LOCAL": (980, 760), "CRONOGRAMA VENC": (1440, 760)}
    w, h = 200, 60
    centro = {k: (x + w / 2, y + h / 2) for k, (x, y) in pos.items()}
    cardinalidades = []  # se dibujan al final para que las entidades no las tapen

    def relacion(a, b, verbo, ca, cb):
        (x1, y1), (x2, y2) = centro[a], centro[b]
        mx, my = (x1 + x2) / 2, (y1 + y2) / 2
        s.line(x1, y1, mx, my, stroke="#404040", sw=1.6)
        s.line(mx, my, x2, y2, stroke="#404040", sw=1.6)
        dw, dh = 62 + len(verbo) * 3.2, 34
        s.polygon([(mx - dw, my), (mx, my - dh), (mx + dw, my), (mx, my + dh)], fill="#fdf0e6", stroke="#c55a11", sw=1.6)
        s.text(mx, my + 5, verbo, size=13, color="#c55a11", bold=True, anchor="middle")
        for (xa, ya), c in (((x1, y1), ca), ((x2, y2), cb)):
            # justo fuera del borde de la entidad, sobre la línea hacia el rombo
            dx, dy = mx - xa, my - ya
            largo = (dx * dx + dy * dy) ** 0.5
            ux, uy = dx / largo, dy / largo
            borde = min(w / 2 / abs(ux) if ux else 1e9, h / 2 / abs(uy) if uy else 1e9)
            cardinalidades.append((xa + ux * (borde + 30), ya + uy * (borde + 30), c))

    relacion("CONTRIBUYENTE", "PERIODO", "declara", "1", "N")
    relacion("PERIODO", "DETERMINACION", "resulta en", "1", "1")
    relacion("DETERMINACION", "CATEGORIA NRUS", "se ubica en", "N", "1")
    relacion("PARAMETRO VERSION", "CATEGORIA NRUS", "define", "1", "N")
    relacion("PARAMETRO VERSION", "CRONOGRAMA VENC", "publica", "1", "N")
    relacion("PERIODO", "FACTURA COMPRA", "contiene", "1", "N")
    relacion("EMISOR", "FACTURA COMPRA", "emite", "1", "N")
    relacion("FACTURA COMPRA", "IMAGEN FACTURA", "respalda", "1", "1")
    relacion("FACTURA COMPRA", "CAMPO EXTRAIDO", "produce", "1", "N")
    relacion("MODELO LOCAL", "CAMPO EXTRAIDO", "lee", "1", "N")
    relacion("PERIODO", "REPORTE MENSUAL", "resume", "1", "N")
    relacion("PERIODO", "AVISO", "programa", "1", "N")
    for k, (x, y) in pos.items():
        s.rect(x, y, w, h, fill="#1f4e79", stroke="#1f4e79", sw=1.6, rx=4)
        s.text(x + w / 2, y + 36, k, size=15, color="#fff", bold=True, anchor="middle")
    for lx, ly, c in cardinalidades:
        s.rect(lx - 12, ly - 13, 24, 22, fill="#fff", stroke="none")
        s.text(lx, ly + 4, c, size=14, color="#c00000", bold=True, anchor="middle")
    s.text(60, 880, "Notación de Chen: rectángulo = entidad, rombo = relación; 1 y N = cardinalidad máxima. "
                    "Los atributos están en la BD lógica y la física.", size=13, color="#7f7f7f")
    svg.guardar(s, "entidad_relacion")


def base_datos():
    print("Base de datos")
    copiar(os.path.join(E3_ARCHIVOS, "bd_logico.png"), "base_de_datos", "bd_logica.png")
    copiar(os.path.join(E3_ARCHIVOS, "bd_fisico_registro.png"), "base_de_datos", "bd_fisica_1_registro.png")
    copiar(os.path.join(E3_ARCHIVOS, "bd_fisico_nrus.png"), "base_de_datos", "bd_fisica_2_nrus.png")
    lineas = ["-- Script físico de la base local de Facturas S20 (SQLite; cifrada con SQLCipher en el teléfono).",
              "-- Generado desde el esquema v1 que exporta Room (android/app/schemas/.../1.json).", ""]
    for e in esquema():
        lineas.append(e["createSql"].replace("${TABLE_NAME}", e["tableName"]) + ";")
        for i in e.get("indices", []):
            lineas.append(i["createSql"].replace("${TABLE_NAME}", e["tableName"]) + ";")
        lineas.append("")
    with open(os.path.join(carpeta("base_de_datos"), "bd_fisica_script.sql"), "w", encoding="utf-8") as f:
        f.write("\n".join(lineas))
    print("  generado base_de_datos\\bd_fisica_script.sql")
    entidad_relacion()


# ---------------------------------------------------------------- anexos
LEAN = {
    "1. Problema": ["Clasificar y sumar a mano las facturas del mes toma horas y produce errores.",
                    "Si el monto queda por debajo del real, subdeclara; si queda por encima, paga de más.",
                    "Las facturas se pierden y el titular queda sin sustento.",
                    "Hoy: suma a mano, cuaderno u hoja de cálculo, contador externo."],
    "4. Solución": ["Foto de la factura cuando llega el pedido.",
                    "Gemma 4 E2B ajustado lee RUC, emisor, serie, número, fecha e importe en el teléfono.",
                    "Acumulado mensual, categoría y cuota.", "Aviso de vencimiento y reporte mensual."],
    "3. Propuesta de valor única": ["Fotografía la factura cuando te llega el pedido y Facturas S20 te dice cuánto "
                                    "compraste en el mes y qué cuota del NRUS te toca pagar, incluso sin internet."],
    "9. Ventaja competitiva": ["Modelo ajustado con facturas peruanas que corre en el teléfono.",
                               "Conjunto propio de facturas etiquetadas.", "Motor de reglas del NRUS.",
                               "Sin costo por documento ni envío de datos a terceros."],
    "2. Segmento de clientes": ["Titulares de bodegas del NRUS con un establecimiento y un Android de gama media.",
                                "Primeros usuarios: bodegueros de 25 a 45 años que ya usan billetera móvil."],
    "8. Métricas clave": ["Bodegas activas en 30 días.", "Facturas por bodega y mes.", "Exactitud por campo.",
                          "Tiempo de lectura.", "Campos corregidos a mano."],
    "5. Canales": ["Google Play gratuito.", "Demostración en bodegas.", "Distribuidores y asociaciones de bodegueros."],
    "7. Estructura de costos": ["Desarrollo y mantenimiento de la app.", "Etiquetado del conjunto de facturas.",
                                "Cómputo para el ajuste fino y la conversión.", "Distribución del modelo (sin servidor)."],
    "6. Flujo de ingresos": ["App gratuita en su versión base.", "Talleres para gremios de bodegueros.",
                             "Licencia para estudios contables.", "Reportes anonimizados con consentimiento."],
}


def lienzo(titulo, subtitulo, celdas, alto_filas, salida, alto):
    """Lienzo de 5 columnas: celdas = [(título, contenido html, columna, fila)] con columna/fila en sintaxis CSS."""
    cajas = "".join(f"<div class='caja' style='grid-column:{c};grid-row:{r}'><h3>{html.escape(t)}</h3>{x}</div>"
                    for t, x, c, r in celdas)
    cuerpo = (f"<div style='padding:28px'><h1>{html.escape(titulo)}</h1><div class='sub'>{html.escape(subtitulo)}"
              f"</div><div style='display:grid;grid-template-columns:repeat(5,1fr);grid-template-rows:{alto_filas};"
              f"gap:10px'>{cajas}</div><div class='pie'>Elaboración propia.</div></div>")
    chrome(cuerpo, salida, 1500, alto)


def lean_canvas():
    pos = {"1. Problema": ("1", "1/3"), "4. Solución": ("2", "1"), "8. Métricas clave": ("2", "2"),
           "3. Propuesta de valor única": ("3", "1/3"), "9. Ventaja competitiva": ("4", "1"), "5. Canales": ("4", "2"),
           "2. Segmento de clientes": ("5", "1/3"), "7. Estructura de costos": ("1/3", "3"),
           "6. Flujo de ingresos": ("3/6", "3")}
    lienzo("Lean Canvas de Facturas S20", "Numeración según el orden de llenado de Maurya (2012)",
           [(t, lista(LEAN[t]), c, r) for t, (c, r) in pos.items()], "auto auto auto",
           os.path.join(carpeta("anexo"), "lean_canvas.png"), 700)

CHARTER = [
    ("Proyecto", "Facturas S20: " + TITULO),
    ("Patrocinador", "Ing. Carlos Alberto Effio Gonzáles (docente del curso)"),
    ("Líder del proyecto", "Diego Aarón Damián Valdivia"),
    ("Equipo", "Paulo Coronel Obregón · Morán Mateo Velásquez · Fabrizzio Jhoel Gonzales Maco"),
    ("Propósito", "Reducir el costo de registrar las facturas de compra de la bodega del NRUS para que la categoría "
                  "y la cuota se determinen con el monto real."),
    ("Objetivo", "Desarrollar una aplicación móvil con IA multimodal (Gemma 4) para mejorar la exactitud de la "
                 "declaración mensual del NRUS en bodegas de S.J.L."),
    ("Alcance", "Captura y lectura de facturas en el teléfono, verificación, acumulado mensual, categoría y cuota, "
                "vencimiento, reporte PDF, historial, instalación del modelo y exportación de datos."),
    ("Fuera del alcance", "Presentación ante la SUNAT, emisión de comprobantes, contabilidad completa e iOS."),
    ("Entregables", "APF1 (sem. 4), APF2 (sem. 8), APF3 (sem. 12), informe final y sustentación (sem. 18), app "
                    "Android y modelo .litertlm."),
    ("Criterio de éxito", "El total y la categoría coinciden con el cálculo de control; exactitud ≥ 90 % por campo; "
                          "lectura ≤ 20 s en el teléfono de referencia."),
    ("Interesados", "Titulares de bodegas del NRUS, contadores externos, docente del curso y equipo del proyecto."),
    ("Supuestos", "Facturas reales disponibles para etiquetar; una PC con GPU para el ajuste fino; un teléfono de "
                  "gama media para las pruebas."),
    ("Restricciones", "18 semanas; S/ 0 en licencias; al menos 50 % del código en Java; sin servidor."),
    ("Riesgos principales", "Ajuste fino sin mejora suficiente; pérdida de exactitud al convertir el modelo; "
                            "lectura lenta en gama media."),
    ("Presupuesto", "S/ 0 en licencias (herramientas abiertas y gratuitas). Opcional: cuenta de Google Play (USD 25)."),
]


def charter():
    filas = "".join(f"<tr><td style='width:190px;font-weight:bold;color:#1f4e79'>{html.escape(a)}</td>"
                    f"<td>{html.escape(b)}</td></tr>" for a, b in CHARTER)
    cuerpo = (f"<div style='padding:28px'><h1>Project Charter</h1><div class='sub'>Acta de constitución del proyecto"
              f"</div><table style='font-size:15px'><tr><th>Campo</th><th>Contenido</th></tr>{filas}</table>"
              f"<div class='pie'>Elaboración propia con base en la guía del PMBOK (Project Management Institute, "
              f"2021).</div></div>")
    chrome(cuerpo, os.path.join(carpeta("anexo"), "charter.png"), 1300, 700)


FODA = {
    ("Fortalezas", "#1e7b4f", "#e3f1e8"): [
        "La IA corre dentro del teléfono: funciona sin internet y las facturas no salen del equipo.",
        "Modelo Gemma 4 ajustado con facturas peruanas reales.",
        "Motor de reglas del NRUS probado (cobertura de pruebas ≥ 80 %) y parámetros versionados.",
        "Base cifrada con SQLCipher y acceso con PIN o huella.",
        "Sin costo por documento ni servidor que mantener; app gratuita."],
    ("Debilidades", "#c55a11", "#fdf0e6"): [
        "La lectura automática necesita unos 6 GB de RAM; en equipos menores se registra a mano.",
        "Conjunto de entrenamiento pequeño (52 fotos): la exactitud aún no llega al 90 % en todos los campos.",
        "La lectura tarda más de 20 s por factura en el teléfono de referencia.",
        "El modelo pesa varios GB y se descarga una vez.",
        "Equipo reducido y plazo de 18 semanas."],
    ("Oportunidades", "#1f4e79", "#dce6f2"): [
        "Gran número de bodegas acogidas al NRUS que no llevan registros.",
        "Digitalización tributaria impulsada por la SUNAT (comprobantes electrónicos).",
        "Modelos abiertos cada vez más capaces y livianos para teléfonos.",
        "Alianzas con distribuidores, gremios de bodegueros y estudios contables.",
        "Teléfonos Android de gama media cada vez más extendidos."],
    ("Amenazas", "#c00000", "#fbe9e9"): [
        "Cambios en las normas del NRUS (categorías, cuotas o topes).",
        "Aplicaciones de la propia SUNAT o de terceros que cubran el mismo problema.",
        "Baja adopción tecnológica y desconfianza del titular.",
        "Facturas en mal estado o con formatos nuevos que el modelo no conoce.",
        "Pérdida del teléfono sin haber exportado los datos."],
}


def foda():
    cajas = "".join(f"<div class='caja' style='background:{fondo};border-color:{color}'>"
                    f"<h3 style='color:{color};font-size:20px'>{html.escape(t)}</h3>{lista(items)}</div>"
                    for (t, color, fondo), items in FODA.items())
    cuerpo = (f"<div style='padding:28px'><h1>Análisis FODA de Facturas S20</h1><div class='sub'>Arriba, factores "
              f"internos (fortalezas y debilidades); abajo, externos (oportunidades y amenazas)</div>"
              f"<div style='display:grid;grid-template-columns:1fr 1fr;gap:14px'>{cajas}</div>"
              f"<div class='pie'>Elaboración propia.</div></div>")
    chrome(cuerpo.replace("font-size:14px", "font-size:15px"), os.path.join(carpeta("anexo"), "foda.png"), 1300, 560)


BMC = [
    ("Asociaciones clave", "Google (Gemma, LiteRT-LM, Google Play); distribuidores mayoristas; asociaciones de "
                           "bodegueros; estudios contables."),
    ("Actividades clave", "Recolección y etiquetado de facturas; ajuste fino y conversión del modelo; desarrollo de "
                          "la app; actualización de parámetros del NRUS."),
    ("Recursos clave", "Modelo Gemma 4 E2B ajustado; conjunto propio de facturas etiquetadas; motor de reglas del "
                       "NRUS; equipo de desarrollo Java."),
    ("Propuesta de valor", "Registrar una factura con una foto, sin internet y sin que los datos salgan del teléfono, "
                           "y saber en todo momento cuánto se compró en el mes y qué cuota corresponde."),
    ("Relación con el cliente", "Autoservicio con guía en la app; recordatorios de vencimiento; soporte por "
                                "mensajería en la adopción."),
    ("Canales", "Google Play; demostraciones en bodegas; distribuidores y asociaciones; grupos vecinales."),
    ("Segmentos de clientes", "Titulares de bodegas del NRUS con un Android de gama media. Secundario: estudios "
                              "contables."),
    ("Estructura de costos", "Desarrollo y mantenimiento de la app; cómputo para el ajuste fino; distribución del "
                             "modelo; captación en campo y soporte. Sin costo de inferencia por documento."),
    ("Fuentes de ingresos", "App gratuita en su versión base; talleres para gremios; licencia para estudios "
                            "contables; reportes anonimizados con consentimiento."),
]


def bmc():
    pos = [("1", "1/3"), ("2", "1"), ("2", "2"), ("3", "1/3"), ("4", "1"), ("4", "2"), ("5", "1/3"), ("1/3", "3"),
           ("3/6", "3")]
    lienzo("Business Model Canvas de Facturas S20", "Lienzo de Osterwalder y Pigneur (2010)",
           [(t, f"<div style='font-size:14px'>{html.escape(x)}</div>", c, r) for (t, x), (c, r) in zip(BMC, pos)],
           "auto auto auto", os.path.join(carpeta("anexo", "otros"), "business_model_canvas.png"), 640)

def anexos():
    print("Anexos")
    lean_canvas()
    charter()
    foda()
    bmc()
    for origen, nombre in (("wbs.png", "wbs.png"), ("casos_uso.png", "casos_de_uso.png"),
                           ("arquitectura_capas.png", "arquitectura_por_capas.png"),
                           ("arquitectura_despliegue.png", "diagrama_de_despliegue.png"),
                           ("clases_dominio.png", "diagrama_de_clases_dominio.png")):
        copiar(os.path.join(E2_ARCHIVOS, origen), "anexo", "otros", nombre)
    copiar(os.path.join(E3_ARCHIVOS, "gantt_semana12.png"), "anexo", "otros", "gantt_semana12.png")


# ---------------------------------------------------------------- proyecto y GitHub
def proyecto():
    print("Proyecto")
    copiar(APK, "proyecto", "facturas_s20.apk")
    archivos = subprocess.run(["git", "ls-files", "android"], cwd=RAIZ, check=True, capture_output=True,
                              text=True, encoding="utf-8").stdout.split("\n")
    salida = os.path.join(carpeta("proyecto"), "facturas_s20_proyecto_intellij.zip")
    with zipfile.ZipFile(salida, "w", zipfile.ZIP_DEFLATED) as z:
        for a in filter(None, archivos):
            z.write(os.path.join(RAIZ, a), os.path.join("facturas_s20", os.path.relpath(a, "android")))
    print("  generado proyecto\\facturas_s20_proyecto_intellij.zip", f"({len(archivos)} archivos)")
    with open(os.path.join(carpeta("github"), "link.txt"), "w", encoding="utf-8") as f:
        f.write(f"Repositorio del proyecto Facturas S20 (código de la app Android en la carpeta android/):\n"
                f"{REPOSITORIO}\n")
    with open(os.path.join(carpeta("github"), "repositorio.url"), "w", encoding="utf-8") as f:
        f.write(f"[InternetShortcut]\nURL={REPOSITORIO}\n")
    print("  generado github\\link.txt")


# ---------------------------------------------------------------- diccionario de datos
DESCRIPCION_TABLAS = {
    "contribuyente": "Negocio dueño de la app (un solo usuario por teléfono).",
    "periodo": "Mes tributario del contribuyente; agrupa sus facturas y sus ventas.",
    "emisor": "Proveedor que emite las facturas de compra.",
    "factura_compra": "Factura de compra registrada con IA o a mano.",
    "imagen_factura": "Foto cifrada que respalda una factura.",
    "campo_extraido": "Cada campo que leyó el modelo y su valor final tras la verificación.",
    "modelo_local": "Versiones del modelo de IA instaladas en el teléfono.",
    "parametro_version": "Versión de los parámetros del NRUS (umbral de aviso y tope anual).",
    "categoria_nrus": "Categorías del NRUS de una versión de parámetros.",
    "cronograma_venc": "Fechas límite de declaración por mes y último dígito del RUC.",
    "determinacion": "Resultado del mes: monto que decide, categoría, cuota y vencimiento.",
    "aviso": "Notificaciones programadas para un mes.",
    "reporte_mensual": "Reportes PDF generados para un mes.",
}
DESCRIPCION = {
    "id_contribuyente": "Identificador del contribuyente.", "ruc": "RUC de 11 dígitos (validado con módulo 11).",
    "nombre": "Nombre comercial del negocio.", "titular": "Nombre del titular.",
    "ultimo_digito": "Último dígito del RUC; define la fecha de vencimiento.",
    "pin_hash": "Hash PBKDF2-HMAC-SHA256 del PIN (nunca el PIN).", "pin_sal": "Sal aleatoria del hash del PIN.",
    "fecha_alta": "Fecha de configuración de la app.", "id_periodo": "Identificador del mes.",
    "anio": "Año del periodo.", "mes": "Mes del periodo (1 a 12).",
    "total_ventas": "Ventas del mes en céntimos; vacío si aún no se ingresan.",
    "estado": "Estado del registro (ver valores).", "id_emisor": "Identificador del proveedor.",
    "razon_social": "Razón social impresa en la factura.", "id_factura": "Identificador de la factura.",
    "serie": "Serie del comprobante (p. ej. F001).", "numero": "Número del comprobante tal como se leyó.",
    "numero_normalizado": "Número sin ceros a la izquierda; sirve para detectar duplicados.",
    "fecha_emision": "Fecha de emisión (ISO-8601).", "moneda": "Moneda: PEN o USD.",
    "importe_total": "Importe total en céntimos.", "origen": "IA (leída por el modelo) o MANUAL.",
    "motivo_anulacion": "Motivo si la factura se anuló.", "fecha_registro": "Fecha y hora en que se guardó.",
    "id_imagen": "Identificador de la imagen.", "ruta_cifrada": "Archivo JPEG cifrado con AES-GCM en la app.",
    "nitidez": "Nitidez medida al capturar.", "fecha_captura": "Fecha y hora de la foto.",
    "id_campo": "Identificador del campo leído.", "id_modelo": "Modelo que leyó el campo; vacío si fue manual.",
    "nombre_campo": "Campo de la factura (ruc, razon_social, serie, numero, fecha_emision, moneda, importe_total).",
    "valor_leido": "Valor que devolvió el modelo.", "valor_final": "Valor guardado tras la verificación.",
    "confianza": "Confianza del campo (0 a 1).", "corregido": "1 si el usuario corrigió el valor leído.",
    "version": "Versión (del modelo o de los parámetros).", "tamano_mb": "Tamaño del archivo del modelo en MB.",
    "sha256": "Huella SHA-256 del archivo, para verificarlo.", "ruta": "Ruta del archivo del modelo en el teléfono.",
    "fecha_instalacion": "Fecha de instalación del modelo.", "id_parametro": "Identificador de la versión.",
    "umbral_aviso": "Fracción del límite para avisar (0,80).", "tope_anual": "Tope anual del régimen en céntimos.",
    "vigente_desde": "Fecha desde la que rige.", "activo": "1 si es la versión vigente.",
    "id_categoria": "Categoría del NRUS.", "codigo": "Número de la categoría (1 o 2).",
    "limite_mensual": "Límite mensual de la categoría en céntimos.", "cuota": "Cuota mensual en céntimos.",
    "id_cronograma": "Identificador de la fecha límite.", "fecha_limite": "Último día para declarar y pagar.",
    "id_determinacion": "Identificador de la determinación.",
    "total_adquisiciones": "Suma de las facturas vigentes del mes en céntimos.",
    "monto_determinante": "El mayor entre compras y ventas; decide la categoría.",
    "fecha_vencimiento": "Fecha límite de pago del mes.", "nivel_alerta": "NINGUNA, AVISO_80, LIMITE_100 o FUERA_DE_REGIMEN.",
    "aviso_tope_anual": "1 si las compras o ventas del año se acercan al tope anual.",
    "fecha_calculo": "Fecha y hora del cálculo.", "id_aviso": "Identificador del aviso.",
    "tipo": "TOPE_80, TOPE_100, TOPE_ANUAL o VENCIMIENTO.", "fecha_programada": "Cuándo se muestra el aviso.",
    "enviado": "1 si ya se mostró.", "id_reporte": "Identificador del reporte.", "ruta_pdf": "Archivo PDF generado.",
    "fecha_generacion": "Fecha y hora de generación.",
}
ESTADOS = {"periodo": "ABIERTO o CERRADO.", "factura_compra": "VIGENTE o ANULADA.",
           "modelo_local": "NO_INSTALADO, DESCARGANDO, VERIFICADO o ACTIVO."}


def diccionario():
    print("Diccionario de datos")
    partes = [f"<h1>Diccionario de datos</h1><div class='sub'>Base local de Facturas S20 · SQLite con Room, cifrada "
              f"con SQLCipher · 13 tablas · esquema v1</div>"
              f"<p style='font-size:13px'>Los montos se guardan en céntimos (INTEGER) y en Java se manejan como "
              f"BigDecimal de escala 2; las fechas se guardan como texto ISO-8601. PK = clave primaria, FK = clave "
              f"foránea, UQ = valor único.</p>"]
    for e in esquema():
        t = e["tableName"]
        pk = set(e["primaryKey"]["columnNames"])
        fks = {c: k["table"] for k in e.get("foreignKeys", []) for c in k["columns"]}
        unicas = {i["columnNames"][0] for i in e.get("indices", []) if i.get("unique") and len(i["columnNames"]) == 1}
        filas = []
        for f in e["fields"]:
            c = f["columnName"]
            clave = "PK" if c in pk else ("FK → " + fks[c]) if c in fks else "UQ" if c in unicas else ""
            desc = DESCRIPCION.get(c, "")
            if c == "estado":
                desc = "Estado: " + ESTADOS.get(t, "")
            filas.append(f"<tr><td><b>{c}</b></td><td>{f['affinity']}</td><td>{'Sí' if f.get('notNull') else 'No'}"
                         f"</td><td>{html.escape(clave)}</td><td>{html.escape(desc)}</td></tr>")
        indices = "; ".join(("UNIQUE " if i.get("unique") else "") + "(" + ", ".join(i["columnNames"]) + ")"
                            for i in e.get("indices", []))
        partes.append(f"<div style='break-inside:avoid'><h2>{t}</h2><p style='font-size:13px;margin:0 0 6px'>"
                      f"{html.escape(DESCRIPCION_TABLAS[t])}</p><table><tr><th style='width:22%'>Columna</th>"
                      f"<th style='width:10%'>Tipo</th><th style='width:8%'>Obligatorio</th>"
                      f"<th style='width:18%'>Clave</th><th>Descripción</th></tr>{''.join(filas)}</table>"
                      f"<p class='pie'>Índices: {html.escape(indices) or 'solo la clave primaria'}</p></div>")
    chrome("<div style='padding:10px 18px'>" + "".join(partes) + "</div>",
           os.path.join(carpeta("diccionario_de_datos"), "diccionario_de_datos.pdf"))


# ---------------------------------------------------------------- manual de usuario
MANUAL = [
    ("1. Antes de empezar", "Facturas S20 funciona en teléfonos Android 8.0 o superior. Para leer las facturas con IA "
     "el teléfono necesita unos 6 GB de RAM y 3 GB libres; si no los tiene, puede registrar las facturas a mano. "
     "Todos los datos se guardan cifrados en el teléfono y la app no envía nada por internet.", []),
    ("2. Configurar el negocio y entrar", "La primera vez, escriba el RUC de su negocio (la app comprueba que sea "
     "válido), su nombre y el del titular. Luego cree un PIN de 4 dígitos y repítalo. Si su teléfono tiene huella, "
     "puede activarla. Las siguientes veces entre con el PIN o la huella; tras 3 intentos fallidos la app espera "
     "30 segundos.", [("p01_configuracion.png", "Configuración"), ("p01b_crear_pin.png", "Crear PIN"),
                      ("p02_acceso.png", "Acceso")]),
    ("3. Preparar el lector de facturas", "Desde Ajustes › Lector, la app comprueba la memoria y el espacio del "
     "teléfono y descarga el modelo de IA una sola vez (conéctese a Wi-Fi). Al terminar verifica el archivo; desde "
     "ese momento las facturas se leen sin internet.", [("p03_preparar_modelo.png", "Comprobación"),
                                                        ("p03b_modelo_instalado.png", "Modelo instalado")]),
    ("4. Pantalla de inicio", "Muestra lo que compró en el mes, la categoría y la cuota que le corresponden, la barra "
     "que indica cuánto falta para el límite de su categoría y la fecha de vencimiento. El botón central Escanear "
     "inicia el registro de una factura.", [("p04_inicio.png", "Inicio")]),
    ("5. Registrar una factura con la cámara", "Pulse Escanear, encuadre la factura dentro del marco y tome la foto "
     "cuando la app indique que se ve nítida. El modelo lee los datos en el teléfono. Revise los campos: los que "
     "aparecen en ámbar tienen baja confianza; tóquelos para corregirlos comparando con el papel. Pulse Guardar "
     "factura. Si la factura ya estaba registrada, la app le avisa.",
     [("p06_camara.png", "Cámara"), ("p07b_lectura_gemma.png", "Lectura"), ("p08_verificacion.png", "Verificación"),
      ("p10_guardada.png", "Guardada")]),
    ("6. Registrar una factura a mano", "Si su teléfono no puede leer con IA o la foto no es clara, use Escribir los "
     "datos a mano: complete RUC, serie, número, fecha e importe. Si el proveedor ya existe, la razón social se "
     "completa sola. Puede adjuntar una foto como respaldo.", [("p19_registro_manual.png", "Registro manual"),
                                                               ("p09_duplicado.png", "Aviso de duplicado")]),
    ("7. Revisar y anular facturas", "En Facturas vea las del mes, filtre por leídas con IA, manuales o anuladas, y "
     "busque por proveedor, RUC o número. Toque una factura para ver su detalle y su foto. Si se registró por error, "
     "anúlela indicando el motivo: deja de sumar en el mes.", [("p11_facturas.png", "Facturas del mes"),
                                                               ("p12_detalle.png", "Detalle"),
                                                               ("p12b_anulacion.png", "Anulación")]),
    ("8. Ventas, categoría y vencimiento", "En Mes ingrese el total de sus ventas del mes. La app compara compras y "
     "ventas, toma el mayor y le muestra su categoría y su cuota, con avisos al llegar al 80 % y al 100 % del "
     "límite. En Vencimiento verá hasta qué día puede declarar y pagar según el último dígito de su RUC.",
     [("p13_ventas.png", "Ventas"), ("p14_categoria.png", "Categoría y cuota"), ("p15_vencimiento.png", "Vencimiento")]),
    ("9. Reporte del mes y cierre", "En Reporte genere el PDF del mes con el resumen y la lista de facturas, y "
     "compártalo con su contador o guárdelo. En Historial cierre el mes cuando ya declaró: un mes cerrado ya no "
     "admite cambios.", [("p16_reporte.png", "Reporte"), ("p17_historial.png", "Historial"),
                         ("p17b_cerrar_mes.png", "Cerrar mes")]),
    ("10. Ajustes y exportación de datos", "En Ajustes puede cambiar el PIN, activar la huella, volver a mostrar la "
     "guía de captura y administrar el lector. Exportar datos genera un archivo ZIP con sus facturas en CSV (se "
     "abre en Excel), las fotos y el reporte; guárdelo en un lugar seguro, porque si pierde el teléfono sin "
     "exportar, pierde los datos.", [("p18_ajustes.png", "Ajustes"), ("p20_exportar.png", "Exportar datos")]),
]


def manual():
    print("Manual de usuario")
    portada = (f"<div style='height:1000px;display:flex;flex-direction:column;justify-content:center;"
               f"text-align:center;page-break-after:always'><h1 style='font-size:44px'>Facturas S20</h1>"
               f"<div style='font-size:22px;color:#c00000;margin:10px 0 30px'>Manual de usuario</div>"
               f"<div style='font-size:16px;color:#404040'>{html.escape(TITULO)}</div>"
               f"<div class='sub' style='margin-top:40px'>Versión 0.1 · Universidad Tecnológica del Perú · 2026</div></div>")
    secciones = []
    for titulo, texto, capturas in MANUAL:
        imagenes = "".join(
            f"<figure style='margin:0;text-align:center;width:150px'><img src='file:///"
            f"{os.path.join(CAPTURAS, a).replace(chr(92), '/')}' style='width:150px;border:1px solid #a6a6a6;"
            f"border-radius:6px'><figcaption style='font-size:11px;color:#1f4e79;font-weight:bold'>{html.escape(l)}"
            f"</figcaption></figure>" for a, l in capturas)
        secciones.append(f"<div style='break-inside:avoid'><h2>{html.escape(titulo)}</h2><p style='font-size:14px;"
                         f"line-height:1.45'>{html.escape(texto)}</p><div style='display:flex;gap:14px;"
                         f"flex-wrap:wrap'>{imagenes}</div></div>")
    chrome(portada + "<div style='padding:0 20px'>" + "".join(secciones) + "</div>",
           os.path.join(carpeta("manual_de_usuario"), "manual_de_usuario.pdf"))


# ---------------------------------------------------------------- fuentes y prototipos
def fuentes():
    print("Fuentes")
    for sub, lista_pdf in FUENTES.items():
        for nombre, url in lista_pdf:
            ruta = os.path.join(carpeta("fuentes", sub), nombre)
            if os.path.exists(ruta):
                continue
            pedido = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
            with urllib.request.urlopen(pedido, timeout=180) as r:
                datos = r.read()
            if not datos.startswith(b"%PDF"):
                raise RuntimeError(f"{url} no devolvió un PDF")
            with open(ruta, "wb") as f:
                f.write(datos)
            print("  descargado", os.path.relpath(ruta, DESTINO))


def prototipos():
    print("Prototipos")
    nombres = sorted(n for n in os.listdir(E2_ARCHIVOS) if n.startswith(("pantalla_", "prototipo_hoja_")))
    for n in nombres + ["ui_mapa_navegacion.png", "ui_sistema_diseno.png"]:
        copiar(os.path.join(E2_ARCHIVOS, n), "prototipos", "imagenes_prototipo", n)


def main():
    os.makedirs(DESTINO, exist_ok=True)
    informe()
    base_datos()
    anexos()
    proyecto()
    diccionario()
    manual()
    fuentes()
    prototipos()


if __name__ == "__main__":
    main()
