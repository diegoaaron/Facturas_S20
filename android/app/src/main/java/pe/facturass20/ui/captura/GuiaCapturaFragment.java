package pe.facturass20.ui.captura;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;

import pe.facturass20.App;
import pe.facturass20.R;
import pe.facturass20.databinding.FragmentGuiaCapturaBinding;
import pe.facturass20.databinding.ItemConsejoCapturaBinding;

/**
 * P05 Guía de captura (prototipo pantalla_05_guia_de_captura.png): consejos para la foto, «Abrir cámara»
 * (P06), «Elegir de la galería» (RF-03) y «No volver a mostrar esta guía».
 *
 * <p>Si el usuario marcó esa casilla, Escanear sigue pasando por aquí pero la cámara se abre sola y, al
 * cerrarla sin foto, se vuelve a la pantalla anterior; así el flujo principal queda en 3 toques (RNF-09).</p>
 */
public class GuiaCapturaFragment extends Fragment {

    private static final String ESTADO_CAMARA_ABIERTA = "camara_abierta";

    private FragmentGuiaCapturaBinding binding;
    private CapturaViewModel modelo;
    private PreferenciasCaptura preferencias;
    /** La cámara ya se abrió sola una vez; al volver de P07 no se abre de nuevo. */
    private boolean camaraAbiertaSola;

    private final ActivityResultLauncher<Intent> abrirCamara =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() == Activity.RESULT_OK) {
                    irALectura();
                } else if (preferencias.omitirGuia()) {
                    NavHostFragment.findNavController(this).popBackStack();
                }
            });
    private final ActivityResultLauncher<PickVisualMediaRequest> elegirDeGaleria =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), this::alElegirDeGaleria);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferencias = ((App) requireActivity().getApplication()).contenedor().preferenciasCaptura();
        camaraAbiertaSola = savedInstanceState != null && savedInstanceState.getBoolean(ESTADO_CAMARA_ABIERTA);
        if (!camaraAbiertaSola && preferencias.omitirGuia()) {
            camaraAbiertaSola = true;
            abrirCamara();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentGuiaCapturaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        modelo = new ViewModelProvider(this).get(CapturaViewModel.class);

        binding.barra.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        consejo(binding.consejoLuz, R.drawable.ic_linterna, R.string.guia_luz);
        consejo(binding.consejoPlana, R.drawable.ic_documento, R.string.guia_plana);
        consejo(binding.consejoAcercar, R.drawable.ic_acercar, R.string.guia_acercar);
        consejo(binding.consejoSinInternet, R.drawable.ic_sin_red, R.string.guia_sin_internet);
        binding.casillaNoMostrar.setChecked(preferencias.omitirGuia());
        binding.casillaNoMostrar.setOnCheckedChangeListener((casilla, marcada) -> preferencias.omitirGuia(marcada));
        binding.botonAbrirCamara.setOnClickListener(v -> abrirCamara());
        binding.botonGaleria.setOnClickListener(v -> elegirDeGaleria.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));

        modelo.fotoLista().observe(getViewLifecycleOwner(), evento -> {
            if (evento.tomar() != null) {
                irALectura();
            }
        });
        modelo.ocupado().observe(getViewLifecycleOwner(), ocupado -> {
            binding.progreso.setVisibility(ocupado ? View.VISIBLE : View.GONE);
            binding.botonAbrirCamara.setEnabled(!ocupado);
            binding.botonGaleria.setEnabled(!ocupado);
        });
        modelo.mensaje().observe(getViewLifecycleOwner(), evento -> {
            String texto = evento.tomar();
            if (texto != null) {
                Snackbar.make(binding.getRoot(), texto, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ESTADO_CAMARA_ABIERTA, camaraAbiertaSola);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static void consejo(ItemConsejoCapturaBinding consejo, @DrawableRes int icono, @StringRes int texto) {
        consejo.icono.setImageResource(icono);
        consejo.texto.setText(texto);
    }

    private void abrirCamara() {
        abrirCamara.launch(CapturaActivity.intent(requireContext()));
    }

    private void alElegirDeGaleria(Uri uri) {
        if (uri != null) {
            modelo.desdeGaleria(uri);
        }
    }

    /** Sin la guía, Atrás desde P07 vuelve a donde se tocó Escanear y no a esta pantalla. */
    private void irALectura() {
        NavController navegacion = NavHostFragment.findNavController(this);
        if (navegacion.getCurrentDestination() == null || navegacion.getCurrentDestination().getId() != R.id.guiaCaptura) {
            return;
        }
        NavOptions opciones = preferencias.omitirGuia()
                ? new NavOptions.Builder().setPopUpTo(R.id.guiaCaptura, true).build()
                : null;
        navegacion.navigate(R.id.guiaCaptura_a_lectura, null, opciones);
    }
}
