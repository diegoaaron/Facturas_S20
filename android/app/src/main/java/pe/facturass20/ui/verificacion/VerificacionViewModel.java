package pe.facturass20.ui.verificacion;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.EnumMap;
import java.util.Map;

import pe.facturass20.dominio.casosuso.VerificarFactura;
import pe.facturass20.dominio.modelo.CampoExtraido;
import pe.facturass20.dominio.modelo.CampoFactura;
import pe.facturass20.dominio.modelo.OrigenRegistro;
import pe.facturass20.dominio.modelo.ResultadoExtraccion;
import pe.facturass20.ui.captura.FotoCapturada;

/**
 * P08 Verificación (RF-05, RF-06): la foto y los 7 campos que leyó la IA para que el usuario los revise.
 * Al guardar, los valores finales reemplazan a los leídos en la extracción, así queda registrado qué
 * campos corrigió (sirve para medir la exactitud real del modelo, RNF-01).
 */
public final class VerificacionViewModel extends RegistroFacturaViewModel {

    private final MutableLiveData<Bitmap> imagen = new MutableLiveData<>();
    private FotoCapturada foto;
    private ResultadoExtraccion extraccion;
    private final Map<CampoFactura, String> textosLeidos = new EnumMap<>(CampoFactura.class);

    public VerificacionViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
    }

    /**
     * Toma la foto y la lectura de {@code CapturaEnCurso}. Devuelve {@code false} si ya no están (el
     * sistema cerró la app): hay que volver a tomar la foto.
     */
    public boolean iniciar() {
        if (extraccion != null) {
            return true;
        }
        foto = contenedor().capturaEnCurso().foto();
        extraccion = contenedor().capturaEnCurso().extraccion();
        if (foto == null || extraccion == null) {
            return false;
        }
        for (CampoFactura campo : CampoFactura.values()) {
            textosLeidos.put(campo, RevisionCampo.paraMostrar(campo,
                    extraccion.campo(campo).map(CampoExtraido::valorFinal).orElse(null)));
        }
        byte[] jpeg = foto.jpeg();
        enFondo(() -> BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length), imagen::setValue);
        return true;
    }

    public LiveData<Bitmap> imagen() {
        return imagen;
    }

    /** Lo que se escribe en cada campo al abrir la pantalla. */
    public String textoLeido(CampoFactura campo) {
        return textosLeidos.get(campo);
    }

    public RevisionCampo revisar(CampoFactura campo, String texto) {
        return RevisionCampo.revisar(campo, texto, textosLeidos.get(campo), extraccion.campo(campo).orElse(null));
    }

    /** Campos que la IA leyó con algún valor. */
    public int camposLeidos() {
        int leidos = 0;
        for (CampoExtraido campo : extraccion.campos()) {
            if (campo.valorLeido() != null) {
                leidos++;
            }
        }
        return leidos;
    }

    public void guardar(Map<CampoFactura, String> textos, boolean periodoConfirmado) {
        ResultadoExtraccion revisada = extraccion;
        for (Map.Entry<CampoFactura, String> campo : textos.entrySet()) {
            String texto = campo.getValue().trim();
            if (!texto.equals(textosLeidos.get(campo.getKey()))) {
                revisada = revisada.conValorFinal(campo.getKey(), texto);
            }
        }
        VerificarFactura.Datos datos = new VerificarFactura.Datos(
                textos.get(CampoFactura.RUC), textos.get(CampoFactura.RAZON_SOCIAL), textos.get(CampoFactura.SERIE),
                textos.get(CampoFactura.NUMERO), textos.get(CampoFactura.FECHA_EMISION),
                textos.get(CampoFactura.MONEDA), textos.get(CampoFactura.IMPORTE_TOTAL));
        guardar(datos, OrigenRegistro.IA, foto, revisada, periodoConfirmado);
    }
}
