package negociodato.utils;

/**
 * Configuración del Grupo 13: correo, base de datos y scraping.
 */
public class Constantes {

    // Servidor de Tecno Web
    public static final String GRUPO = "grupo13sc";
    // La contraseña no se guarda en el código (el repositorio es público): se lee de la variable de entorno.
    // Antes de ejecutar: $env:GRUPO_PASSWORD="..."
    public static final String PASSWORD = env("GRUPO_PASSWORD", "");
    public static final String HOST = "mail.tecnoweb.org.bo";

    // Correo (POP3 para leer, SMTP para responder)
    public static final int POP_PORT = 110;
    public static final String SMTP_PORT = "25";
    public static final String MAIL = GRUPO + "@tecnoweb.org.bo";

    // Base de datos. Para probar en local se cambian con variables de entorno, sin recompilar:
    // $env:DB_HOST="localhost"; $env:DB_USER="postgres"; $env:DB_PASSWORD="su_clave"
    public static final String DB_HOST = env("DB_HOST", HOST);
    public static final String DB_PORT = env("DB_PORT", "5432");
    public static final String DB_NAME = env("DB_NAME", "db_grupo13sc");
    public static final String DB_USER = env("DB_USER", GRUPO);
    public static final String DB_PASSWORD = env("DB_PASSWORD", PASSWORD);

    // Scraping
    public static final String PALABRA_CLAVE = "UAGRM";
    public static final int HORAS_ENTRE_CAPTURAS = 6;
    public static final int CAPTURAS_POR_ESCANEO = 4; // 4 x 6 h = páginas externas 1 vez al día
    public static final int DIAS_SEGUIMIENTO = 30;

    // PagoFácil QR Master v2 (documento "ServiciosPagoFacil_QrMaster_v1.1.0").
    // Las credenciales las entrega PagoFácil y se leen de variables de entorno (no van en el código).
    public static final String PAGOFACIL_URL = env("PAGOFACIL_BASE_URL", "https://masterqr.pagofacil.com.bo/api/services/v2");
    public static final String PAGOFACIL_TOKEN_SERVICE = env("PAGOFACIL_TOKEN_SERVICE", "");
    public static final String PAGOFACIL_TOKEN_SECRET = env("PAGOFACIL_TOKEN_SECRET", "");
    // paymentMethodId del QR. Vacío = se pide a /list-enabled-services y se usa el primero habilitado.
    public static final String PAGOFACIL_METODO = env("PAGOFACIL_METODO", "");
    // Callback (opcional): URL pública (ngrok) que apunta al puerto de abajo + /pagofacil/webhook.
    // Sin callback igual funciona: el sistema consulta /query-transaction cada minuto.
    public static final String PAGOFACIL_CALLBACK_URL = env("PAGOFACIL_CALLBACK_URL", "https://postman-echo.com/post");
    public static final int PAGOFACIL_PUERTO = Integer.parseInt(env("PAGOFACIL_PUERTO", "8080"));
    public static final int PAGOFACIL_SEGUNDOS_CONSULTA = 60;
    // Modo prueba: al banco se le cobra 0.01 Bs, pero la cuota se marca pagada por su monto real
    public static final boolean PAGOFACIL_MODO_PRUEBA = true;

    private static String env(String nombre, String defecto) {
        String v = System.getenv(nombre);
        return v == null || v.isEmpty() ? defecto : v;
    }
}
