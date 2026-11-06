package negociodato.negocio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;
import negociodato.dato.DUsuario;
import static negociodato.utils.Params.*;

/** CU1 - Usuarios: Director, Encargado y Cliente. */
public class NUsuario {

    /** [rol_id; nombres; apellidos; ci_nit; email; telefono; username; password] */
    public String guardar(List<String> p) throws SQLException {
        return DUsuario.guardar(entero(p, 0), texto(p, 1), texto(p, 2), opcional(p, 3), email(p, 4),
                opcional(p, 5), texto(p, 6), sha256(texto(p, 7)));
    }

    /**
     * Auto-registro de un visitante: [nombres; apellidos; ci_nit; telefono; username; password].
     * El email es el del remitente (nadie puede registrar el correo de otro) y el rol siempre es Cliente.
     */
    public String registrar(Sesion s, String remitente, List<String> p) throws SQLException {
        if (s.rol != null) {
            throw new IllegalArgumentException("Su correo ya está registrado con el rol " + s.rol);
        }
        return existe(DUsuario.registrarCliente(texto(p, 0), texto(p, 1), opcional(p, 2), remitente.trim().toLowerCase(),
                opcional(p, 3), texto(p, 4), sha256(texto(p, 5))), "No existe el rol Cliente en la base de datos");
    }

    /** [id; nombres; apellidos; ci_nit; email; telefono; username] - el rol se cambia con ASIUSU. */
    public void modificar(List<String> p) throws SQLException {
        existe(DUsuario.modificar(entero(p, 0), texto(p, 1), texto(p, 2), opcional(p, 3), email(p, 4),
                opcional(p, 5), texto(p, 6)), "El usuario no existe");
    }

    /** [usuario_id; rol_id] */
    public void asignarRol(Sesion s, List<String> p) throws SQLException {
        int id = entero(p, 0);
        if (id == s.id) {
            throw new IllegalArgumentException("No puede cambiar su propio rol");
        }
        existe(DUsuario.asignarRol(id, entero(p, 1)), "El usuario no existe");
    }

    public void eliminar(Sesion s, List<String> p) throws SQLException {
        int id = entero(p, 0);
        if (id == s.id) {
            throw new IllegalArgumentException("No puede desactivar su propio usuario");
        }
        existe(DUsuario.desactivar(id), "El usuario no existe");
    }

    public List<String[]> listar() throws SQLException {
        return DUsuario.listar();
    }

    private static String email(List<String> p, int i) {
        String v = texto(p, i);
        if (!v.contains("@")) {
            throw new IllegalArgumentException("El email no es válido: " + v);
        }
        return v.toLowerCase();
    }

    private static String sha256(String texto) {
        try {
            StringBuilder hex = new StringBuilder();
            for (byte b : MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8))) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
