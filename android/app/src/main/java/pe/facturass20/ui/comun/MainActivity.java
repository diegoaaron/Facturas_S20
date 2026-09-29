package pe.facturass20.ui.comun;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import java.util.Set;

import pe.facturass20.App;
import pe.facturass20.R;
import pe.facturass20.databinding.ActivityMainBinding;
import pe.facturass20.ui.acceso.Sesion;

/**
 * Única actividad de la app (salvo la cámara): aloja el NavHost y la barra inferior
 * Inicio · Facturas · [Escanear] · Mes · Ajustes (documentación técnica §8.1).
 *
 * <p>Elige la primera pantalla según la {@link Sesion}: P01 si falta configurar, P02 si hay que ingresar
 * el PIN y P04 si ya se ingresó. Cada vez que la sesión cambia, pone el grafo de nuevo con esa raíz, así
 * que Atrás nunca vuelve a P01 o P02 ni las salta.</p>
 */
public class MainActivity extends AppCompatActivity {

    /** Destinos que muestran la barra inferior (P04, P11, P14, P17 y P18). */
    private static final Set<Integer> DESTINOS_CON_BARRA =
            Set.of(R.id.inicio, R.id.facturas, R.id.categoria, R.id.historial, R.id.ajustes);

    private ActivityMainBinding binding;
    private NavController navegacion;
    private Sesion sesion;
    private Sesion.Estado estadoMostrado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen arranque = SplashScreen.installSplashScreen(this);
        sesion = ((App) getApplication()).contenedor().sesion();
        // Si el sistema cerró el proceso, la sesión volvió a bloquearse: no se restauran las pantallas
        // anteriores para que nadie entre sin el PIN.
        super.onCreate(sesion.desbloqueada() ? savedInstanceState : null);
        arranque.setKeepOnScreenCondition(() -> sesion.estado().getValue() == null);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // La barra inferior ya se ajusta sola a la barra de navegación del sistema; abajo solo cuenta el teclado.
        ViewCompat.setOnApplyWindowInsetsListener(binding.raiz, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets teclado = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(barras.left, barras.top, barras.right, teclado.bottom);
            return insets;
        });

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.contenedor_nav);
        navegacion = host.getNavController();
        configurarBarraInferior();

        sesion.estado().observe(this, this::mostrarSegunSesion);
        sesion.comprobar();
    }

    @Override
    protected void onStart() {
        super.onStart();
        sesion.alVolver();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations()) {
            sesion.alSalir();
        }
    }

    /**
     * Pone el grafo con la raíz que corresponde al estado. Al girar la pantalla el estado no cambia y
     * {@code setGraph} restaura las pantallas abiertas.
     */
    private void mostrarSegunSesion(Sesion.Estado estado) {
        if (estado == null || estado == estadoMostrado) {
            return;
        }
        estadoMostrado = estado;
        NavGraph grafo = navegacion.getNavInflater().inflate(R.navigation.nav_graph);
        grafo.setStartDestination(destinoInicial(estado));
        navegacion.setGraph(grafo, null);
    }

    private static int destinoInicial(Sesion.Estado estado) {
        switch (estado) {
            case SIN_CONFIGURAR:
                return R.id.configuracion;
            case BLOQUEADA:
                return R.id.acceso;
            default:
                return R.id.inicio;
        }
    }

    private void configurarBarraInferior() {
        NavigationUI.setupWithNavController(binding.barraInferior, navegacion);
        // El ítem central solo reserva el espacio del botón: abre la captura y nunca queda seleccionado.
        binding.barraInferior.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.escanear) {
                escanear();
                return false;
            }
            return NavigationUI.onNavDestinationSelected(item, navegacion);
        });
        binding.botonEscanear.setOnClickListener(v -> escanear());
        navegacion.addOnDestinationChangedListener((controlador, destino, argumentos) -> {
            int visibilidad = DESTINOS_CON_BARRA.contains(destino.getId()) ? View.VISIBLE : View.GONE;
            binding.barraInferior.setVisibility(visibilidad);
            binding.botonEscanear.setVisibility(visibilidad);
        });
    }

    private void escanear() {
        navegacion.navigate(R.id.accion_escanear);
    }
}
