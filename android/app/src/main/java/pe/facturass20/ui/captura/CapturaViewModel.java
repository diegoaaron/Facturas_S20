package pe.facturass20.ui.captura;

import android.app.Application;
import android.graphics.Rect;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;

import pe.facturass20.R;
import pe.facturass20.ui.comun.BaseViewModel;
import pe.facturass20.ui.comun.Evento;

/**
 * Prepara la foto de la cámara (P06) o de la galería (P05 y P06) en segundo plano y, si se puede leer,
 * la deja en {@link CapturaEnCurso} para P07. Si no, avisa «Tome otra foto» (RF-02).
 */
public class CapturaViewModel extends BaseViewModel {

    private final MutableLiveData<Evento<Boolean>> fotoLista = new MutableLiveData<>();
    private final MutableLiveData<Boolean> conLectorIa = new MutableLiveData<>();

    public CapturaViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
    }

    /**
     * Averigua en segundo plano si hay lectura con IA, para que P05 lleve la foto a P07 o a P19 (§8.2). Sin
     * {@code enFondo}: no debe desactivar los botones mientras consulta.
     */
    public void comprobarLectorIa() {
        if (conLectorIa.getValue() == null) {
            contenedor().ejecutor().execute(() -> conLectorIa.postValue(contenedor().hayLectorIa()));
        }
    }

    /** Nulo mientras no se sabe. */
    public LiveData<Boolean> conLectorIa() {
        return conLectorIa;
    }

    /** Se dispara una vez cuando la foto quedó en {@link CapturaEnCurso}. */
    public LiveData<Evento<Boolean>> fotoLista() {
        return fotoLista;
    }

    /** Ver {@link PreparadorFoto#desdeCamara}; los datos ya se copiaron del {@code ImageProxy}. */
    public void desdeCamara(byte[] jpeg, int ancho, int alto, Rect visible, int giro, MarcoEncuadre marco) {
        enFondo(() -> {
            long inicio = SystemClock.elapsedRealtime();
            FotoCapturada foto = PreparadorFoto.desdeCamara(jpeg, ancho, alto, visible, giro, marco,
                    contenedor().reloj().ahora());
            Log.i("FacturasS20", "Foto preparada (con la búsqueda del QR) en "
                    + (SystemClock.elapsedRealtime() - inicio) + " ms");
            return foto;
        }, foto -> revisar(foto, false));
    }

    public void desdeGaleria(Uri uri) {
        enFondo(() -> {
            try {
                return PreparadorFoto.desdeGaleria(getApplication().getContentResolver(), uri, contenedor().reloj().ahora());
            } catch (IOException | PreparadorFoto.FotoInvalidaException | SecurityException e) {
                return null;
            }
        }, foto -> revisar(foto, true));
    }

    private void revisar(FotoCapturada foto, boolean deGaleria) {
        if (foto == null) {
            mostrarMensaje(texto(R.string.captura_imagen_invalida));
            return;
        }
        EvaluadorNitidez.Evaluacion evaluacion = foto.evaluacion();
        // Para calibrar los umbrales con fotos reales: adb logcat -s FacturasS20
        Log.d("FacturasS20", String.format(java.util.Locale.ROOT, "Foto: nitidez %.1f, luz %.1f",
                evaluacion.nitidez(), evaluacion.luz()));
        if (!(deGaleria ? evaluacion.imagenAceptable() : evaluacion.fotoAceptable())) {
            mostrarMensaje(texto(motivo(evaluacion)));
            return;
        }
        contenedor().capturaEnCurso().poner(foto);
        fotoLista.setValue(new Evento<>(true));
    }

    private static int motivo(EvaluadorNitidez.Evaluacion evaluacion) {
        switch (evaluacion.nivelLuz()) {
            case BAJA:
                return R.string.captura_foto_oscura;
            case EXCESIVA:
                return R.string.captura_foto_brillo;
            default:
                return R.string.captura_foto_borrosa;
        }
    }
}
