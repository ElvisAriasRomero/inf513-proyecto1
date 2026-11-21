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

    /**
     * El cliente pide un servicio por su cuenta (virtual): [servicio_id; cantidad (opcional, 1)].
     * Crea su cotización SIEMPRE al contado y YA APROBADA, con su única cuota lista para pagar con QR
     * (las cuotas y el efectivo son solo presenciales, con el Director o el Encargado).
     * No se le agregan ni quitan servicios: para otro pedido, otra cotización. Devuelve {id, total, cuota_id}.
     */
    public String[] solicitar(Sesion s, List<String> p) throws SQLException {
        if (!s.esCliente()) {
            throw new IllegalArgumentException("SOLCOT es para clientes. Para cotizar a un cliente use INSCOT.");
        }
        int servicio = entero(p, 0);
        int cantidad = opcional(p, 1) == null ? 1 : entero(p, 1);
        if (cantidad < 1) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        String id = existe(DCotizacion.guardar(s.id, s.id, "contado", 1), "No se pudo crear la cotización");
        int cot = Integer.parseInt(id);
        if (negociodato.dato.DDetalle.agregar(cot, servicio, cantidad) == 0) {
            DCotizacion.eliminar(cot);
            throw new IllegalArgumentException("No existe el servicio " + servicio + ". Vea los servicios con LISSER[\"*\"].");
        }
        DCotizacion.recalcularTotal(cot);
        DCotizacion.aprobar(cot); // sin paso de aprobación: queda aprobada con 1 cuota por el total
        String cuota = negociodato.dato.DCuota.listar(cot).get(0)[0];
        return new String[]{id, new java.math.BigDecimal(DCotizacion.ver(cot)[4]).stripTrailingZeros().toPlainString(), cuota};
    }

    /** [id; tipo_pago; nro_cuotas] */
    public void modificar(List<String> p) throws SQLException {
        String tipo = opcion(p, 1, "contado", "cuotas");
        existe(DCotizacion.modificar(entero(p, 0), tipo, nroCuotas(tipo, p, 2)),
                "La cotización no existe o ya no está pendiente");
    }

    public void eliminar(List<String> p) throws SQLException {
        existe(DCotizacion.eliminar(entero(p, 0)),
                "La cotización no existe, ya tiene pagos o un QR generado, o ya fue pagada/rechazada");
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
