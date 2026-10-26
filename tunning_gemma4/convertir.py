"""Convierte el modelo fusionado a `.litertlm` para LiteRT-LM (documentación técnica §6.7).

Antes de exportar reemplaza la plantilla de chat por la del `.litertlm` oficial
(`litert-community/gemma-4-E2B-it-litert-lm`): la del modelo original usa
`map.get()` y LiteRT-LM falla en el teléfono al aplicarla.

Va en su propio entorno (`~/venvs/litert`, con `litert-torch-nightly`), porque
pide otra versión de transformers que Unsloth. Se corre desde `/tmp`:
    python /mnt/c/reposPersonal/Facturas_S20/tunning_gemma4/convertir.py --nombre lora_v1
"""
import argparse
import shutil
import time
from pathlib import Path

from huggingface_hub import hf_hub_download, snapshot_download
from litert_torch.generative.export_hf import export as export_hf

MODELOS = Path(__file__).resolve().parent / 'modelos'
MODELO_BASE = 'unsloth/gemma-4-E2B-it'
REPO_OFICIAL = 'litert-community/gemma-4-E2B-it-litert-lm'


def base_con_plantilla(plantilla):
    """El modelo base sin afinar, con la plantilla oficial, para comparar la conversión con el `.litertlm` oficial.

    Arma una carpeta con enlaces a los archivos del caché de Hugging Face (sin
    copiar los 10 GB ni tocar el caché) y solo la plantilla copiada.
    """
    base = Path(snapshot_download(MODELO_BASE))
    carpeta = Path.home() / '.cache' / 'facturas_s20' / 'base_con_plantilla'
    carpeta.mkdir(parents=True, exist_ok=True)
    for archivo in base.iterdir():
        enlace = carpeta / archivo.name
        if archivo.name != 'chat_template.jinja' and not enlace.exists():
            enlace.symlink_to(archivo.resolve())
    shutil.copy(plantilla, carpeta / 'chat_template.jinja')
    return carpeta


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--nombre', default='lora_v1',
                    help='carpeta dentro de modelos/ con el fusionado/; «base» convierte el modelo sin afinar')
    ap.add_argument('--cuantizacion', default='dynamic_wi4_afp32', help='receta para el modelo de texto')
    ap.add_argument('--cache', type=int, default=1024, help='tokens de contexto (imagen + instrucción + respuesta)')
    args = ap.parse_args()

    carpeta = MODELOS / args.nombre
    salida = carpeta / 'litertlm'
    salida.mkdir(parents=True, exist_ok=True)
    plantilla = hf_hub_download(REPO_OFICIAL, 'chat_template.jinja')
    if args.nombre == 'base':
        origen = base_con_plantilla(plantilla)
    else:
        origen = carpeta / 'fusionado'
        shutil.copy(plantilla, origen / 'chat_template.jinja')

    inicio = time.time()
    export_hf.export(
        model=str(origen),
        output_dir=str(salida),
        task='image_text_to_text',
        cache_length=args.cache,
        quantization_recipe=args.cuantizacion,
        externalize_embedder=True,
        export_vision_encoder=True,
        bundle_litert_lm=True,
        # En float32 (el valor por defecto): con model_dtype='bfloat16' falla al exportar el embebedor
        # («unsupported ScalarType BFloat16»). Ocupa ~20 GB de RAM; hace falta subirla en .wslconfig.
    )
    print(f'Conversión: {(time.time() - inicio) / 60:.1f} min')
    for archivo in sorted(salida.iterdir()):
        print(f'  {archivo.name}: {archivo.stat().st_size / 1024**3:.2f} GiB')


if __name__ == '__main__':
    main()
