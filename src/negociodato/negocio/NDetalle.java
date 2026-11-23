package negociodato.negocio;

import java.sql.SQLException;
import java.util.List;
import negociodato.dato.DDetalle;

/**
 * CU3 - Servicios de una cotización: solo se consultan. Se cargan al crearla (INSCOT o SOLCOT);
 * si hay un error se borra la cotización (ELICOT) y se crea otra.
 */
public class NDetalle {

    public List<String[]> listar(Sesion s, int cotizacionId) throws SQLException {
        NCotizacion.verificarAcceso(s, cotizacionId);
        return DDetalle.listar(cotizacionId);
    }
}
