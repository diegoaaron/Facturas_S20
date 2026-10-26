package pe.facturass20.inferencia.litert

import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import pe.facturass20.inferencia.InferenciaException
import pe.facturass20.inferencia.MotorInferencia
import pe.facturass20.inferencia.ParserRespuesta
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Único archivo Kotlin de la app (documentación técnica §6.5): implementa [MotorInferencia] con LiteRT-LM,
 * cuya API es Kotlin. Por defecto usa la CPU para el texto y la GPU para la imagen y, si el teléfono no
 * tiene GPU compatible, la CPU para todo (es lo más rápido medido en el Moto G34; ver la documentación
 * técnica §6.5).
 *
 * La respuesta llega por partes; en cuanto se cierra el primer objeto JSON se corta la generación, porque
 * lo que el modelo agregue después no se usa. También se corta si pasa de
 * [ParserRespuesta.MAX_TOKENS_RESPUESTA] sin cerrarlo: a veces el modelo se queda repitiendo un carácter
 * y, sin este corte, la lectura duraría hasta el tiempo límite.
 *
 * @param carpetaCache carpeta privada donde LiteRT-LM guarda lo que acelera las siguientes cargas
 * @param modo         qué procesador usar; [Modo.AUTOMATICO] salvo en las mediciones
 */
class LiteRtLmPuente @JvmOverloads constructor(
    private val carpetaCache: String?,
    private val modo: Modo = Modo.AUTOMATICO,
) : MotorInferencia {

    enum class Modo {
        /** [CPU_CON_VISION_GPU] y, si no se puede, [CPU]. */
        AUTOMATICO,
        CPU,
        /** CPU para el texto y GPU para la imagen. */
        CPU_CON_VISION_GPU,
        /** GPU para todo; solo para las mediciones, es más lenta que la CPU en el Moto G34. */
        GPU,
    }

    private var motor: Engine? = null

    /** Qué procesador quedó en uso, para Logcat y las mediciones (RNF-04). */
    var procesador: String = ""
        private set

    /** Métricas de LiteRT-LM de la última lectura (tokens y velocidad), para las mediciones. */
    var ultimaMedicion: String = ""
        private set

    override fun inicializar(rutaModelo: String) {
        close()
        if (modo != Modo.AUTOMATICO) {
            motor = try {
                crear(rutaModelo, modo)
            } catch (e: Exception) {
                throw InferenciaException("No se pudo cargar el modelo con $modo", e)
            }
            return
        }
        motor = try {
            crear(rutaModelo, Modo.CPU_CON_VISION_GPU)
        } catch (e: Exception) {
            Log.w(ETIQUETA, "Sin GPU para la imagen (${e.javaClass.simpleName}); se usa solo la CPU")
            try {
                crear(rutaModelo, Modo.CPU)
            } catch (e2: Exception) {
                throw InferenciaException("No se pudo cargar el modelo", e2)
            }
        }
    }

    private fun crear(rutaModelo: String, modo: Modo): Engine {
        val texto = if (modo == Modo.GPU) Backend.GPU() else Backend.CPU()
        val vision = if (modo == Modo.CPU) Backend.CPU() else Backend.GPU()
        return crear(rutaModelo, texto, vision).also { procesador = modo.name }
    }

    private fun crear(rutaModelo: String, texto: Backend, vision: Backend): Engine {
        val nuevo = Engine(
            EngineConfig(
                modelPath = rutaModelo,
                backend = texto,
                visionBackend = vision,
                cacheDir = carpetaCache,
            )
        )
        try {
            nuevo.initialize()
        } catch (e: Exception) {
            nuevo.close()
            throw e
        }
        return nuevo
    }

    override fun inicializado(): Boolean = motor != null

    override fun generar(jpeg: ByteArray, instruccion: String, timeoutMs: Long): String {
        val cargado = motor ?: throw InferenciaException("El modelo no está cargado", null)
        // Siempre el token más probable: para extraer datos no se quiere variedad.
        val configuracion = ConversationConfig(samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 1.0))
        val conversacion = try {
            cargado.createConversation(configuracion)
        } catch (e: Exception) {
            throw InferenciaException("No se pudo abrir la conversación", e)
        }
        val texto = StringBuilder()
        val fin = CountDownLatch(1)
        var error: Throwable? = null
        // Se cortó a propósito (JSON completo o demasiado largo): el CANCELLED que llega después no es un error.
        var cortado = false
        var tokens = 0
        conversacion.sendMessageAsync(
            Contents.of(Content.ImageBytes(jpeg), Content.Text(instruccion)),
            object : MessageCallback {
                override fun onMessage(message: Message) {
                    synchronized(texto) {
                        if (cortado) {
                            return
                        }
                        message.contents.contents.filterIsInstance<Content.Text>().forEach { texto.append(it.text) }
                        // Cada parte es un paso de generación, o sea un token.
                        tokens++
                        val completo = ParserRespuesta.objetoCompleto(texto)
                        if (completo || tokens >= ParserRespuesta.MAX_TOKENS_RESPUESTA) {
                            if (!completo) {
                                Log.w(ETIQUETA, "Respuesta cortada a los $tokens tokens sin cerrar el JSON")
                            }
                            cortado = true
                            conversacion.cancelProcess()
                            fin.countDown()
                        }
                    }
                }

                override fun onDone() = fin.countDown()

                override fun onError(throwable: Throwable) {
                    synchronized(texto) {
                        if (!cortado) {
                            error = throwable
                        }
                    }
                    fin.countDown()
                }
            },
        )
        val termino = try {
            fin.await(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            // El usuario canceló la lectura en P07: se corta para no dejar el motor ocupado.
            conversacion.cancelProcess()
            Thread.currentThread().interrupt()
            throw InferenciaException("Lectura cancelada", e)
        }
        if (!termino) {
            conversacion.cancelProcess()
            throw InferenciaException("Se pasó el límite de $timeoutMs ms", null, true)
        }
        ultimaMedicion = try {
            @OptIn(ExperimentalApi::class)
            conversacion.getBenchmarkInfo().toString()
        } catch (e: Exception) {
            ""
        }
        conversacion.close()
        error?.let { throw InferenciaException("Falló la generación", it) }
        return synchronized(texto) { texto.toString() }
    }

    override fun close() {
        motor?.close()
        motor = null
        procesador = ""
    }

    private companion object {
        const val ETIQUETA = "FacturasS20"
    }
}
