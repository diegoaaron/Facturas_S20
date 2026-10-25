package pe.facturass20.ui.facturas;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.OrigenRegistro;
import pe.facturass20.dominio.modelo.PeriodoMensual;

/**
 * Lo que muestra P11 a partir de las facturas de un mes: filtro, búsqueda y grupos por semana (de lunes a
 * domingo, recortada a los días del mes), de la más reciente a la más antigua. Es Java puro para probarlo
 * en la JVM.
 */
public final class ListadoFacturas {

    /** «Todas», «Leídas por IA» y «Manuales» son las vigentes; «Anuladas», las anuladas. */
    public enum Filtro { TODAS, IA, MANUALES, ANULADAS }

    /** Facturas de una semana; {@code desde} y {@code hasta} son días del mes. */
    public record Semana(int desde, int hasta, List<FacturaCompra> facturas) { }

    private ListadoFacturas() { }

    public static boolean pasa(FacturaCompra factura, Filtro filtro) {
        switch (filtro) {
            case IA:
                return factura.esVigente() && factura.origen() == OrigenRegistro.IA;
            case MANUALES:
                return factura.esVigente() && factura.origen() == OrigenRegistro.MANUAL;
            case ANULADAS:
                return !factura.esVigente();
            default:
                return factura.esVigente();
        }
    }

    public static int contar(PeriodoMensual periodo, Filtro filtro) {
        int total = 0;
        for (FacturaCompra factura : periodo.facturas()) {
            if (pasa(factura, filtro)) {
                total++;
            }
        }
        return total;
    }

    /** Busca en el proveedor, el RUC y la serie-número, sin distinguir mayúsculas. */
    public static boolean coincide(FacturaCompra factura, String busqueda) {
        if (busqueda == null || busqueda.trim().isEmpty()) {
            return true;
        }
        String texto = busqueda.trim().toLowerCase(Locale.ROOT);
        return factura.emisor().razonSocial().toLowerCase(Locale.ROOT).contains(texto)
                || factura.emisor().ruc().contains(texto)
                || (factura.serie() + "-" + factura.numero()).toLowerCase(Locale.ROOT).contains(texto);
    }

    public static List<Semana> agrupar(PeriodoMensual periodo, Filtro filtro, String busqueda) {
        List<FacturaCompra> elegidas = new ArrayList<>();
        for (FacturaCompra factura : periodo.facturas()) {
            if (pasa(factura, filtro) && coincide(factura, busqueda)) {
                elegidas.add(factura);
            }
        }
        elegidas.sort(Comparator.comparing(FacturaCompra::fechaEmision)
                .thenComparing(f -> f.id() == null ? Long.MAX_VALUE : f.id())
                .reversed());

        YearMonth mes = periodo.periodo();
        List<Semana> semanas = new ArrayList<>();
        LocalDate lunesActual = null;
        List<FacturaCompra> actual = null;
        for (FacturaCompra factura : elegidas) {
            LocalDate lunes = factura.fechaEmision().with(DayOfWeek.MONDAY);
            if (!lunes.equals(lunesActual)) {
                lunesActual = lunes;
                actual = new ArrayList<>();
                semanas.add(new Semana(Math.max(1, diaDelMes(lunes, mes)),
                        Math.min(mes.lengthOfMonth(), diaDelMes(lunes.plusDays(6), mes)), actual));
            }
            actual.add(factura);
        }
        List<Semana> resultado = new ArrayList<>();
        for (Semana semana : semanas) {
            resultado.add(new Semana(semana.desde(), semana.hasta(), Collections.unmodifiableList(semana.facturas())));
        }
        return resultado;
    }

    /** Suma de las facturas vigentes del mes (el total del pie de P11, igual que el acumulado de P04). */
    public static BigDecimal total(PeriodoMensual periodo) {
        return periodo.totalAdquisiciones();
    }

    /** Día del mes de {@code fecha}, o fuera de rango (0 o 32) si cae en el mes anterior o siguiente. */
    private static int diaDelMes(LocalDate fecha, YearMonth mes) {
        YearMonth suyo = YearMonth.from(fecha);
        if (suyo.isBefore(mes)) {
            return 0;
        }
        if (suyo.isAfter(mes)) {
            return 32;
        }
        return fecha.getDayOfMonth();
    }
}
