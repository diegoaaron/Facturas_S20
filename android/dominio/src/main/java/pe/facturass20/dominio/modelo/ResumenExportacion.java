package pe.facturass20.dominio.modelo;

import java.math.BigDecimal;

/**
 * Lo que quedó en el ZIP, para el mensaje final de P20.
 *
 * @param facturas     filas de {@code facturas.csv} (vigentes y anuladas)
 * @param totalVigente suma de las vigentes: debe coincidir con el acumulado del período (§9.5)
 */
public record ResumenExportacion(int facturas, int imagenes, int reportes, BigDecimal totalVigente) { }
