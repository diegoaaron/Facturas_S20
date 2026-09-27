"""Generador mínimo de OOXML (WordprocessingML) con los mismos estilos del informe del entregable 1.

Produce cadenas XML de párrafos, tablas, figuras y bloques de código que se insertan en el
document.xml del informe base. Los títulos usan los estilos Ttulo1/Ttulo2/Ttulo3 del documento
(Título 1-3 en Word en español) para que el índice se regenere solo.
"""
import re
from html import escape

NS = ('xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" '
      'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" '
      'xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" '
      'xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" '
      'xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"')

ANCHO = 9026  # ancho útil de la página A4 con márgenes de 2,54 cm (dxa)
AZUL = "1F4E79"
AZUL_CLARO = "DCE6F2"
BORDE = "A6A6A6"


def x(t):
    return escape(str(t), quote=False)


def _runs(texto, base_rpr=""):
    """Convierte **negrita** y *cursiva* en runs."""
    partes = re.split(r"(\*\*[^*]+\*\*|\*[^*]+\*)", str(texto))
    out = []
    for p in partes:
        if not p:
            continue
        rpr = base_rpr
        if p.startswith("**"):
            p, rpr = p[2:-2], rpr + "<w:b/><w:bCs/>"
        elif p.startswith("*"):
            p, rpr = p[1:-1], rpr + "<w:i/><w:iCs/>"
        rp = f"<w:rPr>{rpr}</w:rPr>" if rpr else ""
        out.append(f'<w:r>{rp}<w:t xml:space="preserve">{x(p)}</w:t></w:r>')
    return "".join(out)


def h1(t):
    return f'<w:p><w:pPr><w:pStyle w:val="Ttulo1"/><w:pageBreakBefore/></w:pPr>{_runs(t)}</w:p>'


def h2(t, salto=False):
    sb = "<w:pageBreakBefore/>" if salto else ""
    return f'<w:p><w:pPr><w:pStyle w:val="Ttulo2"/><w:keepNext/>{sb}</w:pPr>{_runs(t)}</w:p>'


def h3(t):
    return f'<w:p><w:pPr><w:pStyle w:val="Ttulo3"/><w:keepNext/></w:pPr>{_runs(t)}</w:p>'


def p(t, jc="both"):
    return (f'<w:p><w:pPr><w:spacing w:after="160" w:line="360" w:lineRule="auto"/><w:jc w:val="{jc}"/></w:pPr>'
            f'{_runs(t)}</w:p>')


def subtitulo(t):
    return (f'<w:p><w:pPr><w:keepNext/><w:spacing w:before="160" w:after="100"/></w:pPr>'
            f'{_runs(t, "<w:b/><w:bCs/><w:i/><w:iCs/>")}</w:p>')


def vineta(t):
    return (f'<w:p><w:pPr><w:pStyle w:val="Prrafodelista"/><w:numPr><w:ilvl w:val="0"/><w:numId w:val="2"/></w:numPr>'
            f'<w:spacing w:after="80" w:line="320" w:lineRule="auto"/><w:jc w:val="both"/></w:pPr>{_runs(t)}</w:p>')


def vinetas(items):
    return "".join(vineta(i) for i in items)


def nota(t):
    return (f'<w:p><w:pPr><w:spacing w:after="200"/></w:pPr><w:r><w:rPr><w:i/><w:iCs/><w:sz w:val="18"/><w:szCs w:val="18"/></w:rPr>'
            f'<w:t xml:space="preserve">Nota. </w:t></w:r>{_runs(t, "<w:sz w:val=\"18\"/><w:szCs w:val=\"18\"/>")}</w:p>')


def espacio():
    return '<w:p><w:pPr><w:spacing w:after="120"/></w:pPr></w:p>'


def leyenda(tipo, clave, titulo):
    """Título de tabla o figura. El número se asigna al final ({N:clave})."""
    return (f'<w:p><w:pPr><w:keepNext/><w:spacing w:before="200" w:after="80"/></w:pPr>'
            f'<w:r><w:rPr><w:b/><w:bCs/></w:rPr><w:t xml:space="preserve">{tipo} {{N:{clave}}}. </w:t></w:r>'
            f'<w:r><w:rPr><w:i/><w:iCs/></w:rPr><w:t>{x(titulo)}</w:t></w:r></w:p>')


def codigo(lineas, titulo=None):
    out = []
    if titulo:
        out.append(f'<w:p><w:pPr><w:keepNext/><w:spacing w:before="120" w:after="60"/></w:pPr>'
                   f'<w:r><w:rPr><w:b/><w:bCs/><w:sz w:val="19"/><w:szCs w:val="19"/></w:rPr><w:t>{x(titulo)}</w:t></w:r></w:p>')
    lineas = lineas.strip("\n").split("\n") if isinstance(lineas, str) else lineas
    for i, l in enumerate(lineas):
        kn = "<w:keepNext/>" if i < len(lineas) - 1 else ""
        out.append(f'<w:p><w:pPr>{kn}<w:keepLines/><w:pBdr><w:left w:val="single" w:sz="12" w:space="6" w:color="{AZUL}"/></w:pBdr>'
                   f'<w:shd w:val="clear" w:color="auto" w:fill="F4F6F8"/><w:spacing w:after="0" w:line="240" w:lineRule="auto"/></w:pPr>'
                   f'<w:r><w:rPr><w:rFonts w:ascii="Consolas" w:eastAsia="Consolas" w:hAnsi="Consolas" w:cs="Consolas"/>'
                   f'<w:sz w:val="16"/><w:szCs w:val="16"/></w:rPr><w:t xml:space="preserve">{x(l) if l else " "}</w:t></w:r></w:p>')
    out.append(espacio())
    return "".join(out)


