package pe.facturass20.ui.verificacion;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.time.YearMonth;
import java.util.Collections;
import java.util.Map;

import pe.facturass20.ContenedorDependencias;
import pe.facturass20.dominio.casosuso.FacturaDuplicadaException;
import pe.facturass20.dominio.casosuso.VerificarFactura;
import pe.facturass20.dominio.modelo.CampoFactura;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.ImagenFactura;
import pe.facturass20.dominio.modelo.OrigenRegistro;
import pe.facturass20.dominio.modelo.ResultadoExtraccion;
import pe.facturass20.ui.captura.FotoCapturada;
import pe.facturass20.ui.comun.BaseViewModel;
import pe.facturass20.ui.comun.Evento;

/**
 * Guardar una factura, igual desde P08 (leída por la IA) que desde P19 (escrita a mano): valida con
 * {@link VerificarFactura}, avisa del duplicado (P09) o pide confirmar un mes anterior, cifra la foto y la
 * registra con {@code RegistrarFactura} (RF-05 a RF-08).
 *
 * <p>La foto se cifra antes de la transacción; si el registro falla, se borra para no dejar archivos
 * sueltos.</p>
 */
public abstract class RegistroFacturaViewModel extends BaseViewModel {

    private final MutableLiveData<Map<CampoFactura, String>> errores = new MutableLiveData<>(Collections.emptyMap());
    private final MutableLiveData<Evento<FacturaCompra>> duplicado = new MutableLiveData<>();
    private final MutableLiveData<Evento<YearMonth>> confirmarPeriodo = new MutableLiveData<>();
    private final MutableLiveData<Evento<Long>> guardada = new MutableLiveData<>();

    protected RegistroFacturaViewModel(@NonNull Application aplicacion) {
        super(aplicacion);
    }

    /** Errores por campo de la última vez que se tocó Guardar; vacío si no hubo. */
    public LiveData<Map<CampoFactura, String>> errores() {
        return errores;
    }

    /** Ya hay una factura vigente con el mismo RUC, serie y número: abrir P09. */
    public LiveData<Evento<FacturaCompra>> duplicado() {
        return duplicado;
    }

    /** La fecha es de un mes anterior: preguntar si se registra en ese mes. */
    public LiveData<Evento<YearMonth>> confirmarPeriodo() {
        return confirmarPeriodo;
    }

    /** Id de la factura guardada, para P10. */
    public LiveData<Evento<Long>> guardada() {
        return guardada;
    }

    /**
     * @param foto       nula si se registra a mano sin foto
     * @param extraccion nula si se registra a mano
     */
    protected final void guardar(VerificarFactura.Datos datos, OrigenRegistro origen, FotoCapturada foto,
            ResultadoExtraccion extraccion, boolean periodoConfirmado) {
        if (Boolean.TRUE.equals(ocupado().getValue())) {
            return;
        }
        ContenedorDependencias c = contenedor();
        enFondo(() -> {
            VerificarFactura.Resultado verificacion = c.verificarFactura().ejecutar(datos, origen);
            if (!verificacion.errores().isEmpty()) {
                return Intento.conErrores(verificacion.errores());
            }
            if (verificacion.duplicado().isPresent()) {
                return Intento.duplicada(verificacion.duplicado().get());
            }
            FacturaCompra factura = verificacion.factura().orElseThrow(IllegalStateException::new);
            if (verificacion.requiereConfirmarPeriodo() && !periodoConfirmado) {
                return Intento.porConfirmar(factura.periodo());
            }
            return registrar(c, factura, foto, extraccion, periodoConfirmado);
        }, intento -> {
            errores.setValue(intento.errores);
            if (intento.duplicado != null) {
                duplicado.setValue(new Evento<>(intento.duplicado));
            } else if (intento.periodo != null) {
                confirmarPeriodo.setValue(new Evento<>(intento.periodo));
            } else if (intento.idFactura != null) {
                c.capturaEnCurso().limpiar();
                guardada.setValue(new Evento<>(intento.idFactura));
            }
        });
    }

    private static Intento registrar(ContenedorDependencias c, FacturaCompra factura, FotoCapturada foto,
            ResultadoExtraccion extraccion, boolean periodoConfirmado) {
        String ruta = foto == null ? null : c.almacenImagenes().guardar(foto.jpeg());
        ImagenFactura imagen = ruta == null ? null
                : new ImagenFactura(ruta, foto.evaluacion().nitidez(), foto.fechaCaptura());
        try {
            FacturaCompra guardada = c.registrarFactura().ejecutar(factura, imagen, extraccion, periodoConfirmado)
                    .factura();
            c.revisarAvisosPronto();
            return Intento.guardada(guardada.id());
        } catch (FacturaDuplicadaException e) {
            borrar(c, ruta);
            return Intento.duplicada(e.existente());
        } catch (RuntimeException e) {
            borrar(c, ruta);
            throw e;
        }
    }

    private static void borrar(ContenedorDependencias c, String ruta) {
        if (ruta != null) {
            c.almacenImagenes().borrar(ruta);
        }
    }

    /** Lo que pasó al tocar Guardar; solo uno de los campos opcionales viene lleno. */
    private static final class Intento {

        final Map<CampoFactura, String> errores;
        final FacturaCompra duplicado;
        final YearMonth periodo;
        final Long idFactura;

        private Intento(Map<CampoFactura, String> errores, FacturaCompra duplicado, YearMonth periodo, Long idFactura) {
            this.errores = errores;
            this.duplicado = duplicado;
            this.periodo = periodo;
            this.idFactura = idFactura;
        }

        static Intento conErrores(Map<CampoFactura, String> errores) {
            return new Intento(errores, null, null, null);
        }

        static Intento duplicada(FacturaCompra existente) {
            return new Intento(Collections.emptyMap(), existente, null, null);
        }

        static Intento porConfirmar(YearMonth periodo) {
            return new Intento(Collections.emptyMap(), null, periodo, null);
        }

        static Intento guardada(long id) {
            return new Intento(Collections.emptyMap(), null, null, id);
        }
    }
}
