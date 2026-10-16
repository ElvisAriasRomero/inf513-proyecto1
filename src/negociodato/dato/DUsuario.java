package negociodato.dato;

import java.sql.SQLException;
import java.util.List;

/** CU1 - Tabla usuario (Director, Encargado y Cliente). */
public class DUsuario extends Dato {

    public static final String[] HEADERS
            = {"ID", "ROL", "NOMBRES", "APELLIDOS", "CI/NIT", "EMAIL", "TELEFONO", "USERNAME", "ACTIVO"};

    public static String guardar(int rolId, String nombres, String apellidos, String ciNit, String email,
            String telefono, String username, String password) throws SQLException {
        return valor("INSERT INTO usuario(rol_id, nombres, apellidos, ci_nit, email, telefono, username, password) "
                + "VALUES(?,?,?,?,?,?,?,?) RETURNING id",
                rolId, nombres, apellidos, ciNit, email, telefono, username, password);
    }

    public static int modificar(int id, int rolId, String nombres, String apellidos, String ciNit, String email,
            String telefono, String username) throws SQLException {
        return ejecutar("UPDATE usuario SET rol_id=?, nombres=?, apellidos=?, ci_nit=?, email=?, telefono=?, username=? "
                + "WHERE id=?", rolId, nombres, apellidos, ciNit, email, telefono, username, id);
    }

    /** No se borra: tiene cotizaciones, pagos y revisiones a su nombre. */
    public static int desactivar(int id) throws SQLException {
        return ejecutar("UPDATE usuario SET estado=FALSE WHERE id=?", id);
    }

    public static List<String[]> listar() throws SQLException {
        return consultar("SELECT u.id, r.nombre, u.nombres, u.apellidos, u.ci_nit, u.email, u.telefono, u.username, "
                + "CASE WHEN u.estado THEN 'Sí' ELSE 'No' END "
                + "FROM usuario u JOIN rol r ON r.id = u.rol_id ORDER BY u.id");
    }

    /** {id, rol} del usuario activo con ese correo, o null. */
    public static String[] sesion(String email) throws SQLException {
        return fila("SELECT u.id, r.nombre FROM usuario u JOIN rol r ON r.id = u.rol_id "
                + "WHERE LOWER(u.email) = LOWER(?) AND u.estado", email);
    }
}
