"""Valida datos/etiquetas.csv antes de usarlo para el ajuste fino.

Revisa que cada foto exista, que cada foto tenga su fila y que los campos
tengan un formato válido (RUC con dígito verificador módulo 11, fecha real,
serie, moneda e importe). No corrige nada: solo informa.

Uso: python validar_etiquetas.py
"""
import csv
import re
import sys
from datetime import date
from decimal import Decimal, InvalidOperation
from pathlib import Path

CARPETA = Path(__file__).parent / 'datos'
CSV = CARPETA / 'etiquetas.csv'
IMAGENES = CARPETA / 'imagenes'
CAMPOS = ['ruc', 'razon_social', 'serie', 'numero', 'fecha_emision', 'moneda', 'importe_total']
EXTENSIONES = {'.jpg', '.jpeg', '.png'}


def error_ruc(ruc):
    if not re.fullmatch(r'\d{11}', ruc):
        return 'no tiene 11 dígitos'
    if ruc[:2] not in ('10', '15', '17', '20'):
        return f'prefijo {ruc[:2]} inválido'
    pesos = [5, 4, 3, 2, 7, 6, 5, 4, 3, 2]
    resto = 11 - sum(int(d) * p for d, p in zip(ruc, pesos)) % 11
    if {10: 0, 11: 1}.get(resto, resto) != int(ruc[10]):
        return 'el dígito verificador no cuadra'
    return None


def error_fecha(texto):
    try:
        fecha = date.fromisoformat(texto)
    except ValueError:
        return 'no es una fecha AAAA-MM-DD'
    if fecha > date.today():
        return 'está en el futuro'
    return None


def error_serie(serie):
    # Electrónicas: F/B/E + 3 caracteres; impresas: 4 dígitos.
    if not re.fullmatch(r'[FBE][A-Z0-9]{3}|\d{4}', serie):
        return 'no parece una serie (F001, B001, E001 o 0001)'
    return None


def error_numero(numero):
    if not re.fullmatch(r'\d{1,8}', numero):
        return 'no son de 1 a 8 dígitos'
    return None


def error_moneda(moneda):
    return None if moneda in ('PEN', 'USD') else 'no es PEN ni USD'


def error_importe(texto):
    if not re.fullmatch(r'\d+\.\d{2}', texto):
        return 'no tiene el formato 1234.56'
    try:
        if Decimal(texto) <= 0:
            return 'no es mayor que cero'
    except InvalidOperation:
        return 'no es un número'
    return None


VALIDADORES = {
    'ruc': error_ruc,
    'serie': error_serie,
    'numero': error_numero,
    'fecha_emision': error_fecha,
    'moneda': error_moneda,
    'importe_total': error_importe,
}


def main():
    with open(CSV, encoding='utf-8-sig', newline='') as f:
        filas = list(csv.DictReader(f))

    problemas = []
    vistos = set()
    for n, fila in enumerate(filas, start=2):
        archivo = fila['archivo'].strip()
        if archivo in vistos:
            problemas.append(f'línea {n}: {archivo} está repetido')
        vistos.add(archivo)
        if not (IMAGENES / archivo).exists():
            problemas.append(f'línea {n}: no existe la imagen {archivo}')
        for campo in CAMPOS:
            valor = fila[campo].strip()
            if not valor:
                continue  # vacío = no se lee en la foto; el modelo debe responder null
            validador = VALIDADORES.get(campo)
            motivo = validador(valor) if validador else None
            if motivo:
                problemas.append(f'línea {n} ({archivo}): {campo} «{valor}» {motivo}')

    sin_fila = sorted(p.name for p in IMAGENES.iterdir()
                      if p.suffix.lower() in EXTENSIONES and p.name not in vistos)
    marcadas = [f['archivo'] for f in filas if f.get('revisar', '').strip()]

    print(f'Filas: {len(filas)} | Imágenes sin fila: {len(sin_fila)} | Marcadas para revisar: {len(marcadas)}')
    for p in problemas:
        print('  ✗', p)
    if marcadas:
        print('Para revisar:', ', '.join(marcadas))
    if not problemas:
        print('Sin errores de formato.')
    return 1 if problemas else 0


if __name__ == '__main__':
    sys.exit(main())
