package pe.facturass20.ui.captura;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.util.Size;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pe.facturass20.App;
import pe.facturass20.R;
import pe.facturass20.databinding.ActivityCapturaBinding;
import pe.facturass20.ui.acceso.Sesion;

/**
 * P06 Cámara (RF-01 a RF-03): vista previa con el marco de encuadre, nitidez y luz medidas en vivo dentro
 * del marco, linterna (se enciende sola con poca luz) y galería. El disparador solo se activa cuando nitidez y luz son «buenas»; la foto se
 * vuelve a medir ya preparada y, si pasa, queda en {@link CapturaEnCurso} y la actividad termina con
 * {@code RESULT_OK} (quien la abrió sigue a P07).
 *
 * <p>Es una actividad aparte (y no un destino del grafo) para tener su propio tema oscuro a pantalla
 * completa y devolver el resultado a P05 con un {@code ActivityResultLauncher}.</p>
 */
public class CapturaActivity extends AppCompatActivity {

    private static final String ETIQUETA = "FacturasS20";
    /** Cada cuánto se mide un fotograma; más seguido no aporta y gasta batería. */
    private static final long INTERVALO_ANALISIS_MS = 200;
    /**
     * Fotogramas legibles (y con comprobante) seguidos (~0,6 s) para habilitar el disparador: moviendo el teléfono, la nitidez
     * salta de un fotograma a otro (22 → 445 → 117 en el Moto G34) y uno solo nítido no basta.
     */
    private static final int LEGIBLES_SEGUIDOS = 3;
    /**
     * Con poca luz durante este tiempo se enciende la linterna (luz continua, no flash: el destello deja
     * una mancha blanca sobre el papel que puede tapar los dígitos o el QR).
     */
    private static final long POCA_LUZ_PARA_LINTERNA_MS = 1_000;
    /** Lado mayor ~900 px dentro del marco, como la foto final (§6.4), para que la nitidez sea comparable. */
    private static final Size RESOLUCION_ANALISIS = new Size(1280, 960);
    /**
     * La más alta 4:3: la foto se reduce a 896 px, pero antes se busca el QR en el marco a resolución
     * completa, y a 1920 px se leía en muchas menos fotos (§6.4). El Moto G34 entrega 3264 × 2448 (8 MP):
     * disparo de ~500 ms, foto lista con el QR en ~200–400 ms, y lee el QR (prueba del 2026-10-27).
     */
    private static final Size RESOLUCION_FOTO = new Size(4080, 3072);

    private ActivityCapturaBinding binding;
    private CapturaViewModel modelo;
    private Sesion sesion;
    private ExecutorService hiloAnalisis;
    private ImageCapture captura;
    private Camera camara;
    private boolean camaraIniciada;
    private boolean linternaEncendida;
    /** El usuario la apagó: ya no se enciende sola mientras siga en esta pantalla. */
    private boolean linternaApagadaAMano;
    /** Desde cuándo hay poca luz sin linterna; 0 si hay luz suficiente. */
    private long inicioPocaLuz;
    private boolean disparando;
    private EvaluadorNitidez.Evaluacion evaluacion;
    /** Cuántas evaluaciones legibles van seguidas; se pone en 0 con la primera que no lo es. */
    private int legiblesSeguidas;
    /** Lo escribe el hilo de UI al medir la vista y lo lee el hilo de análisis. */
    private volatile MarcoEncuadre marco = MarcoEncuadre.completo();
    private long ultimoAnalisis;
    /** Para calibrar los umbrales con el teléfono: la vista previa se registra una vez por segundo. */
    private long ultimoRegistro;
    private byte[] luma = new byte[0];
    /** El marco del plano Y sin relleno entre filas, para buscar el comprobante. */
    private byte[] region = new byte[0];
    /** Si la última evaluación encontró un comprobante en el marco. */
    private boolean comprobanteEnMarco;

