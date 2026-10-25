"""Construye documentos_teoricos/entregable2/entregable2_documento.docx a partir de entregable1/entregable1_documento.docx.

El segundo avance no reemplaza al primero: lo amplía. Por eso el script toma el cuerpo del informe
del entregable 1, reordena sus bloques según la estructura oficial del proyecto integrador
(capítulo 3 con 3.1 a 3.7, capítulo 4 de cronograma y presupuesto, anexos A a T) y añade las
secciones de diseño nuevas. Las tablas y figuras se renumeran al final.

Uso:  python construir_documento.py   (luego ejecutar actualizar_indice.ps1 para regenerar el índice)
"""
import copy
import os
import re
import shutil
import struct
import zipfile

from lxml import etree

import docx_xml as D
import contenido_documento as C

AQUI = os.path.dirname(os.path.abspath(__file__))
IMAGENES = os.path.dirname(AQUI)
ENTREGABLE = os.path.dirname(IMAGENES)
TEORICOS = os.path.dirname(ENTREGABLE)
BASE = os.path.join(TEORICOS, "entregable1", "entregable1_documento.docx")
SALIDA = os.path.join(ENTREGABLE, "entregable2_documento.docx")

W = "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}"


def tamano_png(ruta):
    with open(ruta, "rb") as f:
        cab = f.read(24)
    return struct.unpack(">II", cab[16:24])


