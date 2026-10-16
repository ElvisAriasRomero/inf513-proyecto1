package negociodato.dato;

import database.DBConeccion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a PostgreSQL compartido por todas las clases D*.
 * Cada llamada abre su conexión y la cierra al terminar.
 */
public abstract class Dato {

    /** INSERT, UPDATE o DELETE. Devuelve cuántas filas cambió. */
    protected static int ejecutar(String sql, Object... params) throws SQLException {
        try (Connection c = DBConeccion.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            asignar(ps, params);
            return ps.executeUpdate();
        }
    }

    /** SELECT (o INSERT ... RETURNING). Cada fila como String[]; los null salen como "". */
    protected static List<String[]> consultar(String sql, Object... params) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        try (Connection c = DBConeccion.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            asignar(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                int columnas = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    String[] fila = new String[columnas];
                    for (int i = 0; i < columnas; i++) {
                        String v = rs.getString(i + 1);
                        fila[i] = v == null ? "" : v;
                    }
                    filas.add(fila);
                }
            }
        }
        return filas;
    }

    /** Primera fila o null. */
    protected static String[] fila(String sql, Object... params) throws SQLException {
        List<String[]> filas = consultar(sql, params);
        return filas.isEmpty() ? null : filas.get(0);
    }

    /** Primer valor de la primera fila o null (útil con RETURNING id). */
    protected static String valor(String sql, Object... params) throws SQLException {
        String[] f = fila(sql, params);
        return f == null || f[0].isEmpty() ? null : f[0];
    }

    private static void asignar(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
}
