"""Ajusta dos figuras del entregable 1 (que no tienen fuente editable) al diseño sin servidor del APF2.

- e1_casos_uso.png -> casos_uso.png: quita el actor «Administrador» y su asociación con CU-09.
- e1_wbs.png -> wbs.png: el paquete 5.7 «Servicio de respaldo Spring Boot» pasa a «Exportación de datos para PC».

Los originales no se modifican. Uso: python ajustar_figuras_e1.py
"""
import os

from PIL import Image, ImageDraw, ImageFont

AQUI = os.path.dirname(os.path.abspath(__file__))
CARPETA = os.path.dirname(AQUI)
FUENTE = r"C:\Windows\Fonts\arial.ttf"


def casos_uso():
    im = Image.open(os.path.join(CARPETA, "e1_casos_uso.png")).convert("RGB")
    d = ImageDraw.Draw(im)
    fondo_sistema, blanco = im.getpixel((2100, 300)), (255, 255, 255)
    # asociación CU-09 -> Administrador: y = 1529 + 0,29 (x - 1900); el borde del sistema está en x = 2264..2268
    def y(x):
        return 1529.5 + 0.29 * (x - 1900)
    d.line([(1868, y(1868)), (2260, y(2260))], fill=fondo_sistema, width=7)
    d.line([(2273, y(2273)), (2560, y(2560))], fill=blanco, width=7)
    borde = im.getpixel((2266, 1450))
    d.rectangle([2258, 1626, 2263, 1646], fill=fondo_sistema)
    d.rectangle([2264, 1626, 2268, 1646], fill=borde)
    d.rectangle([2269, 1626, 2276, 1646], fill=blanco)
    # actor y sus rótulos
    d.rectangle([2330, 1600, 2860, 1935], fill=blanco)
    im.save(os.path.join(CARPETA, "casos_uso.png"))
    print("  ok casos_uso", im.size)


def wbs():
    im = Image.open(os.path.join(CARPETA, "e1_wbs.png")).convert("RGB")
    d = ImageDraw.Draw(im)
    color = im.getpixel((1914, 1368))
    d.rectangle([1897, 1337, 2294, 1462], fill=(255, 255, 255))
    f = ImageFont.truetype(FUENTE, 26)
    d.text((1911, 1375), "5.7 Exportación de", font=f, fill=(34, 34, 34) if sum(color) > 600 else color, anchor="ls")
    d.text((1911, 1410), "datos para PC", font=f, fill=(34, 34, 34) if sum(color) > 600 else color, anchor="ls")
    im.save(os.path.join(CARPETA, "wbs.png"))
    print("  ok wbs", im.size)


def generar():
    casos_uso()
    wbs()


if __name__ == "__main__":
    generar()
