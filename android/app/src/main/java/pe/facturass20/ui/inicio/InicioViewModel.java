package pe.facturass20.ui.inicio;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.time.LocalDate;
import java.time.YearMonth;

import pe.facturass20.ContenedorDependencias;
import pe.facturass20.R;
import pe.facturass20.dominio.modelo.Contribuyente;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.modelo.ReglaNegocioException;
import pe.facturass20.ui.comun.BaseViewModel;

/**
 * P04 Inicio: resumen del mes en curso. Cada vez que la pantalla vuelve al frente recalcula la
 * determinación del mes (lo crea si todavía no existe) y lee sus facturas.
 */
public final class InicioViewModel extends BaseViewModel {

    private final MutableLiveData<ResumenInicio> resumen = new MutableLiveData<>();

    public InicioViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
    }

    /** Nulo hasta la primera carga. */
    public LiveData<ResumenInicio> resumen() {
        return resumen;
    }

    public void cargar() {
        if (Boolean.TRUE.equals(ocupado().getValue())) {
            return;
        }
        ContenedorDependencias c = contenedor();
        enFondo(() -> {
            LocalDate hoy = c.reloj().hoy();
            YearMonth mes = YearMonth.from(hoy);
            Contribuyente contribuyente = c.contribuyentes().obtener()
                    .orElseThrow(() -> new ReglaNegocioException(texto(R.string.error_sin_configurar)));
            Determinacion determinacion = c.determinarCategoria().ejecutar(mes);
            PeriodoMensual periodo = c.periodos().buscar(mes).orElseGet(() -> PeriodoMensual.nuevo(mes));
            return ResumenInicio.de(contribuyente, periodo, determinacion, c.parametros().vigentes(), hoy);
        }, resumen::setValue);
    }
}
