package pe.facturass20.ui.reportes;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import pe.facturass20.ContenedorDependencias;
import pe.facturass20.R;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.reglas.Montos;
import pe.facturass20.ui.comun.BaseViewModel;
import pe.facturass20.ui.comun.Evento;

/**
 * P17 Historial y cierre (RF-17, RF-12, RF-21): los meses registrados, del más reciente al más antiguo,
 * el acumulado del año frente al tope anual y el cierre de un mes, que deja generado su reporte PDF.
 */
public final class HistorialViewModel extends BaseViewModel {

    /** @param determinacion nula si el mes todavía no se calculó */
    public record Mes(PeriodoMensual periodo, Determinacion determinacion) { }

    /** @param comprasAnio compras vigentes del año en curso; {@code ventasAnio}, las ventas registradas */
    public record Vista(List<Mes> meses, int anio, BigDecimal comprasAnio, BigDecimal ventasAnio,
            BigDecimal topeAnual) { }

    /** @param conPdf si se pudo generar el reporte PDF del mes cerrado */
    public record Cierre(YearMonth mes, boolean conPdf) { }

    private final MutableLiveData<Vista> vista = new MutableLiveData<>();
    private final MutableLiveData<Evento<Cierre>> cerrado = new MutableLiveData<>();

    public HistorialViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
    }

    public LiveData<Vista> vista() {
        return vista;
    }

    /** El mes que se acaba de cerrar: avisar del PDF y sugerir la exportación (§8.4, cierre_sugerir_exportar). */
    public LiveData<Evento<Cierre>> cerrado() {
        return cerrado;
    }

    public YearMonth mesEnCurso() {
        return YearMonth.from(contenedor().reloj().hoy());
    }

    public void cargar() {
        ContenedorDependencias c = contenedor();
        enFondo(() -> {
            YearMonth actual = YearMonth.from(c.reloj().hoy());
            c.determinarCategoria().ejecutar(actual);
            List<Mes> meses = new ArrayList<>();
            BigDecimal compras = Montos.CERO;
            BigDecimal ventas = Montos.CERO;
            for (PeriodoMensual periodo : c.periodos().listar()) {
                meses.add(new Mes(periodo, c.periodos().buscarDeterminacion(periodo.periodo()).orElse(null)));
                if (periodo.anio() == actual.getYear()) {
                    compras = compras.add(periodo.totalAdquisiciones());
                    ventas = ventas.add(periodo.totalVentas());
                }
            }
            return new Vista(meses, actual.getYear(), compras, ventas, c.parametros().vigentes().topeAnual());
        }, vista::setValue);
    }

    /**
     * Cierra el mes y genera su reporte PDF con la determinación ya congelada (§8.5), así queda listo sin
     * abrir P16. Si el PDF falla, el mes queda cerrado igual: P16 lo vuelve a generar al compartirlo.
     */
    public void cerrar(YearMonth mes) {
        ContenedorDependencias c = contenedor();
        Application aplicacion = getApplication();
        String sinConfigurar = texto(R.string.error_sin_configurar);
        enFondo(() -> {
            c.cerrarPeriodo().ejecutar(mes);
            try {
                GeneradorReporte.generar(aplicacion, c, GeneradorReporte.datos(c, mes, sinConfigurar));
                return new Cierre(mes, true);
            } catch (IOException | RuntimeException e) {
                Log.e("FacturasS20", "No se generó el PDF al cerrar el mes: " + e.getClass().getName());
                return new Cierre(mes, false);
            }
        }, cierre -> {
            cerrado.setValue(new Evento<>(cierre));
            cargar();
        });
    }
}
