"""Cambia la visión de nuestro `.litertlm` por la del oficial, para que lea bien en el teléfono (documentación técnica §6.7).

Medido en el Moto G34 con la factura de control: con el texto y los embebedores
a 8 bits (lo que deja `convertir.py`) y la visión que exporta `litert-torch`, el
modelo no lee la serie ni el número; con la visión del `.litertlm` oficial
(codificador, adaptador y fin de visión, con activaciones fp16) lee como el
oficial. El LoRA no toca la visión, así que sirve igual para el afinado. Los
embebedores del oficial, en cambio, no son compatibles con nuestro texto.

Toma `modelos/<nombre>/litertlm/model.litertlm` y deja
`modelos/<nombre>/empaquetado/model.litertlm`. No cuantiza nada: tarda segundos.
Va en `~/venvs/litert`, desde `/tmp`:
    python /mnt/c/reposPersonal/Facturas_S20/tunning_gemma4/empaquetar.py --nombre lora_v1
"""
import argparse
import re
import subprocess
from pathlib import Path

from huggingface_hub import hf_hub_download

MODELOS = Path(__file__).resolve().parent / 'modelos'
REPO_OFICIAL = 'litert-community/gemma-4-E2B-it-litert-lm'
PARTES = Path.home() / '.cache' / 'facturas_s20' / 'partes'
# Las secciones de visión del oficial y lo que el oficial declara para cada una.
VISION_OFICIAL = {
    'vision_encoder': 'additional_metadata = [\n'
                      '  { key = "prefer_activation_type", value_type = "String", value = "fp16" },\n]\n',
    'vision_adapter': 'backend_constraint = "cpu"\n',
    'end_of_vision': '',
}
# El codificador de visión oficial admite hasta 2520 parches; el que exporta litert-torch, 1260.
MAX_PARCHES = 2520


def desarmar(litertlm, destino):
    """Las secciones de un `.litertlm` como archivos sueltos, con el `model.toml` que las vuelve a juntar."""
    destino.mkdir(parents=True, exist_ok=True)
    subprocess.run(['litert-lm-peek', f'--litertlm_file={litertlm}', f'--dump_files_dir={destino}'],
                   check=True, stdout=subprocess.DEVNULL)
    return destino


def con_rutas_absolutas(toml, carpeta):
    return re.sub(r'data_path = "([^"/][^"]*)"', lambda m: f'data_path = "{carpeta / m.group(1)}"', toml)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--nombre', default='lora_v1', help='carpeta dentro de modelos/ con el litertlm/ de convertir.py')
    args = ap.parse_args()

    carpeta = MODELOS / args.nombre
    salida = carpeta / 'empaquetado'
    salida.mkdir(exist_ok=True)
    nuestro = desarmar(carpeta / 'litertlm' / 'model.litertlm', PARTES / args.nombre)
    oficial = PARTES / 'oficial'
    if not (oficial / 'model.toml').exists():
        desarmar(hf_hub_download(REPO_OFICIAL, 'gemma-4-E2B-it.litertlm'), oficial)
    archivos_oficiales = {tipo: next(oficial.glob(f'Section*_tf_lite_{tipo}.tflite')) for tipo in VISION_OFICIAL}

    llm = (nuestro / 'LlmMetadataProto.pbtext').read_text(encoding='utf-8')
    (salida / 'LlmMetadataProto.pbtext').write_text(
        re.sub(r'max_num_patches: \d+', f'max_num_patches: {MAX_PARCHES}', llm), encoding='utf-8')

    toml = con_rutas_absolutas((nuestro / 'model.toml').read_text(encoding='utf-8'), nuestro)
    toml = toml.replace(f'"{nuestro / "LlmMetadataProto.pbtext"}"', f'"{salida / "LlmMetadataProto.pbtext"}"')
    for tipo, extra in VISION_OFICIAL.items():
        toml, cambios = re.subn(
            rf'(?:additional_metadata = \[[^\]]*\]\n)?(?:backend_constraint = "[^"]*"\n)?'
            rf'model_type = "{tipo}"\nsection_type = "TFLiteModel"\ndata_path = "[^"]*"',
            f'{extra}model_type = "{tipo}"\nsection_type = "TFLiteModel"\ndata_path = "{archivos_oficiales[tipo]}"',
            toml)
        if cambios != 1:
            raise SystemExit(f'No encontré la sección {tipo} en {nuestro / "model.toml"}')
    (salida / 'model.toml').write_text(toml, encoding='utf-8')

    modelo = salida / 'model.litertlm'
    subprocess.run(['litert-lm-builder', 'toml', '--path', str(salida / 'model.toml'), 'output', '--path', str(modelo)],
                   check=True, stdout=subprocess.DEVNULL)
    print(f'Listo: {modelo} ({modelo.stat().st_size / 1024**3:.2f} GiB)')


if __name__ == '__main__':
    main()
