import interpreter.analex.Interpreter;
import interpreter.analex.interfaces.ITokenEventListener;
import interpreter.analex.utils.Token;
import interpreter.events.TokenEvent;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import main.Permisos;
import negociodato.negocio.NPagina;
import pagos.PagoFacil;
import pagos.WebhookPagoFacil;
import scraping.Scraper;

/**
 * Verificación sin base de datos ni correo: intérprete, scraping y permisos.
 * Ejecutar con: java -ea -cp build/classes;build/test/classes Verificacion
 */
public class Verificacion {

    private static String metodo;
    private static TokenEvent evento;

    public static void main(String[] args) {
        // --- Intérprete (formato del enunciado: CODIGO["p1","p2"]) ---
        interpretar("INSCOT[\"3\",\"cuotas\",\"3\",\"1\",\"2\"]");
        check(metodo.equals("cotizacion") && evento.getAction() == Token.AGREGAR
                && evento.getParams().equals(List.of("3", "cuotas", "3", "1", "2")), "INSCOT");

        interpretar("LISUSU[\"*\"]");
        check(metodo.equals("usuario") && evento.getAction() == Token.LISTAR && evento.getParams().equals(List.of("*")), "LISUSU *");

        interpretar("lisusu [ \"*\" ]");
        check(metodo.equals("usuario") && evento.getAction() == Token.LISTAR, "minúsculas y espacios");

        interpretar("INSPAU[\"1\", \"2\", \"en_vivo\", \"2026-10-15 20:00\", \"2026-10-15 21:00\"]");
        check(evento.getParams().equals(List.of("1", "2", "en_vivo", "2026-10-15 20:00", "2026-10-15 21:00")), "fechas con hora");

        interpretar("INSMAT[\"5\",\"Banner, Feria\",\"https://drive.google.com/x?id=1&a=b\",\"imagen\"]");
        check(evento.getParams().get(1).equals("Banner, Feria")
                && evento.getParams().get(2).equals("https://drive.google.com/x?id=1&a=b"), "coma dentro de comillas y url");

        interpretar("INSUSU[\"3\",\"José\",\"Peña Añez\",\"123\",\"jose@gmail.com\",\"777\",\"jose\",\"Ñandú;#1]\"]");
        check(evento.getParams().get(2).equals("Peña Añez") && evento.getParams().get(7).equals("Ñandú;#1]"), "tildes, ñ y símbolos");

        interpretar("RECMAT[\"4\",\"\"]");
        check(evento.getParams().equals(List.of("4", "")), "parámetro vacío conserva su posición");

        interpretar("REPRED[\"*\"]");
        check(metodo.equals("reporte") && evento.getAction() == Token.REDES, "reporte");

        interpretar("PAGCUO[\"5\",\"efectivo\"]");
        check(metodo.equals("cuota") && evento.getAction() == Token.PAGAR, "PAGCUO (pagar cuota)");

        interpretar("LISPAG[\"*\"]");
        check(metodo.equals("pagina") && evento.getAction() == Token.LISTAR, "LISPAG (listar páginas)");

        interpretar("HELP");
        check(metodo.equals("help"), "HELP");

        interpretar("help");
        check(metodo.equals("help"), "help en minúsculas");

        interpretar("LISPER[\"*\"]");
        check(metodo.equals("error") && evento.getParams().get(1).equals("LISPER"), "comando desconocido");

        interpretar("usuario listar");
        check(metodo.equals("error"), "formato viejo ya no se acepta");

        // Comillas escapadas dentro de un parámetro
        interpretar("INSMAT[\"1\",\"Banner \\\"Feria\\\" 2026\",\"https://x.com/a\",\"imagen\"]");
        check(metodo.equals("material") && evento.getParams().get(1).equals("Banner \"Feria\" 2026"), "comilla escapada \\\"");
        interpretar("INSROL[\"C:\\\\carpeta\"]");
        check(evento.getParams().get(0).equals("C:\\carpeta"), "barra escapada \\\\");

        // Cantidad exacta de parámetros
        interpretar("INSUSU[\"3\",\"Juan\"]");
        check(metodo.equals("error") && evento.getAction() == Token.ERROR_PARAMS
                && evento.getParams().get(1).equals("INSUSU espera 8 parámetro(s) y recibió 2"), "faltan parámetros");
        interpretar("ELIUSU[\"2\",\"3\"]");
        check(metodo.equals("error") && evento.getAction() == Token.ERROR_PARAMS, "sobran parámetros");
        interpretar("INSCOT[\"3\",\"contado\"]");
        check(metodo.equals("error") && evento.getAction() == Token.ERROR_PARAMS, "INSCOT sin servicios no se acepta");
        interpretar("INSCOT[\"3\",\"contado\",\"4\",\"1\"]");
        check(metodo.equals("cotizacion"), "INSCOT al contado con un servicio");
        interpretar("INSDET[\"1\",\"3\",\"2\"]");
        check(metodo.equals("error"), "INSDET ya no existe (el detalle solo se consulta)");
        interpretar("INSCOT[\"5\",\"cuotas\",\"2\",\"1\",\"2\",\"3\",\"1\"]");
        check(metodo.equals("cotizacion") && evento.getParams().size() == 7, "INSCOT con servicios en la misma línea");
        interpretar("PAGCUO[\"5\",\"QR\",\"88\"]");
        check(metodo.equals("cuota"), "PAGCUO dentro del rango 2 a 4 (la regla de QR la valida el negocio)");
        interpretar("LISUSU");
        check(metodo.equals("usuario"), "listar sin parámetros");

        // El cliente pide un servicio por su cuenta (siempre al contado)
        interpretar("SOLCOT[\"1\",\"2\"]");
        check(metodo.equals("cotizacion") && evento.getAction() == Token.SOLICITAR, "SOLCOT");
        interpretar("SOLCOT[\"1\"]");
        check(metodo.equals("cotizacion"), "SOLCOT sin cantidad");
        check(Permisos.permitido("Cliente", Token.COTIZACION, Token.SOLICITAR), "cliente solicita");
        check(!Permisos.permitido("Cliente", Token.DETALLE, Token.AGREGAR), "cliente no agrega servicios (crea otra cotización)");
        check(!Permisos.permitido("Cliente", Token.COTIZACION, Token.AGREGAR), "cliente no usa INSCOT (no elige cuotas)");
        check(!Permisos.permitido("Cliente", Token.COTIZACION, Token.MODIFICAR), "cliente no cambia a cuotas");
        interpretar("REGUSU[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\"][\"extra\"]");
        check(metodo.equals("error") && evento.getAction() == Token.ERROR_CHARACTER, "texto después del corchete de cierre");
        interpretar("LISUSU[\"*\"]   ");
        check(metodo.equals("usuario"), "espacios después del corchete se aceptan");

        // --- Scraping: métricas ---
        long[] tiktok = Scraper.leerMetricas("{\"playCount\":2300,\"diggCount\":160,\"commentCount\":21,\"shareCount\":14}");
        check(Arrays.equals(tiktok, new long[]{2300, 160, 21, 14}), "métricas TikTok");
        long[] ig = Scraper.leerMetricas("{\"edge_liked_by\":{\"count\":55},\"edge_media_to_comment\":{\"count\":4}}");
        check(Arrays.equals(ig, new long[]{0, 55, 4, 0}), "métricas Instagram");
        check(Scraper.leerMetricas("<html>sin datos</html>") == null, "página sin métricas");

        // --- Scraping: páginas externas ---
        String ddg = "<a rel=\"nofollow\" class=\"result__a\" href=\"//duckduckgo.com/l/?uddg=https%3A%2F%2Fwww.facebook.com%2Fuagrm.cursos2%2F&amp;rut=x\">"
                + "<b>UAGRM</b> Oficial Cursos</a> ... <a class=\"result__snippet\" href=\"x\">12,5 mil seguidores · cursos</a>"
                + "<a rel=\"nofollow\" class=\"result__a\" href=\"//duckduckgo.com/l/?uddg=https%3A%2F%2Fotra.com%2Fuagrm\">Otra</a>";
        List<String[]> paginas = Scraper.leerResultados(ddg, "facebook.com");
        check(paginas.size() == 1, "solo resultados del dominio");
        check(paginas.get(0)[0].equals("UAGRM Oficial Cursos") && paginas.get(0)[1].equals("https://facebook.com/uagrm.cursos2")
                && paginas.get(0)[2].equals("12500"), "nombre, url y seguidores");

        check(Scraper.seguidores("3.4K followers") == 3400, "3.4K");
        check(Scraper.seguidores("1.234 seguidores") == 1234, "miles con punto");
        check(Scraper.seguidores("2 M seguidores") == 2_000_000, "millones");
        check(Scraper.normalizar("https://www.Facebook.com/UAGRM/?ref=x").equals("facebook.com/uagrm"), "normalizar");

        List<String> propias = List.of("facebook.com/uagrm");
        check(NPagina.esPropia("facebook.com/uagrm/posts/1", propias), "publicación propia");
        check(!NPagina.esPropia("facebook.com/uagrm.cursos2", propias), "página parecida no es propia");

        // --- PagoFácil ---
        interpretar("QRPCUO[\"5\"]");
        check(metodo.equals("cuota") && evento.getAction() == Token.QR && evento.getParams().equals(List.of("5")), "cuota qr");
        String respuesta = "{\"error\":0,\"values\":{\"transactionId\":987654,\"qrBase64\":\"iVBORw0KGgo\\/AAA+b==\"}}";
        check(PagoFacil.campo(respuesta, "qrBase64").equals("iVBORw0KGgo/AAA+b=="), "qrBase64 con \\/");
        check(PagoFacil.campo(respuesta, "transactionId").equals("987654"), "transactionId numérico");
        check(PagoFacil.campo("{\"values\":{\"accessToken\":\"abc.def\"}}", "accessToken").equals("abc.def"), "accessToken");
        String avisoJson = "{\"PedidoID\":\"CUOTA-5-1728345\",\"Estado\":2,\"TransaccionDePago\":\"777\"}&";
        check(WebhookPagoFacil.dato(avisoJson, "Estado", "TransactionStatus").equals("2"), "webhook JSON estado");
        check(WebhookPagoFacil.dato(avisoJson, "PedidoID", "PaymentNumber").equals("CUOTA-5-1728345"), "webhook JSON pedido");
        String avisoForm = "&PaymentNumber=CUOTA-9-1&TransactionStatus=2";
        check(WebhookPagoFacil.dato(avisoForm, "PedidoID", "PaymentNumber").equals("CUOTA-9-1"), "webhook formulario");
        check(WebhookPagoFacil.dato(avisoForm, "Estado", "TransactionStatus").equals("2"), "webhook formulario estado");
        check(Permisos.permitido("Cliente", Token.CUOTA, Token.QR), "cliente paga con QR");
        String lista = "{\"error\":0,\"status\":2006,\"values\":[{\"paymentMethodId\":4,\"paymentMethodName\":\"QR\",\"currencyName\":\"BOB\"}]}";
        check(PagoFacil.campo(lista, "paymentMethodId").equals("4"), "list-enabled-services");
        String consulta = "{\"error\":0,\"status\":2008,\"values\":{\"companyTransactionId\":\"CUOTA-5-1\",\"paymentStatus\":2,\"amount\":\"1.00\"}}";
        check(PagoFacil.esPagado(PagoFacil.campo(consulta, "paymentStatus")), "query-transaction pagado");
        check(!PagoFacil.esPagado("1") && !PagoFacil.esPagado(null), "pendiente no cuenta como pagado");
        check(!PagoFacil.esPagado(PagoFacil.REVISION) && !PagoFacil.esPagado(PagoFacil.ANULADO), "revisión y anulado no son pagado");
        check(PagoFacil.montoCobrado(new java.math.BigDecimal("0.002")).compareTo(new java.math.BigDecimal("0.01")) == 0,
                "al banco se le cobra mínimo 0.01");
        interpretar("VERCUO[\"5\"]");
        check(metodo.equals("cuota") && evento.getAction() == Token.VER, "VERCUO");
        check(Permisos.permitido("Cliente", Token.CUOTA, Token.VER), "cliente consulta su pago QR");
        check(PagoFacil.campo("{\"error\":0,\"values\":{\"expiresInMinutes\":789.71}}", "expiresInMinutes").equals("789.71"), "vida del token");
        check(!Permisos.permitido("Cliente", Token.CUOTA, Token.PAGAR), "cliente no registra pagos a mano");

        // --- Permisos ---
        check(Permisos.permitido(null, Token.HELP, 0), "help sin registro");
        check(!Permisos.permitido(null, Token.COTIZACION, Token.LISTAR), "no registrado bloqueado");
        check(!Permisos.permitido("Cliente", Token.COTIZACION, Token.APROBAR), "cliente ya no aprueba (SOLCOT queda aprobada)");
        check(!Permisos.permitido("Cliente", Token.PAUTA, Token.AGREGAR), "cliente no programa pautas");
        check(!Permisos.permitido("Encargado", Token.ROL, Token.AGREGAR), "encargado no crea roles");
        check(Permisos.permitido("Encargado", Token.PAGINA, Token.DENUNCIAR), "encargado denuncia");
        check(Permisos.permitido("Director", Token.USUARIO, Token.ELIMINAR), "director todo");

        // --- Auto-registro y asignación de roles ---
        interpretar("REGUSU[\"Juan\",\"Pérez\",\"789\",\"777\",\"juanp\",\"clave\"]");
        check(metodo.equals("usuario") && evento.getAction() == Token.REGISTRAR && evento.getParams().size() == 6, "REGUSU");
        interpretar("ASIUSU[\"3\",\"2\"]");
        check(metodo.equals("usuario") && evento.getAction() == Token.ASIGNAR, "ASIUSU");
        check(Permisos.permitido(null, Token.USUARIO, Token.REGISTRAR), "visitante se registra");
        check(!Permisos.permitido(null, Token.USUARIO, Token.LISTAR), "visitante no lista usuarios");
        check(Permisos.permitido(null, Token.SERVICIO, Token.LISTAR) && Permisos.permitido(null, Token.CUENTA, Token.LISTAR), "visitante ve servicios y cuentas");
        check(!Permisos.permitido(null, Token.CUENTA, Token.AGREGAR) && !Permisos.permitido("Cliente", Token.CUENTA, Token.ELIMINAR)
                && !Permisos.permitido(null, Token.SERVICIO, Token.MODIFICAR), "visitante y cliente solo listan");
        check(!Permisos.permitido("Encargado", Token.USUARIO, Token.AGREGAR), "encargado no crea usuarios con rol");
        check(!Permisos.permitido("Encargado", Token.USUARIO, Token.MODIFICAR), "encargado no modifica usuarios");
        check(!Permisos.permitido("Encargado", Token.USUARIO, Token.ASIGNAR), "encargado no asigna roles");
        check(Permisos.permitido("Encargado", Token.USUARIO, Token.LISTAR), "encargado lista usuarios");
        check(Permisos.permitido("Director", Token.USUARIO, Token.ASIGNAR), "director asigna roles");
        check(!Permisos.permitido("Cliente", Token.USUARIO, Token.ASIGNAR), "cliente no asigna roles");

        check(main.HtmlBuilder.monto("0.020").equals("0.02") && main.HtmlBuilder.monto("150.000").equals("150") && main.HtmlBuilder.monto("Juan").equals("Juan"), "montos sin ceros de más");
        long[] yt = scraping.Scraper.leerMetricas("\"viewCount\":\"60901445\",\"contextualInfo\":{\"runs\":[{\"text\":\"14 k\"}]}"); // YouTube usa espacio de no separación
        check(yt != null && yt[0] == 60901445 && yt[2] == 14000, "comentarios de YouTube abreviados (14 k)");
        System.out.println("OK: todas las verificaciones pasaron");
    }

    private static void interpretar(String asunto) {
        metodo = null;
        evento = null;
        ITokenEventListener listener = (ITokenEventListener) Proxy.newProxyInstance(
                Verificacion.class.getClassLoader(), new Class<?>[]{ITokenEventListener.class},
                (proxy, m, a) -> {
                    metodo = m.getName();
                    evento = (TokenEvent) a[0];
                    return null;
                });
        Interpreter interpreter = new Interpreter(asunto + " ", "test@gmail.com");
        interpreter.setListener(listener);
        interpreter.run();
    }

    private static void check(boolean condicion, String nombre) {
        if (!condicion) {
            throw new AssertionError("Falló: " + nombre + (evento == null ? "" : " → " + evento));
        }
    }
}
