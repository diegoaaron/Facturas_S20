package pe.facturass20.ui.ajustes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.work.Data;
import androidx.work.WorkInfo;

import java.util.List;
import java.util.Locale;

import pe.facturass20.R;
import pe.facturass20.databinding.FragmentPreparacionModeloBinding;
import pe.facturass20.databinding.ItemComprobacionBinding;
import pe.facturass20.inferencia.GestorModeloLocal;
import pe.facturass20.trabajos.DescargaModeloWorker;
import pe.facturass20.ui.comun.Mensajes;

/**
 * P03 Preparar lectura con IA (prototipo pantalla_03_preparar_lectura_con.png): memoria, espacio,
 * conexión y versión de Android del teléfono, y el estado del modelo con el avance de
 * {@link DescargaModeloWorker}; «Registrar a mano» abre P19.
 */
public class PreparacionModeloFragment extends Fragment {

    private FragmentPreparacionModeloBinding binding;
    private PreparacionModeloViewModel modelo;
    /** La última descarga, para pintar el estado del modelo y el botón juntos. */
    private WorkInfo descarga;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPreparacionModeloBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        modelo = new ViewModelProvider(this).get(PreparacionModeloViewModel.class);
        binding.barra.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        binding.botonManual.setOnClickListener(v -> {
            PreparacionModeloViewModel.Equipo equipo = modelo.equipo().getValue();
            Bundle argumentos = new Bundle();
            argumentos.putString("motivo", equipo != null && !equipo.apto() ? "sin_ia" : "guia");
            NavHostFragment.findNavController(this).navigate(R.id.preparacionModelo_a_registroManual, argumentos);
        });
        modelo.equipo().observe(getViewLifecycleOwner(), this::mostrar);
        modelo.modelo().observe(getViewLifecycleOwner(), m -> mostrarModelo());
        modelo.descarga().observe(getViewLifecycleOwner(), this::alCambiarDescarga);
        Mensajes.observar(this, modelo, binding.getRoot());
        modelo.comprobar();
    }

    private void mostrar(PreparacionModeloViewModel.Equipo equipo) {
        binding.comprobaciones.removeAllViews();
        comprobacion(R.string.modelo_memoria, gigas(equipo.memoria()), equipo.memoriaSuficiente());
        comprobacion(R.string.modelo_espacio, getString(R.string.modelo_espacio_valor, gigas(equipo.espacioLibre()),
                gigas(PreparacionModeloViewModel.ESPACIO_MINIMO)), equipo.espacioSuficiente());
        comprobacion(R.string.modelo_conexion, getString(equipo.wifi() ? R.string.modelo_wifi : R.string.modelo_sin_wifi),
                equipo.wifi());
        comprobacion(R.string.modelo_android, equipo.android(), true);
        mostrarModelo();
    }

    private void alCambiarDescarga(List<WorkInfo> trabajos) {
        WorkInfo anterior = descarga;
        descarga = trabajos == null || trabajos.isEmpty() ? null : trabajos.get(0);
        boolean termino = anterior != null && !anterior.getState().isFinished()
                && descarga != null && descarga.getState().isFinished();
        if (termino) {
            modelo.comprobarModelo();
        }
        mostrarModelo();
    }

    /** Estado del modelo, avance de la descarga y botón, según lo que se sabe hasta el momento. */
    private void mostrarModelo() {
        PreparacionModeloViewModel.Modelo m = modelo.modelo().getValue();
        PreparacionModeloViewModel.Equipo equipo = modelo.equipo().getValue();
        if (m == null || equipo == null) {
            return;
        }
        binding.textoDescarga.setText(getString(R.string.modelo_descarga, gigas(m.tamanoBytes())));
        binding.progresoDescarga.setVisibility(View.GONE);
        boolean enCurso = descarga != null && !descarga.getState().isFinished();
        if (m.estado() == GestorModeloLocal.Estado.INCOMPATIBLE) {
            estado(getString(R.string.modelo_incompatible), R.string.modelo_descargar, false, null);
        } else if (m.estado() == GestorModeloLocal.Estado.LISTO) {
            estado(getString(R.string.modelo_instalado, m.versionActiva()), R.string.modelo_boton_instalado, false, null);
        } else if (enCurso) {
            mostrarAvance(descarga, equipo);
        } else if (!equipo.apto()) {
            estado(getString(R.string.modelo_no_apto), R.string.modelo_descargar, false, null);
        } else {
            String texto = m.estado() == GestorModeloLocal.Estado.ACTUALIZACION
                    ? getString(R.string.modelo_actualizacion, m.versionManifiesto(), m.versionActiva())
                    : getString(R.string.modelo_para_descargar, m.versionManifiesto());
            boolean fallo = descarga != null && descarga.getState() == WorkInfo.State.FAILED;
            String error = fallo ? descarga.getOutputData().getString(DescargaModeloWorker.ERROR) : null;
            estado(error != null ? error : texto, fallo ? R.string.modelo_reintentar : R.string.modelo_descargar,
                    true, modelo::descargar);
        }
    }

    private void mostrarAvance(WorkInfo trabajo, PreparacionModeloViewModel.Equipo equipo) {
        Data avance = trabajo.getProgress();
        long total = avance.getLong(DescargaModeloWorker.TOTAL, 0);
        String fase = avance.getString(DescargaModeloWorker.FASE);
        String texto;
        if (trabajo.getState() == WorkInfo.State.ENQUEUED && !equipo.wifi()) {
            texto = getString(R.string.modelo_esperando_wifi);
        } else if (fase == null || total == 0) {
            texto = getString(R.string.modelo_descargando);
        } else if (DescargaModeloWorker.Fase.PROBANDO.name().equals(fase)) {
            // El archivo ya está completo: el avance en bytes no dice nada aquí.
            texto = getString(R.string.modelo_probando);
            binding.progresoDescarga.setIndeterminate(true);
            binding.progresoDescarga.setVisibility(View.VISIBLE);
        } else {
            long hechos = avance.getLong(DescargaModeloWorker.HECHOS, 0);
            int porcentaje = (int) (hechos * 100 / total);
            texto = getString(R.string.modelo_avance, getString(textoFase(fase)), porcentaje, gigas(hechos), gigas(total));
            binding.progresoDescarga.setIndeterminate(false);
            binding.progresoDescarga.setProgressCompat(porcentaje, true);
            binding.progresoDescarga.setVisibility(View.VISIBLE);
        }
        estado(texto, R.string.modelo_cancelar, true, modelo::cancelarDescarga);
    }

    private static int textoFase(String fase) {
        switch (DescargaModeloWorker.Fase.valueOf(fase)) {
            case COPIANDO:
                return R.string.modelo_copiando;
            case VERIFICANDO:
                return R.string.modelo_verificando;
            case PROBANDO:
                return R.string.modelo_probando;
            default:
                return R.string.modelo_descargando;
        }
    }

    private void estado(String texto, int boton, boolean activo, Runnable alTocar) {
        binding.textoEstado.setText(texto);
        binding.botonDescargar.setText(boton);
        binding.botonDescargar.setEnabled(activo);
        binding.botonDescargar.setOnClickListener(alTocar == null ? null : v -> alTocar.run());
    }

    private void comprobacion(int etiqueta, String valor, boolean bien) {
        ItemComprobacionBinding fila = ItemComprobacionBinding.inflate(getLayoutInflater(), binding.comprobaciones, true);
        fila.icono.setImageResource(bien ? R.drawable.ic_check_circulo : R.drawable.ic_error_circulo);
        fila.icono.setColorFilter(ContextCompat.getColor(requireContext(), bien ? R.color.color_ok : R.color.color_error));
        fila.textoEtiqueta.setText(etiqueta);
        fila.textoValor.setText(valor);
        fila.getRoot().setContentDescription(getString(bien ? R.string.modelo_cumple : R.string.modelo_no_cumple,
                getString(etiqueta), valor));
    }

    /** {@code 2,6 GB}, con coma decimal como el resto de la app. */
    private static String gigas(long bytes) {
        double gb = bytes / (1024.0 * 1024 * 1024);
        return String.format(Locale.ROOT, "%.1f", gb) + " GB";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
