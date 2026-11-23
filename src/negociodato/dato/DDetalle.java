package negociodato.dato;

import java.sql.SQLException;
import java.util.List;

/** CU3 - Tabla detalle_cotizacion. */
public class DDetalle extends Dato {

    public static final String[] HEADERS = {"ID", "SERVICIO", "CANTIDAD", "PRECIO (Bs)", "SUBTOTAL (Bs)"};

    /** Toma el precio actual del servicio. Solo si la cotización sigue pendiente. */
    public static int agregar(int cotizacionId, int servicioId, int cantidad) throws SQLException {
        return ejecutar("INSERT INTO detalle_cotizacion(cotizacion_id, servicio_id, cantidad, precio_unitario, subtotal) "
                + "SELECT c.id, s.id, ?, s.precio, s.precio * ? FROM cotizacion c, servicio s "
                + "WHERE c.id = ? AND c.estado = 'pendiente' AND s.id = ?",
                cantidad, cantidad, cotizacionId, servicioId);
    }

    public static List<String[]> listar(int cotizacionId) throws SQLException {
        return consultar("SELECT d.id, s.nombre, d.cantidad, d.precio_unitario, d.subtotal "
                + "FROM detalle_cotizacion d JOIN servicio s ON s.id = d.servicio_id "
                + "WHERE d.cotizacion_id = ? ORDER BY d.id", cotizacionId);
    }
}
