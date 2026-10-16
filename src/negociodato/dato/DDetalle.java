package negociodato.dato;

import java.sql.SQLException;
import java.util.List;

/** CU3 - Tabla detalle_cotizacion. */
public class DDetalle extends Dato {

    public static final String[] HEADERS = {"ID", "SERVICIO", "CANTIDAD", "PRECIO (Bs)", "SUBTOTAL (Bs)"};

    /** Toma el precio actual del servicio. Solo si la cotización sigue pendiente y el servicio está activo. */
    public static int agregar(int cotizacionId, int servicioId, int cantidad) throws SQLException {
        return ejecutar("INSERT INTO detalle_cotizacion(cotizacion_id, servicio_id, cantidad, precio_unitario, subtotal) "
                + "SELECT c.id, s.id, ?, s.precio, s.precio * ? FROM cotizacion c, servicio s "
                + "WHERE c.id = ? AND c.estado = 'pendiente' AND s.id = ? AND s.estado",
                cantidad, cantidad, cotizacionId, servicioId);
    }

    /** Devuelve el id de la cotización o null si no se pudo quitar. */
    public static String eliminar(int id) throws SQLException {
        return valor("DELETE FROM detalle_cotizacion d USING cotizacion c "
                + "WHERE d.id = ? AND c.id = d.cotizacion_id AND c.estado = 'pendiente' RETURNING d.cotizacion_id", id);
    }

    public static List<String[]> listar(int cotizacionId) throws SQLException {
        return consultar("SELECT d.id, s.nombre, d.cantidad, d.precio_unitario, d.subtotal "
                + "FROM detalle_cotizacion d JOIN servicio s ON s.id = d.servicio_id "
                + "WHERE d.cotizacion_id = ? ORDER BY d.id", cotizacionId);
    }
}
