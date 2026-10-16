package main;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Arma el HTML de las respuestas por correo.
 */
public class HtmlBuilder {

    private static final String TITULO = "Sistema de Publicidad Universitaria";

    private static final String ESTILO = "<style>"
            + "body{font-family:Arial,Helvetica,sans-serif;color:#222;margin:0;padding:16px}"
            + "h1{color:#0b3d91;text-align:center;margin:0 0 8px}"
            + "h2,h3{text-align:center}"
            + "table{margin:20px auto;border-collapse:collapse}"
            + "caption{font-size:20px;font-weight:bold;padding-bottom:10px;color:#0b3d91}"
            + "th{background:#0b3d91;color:#fff}"
            + "th,td{border:1px solid #9aa7bd;padding:6px 14px;text-align:left}"
            + "tr:nth-child(even) td{background:#f2f5fb}"
            + "img{display:block;margin:20px auto;max-width:100%}"
            + ".pie{text-align:center;color:#888;font-size:12px;margin-top:30px}"
            + "</style>";

    /** Mensaje simple: un título y líneas debajo. */
    public static String texto(String titulo, String... lineas) {
        StringBuilder sb = new StringBuilder("<h2>").append(escapar(titulo)).append("</h2>");
        for (String linea : lineas) {
            sb.append("<h3>").append(escapar(linea)).append("</h3>");
        }
        return pagina(sb.toString());
    }

    /** Mensaje con el QR de pago adjunto (la imagen viaja en el correo con Content-ID "imagen"). */
    public static String qr(String titulo, String mensaje) {
        return pagina("<h2>" + escapar(titulo) + "</h2><h3>" + escapar(mensaje) + "</h3>"
                + "<img src=\"cid:imagen\" width=\"300\" height=\"300\" alt=\"QR de pago\">");
    }

    public static String tabla(String titulo, String[] headers, List<String[]> filas) {
        return pagina(seccion(titulo, headers, filas));
    }

    /** Una tabla para combinar varias en el mismo correo con pagina(...). */
    public static String seccion(String titulo, String[] headers, List<String[]> filas) {
        StringBuilder sb = new StringBuilder("<table><caption>").append(escapar(titulo)).append("</caption><thead><tr>");
        for (String h : headers) {
            sb.append("<th>").append(escapar(h)).append("</th>");
        }
        sb.append("</tr></thead><tbody>");
        if (filas.isEmpty()) {
            sb.append("<tr><td colspan=\"").append(headers.length).append("\">No hay registros</td></tr>");
        }
        for (String[] fila : filas) {
            sb.append("<tr>");
            for (String v : fila) {
                sb.append("<td>").append(escapar(v)).append("</td>");
            }
            sb.append("</tr>");
        }
        return sb.append("</tbody></table>").toString();
    }

    /**
     * Gráfico de barras como imagen de quickchart.io (el correo no ejecuta JavaScript).
     * Usa la columna 'etiqueta' como nombre de cada barra y la columna 'valor' como altura.
     */
    public static String grafica(String titulo, List<String[]> filas, int etiqueta, int valor, String serie) {
        if (filas.isEmpty()) {
            return "<h3>" + escapar(titulo) + ": sin datos todavía</h3>";
        }
        StringBuilder labels = new StringBuilder();
        StringBuilder datos = new StringBuilder();
        for (String[] f : filas) {
            labels.append(labels.length() == 0 ? "" : ",").append('\'').append(f[etiqueta].replace("'", "")).append('\'');
            datos.append(datos.length() == 0 ? "" : ",").append(f[valor].isEmpty() ? "0" : f[valor]);
        }
        String config = "{type:'bar',data:{labels:[" + labels + "],datasets:[{label:'" + serie + "',data:[" + datos
                + "],backgroundColor:'#0b3d91'}]},options:{title:{display:true,text:'" + titulo.replace("'", "") + "'}}}";
        return "<img width=\"600\" height=\"300\" alt=\"" + escapar(titulo) + "\" src=\"https://quickchart.io/chart?w=600&h=300&c="
                + URLEncoder.encode(config, StandardCharsets.UTF_8) + "\">";
    }

    public static String pagina(String... secciones) {
        return "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" + ESTILO + "</head><body>"
                + "<h1>" + TITULO + "</h1>" + String.join("", secciones)
                + "<p class=\"pie\">Grupo 13 · Tecnología Web · Envíe <b>HELP</b> para ver los comandos</p>"
                + "</body></html>";
    }

    /** Los datos vienen de los usuarios: se escapan para que no inyecten HTML en el correo. */
    private static String escapar(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
