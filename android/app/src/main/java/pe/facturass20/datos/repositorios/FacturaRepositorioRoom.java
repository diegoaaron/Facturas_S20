package pe.facturass20.datos.repositorios;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pe.facturass20.datos.BaseDatosFacturas;
import pe.facturass20.datos.dao.FacturaConEmisor;
import pe.facturass20.datos.dao.FacturaDao;
import pe.facturass20.datos.entidades.CampoExtraidoEntity;
import pe.facturass20.datos.entidades.EmisorEntity;
import pe.facturass20.datos.entidades.ModeloLocalEntity;
import pe.facturass20.dominio.modelo.CampoExtraido;
import pe.facturass20.dominio.modelo.Determinacion;
import pe.facturass20.dominio.modelo.Emisor;
import pe.facturass20.dominio.modelo.FacturaCompra;
import pe.facturass20.dominio.modelo.ImagenFactura;
import pe.facturass20.dominio.modelo.PeriodoMensual;
import pe.facturass20.dominio.modelo.ResultadoExtraccion;
import pe.facturass20.dominio.puertos.FacturaRepositorio;
import pe.facturass20.dominio.puertos.Reloj;

/**
 * Facturas de compra. {@link #registrar} y {@link #anular} son una sola transacción cada una: si algo
 * falla a la mitad (o la app se cierra), no queda nada a medias (RNF-13).
 */
public final class FacturaRepositorioRoom implements FacturaRepositorio {

    private final BaseDatosFacturas db;
    private final FacturaDao dao;
    private final PeriodoRepositorioRoom periodos;
    private final Reloj reloj;

    public FacturaRepositorioRoom(BaseDatosFacturas db, PeriodoRepositorioRoom periodos, Reloj reloj) {
        this.db = db;
        this.dao = db.facturaDao();
        this.periodos = periodos;
        this.reloj = reloj;
    }

    @Override
    public Optional<FacturaCompra> buscarPorId(long id) {
        FacturaConEmisor fila = dao.buscarPorId(id);
        return fila == null ? Optional.empty() : Optional.of(Mapeos.aDominio(fila));
    }

    @Override
    public List<FacturaCompra> buscarPorEmisorYSerie(String rucEmisor, String serie) {
        return Mapeos.facturas(dao.buscarPorEmisorYSerie(rucEmisor, serie));
    }

    @Override
    public Optional<Emisor> buscarEmisor(String ruc) {
        EmisorEntity fila = dao.buscarEmisor(ruc);
        return fila == null ? Optional.empty() : Optional.of(Mapeos.aDominio(fila));
    }

    @Override
    public long registrar(PeriodoMensual periodo, FacturaCompra factura, ImagenFactura imagen,
            ResultadoExtraccion extraccion, Determinacion determinacion) {
        return db.runInTransaction(() -> {
            long idPeriodo = periodos.guardarPeriodo(periodo);
            long idFactura = dao.insertar(Mapeos.aEntidad(factura, idPeriodo, guardarEmisor(factura.emisor()),
                    reloj.ahora()));
            if (imagen != null) {
                dao.insertarImagen(Mapeos.aEntidad(imagen, idFactura));
            }
            if (extraccion != null) {
                ModeloLocalEntity modelo = db.modeloDao().buscarPorVersion(extraccion.versionModelo());
                Long idModelo = modelo == null ? null : modelo.idModelo;
                List<CampoExtraidoEntity> campos = new ArrayList<>();
                for (CampoExtraido campo : extraccion.campos()) {
                    campos.add(Mapeos.aEntidad(campo, idFactura, idModelo));
                }
                dao.insertarCampos(campos);
            }
            periodos.guardarDeterminacion(idPeriodo, determinacion);
            return idFactura;
        });
    }

    @Override
    public void anular(FacturaCompra facturaAnulada, Determinacion determinacion) {
        db.runInTransaction(() -> {
            FacturaConEmisor fila = dao.buscarPorId(facturaAnulada.id());
            if (fila == null) {
                throw new IllegalStateException("No existe la factura " + facturaAnulada.id());
            }
            dao.actualizarEstado(fila.factura.idFactura, facturaAnulada.estado().name(),
                    facturaAnulada.motivoAnulacion());
            periodos.guardarDeterminacion(fila.factura.idPeriodo, determinacion);
        });
    }

    /** Busca el emisor por RUC; si no existe lo crea y si cambió la razón social la actualiza. */
    private long guardarEmisor(Emisor emisor) {
        EmisorEntity fila = dao.buscarEmisor(emisor.ruc());
        if (fila == null) {
            fila = new EmisorEntity();
            fila.ruc = emisor.ruc();
            fila.razonSocial = emisor.razonSocial();
            return dao.insertarEmisor(fila);
        }
        if (!fila.razonSocial.equals(emisor.razonSocial())) {
            fila.razonSocial = emisor.razonSocial();
            dao.actualizarEmisor(fila);
        }
        return fila.idEmisor;
    }
}
