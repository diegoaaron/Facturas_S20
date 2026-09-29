package pe.facturass20.dominio;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import pe.facturass20.dominio.modelo.CategoriaNRUS;
import pe.facturass20.dominio.modelo.CronogramaVencimiento;
import pe.facturass20.dominio.modelo.Emisor;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.Moneda;
import pe.facturass20.dominio.modelo.OrigenRegistro;
import pe.facturass20.dominio.modelo.ParametrosNrus;
import pe.facturass20.dominio.reglas.Montos;

/** Datos de ejemplo compartidos por las pruebas: parámetros 2026.1 (§5.2) y facturas. */
public final class Datos {

    public static final String RUC_EMISOR = "20601234565";
    /** RUC del contribuyente de prueba; termina en 4, como la fila de ejemplo del cronograma. */
    public static final String RUC_CONTRIBUYENTE = "10456789124";
    public static final YearMonth SEPTIEMBRE = YearMonth.of(2026, 9);

    private Datos() { }

    /** Parámetros 2026.1 con el cronograma de 2026 completo para el dígito 4 (el 15 del mes siguiente). */
    public static ParametrosNrus parametros() {
        List<CronogramaVencimiento> cronograma = new ArrayList<>();
        for (int mes = 1; mes <= 12; mes++) {
            cronograma.add(new CronogramaVencimiento(2026, mes, 4, YearMonth.of(2026, mes).plusMonths(1).atDay(15)));
        }
        return new ParametrosNrus("2026.1", LocalDate.of(2026, 1, 1), new java.math.BigDecimal("0.80"),
                Montos.de("96000.00"),
                List.of(new CategoriaNRUS(2, Montos.de("8000.00"), Montos.de("50.00")),
                        new CategoriaNRUS(1, Montos.de("5000.00"), Montos.de("20.00"))),
                cronograma);
    }

    public static FacturaCompra factura(String numero, String importe) {
        return factura(numero, importe, LocalDate.of(2026, 9, 22));
    }

    public static FacturaCompra factura(String numero, String importe, LocalDate fecha) {
        return FacturaCompra.nueva(new Emisor(RUC_EMISOR, "DISTRIBUIDORA ANDINA S.A.C."), "F001", numero, fecha,
                Moneda.PEN, Montos.de(importe), OrigenRegistro.IA);
    }

    /** Factura ya guardada, con id. */
    public static FacturaCompra guardada(long id, String numero, String importe) {
        FacturaCompra factura = factura(numero, importe);
        factura.asignarId(id);
        return factura;
    }
}
