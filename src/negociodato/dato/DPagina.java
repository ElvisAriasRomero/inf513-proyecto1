package negociodato.dato;

import java.sql.SQLException;
import java.util.List;

/** CU6 - Tabla pagina_detectada (páginas externas que usan el nombre de la universidad). */
public class DPagina extends Dato {

    public static final String[] HEADERS
            = {"ID", "RED", "PAGINA", "URL", "PALABRA", "SEGUIDORES", "ESTADO", "REVISADO POR", "DETECTADA"};

    /** Devuelve 1 si es nueva y 0 si esa URL ya se conocía. */
    public static int guardar(String red, String nombre, String url, String palabra, long seguidores) throws SQLException {
        return ejecutar("INSERT INTO pagina_detectada(red_social, nombre_pagina, url, palabra_clave, seguidores) "
                + "VALUES(?,?,?,?,?) ON CONFLICT (url) DO NOTHING", red, nombre, url, palabra, seguidores);
    }

    /** Primero las que faltan revisar y, entre ellas, las que tienen más seguidores. */
    public static List<String[]> listar() throws SQLException {
        return consultar("SELECT p.id, p.red_social, p.nombre_pagina, p.url, p.palabra_clave, p.seguidores, p.estado, "
                + "u.nombres || ' ' || u.apellidos, TO_CHAR(p.fecha_deteccion, 'YYYY-MM-DD') "
                + "FROM pagina_detectada p LEFT JOIN usuario u ON u.id = p.revisado_por "
                + "ORDER BY p.estado = 'detectada' DESC, p.seguidores DESC, p.id DESC");
    }

    public static int revisar(int id, String estado, int usuarioId) throws SQLException {
        return ejecutar("UPDATE pagina_detectada SET estado=?, revisado_por=? WHERE id=? AND estado='detectada'",
                estado, usuarioId, id);
    }

    public static String pendientes() throws SQLException {
        return valor("SELECT COUNT(*) FROM pagina_detectada WHERE estado='detectada'");
    }
}
