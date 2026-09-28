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
