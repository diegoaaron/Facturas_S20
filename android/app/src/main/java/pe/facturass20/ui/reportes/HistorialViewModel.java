package pe.facturass20.ui.reportes;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import pe.facturass20.ContenedorDependencias;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.reglas.Montos;
import pe.facturass20.ui.comun.BaseViewModel;
import pe.facturass20.ui.comun.Evento;

/**
 * P17 Historial y cierre (RF-17, RF-12, RF-21): los meses registrados, del más reciente al más antiguo,
 * el acumulado del año frente al tope anual y el cierre de un mes.
 */
public final class HistorialViewModel extends BaseViewModel {

    /** @param determinacion nula si el mes todavía no se calculó */
    public record Mes(PeriodoMensual periodo, Determinacion determinacion) { }

    /** @param comprasAnio compras vigentes del año en curso; {@code ventasAnio}, las ventas registradas */
    public record Vista(List<Mes> meses, int anio, BigDecimal comprasAnio, BigDecimal ventasAnio,
            BigDecimal topeAnual) { }

    private final MutableLiveData<Vista> vista = new MutableLiveData<>();
    private final MutableLiveData<Evento<YearMonth>> cerrado = new MutableLiveData<>();

    public HistorialViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
    }

    public LiveData<Vista> vista() {
        return vista;
    }

    /** El mes que se acaba de cerrar: sugerir la exportación (§8.4, cierre_sugerir_exportar). */
    public LiveData<Evento<YearMonth>> cerrado() {
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

    public void cerrar(YearMonth mes) {
        enFondo(() -> contenedor().cerrarPeriodo().ejecutar(mes), determinacion -> {
            cerrado.setValue(new Evento<>(mes));
            cargar();
        });
    }
}
