package negociodato.dato;

import java.sql.SQLException;
import java.util.List;

/** CU1 - Tabla rol. */
public class DRol extends Dato {

    public static final String[] HEADERS = {"ID", "NOMBRE"};

    public static String guardar(String nombre) throws SQLException {
        return valor("INSERT INTO rol(nombre) VALUES(?) RETURNING id", nombre);
    }

    public static int modificar(int id, String nombre) throws SQLException {
        return ejecutar("UPDATE rol SET nombre=? WHERE id=?", nombre, id);
    }

    public static int eliminar(int id) throws SQLException {
        return ejecutar("DELETE FROM rol WHERE id=?", id);
    }

    public static List<String[]> listar() throws SQLException {
        return consultar("SELECT id, nombre FROM rol ORDER BY id");
    }
}
