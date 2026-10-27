---
name: 010_27_10_26_feedback_captura_manual_sin_escaner_google
description: La captura de la foto es toda manual en nuestra P06; el escáner de documentos de ML Kit (Google) quedó descartado por su interfaz
metadata:
  type: feedback
---

La foto del comprobante se toma en nuestra P06 (`CapturaActivity`), con disparo manual y nuestra linterna (se enciende sola con poca luz y se apaga después de la foto). El recorte al papel lo hace la app sola después del disparo (`RecortadorComprobante`), sin pantallas extra. No proponer de nuevo el escáner de documentos de ML Kit ni otra interfaz de cámara de terceros, ni opciones de disparo automático.

**Why:** el 2026-10-27 se probó el escáner de ML Kit y al usuario le pareció «un asco» para la experiencia: abre en disparo automático, su linterna no se apaga después de la foto y no respeta el estilo de la app; nada de eso se puede controlar desde la API.

**How to apply:** cualquier mejora de la captura (recorte, enderezado, guía de encuadre) se hace dentro de P06 o en el preprocesado (`PreparadorFoto`), manteniendo el disparo manual y la linterna propia. Si una librería trae su propia pantalla, descartarla de entrada. Relacionado: [[009_26_10_26_feedback_usuario_decide_todo]].
