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
        exigirSinPagoEnCurso(id);

        String pedido = "CUOTA-" + id + "-" + System.currentTimeMillis();
        BigDecimal monto = new BigDecimal(q[1]);
        BigDecimal cobrado = PagoFacil.montoCobrado(monto);
        String[] qr = PagoFacil.generarQr(pedido, cobrado, q[4], q[5], q[6], q[7], q[3],
                "Cuota " + q[2] + " de la cotización " + q[0]);
        DCuota.guardarPedido(id, pedido);

        String mensaje = "Cuota " + q[2] + " de la cotización " + q[0] + ": " + q[1] + " Bs"
                + (cobrado.compareTo(monto) != 0 ? " (el banco cobra " + cobrado.toPlainString() + " Bs"
                        + (Constantes.PAGOFACIL_MODO_PRUEBA ? ", modo prueba)" : ")") : "")
                + ". Escanee el QR con la app de su banco"
                + (qr[2] == null || qr[2].isEmpty() ? "" : " antes de " + qr[2])
                + ". Al pagar le llegará la confirmación por correo. Para revisar el estado envíe VERCUO[\"" + id + "\"].";
        return new String[]{mensaje, qr[0]};
    }

    /**
     * "Ya pagué": [cuota_id] → pregunta a PagoFácil por el QR de la cuota y, si está Pagado, la confirma.
     * Devuelve el mensaje con lo que dijo PagoFácil.
     */
    public String verificar(Sesion s, List<String> p) throws SQLException, IOException, InterruptedException {
        int id = entero(p, 0);
        String[] c = DCuota.estadoDe(id); // {cotizacion_id, estado}
        if (c == null) {
            throw new IllegalArgumentException("No existe la cuota " + id);
        }
        NCotizacion.verificarAcceso(s, Integer.parseInt(c[0]));
        if (c[1].equals("pagada")) {
            return "La cuota " + id + " ya está pagada.";
        }
        String pedido = DCuota.pedidoDe(id);
        if (pedido == null) {
            return "La cuota " + id + " no tiene un QR generado. Pídalo con QRPCUO[\"" + id + "\"].";
        }
        return verificarPedido(pedido)[1];
    }

    /**
     * Consulta el pedido en PagoFácil (lo usan el verificador de cada 10 s, el callback y VERCUO):
     * Pagado → confirma la cuota con el n° de transacción y la fecha real de PagoFácil;
     * Anulado → libera la cuota para pedir otro QR.
     * Devuelve {estado, mensaje, email del cliente (solo si se acaba de confirmar, para avisarle) }.
     */
    public static String[] verificarPedido(String pedido) throws SQLException, IOException, InterruptedException {
        String[] r = PagoFacil.consultar(pedido); // {paymentStatus, descripción, pagofacilTransactionId, paymentDate}
        if (PagoFacil.esPagado(r[0])) {
            String[] q = DCuota.pagarPorPedido(pedido, "PagoFácil Trn " + r[2], r[3]); // {id, cotizacion_id, nro_cuota, monto, email}
            if (q == null) {
                return new String[]{r[0], "La cuota ya estaba registrada como pagada.", null};
            }
            return new String[]{r[0], "Recibimos su pago QR de la cuota " + q[2] + " (" + q[3] + " Bs). "
                + cerrar(Integer.parseInt(q[1])), q[4]};
        }
        if (PagoFacil.ANULADO.equals(r[0])) {
            DCuota.liberarPedido(pedido);
            return new String[]{r[0], "PagoFácil anuló este QR (venció sin pagarse). Pida uno nuevo con QRPCUO.", null};
        }
        if (PagoFacil.REVISION.equals(r[0])) {
            return new String[]{r[0], "El banco ya cobró y PagoFácil tiene el pago en revisión. "
                + "Se confirmará solo cuando lo apruebe (en pruebas puede tardar hasta 2 días).", null};
        }
        return new String[]{r[0], "PagoFácil todavía no registra el pago (estado: " + r[1] + ").", null};
    }

    /**
     * No se genera otro QR si el anterior ya fue cobrado o sigue vigente: si el cliente pagara el viejo,
     * ese pago se perdería. Si PagoFácil ya lo tiene Pagado, se confirma ahí mismo.
     */
    private static void exigirSinPagoEnCurso(int cuotaId) throws SQLException, IOException, InterruptedException {
        String previo = DCuota.pedidoDe(cuotaId);
        if (previo == null || !PagoFacil.configurado()) {
            return;
        }
        String[] v = verificarPedido(previo);
        if (PagoFacil.esPagado(v[0]) || PagoFacil.REVISION.equals(v[0])) {
            throw new IllegalArgumentException(v[1]);
        }
        long minutos = (System.currentTimeMillis() - pagos.VerificadorPagos.hora(previo)) / 60_000;
        if (!PagoFacil.ANULADO.equals(v[0]) && minutos < 10) {
            throw new IllegalArgumentException("Ya tiene un QR vigente para esta cuota (generado hace " + minutos
                    + " min). Páguelo o espere unos minutos a que venza para pedir otro.");
        }
    }

    /** Si ya no queda nada por pagar, la cotización pasa a pagada. */
    private static String cerrar(int cotizacion) throws SQLException {
        if (DCotizacion.marcarPagadaSiCompleta(cotizacion) > 0) {
            return "Pago registrado. La cotización " + cotizacion + " quedó PAGADA por completo.";
        }
        return "Pago registrado. Saldo pendiente de la cotización " + cotizacion + ": " + DCuota.saldo(cotizacion) + " Bs";
    }
}
