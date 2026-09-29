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
 * del marco, linterna y galería. El disparador solo se activa cuando nitidez y luz son «buenas»; la foto se
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
    /** Lado mayor ~900 px dentro del marco, como la foto final (§6.4), para que la nitidez sea comparable. */
    private static final Size RESOLUCION_ANALISIS = new Size(1280, 960);
    /** Suficiente para recortar el marco y reducirlo a 896 px sin decodificar fotos enormes. */
    private static final Size RESOLUCION_FOTO = new Size(1920, 1440);

    private ActivityCapturaBinding binding;
    private CapturaViewModel modelo;
    private Sesion sesion;
    private ExecutorService hiloAnalisis;
    private ImageCapture captura;
    private Camera camara;
    private boolean camaraIniciada;
    private boolean linternaEncendida;
    private boolean disparando;
    private EvaluadorNitidez.Evaluacion evaluacion;
    /** Lo escribe el hilo de UI al medir la vista y lo lee el hilo de análisis. */
    private volatile MarcoEncuadre marco = MarcoEncuadre.completo();
    private long ultimoAnalisis;
    private byte[] luma = new byte[0];

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
        binding.botonLinterna.setOnClickListener(v -> cambiarLinterna());
        binding.botonDisparar.setOnClickListener(v -> disparar());
        binding.botonGaleria.setOnClickListener(v -> elegirDeGaleria.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
        binding.botonPermiso.setOnClickListener(v -> solicitarPermiso());
        binding.marco.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> marco = binding.marco.marco());

        modelo.fotoLista().observe(this, evento -> {
            if (evento.tomar() != null) {
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

        mostrarEvaluacion(null);
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
        if (!isChangingConfigurations()) {
            sesion.alSalir();
        }
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
            EvaluadorNitidez.Evaluacion resultado = EvaluadorNitidez.evaluar(luma, paso,
                    Math.max(0, r[0]), Math.max(0, r[1]), Math.min(imagen.getWidth(), r[2]), Math.min(filas, r[3]));
            runOnUiThread(() -> mostrarEvaluacion(resultado));
        } catch (RuntimeException e) {
            Log.w(ETIQUETA, "Fotograma no evaluado: " + e.getClass().getName());
        } finally {
            imagen.close();
        }
    }

    private void disparar() {
        if (captura == null || disparando) {
            return;
        }
        disparando = true;
        actualizarDisparador();
        MarcoEncuadre marcoAlDisparar = binding.marco.marco();
        captura.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy imagen) {
                try {
                    ByteBuffer datos = imagen.getPlanes()[0].getBuffer();
                    datos.rewind();
                    byte[] jpeg = new byte[datos.remaining()];
                    datos.get(jpeg);
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

    private void cambiarLinterna() {
        if (camara == null) {
            return;
        }
        linternaEncendida = !linternaEncendida;
        camara.getCameraControl().enableTorch(linternaEncendida);
        binding.botonLinterna.setImageResource(linternaEncendida ? R.drawable.ic_linterna : R.drawable.ic_linterna_apagada);
        binding.botonLinterna.setContentDescription(getString(linternaEncendida
                ? R.string.captura_linterna_apagar : R.string.captura_linterna_encender));
    }

    private void alElegirDeGaleria(Uri uri) {
        if (uri != null) {
            modelo.desdeGaleria(uri);
        }
    }

    // --- Estado en pantalla ---

    /** {@code null} mientras no llega el primer fotograma. */
    private void mostrarEvaluacion(EvaluadorNitidez.Evaluacion nueva) {
        evaluacion = nueva;
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
            boolean legible = nueva.legible();
            fondo = legible ? R.color.color_ok : R.color.color_alerta;
            colorTexto = legible ? R.color.color_superficie : R.color.color_texto;
            estado = getString(R.string.captura_estado,
                    getString(nueva.nitidezBuena() ? R.string.captura_nitidez_buena : R.string.captura_nitidez_baja),
                    getString(textoLuz(nueva.nivelLuz())));
            ayuda = ayuda(nueva);
        }
        binding.marco.setBueno(nueva != null && nueva.legible());
        if (!estado.contentEquals(binding.etiquetaEstado.getText())) {
            binding.etiquetaEstado.setText(estado);
        }
        binding.etiquetaEstado.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, fondo)));
        binding.etiquetaEstado.setTextColor(ContextCompat.getColor(this, colorTexto));
        binding.textoAyuda.setText(ayuda);
        actualizarDisparador();
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

    private void actualizarDisparador() {
        boolean ocupado = Boolean.TRUE.equals(modelo.ocupado().getValue()) || disparando;
        boolean activo = captura != null && tienePermiso() && evaluacion != null && evaluacion.legible() && !ocupado;
        binding.botonDisparar.setEnabled(activo);
        binding.progreso.setVisibility(ocupado ? View.VISIBLE : View.GONE);
        binding.botonGaleria.setEnabled(!ocupado);
    }

    private void mostrarMensaje(String texto) {
        Snackbar.make(binding.getRoot(), texto, Snackbar.LENGTH_LONG).setAnchorView(binding.panelInferior).show();
    }
}
