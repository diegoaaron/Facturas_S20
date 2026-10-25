package pe.facturass20.ui.ajustes;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputLayout;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import pe.facturass20.App;
import pe.facturass20.R;
import pe.facturass20.databinding.FragmentRegistroManualBinding;
import pe.facturass20.dominio.modelo.CampoFactura;
import pe.facturass20.dominio.modelo.Emisor;
import pe.facturass20.dominio.reglas.ValidadorFecha;
import pe.facturass20.ui.captura.CapturaViewModel;
import pe.facturass20.ui.comun.Formatos;
import pe.facturass20.ui.comun.Mensajes;
import pe.facturass20.ui.verificacion.CamposFactura;
import pe.facturass20.ui.verificacion.FlujoGuardado;

/**
 * P19 Registro manual (prototipo pantalla_19_registro_manual_sin.png): para cuando la IA no está o no pudo
 * leer la foto, o el usuario prefiere escribir. Mismas validaciones que P08; la fecha trae hoy y tiene
 * calendario, y la foto es opcional (galería, con la misma revisión de nitidez de RF-02).
 */
public class RegistroManualFragment extends Fragment {

    private static final String MOTIVO_LECTURA = "lectura";
    private static final String MOTIVO_SIN_IA = "sin_ia";

    private FragmentRegistroManualBinding binding;
    private RegistroManualViewModel modelo;
    private CapturaViewModel captura;
    private CamposFactura campos;
    /** La razón social la puso el autocompletado; si el RUC cambia, se puede reemplazar. */
    private boolean razonAutocompletada;

    private final ActivityResultLauncher<PickVisualMediaRequest> elegirFoto =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    captura.desdeGaleria(uri);
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegistroManualBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        modelo = new ViewModelProvider(this).get(RegistroManualViewModel.class);
        captura = new ViewModelProvider(this).get(CapturaViewModel.class);
        String motivo = requireArguments().getString("motivo", "");

        binding.barra.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        binding.botonCancelar.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        binding.textoAviso.setText(MOTIVO_SIN_IA.equals(motivo) ? R.string.manual_sin_ia
                : MOTIVO_LECTURA.equals(motivo) ? R.string.manual_lectura_fallida : R.string.manual_elegido);

        campos = new CamposFactura(binding.campos);
        campos.campo(CampoFactura.MONEDA).setVisibility(View.GONE);
        if (savedInstanceState == null) {
            campos.poner(CampoFactura.MONEDA, "PEN");
            campos.poner(CampoFactura.FECHA_EMISION, Formatos.fecha(modelo.hoy()));
            if (MOTIVO_LECTURA.equals(motivo)) {
                modelo.adjuntarFotoCapturada();
            } else {
                ((App) requireActivity().getApplication()).contenedor().capturaEnCurso().limpiar();
            }
        }
        for (CampoFactura campo : CampoFactura.values()) {
            campos.pintar(campo, modelo.revisar(campo, campos.texto(campo)));
        }
        campos.alCambiar((campo, texto) -> {
            campos.pintar(campo, modelo.revisar(campo, texto));
            if (campo == CampoFactura.RUC) {
                modelo.alCambiarRuc(texto);
            } else if (campo == CampoFactura.RAZON_SOCIAL) {
                razonAutocompletada = false;
            }
        });
        configurarCalendario();

        modelo.emisorConocido().observe(getViewLifecycleOwner(), this::mostrarEmisor);
        modelo.miniatura().observe(getViewLifecycleOwner(), miniatura -> {
            binding.imagenFoto.setImageBitmap(miniatura);
            binding.imagenFoto.setImageTintList(null);
            binding.textoFoto.setText(R.string.manual_foto_adjunta);
            binding.textoFotoAyuda.setText(R.string.manual_foto_cambiar);
        });
        binding.tarjetaFoto.setOnClickListener(v -> elegirFoto.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
        captura.fotoLista().observe(getViewLifecycleOwner(), evento -> {
            if (evento.tomar() != null) {
                modelo.adjuntarFotoCapturada();
            }
        });
        captura.ocupado().observe(getViewLifecycleOwner(), ocupado ->
                binding.progresoFoto.setVisibility(ocupado ? View.VISIBLE : View.GONE));
        Mensajes.observar(this, captura, binding.getRoot());

        binding.botonGuardar.setOnClickListener(v -> guardar(false));
        FlujoGuardado.conectar(this, modelo, campos, R.id.registroManual, R.id.registroManual_a_guardada,
                R.id.registroManual_a_detalleFactura, R.string.duplicado_descartar_registro, this::guardar);
        modelo.ocupado().observe(getViewLifecycleOwner(), ocupado -> {
            binding.progreso.setVisibility(ocupado ? View.VISIBLE : View.GONE);
            binding.botonGuardar.setText(ocupado ? "" : getString(R.string.verif_guardar));
            binding.botonGuardar.setEnabled(!ocupado);
        });
        Mensajes.observar(this, modelo, binding.getRoot());
    }

    /** «RUC válido · Bebidas Rímac S.A.C.» y, si la razón social está vacía, la completa. */
    private void mostrarEmisor(@Nullable Emisor emisor) {
        TextInputLayout ruc = campos.campo(CampoFactura.RUC);
        if (emisor == null) {
            if (ruc.getError() == null) {
                ruc.setHelperText(null);
            }
            return;
        }
        ruc.setHelperTextColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.color_ok)));
        ruc.setHelperText(getString(R.string.manual_ruc_conocido, emisor.razonSocial()));
        if (campos.texto(CampoFactura.RAZON_SOCIAL).trim().isEmpty() || razonAutocompletada) {
            campos.poner(CampoFactura.RAZON_SOCIAL, emisor.razonSocial());
            campos.pintar(CampoFactura.RAZON_SOCIAL, modelo.revisar(CampoFactura.RAZON_SOCIAL, emisor.razonSocial()));
            razonAutocompletada = true;
        }
    }

    private void configurarCalendario() {
        TextInputLayout fecha = campos.campo(CampoFactura.FECHA_EMISION);
        fecha.setStartIconDrawable(R.drawable.ic_calendario);
        fecha.setStartIconContentDescription(R.string.manual_elegir_fecha);
        fecha.setStartIconOnClickListener(v -> {
            LocalDate actual = ValidadorFecha.interpretar(campos.texto(CampoFactura.FECHA_EMISION)).orElse(modelo.hoy());
            MaterialDatePicker<Long> selector = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.campo_fecha)
                    .setSelection(actual.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                    .build();
            selector.addOnPositiveButtonClickListener(milisegundos -> {
                LocalDate elegida = Instant.ofEpochMilli(milisegundos).atZone(ZoneOffset.UTC).toLocalDate();
                String texto = Formatos.fecha(elegida);
                campos.poner(CampoFactura.FECHA_EMISION, texto);
                campos.pintar(CampoFactura.FECHA_EMISION, modelo.revisar(CampoFactura.FECHA_EMISION, texto));
            });
            selector.show(getChildFragmentManager(), "fecha");
        });
    }

    private void guardar(boolean periodoConfirmado) {
        campos.tocarTodos();
        modelo.guardar(campos.textos(), periodoConfirmado);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
