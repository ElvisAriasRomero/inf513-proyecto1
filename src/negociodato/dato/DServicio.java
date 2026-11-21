package negociodato.dato;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/** CU3 - Tabla servicio (oferta comercial). */
public class DServicio extends Dato {

    public static final String[] HEADERS = {"ID", "NOMBRE", "DESCRIPCION", "PRECIO (Bs)"};

    public static String guardar(String nombre, String descripcion, BigDecimal precio) throws SQLException {
        return valor("INSERT INTO servicio(nombre, descripcion, precio) VALUES(?,?,?) RETURNING id",
                nombre, descripcion, precio);
    }

    public static int modificar(int id, String nombre, String descripcion, BigDecimal precio) throws SQLException {
        return ejecutar("UPDATE servicio SET nombre=?, descripcion=?, precio=? WHERE id=?", nombre, descripcion, precio, id);
    }

    /**
     * Borra el servicio; la base lo quita en cascada de las cotizaciones que lo tenían.
     * Las cotizaciones pendientes recalculan su total (las ya aprobadas conservan el total de sus cuotas).
     */
    public static int eliminar(int id) throws SQLException {
        int filas = ejecutar("DELETE FROM servicio WHERE id=?", id);
        ejecutar("UPDATE cotizacion c SET total = (SELECT COALESCE(SUM(subtotal), 0) FROM detalle_cotizacion d "
                + "WHERE d.cotizacion_id = c.id) WHERE c.estado = 'pendiente'");
        return filas;
    }

    public static List<String[]> listar() throws SQLException {
        return consultar("SELECT id, nombre, descripcion, trim_scale(precio) FROM servicio ORDER BY id"); // 0.020 → 0.02
    }
}
