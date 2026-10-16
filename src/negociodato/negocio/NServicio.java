package negociodato.negocio;

import java.sql.SQLException;
import java.util.List;
import negociodato.dato.DServicio;
import static negociodato.utils.Params.*;

/** CU3 - Oferta comercial (servicios con precio). */
public class NServicio {

    /** [nombre; descripcion; precio] */
    public String guardar(List<String> p) throws SQLException {
        return DServicio.guardar(texto(p, 0), opcional(p, 1), decimal(p, 2));
    }

    /** [id; nombre; descripcion; precio] */
    public void modificar(List<String> p) throws SQLException {
        existe(DServicio.modificar(entero(p, 0), texto(p, 1), opcional(p, 2), decimal(p, 3)), "El servicio no existe");
    }

    public void eliminar(List<String> p) throws SQLException {
        existe(DServicio.desactivar(entero(p, 0)), "El servicio no existe");
    }

    public List<String[]> listar() throws SQLException {
        return DServicio.listar();
    }
}
