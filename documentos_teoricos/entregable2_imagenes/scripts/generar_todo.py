"""Regenera todas las figuras del entregable 2, el informe Word y la presentación.

Uso (desde esta carpeta):
    python generar_todo.py            # figuras + Word + PPT
    python generar_todo.py figuras    # solo figuras
Después de regenerar el Word, ejecutar actualizar_indice.ps1 para que Word recalcule el índice.
"""
import subprocess
import sys

import diagramas_bpmn
import diagramas_uml
import pantallas


def main():
    solo_figuras = len(sys.argv) > 1 and sys.argv[1] == "figuras"
    print("Diagramas BPMN"); diagramas_bpmn.generar()
    print("Diagramas UML, DER y Gantt"); diagramas_uml.generar()
    print("Prototipo"); pantallas.generar()
    if solo_figuras:
        return
    print("Informe Word"); subprocess.run([sys.executable, "construir_documento.py"], check=True)
    print("Índice del Word")
    subprocess.run(["powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "actualizar_indice.ps1"], check=True)
    print("Presentación"); subprocess.run([sys.executable, "construir_presentacion.py"], check=True)


if __name__ == "__main__":
    main()
