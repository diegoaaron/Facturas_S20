---
name: usuario-dos-computadoras
description: El usuario trabaja este proyecto desde dos computadoras; la memoria de Claude se guarda en memorias/ dentro del repo
metadata:
  type: user
---

El usuario (Diego) maneja Facturas_S20 desde 2 computadoras distintas (Windows). Por eso la memoria del proyecto se guarda en `memorias/` dentro del repositorio y no en la memoria local de Claude (`~/.claude/projects/...`).

**Why:** la memoria local no se sincroniza entre máquinas; la del repo sí, vía git.
**How to apply:** guardar y actualizar memorias solo en `memorias/`; al empezar, si algo parece desactualizado, sugerir `git pull`; tras crear/editar memorias, recordar que hay que commitear y hacer push para que la otra computadora las vea. Relacionado: [[proyecto-tunel-gemma]].
