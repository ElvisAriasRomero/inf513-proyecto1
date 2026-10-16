package scraping;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scraping con el HttpClient de Java (sin librerías extra).
 * - leerMetricas: lee los contadores públicos que la red incrusta en el HTML de una publicación.
 * - buscarPaginas: busca "palabra site:red.com" en DuckDuckGo y devuelve las páginas encontradas.
 *
 * ponytail: depende del HTML de cada red; si cambian sus claves, actualizar los arreglos de abajo.
 * Para producción, reemplazar por las APIs oficiales usando el token de cuenta_red_social.
 */
public class Scraper {

    private static final String AGENTE = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/124.0 Safari/537.36";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    // Nombres que usan TikTok, Instagram, Facebook y YouTube en el JSON incrustado de la página
    private static final String[] VISTAS = {"playCount", "play_count", "video_view_count", "viewCount", "view_count"};
    private static final String[] REACCIONES = {"diggCount", "like_count", "likeCount", "reaction_count", "edge_liked_by"};
    private static final String[] COMENTARIOS = {"commentCount", "comment_count", "edge_media_to_comment"};
    private static final String[] COMPARTIDOS = {"shareCount", "share_count", "reshare_count"};

    private static final Pattern RESULTADO = Pattern.compile(
            "class=\"result__a\" href=\"([^\"]+)\"[^>]*>(.*?)</a>(?:.*?class=\"result__snippet\"[^>]*>(.*?)</a>)?",
            Pattern.DOTALL);
    private static final Pattern SEGUIDORES = Pattern.compile(
            "([\\d][\\d.,]*)\\s*(k|m|mil)?\\s*(seguidores|followers|suscriptores|subscribers|me gusta|likes)",
            Pattern.CASE_INSENSITIVE);

    public static String descargar(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", AGENTE)
                .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
                .build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() >= 400) {
            throw new IOException("HTTP " + res.statusCode() + " en " + url);
        }
        return res.body();
    }

    /** {vistas, reacciones, comentarios, compartidos} o null si la página no muestra ninguno. */
    public static long[] leerMetricas(String html) {
        long[] m = {buscar(html, VISTAS), buscar(html, REACCIONES), buscar(html, COMENTARIOS), buscar(html, COMPARTIDOS)};
        if (m[0] < 0 && m[1] < 0 && m[2] < 0 && m[3] < 0) {
            return null;
        }
        for (int i = 0; i < m.length; i++) {
            m[i] = Math.max(m[i], 0);
        }
        return m;
    }

    /** Primer número asociado a alguna de las claves: "clave":123, "clave":"123" o "clave":{"count":123}. */
    private static long buscar(String html, String[] claves) {
        for (String clave : claves) {
            Matcher m = Pattern.compile("\"" + clave + "\"\\s*:\\s*(?:\\{\\s*\"count\"\\s*:\\s*)?\"?(\\d+)").matcher(html);
            if (m.find()) {
                return Long.parseLong(m.group(1));
            }
        }
        return -1;
    }

    /** {nombre, url normalizada, seguidores} de cada resultado que pertenece a ese dominio. */
    public static List<String[]> buscarPaginas(String dominio, String palabra) throws IOException, InterruptedException {
        String q = URLEncoder.encode(palabra + " site:" + dominio, StandardCharsets.UTF_8);
        return leerResultados(descargar("https://html.duckduckgo.com/html/?q=" + q), dominio);
    }

    public static List<String[]> leerResultados(String html, String dominio) {
        List<String[]> paginas = new ArrayList<>();
        Matcher m = RESULTADO.matcher(html);
        while (m.find()) {
            String url = enlaceReal(m.group(1));
            String norm = normalizar(url);
            if (!norm.startsWith(dominio + "/") || norm.length() <= dominio.length() + 1) {
                continue;
            }
            String titulo = limpiar(m.group(2));
            String snippet = m.group(3) == null ? "" : limpiar(m.group(3));
            paginas.add(new String[]{titulo, "https://" + norm, String.valueOf(seguidores(titulo + " " + snippet))});
        }
        return paginas;
    }

    /** DuckDuckGo envuelve los enlaces: //duckduckgo.com/l/?uddg=<url codificada>&rut=... */
    private static String enlaceReal(String href) {
        href = href.replace("&amp;", "&");
        int i = href.indexOf("uddg=");
        if (i >= 0) {
            int fin = href.indexOf('&', i);
            return URLDecoder.decode(href.substring(i + 5, fin < 0 ? href.length() : fin), StandardCharsets.UTF_8);
        }
        return href.startsWith("//") ? "https:" + href : href;
    }

    /** Para comparar enlaces: sin http(s), www., m., parámetros ni barra final; en minúsculas. */
    public static String normalizar(String url) {
        String u = url.trim().toLowerCase().replaceFirst("^https?://", "").replaceFirst("^(www\\.|m\\.|web\\.)", "");
        u = u.replaceAll("[?#].*$", "");
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }

    /** "12,5 mil seguidores" → 12500, "3.4K followers" → 3400, "1.234 seguidores" → 1234. 0 si no aparece. */
    public static long seguidores(String texto) {
        Matcher m = SEGUIDORES.matcher(texto);
        if (!m.find()) {
            return 0;
        }
        String numero = m.group(1).replaceAll("[.,]+$", "");
        String sufijo = m.group(2) == null ? "" : m.group(2).toLowerCase();
        if (sufijo.isEmpty()) {
            return Long.parseLong(numero.replaceAll("[.,]", "")); // sin sufijo los separadores son de miles
        }
        // con sufijo, el último separador es el decimal: "12,5" y "3.4" → 12.5 y 3.4
        String n = numero.replace(',', '.');
        int ultimo = n.lastIndexOf('.');
        if (ultimo >= 0) {
            n = n.substring(0, ultimo).replace(".", "") + n.substring(ultimo);
        }
        return Math.round(Double.parseDouble(n) * (sufijo.equals("m") ? 1_000_000 : 1_000));
    }

    private static String limpiar(String html) {
        return html.replaceAll("<[^>]+>", "").replace("&amp;", "&").replace("&#x27;", "'").replace("&quot;", "\"").trim();
    }
}
