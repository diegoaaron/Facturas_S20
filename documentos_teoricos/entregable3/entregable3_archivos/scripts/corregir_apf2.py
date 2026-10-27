"""Genera la copia corregida del informe del APF2 para el entregable 3.

Parte de entregable2/solo_entregable2_documento.docx, que ya incorpora las correcciones de la revisión docente
(entregable2/observaciones_profesor_entregable/revision_entregable2_.docx) hechas en los scripts del entregable 2:
citas APA, formulación del problema retirada, objetivo general dictado por el docente, objetivos específicos con
indicador y leyendas APA de tablas y figuras.

Lo que faltaba de la observación «REVISAR NORMAS APA» de la Figura 1 era que varios diagramas llevaban su título
dentro de la imagen, repitiendo el que va arriba en cursiva. Este script recorta esa franja de título de las imágenes
(y ajusta su alto en el Word) sin tocar los archivos del entregable 2.

Además retira la prueba de usabilidad con tres titulares, que se descartó el 2026-10-27 (la app se prueba solo en el
teléfono de referencia; ver roadmap/001, I4 paso 8): su mención en 2.1.9, 3.6.9, la verificación de RNF, el
cronograma, la cuarta conclusión, el RNF-11 y el Anexo S.2, junto con las tablas de metas y de tareas y la cita de
Brooke (1996). Las tablas siguientes se renumeran.

Uso (Python de Windows con lxml y Pillow):
    python corregir_apf2.py
Salida: documentos_teoricos/entregable3/entregable3_apf2_corregido.docx
"""
import io
import os
import re
import zipfile

from lxml import etree
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
TEORICOS = os.path.normpath(os.path.join(AQUI, "..", "..", ".."))
ORIGEN = os.path.join(TEORICOS, "entregable2", "solo_entregable2_documento.docx")
SALIDA = os.path.join(TEORICOS, "entregable3", "entregable3_apf2_corregido.docx")

W = "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}"
A = "{http://schemas.openxmlformats.org/drawingml/2006/main}"
R = "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}"
WP = "{http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing}"

# Figuras cuyo título está dibujado dentro de la imagen (numeración del informe solo APF2).
FIGURAS_CON_TITULO = {1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 15, 16, 17, 18, 19, 22, 26, 27}
UMBRAL_BLANCO = 245   # un píxel con los tres canales por encima se toma como fondo
HUECO_MINIMO = 12     # filas de fondo seguidas que separan el título del contenido (imágenes a 2×)
MARGEN = 16           # filas de fondo que se dejan sobre el contenido


def es_fondo(fila):
    return all(min(p) > UMBRAL_BLANCO for p in fila)


def corte_titulo(im):
    """Devuelve la fila donde empieza el contenido debajo del título, o None si no se reconoce un título."""
    im = im.convert("RGB")
    w, h = im.size
    datos = im.load()
    filas = [es_fondo([datos[x, y] for x in range(0, w, 2)]) for y in range(min(h, int(h * 0.25)))]
    y = 0
    while y < len(filas) and filas[y]:
        y += 1                                   # margen superior
    inicio_titulo = y
    while y < len(filas) and not filas[y]:
        y += 1                                   # texto del título
    if y - inicio_titulo > 90:                   # demasiado alto para ser una línea de título
        return None
    inicio_hueco = y
    while y < len(filas) and filas[y]:
        y += 1                                   # separación hasta el contenido
    if y - inicio_hueco < HUECO_MINIMO or y >= len(filas):
        return None
    return max(inicio_titulo, y - MARGEN)


# --- Retiro de la prueba de usabilidad -------------------------------------------------------------------------

def texto(e):
    return "".join(t.text or "" for t in e.iter(W + "t")).strip()


def reemplazar(parrafo, viejo, nuevo):
    """Reemplaza un texto que puede estar repartido entre varias corridas; el nuevo hereda el formato de la primera."""
    nodos = list(parrafo.iter(W + "t"))
    completo = "".join(t.text or "" for t in nodos)
    inicio = completo.find(viejo)
    if inicio < 0:
        raise ValueError(f"No se encontró «{viejo[:60]}»")
    fin, pos, puesto = inicio + len(viejo), 0, False
    for t in nodos:
        a, b = pos, pos + len(t.text or "")
        pos = b
        if b <= inicio or a >= fin:
            continue
        antes = t.text[:max(0, inicio - a)]
        despues = t.text[max(0, fin - a):] if fin < b else ""
        t.text = antes + ("" if puesto else nuevo) + despues
        t.set("{http://www.w3.org/XML/1998/namespace}space", "preserve")
        puesto = True


def buscar(cuerpo, inicio):
    return next(i for i, e in enumerate(cuerpo) if texto(e).startswith(inicio))


def quitar(padre, elementos):
    for e in elementos:
        padre.remove(e)


