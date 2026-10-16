package negociodato.dato;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/** CU3 - Tabla servicio (oferta comercial). */
public class DServicio extends Dato {

    public static final String[] HEADERS = {"ID", "NOMBRE", "DESCRIPCION", "PRECIO (Bs)", "ACTIVO"};

    public static String guardar(String nombre, String descripcion, BigDecimal precio) throws SQLException {
        return valor("INSERT INTO servicio(nombre, descripcion, precio) VALUES(?,?,?) RETURNING id",
                nombre, descripcion, precio);
    }

    public static int modificar(int id, String nombre, String descripcion, BigDecimal precio) throws SQLException {
        return ejecutar("UPDATE servicio SET nombre=?, descripcion=?, precio=? WHERE id=?", nombre, descripcion, precio, id);
    }

    /** No se borra: puede estar en cotizaciones anteriores. */
    public static int desactivar(int id) throws SQLException {
        return ejecutar("UPDATE servicio SET estado=FALSE WHERE id=?", id);
    }

    public static List<String[]> listar() throws SQLException {
        return consultar("SELECT id, nombre, descripcion, precio, CASE WHEN estado THEN 'Sí' ELSE 'No' END "
                + "FROM servicio ORDER BY estado DESC, id");
    }
}
