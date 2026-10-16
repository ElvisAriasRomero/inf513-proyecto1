package negociodato.dato;

import java.sql.SQLException;
import java.util.List;

/** CU7 - Tabla metrica (una fila por cada captura de una publicación). */
public class DMetrica extends Dato {

    public static final String[] HEADERS = {"CAPTURA", "VISTAS", "REACCIONES", "COMENTARIOS", "COMPARTIDOS"};

    public static int guardar(int pautaId, long vistas, long reacciones, long comentarios, long compartidos)
            throws SQLException {
        return ejecutar("INSERT INTO metrica(pauta_id, vistas, reacciones, comentarios, compartidos) VALUES(?,?,?,?,?)",
                pautaId, vistas, reacciones, comentarios, compartidos);
    }

    public static List<String[]> listar(int pautaId) throws SQLException {
        return consultar("SELECT TO_CHAR(fecha_captura, 'YYYY-MM-DD HH24:MI'), vistas, reacciones, comentarios, compartidos "
                + "FROM metrica WHERE pauta_id = ? ORDER BY fecha_captura", pautaId);
    }
}
