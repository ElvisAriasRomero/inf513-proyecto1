package main;

import coneccion.comunicacion.MailVerificationThread;
import coneccion.comunicacion.SendEmailThread;
import coneccion.interfaces.IEmailEventListener;
import coneccion.utils.Email;
import interpreter.analex.Interpreter;
import interpreter.analex.interfaces.ITokenEventListener;
import interpreter.analex.utils.Token;
import interpreter.events.TokenEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import negociodato.dato.*;
import negociodato.negocio.*;
import negociodato.utils.Constantes;
import negociodato.utils.Params;
import pagos.WebhookPagoFacil;
import scraping.ScrapingThread;

/**
 * Recibe los comandos por correo, revisa el rol de quien los envía,
 * ejecuta el caso de uso y responde con HTML.
 */
public class Aplication implements IEmailEventListener, ITokenEventListener {

    private final MailVerificationThread mailVerificationThread = new MailVerificationThread();

    private final NRol nRol = new NRol();
    private final NUsuario nUsuario = new NUsuario();
    private final NCuenta nCuenta = new NCuenta();
    private final NServicio nServicio = new NServicio();
    private final NCotizacion nCotizacion = new NCotizacion();
    private final NDetalle nDetalle = new NDetalle();
    private final NCuota nCuota = new NCuota();
    private final NMaterial nMaterial = new NMaterial();
    private final NPauta nPauta = new NPauta();
    private final NPagina nPagina = new NPagina();
    private final NMetrica nMetrica = new NMetrica();
    private final NReporte nReporte = new NReporte();

    /** Lo que hace un comando: recibe quién lo envió y devuelve el HTML de la respuesta. */
    private interface Accion {

        String ejecutar(Sesion s) throws Exception;
    }

    public Aplication() {
        mailVerificationThread.setEmailEventListener(this);
    }

    public void start() {
        iniciar(mailVerificationThread, "Mail Verification Thread");
        iniciar(new ScrapingThread(), "Scraping Thread");
        WebhookPagoFacil.iniciar();
    }

    @Override
    public void onReceiveEmailEvent(List<Email> emails) {
        for (Email email : emails) {
            Interpreter interpreter = new Interpreter(email.getSubject() + " ", email.getFrom());
            interpreter.setListener(this);
            iniciar(interpreter, "Interpreter Thread");
        }
    }

    // ===================== CASOS DE USO =====================

    @Override
    public void help(TokenEvent e) {
        responder(e, Token.HELP, s -> HtmlBuilder.tabla("Lista de comandos", DComando.HEADERS, DComando.listar()));
    }

    @Override
    public void rol(TokenEvent e) {
        List<String> p = e.getParams();
        responder(e, Token.ROL, s -> {
            switch (e.getAction()) {
                case Token.AGREGAR:
                    return ok("Rol " + nRol.guardar(p) + " creado");
                case Token.MODIFICAR:
                    nRol.modificar(p);
                    return ok("Rol modificado");
                case Token.ELIMINAR:
                    nRol.eliminar(p);
                    return ok("Rol eliminado");
                case Token.LISTAR:
                    return HtmlBuilder.tabla("Roles", DRol.HEADERS, nRol.listar());
                default:
                    return accionNoValida();
            }
        });
    }

    @Override
    public void usuario(TokenEvent e) {
        List<String> p = e.getParams();
        responder(e, Token.USUARIO, s -> {
            switch (e.getAction()) {
                case Token.AGREGAR:
                    return ok("Usuario " + nUsuario.guardar(p) + " registrado");
                case Token.MODIFICAR:
                    nUsuario.modificar(p);
                    return ok("Usuario modificado");
                case Token.ELIMINAR:
                    nUsuario.eliminar(s, p);
                    return ok("Usuario desactivado");
                case Token.LISTAR:
                    return HtmlBuilder.tabla("Usuarios", DUsuario.HEADERS, nUsuario.listar());
                default:
                    return accionNoValida();
            }
        });
    }

    @Override
    public void cuenta(TokenEvent e) {
        List<String> p = e.getParams();
        responder(e, Token.CUENTA, s -> {
            switch (e.getAction()) {
                case Token.AGREGAR:
                    return ok("Cuenta " + nCuenta.guardar(s, p) + " registrada");
                case Token.MODIFICAR:
                    nCuenta.modificar(p);
                    return ok("Cuenta modificada");
                case Token.ELIMINAR:
                    nCuenta.eliminar(p);
                    return ok("Cuenta desactivada");
                case Token.LISTAR:
                    return HtmlBuilder.tabla("Cuentas de redes sociales", DCuenta.HEADERS, nCuenta.listar());
                default:
                    return accionNoValida();
            }
        });
    }

