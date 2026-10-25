package pe.facturass20.ui.facturas;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.time.YearMonth;

import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.ui.comun.BaseViewModel;

/** P11 Facturas del mes: lee el mes con todas sus facturas; el filtro y la búsqueda los aplica {@link ListadoFacturas}. */
public final class ListaFacturasViewModel extends BaseViewModel {

    private final MutableLiveData<PeriodoMensual> periodo = new MutableLiveData<>();
    private ListadoFacturas.Filtro filtro = ListadoFacturas.Filtro.TODAS;
    private String busqueda = "";

    public ListaFacturasViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
    }

    /** Nulo hasta la primera carga. */
    public LiveData<PeriodoMensual> periodo() {
        return periodo;
    }

    public ListadoFacturas.Filtro filtro() {
        return filtro;
    }

    public void filtrar(ListadoFacturas.Filtro filtro) {
        this.filtro = filtro;
    }

    public String busqueda() {
        return busqueda;
    }

    public void buscar(String busqueda) {
        this.busqueda = busqueda;
    }

    /** Se llama al volver a la pantalla, por si se anuló o registró una factura. */
    public void cargar(String argumentoPeriodo) {
        YearMonth mes = periodoDe(argumentoPeriodo);
        enFondo(() -> contenedor().periodos().buscar(mes).orElseGet(() -> PeriodoMensual.nuevo(mes)), periodo::setValue);
    }
}
