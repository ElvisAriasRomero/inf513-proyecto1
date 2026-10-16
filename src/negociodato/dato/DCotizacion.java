package negociodato.dato;

import java.sql.SQLException;
import java.util.List;

/** CU3 - Tabla cotizacion. */
public class DCotizacion extends Dato {

    public static final String[] HEADERS
            = {"ID", "CLIENTE", "GENERADA POR", "FECHA", "TOTAL (Bs)", "PAGO", "CUOTAS", "ESTADO"};

    private static final String SELECT = "SELECT c.id, cl.nombres || ' ' || cl.apellidos, "
            + "u.nombres || ' ' || u.apellidos, c.fecha, c.total, c.tipo_pago, c.nro_cuotas, c.estado "
            + "FROM cotizacion c JOIN usuario cl ON cl.id = c.cliente_id JOIN usuario u ON u.id = c.usuario_id ";

    /** Solo se crea si cliente_id es un usuario con rol Cliente. Devuelve el id o null. */
    public static String guardar(int clienteId, int usuarioId, String tipoPago, int nroCuotas) throws SQLException {
        return valor("INSERT INTO cotizacion(cliente_id, usuario_id, tipo_pago, nro_cuotas) "
                + "SELECT u.id, ?, ?, ? FROM usuario u JOIN rol r ON r.id = u.rol_id "
                + "WHERE u.id = ? AND r.nombre = 'Cliente' RETURNING id",
                usuarioId, tipoPago, nroCuotas, clienteId);
    }

    public static int modificar(int id, String tipoPago, int nroCuotas) throws SQLException {
        return ejecutar("UPDATE cotizacion SET tipo_pago=?, nro_cuotas=? WHERE id=? AND estado='pendiente'",
                tipoPago, nroCuotas, id);
    }

    public static int eliminar(int id) throws SQLException {
        return ejecutar("DELETE FROM cotizacion WHERE id=? AND estado='pendiente'", id);
    }

    public static List<String[]> listar() throws SQLException {
        return consultar(SELECT + "ORDER BY c.id DESC");
    }

    public static List<String[]> listarDeCliente(int clienteId) throws SQLException {
        return consultar(SELECT + "WHERE c.cliente_id = ? ORDER BY c.id DESC", clienteId);
    }

    public static String[] ver(int id) throws SQLException {
        return fila(SELECT + "WHERE c.id = ?", id);
    }

    public static String clienteDe(int id) throws SQLException {
        return valor("SELECT cliente_id FROM cotizacion WHERE id=?", id);
    }

    /**
     * Pasa a aprobada y crea sus cuotas en una sola sentencia.
     * La última cuota absorbe los centavos del redondeo; vencen una por mes desde hoy.
     * Devuelve cuántas cuotas creó (0 si no estaba pendiente o no tiene servicios).
     */
    public static int aprobar(int id) throws SQLException {
        return ejecutar("WITH c AS ("
                + "  UPDATE cotizacion SET estado='aprobada' WHERE id=? AND estado='pendiente' AND total > 0"
                + "  RETURNING id, total, nro_cuotas) "
                + "INSERT INTO cuota(cotizacion_id, nro_cuota, monto, fecha_vencimiento) "
                + "SELECT c.id, g, "
                + "  CASE WHEN g < c.nro_cuotas THEN ROUND(c.total / c.nro_cuotas, 2) "
                + "       ELSE c.total - ROUND(c.total / c.nro_cuotas, 2) * (c.nro_cuotas - 1) END, "
                + "  (CURRENT_DATE + (g - 1) * INTERVAL '1 month')::date "
                + "FROM c, generate_series(1, c.nro_cuotas) g", id);
    }

    public static int rechazar(int id) throws SQLException {
        return ejecutar("UPDATE cotizacion SET estado='rechazada' WHERE id=? AND estado='pendiente'", id);
    }

    public static void recalcularTotal(int id) throws SQLException {
        ejecutar("UPDATE cotizacion SET total = (SELECT COALESCE(SUM(subtotal), 0) FROM detalle_cotizacion "
                + "WHERE cotizacion_id = ?) WHERE id = ?", id, id);
    }

    /** Pasa a pagada cuando ya no le queda ninguna cuota por pagar. */
    public static int marcarPagadaSiCompleta(int id) throws SQLException {
        return ejecutar("UPDATE cotizacion SET estado='pagada' WHERE id=? AND estado='aprobada' "
                + "AND NOT EXISTS (SELECT 1 FROM cuota WHERE cotizacion_id=? AND estado<>'pagada')", id, id);
    }
}
