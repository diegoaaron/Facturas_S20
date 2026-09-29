package pe.facturass20.ui.inicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import pe.facturass20.dominio.modelo.CategoriaNRUS;
import pe.facturass20.dominio.modelo.Contribuyente;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.NivelAlerta;
import pe.facturass20.dominio.modelo.ParametrosNrus;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.reglas.Montos;

/**
 * Lo que muestra P04 Inicio sobre el mes en curso (RF-08, RF-10 a RF-13), ya calculado y sin textos:
 * el Fragment solo le da formato. Es Java puro para probarlo en la JVM.
 *
 * @param primerNombre      primera palabra del titular, para el saludo
 * @param numeroFacturas    facturas vigentes del mes
 * @param limite            límite mensual de la categoría del mes (de la última si está fuera del NRUS)
 * @param porcentaje        monto determinante sobre {@code limite}, en entero hacia abajo; puede pasar de 100
 * @param restante          lo que falta para el límite, o 0
 * @param siguienteCategoria código de la categoría siguiente; nulo si ya es la última o está fuera
 * @param ventas            nulo si todavía no se registran las ventas del mes
 * @param ventasDeterminan  las ventas son mayores que las compras: son ellas las que deciden la categoría
 * @param umbralAviso       fracción del límite donde va la marca de la barra, p. ej. 0,80
 * @param diasParaVencer    días desde hoy hasta la fecha límite; negativo si ya pasó
 * @param ultimas           hasta 3 facturas vigentes, de la más reciente a la más antigua
 */
public record ResumenInicio(String primerNombre, YearMonth periodo, BigDecimal compras, int numeroFacturas,
        NivelAlerta nivel, Integer categoria, BigDecimal cuota, BigDecimal limite, int porcentaje,
        BigDecimal restante, Integer siguienteCategoria, BigDecimal ventas, boolean ventasDeterminan,
        BigDecimal umbralAviso, boolean avisoTopeAnual, BigDecimal topeAnual, LocalDate vencimiento,
        long diasParaVencer, List<FacturaCompra> ultimas) {

    public static final int FACTURAS_RECIENTES = 3;

    public static ResumenInicio de(Contribuyente contribuyente, PeriodoMensual periodo, Determinacion determinacion,
            ParametrosNrus parametros, LocalDate hoy) {
        List<CategoriaNRUS> categorias = parametros.categoriasVigentes(periodo.periodo().atDay(1));
        CategoriaNRUS actual = determinacion.categoria();
        CategoriaNRUS referencia = actual != null ? actual : categorias.get(categorias.size() - 1);
        BigDecimal determinante = determinacion.montoDeterminante();
        BigDecimal limite = referencia.limiteMensual();

        int porcentaje = determinante.multiply(BigDecimal.valueOf(100))
                .divide(limite, 0, RoundingMode.DOWN).intValue();
        BigDecimal restante = limite.subtract(determinante);
        if (restante.signum() < 0) {
            restante = Montos.CERO;
        }

        Integer siguiente = null;
        if (actual != null) {
            int indice = indiceDe(categorias, actual);
            if (indice >= 0 && indice + 1 < categorias.size()) {
                siguiente = categorias.get(indice + 1).codigo();
            }
        }

        BigDecimal compras = determinacion.totalAdquisiciones();
        BigDecimal ventas = periodo.ventasRegistradas() ? periodo.totalVentas() : null;
        boolean ventasDeterminan = ventas != null && ventas.compareTo(compras) > 0;

        List<FacturaCompra> vigentes = periodo.facturasVigentes();
        return new ResumenInicio(primerNombre(contribuyente.titular()), periodo.periodo(), compras, vigentes.size(),
                determinacion.nivelAlerta(), actual == null ? null : actual.codigo(), determinacion.cuota(), limite,
                porcentaje, restante, siguiente, ventas, ventasDeterminan, parametros.umbralAviso(),
                determinacion.avisoTopeAnual(), parametros.topeAnual(), determinacion.fechaVencimiento(),
                ChronoUnit.DAYS.between(hoy, determinacion.fechaVencimiento()), recientes(vigentes));
    }

    /** Porcentaje para la barra de progreso, de 0 a 100. */
    public int progreso() {
        return Math.max(0, Math.min(porcentaje, 100));
    }

    public boolean fueraDeRegimen() {
        return nivel == NivelAlerta.FUERA_DE_REGIMEN;
    }

    /** Hay algo que avisar: la campana muestra el punto. */
    public boolean hayAvisos() {
        return nivel != NivelAlerta.NINGUNA || avisoTopeAnual;
    }

    private static int indiceDe(List<CategoriaNRUS> categorias, CategoriaNRUS categoria) {
        for (int i = 0; i < categorias.size(); i++) {
            if (categorias.get(i).codigo() == categoria.codigo()) {
                return i;
            }
        }
        return -1;
    }

    private static List<FacturaCompra> recientes(List<FacturaCompra> vigentes) {
        List<FacturaCompra> ordenadas = new ArrayList<>(vigentes);
        // Por fecha de emisión y, en el mismo día, la última registrada primero.
        ordenadas.sort(Comparator.comparing(FacturaCompra::fechaEmision)
                .thenComparing(f -> f.id() == null ? Long.MAX_VALUE : f.id())
                .reversed());
        return Collections.unmodifiableList(
                new ArrayList<>(ordenadas.subList(0, Math.min(FACTURAS_RECIENTES, ordenadas.size()))));
    }

    private static String primerNombre(String titular) {
        String[] partes = titular.trim().split("\\s+");
        return partes.length == 0 ? titular : partes[0];
    }
}
