package pagos;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static negociodato.utils.Constantes.*;

/**
 * Cliente de PagoFácil QR Master v2 (documento ServiciosPagoFacil_QrMaster_v1.1.0):
 * 1. POST /login                 (tcTokenService, tcTokenSecret) → accessToken, se reutiliza hasta que vence
 * 2. POST /list-enabled-services → paymentMethodId del QR (si no se configuró PAGOFACIL_METODO)
 * 3. POST /generate-qr           → qrBase64, transactionId, expirationDate
 * 4. POST /query-transaction     → paymentStatus (lo usa VerificadorPagos cada minuto)
 * 5. Callback a callbackUrl      → lo recibe WebhookPagoFacil
 */
public class PagoFacil {

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    private static String token;
    private static long tokenVence; // milisegundos

    public static boolean configurado() {
        return !PAGOFACIL_TOKEN_SERVICE.isEmpty() && !PAGOFACIL_TOKEN_SECRET.isEmpty();
    }

    /** Devuelve {qrBase64, transactionId, expirationDate}. */
    public static String[] generarQr(String pedido, BigDecimal monto, String nombre, String ci, String telefono,
            String email, String clienteId, String detalle) throws IOException, InterruptedException {
        if (!configurado()) {
            throw new IllegalArgumentException("PagoFácil no está configurado: faltan PAGOFACIL_TOKEN_SERVICE y PAGOFACIL_TOKEN_SECRET");
        }
        String m = monto.toPlainString();
        String body = "{\"paymentMethod\":" + metodoQr() + ","
                + "\"clientName\":" + json(nombre) + ","
                + "\"documentType\":1,"                     // 1 = CI
                + "\"documentId\":" + json(ci == null || ci.isEmpty() ? "0000000" : ci) + ","
                + "\"phoneNumber\":" + json(telefono == null || telefono.isEmpty() ? "00000000" : telefono) + ","
                + "\"email\":" + json(email) + ","
                + "\"paymentNumber\":" + json(pedido) + ","
                + "\"amount\":" + m + ","
                + "\"currency\":2,"                         // 2 = BOB
                + "\"clientCode\":" + json(clienteId) + ","
                + "\"callbackUrl\":" + json(PAGOFACIL_CALLBACK_URL) + ","
                + "\"orderDetail\":[{\"serial\":1,\"product\":" + json(detalle) + ",\"quantity\":1,"
                + "\"price\":" + m + ",\"discount\":0,\"total\":" + m + "}]}";

        String res = conToken("/generate-qr", body);
        String qr = campo(res, "qrBase64");
        if (qr == null || qr.isEmpty()) {
            throw new IOException("PagoFácil no devolvió el QR: " + res);
        }
        return new String[]{qr, campo(res, "transactionId"), campo(res, "expirationDate")};
    }

    /** paymentStatus de un pedido nuestro (companyTransactionId), o null si PagoFácil no lo informa. */
    public static String estado(String pedido) throws IOException, InterruptedException {
        return campo(conToken("/query-transaction", "{\"companyTransactionId\":" + json(pedido) + "}"), "paymentStatus");
    }

    /**
     * ponytail: el documento no publica la tabla de paymentStatus; se toma 2 = pagado, igual que el
     * "Estado" del callback en taller-web. Confirmarlo con GET /bibliography/paymentStatus y ajustar aquí.
     */
    public static boolean esPagado(String estado) {
        return estado != null && (estado.equals("2") || estado.equalsIgnoreCase("pagado")
                || estado.equalsIgnoreCase("completado") || estado.equalsIgnoreCase("paid"));
    }

    private static String metodoQr() throws IOException, InterruptedException {
        if (!PAGOFACIL_METODO.isEmpty()) {
            return PAGOFACIL_METODO;
        }
        String id = campo(conToken("/list-enabled-services", "{}"), "paymentMethodId");
        if (id == null || id.isEmpty()) {
            throw new IOException("El comercio no tiene métodos QR habilitados en PagoFácil");
        }
        return id;
    }

    /** POST autenticado; si PagoFácil rechaza el token (venció antes de tiempo), pide uno nuevo y reintenta una vez. */
    private static String conToken(String ruta, String body) throws IOException, InterruptedException {
        try {
            return post(ruta, body, "Authorization", "Bearer " + token());
        } catch (IOException e) {
            String m = String.valueOf(e.getMessage()).toLowerCase();
            if (!m.contains("http 401") && !m.contains("token")) {
                throw e;
            }
            token = null;
            return post(ruta, body, "Authorization", "Bearer " + token());
        }
    }

    private static synchronized String token() throws IOException, InterruptedException {
        if (token != null && System.currentTimeMillis() < tokenVence) {
            return token;
        }
        String res = post("/login", "{}", "tcTokenService", PAGOFACIL_TOKEN_SERVICE, "tcTokenSecret", PAGOFACIL_TOKEN_SECRET);
        String nuevo = campo(res, "accessToken");
        if (nuevo == null || nuevo.isEmpty()) {
            throw new IOException("El login de PagoFácil no devolvió accessToken. Revise las credenciales. Respuesta: " + res);
        }
        String minutos = campo(res, "expiresInMinutes");
        double vida = minutos == null ? 30 : Double.parseDouble(minutos);
        tokenVence = System.currentTimeMillis() + (long) ((vida - 1) * 60_000); // 1 minuto de margen
        token = nuevo;
        return token;
    }

    private static String post(String ruta, String body, String... headers) throws IOException, InterruptedException {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(PAGOFACIL_URL + ruta))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Response-Language", "es")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        for (int i = 0; i < headers.length; i += 2) {
            req.header(headers[i], headers[i + 1]);
        }
        HttpResponse<String> res = HTTP.send(req.build(), HttpResponse.BodyHandlers.ofString());
        String error = campo(res.body(), "error");
        if (res.statusCode() >= 400 || (error != null && !error.equals("0"))) {
            String mensaje = campo(res.body(), "message");
            throw new IOException("PagoFácil (" + ruta + "): " + (mensaje != null ? mensaje : "HTTP " + res.statusCode() + " " + res.body()));
        }
        return res.body();
    }

    /** Valor de una clave del JSON de respuesta ("clave":"valor" o "clave":123), o null. */
    public static String campo(String json, String clave) {
        Matcher m = Pattern.compile("\"" + clave + "\"\\s*:\\s*\"?([^\",}]*)").matcher(json);
        return m.find() ? m.group(1).replace("\\/", "/").trim() : null;
    }

    private static String json(String s) {
        return "\"" + (s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"")) + "\"";
    }
}
