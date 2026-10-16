package negociodato.negocio;

import java.sql.SQLException;
import java.util.List;
import negociodato.dato.DRol;
import static negociodato.utils.Params.*;

/** CU1 - Roles. */
public class NRol {

    public String guardar(List<String> p) throws SQLException {
        return DRol.guardar(texto(p, 0));
    }

    public void modificar(List<String> p) throws SQLException {
        existe(DRol.modificar(entero(p, 0), texto(p, 1)), "El rol no existe");
    }

    public void eliminar(List<String> p) throws SQLException {
        existe(DRol.eliminar(entero(p, 0)), "El rol no existe");
    }

    public List<String[]> listar() throws SQLException {
        return DRol.listar();
    }
}
