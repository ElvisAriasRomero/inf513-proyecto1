package negociodato.dato;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import negociodato.utils.Constantes;

/** CU5 - Tabla pauta (una fila por red social). */
public class DPauta extends Dato {

    public static final String[] HEADERS
            = {"ID", "MATERIAL", "RED", "CUENTA", "TIPO", "INICIO", "FIN", "URL", "ESTADO"};

    /** Solo con material aprobado y cuenta activa. Devuelve el id o null. */
    public static String guardar(int materialId, int cuentaId, String tipo, LocalDateTime inicio, LocalDateTime fin)
            throws SQLException {
        return valor("INSERT INTO pauta(material_id, cuenta_red_social_id, tipo, fecha_hora_inicio, fecha_hora_fin) "
                + "SELECT m.id, c.id, ?, ?, ? FROM material_publicitario m, cuenta_red_social c "
                + "WHERE m.id = ? AND m.estado = 'aprobado' AND c.id = ? AND c.estado RETURNING id",
                tipo, inicio, fin, materialId, cuentaId);
    }

    /** Pauta activa de la misma cuenta que se cruza con [inicio, fin), o null. */
    public static String[] choque(int cuentaId, LocalDateTime inicio, LocalDateTime fin) throws SQLException {
        return fila("SELECT id, tipo, TO_CHAR(fecha_hora_inicio, 'YYYY-MM-DD HH24:MI'), "
                + "TO_CHAR(fecha_hora_fin, 'YYYY-MM-DD HH24:MI') FROM pauta "
                + "WHERE cuenta_red_social_id = ? AND estado <> 'cancelada' "
                + "AND fecha_hora_inicio < ? AND fecha_hora_fin > ?", cuentaId, fin, inicio);
    }

    public static int publicar(int id, String url) throws SQLException {
        return ejecutar("UPDATE pauta SET estado='publicada', url_publicacion=? WHERE id=? AND estado='programada'", url, id);
    }

    public static int cancelar(int id) throws SQLException {
        return ejecutar("UPDATE pauta SET estado='cancelada' WHERE id=? AND estado='programada'", id);
    }

    public static List<String[]> listar() throws SQLException {
        return consultar("SELECT p.id, m.titulo, c.red_social, c.nombre_cuenta, p.tipo, "
                + "TO_CHAR(p.fecha_hora_inicio, 'YYYY-MM-DD HH24:MI'), TO_CHAR(p.fecha_hora_fin, 'YYYY-MM-DD HH24:MI'), "
                + "p.url_publicacion, p.estado FROM pauta p "
                + "JOIN material_publicitario m ON m.id = p.material_id "
                + "JOIN cuenta_red_social c ON c.id = p.cuenta_red_social_id ORDER BY p.fecha_hora_inicio DESC");
    }

    /** {id, url} de las pautas publicadas en los últimos días, para el scraping de métricas. */
    public static List<String[]> paraCapturar() throws SQLException {
        return consultar("SELECT id, url_publicacion FROM pauta WHERE estado = 'publicada' "
                + "AND fecha_hora_inicio >= NOW() - (? * INTERVAL '1 day')", Constantes.DIAS_SEGUIMIENTO);
    }
}
