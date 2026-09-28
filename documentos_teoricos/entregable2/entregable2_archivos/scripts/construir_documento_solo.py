"""Construye documentos_teoricos/entregable2/solo_entregable2_documento.docx a partir de entregable2_documento.docx.

El informe completo es acumulativo (primer avance + segundo avance). Esta versión conserva solo los apartados que exige el
segundo avance según la estructura oficial y la consigna, y del primer avance solo lo que el diseño necesita: contexto,
problema, objetivos, alcance, alternativas, actores y casos de uso, y los anexos A, B, D, F, H e I. Las secciones se eligen
por su título, las tablas y figuras se renumeran y se ajustan las frases que remitían a apartados que no se incluyen.

Uso:  python construir_documento_solo.py   (después de construir_documento.py; luego
      powershell -File actualizar_indice.ps1 -Docx ..\\..\\solo_entregable2_documento.docx)
"""
import os
import re
import zipfile
from xml.sax.saxutils import escape

AQUI = os.path.dirname(os.path.abspath(__file__))
ENTREGABLE = os.path.dirname(os.path.dirname(AQUI))
ORIGEN = os.path.join(ENTREGABLE, "entregable2_documento.docx")
SALIDA = os.path.join(ENTREGABLE, "solo_entregable2_documento.docx")

# Títulos (inicio del texto) cuyas secciones completas, con sus subapartados, no se incluyen.
EXCLUIR = ["1.2.4. ", "1.2.5. ", "2.1.1. ", "2.1.2. ", "2.1.3. ", "2.1.4. ", "2.1.5. ", "2.1.6. ",
           "3.1. Metodología", "3.3. Gestión del proyecto", "3.4.3. ", "3.4.4. ", "3.4.7. ",
           "Anexo C:", "Anexo E:", "Anexo G:", "Anexo J:"]
# Títulos que se conservan sin el texto que tienen antes de su primer subapartado.
SIN_INTRODUCCION = ["2.1. Fundamento teórico"]

TOKEN = re.compile(r'<(/?)(w:[A-Za-z]+)((?:"[^"]*"|[^>"])*?)(/?)>')
T = re.compile(r'(<w:t(?: [^>]*)?>)([^<]*)(</w:t>)')


def elementos(xml):
    """Divide el cuerpo en elementos de primer nivel conservando el XML original."""
    ini = xml.index("<w:body>") + len("<w:body>")
    fin = xml.rindex("</w:body>")
    cuerpo, out, prof, inicio = xml[ini:fin], [], 0, None
    for m in TOKEN.finditer(cuerpo):
        cierre, _, _, auto = m.groups()
        if not cierre:
            if prof == 0:
                inicio = m.start()
            if not auto:
                prof += 1
            elif prof == 0:
                out.append(cuerpo[inicio:m.end()])
        else:
            prof -= 1
            if prof == 0:
                out.append(cuerpo[inicio:m.end()])
    return xml[:ini], out, xml[fin:]


def texto(el):
    return "".join(m.group(2) for m in T.finditer(el))


def nivel(el):
    if not el.startswith("<w:p"):
        return None
    m = re.search(r'<w:pStyle w:val="[^"]*?(?:tulo|Heading)(\d)"', el)
    return int(m.group(1)) if m else None


def seleccionar(els):
    sel, saltar, sin_intro = [], None, False
    for el in els:
        n = nivel(el)
        t = texto(el)
        if n is not None:
            if saltar is not None and n <= saltar:
                saltar = None
            sin_intro = False
            if saltar is None and any(t.startswith(x) for x in EXCLUIR):
                saltar = n
                continue
            if saltar is None and any(t.startswith(x) for x in SIN_INTRODUCCION):
                sel.append(el)
                sin_intro = True
                continue
        if saltar is not None or (sin_intro and n is None and "<w:sectPr" not in el):
            continue
        sel.append(el)
    return sel


def poner_textos(el, nuevos):
    it = iter(nuevos)
    return T.sub(lambda m: m.group(1) + next(it) + m.group(3), el)


def reescribir(els, empieza, nuevo):
    """Reemplaza el texto del párrafo que empieza con `empieza`, conservando el formato del primer run."""
    for i, el in enumerate(els):
        if el.startswith("<w:p") and texto(el).startswith(escape(empieza)):
            ts = [m.group(2) for m in T.finditer(el)]
            el = poner_textos(el, [escape(nuevo)] + [""] * (len(ts) - 1))
            els[i] = el.replace("<w:t>", '<w:t xml:space="preserve">', 1)
            return
    raise SystemExit(f"No se encontró el párrafo «{empieza}»")


