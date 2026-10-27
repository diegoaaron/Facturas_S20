package pe.facturass20.ui.exportacion;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pe.facturass20.R;
import pe.facturass20.databinding.FragmentExportarDatosBinding;
import pe.facturass20.databinding.ItemEnlaceBinding;
import pe.facturass20.databinding.ItemInterruptorBinding;
import pe.facturass20.ui.comun.Filas;
import pe.facturass20.ui.comun.Formatos;
import pe.facturass20.ui.comun.Mensajes;

/**
 * P20 Exportar datos (prototipo pantalla_20_exportar_datos.png, RF-21): un mes o todo un año, con o sin
 * imágenes y reporte PDF, con el nombre y el tamaño estimado del ZIP. Solo ofrece períodos con datos.
 */
public class ExportarDatosFragment extends Fragment {

    private FragmentExportarDatosBinding binding;
    private ExportarDatosViewModel modelo;
    private ItemInterruptorBinding imagenes;
    private ItemInterruptorBinding pdf;
    /** Lo que se puede elegir en la lista: meses o años, según el alcance. */
    private final List<String> opciones = new ArrayList<>();
    private int elegida;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentExportarDatosBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        modelo = new ViewModelProvider(this).get(ExportarDatosViewModel.class);
        binding.barra.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        ItemEnlaceBinding csv = Filas.enlace(binding.opciones, getString(R.string.exportar_csv),
                getString(R.string.exportar_csv_detalle), null);
        csv.icono.setVisibility(View.VISIBLE);
        csv.icono.setImageResource(R.drawable.ic_check_circulo);
        csv.icono.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.color_ok)));
        Filas.separador(binding.opciones);
        imagenes = Filas.interruptor(binding.opciones, getString(R.string.exportar_imagenes), "", true, v -> actualizar());
        Filas.separador(binding.opciones);
        pdf = Filas.interruptor(binding.opciones, getString(R.string.exportar_pdf), "", true, v -> actualizar());

        binding.alcance.addOnButtonCheckedListener((grupo, id, marcado) -> {
            if (marcado) {
                elegida = 0;
                llenarOpciones();
            }
        });
        binding.listaPeriodos.setOnItemClickListener((padre, vista, posicion, id) -> {
            elegida = posicion;
            actualizar();
        });
        binding.botonGuardar.setOnClickListener(v -> modelo.exportar());
        binding.botonCompartir.setOnClickListener(v -> modelo.exportar());

        modelo.meses().observe(getViewLifecycleOwner(), meses -> {
            boolean hay = !meses.isEmpty();
            binding.contenido.setVisibility(hay ? View.VISIBLE : View.GONE);
            binding.textoSinDatos.setVisibility(hay ? View.GONE : View.VISIBLE);
            binding.botonGuardar.setEnabled(hay);
            binding.botonCompartir.setEnabled(hay);
            if (hay) {
                llenarOpciones();
            }
        });
        Mensajes.observar(this, modelo, binding.getRoot());
        modelo.cargar();
    }

    private boolean porAnio() {
        return binding.alcance.getCheckedButtonId() == R.id.opcion_anio;
    }

    private void llenarOpciones() {
        List<ExportarDatosViewModel.Mes> meses = modelo.meses().getValue();
        if (meses == null || meses.isEmpty()) {
            return;
        }
        opciones.clear();
        for (ExportarDatosViewModel.Mes mes : meses) {
            String opcion = porAnio() ? String.valueOf(mes.periodo().getYear()) : mes.periodo().toString();
            if (!opciones.contains(opcion)) {
                opciones.add(opcion);
            }
        }
        List<String> nombres = new ArrayList<>();
        for (String opcion : opciones) {
            nombres.add(porAnio() ? opcion : Formatos.periodo(YearMonth.parse(opcion)));
        }
        binding.campoPeriodo.setHint(porAnio() ? R.string.exportar_anio : R.string.exportar_mes);
        binding.listaPeriodos.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, nombres));
        elegida = Math.min(elegida, opciones.size() - 1);
        binding.listaPeriodos.setText(nombres.get(elegida), false);
        actualizar();
    }

    /** Recalcula los detalles de las opciones, el nombre del ZIP y el tamaño. */
    private void actualizar() {
        List<ExportarDatosViewModel.Mes> meses = modelo.meses().getValue();
        if (meses == null || opciones.isEmpty() || binding == null) {
            return;
        }
        String elegido = opciones.get(elegida);
        int facturas = 0;
        int fotos = 0;
        int cantidadMeses = 0;
        for (ExportarDatosViewModel.Mes mes : meses) {
            String clave = porAnio() ? String.valueOf(mes.periodo().getYear()) : mes.periodo().toString();
            if (clave.equals(elegido)) {
                facturas += mes.facturas();
                fotos += mes.imagenes();
                cantidadMeses++;
            }
        }
        imagenes.textoDetalle.setVisibility(View.VISIBLE);
        imagenes.textoDetalle.setText(getResources().getQuantityString(R.plurals.exportar_imagenes_detalle, fotos, fotos));
        pdf.textoDetalle.setVisibility(View.VISIBLE);
        pdf.textoDetalle.setText(porAnio() ? getString(R.string.exportar_pdf_anio)
                : getString(R.string.exportar_pdf_mes, elegido));
        long bytes = ExportarDatosViewModel.estimarBytes(facturas, fotos, cantidadMeses,
                imagenes.interruptor.isChecked(), pdf.interruptor.isChecked());
        binding.textoArchivo.setText(getString(R.string.exportar_archivo, elegido, tamano(bytes)));
    }

    private static String tamano(long bytes) {
        if (bytes < 1024 * 1024) {
            return Math.max(1, bytes / 1024) + " KB";
        }
        return String.format(Locale.ROOT, "%.1f", bytes / (1024.0 * 1024)) + " MB";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
