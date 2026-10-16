package negociodato.dato;

import java.sql.SQLException;
import java.util.List;

/** CU3 - Tabla cuota (al contado = 1 cuota). */
public class DCuota extends Dato {

    public static final String[] HEADERS
            = {"ID", "COTIZACION", "N°", "MONTO (Bs)", "VENCE", "PAGADA EL", "METODO", "NRO TRANSACCION", "ESTADO"};

    public static final String[] HEADERS_COBRANZA
            = {"CUOTA", "COTIZACION", "CLIENTE", "TELEFONO", "N°", "MONTO (Bs)", "VENCE", "ESTADO"};

    private static final String SELECT = "SELECT id, cotizacion_id, nro_cuota, monto, fecha_vencimiento, fecha_pago, "
            + "metodo, nro_transaccion, estado FROM cuota ";

    /** Las pendientes con fecha pasada pasan a vencidas. */
    public static void marcarVencidas() throws SQLException {
        ejecutar("UPDATE cuota SET estado='vencida' WHERE estado='pendiente' AND fecha_vencimiento < CURRENT_DATE");
    }

    public static List<String[]> listar(int cotizacionId) throws SQLException {
        return consultar(SELECT + "WHERE cotizacion_id = ? ORDER BY nro_cuota", cotizacionId);
    }

    /** Cuotas sin pagar de todos los clientes, primero las vencidas. */
    public static List<String[]> porCobrar() throws SQLException {
        return consultar("SELECT q.id, q.cotizacion_id, u.nombres || ' ' || u.apellidos, u.telefono, q.nro_cuota, "
                + "q.monto, q.fecha_vencimiento, q.estado FROM cuota q "
                + "JOIN cotizacion c ON c.id = q.cotizacion_id JOIN usuario u ON u.id = c.cliente_id "
                + "WHERE q.estado <> 'pagada' ORDER BY q.estado = 'vencida' DESC, q.fecha_vencimiento");
    }

    /** Registra el pago. Devuelve el id de la cotización o null si la cuota no existe o ya estaba pagada. */
    public static String pagar(int id, String metodo, String nroTransaccion, String comprobante, int verificadoPor)
            throws SQLException {
        return valor("UPDATE cuota SET estado='pagada', fecha_pago=CURRENT_DATE, metodo=?, nro_transaccion=?, "
                + "comprobante=?, verificado_por=? WHERE id=? AND estado<>'pagada' RETURNING cotizacion_id",
                metodo, nroTransaccion, comprobante, verificadoPor, id);
    }

    /** Datos para generar el QR: {cotizacion_id, monto, nro_cuota, cliente_id, cliente, ci_nit, telefono, email}. */
    public static String[] paraQr(int id) throws SQLException {
        return fila("SELECT q.cotizacion_id, q.monto, q.nro_cuota, u.id, u.nombres || ' ' || u.apellidos, u.ci_nit, "
                + "u.telefono, u.email FROM cuota q JOIN cotizacion c ON c.id = q.cotizacion_id "
                + "JOIN usuario u ON u.id = c.cliente_id WHERE q.id = ? AND q.estado <> 'pagada'", id);
    }

    /** Guarda el número de pedido enviado a PagoFácil; el webhook lo usa para saber qué cuota se pagó. */
    public static int guardarPedido(int id, String pedido) throws SQLException {
        return ejecutar("UPDATE cuota SET nro_transaccion=? WHERE id=? AND estado<>'pagada'", pedido, id);
    }

    /**
     * Marca pagada la cuota de ese pedido (solo si sigue sin pagar, así un aviso repetido no hace nada).
     * Devuelve {id, cotizacion_id, nro_cuota, monto, email del cliente} o null.
     */
    public static String[] pagarPorPedido(String pedido, String comprobante) throws SQLException {
        return fila("WITH q AS (UPDATE cuota SET estado='pagada', fecha_pago=CURRENT_DATE, metodo='QR', comprobante=? "
                + "WHERE nro_transaccion=? AND estado<>'pagada' RETURNING id, cotizacion_id, nro_cuota, monto) "
                + "SELECT q.id, q.cotizacion_id, q.nro_cuota, q.monto, u.email FROM q "
                + "JOIN cotizacion c ON c.id = q.cotizacion_id JOIN usuario u ON u.id = c.cliente_id",
                comprobante, pedido);
    }

    public static String saldo(int cotizacionId) throws SQLException {
        return valor("SELECT COALESCE(SUM(monto), 0) FROM cuota WHERE cotizacion_id=? AND estado<>'pagada'", cotizacionId);
    }
}
