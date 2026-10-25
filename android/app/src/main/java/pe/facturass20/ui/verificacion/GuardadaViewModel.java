package pe.facturass20.ui.verificacion;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.time.YearMonth;

import pe.facturass20.ContenedorDependencias;
import pe.facturass20.R;
import pe.facturass20.dominio.modelo.Contribuyente;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.modelo.ReglaNegocioException;
import pe.facturass20.ui.comun.BaseViewModel;
import pe.facturass20.ui.inicio.ResumenInicio;

/** P10 Guardada (RF-08, RF-12): la factura recién guardada y cómo quedó el mes de esa factura. */
public final class GuardadaViewModel extends BaseViewModel {

    /** La factura y el resumen de su mes, con el mismo cálculo de la barra que P04. */
    public record Vista(FacturaCompra factura, ResumenInicio mes) { }

    private final MutableLiveData<Vista> vista = new MutableLiveData<>();

    public GuardadaViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
    }

    public LiveData<Vista> vista() {
        return vista;
    }

    public void cargar(long idFactura) {
        if (vista.getValue() != null) {
            return;
        }
        ContenedorDependencias c = contenedor();
        enFondo(() -> {
            FacturaCompra factura = c.facturas().buscarPorId(idFactura)
                    .orElseThrow(() -> new ReglaNegocioException(texto(R.string.error_factura_no_existe)));
            YearMonth mes = factura.periodo();
            Contribuyente contribuyente = c.contribuyentes().obtener()
                    .orElseThrow(() -> new ReglaNegocioException(texto(R.string.error_sin_configurar)));
            PeriodoMensual periodo = c.periodos().buscar(mes).orElseGet(() -> PeriodoMensual.nuevo(mes));
            Determinacion determinacion = c.periodos().buscarDeterminacion(mes)
                    .orElseGet(() -> c.determinarCategoria().ejecutar(mes));
            return new Vista(factura, ResumenInicio.de(contribuyente, periodo, determinacion,
                    c.parametros().vigentes(), c.reloj().hoy()));
        }, vista::setValue);
    }
}
