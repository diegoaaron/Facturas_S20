package pe.facturass20.dominio.puertos;

import java.io.IOException;
import java.io.OutputStream;

import pe.facturass20.dominio.modelo.DatosExportacion;
import pe.facturass20.dominio.modelo.ResumenExportacion;

/** Escribe los datos exportados en un archivo abierto (el ZIP de §9.2). No cierra {@code destino}. */
public interface ExportadorArchivos {

    ResumenExportacion exportar(DatosExportacion datos, OutputStream destino) throws IOException;
}
