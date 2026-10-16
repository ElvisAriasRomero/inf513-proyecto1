package negociodato.dato;

import java.sql.SQLException;
import java.util.List;

/** CU4 - Tabla material_publicitario. */
public class DMaterial extends Dato {

    public static final String[] HEADERS
            = {"ID", "COTIZACION", "TITULO", "ARCHIVO", "TIPO", "ESTADO", "OBSERVACION", "VALIDADO POR", "FECHA"};

    private static final String SELECT = "SELECT m.id, m.cotizacion_id, m.titulo, m.archivo, m.tipo, m.estado, "
            + "m.observacion, v.nombres || ' ' || v.apellidos, TO_CHAR(m.fecha_carga, 'YYYY-MM-DD HH24:MI') "
            + "FROM material_publicitario m JOIN cotizacion c ON c.id = m.cotizacion_id "
            + "LEFT JOIN usuario v ON v.id = m.validado_por ";

    /** Solo se acepta si la cotización ya tiene al menos una cuota pagada. Devuelve el id o null. */
    public static String guardar(int cotizacionId, String titulo, String archivo, String tipo) throws SQLException {
        return valor("INSERT INTO material_publicitario(cotizacion_id, titulo, archivo, tipo) "
                + "SELECT ?, ?, ?, ? WHERE EXISTS (SELECT 1 FROM cuota WHERE cotizacion_id = ? AND estado = 'pagada') "
                + "RETURNING id", cotizacionId, titulo, archivo, tipo, cotizacionId);
    }

    public static List<String[]> listar() throws SQLException {
        return consultar(SELECT + "ORDER BY m.estado = 'pendiente' DESC, m.id DESC");
    }

    public static List<String[]> listarDeCotizacion(int cotizacionId) throws SQLException {
        return consultar(SELECT + "WHERE m.cotizacion_id = ? ORDER BY m.id DESC", cotizacionId);
    }

    public static List<String[]> listarDeCliente(int clienteId) throws SQLException {
        return consultar(SELECT + "WHERE c.cliente_id = ? ORDER BY m.id DESC", clienteId);
    }

    public static int validar(int id, String estado, String observacion, int usuarioId) throws SQLException {
        return ejecutar("UPDATE material_publicitario SET estado=?, observacion=?, validado_por=? "
                + "WHERE id=? AND estado='pendiente'", estado, observacion, usuarioId, id);
    }
}
