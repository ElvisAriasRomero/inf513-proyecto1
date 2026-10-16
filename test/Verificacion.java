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
        interpretar("INSCOT[\"3\",\"cuotas\",\"3\"]");
        check(metodo.equals("cotizacion") && evento.getAction() == Token.AGREGAR
                && evento.getParams().equals(List.of("3", "cuotas", "3")), "INSCOT");

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
        check(!Permisos.permitido("Cliente", Token.CUOTA, Token.PAGAR), "cliente no registra pagos a mano");

        // --- Permisos ---
        check(Permisos.permitido(null, Token.HELP, 0), "help sin registro");
        check(!Permisos.permitido(null, Token.COTIZACION, Token.LISTAR), "no registrado bloqueado");
        check(Permisos.permitido("Cliente", Token.COTIZACION, Token.APROBAR), "cliente acepta cotización");
        check(!Permisos.permitido("Cliente", Token.PAUTA, Token.AGREGAR), "cliente no programa pautas");
        check(!Permisos.permitido("Encargado", Token.ROL, Token.AGREGAR), "encargado no crea roles");
        check(Permisos.permitido("Encargado", Token.PAGINA, Token.DENUNCIAR), "encargado denuncia");
        check(Permisos.permitido("Director", Token.USUARIO, Token.ELIMINAR), "director todo");

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