    @Override
    public void servicio(TokenEvent e) {
        List<String> p = e.getParams();
        responder(e, Token.SERVICIO, s -> {
            switch (e.getAction()) {
                case Token.AGREGAR:
                    return ok("Servicio " + nServicio.guardar(p) + " registrado");
                case Token.MODIFICAR:
                    nServicio.modificar(p);
                    return ok("Servicio modificado");
                case Token.ELIMINAR:
                    nServicio.eliminar(p);
                    return ok("Servicio desactivado");
                case Token.LISTAR:
                    return HtmlBuilder.tabla("Oferta comercial", DServicio.HEADERS, nServicio.listar());
                default:
                    return accionNoValida();
            }
        });
    }

    @Override
    public void cotizacion(TokenEvent e) {
        List<String> p = e.getParams();
        responder(e, Token.COTIZACION, s -> {
            switch (e.getAction()) {
                case Token.AGREGAR:
                    String id = nCotizacion.guardar(s, p);
                    return ok("Cotización " + id + " creada. Agregue servicios con: INSDET[\"" + id + "\",\"servicio_id\",\"cantidad\"]");
                case Token.MODIFICAR:
                    nCotizacion.modificar(p);
                    return ok("Cotización modificada");
                case Token.ELIMINAR:
                    nCotizacion.eliminar(p);
                    return ok("Cotización eliminada");
                case Token.LISTAR:
                    return HtmlBuilder.tabla("Cotizaciones", DCotizacion.HEADERS, nCotizacion.listar(s));
                case Token.VER:
                    int cot = Params.entero(p, 0);
                    return HtmlBuilder.pagina(
                            HtmlBuilder.seccion("Cotización " + cot, DCotizacion.HEADERS, Collections.singletonList(nCotizacion.ver(s, cot))),
                            HtmlBuilder.seccion("Servicios", DDetalle.HEADERS, nDetalle.listar(s, cot)),
                            HtmlBuilder.seccion("Cuotas", DCuota.HEADERS, nCuota.listar(s, cot)));
                case Token.APROBAR:
                    return ok("Cotización aprobada. Se generaron " + nCotizacion.aprobar(s, p) + " cuota(s). "
                            + "Véalas con: LISCUO[\"" + p.get(0).trim() + "\"]");
                case Token.RECHAZAR:
                    nCotizacion.rechazar(s, p);
                    return ok("Cotización rechazada");
                default:
                    return accionNoValida();
            }
        });
    }

    @Override
    public void detalle(TokenEvent e) {
        List<String> p = e.getParams();
        responder(e, Token.DETALLE, s -> {
            switch (e.getAction()) {
                case Token.AGREGAR:
                    nDetalle.agregar(p);
                    return ok("Servicio agregado a la cotización. Total actualizado.");
                case Token.ELIMINAR:
                    nDetalle.eliminar(p);
                    return ok("Servicio quitado de la cotización. Total actualizado.");
                case Token.LISTAR:
                    return HtmlBuilder.tabla("Servicios de la cotización", DDetalle.HEADERS,
                            nDetalle.listar(s, Params.entero(p, 0)));
                default:
                    return accionNoValida();
            }
        });
    }

    @Override
    public void cuota(TokenEvent e) {
        List<String> p = e.getParams();
        responder(e, Token.CUOTA, s -> {
            switch (e.getAction()) {
                case Token.LISTAR:
                    return HtmlBuilder.tabla("Cuotas", DCuota.HEADERS, nCuota.listar(s, Params.entero(p, 0)));
                case Token.PAGAR:
                    return ok(nCuota.pagar(s, p));
                case Token.QR:
                    String[] qr = nCuota.generarQr(s, p); // {mensaje, qrBase64}
                    enviar(e.getSender(), HtmlBuilder.qr("Pague su cuota con QR", qr[0]), qr[1]);
                    return null; // ya se envió con la imagen adjunta
                case Token.VENCIDAS:
                    return HtmlBuilder.tabla("Cuotas por cobrar", DCuota.HEADERS_COBRANZA, nCuota.porCobrar());
                default:
                    return accionNoValida();
            }
        });
    }

