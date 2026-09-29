package pe.facturass20.ui.comun;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import java.util.Set;

import pe.facturass20.R;
import pe.facturass20.databinding.ActivityMainBinding;

/**
 * Única actividad de la app (salvo la cámara): aloja el NavHost y la barra inferior
 * Inicio · Facturas · [Escanear] · Mes · Ajustes (documentación técnica §8.1).
 */
public class MainActivity extends AppCompatActivity {

    /** Destinos que muestran la barra inferior (P04, P11, P14, P17 y P18). */
    private static final Set<Integer> DESTINOS_CON_BARRA =
            Set.of(R.id.inicio, R.id.facturas, R.id.categoria, R.id.historial, R.id.ajustes);

    private ActivityMainBinding binding;
    private NavController navegacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // La barra inferior ya se ajusta sola a la barra de navegación del sistema.
        ViewCompat.setOnApplyWindowInsetsListener(binding.raiz, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, 0);
            return insets;
        });

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.contenedor_nav);
        navegacion = host.getNavController();
        configurarBarraInferior();
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
