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
 * Cliente de PagoFácil (Tigo Money), igual al PagoFacilService de taller-web:
 * 1. POST /login con tcTokenService y tcTokenSecret → accessToken
 * 2. POST /generate-qr con el token y tcCommerceId → qrBase64 y transactionId
 * 3. Cuando el cliente paga, PagoFácil avisa al callbackUrl (ver WebhookPagoFacil).
 */
public class PagoFacil {

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    /** Devuelve {qrBase64, transactionId}. */
    public static String[] generarQr(String pedido, BigDecimal monto, String nombre, String ci, String telefono,
            String email, String clienteId, String detalle) throws IOException, InterruptedException {
        if (PAGOFACIL_TOKEN_SERVICE.isEmpty() || PAGOFACIL_TOKEN_SECRET.isEmpty() || PAGOFACIL_COMMERCE_ID.isEmpty()) {
            throw new IllegalArgumentException("PagoFácil no está configurado: faltan las credenciales en Constantes");
        }
        String token = login();
        String m = monto.toPlainString();
        String body = "{\"paymentMethod\":34,"              // 34 = QR
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

        String res = post("/generate-qr", body, "Authorization", "Bearer " + token, "tcCommerceId", PAGOFACIL_COMMERCE_ID);
        String qr = campo(res, "qrBase64");
        if (qr == null) {
            throw new IOException("PagoFácil no devolvió el QR: " + res);
        }
        return new String[]{qr, campo(res, "transactionId")};
    }

    private static String login() throws IOException, InterruptedException {
        String res = post("/login", "{}", "tcTokenService", PAGOFACIL_TOKEN_SERVICE, "tcTokenSecret", PAGOFACIL_TOKEN_SECRET);
        String token = campo(res, "accessToken");
        if (token == null || token.isEmpty()) {
            throw new IOException("El login de PagoFácil no devolvió accessToken. Revise las credenciales. Respuesta: " + res);
        }
        return token;
    }

    private static String post(String ruta, String body, String... headers) throws IOException, InterruptedException {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(PAGOFACIL_URL + ruta))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        for (int i = 0; i < headers.length; i += 2) {
            req.header(headers[i], headers[i + 1]);
        }
        HttpResponse<String> res = HTTP.send(req.build(), HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() >= 400) {
            throw new IOException("PagoFácil respondió HTTP " + res.statusCode() + ": " + res.body());
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