def retirar_prueba_usabilidad(body):
    cuerpo = list(body)
    # 2.1.9: la escala SUS solo se usaba en la prueba.
    reemplazar(cuerpo[buscar(cuerpo, "La calidad de uso se evalúa")],
               "La satisfacción se mide con la escala SUS (System Usability Scale), un cuestionario de diez ítems "
               "propuesto por Brooke (1996). ", "")
    # 3.6.9: el segundo nivel de validación pasa a ser la revisión en el teléfono de referencia.
    reemplazar(cuerpo[buscar(cuerpo, "El prototipo se validó en dos niveles")],
               "Segundo, se definió el protocolo de prueba de usabilidad con tres titulares de bodega sin formación "
               "contable, que se aplicará sobre el prototipo navegable en las semanas 11 y 12 (iteración I4) con las "
               "metas de la Tabla 32. La prueba aún no se ha realizado, por lo que este informe no presenta sus resultados.",
               "Segundo, los requerimientos de usabilidad (registrar una factura con tres toques desde Inicio, objetivos "
               "táctiles de al menos 48 dp, texto de datos de al menos 16 sp y lenguaje sin términos contables) se "
               "verifican en la aplicación construida, sobre el teléfono de referencia.")
    i = buscar(cuerpo, "Metas de la prueba de usabilidad del prototipo")
    quitar(body, cuerpo[i - 1:i + 3])                      # «Tabla 32», título, tabla y nota
    # Verificación de RNF, RNF-11 del anexo y cronograma.
    for fila in list(body.iter(W + "tr")):
        celdas = [texto(c) for c in fila.iter(W + "tc")]
        if celdas[:1] == ["RNF-09 a RNF-11"] and "titulares" in celdas[2]:
            reemplazar(fila, "Prueba con 3 titulares (Tabla 32)",
                       "Revisión en el teléfono de referencia: 3 toques, objetivos ≥ 48 dp, texto ≥ 16 sp y textos "
                       "sin términos contables")
            reemplazar(fila, "I4 (sem. 11–12)", "APF3")
        elif celdas[:1] == ["RNF-11"] and "titulares" in celdas[-1]:
            reemplazar(fila, "Prueba con 3 titulares sin formación contable",
                       "Revisión de los textos de todas las pantallas: cero términos contables")
        elif celdas[1:2] == ["Prueba de usabilidad del prototipo con tres titulares"]:
            fila.getparent().remove(fila)
    cuerpo = list(body)
    reemplazar(cuerpo[buscar(cuerpo, "Cuarta. El prototipo reduce")],
               "; su validación con tres titulares se realizará en las semanas 11 y 12 (iteración I4) con metas definidas.",
               "; ambos aspectos se verifican en la aplicación construida, sobre el teléfono de referencia.")
    # Anexo S.2 completo: título, «Tabla 67», título, tabla, nota y párrafo de participantes.
    i = buscar(cuerpo, "S.2. Protocolo de la prueba de usabilidad")
    quitar(body, cuerpo[i:i + 6])
    cuerpo = list(body)
    quitar(body, [cuerpo[buscar(cuerpo, "Brooke, J. (1996)")]])


def renumerar_tablas(body):
    """Vuelve a numerar las leyendas «Tabla N» y actualiza las referencias del texto."""
    leyendas = [p for p in body if etree.QName(p).localname == "p" and re.fullmatch(r"Tabla \d+", texto(p))]
    nuevo = {int(texto(p).split()[1]): n for n, p in enumerate(leyendas, 1)}
    marca = "Tabla⁠"                             # evita volver a renumerar lo ya cambiado
    for p in body.iter(W + "p"):
        for m in sorted(set(re.findall(r"Tabla (\d+)\b", texto(p))), key=int, reverse=True):
            if nuevo.get(int(m), int(m)) != int(m):
                while re.search(rf"Tabla {m}\b", "".join(t.text or "" for t in p.iter(W + "t"))):
                    reemplazar(p, f"Tabla {m}", f"{marca}{nuevo[int(m)]}")
    for t in body.iter(W + "t"):
        if t.text and marca in t.text:
            t.text = t.text.replace(marca, "Tabla ")


def main():
    zin = zipfile.ZipFile(ORIGEN)
    documento = etree.fromstring(zin.read("word/document.xml"))
    rels = etree.fromstring(zin.read("word/_rels/document.xml.rels"))
    destino = {r.get("Id"): "word/" + r.get("Target") for r in rels}

    # Relaciona cada imagen con su número de figura: la leyenda «Figura N» va dos párrafos antes.
    cuerpo = list(documento.find(W + "body"))
    cambios = {}
    for i, elem in enumerate(cuerpo):
        for dibujo in elem.iter(WP + "inline", WP + "anchor"):
            blip = next(dibujo.iter(A + "blip"), None)
            leyenda = texto(cuerpo[i - 2]) if i >= 2 else ""
            if blip is None or not leyenda.startswith("Figura "):
                continue
            numero = int(leyenda.split()[1])
            if numero not in FIGURAS_CON_TITULO:
                continue
            nombre = destino[blip.get(R + "embed")]
            im = Image.open(io.BytesIO(zin.read(nombre)))
            corte = corte_titulo(im)
            if corte is None:
                print(f"Figura {numero}: no se reconoció el título; se deja igual")
                continue
            recortada = im.crop((0, corte, im.width, im.height))
            salida = io.BytesIO()
            recortada.save(salida, "PNG", optimize=True)
            cambios[nombre] = salida.getvalue()
            proporcion = recortada.height / im.height
            for ext in (dibujo.find(WP + "extent"), next(dibujo.iter(A + "ext"), None)):
                ext.set("cy", str(round(int(ext.get("cy")) * proporcion)))
            print(f"Figura {numero}: título recortado ({corte} px)")

    retirar_prueba_usabilidad(documento.find(W + "body"))
    renumerar_tablas(documento.find(W + "body"))
    print("Prueba de usabilidad retirada y tablas renumeradas")

    os.makedirs(os.path.dirname(SALIDA), exist_ok=True)
    with zipfile.ZipFile(SALIDA, "w", zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            if item.filename == "word/document.xml":
                datos = etree.tostring(documento, xml_declaration=True, encoding="UTF-8", standalone=True)
            else:
                datos = cambios.get(item.filename) or zin.read(item.filename)
            zout.writestr(item, datos)
    print("Guardado en", SALIDA)


if __name__ == "__main__":
    main()
