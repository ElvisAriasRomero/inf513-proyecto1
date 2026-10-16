package main;

import interpreter.analex.Interpreter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import pagos.WebhookPagoFacil;

/**
 * Modo local: los comandos se escriben en la terminal (como si fueran el asunto del correo)
 * y cada respuesta se guarda en respuestas/respuesta-N.html para abrirla en el navegador.
 * Usa la misma lógica, permisos y base de datos que el modo correo.
 */
public class Consola extends Aplication {

    private static final Path CARPETA = Path.of("respuestas");
    private int numero = 0;

    public static void main(String[] args) throws IOException {
        new Consola().ejecutar(args.length > 0 ? args[0] : "director@gmail.com");
    }

    private void ejecutar(String remitente) throws IOException {
        Files.createDirectories(CARPETA);
        WebhookPagoFacil.iniciar();
        System.out.println("=== Modo local (sin correo) ===");
        System.out.println("Escriba un comando igual que en el asunto del correo, por ejemplo: HELP o LISUSU[\"*\"]");
        System.out.println("Cambiar de remitente: como juan@gmail.com   |   Terminar: salir");

        Reader entrada = System.console() != null ? System.console().reader() : new InputStreamReader(System.in);
        BufferedReader in = new BufferedReader(entrada);
        while (true) {
            System.out.print("\n" + remitente + "> ");
            String linea = in.readLine();
            if (linea == null || linea.trim().equalsIgnoreCase("salir")) {
                System.exit(0);
            }
            linea = linea.trim();
            if (linea.isEmpty()) {
                continue;
            }
            if (linea.toLowerCase().startsWith("como ")) {
                remitente = linea.substring(5).trim();
                continue;
            }
            Interpreter interpreter = new Interpreter(linea + " ", remitente);
            interpreter.setListener(this);
            interpreter.run(); // en el mismo hilo, para que la respuesta salga antes del siguiente comando
        }
    }

    @Override
    protected synchronized void enviar(String to, String html, String imagenBase64) {
        numero++;
        try {
            if (imagenBase64 != null) {
                String png = "qr-" + numero + ".png";
                Files.write(CARPETA.resolve(png), Base64.getMimeDecoder().decode(imagenBase64));
                html = html.replace("cid:imagen", png);
            }
            Path archivo = CARPETA.resolve("respuesta-" + numero + ".html");
            Files.writeString(archivo, html, StandardCharsets.UTF_8);
            System.out.println(comoTexto(html));
            System.out.println("Respuesta guardada en: " + archivo.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("No se pudo guardar la respuesta: " + e.getMessage());
        }
    }

    /** Versión en texto del HTML para leerla directo en la terminal. */
    static String comoTexto(String html) {
        String t = html.replaceAll("(?s)<head>.*?</head>", "")
                .replaceAll("(?s)<p class=\"pie\">.*?</p>", "")
                .replaceAll("<img[^>]*alt=\"([^\"]*)\"[^>]*>", "[imagen: $1]\n")
                .replaceAll("</t[hd]>", " | ")
                .replaceAll("</(tr|h1|h2|h3|caption)>", "\n")
                .replaceAll("<[^>]+>", "")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&amp;", "&");
        return t.replaceAll("[ \\t]+", " ").replaceAll("\\n\\s*\\n+", "\n").trim();
    }
}
