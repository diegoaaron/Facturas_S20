"""Arma el `.litertlm` que lee bien en el teléfono a partir de las partes en float32 (documentación técnica §6.7).

La receta por defecto de `litert-torch` (todo a 4 bits) estropea la lectura,
incluso en el modelo base. Lo que reproduce al `.litertlm` oficial, medido en
el Moto G34 con la factura de control:
- texto (`prefill_decode`), embebedor y embeddings por capa: los nuestros, a 8 bits
  (`dynamic_wi8_afp32`);
- visión (codificador, adaptador y fin de visión): la del `.litertlm` oficial, con
  activaciones fp16. El LoRA no toca la visión, así que sirve igual para el afinado.
  Los embebedores del oficial no sirven: no son compatibles con nuestro texto.

Toma las partes en float32 de `convertir.py --solo-float` (en `modelos/<nombre>/float/`)
y los metadatos y el tokenizador de un `.litertlm` nuestro ya convertido. Deja
`modelos/<nombre>/empaquetado/model.litertlm`. Cuantizar pide mucha RAM: correrlo
desde una terminal propia, en `~/venvs/litert` y desde `/tmp`:
    python /mnt/c/reposPersonal/Facturas_S20/tunning_gemma4/empaquetar.py --nombre lora_v1
"""
import argparse
import re
import shutil
import subprocess
import time
from pathlib import Path

from huggingface_hub import hf_hub_download
from litert_torch.generative.export_hf.core import quantization

MODELOS = Path(__file__).resolve().parent / 'modelos'
REPO_OFICIAL = 'litert-community/gemma-4-E2B-it-litert-lm'
PARTES = Path.home() / '.cache' / 'facturas_s20' / 'partes'
RECETA = 'dynamic_wi8_afp32'
# Las tres partes nuestras, por nombre de archivo en float32 y tipo de sección.
NUESTRAS = {'model': 'prefill_decode', 'embedder': 'embedder', 'per_layer_embedder': 'per_layer_embedder'}
VISION_OFICIAL = {
    'vision_encoder': ('Section7_TFLiteModel_tf_lite_vision_encoder.tflite', 'fp16', None),
    'vision_adapter': ('Section8_TFLiteModel_tf_lite_vision_adapter.tflite', None, 'cpu'),
    'end_of_vision': ('Section9_TFLiteModel_tf_lite_end_of_vision.tflite', None, None),
}
# El codificador de visión oficial admite hasta 2520 parches; el nuestro, 1260.
MAX_PARCHES = 2520


def desarmar(litertlm, destino):
    """Las secciones de un `.litertlm`, como archivos sueltos (una sola vez por archivo)."""
    if not (destino / 'model.toml').exists():
        destino.mkdir(parents=True, exist_ok=True)
        subprocess.run(['litert-lm-peek', f'--litertlm_file={litertlm}', f'--dump_files_dir={destino}'],
                       check=True, stdout=subprocess.DEVNULL)
    return destino


def buscar(carpeta, nombre):
    encontrados = sorted(carpeta.rglob(nombre), key=lambda p: p.stat().st_mtime)
    if not encontrados:
        raise SystemExit(f'No está {nombre} en {carpeta}: corre antes convertir.py --solo-float')
    return encontrados[-1]


def cuantizar(origen, destino):
    """`origen` a 8 bits en `destino`; si ya está, no lo repite (tarda y pide mucha RAM)."""
    if destino.exists():
        print(f'  {destino.name}: ya estaba')
        return
    inicio = time.time()
    shutil.move(quantization.quantize_model(str(origen), RECETA), destino)
    print(f'  {destino.name}: {(time.time() - inicio) / 60:.1f} min, {destino.stat().st_size / 1024**3:.2f} GiB')


def seccion(tipo, ruta, activacion=None, procesador=None):
    lineas = ['[[section]]']
    if activacion:
        lineas.append('additional_metadata = [\n'
                      f'  {{ key = "prefer_activation_type", value_type = "String", value = "{activacion}" }},\n]')
    if procesador:
        lineas.append(f'backend_constraint = "{procesador}"')
    lineas += [f'model_type = "{tipo}"', 'section_type = "TFLiteModel"', f'data_path = "{ruta}"']
    return '\n'.join(lineas)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--nombre', default='lora_v1', help='carpeta dentro de modelos/')
    ap.add_argument('--float', type=Path, help='carpeta con las partes en float32 (por defecto modelos/<nombre>/float)')
    ap.add_argument('--metadatos', type=Path,
                    help='un .litertlm nuestro del que tomar metadatos y tokenizador '
                         '(por defecto modelos/<nombre>/litertlm/model.litertlm o el del base)')
    args = ap.parse_args()

    carpeta = MODELOS / args.nombre
    origen_float = args.float or carpeta / 'float'
    salida = carpeta / 'empaquetado'
    salida.mkdir(parents=True, exist_ok=True)

    print('Cuantizando a 8 bits:')
    for archivo, tipo in NUESTRAS.items():
        cuantizar(buscar(origen_float, f'{archivo}.tflite'), salida / f'{tipo}_wi8.tflite')

    metadatos = args.metadatos or next(
        (p for p in (carpeta / 'litertlm' / 'model.litertlm', MODELOS / 'base' / 'litertlm' / 'model.litertlm')
         if p.exists()), None)
    if metadatos is None:
        raise SystemExit('Falta un .litertlm nuestro para los metadatos: pásalo con --metadatos')
    nuestro = desarmar(metadatos, PARTES / f'metadatos_{args.nombre}')
    oficial = desarmar(hf_hub_download(REPO_OFICIAL, 'gemma-4-E2B-it.litertlm'), PARTES / 'oficial')

    llm = (nuestro / 'LlmMetadataProto.pbtext').read_text(encoding='utf-8')
    (salida / 'LlmMetadataProto.pbtext').write_text(
        re.sub(r'max_num_patches: \d+', f'max_num_patches: {MAX_PARCHES}', llm), encoding='utf-8')
    tokenizador = buscar(nuestro, 'Section*_HF_Tokenizer_Zlib.zlib')
    toml = [
        '[system_metadata]\nentries = [\n  { key = "Authors", value_type = "String", value = "ODML" },\n]',
        f'[[section]]\nsection_type = "LlmMetadata"\ndata_path = "{salida / "LlmMetadataProto.pbtext"}"',
        f'[[section]]\nsection_type = "ExecutorMetadata"\ndata_path = "{nuestro / "ExecutorMetadataProto.pbtext"}"',
        f'[[section]]\nsection_type = "HF_Tokenizer"\ndata_path = "{tokenizador}"',
    ]
    toml += [seccion(tipo, salida / f'{tipo}_wi8.tflite') for tipo in NUESTRAS.values()]
    toml += [seccion(tipo, oficial / archivo, activacion, procesador)
             for tipo, (archivo, activacion, procesador) in VISION_OFICIAL.items()]
    (salida / 'model.toml').write_text('\n\n'.join(toml) + '\n', encoding='utf-8')

    modelo = salida / 'model.litertlm'
    subprocess.run(['litert-lm-builder', 'toml', '--path', str(salida / 'model.toml'), 'output', '--path', str(modelo)],
                   check=True, stdout=subprocess.DEVNULL)
    print(f'Listo: {modelo} ({modelo.stat().st_size / 1024**3:.2f} GiB)')


if __name__ == '__main__':
    main()
