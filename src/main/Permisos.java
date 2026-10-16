package main;

import interpreter.analex.utils.Token;
import java.util.Set;

/**
 * Qué puede hacer cada rol. El rol sale del email de quien envía el comando.
 *  - Director: todo.
 *  - Encargado: todo menos administrar roles y desactivar usuarios.
 *  - Cliente: ver sus cotizaciones y pagos, aceptarlas o rechazarlas, pagar sus cuotas con QR y subir su material.
 *  - No registrado: solo help.
 */
public class Permisos {

    private static final Set<String> CLIENTE = Set.of(
            clave(Token.SERVICIO, Token.LISTAR),
            clave(Token.COTIZACION, Token.LISTAR),
            clave(Token.COTIZACION, Token.VER),
            clave(Token.COTIZACION, Token.APROBAR),
            clave(Token.COTIZACION, Token.RECHAZAR),
            clave(Token.DETALLE, Token.LISTAR),
            clave(Token.CUOTA, Token.LISTAR),
            clave(Token.CUOTA, Token.QR),
            clave(Token.MATERIAL, Token.AGREGAR),
            clave(Token.MATERIAL, Token.LISTAR));

    public static boolean permitido(String rol, int cu, int accion) {
        if (cu == Token.HELP) {
            return true;
        }
        if (rol == null) {
            return false;
        }
        switch (rol) {
            case "Director":
                return true;
            case "Encargado":
                return !(cu == Token.ROL && accion != Token.LISTAR)
                        && !(cu == Token.USUARIO && accion == Token.ELIMINAR);
            case "Cliente":
                return CLIENTE.contains(clave(cu, accion));
            default:
                return false;
        }
    }

    private static String clave(int cu, int accion) {
        return cu + ":" + accion;
    }
}