    @Override
    public void material(TokenEvent e) {
        List<String> p = e.getParams();
        responder(e, Token.MATERIAL, s -> {
            switch (e.getAction()) {
                case Token.AGREGAR:
                    return ok("Material " + nMaterial.guardar(s, p) + " recibido. Queda pendiente de revisión.");
                case Token.LISTAR:
                    return HtmlBuilder.tabla("Material publicitario", DMaterial.HEADERS, nMaterial.listar(s, p));
                case Token.APROBAR:
                    nMaterial.aprobar(s, p);
                    return ok("Material aprobado. Ya se puede programar su pauta.");
                case Token.RECHAZAR:
                    nMaterial.rechazar(s, p);
                    return ok("Material rechazado con su observación");
                default:
                    return accionNoValida();
            }
        });
    }

    @Override
    public void pauta(TokenEvent e) {
        List<String> p = e.getParams();
        responder(e, Token.PAUTA, s -> {
            switch (e.getAction()) {
                case Token.AGREGAR:
                    return ok("Pauta " + nPauta.guardar(p) + " programada");
                case Token.PUBLICAR:
                    nPauta.publicar(p);
                    return ok("Pauta publicada. El scraping capturará sus métricas cada "
                            + Constantes.HORAS_ENTRE_CAPTURAS + " horas.");
                case Token.CANCELAR:
                    nPauta.cancelar(p);
                    return ok("Pauta cancelada. El horario quedó libre.");
                case Token.LISTAR:
                    return HtmlBuilder.tabla("Pautas", DPauta.HEADERS, nPauta.listar());
                default:
                    return accionNoValida();
            }
        });
    }

    @Override
    public void pagina(TokenEvent e) {
        List<String> p = e.getParams();
        responder(e, Token.PAGINA, s -> {
            switch (e.getAction()) {
                case Token.ESCANEAR:
                    String palabra = Params.opcional(p, 0);
                    return ok(nPagina.escanear(palabra == null ? Constantes.PALABRA_CLAVE : palabra));
                case Token.AGREGAR:
                    nPagina.guardar(p);
                    return ok("Página registrada como detectada");
                case Token.DENUNCIAR:
                    nPagina.denunciar(s, p);
                    return ok("Página marcada como denunciada");
                case Token.DESCARTAR:
                    nPagina.descartar(s, p);
                    return ok("Página descartada");
                case Token.LISTAR:
                    return HtmlBuilder.tabla("Páginas externas detectadas", DPagina.HEADERS, nPagina.listar());
                default:
                    return accionNoValida();
            }
        });
    }

    @Override
    public void metrica(TokenEvent e) {
        List<String> p = e.getParams();
        responder(e, Token.METRICA, s -> {
            switch (e.getAction()) {
                case Token.CAPTURAR:
                    return ok(nMetrica.capturar());
                case Token.AGREGAR:
                    nMetrica.guardar(p);
                    return ok("Métrica registrada");
                case Token.LISTAR:
                    return HtmlBuilder.tabla("Evolución de la pauta " + p.get(0).trim(), DMetrica.HEADERS, nMetrica.listar(p));
                default:
                    return accionNoValida();
            }
        });
    }

    @Override
    public void reporte(TokenEvent e) {
        responder(e, Token.REPORTE, s -> {
            switch (e.getAction()) {
                case Token.REDES:
                    List<String[]> redes = nReporte.redes();
                    return HtmlBuilder.pagina(
                            HtmlBuilder.seccion("Efectividad por red social", DReporte.HEADERS_REDES, redes),
                            HtmlBuilder.grafica("Engagement por red (%)", redes, 0, 5, "Engagement %"),
                            HtmlBuilder.grafica("Vistas promedio por red", redes, 0, 4, "Vistas promedio"));
                case Token.GRAFICA:
                    List<String[]> datos = nReporte.redes();
                    return HtmlBuilder.pagina(
                            HtmlBuilder.grafica("Engagement por red (%)", datos, 0, 5, "Engagement %"),
                            HtmlBuilder.grafica("Vistas totales por red", datos, 0, 2, "Vistas"));
                case Token.TIPOS:
                    return HtmlBuilder.tabla("Efectividad por tipo de publicación", DReporte.HEADERS_TIPOS, nReporte.tipos());
                case Token.MENSUAL:
                    return HtmlBuilder.tabla("Movimiento por mes", DReporte.HEADERS_MENSUAL, nReporte.mensual());
                case Token.PAGINAS:
                    return HtmlBuilder.tabla("Páginas externas por red", DReporte.HEADERS_PAGINAS, nReporte.paginas());
                case Token.COBRANZA:
                    return HtmlBuilder.tabla("Cuotas por cobrar", DCuota.HEADERS_COBRANZA, nCuota.porCobrar());
                case Token.COTIZACIONES:
                    List<String[]> cot = nReporte.cotizaciones();
                    return HtmlBuilder.pagina(
                            HtmlBuilder.seccion("Cotizaciones por estado", DReporte.HEADERS_COTIZACIONES, cot),
                            HtmlBuilder.grafica("Cotizaciones por estado", cot, 0, 1, "Cantidad"));
                default:
                    return accionNoValida();
            }
        });
    }

