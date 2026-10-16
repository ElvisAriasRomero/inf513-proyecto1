package negociodato.dato;

import java.sql.SQLException;
import java.util.List;

/** CU2 - Tabla cuenta_red_social (cuentas oficiales de la universidad). */
public class DCuenta extends Dato {

    public static final String[] HEADERS = {"ID", "RED", "CUENTA", "URL", "ENCARGADO", "ACTIVA"};

    public static String guardar(int usuarioId, String red, String nombre, String url, String token) throws SQLException {
        return valor("INSERT INTO cuenta_red_social(usuario_id, red_social, nombre_cuenta, url, token_acceso) "
                + "VALUES(?,?,?,?,?) RETURNING id", usuarioId, red, nombre, url, token);
    }

    public static int modificar(int id, String red, String nombre, String url, String token) throws SQLException {
        return ejecutar("UPDATE cuenta_red_social SET red_social=?, nombre_cuenta=?, url=?, token_acceso=? WHERE id=?",
                red, nombre, url, token, id);
    }

    public static int desactivar(int id) throws SQLException {
        return ejecutar("UPDATE cuenta_red_social SET estado=FALSE WHERE id=?", id);
    }

    /** El token no se muestra por correo. */
    public static List<String[]> listar() throws SQLException {
        return consultar("SELECT c.id, c.red_social, c.nombre_cuenta, c.url, u.nombres || ' ' || u.apellidos, "
                + "CASE WHEN c.estado THEN 'Sí' ELSE 'No' END "
                + "FROM cuenta_red_social c JOIN usuario u ON u.id = c.usuario_id ORDER BY c.red_social, c.id");
    }

    /** URLs de todas las cuentas propias, para que el scraping no las marque como externas. */
    public static List<String[]> urls() throws SQLException {
        return consultar("SELECT url FROM cuenta_red_social");
    }
}
