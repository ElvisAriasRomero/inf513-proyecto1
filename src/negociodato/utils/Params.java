package negociodato.utils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lectura y validación de los parámetros del comando.
 * Los errores de regla se lanzan como IllegalArgumentException y su mensaje llega tal cual al correo.
 */
public final class Params {

    private Params() {
    }

    /** Obligatorio. */
    public static String texto(List<String> p, int i) {
        String v = p.get(i).trim();
        if (v.isEmpty()) {
            throw new IllegalArgumentException("El parámetro " + (i + 1) + " no puede estar vacío");
        }
        return v;
    }

    /** Opcional: si falta, está vacío, es "-" o es "*", devuelve null. */
    public static String opcional(List<String> p, int i) {
        if (i >= p.size()) {
            return null;
        }
        String v = p.get(i).trim();
        return v.isEmpty() || v.equals("-") || v.equals("*") ? null : v;
    }

    public static int entero(List<String> p, int i) {
        return Integer.parseInt(texto(p, i));
    }

    public static BigDecimal decimal(List<String> p, int i) {
        return new BigDecimal(texto(p, i).replace(',', '.'));
    }

    /** Formato AAAA-MM-DD HH:MM. */
    public static LocalDateTime fechaHora(List<String> p, int i) {
        return LocalDateTime.parse(texto(p, i).replace(' ', 'T'));
    }

    /** Uno de los valores permitidos, sin importar mayúsculas. Devuelve la forma oficial. */
    public static String opcion(List<String> p, int i, String... validas) {
        String v = texto(p, i);
        for (String o : validas) {
            if (o.equalsIgnoreCase(v)) {
                return o;
            }
        }
        throw new IllegalArgumentException("Valor no válido: " + v + ". Use: " + String.join(", ", validas));
    }

    public static String url(List<String> p, int i) {
        String v = texto(p, i);
        if (!v.startsWith("http://") && !v.startsWith("https://")) {
            throw new IllegalArgumentException("El enlace debe empezar con http:// o https://");
        }
        return v;
    }

    /** Para UPDATE/DELETE: si no cambió ninguna fila, avisa con el mensaje. */
    public static void existe(int filas, String error) {
        if (filas == 0) {
            throw new IllegalArgumentException(error);
        }
    }

    public static String existe(String id, String error) {
        if (id == null) {
            throw new IllegalArgumentException(error);
        }
        return id;
    }
}
