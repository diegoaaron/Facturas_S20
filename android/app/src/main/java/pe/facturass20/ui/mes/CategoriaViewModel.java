package pe.facturass20.ui.mes;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.time.YearMonth;

import pe.facturass20.ContenedorDependencias;
import pe.facturass20.R;
import pe.facturass20.dominio.modelo.Contribuyente;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.ParametrosNrus;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.modelo.ReglaNegocioException;
import pe.facturass20.ui.comun.BaseViewModel;
import pe.facturass20.ui.inicio.ResumenInicio;

/**
 * P14 Categoría y cuota (RF-10 a RF-12, RF-22) y P15 Vencimiento (RF-13): la determinación del mes en
 * curso, recalculada al volver a la pantalla, con los parámetros vigentes del NRUS.
 */
public final class CategoriaViewModel extends BaseViewModel {

    /** El resumen del mes (el mismo cálculo que P04), los parámetros y el último dígito del RUC. */
    public record Vista(ResumenInicio resumen, ParametrosNrus parametros, int ultimoDigitoRuc) { }

    private final MutableLiveData<Vista> vista = new MutableLiveData<>();

    public CategoriaViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
    }

    public LiveData<Vista> vista() {
        return vista;
    }

    public boolean avisoActivado(PreferenciasAvisos.Aviso aviso) {
        return contenedor().preferenciasAvisos().activado(aviso);
    }

    public void activarAviso(PreferenciasAvisos.Aviso aviso, boolean activado) {
        contenedor().preferenciasAvisos().activar(aviso, activado);
    }

    public void cargar() {
        ContenedorDependencias c = contenedor();
        enFondo(() -> {
            YearMonth mes = YearMonth.from(c.reloj().hoy());
            Contribuyente contribuyente = c.contribuyentes().obtener()
                    .orElseThrow(() -> new ReglaNegocioException(texto(R.string.error_sin_configurar)));
            Determinacion determinacion = c.determinarCategoria().ejecutar(mes);
            PeriodoMensual periodo = c.periodos().buscar(mes).orElseGet(() -> PeriodoMensual.nuevo(mes));
            ParametrosNrus parametros = c.parametros().vigentes();
            return new Vista(ResumenInicio.de(contribuyente, periodo, determinacion, parametros, c.reloj().hoy()),
                    parametros, contribuyente.ultimoDigitoRuc());
        }, vista::setValue);
    }
}
