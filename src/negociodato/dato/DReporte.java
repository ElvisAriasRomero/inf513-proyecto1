package negociodato.dato;

import java.sql.SQLException;
import java.util.List;

/**
 * CU8 - Reportes. No tiene tabla propia: consulta las demás.
 * En metrica cada fila es el acumulado de ese momento, así que se usa solo la última captura de cada pauta.
 */
public class DReporte extends Dato {

    public static final String[] HEADERS_REDES
            = {"RED", "PUBLICACIONES", "VISTAS", "INTERACCIONES", "VISTAS PROMEDIO", "ENGAGEMENT %"};
    public static final String[] HEADERS_TIPOS
            = {"RED", "TIPO", "PUBLICACIONES", "VISTAS PROMEDIO", "INTERACCIONES PROMEDIO"};
    public static final String[] HEADERS_MENSUAL = {"MES", "RED", "VISTAS", "INTERACCIONES"};
    public static final String[] HEADERS_PAGINAS = {"RED", "ESTADO", "CANTIDAD", "SEGUIDORES"};
    public static final String[] HEADERS_COTIZACIONES = {"ESTADO", "CANTIDAD", "MONTO (Bs)"};

    private static final String ULTIMA = "WITH ultima AS (SELECT DISTINCT ON (pauta_id) * FROM metrica "
            + "ORDER BY pauta_id, fecha_captura DESC) ";
    private static final String JOINS = "FROM ultima u JOIN pauta p ON p.id = u.pauta_id "
            + "JOIN cuenta_red_social c ON c.id = p.cuenta_red_social_id ";
    private static final String INTERACCIONES = "(u.reacciones + u.comentarios + u.compartidos)";

    /** ¿Qué red social es más efectiva? */
    public static List<String[]> redes() throws SQLException {
        return consultar(ULTIMA + "SELECT c.red_social, COUNT(*), SUM(u.vistas), SUM" + INTERACCIONES + ", "
                + "ROUND(AVG(u.vistas)), "
                + "COALESCE(ROUND(100.0 * SUM" + INTERACCIONES + " / NULLIF(SUM(u.vistas), 0), 2), 0) AS engagement "
                + JOINS + "GROUP BY c.red_social ORDER BY engagement DESC");
    }

    /** ¿Qué tipo de publicación rinde más en cada red? */
    public static List<String[]> tipos() throws SQLException {
        return consultar(ULTIMA + "SELECT c.red_social, p.tipo, COUNT(*), ROUND(AVG(u.vistas)) AS vistas, "
                + "ROUND(AVG" + INTERACCIONES + ") " + JOINS
                + "GROUP BY c.red_social, p.tipo ORDER BY c.red_social, vistas DESC");
    }

    /** Movimiento por mes en cada red. */
    public static List<String[]> mensual() throws SQLException {
        return consultar(ULTIMA + "SELECT TO_CHAR(p.fecha_hora_inicio, 'YYYY-MM') AS mes, c.red_social, "
                + "SUM(u.vistas), SUM" + INTERACCIONES + " " + JOINS
                + "GROUP BY mes, c.red_social ORDER BY mes, c.red_social");
    }

    /** Páginas externas por red y estado. */
    public static List<String[]> paginas() throws SQLException {
        return consultar("SELECT red_social, estado, COUNT(*), SUM(seguidores) FROM pagina_detectada "
                + "GROUP BY red_social, estado ORDER BY red_social, estado");
    }

    /** Cotizaciones por estado. */
    public static List<String[]> cotizaciones() throws SQLException {
        return consultar("SELECT estado, COUNT(*), SUM(total) FROM cotizacion GROUP BY estado ORDER BY estado");
    }
}
