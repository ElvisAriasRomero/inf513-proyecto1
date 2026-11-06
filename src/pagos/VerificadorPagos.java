package pagos;

import java.util.logging.Level;
import java.util.logging.Logger;
import negociodato.dato.DCuota;
import negociodato.utils.Constantes;

/**
 * "Consulta Manual" del documento de PagoFácil: cada minuto pregunta a /query-transaction por los QR
 * generados que siguen sin pagar. Así el pago se registra solo aunque el callback no llegue
 * (por ejemplo, sin ngrok o desde una red sin acceso público).
 */
public class VerificadorPagos implements Runnable {

    private static final long UN_DIA = 24 * 3_600_000L;

    @Override
    public void run() {
        if (!PagoFacil.configurado()) {
            System.out.println("PagoFácil sin credenciales: no se consultarán pagos QR.");
            return;
        }
        while (true) {
            try {
                for (String pedido : DCuota.pedidosPendientes()) {
                    if (System.currentTimeMillis() - hora(pedido) > UN_DIA) {
                        continue; // QR viejo: ya no se consulta (el cliente puede pedir otro con QRPCUO)
                    }
                    if (PagoFacil.esPagado(PagoFacil.estado(pedido))) {
                        WebhookPagoFacil.confirmar(pedido, "PagoFácil " + pedido);
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(VerificadorPagos.class.getName()).log(Level.WARNING, "Consulta de pagos QR", ex);
            }
            try {
                Thread.sleep(Constantes.PAGOFACIL_SEGUNDOS_CONSULTA * 1000L);
            } catch (InterruptedException ex) {
                return;
            }
        }
    }

    /** El pedido es CUOTA-<id>-<milisegundos en que se generó el QR>. */
    static long hora(String pedido) {
        try {
            return Long.parseLong(pedido.substring(pedido.lastIndexOf('-') + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
