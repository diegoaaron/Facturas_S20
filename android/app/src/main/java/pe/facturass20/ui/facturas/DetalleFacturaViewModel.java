package pe.facturass20.ui.facturas;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.Optional;

import pe.facturass20.ContenedorDependencias;
import pe.facturass20.R;
import pe.facturass20.dominio.modelo.CampoExtraido;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.ImagenFactura;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.modelo.ReglaNegocioException;
import pe.facturass20.ui.comun.BaseViewModel;

/** P12 Detalle y anulación (RF-18): la factura, su foto descifrada en memoria y la anulación con motivo. */
public final class DetalleFacturaViewModel extends BaseViewModel {

    private static final String ETIQUETA = "FacturasS20";

    /**
     * @param imagen     nula si se registró sin foto o no se pudo descifrar
     * @param corregidos campos que el usuario cambió respecto de lo que leyó la IA
     * @param mesAbierto solo se anula en un mes abierto
     */
    public record Vista(FacturaCompra factura, Bitmap imagen, int corregidos, boolean mesAbierto) { }

    private final MutableLiveData<Vista> vista = new MutableLiveData<>();
    private long idFactura;

    public DetalleFacturaViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
    }

    public LiveData<Vista> vista() {
        return vista;
    }

    public void cargar(long idFactura) {
        this.idFactura = idFactura;
        ContenedorDependencias c = contenedor();
        enFondo(() -> {
            FacturaCompra factura = c.facturas().buscarPorId(idFactura)
                    .orElseThrow(() -> new ReglaNegocioException(texto(R.string.error_factura_no_existe)));
            boolean abierto = c.periodos().buscar(factura.periodo()).map(PeriodoMensual::estaAbierto).orElse(true);
            int corregidos = 0;
            List<CampoExtraido> campos = c.facturas().buscarCampos(idFactura);
            for (CampoExtraido campo : campos) {
                if (campo.corregido()) {
                    corregidos++;
                }
            }
            return new Vista(factura, imagen(c, idFactura), corregidos, abierto);
        }, vista::setValue);
    }

    public void anular(String motivo) {
        enFondo(() -> contenedor().anularFactura().ejecutar(idFactura, motivo), determinacion -> {
            mostrarMensaje(texto(R.string.detalle_anulada_ok));
            cargar(idFactura);
        });
    }

    private static Bitmap imagen(ContenedorDependencias c, long idFactura) {
        Optional<ImagenFactura> imagen = c.facturas().buscarImagen(idFactura);
        if (!imagen.isPresent()) {
            return null;
        }
        try {
            byte[] jpeg = c.almacenImagenes().leer(imagen.get().ruta());
            return BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
        } catch (RuntimeException e) {
            // La factura se muestra igual; solo falta la foto.
            Log.e(ETIQUETA, "No se pudo leer la foto de la factura: " + e.getClass().getName());
            return null;
        }
    }
}
