---
name: 008_25_10_26_proyecto_entrenamiento_local_wsl
description: El ajuste fino de Gemma 4 E2B sí corre en la RTX 3050 de 6 GB (WSL), dejando embed_tokens_per_layer en la CPU
metadata:
  type: project
---

El 2026-10-25 se probó el ajuste fino en la PC con la RTX 3050 Laptop de 6 GB (`tunning_gemma4/prueba_memoria_gpu.py`): **funciona**, con pico de 4,17 GiB (70 %) a 512 px, LoRA r=16, lote 1, y ~1 s por paso tras una compilación inicial de ~85 s.

- Entorno: WSL `Ubuntu-26.04`, venv `~/venvs/gemma` (Python 3.12 con `uv`; el Python del sistema es 3.14, demasiado nuevo), Unsloth 2026.10.8, torch 2.14.1+cu130, transformers 5.5.0, TRL 1.15.0. Triton necesita `build-essential` (instalado con `wsl -u root`, porque `sudo` no puede pedir contraseña desde Claude Code). WSL tiene solo 11 GB de RAM asignados.
- **Truco clave:** el modelo completo pesa 7,55 GiB porque `embed_tokens_per_layer` (4,38 GiB) no se cuantiza; sin él, Unsloth deja todo en la CPU y falla («No or negligible GPU memory available for fused cross entropy»). Se deja esa tabla en la CPU (`EmbeddingEnCpu` en el script) y el resto va a la GPU (3,33 GiB).
- Ejecutar los scripts desde `/tmp` (o con cwd fuera del repo): Unsloth crea `unsloth_compiled_cache/` en el directorio actual (está en `.gitignore`).
- Llamar a WSL desde la herramienta PowerShell, no desde Git Bash (que convierte rutas y expande `$VAR`).

**Why:** decide que el entrenamiento se haga en local y no en Colab, y evita repetir el diagnóstico.

**How to apply:** reutilizar `EmbeddingEnCpu`/`mover_a_gpu_sin_embeddings_por_capa` en el script de entrenamiento real. Para fusionar el LoRA y convertir a `.litertlm` probablemente haya que subir la RAM de WSL en `.wslconfig`. Relacionado: [[007_25_10_26_proyecto_etiquetado_dataset]].
