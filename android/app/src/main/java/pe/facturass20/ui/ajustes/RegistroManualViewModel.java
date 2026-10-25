package pe.facturass20.ui.ajustes;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.time.LocalDate;
import java.util.Map;

import pe.facturass20.dominio.casosuso.VerificarFactura;
import pe.facturass20.dominio.modelo.CampoFactura;
import pe.facturass20.dominio.modelo.Emisor;
import pe.facturass20.dominio.modelo.Moneda;
import pe.facturass20.dominio.modelo.OrigenRegistro;
import pe.facturass20.dominio.reglas.ValidadorRuc;
import pe.facturass20.ui.captura.FotoCapturada;
import pe.facturass20.ui.verificacion.RegistroFacturaViewModel;
import pe.facturass20.ui.verificacion.RevisionCampo;

/**
 * P19 Registro manual (RF-01, RF-06): los mismos campos y validaciones que P08, con {@code origen = MANUAL}
 * y sin lectura de la IA. Autocompleta la razón social si el proveedor ya se registró antes, y admite una
 * foto opcional que se guarda cifrada igual que las demás.
 */
public final class RegistroManualViewModel extends RegistroFacturaViewModel {

    private final MutableLiveData<Emisor> emisorConocido = new MutableLiveData<>();
    private final MutableLiveData<Bitmap> miniatura = new MutableLiveData<>();
    private FotoCapturada foto;
    private String ultimoRucBuscado;

    public RegistroManualViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
    }

    /** El proveedor que ya estaba registrado con el RUC escrito; nulo si no hay. */
    public LiveData<Emisor> emisorConocido() {
        return emisorConocido;
    }

    /** La foto adjunta, ya reducida; nula si no hay. */
    public LiveData<Bitmap> miniatura() {
        return miniatura;
    }

    public LocalDate hoy() {
        return contenedor().reloj().hoy();
    }

    /** Adjunta la foto de {@code CapturaEnCurso}: la de una lectura fallida o la que se acaba de elegir. */
    public void adjuntarFotoCapturada() {
        FotoCapturada capturada = contenedor().capturaEnCurso().foto();
        if (capturada == null || capturada == foto) {
            return;
        }
        foto = capturada;
        byte[] jpeg = capturada.jpeg();
        enFondo(() -> BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length), miniatura::setValue);
    }

    public boolean tieneFoto() {
        return foto != null;
    }

    public RevisionCampo revisar(CampoFactura campo, String texto) {
        return RevisionCampo.revisar(campo, texto, null, null);
    }

    /** Busca el proveedor cuando el RUC escrito es válido; cada RUC se busca una sola vez. */
    public void alCambiarRuc(String ruc) {
        if (!ValidadorRuc.esValido(ruc)) {
            ultimoRucBuscado = null;
            emisorConocido.setValue(null);
            return;
        }
        if (ruc.equals(ultimoRucBuscado)) {
            return;
        }
        ultimoRucBuscado = ruc;
        enFondo(() -> contenedor().facturas().buscarEmisor(ruc), encontrado -> {
            if (ruc.equals(ultimoRucBuscado)) {
                emisorConocido.setValue(encontrado.orElse(null));
            }
        });
    }

    public void guardar(Map<CampoFactura, String> textos, boolean periodoConfirmado) {
        VerificarFactura.Datos datos = new VerificarFactura.Datos(
                textos.get(CampoFactura.RUC), textos.get(CampoFactura.RAZON_SOCIAL), textos.get(CampoFactura.SERIE),
                textos.get(CampoFactura.NUMERO), textos.get(CampoFactura.FECHA_EMISION), Moneda.PEN.name(),
                textos.get(CampoFactura.IMPORTE_TOTAL));
        guardar(datos, OrigenRegistro.MANUAL, foto, null, periodoConfirmado);
    }
}
