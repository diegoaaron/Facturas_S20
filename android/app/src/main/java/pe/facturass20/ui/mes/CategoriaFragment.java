package pe.facturass20.ui.mes;

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

import java.math.BigDecimal;
import java.math.RoundingMode;

import pe.facturass20.R;
import pe.facturass20.databinding.FragmentCategoriaBinding;
import pe.facturass20.databinding.ItemFilaCategoriaBinding;
import pe.facturass20.dominio.modelo.CategoriaNRUS;
import pe.facturass20.dominio.modelo.ParametrosNrus;
import pe.facturass20.ui.comun.BarraLimiteView;
import pe.facturass20.ui.comun.Filas;
import pe.facturass20.ui.comun.Formatos;
import pe.facturass20.ui.comun.Mensajes;
import pe.facturass20.ui.inicio.ResumenInicio;

/**
 * P14 Categoría y cuota (prototipo pantalla_14_categoria_y_cuota.png): la categoría y la cuota del mes,
 * compras y ventas frente al límite (decide el mayor), la tabla de la versión vigente de los parámetros
 * (RF-22), los avisos que el usuario quiere recibir y los enlaces a P15 y P17.
 */
public class CategoriaFragment extends Fragment {

    private FragmentCategoriaBinding binding;
    private CategoriaViewModel modelo;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCategoriaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        modelo = new ViewModelProvider(this).get(CategoriaViewModel.class);

        aviso(R.string.categoria_aviso_80, PreferenciasAvisos.Aviso.LIMITE_80);
        aviso(R.string.categoria_aviso_100, PreferenciasAvisos.Aviso.LIMITE_100);
        aviso(R.string.categoria_aviso_tope, PreferenciasAvisos.Aviso.TOPE_ANUAL);
        Filas.enlace(binding.enlaces, getString(R.string.p15_titulo), getString(R.string.categoria_vencimiento_detalle),
                () -> navegar(R.id.categoria_a_vencimiento));
        Filas.separador(binding.enlaces);
        Filas.enlace(binding.enlaces, getString(R.string.p17_titulo), getString(R.string.categoria_historial_detalle),
                () -> navegar(R.id.categoria_a_historial));
        binding.botonVentas.setOnClickListener(v -> navegar(R.id.categoria_a_ventas));

        modelo.vista().observe(getViewLifecycleOwner(), this::mostrar);
        modelo.ocupado().observe(getViewLifecycleOwner(), ocupado ->
                binding.progreso.setVisibility(ocupado && modelo.vista().getValue() == null ? View.VISIBLE : View.GONE));
        Mensajes.observar(this, modelo, binding.getRoot());
    }

    @Override
    public void onResume() {
        super.onResume();
        modelo.cargar();
    }

    private void aviso(int titulo, PreferenciasAvisos.Aviso aviso) {
        Filas.interruptor(binding.avisos, getString(titulo), null, modelo.avisoActivado(aviso),
                activado -> modelo.activarAviso(aviso, activado));
    }

    private void mostrar(CategoriaViewModel.Vista vista) {
        ResumenInicio resumen = vista.resumen();
        ParametrosNrus parametros = vista.parametros();
        binding.desplazamiento.setVisibility(View.VISIBLE);
        binding.barra.setSubtitle(Formatos.periodo(resumen.periodo()));

        if (resumen.fueraDeRegimen()) {
            binding.textoCategoria.setText(R.string.p04_fuera_nrus);
            binding.textoCuota.setText(R.string.p04_fuera_ayuda);
            binding.textoNumeroCategoria.setText("!");
        } else {
            binding.textoCategoria.setText(getString(R.string.p04_categoria, resumen.categoria()));
            binding.textoCuota.setText(getString(R.string.categoria_cuota_mensual, Formatos.moneda(resumen.cuota())));
            binding.textoNumeroCategoria.setText(String.valueOf(resumen.categoria()));
        }
        binding.textoTopeAnual.setVisibility(resumen.avisoTopeAnual() ? View.VISIBLE : View.GONE);
        binding.textoTopeAnual.setText(getString(R.string.p04_tope_anual, Formatos.monedaCorta(resumen.topeAnual())));

        BigDecimal compras = resumen.compras();
        BigDecimal ventas = resumen.ventas();
        boolean comprasMayor = ventas == null || compras.compareTo(ventas) >= 0;
        String mayor = getString(R.string.categoria_mayor);
        binding.textoCompras.setText(comprasMayor ? Formatos.moneda(compras) + mayor : Formatos.moneda(compras));
        barra(binding.barraCompras, compras, resumen);
        if (ventas == null) {
            binding.textoVentas.setText(R.string.p04_ventas_sin_registrar);
            barra(binding.barraVentas, BigDecimal.ZERO, resumen);
            binding.botonVentas.setText(R.string.categoria_registrar_ventas);
        } else {
            binding.textoVentas.setText(comprasMayor ? Formatos.moneda(ventas) : Formatos.moneda(ventas) + mayor);
            barra(binding.barraVentas, ventas, resumen);
            binding.botonVentas.setText(R.string.categoria_editar_ventas);
        }
        BigDecimal umbral = resumen.limite().multiply(resumen.umbralAviso());
        binding.textoMarca.setText(getString(R.string.categoria_marca, porcentaje(resumen.umbralAviso()),
                Formatos.monedaCorta(umbral)));

        binding.textoTabla.setText(getString(R.string.categoria_tabla, parametros.version()));
        binding.tabla.removeAllViews();
        for (CategoriaNRUS categoria : parametros.categoriasVigentes(resumen.periodo().atDay(1))) {
            ItemFilaCategoriaBinding fila = ItemFilaCategoriaBinding.inflate(getLayoutInflater(), binding.tabla, true);
            fila.textoCodigo.setText(getString(R.string.categoria_codigo, categoria.codigo()));
            fila.textoLimite.setText(getString(R.string.categoria_hasta, Formatos.monedaCorta(categoria.limiteMensual())));
            fila.textoCuota.setText(Formatos.monedaCorta(categoria.cuota()));
            if (resumen.categoria() != null && resumen.categoria() == categoria.codigo()) {
                fila.getRoot().setBackgroundResource(R.drawable.fondo_fila_resaltada);
            }
        }
    }

    /** Barra de un monto frente al límite de la categoría, ámbar desde el umbral y roja al pasarlo. */
    private void barra(BarraLimiteView barra, BigDecimal monto, ResumenInicio resumen) {
        BigDecimal limite = resumen.limite();
        int progreso = monto.multiply(BigDecimal.valueOf(100)).divide(limite, 0, RoundingMode.DOWN).intValue();
        barra.setProgreso(progreso);
        barra.setMarca(resumen.umbralAviso().floatValue());
        int color;
        if (monto.compareTo(limite) > 0) {
            color = R.color.color_error;
        } else if (monto.compareTo(limite.multiply(resumen.umbralAviso())) >= 0) {
            color = R.color.color_alerta;
        } else {
            color = R.color.color_acento;
        }
        barra.setColorIndicador(ContextCompat.getColor(requireContext(), color));
    }

    private static String porcentaje(BigDecimal fraccion) {
        return fraccion.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + Formatos.ESPACIO + "%";
    }

    private void navegar(int accion) {
        NavHostFragment.findNavController(this).navigate(accion);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
