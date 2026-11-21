package main;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Logs locales para la auditoría:
 *  - logs/access.log: cada correo recibido y cada comando ejecutado con su resultado (ÉXITO o ERROR).
 *  - logs/error.log:  cada error con su detalle y, si hubo una excepción inesperada, su traza.
 *    También recibe todo lo que el programa registra con java.util.logging (correo, scraping, PagoFácil).
 */
public final class Bitacora {

    private static final Path CARPETA = Path.of("logs");
    private static final Path ACCESO = CARPETA.resolve("access.log");
    private static final Logger ERRORES = Logger.getLogger("bitacora");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Bitacora() {
    }

    /** Se llama una vez al iniciar: abre logs/error.log para todos los avisos y errores del programa. */
    public static void iniciar() {
        try {
            Files.createDirectories(CARPETA);
            FileHandler archivo = new FileHandler(CARPETA.resolve("error.log").toString(), true);
            archivo.setEncoding("UTF-8");
            archivo.setFormatter(new SimpleFormatter());
            archivo.setLevel(Level.WARNING);
            Logger.getLogger("").addHandler(archivo);
            // Los errores de los comandos van solo al archivo (no ensucian la terminal)
            ERRORES.setUseParentHandlers(false);
            ERRORES.addHandler(archivo);
            // En la terminal, una sola línea por error; la traza completa queda en logs/error.log
            for (java.util.logging.Handler h : Logger.getLogger("").getHandlers()) {
                if (h instanceof java.util.logging.ConsoleHandler) {
                    h.setFormatter(new java.util.logging.Formatter() {
                        @Override
                        public String format(java.util.logging.LogRecord r) {
                            Throwable t = r.getThrown();
                            while (t != null && t.getCause() != null) {
                                t = t.getCause();
                            }
                            String clase = r.getSourceClassName() == null ? ""
                                    : r.getSourceClassName().substring(r.getSourceClassName().lastIndexOf('.') + 1);
                            String msg = t != null ? t.getClass().getSimpleName() + ": " + t.getMessage() : r.getMessage();
                            return LocalDateTime.now().format(HORA) + " ERROR " + clase + ": " + msg
                                    + " (detalle en logs/error.log)" + System.lineSeparator();
                        }
                    });
                }
            }
        } catch (IOException e) {
            System.err.println("No se pudo abrir logs/error.log: " + e.getMessage());
        }
    }

    /** Una línea en access.log. */
    public static synchronized void acceso(String linea) {
        try {
            Files.createDirectories(CARPETA);
            Files.writeString(ACCESO, LocalDateTime.now().format(HORA) + " | " + linea + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("No se pudo escribir logs/access.log: " + e.getMessage());
        }
    }

    /** Un error en error.log; con traza solo si hubo una excepción (null para errores de validación). */
    public static void error(String linea, Throwable causa) {
        ERRORES.log(Level.WARNING, linea, causa);
    }
}
