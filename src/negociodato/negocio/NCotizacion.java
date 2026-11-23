package negociodato.negocio;

import java.sql.SQLException;
import java.util.List;
import negociodato.dato.DCotizacion;
import static negociodato.utils.Params.*;

/** CU3 - Cotizaciones. Se crean ya aprobadas con sus cuotas (INSCOT presencial, SOLCOT del cliente) → pagada. */
public class NCotizacion {

    /**
     * Presencial (Director o Encargado), sin paso de aprobación: si el cliente no quiere, no se crea nada.
     * [cliente_id; contado | cuotas; nro_cuotas (solo en cuotas); servicio_id; cantidad; ...más pares]
     * Queda aprobada con sus cuotas listas para cobrar (efectivo o QR). Devuelve {id, total, primera cuota_id}.
     */
    public String[] guardar(Sesion s, List<String> p) throws SQLException {
        String tipo = opcion(p, 1, "contado", "cuotas");
        int desde = tipo.equals("contado") ? 2 : 3;
        if (p.size() <= desde || (p.size() - desde) % 2 != 0) {
            throw new IllegalArgumentException("Ponga al menos un servicio, en pares: \"servicio_id\",\"cantidad\""
                    + (tipo.equals("contado") ? " (al contado no se pone nro_cuotas)" : ""));
        }
        String id = existe(DCotizacion.guardar(entero(p, 0), s.id, tipo, nroCuotas(tipo, p, 2)),
                "El cliente no existe o no tiene el rol Cliente");
        return completar(Integer.parseInt(id), p, desde);
    }

    /**
     * El cliente pide un servicio por su cuenta (virtual): [servicio_id; cantidad (opcional, 1)].
     * Siempre al contado y ya aprobada, con su única cuota lista para pagar con QR
     * (las cuotas y el efectivo son solo presenciales). Para otro pedido, otra cotización. Devuelve {id, total, cuota_id}.
     */
    public String[] solicitar(Sesion s, List<String> p) throws SQLException {
        if (!s.esCliente()) {
            throw new IllegalArgumentException("SOLCOT es para clientes. Para cotizar a un cliente use INSCOT.");
        }
        List<String> par = List.of(texto(p, 0), opcional(p, 1) == null ? "1" : texto(p, 1));
        String id = existe(DCotizacion.guardar(s.id, s.id, "contado", 1), "No se pudo crear la cotización");
        return completar(Integer.parseInt(id), par, 0);
    }

    /** Agrega los pares servicio,cantidad desde p[desde] y la aprueba (crea las cuotas). Si algo falla, la borra. */
    private static String[] completar(int cot, List<String> p, int desde) throws SQLException {
        for (int i = desde; i < p.size(); i += 2) {
            int servicio = entero(p, i);
            int cantidad = entero(p, i + 1);
            if (cantidad < 1 || negociodato.dato.DDetalle.agregar(cot, servicio, cantidad) == 0) {
                DCotizacion.eliminar(cot);
                throw new IllegalArgumentException(cantidad < 1 ? "La cantidad debe ser mayor a 0"
                        : "No existe el servicio " + servicio + ". Vea los servicios con LISSER[\"*\"].");
            }
        }
        DCotizacion.recalcularTotal(cot);
        DCotizacion.aprobar(cot);
        String cuota = negociodato.dato.DCuota.listar(cot).get(0)[0];
        String total = new java.math.BigDecimal(DCotizacion.ver(cot)[4]).stripTrailingZeros().toPlainString();
        return new String[]{String.valueOf(cot), total, cuota};
    }

    public void eliminar(List<String> p) throws SQLException {
        existe(DCotizacion.eliminar(entero(p, 0)),
                "La cotización no existe, ya tiene pagos o un QR generado, o ya fue pagada");
    }

    /** El cliente solo ve las suyas. */
    public List<String[]> listar(Sesion s) throws SQLException {
        return s.esCliente() ? DCotizacion.listarDeCliente(s.id) : DCotizacion.listar();
    }

    public String[] ver(Sesion s, int id) throws SQLException {
        verificarAcceso(s, id);
        return DCotizacion.ver(id);
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
