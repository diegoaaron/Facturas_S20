"""Ajuste fino de Gemma 4 E2B con LoRA para leer facturas y boletas (iteración I4).

Divide las fotos por emisor (ver `comun.dividir_por_emisor`), entrena un LoRA
sobre el modelo en 4 bits con la visión congelada y guarda el adaptador en
`modelos/<nombre>/`, junto con la división usada (`division.json`), para que
`evaluar.py` mida con las mismas fotos de prueba.

Se corre dentro de WSL con el entorno ~/venvs/gemma y con el cwd fuera del repo
(Unsloth crea `unsloth_compiled_cache/` en el directorio actual):
    cd /tmp && python /mnt/c/reposPersonal/Facturas_S20/tunning_gemma4/entrenar.py
"""
import argparse
import json
import time

import unsloth  # noqa: F401  (debe importarse antes que transformers)
import torch
from trl import SFTConfig, SFTTrainer
from unsloth import FastVisionModel
from unsloth.trainer import UnslothVisionDataCollator

from comun import (MARCA_MODELO, MARCA_USUARIO, MODELOS, a_ejemplo, cargar_modelo_base,
                   dividir_por_emisor, gib, leer_etiquetas)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--nombre', default='lora_v1', help='carpeta de salida dentro de modelos/')
    ap.add_argument('--epocas', type=float, default=3)
    ap.add_argument('--resolucion', type=int, default=512, help='lado mayor de la foto, como en la app')
    ap.add_argument('--rango', type=int, default=16, help='rango r del LoRA')
    ap.add_argument('--acumulacion', type=int, default=4, help='pasos de acumulación de gradiente')
    ap.add_argument('--tasa', type=float, default=2e-4, help='tasa de aprendizaje')
    ap.add_argument('--semilla', type=int, default=42)
    args = ap.parse_args()

    salida = MODELOS / args.nombre
    salida.mkdir(parents=True, exist_ok=True)

    grupos = dividir_por_emisor(leer_etiquetas(), semilla=args.semilla)
    division = {g: [f['archivo'] for f in filas] for g, filas in grupos.items()}
    (salida / 'division.json').write_text(json.dumps(division, indent=2), encoding='utf-8')
    for g, filas in grupos.items():
        print(f'{g}: {len(filas)} fotos de {len({f["ruc"] for f in filas})} emisores')

    entrenamiento = [a_ejemplo(f, args.resolucion) for f in grupos['entrenamiento']]
    validacion = [a_ejemplo(f, args.resolucion) for f in grupos['validacion']]

    model, tokenizer = cargar_modelo_base()
    model = FastVisionModel.get_peft_model(
        model,
        finetune_vision_layers=False,
        finetune_language_layers=True,
        finetune_attention_modules=True,
        finetune_mlp_modules=True,
        r=args.rango,
        lora_alpha=args.rango * 2,
        lora_dropout=0.05,
        bias='none',
        random_state=args.semilla,
    )
    torch.cuda.reset_peak_memory_stats()

    FastVisionModel.for_training(model)
    trainer = SFTTrainer(
        model=model,
        processing_class=tokenizer,
        data_collator=UnslothVisionDataCollator(
            model, tokenizer, resize=args.resolucion,
            # La pérdida se calcula solo sobre el JSON de la respuesta, no sobre la foto ni la instrucción.
            train_on_responses_only=True, instruction_part=MARCA_USUARIO, response_part=MARCA_MODELO,
        ),
        train_dataset=entrenamiento,
        eval_dataset=validacion,
        args=SFTConfig(
            per_device_train_batch_size=1,
            per_device_eval_batch_size=1,
            gradient_accumulation_steps=args.acumulacion,
            num_train_epochs=args.epocas,
            learning_rate=args.tasa,
            lr_scheduler_type='linear',
            warmup_ratio=0.1,
            weight_decay=0.01,
            logging_steps=1,
            eval_strategy='epoch',
            save_strategy='epoch',
            save_total_limit=1,
            load_best_model_at_end=True,
            metric_for_best_model='eval_loss',
            optim='adamw_8bit',
            seed=args.semilla,
            report_to='none',
            output_dir=str(salida / 'checkpoints'),
            remove_unused_columns=False,
            dataset_text_field='',
            dataset_kwargs={'skip_prepare_dataset': True},
            max_length=2048,
        ),
    )
    inicio = time.time()
    trainer.train()
    duracion = time.time() - inicio

    model.save_pretrained(salida / 'lora')
    tokenizer.save_pretrained(salida / 'lora')

    historial = [r for r in trainer.state.log_history if 'eval_loss' in r]
    resumen = {
        'argumentos': vars(args),
        'fotos': {g: len(v) for g, v in division.items()},
        'segundos': round(duracion),
        'pico_memoria_gib': round(torch.cuda.max_memory_reserved() / 1024**3, 2),
        'mejor_checkpoint': trainer.state.best_model_checkpoint,
        'perdida_validacion': [{'epoca': r['epoch'], 'perdida': round(r['eval_loss'], 4)} for r in historial],
    }
    (salida / 'entrenamiento.json').write_text(json.dumps(resumen, indent=2), encoding='utf-8')

    print('\n=== Resultado ===')
    print(f'Tiempo: {duracion / 60:.1f} min | pico de memoria: {gib(torch.cuda.max_memory_reserved())}')
    for r in resumen['perdida_validacion']:
        print(f'  época {r["epoca"]:.0f}: pérdida de validación {r["perdida"]}')
    print(f'LoRA guardado en {salida / "lora"}')


if __name__ == '__main__':
    main()
