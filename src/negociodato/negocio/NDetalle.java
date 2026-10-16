package negociodato.negocio;

import java.sql.SQLException;
import java.util.List;
import negociodato.dato.DCotizacion;
import negociodato.dato.DDetalle;
import static negociodato.utils.Params.*;

/** CU3 - Servicios de una cotización. Cada cambio recalcula el total. */
public class NDetalle {

    /** [cotizacion_id; servicio_id; cantidad] */
    public void agregar(List<String> p) throws SQLException {
        int cotizacion = entero(p, 0);
        int cantidad = entero(p, 2);
        if (cantidad < 1) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        existe(DDetalle.agregar(cotizacion, entero(p, 1), cantidad),
                "La cotización no está pendiente o el servicio no existe");
        DCotizacion.recalcularTotal(cotizacion);
    }

    /** [id del detalle] */
    public void eliminar(List<String> p) throws SQLException {
        String cotizacion = existe(DDetalle.eliminar(entero(p, 0)),
                "El detalle no existe o la cotización ya no está pendiente");
        DCotizacion.recalcularTotal(Integer.parseInt(cotizacion));
    }

    public List<String[]> listar(Sesion s, int cotizacionId) throws SQLException {
        NCotizacion.verificarAcceso(s, cotizacionId);
        return DDetalle.listar(cotizacionId);
    }
}