def cambiar(els, viejo, nuevo):
    """Reemplaza un texto que está dentro de un solo run (debe aparecer una sola vez)."""
    v, n = escape(viejo), escape(nuevo)
    hits = [(i, k) for i, el in enumerate(els) for k, m in enumerate(T.finditer(el)) if v in m.group(2)]
    if len(hits) != 1:
        raise SystemExit(f"«{viejo}» aparece {len(hits)} veces")
    i, k = hits[0]
    ts = [m.group(2) for m in T.finditer(els[i])]
    ts[k] = ts[k].replace(v, n)
    els[i] = poner_textos(els[i], ts)


def ajustar_textos(els):
    reescribir(els, "Este informe corresponde al segundo avance",
               "Este informe presenta solo lo que corresponde al segundo avance del proyecto final del curso: el diseño de la "
               "solución informática. Incluye los procesos de negocio modelados con BPMN, el diseño lógico y físico de la base "
               "de datos SQLite del teléfono, los diagramas de clases, el prototipo de la interfaz y de los reportes, la "
               "validación del diseño frente al alcance, el cronograma actualizado con su presupuesto y la documentación técnica "
               "para los desarrolladores. Del primer avance se conserva únicamente lo necesario para entender el diseño: el "
               "análisis del contexto, el problema y su impacto en los procesos, los objetivos, el alcance, las alternativas de "
               "solución y el catálogo de actores y casos de uso. Respecto del primer avance se retiró el servidor de respaldo "
               "opcional: toda la información permanece en el teléfono y el usuario la exporta cuando la necesita.")
    reescribir(els, "El informe sigue la estructura del proyecto integrador",
               "El informe sigue la numeración de la estructura oficial del proyecto integrador, por lo que solo aparecen los "
               "apartados que exige este avance. El Capítulo 1 refuerza el contexto de la empresa cliente, el problema y cómo "
               "afecta sus procesos, los objetivos y el alcance de la solución. El Capítulo 2 reúne el fundamento teórico del "
               "diseño: la notación BPMN, el diseño de bases de datos seguras, el diseño de experiencia de usuario y la "
               "documentación técnica. El Capítulo 3 profundiza las alternativas de solución, resume el análisis y desarrolla el "
               "diseño detallado, el diseño del prototipo y la validación. El Capítulo 4 presenta el cronograma actualizado y el "
               "presupuesto, y el Capítulo 5 los resultados del avance. Los anexos conservan la letra que les asigna la "
               "estructura oficial.")
    cambiar(els, "Las fichas técnicas del primer avance se conservan en el Anexo E y las pantallas de las alternativas 2 y 3 en el Anexo F.",
            "Las pantallas de las alternativas 2 y 3 están en el Anexo F.")
    cambiar(els, "3.2, Anexos E y F", "3.2 y Anexo F")
    cambiar(els, "3.4, Anexos G a J", "3.4 y Anexos H e I")
    cambiar(els, "Charter, WBS, Gantt,", "Charter, Gantt,")
    cambiar(els, "3.3, 3.5, Anexos B a D y K a N", "3.5 y Anexos B, D y K a N")
    reescribir(els, "Los anexos A a J reúnen",
               "Los anexos A, B, D, F, H e I conservan las evidencias del primer avance que este avance necesita: el modelo de "
               "negocio, el Project Charter, la línea base del Gantt con el registro de riesgos, las pantallas de las "
               "alternativas y los requerimientos. Los anexos K a T contienen las evidencias del segundo avance. Las imágenes "
               "originales y sus fuentes editables están en la carpeta entregable2_archivos que acompaña al informe.")


def verificar_referencias(els):
    """Falla si algún texto conservado remite a un apartado o anexo que no se incluye."""
    conservados = {texto(e).split(" ")[0] for e in els if nivel(e)}
    patron = re.compile(r"(?:apartados?|Anexos?) ([0-9]\.[0-9](?:\.[0-9]+)?|[A-T])\b")
    faltan = set()
    for e in els:
        if nivel(e) or "<w:sdt" in e:
            continue
        for m in patron.finditer(texto(e)):
            ref = m.group(1)
            if len(ref) == 1:
                if f"Anexo {ref}:" not in " ".join(texto(x) for x in els if nivel(x) == 2):
                    faltan.add("Anexo " + ref)
            elif ref + "." not in conservados:
                faltan.add("apartado " + ref)
    if faltan:
        raise SystemExit(f"Referencias a partes no incluidas: {sorted(faltan)}")


