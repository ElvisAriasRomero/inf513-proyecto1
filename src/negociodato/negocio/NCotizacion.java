package negociodato.negocio;

import java.sql.SQLException;
import java.util.List;
import negociodato.dato.DCotizacion;
import static negociodato.utils.Params.*;

/** CU3 - Cotizaciones. Flujo: pendiente → aprobada (crea cuotas) → pagada, o rechazada. */
public class NCotizacion {

    /** [cliente_id; tipo_pago; nro_cuotas] (al contado no hace falta nro_cuotas) */
    public String guardar(Sesion s, List<String> p) throws SQLException {
        String tipo = opcion(p, 1, "contado", "cuotas");
        return existe(DCotizacion.guardar(entero(p, 0), s.id, tipo, nroCuotas(tipo, p, 2)),
                "El cliente no existe o no tiene el rol Cliente");
    }

    /** [id; tipo_pago; nro_cuotas] */
    public void modificar(List<String> p) throws SQLException {
        String tipo = opcion(p, 1, "contado", "cuotas");
        existe(DCotizacion.modificar(entero(p, 0), tipo, nroCuotas(tipo, p, 2)),
                "La cotización no existe o ya no está pendiente");
    }

    public void eliminar(List<String> p) throws SQLException {
        existe(DCotizacion.eliminar(entero(p, 0)), "La cotización no existe o ya no está pendiente");
    }

    /** El cliente solo ve las suyas. */
    public List<String[]> listar(Sesion s) throws SQLException {
        return s.esCliente() ? DCotizacion.listarDeCliente(s.id) : DCotizacion.listar();
    }

    public String[] ver(Sesion s, int id) throws SQLException {
        verificarAcceso(s, id);
        return DCotizacion.ver(id);
    }

    /** Devuelve cuántas cuotas se crearon. */
    public int aprobar(Sesion s, List<String> p) throws SQLException {
        int id = entero(p, 0);
        verificarAcceso(s, id);
        int cuotas = DCotizacion.aprobar(id);
        existe(cuotas, "La cotización debe estar pendiente y tener al menos un servicio");
        return cuotas;
    }

    public void rechazar(Sesion s, List<String> p) throws SQLException {
        int id = entero(p, 0);
        verificarAcceso(s, id);
        existe(DCotizacion.rechazar(id), "La cotización ya no está pendiente");
    }

    /** Si quien pide es un cliente, la cotización tiene que ser suya. */
    public static void verificarAcceso(Sesion s, int cotizacionId) throws SQLException {
        String cliente = DCotizacion.clienteDe(cotizacionId);
        if (cliente == null || (s.esCliente() && !cliente.equals(String.valueOf(s.id)))) {
            throw new IllegalArgumentException("No existe la cotización " + cotizacionId);
        }
    }

    private static int nroCuotas(String tipo, List<String> p, int i) {
        if (tipo.equals("contado")) {
            return 1;
        }
        int n = entero(p, i);
        if (n < 2) {
            throw new IllegalArgumentException("En cuotas deben ser al menos 2");
        }
        return n;
    }
}
