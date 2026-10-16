package negociodato.negocio;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import negociodato.dato.DCotizacion;
import negociodato.dato.DCuota;
import negociodato.utils.Constantes;
import pagos.PagoFacil;
import static negociodato.utils.Params.*;

/**
 * CU3 - Pagos al contado o en cuotas, solo en efectivo o QR.
 * QR automático con PagoFácil (cuota qr) o QR registrado a mano con su comprobante (cuota pagar).
 */
public class NCuota {

    public List<String[]> listar(Sesion s, int cotizacionId) throws SQLException {
        NCotizacion.verificarAcceso(s, cotizacionId);
        DCuota.marcarVencidas();
        return DCuota.listar(cotizacionId);
    }

    public List<String[]> porCobrar() throws SQLException {
        DCuota.marcarVencidas();
        return DCuota.porCobrar();
    }

    /**
     * Efectivo: [id; efectivo]
     * QR:       [id; QR; nro_transaccion; enlace_del_comprobante]
     * Devuelve el mensaje con el saldo que queda.
     */
    public String pagar(Sesion s, List<String> p) throws SQLException {
        String metodo = opcion(p, 1, "efectivo", "QR");
        String nro = null;
        String comprobante = null;
        if (metodo.equals("QR")) {
            nro = texto(p, 2);
            comprobante = url(p, 3);
        }
        int cotizacion = Integer.parseInt(existe(DCuota.pagar(entero(p, 0), metodo, nro, comprobante, s.id),
                "La cuota no existe o ya está pagada"));

        return cerrar(cotizacion);
    }

    /**
     * [cuota_id] → genera el QR de PagoFácil por el monto de la cuota.
     * Devuelve {mensaje, qrBase64}. Cuando el cliente paga, el webhook marca la cuota sola.
     */
    public String[] generarQr(Sesion s, List<String> p) throws SQLException, IOException, InterruptedException {
        int id = entero(p, 0);
        String[] q = DCuota.paraQr(id); // {cotizacion_id, monto, nro_cuota, cliente_id, cliente, ci_nit, telefono, email}
        if (q == null) {
            throw new IllegalArgumentException("La cuota no existe o ya está pagada");
        }
        NCotizacion.verificarAcceso(s, Integer.parseInt(q[0]));

        String pedido = "CUOTA-" + id + "-" + System.currentTimeMillis();
        BigDecimal monto = Constantes.PAGOFACIL_MODO_PRUEBA ? new BigDecimal("0.01") : new BigDecimal(q[1]);
        String[] qr = PagoFacil.generarQr(pedido, monto, q[4], q[5], q[6], q[7], q[3],
                "Cuota " + q[2] + " de la cotización " + q[0]);
        DCuota.guardarPedido(id, pedido);

        String mensaje = "Cuota " + q[2] + " de la cotización " + q[0] + ": " + q[1] + " Bs"
                + (Constantes.PAGOFACIL_MODO_PRUEBA ? " (modo prueba: el banco cobra 0.01 Bs)" : "")
                + ". Escanee el QR con la app de su banco. Al pagar le llegará la confirmación por correo.";
        return new String[]{mensaje, qr[0]};
    }

    /** Lo llama el webhook. Devuelve {email del cliente, mensaje} o null si ese pedido no está pendiente. */
    public static String[] confirmarPagoFacil(String pedido, String transaccion) throws SQLException {
        String[] q = DCuota.pagarPorPedido(pedido, "PagoFácil " + transaccion); // {id, cotizacion_id, nro_cuota, monto, email}
        if (q == null) {
            return null;
        }
        return new String[]{q[4], "Recibimos su pago QR de la cuota " + q[2] + " (" + q[3] + " Bs). "
            + cerrar(Integer.parseInt(q[1]))};
    }

    /** Si ya no queda nada por pagar, la cotización pasa a pagada. */
    private static String cerrar(int cotizacion) throws SQLException {
        if (DCotizacion.marcarPagadaSiCompleta(cotizacion) > 0) {
            return "Pago registrado. La cotización " + cotizacion + " quedó PAGADA por completo.";
        }
        return "Pago registrado. Saldo pendiente de la cotización " + cotizacion + ": " + DCuota.saldo(cotizacion) + " Bs";
    }
}