def renumerar(els):
    leyenda = re.compile(r"^(Tabla|Figura) (\d+)\. ")
    mapa, cont = {"Tabla": {}, "Figura": {}}, {"Tabla": 0, "Figura": 0}
    for el in els:
        m = leyenda.match(texto(el)) if el.startswith("<w:p") else None
        if m:
            cont[m.group(1)] += 1
            mapa[m.group(1)][m.group(2)] = str(cont[m.group(1)])
    ref = re.compile(r"(Tabla|Figura)s?\s+\d+(?:(?:,| y| a| e) \d+)*")
    rotas = []

    def aplicar(el):
        ts = [m.group(2) for m in T.finditer(el)]
        if not ts:
            return el
        junto = "".join(ts)
        pos = [(k, j) for k, t in enumerate(ts) for j in range(len(t))]
        cambios = []
        for m in ref.finditer(junto):
            tipo = m.group(1)
            for d in re.finditer(r"\d+", m.group(0)[len(tipo):]):
                a = m.start() + len(tipo) + d.start()
                nuevo = mapa[tipo].get(d.group())
                if nuevo is None:
                    rotas.append(f"{tipo} {d.group()}: …{junto[max(0, m.start() - 60):m.end() + 10]}…")
                else:
                    cambios.append((a, a + len(d.group()), nuevo))
        for a, b, nuevo in sorted(cambios, reverse=True):
            k, j = pos[a]
            if pos[b - 1][0] != k:
                raise SystemExit("Número de tabla o figura partido entre runs")
            ts[k] = ts[k][:j] + nuevo + ts[k][j + (b - a):]
        return poner_textos(el, ts)

    els = [aplicar(el) for el in els]
    if rotas:
        raise SystemExit("Referencias a tablas o figuras que no se incluyen:\n  " + "\n  ".join(rotas))
    print(f"  {cont['Tabla']} tablas y {cont['Figura']} figuras renumeradas")
    return els


def limpiar_marcadores(cuerpo):
    ini = set(re.findall(r'<w:bookmarkStart [^>]*w:id="(\d+)"', cuerpo))
    fin = set(re.findall(r'<w:bookmarkEnd w:id="(\d+)"/>', cuerpo))
    for i in ini - fin:
        cuerpo = re.sub(r'<w:bookmarkStart [^>]*w:id="%s"[^>]*/>' % i, "", cuerpo)
    for i in fin - ini:
        cuerpo = cuerpo.replace('<w:bookmarkEnd w:id="%s"/>' % i, "")
    return cuerpo


def main():
    zin = zipfile.ZipFile(ORIGEN)
    pre, els, post = elementos(zin.read("word/document.xml").decode("utf-8"))
    sel = seleccionar(els)
    ajustar_textos(sel)
    verificar_referencias(sel)
    sel = renumerar(sel)
    xml = pre + limpiar_marcadores("".join(sel)) + post
    # imágenes que ya no se usan
    rels = zin.read("word/_rels/document.xml.rels").decode("utf-8")
    usados = set(re.findall(r'r:(?:embed|id|link)="([^"]+)"', xml))
    quitar = set()

    def filtrar(m):
        rel = m.group(0)
        rid = re.search(r'Id="([^"]+)"', rel).group(1)
        if '/image"' in rel and rid not in usados:
            quitar.add("word/" + re.search(r'Target="([^"]+)"', rel).group(1))
            return ""
        return rel

    rels = re.sub(r"<Relationship [^>]*/>", filtrar, rels)
    with zipfile.ZipFile(SALIDA, "w", zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            if item.filename in quitar:
                continue
            datos = zin.read(item.filename)
            if item.filename == "word/document.xml":
                datos = xml.encode("utf-8")
            elif item.filename == "word/_rels/document.xml.rels":
                datos = rels.encode("utf-8")
            zout.writestr(item, datos)
    print(f"  {len(sel)} de {len(els)} bloques; {len(quitar)} imágenes sin uso quitadas")
    print("  guardado", SALIDA)


if __name__ == "__main__":
    main()