class Documento:
    def __init__(self):
        self.zin = zipfile.ZipFile(BASE)
        self.xml = etree.fromstring(self.zin.read("word/document.xml"))
        self.body = self.xml.find(W + "body")
        self.els = list(self.body)
        self.rels = self.zin.read("word/_rels/document.xml.rels").decode("utf-8")
        self.medios = {}      # nombre en word/media -> ruta local
        self.sig_rid = 200
        self.sig_doc = 9000
        self.items = []

    # ---------- reutilizar bloques del entregable 1 ----------
    def e1(self, i, j=None, **cambios):
        """Reutiliza los bloques i..j. reemplazos: [(viejo, nuevo)] o [(viejo, nuevo, fila)], donde fila es un texto
        que identifica la fila de tabla (w:tr) a la que se limita el reemplazo."""
        j = i if j is None else j
        pendientes = {r[0] for r in cambios.get("reemplazos", [])}
        for k in range(i, j + 1):
            el = copy.deepcopy(self.els[k])
            for viejo, nuevo, *fila in cambios.get("reemplazos", []):
                destinos = [el] if not fila else [tr for tr in el.iter(W + "tr")
                                                  if fila[0] in "".join(t.text or "" for t in tr.iter(W + "t"))]
                if any([reemplazar(dst, viejo, nuevo) for dst in destinos]):
                    pendientes.discard(viejo)
            self.items.append(el)
        if pendientes:
            raise SystemExit(f"No se encontró {pendientes} en los bloques {i}-{j}")

    def e1_titulo(self, i, texto):
        el = copy.deepcopy(self.els[i])
        ts = el.findall(".//" + W + "t")
        ts[0].text = texto
        for t in ts[1:]:
            t.text = ""
        self.items.append(el)

    def e1_texto(self, i, texto):
        """Copia el formato del párrafo i con otro texto (p. ej., una referencia bibliográfica)."""
        el = copy.deepcopy(self.els[i])
        for r in el.findall(W + "r"):
            el.remove(r)
        for r in etree.fromstring(f"<w:root {D.NS}>{D._runs(texto)}</w:root>"):
            el.append(r)
        self.items.append(el)

    def e1_estilo(self, i, estilo, texto=None):
        """Reutiliza un título cambiando su nivel (Ttulo2 -> Ttulo3, etc.)."""
        el = copy.deepcopy(self.els[i])
        st = el.find(".//" + W + "pStyle")
        st.set(W + "val", estilo)
        pb = el.find(".//" + W + "pageBreakBefore")
        if pb is not None and estilo != "Ttulo1":
            pb.getparent().remove(pb)
        if texto:
            ts = el.findall(".//" + W + "t")
            ts[0].text = texto
            for t in ts[1:]:
                t.text = ""
        self.items.append(el)

    # ---------- contenido nuevo ----------
    def add(self, *xmls):
        for x in xmls:
            self.items.append(x)

    def figura(self, clave, archivo, titulo, ancho_cm=15.9, nota="Elaboración propia.", max_alto_cm=21.0):
        ruta = os.path.join(IMAGENES, archivo)
        w, h = tamano_png(ruta)
        alto = ancho_cm * h / w
        if alto > max_alto_cm:
            ancho_cm, alto = ancho_cm * max_alto_cm / alto, max_alto_cm
        nombre = "e2_" + archivo
        if nombre not in self.medios:
            rid = f"rId{self.sig_rid}"
            self.sig_rid += 1
            self.medios[nombre] = (ruta, rid)
            self.rels = self.rels.replace("</Relationships>",
                                          f'<Relationship Id="{rid}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" '
                                          f'Target="media/{nombre}"/></Relationships>')
        rid = self.medios[nombre][1]
        self.sig_doc += 1
        self.add(D.leyenda("Figura", clave, titulo), D.figura(rid, ancho_cm, alto, archivo[:-4], self.sig_doc), D.nota(nota))

    def tabla(self, clave, titulo, cabeceras, filas, anchos, nota=None, **kw):
        self.add(D.leyenda("Tabla", clave, titulo), D.tabla(cabeceras, filas, anchos, **kw))
        if nota:
            self.items[-1] = self.items[-1].replace(D.espacio(), "")
            self.add(D.nota(nota))

    # ---------- ensamblado ----------
    def construir(self):
        sect = copy.deepcopy(self.els[-1])
        for e in list(self.body):
            self.body.remove(e)
        for it in self.items:
            if isinstance(it, str):
                frag = etree.fromstring(f"<w:root {D.NS}>{it}</w:root>")
                for e in frag:
                    self.body.append(e)
            else:
                self.body.append(it)
        self.body.append(sect)
        self.numerar()
        self.notas_tablas()
        self.leyendas_apa()
        self.verificar_figuras()

    # ---------- formato APA 7 de tablas y figuras ----------
    @staticmethod
    def _texto(el):
        return "".join(t.text or "" for t in el.iter(W + "t"))

    def notas_tablas(self):
        """APA 7: toda tabla lleva debajo una nota con su fuente. Si una tabla no la tiene, se agrega
        «Nota. Elaboración propia.» o la nota específica de C.NOTAS_TABLAS (buscada por el inicio del título)."""
        patron = re.compile(r"^Tabla \d+\. (.*)$")
        agregadas = 0
        for tbl in [e for e in self.body if e.tag == W + "tbl"]:
            previo, siguiente = tbl.getprevious(), tbl.getnext()
            m = patron.match(self._texto(previo)) if previo is not None else None
            if not m:
                continue
            if siguiente is not None and self._texto(siguiente).startswith("Nota."):
                continue
            titulo = m.group(1)
            texto = next((v for k, v in C.NOTAS_TABLAS.items() if titulo.startswith(k)), "Elaboración propia.")
            nota = etree.fromstring(f"<w:root {D.NS}>{D.nota(texto)}</w:root>")[0]
            vacio = (siguiente is not None and siguiente.tag == W + "p" and not self._texto(siguiente)
                     and siguiente.find(".//" + W + "drawing") is None and siguiente.find(".//" + W + "sectPr") is None)
            if vacio:
                self.body.replace(siguiente, nota)
            else:
                tbl.addnext(nota)
            agregadas += 1
        print(f"  {agregadas} notas de tabla agregadas")

    def leyendas_apa(self):
        """APA 7: «Tabla N» / «Figura N» en negrita en una línea y el título en cursiva en la línea siguiente."""
        patron = re.compile(r"^(Tabla|Figura) (\d+)\. ")
        for par in [e for e in self.body if e.tag == W + "p"]:
            runs = par.findall(W + "r")
            if not runs:
                continue
            t0 = runs[0].find(W + "t")
            if t0 is None or not t0.text or not patron.match(t0.text) or len(t0.text) > 40:
                continue
            titulo = copy.deepcopy(par)
            # línea 1: solo el número, en negrita
            t0.text = patron.match(t0.text).group(1) + " " + patron.match(t0.text).group(2)
            for r in runs[1:]:
                par.remove(r)
            sp = par.find(W + "pPr/" + W + "spacing")
            if sp is not None:
                sp.set(W + "after", "0")
            # línea 2: el título, en cursiva
            truns = titulo.findall(W + "r")
            resto = patron.sub("", truns[0].find(W + "t").text)
            if resto:
                truns[0].find(W + "t").text = resto
            else:
                titulo.remove(truns[0])
            for r in titulo.findall(W + "r"):
                rpr = r.find(W + "rPr")
                if rpr is None:
                    rpr = etree.SubElement(r, W + "rPr")
                    r.insert(0, rpr)
                for tag in ("b", "bCs"):
                    for e in rpr.findall(W + tag):
                        rpr.remove(e)
                if rpr.find(W + "i") is None:
                    rpr.insert(0, etree.Element(W + "iCs"))
                    rpr.insert(0, etree.Element(W + "i"))
            sp = titulo.find(W + "pPr/" + W + "spacing")
            if sp is not None:
                sp.set(W + "before", "0")
            par.addnext(titulo)

    def verificar_figuras(self):
        """Cada figura debe tener su nota APA debajo de la imagen."""
        els = list(self.body)
        faltan = []
        for i, el in enumerate(els):
            if el.tag == W + "p" and re.fullmatch(r"Figura \d+", self._texto(el)):
                j = i + 2
                while j < len(els) and els[j].find(".//" + W + "drawing") is not None:
                    j += 1
                if j >= len(els) or not self._texto(els[j]).startswith("Nota."):
                    faltan.append(self._texto(el))
        if faltan:
            raise SystemExit(f"Figuras sin nota: {faltan}")

    def numerar(self):
        cont = {"Tabla": 0, "Figura": 0}
        claves = {}
        patron = re.compile(r"^(Tabla|Figura) (\d+|\{N:(\w+)\})\. ")
        for par in self.body.iter(W + "p"):
            ts = par.findall(".//" + W + "t")
            if not ts or not ts[0].text:
                continue
            m = patron.match(ts[0].text)
            if not m or len(ts[0].text) > 40:
                continue
            tipo = m.group(1)
            cont[tipo] += 1
            n = cont[tipo]
            if m.group(3):
                claves[m.group(3)] = n
            ts[0].text = patron.sub(f"{tipo} {n}. ", ts[0].text)
        faltan = set()
        for t in self.body.iter(W + "t"):
            if t.text and "{ref:" in t.text:
                def sustituir(mm):
                    if mm.group(1) not in claves:
                        faltan.add(mm.group(1))
                        return "??"
                    return str(claves[mm.group(1)])
                t.text = re.sub(r"\{ref:(\w+)\}", sustituir, t.text)
        if faltan:
            raise SystemExit(f"Referencias sin figura o tabla: {sorted(faltan)}")
        print(f"  {cont['Tabla']} tablas y {cont['Figura']} figuras numeradas")

    def guardar(self):
        with zipfile.ZipFile(SALIDA, "w", zipfile.ZIP_DEFLATED) as zout:
            for item in self.zin.infolist():
                datos = self.zin.read(item.filename)
                if item.filename == "word/document.xml":
                    datos = etree.tostring(self.xml, xml_declaration=True, encoding="UTF-8", standalone=True)
                elif item.filename == "word/_rels/document.xml.rels":
                    datos = self.rels.encode("utf-8")
                elif item.filename == "word/footer1.xml":
                    datos = datos.decode("utf-8").replace("APF1", "APF2").encode("utf-8")
                elif item.filename == "docProps/core.xml":
                    datos = (datos.decode("utf-8").replace("APF1 - Informe", "APF2 - Informe")
                             .replace("Avance de Proyecto Final 1", "Avance de Proyecto Final 2").encode("utf-8"))
                zout.writestr(item, datos)
            for nombre, (ruta, _) in self.medios.items():
                zout.write(ruta, "word/media/" + nombre)
        print("  guardado", SALIDA)


def reemplazar(el, viejo, nuevo):
    hecho = False
    for t in list(el.iter(W + "t")):
        if t.text and viejo in t.text:
            t.text = t.text.replace(viejo, nuevo)
            hecho = True
    return hecho


def main():
    d = Documento()
    C.escribir(d)
    d.construir()
    d.guardar()


if __name__ == "__main__":
    main()
