package interpreter.analex.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Tokens del comando. Formato del enunciado: CODIGO["p1","p2",...]
 * El código junta la acción y la tabla, por ejemplo LISUSU = listar usuarios, INSCOT = insertar cotización.
 * Casos de uso (100-199), acciones (200-299) y errores (300-399).
 */
public class Token {

    private int name;      // CU, ACTION, PARAMS, END o ERROR
    private int attribute; // qué CU, qué acción o qué error

    public static final int CU = 0;
    public static final int ACTION = 1;
    public static final int PARAMS = 2;
    public static final int END = 3;
    public static final int ERROR = 4;

    // Casos de uso (100-199)
    public static final int HELP = 100;
    public static final int ROL = 101;          // CU1
    public static final int USUARIO = 102;      // CU1
    public static final int CUENTA = 103;       // CU2
    public static final int SERVICIO = 104;     // CU3
    public static final int COTIZACION = 105;   // CU3
    public static final int DETALLE = 106;      // CU3
    public static final int CUOTA = 107;        // CU3
    public static final int MATERIAL = 108;     // CU4
    public static final int PAUTA = 109;        // CU5
    public static final int PAGINA = 110;       // CU6
    public static final int METRICA = 111;      // CU7
    public static final int REPORTE = 112;      // CU8

    // Acciones (200-299)
    public static final int AGREGAR = 200;
    public static final int MODIFICAR = 201;
    public static final int ELIMINAR = 202;
    public static final int LISTAR = 203;
    public static final int VER = 204;
    public static final int APROBAR = 205;
    public static final int RECHAZAR = 206;
    public static final int PAGAR = 207;
    public static final int VENCIDAS = 208;
    public static final int PUBLICAR = 209;
    public static final int CANCELAR = 210;
    public static final int ESCANEAR = 211;
    public static final int DENUNCIAR = 212;
    public static final int DESCARTAR = 213;
    public static final int CAPTURAR = 214;
    public static final int REDES = 215;
    public static final int TIPOS = 216;
    public static final int MENSUAL = 217;
    public static final int PAGINAS = 218;
    public static final int COBRANZA = 219;
    public static final int COTIZACIONES = 220;
    public static final int GRAFICA = 221;
    public static final int QR = 222;
    public static final int REGISTRAR = 223;
    public static final int ASIGNAR = 224;
    public static final int SOLICITAR = 225;

    public static final int ERROR_COMMAND = 300;
    public static final int ERROR_CHARACTER = 301;
    public static final int ERROR_PARAMS = 302;

    /** Código del asunto → {caso de uso, acción, mínimo de parámetros, máximo de parámetros}. */
    private static final Map<String, int[]> COMANDOS = new HashMap<>();
    private static final Map<Integer, String> NOMBRES = new HashMap<>();
    /** (caso de uso, acción) → {mínimo, máximo} de parámetros. */
    private static final Map<Integer, int[]> PARAMETROS = new HashMap<>();

