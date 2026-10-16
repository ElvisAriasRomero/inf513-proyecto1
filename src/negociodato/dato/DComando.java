package negociodato.dato;

import java.sql.SQLException;
import java.util.List;

/** Tabla comando: la lista que se envía con "help". */
public class DComando extends Dato {

    public static final String[] HEADERS = {"CASO DE USO", "COMANDO", "PARAMETROS", "EJEMPLO", "QUIEN"};

    public static List<String[]> listar() throws SQLException {
        return consultar("SELECT caso_uso, accion, parametro, ejemplo, roles FROM comando ORDER BY id");
    }
}
