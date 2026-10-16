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

    public static final int ERROR_COMMAND = 300;
    public static final int ERROR_CHARACTER = 301;

    /** Código del asunto → {caso de uso, acción}. */
    private static final Map<String, int[]> COMANDOS = new HashMap<>();
    private static final Map<Integer, String> NOMBRES = new HashMap<>();

    static {
        comando("HELP", HELP, 0);

        crud(ROL, "ROL");
        crud(USUARIO, "USU");
        crud(CUENTA, "CUE");
        crud(SERVICIO, "SER");

        crud(COTIZACION, "COT");
        comando("VERCOT", COTIZACION, VER);
        comando("APRCOT", COTIZACION, APROBAR);
        comando("RECCOT", COTIZACION, RECHAZAR);

        comando("INSDET", DETALLE, AGREGAR);
        comando("ELIDET", DETALLE, ELIMINAR);
        comando("LISDET", DETALLE, LISTAR);

        comando("LISCUO", CUOTA, LISTAR);
        comando("PAGCUO", CUOTA, PAGAR);
        comando("QRPCUO", CUOTA, QR);
        comando("VENCUO", CUOTA, VENCIDAS);

        comando("INSMAT", MATERIAL, AGREGAR);
        comando("LISMAT", MATERIAL, LISTAR);
        comando("APRMAT", MATERIAL, APROBAR);
        comando("RECMAT", MATERIAL, RECHAZAR);

        comando("INSPAU", PAUTA, AGREGAR);
        comando("PUBPAU", PAUTA, PUBLICAR);
        comando("CANPAU", PAUTA, CANCELAR);
        comando("LISPAU", PAUTA, LISTAR);

        comando("ESCPAG", PAGINA, ESCANEAR);
        comando("INSPAG", PAGINA, AGREGAR);
        comando("DENPAG", PAGINA, DENUNCIAR);
        comando("DESPAG", PAGINA, DESCARTAR);
        comando("LISPAG", PAGINA, LISTAR);

        comando("CAPMET", METRICA, CAPTURAR);
        comando("INSMET", METRICA, AGREGAR);
        comando("LISMET", METRICA, LISTAR);

        comando("REPRED", REPORTE, REDES);
        comando("REPGRA", REPORTE, GRAFICA);
        comando("REPTIP", REPORTE, TIPOS);
        comando("REPMEN", REPORTE, MENSUAL);
        comando("REPPAG", REPORTE, PAGINAS);
        comando("REPCOB", REPORTE, COBRANZA);
        comando("REPCOT", REPORTE, COTIZACIONES);

        NOMBRES.put(CU, "caso de uso");
        NOMBRES.put(ACTION, "action");
        NOMBRES.put(PARAMS, "params");
        NOMBRES.put(END, "end");
        NOMBRES.put(ERROR, "error");
        NOMBRES.put(ERROR_COMMAND, "UNKNOWN COMMAND");
        NOMBRES.put(ERROR_CHARACTER, "UNKNOWN CHARACTER");
    }

    private static void crud(int cu, String tabla) {
        comando("INS" + tabla, cu, AGREGAR);
        comando("MOD" + tabla, cu, MODIFICAR);
        comando("ELI" + tabla, cu, ELIMINAR);
        comando("LIS" + tabla, cu, LISTAR);
    }

    private static void comando(String codigo, int cu, int accion) {
        COMANDOS.put(codigo, new int[]{cu, accion});
        NOMBRES.put(cu * 1000 + accion, codigo);
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