    static {
        // Cantidad exacta de parámetros de cada comando: (código, CU, acción, mínimo, máximo).
        // ["*"] cuenta como 1 parámetro; los listados aceptan también no escribir nada.
        comando("HELP", HELP, 0, 0, 1);

        comando("INSROL", ROL, AGREGAR, 1, 1);
        comando("MODROL", ROL, MODIFICAR, 2, 2);
        comando("ELIROL", ROL, ELIMINAR, 1, 1);
        comando("LISROL", ROL, LISTAR, 0, 1);

        comando("INSUSU", USUARIO, AGREGAR, 8, 8);
        comando("MODUSU", USUARIO, MODIFICAR, 7, 7);
        comando("ELIUSU", USUARIO, ELIMINAR, 1, 1);
        comando("LISUSU", USUARIO, LISTAR, 0, 1);
        comando("REGUSU", USUARIO, REGISTRAR, 6, 6); // auto-registro como Cliente
        comando("ASIUSU", USUARIO, ASIGNAR, 2, 2);   // asignar rol

        comando("INSCUE", CUENTA, AGREGAR, 3, 4);     // el token es opcional
        comando("MODCUE", CUENTA, MODIFICAR, 4, 5);
        comando("ELICUE", CUENTA, ELIMINAR, 1, 1);
        comando("LISCUE", CUENTA, LISTAR, 0, 1);

        comando("INSSER", SERVICIO, AGREGAR, 3, 3);
        comando("MODSER", SERVICIO, MODIFICAR, 4, 4);
        comando("ELISER", SERVICIO, ELIMINAR, 1, 1);
        comando("LISSER", SERVICIO, LISTAR, 0, 1);

        comando("INSCOT", COTIZACION, AGREGAR, 2, 3);   // al contado no lleva nro_cuotas
        comando("MODCOT", COTIZACION, MODIFICAR, 2, 3);
        comando("ELICOT", COTIZACION, ELIMINAR, 1, 1);
        comando("LISCOT", COTIZACION, LISTAR, 0, 1);
        comando("VERCOT", COTIZACION, VER, 1, 1);
        comando("APRCOT", COTIZACION, APROBAR, 1, 1);
        comando("RECCOT", COTIZACION, RECHAZAR, 1, 1);
        comando("SOLCOT", COTIZACION, SOLICITAR, 1, 2); // el cliente pide un servicio (siempre al contado)

        comando("INSDET", DETALLE, AGREGAR, 3, 3);
        comando("ELIDET", DETALLE, ELIMINAR, 1, 1);
        comando("LISDET", DETALLE, LISTAR, 1, 1);

        comando("LISCUO", CUOTA, LISTAR, 1, 1);
        comando("PAGCUO", CUOTA, PAGAR, 2, 4);        // efectivo: 2; QR a mano: 4
        comando("QRPCUO", CUOTA, QR, 1, 1);
        comando("VERCUO", CUOTA, VER, 1, 1);          // "ya pagué": consulta el QR en PagoFácil
        comando("VENCUO", CUOTA, VENCIDAS, 0, 1);

        comando("INSMAT", MATERIAL, AGREGAR, 4, 4);
        comando("LISMAT", MATERIAL, LISTAR, 0, 1);
        comando("APRMAT", MATERIAL, APROBAR, 1, 2);   // observación opcional
        comando("RECMAT", MATERIAL, RECHAZAR, 2, 2);

        comando("INSPAU", PAUTA, AGREGAR, 4, 5);      // en vivo lleva hora de fin
        comando("PUBPAU", PAUTA, PUBLICAR, 2, 2);
        comando("CANPAU", PAUTA, CANCELAR, 1, 1);
        comando("LISPAU", PAUTA, LISTAR, 0, 1);

        comando("ESCPAG", PAGINA, ESCANEAR, 0, 1);
        comando("INSPAG", PAGINA, AGREGAR, 4, 5);     // seguidores opcional
        comando("DENPAG", PAGINA, DENUNCIAR, 1, 1);
        comando("DESPAG", PAGINA, DESCARTAR, 1, 1);
        comando("LISPAG", PAGINA, LISTAR, 0, 1);

        comando("CAPMET", METRICA, CAPTURAR, 0, 1);
        comando("INSMET", METRICA, AGREGAR, 5, 5);
        comando("LISMET", METRICA, LISTAR, 1, 1);

        comando("REPRED", REPORTE, REDES, 0, 1);
        comando("REPGRA", REPORTE, GRAFICA, 0, 1);
        comando("REPTIP", REPORTE, TIPOS, 0, 1);
        comando("REPMEN", REPORTE, MENSUAL, 0, 1);
        comando("REPPAG", REPORTE, PAGINAS, 0, 1);
        comando("REPCOB", REPORTE, COBRANZA, 0, 1);
        comando("REPCOT", REPORTE, COTIZACIONES, 0, 1);

        NOMBRES.put(CU, "caso de uso");
        NOMBRES.put(ACTION, "action");
        NOMBRES.put(PARAMS, "params");
        NOMBRES.put(END, "end");
        NOMBRES.put(ERROR, "error");
        NOMBRES.put(ERROR_COMMAND, "UNKNOWN COMMAND");
        NOMBRES.put(ERROR_CHARACTER, "UNKNOWN CHARACTER");
        NOMBRES.put(ERROR_PARAMS, "WRONG PARAMS");
    }

    private static void comando(String codigo, int cu, int accion, int minimo, int maximo) {
        COMANDOS.put(codigo, new int[]{cu, accion});
        NOMBRES.put(cu * 1000 + accion, codigo);
        PARAMETROS.put(cu * 1000 + accion, new int[]{minimo, maximo});
    }

    /** Código del comando (LISUSU, INSCOT...) a partir del caso de uso y la acción. */
    public static String codigo(int cu, int accion) {
        return NOMBRES.getOrDefault(cu * 1000 + accion, "?");
    }

    /** {mínimo, máximo} de parámetros que acepta el comando, o null si no está definido. */
    public static int[] parametros(int cu, int accion) {
        return PARAMETROS.get(cu * 1000 + accion);
    }

    /** Tokens del código (caso de uso y acción), o null si el código no existe. */
    public static Token[] comando(String codigo) {
        int[] c = COMANDOS.get(codigo.toUpperCase());
        if (c == null) {
            return null;
        }
        return c[1] == 0
                ? new Token[]{new Token(CU, c[0])}
                : new Token[]{new Token(CU, c[0]), new Token(ACTION, c[1])};
    }

    public Token() {
    }

    public Token(int name) {
        this.name = name;
    }

    public Token(int name, int attribute) {
        this.name = name;
        this.attribute = attribute;
    }

    public int getName() {
        return name;
    }

    public int getAttribute() {
        return attribute;
    }

    public String getStringToken(int token) {
        return NOMBRES.getOrDefault(token, "N: " + token);
    }

    @Override
    public String toString() {
        if (name == PARAMS) {
            return "< params , " + attribute + ">";
        }
        if (name == END) {
            return "< end , _______ >";
        }
        return "< " + getStringToken(name) + " , " + attribute + ">";
    }
}
