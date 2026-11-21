package main;

import interpreter.analex.utils.Token;
import java.util.Set;

/**
 * Qué puede hacer cada rol. El rol sale del email de quien envía el comando.
 *  - Director: todo. Es el único que crea usuarios con rol, los modifica, les asigna rol o los elimina.
 *  - Encargado: todo lo operativo; de usuarios y roles solo puede listar.
 *  - Cliente (virtual): pedir un servicio (SOLCOT, al contado y ya aprobado), pagarlo con QR, subir su material
 *    y ver sus cotizaciones, cuotas y material. No agrega ni quita servicios: para otro pedido crea otra cotización.
 *  - Visitante (correo no registrado): HELP y registrarse solo (queda como Cliente).
 */
public class Permisos {

    private static final Set<String> CLIENTE = Set.of(
            clave(Token.SERVICIO, Token.LISTAR),
            clave(Token.COTIZACION, Token.LISTAR),
            clave(Token.COTIZACION, Token.VER),
            clave(Token.COTIZACION, Token.SOLICITAR),   // pide un servicio: ya aprobada, al contado, solo QR
            clave(Token.DETALLE, Token.LISTAR),
            clave(Token.CUOTA, Token.LISTAR),
            clave(Token.CUOTA, Token.QR),
            clave(Token.CUOTA, Token.VER),
            clave(Token.MATERIAL, Token.AGREGAR),
            clave(Token.MATERIAL, Token.LISTAR));

    public static boolean permitido(String rol, int cu, int accion) {
        if (cu == Token.HELP) {
            return true;
        }
        if (rol == null) {
            return cu == Token.USUARIO && accion == Token.REGISTRAR;
        }
        switch (rol) {
            case "Director":
                return true;
            case "Encargado":
                return !((cu == Token.ROL || cu == Token.USUARIO) && accion != Token.LISTAR);
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