    @Override
    public void error(TokenEvent e) {
        List<String> p = e.getParams();
        String motivo = e.getAction() == Token.ERROR_CHARACTER
                ? "El carácter \"" + p.get(1) + "\" no está permitido."
                : "No se reconoce \"" + p.get(1) + "\" como un comando válido.";
        enviar(e.getSender(), HtmlBuilder.texto("Comando desconocido",
                "No se pudo ejecutar [" + p.get(0) + "]", motivo, "Envíe HELP para ver la lista de comandos."));
    }

    // ===================== RESPUESTAS =====================

    /** Revisa permisos, ejecuta y traduce cualquier error a un mensaje claro para el correo. */
    private void responder(TokenEvent e, int cu, Accion accion) {
        String html;
        try {
            Sesion s = Sesion.de(e.getSender());
            if (!Permisos.permitido(s.rol, cu, e.getAction())) {
                html = HtmlBuilder.texto("Acceso denegado", s.rol == null
                        ? "Su correo " + e.getSender() + " no está registrado en el sistema."
                        : "Su rol (" + s.rol + ") no tiene permiso para esta acción.");
            } else {
                html = accion.ejecutar(s);
            }
        } catch (SQLException ex) {
            html = HtmlBuilder.texto("Error en la base de datos", mensajeSql(ex));
        } catch (NumberFormatException ex) {
            html = HtmlBuilder.texto("Error en el tipo de parámetro", "Uno de los parámetros debía ser un número.");
        } catch (IndexOutOfBoundsException ex) {
            html = HtmlBuilder.texto("Cantidad de parámetros incorrecta",
                    "Faltan parámetros para esta acción. Envíe HELP para ver el formato.");
        } catch (DateTimeParseException ex) {
            html = HtmlBuilder.texto("Fecha con formato incorrecto", "Use AAAA-MM-DD HH:MM, por ejemplo 2026-10-15 18:00");
        } catch (IllegalArgumentException ex) {
            html = HtmlBuilder.texto("No se pudo realizar la acción", ex.getMessage());
        } catch (IOException ex) {
            html = HtmlBuilder.texto("No se pudo contactar al servicio externo", ex.getMessage());
        } catch (Exception ex) {
            Logger.getLogger(Aplication.class.getName()).log(Level.SEVERE, null, ex);
            html = HtmlBuilder.texto("Error inesperado", "Intente nuevamente más tarde.");
        }
        if (html != null) {
            enviar(e.getSender(), html);
        }
    }

    private static String mensajeSql(SQLException ex) {
        String estado = ex.getSQLState() == null ? "" : ex.getSQLState();
        switch (estado) {
            case "23505":
                return "Ya existe un registro con ese dato (email, username, url o nombre repetido).";
            case "23503":
                return "Se hace referencia a un registro que no existe, o el registro está en uso.";
            case "23514":
                return "Uno de los valores no está permitido.";
            case "23P01":
                return "Horario ocupado: esa cuenta ya tiene una pauta en ese horario.";
            default:
                return estado.startsWith("08") ? "No hay conexión con la base de datos." : ex.getMessage();
        }
    }

    private static String ok(String mensaje) {
        return HtmlBuilder.texto("Petición realizada correctamente", mensaje);
    }

    private static String accionNoValida() {
        throw new IllegalArgumentException("Esa acción no existe para este comando. Envíe HELP para ver la lista.");
    }

    private void enviar(String to, String html) {
        enviar(to, html, null);
    }

    /** Envía la respuesta por correo. La consola local lo reemplaza para guardarla en un archivo. */
    protected void enviar(String to, String html, String imagenBase64) {
        iniciar(new SendEmailThread(new Email(to, Email.SUBJECT, html, imagenBase64)), "Send email Thread");
    }

    private static void iniciar(Runnable tarea, String nombre) {
        Thread thread = new Thread(tarea);
        thread.setName(nombre);
        thread.start();
    }
}