def _celda(texto, w, cabecera=False, sombra=None, negrita=False, centro=False, sz=19):
    fill = AZUL if cabecera else sombra
    shd = f'<w:shd w:val="clear" w:color="auto" w:fill="{fill}"/>' if fill else ""
    bordes = "".join(f'<w:{b} w:val="single" w:sz="4" w:space="0" w:color="{BORDE}"/>' for b in ("top", "left", "bottom", "right"))
    rpr = f'<w:sz w:val="{sz}"/><w:szCs w:val="{sz}"/>'
    if cabecera:
        rpr = f'<w:b/><w:bCs/><w:color w:val="FFFFFF"/>' + rpr
    elif negrita:
        rpr = '<w:b/><w:bCs/>' + rpr
    jc = '<w:jc w:val="center"/>' if (cabecera or centro) else ""
    parrafos = []
    for linea in str(texto).split("\n"):
        parrafos.append(f'<w:p><w:pPr><w:spacing w:after="20" w:line="260" w:lineRule="auto"/>{jc}</w:pPr>{_runs(linea, rpr)}</w:p>')
    return (f'<w:tc><w:tcPr><w:tcW w:w="{w}" w:type="dxa"/><w:tcBorders>{bordes}</w:tcBorders>{shd}'
            f'<w:tcMar><w:top w:w="50" w:type="dxa"/><w:left w:w="90" w:type="dxa"/><w:bottom w:w="50" w:type="dxa"/>'
            f'<w:right w:w="90" w:type="dxa"/></w:tcMar><w:vAlign w:val="center"/></w:tcPr>{"".join(parrafos)}</w:tc>')


def tabla(cabeceras, filas, anchos, primera_col=True, sz=19, centrar=(), resaltar_filas=()):
    """anchos en proporciones; se escalan al ancho útil."""
    total = sum(anchos)
    ws = [int(ANCHO * a / total) for a in anchos]
    ws[-1] += ANCHO - sum(ws)
    grid = "".join(f'<w:gridCol w:w="{w}"/>' for w in ws)
    out = [f'<w:tbl><w:tblPr><w:tblW w:w="{ANCHO}" w:type="dxa"/><w:tblBorders>'
           + "".join(f'<w:{b} w:val="single" w:sz="4" w:space="0" w:color="auto"/>' for b in ("top", "left", "bottom", "right", "insideH", "insideV"))
           + '</w:tblBorders><w:tblCellMar><w:left w:w="10" w:type="dxa"/><w:right w:w="10" w:type="dxa"/></w:tblCellMar>'
           '<w:tblLook w:val="04A0" w:firstRow="1" w:lastRow="0" w:firstColumn="1" w:lastColumn="0" w:noHBand="0" w:noVBand="1"/>'
           f'</w:tblPr><w:tblGrid>{grid}</w:tblGrid>']
    if cabeceras:
        out.append('<w:tr><w:trPr><w:tblHeader/></w:trPr>' + "".join(_celda(c, w, cabecera=True, sz=sz) for c, w in zip(cabeceras, ws)) + '</w:tr>')
    for i, fila in enumerate(filas):
        celdas = []
        for j, (c, w) in enumerate(zip(fila, ws)):
            sombra = AZUL_CLARO if (primera_col and j == 0) else ("EEF6E4" if i in resaltar_filas else None)
            celdas.append(_celda(c, w, sombra=sombra, negrita=(primera_col and j == 0) or i in resaltar_filas, centro=j in centrar, sz=sz))
        out.append('<w:tr><w:trPr><w:cantSplit/></w:trPr>' + "".join(celdas) + '</w:tr>')
    out.append('</w:tbl>')
    return "".join(out) + espacio()


def figura(rid, ancho_cm, alto_cm, nombre, doc_id):
    cx, cy = int(ancho_cm * 360000), int(alto_cm * 360000)
    return (f'<w:p><w:pPr><w:keepNext/><w:jc w:val="center"/></w:pPr><w:r><w:rPr><w:noProof/></w:rPr><w:drawing>'
            f'<wp:inline distT="0" distB="0" distL="0" distR="0"><wp:extent cx="{cx}" cy="{cy}"/><wp:effectExtent l="0" t="0" r="0" b="0"/>'
            f'<wp:docPr id="{doc_id}" name="{x(nombre)}" descr="{x(nombre)}"/><wp:cNvGraphicFramePr><a:graphicFrameLocks noChangeAspect="1"/></wp:cNvGraphicFramePr>'
            f'<a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:pic>'
            f'<pic:nvPicPr><pic:cNvPr id="0" name="{x(nombre)}"/><pic:cNvPicPr><a:picLocks noChangeAspect="1" noChangeArrowheads="1"/></pic:cNvPicPr></pic:nvPicPr>'
            f'<pic:blipFill><a:blip r:embed="{rid}"/><a:srcRect/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>'
            f'<pic:spPr bwMode="auto"><a:xfrm><a:off x="0" y="0"/><a:ext cx="{cx}" cy="{cy}"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr>'
            f'</pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>')
