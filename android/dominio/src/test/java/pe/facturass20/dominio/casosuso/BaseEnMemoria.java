package pe.facturass20.dominio.casosuso;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import pe.facturass20.dominio.Datos;
import pe.facturass20.dominio.modelo.Contribuyente;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.Emisor;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.ImagenFactura;
import pe.facturass20.dominio.modelo.ParametrosNrus;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.modelo.ResultadoExtraccion;
import pe.facturass20.dominio.puertos.ContribuyenteRepositorio;
import pe.facturass20.dominio.puertos.FacturaRepositorio;
import pe.facturass20.dominio.puertos.ParametrosRepositorio;
import pe.facturass20.dominio.puertos.PeriodoRepositorio;

/** Doble de prueba de los repositorios. Solo para las pruebas del dominio; la app usa Room. */
final class BaseEnMemoria implements PeriodoRepositorio, FacturaRepositorio, ContribuyenteRepositorio,
        ParametrosRepositorio {

    final Map<YearMonth, PeriodoMensual> periodos = new TreeMap<>();
    final Map<YearMonth, Determinacion> determinaciones = new HashMap<>();
    Contribuyente contribuyente;
    ParametrosNrus parametros = Datos.parametros();
    ImagenFactura ultimaImagen;
    ResultadoExtraccion ultimaExtraccion;
    int escrituras;
    private long siguienteId = 1;

    @Override
    public Optional<PeriodoMensual> buscar(YearMonth periodo) {
        return Optional.ofNullable(periodos.get(periodo));
    }

    @Override
    public List<PeriodoMensual> listarDelAnio(int anio) {
        return periodos.values().stream().filter(p -> p.anio() == anio).toList();
    }

    @Override
    public List<PeriodoMensual> listar() {
        List<PeriodoMensual> todos = new ArrayList<>(periodos.values());
        java.util.Collections.reverse(todos);
        return todos;
    }

    @Override
    public Optional<Determinacion> buscarDeterminacion(YearMonth periodo) {
        return Optional.ofNullable(determinaciones.get(periodo));
    }

    @Override
    public void guardar(PeriodoMensual periodo, Determinacion determinacion) {
        periodos.put(periodo.periodo(), periodo);
        determinaciones.put(periodo.periodo(), determinacion);
        escrituras++;
    }

    private List<FacturaCompra> todasLasFacturas() {
        return periodos.values().stream().flatMap(p -> p.facturas().stream()).toList();
    }

    @Override
    public Optional<FacturaCompra> buscarPorId(long id) {
        return todasLasFacturas().stream().filter(f -> f.id() != null && f.id() == id).findFirst();
    }

    @Override
    public List<FacturaCompra> buscarPorEmisorYSerie(String rucEmisor, String serie) {
        return todasLasFacturas().stream()
                .filter(f -> f.emisor().ruc().equals(rucEmisor) && f.serie().equals(serie))
                .toList();
    }

    @Override
    public Optional<Emisor> buscarEmisor(String ruc) {
        return todasLasFacturas().stream().map(FacturaCompra::emisor).filter(e -> e.ruc().equals(ruc)).findFirst();
    }

    @Override
    public long registrar(PeriodoMensual periodo, FacturaCompra factura, ImagenFactura imagen,
            ResultadoExtraccion extraccion, Determinacion determinacion) {
        periodos.put(periodo.periodo(), periodo);
        determinaciones.put(periodo.periodo(), determinacion);
        ultimaImagen = imagen;
        ultimaExtraccion = extraccion;
        escrituras++;
        return siguienteId++;
    }

    @Override
    public void anular(FacturaCompra facturaAnulada, Determinacion determinacion) {
        determinaciones.put(facturaAnulada.periodo(), determinacion);
        escrituras++;
    }

    @Override
    public Optional<Contribuyente> obtener() {
        return Optional.ofNullable(contribuyente);
    }

    @Override
    public void guardar(Contribuyente contribuyente) {
        this.contribuyente = contribuyente;
    }

    @Override
    public ParametrosNrus vigentes() {
        return parametros;
    }
}
