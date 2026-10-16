package negociodato.negocio;

import java.sql.SQLException;
import java.util.List;
import negociodato.dato.DCuenta;
import static negociodato.utils.Params.*;

/** CU2 - Cuentas oficiales de redes sociales. El encargado es quien envía el correo. */
public class NCuenta {

    public static final String[] REDES = {"Facebook", "Instagram", "TikTok", "YouTube"};

    /** [red_social; nombre_cuenta; url; token] (token opcional: "-") */
    public String guardar(Sesion s, List<String> p) throws SQLException {
        return DCuenta.guardar(s.id, opcion(p, 0, REDES), texto(p, 1), url(p, 2), opcional(p, 3));
    }

    /** [id; red_social; nombre_cuenta; url; token] */
    public void modificar(List<String> p) throws SQLException {
        existe(DCuenta.modificar(entero(p, 0), opcion(p, 1, REDES), texto(p, 2), url(p, 3), opcional(p, 4)),
                "La cuenta no existe");
    }

    public void eliminar(List<String> p) throws SQLException {
        existe(DCuenta.desactivar(entero(p, 0)), "La cuenta no existe");
    }

    public List<String[]> listar() throws SQLException {
        return DCuenta.listar();
    }
}