    private final ActivityResultLauncher<String> pedirPermiso =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> revisarPermiso());
    private final ActivityResultLauncher<PickVisualMediaRequest> elegirDeGaleria =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), this::alElegirDeGaleria);

    public static Intent intent(Context contexto) {
        return new Intent(contexto, CapturaActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sesion = ((App) getApplication()).contenedor().sesion();
        // Tras cerrarse el proceso la sesión se bloquea: sin PIN no se abre la cámara.
        if (!sesion.desbloqueada()) {
            finish();
            return;
        }
        EdgeToEdge.enable(this);
        binding = ActivityCapturaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.raiz, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });
        hiloAnalisis = Executors.newSingleThreadExecutor();
        modelo = new ViewModelProvider(this).get(CapturaViewModel.class);

        binding.botonCerrar.setOnClickListener(v -> finish());
        binding.botonLinterna.setOnClickListener(v -> {
            if (linternaEncendida) {
                linternaApagadaAMano = true;
            }
            Log.i(ETIQUETA, "Linterna " + (linternaEncendida ? "apagada" : "encendida") + " a mano");
            ponerLinterna(!linternaEncendida);
        });
        binding.botonDisparar.setOnClickListener(v -> disparar());
        binding.botonGaleria.setOnClickListener(v -> elegirDeGaleria.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
        binding.botonPermiso.setOnClickListener(v -> solicitarPermiso());
        binding.marco.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> marco = binding.marco.marco());

        modelo.fotoLista().observe(this, evento -> {
            if (evento.tomar() != null) {
                // Ya no hace falta: que no siga encendida mientras se cierra la cámara.
                apagarLinterna();
                setResult(RESULT_OK);
                finish();
            }
        });
        modelo.ocupado().observe(this, ocupado -> actualizarDisparador());
        modelo.mensaje().observe(this, evento -> {
            String texto = evento.tomar();
            if (texto != null) {
                mostrarMensaje(texto);
            }
        });

        mostrarEvaluacion(null, false);
        if (savedInstanceState == null && !tienePermiso()) {
            pedirPermiso.launch(Manifest.permission.CAMERA);
        } else {
            revisarPermiso();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        sesion.alVolver();
        if (!sesion.desbloqueada()) {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Al volver de los ajustes del sistema, el permiso pudo cambiar.
        if (binding != null) {
            revisarPermiso();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        apagarLinterna();
        sesion.alSalir();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hiloAnalisis != null) {
            hiloAnalisis.shutdown();
        }
    }

    // --- Permiso ---

    private boolean tienePermiso() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void revisarPermiso() {
        boolean concedido = tienePermiso();
        binding.panelPermiso.setVisibility(concedido ? View.GONE : View.VISIBLE);
        binding.marco.setVisibility(concedido ? View.VISIBLE : View.INVISIBLE);
        binding.etiquetaEstado.setVisibility(concedido ? View.VISIBLE : View.INVISIBLE);
        if (!concedido) {
            // Si el usuario marcó «No volver a preguntar», solo queda ir a los ajustes del sistema.
            boolean puedePreguntar = shouldShowRequestPermissionRationale(Manifest.permission.CAMERA);
            binding.botonPermiso.setText(puedePreguntar ? R.string.captura_permiso_boton : R.string.captura_permiso_ajustes);
            binding.botonPermiso.setTag(puedePreguntar);
        } else if (!camaraIniciada) {
            camaraIniciada = true;
            iniciarCamara();
        }
        actualizarDisparador();
    }

    private void solicitarPermiso() {
        if (Boolean.TRUE.equals(binding.botonPermiso.getTag())) {
            pedirPermiso.launch(Manifest.permission.CAMERA);
        } else {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", getPackageName(), null)));
        }
    }

    // --- Cámara ---

    private void iniciarCamara() {
        ListenableFuture<ProcessCameraProvider> futuro = ProcessCameraProvider.getInstance(this);
        futuro.addListener(() -> {
            try {
                ProcessCameraProvider proveedor = futuro.get();
                binding.vistaPrevia.post(() -> vincular(proveedor));
            } catch (Exception e) {
                errorCamara(e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /** Une vista previa, foto y análisis con el mismo {@link ViewPort}: los tres ven lo mismo que el usuario. */
    private void vincular(ProcessCameraProvider proveedor) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        ViewPort visible = binding.vistaPrevia.getViewPort();
        if (visible == null) {
            // La vista previa todavía no tiene tamaño.
            binding.vistaPrevia.post(() -> vincular(proveedor));
            return;
        }
        Preview previa = new Preview.Builder().setResolutionSelector(selector(null)).build();
        previa.setSurfaceProvider(binding.vistaPrevia.getSurfaceProvider());
        captura = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setResolutionSelector(selector(RESOLUCION_FOTO))
                .build();
        ImageAnalysis analisis = new ImageAnalysis.Builder()
                .setResolutionSelector(selector(RESOLUCION_ANALISIS))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        analisis.setAnalyzer(hiloAnalisis, this::analizar);

        UseCaseGroup grupo = new UseCaseGroup.Builder()
                .setViewPort(visible)
                .addUseCase(previa)
                .addUseCase(captura)
                .addUseCase(analisis)
                .build();
        try {
            proveedor.unbindAll();
            camara = proveedor.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, grupo);
        } catch (RuntimeException e) {
            errorCamara(e);
            return;
        }
        binding.botonLinterna.setVisibility(camara.getCameraInfo().hasFlashUnit() ? View.VISIBLE : View.INVISIBLE);
        actualizarDisparador();
    }

    private static ResolutionSelector selector(Size preferida) {
        ResolutionSelector.Builder constructor = new ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY);
        if (preferida != null) {
            constructor.setResolutionStrategy(new ResolutionStrategy(preferida,
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER));
        }
        return constructor.build();
    }

    private void errorCamara(Exception e) {
        Log.e(ETIQUETA, "No se pudo abrir la cámara: " + e.getClass().getName());
        captura = null;
        mostrarMensaje(getString(R.string.captura_error_camara));
        actualizarDisparador();
    }

    /** En el hilo de análisis: mide nitidez y luz del plano Y dentro del marco. */
    private void analizar(@NonNull ImageProxy imagen) {
        try {
            long ahora = SystemClock.elapsedRealtime();
            if (ahora - ultimoAnalisis < INTERVALO_ANALISIS_MS) {
                return;
            }
            ultimoAnalisis = ahora;
            ImageProxy.PlaneProxy planoY = imagen.getPlanes()[0];
            ByteBuffer datos = planoY.getBuffer();
            datos.rewind();
            if (luma.length != datos.remaining()) {
                luma = new byte[datos.remaining()];
            }
            datos.get(luma);
            Rect visible = imagen.getCropRect();
            int[] r = marco.enSensor(imagen.getImageInfo().getRotationDegrees())
                    .enPixeles(visible.left, visible.top, visible.width(), visible.height());
            int paso = planoY.getRowStride();
            int filas = Math.min(imagen.getHeight(), (luma.length + paso - imagen.getWidth()) / paso);
            int izquierda = Math.max(0, r[0]);
            int arriba = Math.max(0, r[1]);
            int derecha = Math.min(imagen.getWidth(), r[2]);
            int abajo = Math.min(filas, r[3]);
            EvaluadorNitidez.Evaluacion resultado = EvaluadorNitidez.evaluar(luma, paso, izquierda, arriba, derecha, abajo);
            // Solo si lo demás ya está bien: es lo más caro del análisis.
            long inicioBusqueda = SystemClock.elapsedRealtime();
            boolean comprobante = resultado.legible() && hayComprobante(paso, izquierda, arriba, derecha, abajo);
            long busquedaMs = SystemClock.elapsedRealtime() - inicioBusqueda;
            if (ahora - ultimoRegistro >= 1000) {
                ultimoRegistro = ahora;
                // Para calibrar los umbrales: adb logcat -s FacturasS20
                Log.d(ETIQUETA, String.format(java.util.Locale.ROOT,
                        "Vista previa: nitidez %.1f, luz %.1f, comprobante %s en %d ms (%d x %d)",
                        resultado.nitidez(), resultado.luz(), comprobante, busquedaMs, derecha - izquierda, abajo - arriba));
            }
            runOnUiThread(() -> mostrarEvaluacion(resultado, comprobante));
        } catch (RuntimeException e) {
            Log.w(ETIQUETA, "Fotograma no evaluado: " + e.getClass().getName());
        } finally {
            imagen.close();
        }
    }

    /**
     * Si hay un comprobante en el marco: la misma búsqueda de tinta del recorte automático
     * ({@link RecortadorComprobante}). La nitidez sola no basta, porque una mesa con textura da valores de
     * factura enfocada (§6.4). El marco de la vista previa (~885 × 630) ya está cerca del tamaño con el que
     * se calibró la búsqueda.
     */
    private boolean hayComprobante(int paso, int izquierda, int arriba, int derecha, int abajo) {
        int ancho = derecha - izquierda;
        int alto = abajo - arriba;
        if (region.length != ancho * alto) {
            region = new byte[ancho * alto];
        }
        for (int y = 0; y < alto; y++) {
            System.arraycopy(luma, (arriba + y) * paso + izquierda, region, y * ancho, ancho);
        }
        return RecortadorComprobante.esquinas(region, ancho, alto) != null;
    }

    private void disparar() {
        if (captura == null || disparando) {
            return;
        }
        disparando = true;
        actualizarDisparador();
        MarcoEncuadre marcoAlDisparar = binding.marco.marco();
        long inicio = SystemClock.elapsedRealtime();
        captura.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy imagen) {
                try {
                    ByteBuffer datos = imagen.getPlanes()[0].getBuffer();
                    datos.rewind();
                    byte[] jpeg = new byte[datos.remaining()];
                    datos.get(jpeg);
                    Log.i(ETIQUETA, "Disparo en " + (SystemClock.elapsedRealtime() - inicio) + " ms: "
                            + imagen.getWidth() + " x " + imagen.getHeight() + ", " + jpeg.length / 1024 + " KB");
                    modelo.desdeCamara(jpeg, imagen.getWidth(), imagen.getHeight(), new Rect(imagen.getCropRect()),
                            imagen.getImageInfo().getRotationDegrees(), marcoAlDisparar);
                } finally {
                    imagen.close();
                    disparando = false;
                    actualizarDisparador();
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException e) {
                Log.e(ETIQUETA, "No se pudo tomar la foto: " + e.getImageCaptureError());
                disparando = false;
                actualizarDisparador();
                mostrarMensaje(getString(R.string.captura_error_disparo));
            }
        });
    }

    private void ponerLinterna(boolean encendida) {
        if (camara == null) {
            return;
        }
        linternaEncendida = encendida;
        camara.getCameraControl().enableTorch(linternaEncendida);
        binding.iconoLinterna.setImageResource(linternaEncendida ? R.drawable.ic_linterna : R.drawable.ic_linterna_apagada);
        binding.iconoLinterna.setBackgroundResource(linternaEncendida
                ? R.drawable.fondo_boton_linterna_encendida : R.drawable.fondo_boton_galeria);
        binding.botonLinterna.setContentDescription(getString(linternaEncendida
                ? R.string.captura_linterna_apagar : R.string.captura_linterna_encender));
    }

    /** Al salir de la pantalla; si el usuario vuelve, la linterna puede encenderse sola otra vez. */
    private void apagarLinterna() {
        if (linternaEncendida) {
            ponerLinterna(false);
        }
    }

    private void alElegirDeGaleria(Uri uri) {
        if (uri != null) {
            modelo.desdeGaleria(uri);
        }
    }

    // --- Estado en pantalla ---

    /**
     * @param nueva       {@code null} mientras no llega el primer fotograma
     * @param comprobante si hay un comprobante en el marco (solo se busca si la nitidez y la luz son buenas)
     */
    private void mostrarEvaluacion(EvaluadorNitidez.Evaluacion nueva, boolean comprobante) {
        evaluacion = nueva;
        comprobanteEnMarco = comprobante;
        legiblesSeguidas = nueva != null && nueva.legible() && comprobante ? legiblesSeguidas + 1 : 0;
        @ColorRes int fondo;
        @ColorRes int colorTexto;
        String estado;
        @StringRes int ayuda;
        if (nueva == null) {
            fondo = R.color.color_camara_boton;
            colorTexto = R.color.color_superficie;
            estado = getString(R.string.captura_enfocando);
            ayuda = R.string.camara_acercar;
        } else {
            boolean legible = estable();
            fondo = legible ? R.color.color_ok : R.color.color_alerta;
            colorTexto = legible ? R.color.color_superficie : R.color.color_texto;
            if (nueva.legible() && !comprobante) {
                estado = getString(R.string.captura_sin_comprobante);
                ayuda = R.string.captura_ayuda_encuadrar;
            } else {
                estado = getString(R.string.captura_estado,
                        getString(nueva.nitidezBuena() ? R.string.captura_nitidez_buena : R.string.captura_nitidez_baja),
                        getString(textoLuz(nueva.nivelLuz())));
                ayuda = ayuda(nueva);
            }
        }
        binding.marco.setBueno(estable());
        if (!estado.contentEquals(binding.etiquetaEstado.getText())) {
            binding.etiquetaEstado.setText(estado);
        }
        binding.etiquetaEstado.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, fondo)));
        binding.etiquetaEstado.setTextColor(ContextCompat.getColor(this, colorTexto));
        binding.textoAyuda.setText(ayuda);
        encenderLinternaSiFalta(nueva);
        actualizarDisparador();
    }

    /**
     * Enciende la linterna si hay poca luz desde hace {@link #POCA_LUZ_PARA_LINTERNA_MS}. No la apaga sola
     * (con la linterna la luz ya se mide buena); si el usuario la apaga, no se vuelve a encender.
     */
    private void encenderLinternaSiFalta(EvaluadorNitidez.Evaluacion nueva) {
        boolean pocaLuz = nueva != null && nueva.nivelLuz() == EvaluadorNitidez.Luz.BAJA;
        if (!pocaLuz || linternaEncendida || linternaApagadaAMano || camara == null
                || !camara.getCameraInfo().hasFlashUnit()) {
            inicioPocaLuz = 0;
            return;
        }
        long ahora = SystemClock.elapsedRealtime();
        if (inicioPocaLuz == 0) {
            inicioPocaLuz = ahora;
        } else if (ahora - inicioPocaLuz >= POCA_LUZ_PARA_LINTERNA_MS) {
            Log.i(ETIQUETA, "Linterna encendida sola: poca luz");
            ponerLinterna(true);
            mostrarMensaje(getString(R.string.captura_linterna_automatica));
        }
    }

    private static int textoLuz(EvaluadorNitidez.Luz luz) {
        switch (luz) {
            case BAJA:
                return R.string.captura_luz_baja;
            case EXCESIVA:
                return R.string.captura_luz_excesiva;
            default:
                return R.string.captura_luz_buena;
        }
    }

    private static int ayuda(EvaluadorNitidez.Evaluacion evaluacion) {
        switch (evaluacion.nivelLuz()) {
            case BAJA:
                return R.string.captura_ayuda_poca_luz;
            case EXCESIVA:
                return R.string.captura_ayuda_brillo;
            default:
                return evaluacion.nitidezBuena() ? R.string.captura_ayuda_lista : R.string.camara_acercar;
        }
    }

    /**
     * Nitidez y luz buenas y un comprobante en el marco en los últimos {@link #LEGIBLES_SEGUIDOS} fotogramas:
     * se puede disparar.
     */
    private boolean estable() {
        return evaluacion != null && comprobanteEnMarco && legiblesSeguidas >= LEGIBLES_SEGUIDOS;
    }

    private void actualizarDisparador() {
        boolean ocupado = Boolean.TRUE.equals(modelo.ocupado().getValue()) || disparando;
        boolean activo = captura != null && tienePermiso() && estable() && !ocupado;
        binding.botonDisparar.setEnabled(activo);
        binding.progreso.setVisibility(ocupado ? View.VISIBLE : View.GONE);
        binding.botonGaleria.setEnabled(!ocupado);
    }

    private void mostrarMensaje(String texto) {
        Snackbar.make(binding.getRoot(), texto, Snackbar.LENGTH_LONG).setAnchorView(binding.panelInferior).show();
    }
}
