package negociodato.negocio;

import java.sql.SQLException;
import negociodato.dato.DUsuario;

/**
 * Quién envió el correo. Se busca su email en la tabla usuario.
 * Si no está registrado (o está inactivo), rol es null y solo puede usar "help".
 */
public class Sesion {

    public final int id;
    public final String rol;

    private Sesion(int id, String rol) {
        this.id = id;
        this.rol = rol;
    }

    public static Sesion de(String email) throws SQLException {
        String[] u = DUsuario.sesion(email);
        return u == null ? new Sesion(0, null) : new Sesion(Integer.parseInt(u[0]), u[1]);
    }

    public boolean esCliente() {
        return "Cliente".equals(rol);
    }
}
