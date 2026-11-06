package pagos;

import coneccion.comunicacion.SendEmailThread;
import coneccion.utils.Email;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import main.HtmlBuilder;
import negociodato.negocio.NCuota;
import negociodato.utils.Constantes;

/**
 * Servidor HTTP mínimo (incluido en Java) que recibe el aviso de PagoFácil cuando el cliente paga el QR.
 * Igual que el webhook de taller-web: Estado/TransactionStatus = 2 significa pagado,
 * y PedidoID/PaymentNumber es el número de pedido que enviamos al generar el QR (CUOTA-id-hora).
 *
 * Para recibir avisos desde internet hay que exponer el puerto con ngrok y poner esa URL en
 * Constantes.PAGOFACIL_CALLBACK_URL.
 */
public class WebhookPagoFacil {

    public static final String RUTA = "/pagofacil/webhook";

    public static void iniciar() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(Constantes.PAGOFACIL_PUERTO), 0);
            server.createContext(RUTA, ex -> {
                int codigo = 200;
                // Respuesta que pide el documento de PagoFácil para dar por recibida la notificación
                String respuesta = "{\"error\":0,\"status\":1,\"message\":\"Notificacion recibida correctamente\",\"values\":true}";
                try {
                    String query = ex.getRequestURI().getRawQuery();
                    procesar(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)
                            + "&" + (query == null ? "" : query));
                } catch (Exception e) {
                    Logger.getLogger(WebhookPagoFacil.class.getName()).log(Level.SEVERE, "Webhook PagoFácil", e);
                    codigo = 500;
                    respuesta = "{\"error\":1,\"status\":0,\"message\":\"Error interno\",\"values\":false}";
                }
                byte[] out = respuesta.getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().add("Content-Type", "application/json");
                ex.sendResponseHeaders(codigo, out.length);
                ex.getResponseBody().write(out);
                ex.close();
            });
            server.start();
            System.out.println("Webhook PagoFácil escuchando en el puerto " + Constantes.PAGOFACIL_PUERTO + RUTA);
        } catch (IOException e) {
            System.err.println("No se pudo abrir el webhook de PagoFácil en el puerto " + Constantes.PAGOFACIL_PUERTO
                    + ": " + e.getMessage() + ". Los pagos QR se pueden registrar a mano con cuota pagar.");
        }
    }

    private static void procesar(String datos) throws Exception {
        System.out.println("Webhook PagoFácil recibido: " + datos);
        String pedido = dato(datos, "PedidoID", "PaymentNumber");
        if (pedido == null) {
            return;
        }
        // El aviso no viene firmado: se confirma con PagoFácil (/query-transaction) para que nadie pueda inventarlo.
        String estado = PagoFacil.configurado() ? PagoFacil.estado(pedido) : dato(datos, "Estado", "TransactionStatus");
        if (PagoFacil.esPagado(estado)) {
            String metodo = dato(datos, "MetodoPago");
            confirmar(pedido, metodo == null ? pedido : metodo + " " + pedido);
        }
    }

    /** Marca la cuota de ese pedido como pagada y avisa al cliente por correo. No hace nada si ya estaba pagada. */
    static void confirmar(String pedido, String comprobante) throws Exception {
        String[] aviso = NCuota.confirmarPagoFacil(pedido, comprobante); // {email, mensaje}
        if (aviso != null) {
            System.out.println("PagoFácil: pago confirmado del pedido " + pedido);
            Email email = new Email(aviso[0], Email.asunto(), HtmlBuilder.texto("Pago QR recibido", aviso[1]));
            new Thread(new SendEmailThread(email), "Send email Thread").start();
        }
    }

    /** Primer campo encontrado, venga en JSON ("Estado":2) o como formulario (Estado=2). */
    public static String dato(String datos, String... claves) {
        for (String clave : claves) {
            Matcher json = Pattern.compile("\"" + clave + "\"\\s*:\\s*\"?([^\",}]*)").matcher(datos);
            if (json.find()) {
                return json.group(1).trim();
            }
            Matcher form = Pattern.compile("(?:^|&)" + clave + "=([^&]*)").matcher(datos);
            if (form.find()) {
                return URLDecoder.decode(form.group(1), StandardCharsets.UTF_8).trim();
            }
        }
        return null;
    }
}
