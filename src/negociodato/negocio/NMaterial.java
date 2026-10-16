package negociodato.negocio;

import java.sql.SQLException;
import java.util.List;
import negociodato.dato.DMaterial;
import static negociodato.utils.Params.*;

/** CU4 - Carga y validación del material publicitario (el archivo se envía como enlace). */
public class NMaterial {

    /** [cotizacion_id; titulo; enlace_del_archivo; imagen|video] */
    public String guardar(Sesion s, List<String> p) throws SQLException {
        int cotizacion = entero(p, 0);
        NCotizacion.verificarAcceso(s, cotizacion);
        return existe(DMaterial.guardar(cotizacion, texto(p, 1), url(p, 2), opcion(p, 3, "imagen", "video")),
                "La cotización todavía no tiene ningún pago registrado");
    }

    /** Sin parámetros: todos (el cliente solo ve los suyos). [cotizacion_id]: los de esa cotización. */
    public List<String[]> listar(Sesion s, List<String> p) throws SQLException {
        String cotizacion = opcional(p, 0);
        if (cotizacion != null) {
            NCotizacion.verificarAcceso(s, Integer.parseInt(cotizacion));
            return DMaterial.listarDeCotizacion(Integer.parseInt(cotizacion));
        }
        return s.esCliente() ? DMaterial.listarDeCliente(s.id) : DMaterial.listar();
    }

    public void aprobar(Sesion s, List<String> p) throws SQLException {
        existe(DMaterial.validar(entero(p, 0), "aprobado", opcional(p, 1), s.id),
                "El material no existe o ya fue revisado");
    }

    /** [id; observacion] - la observación es obligatoria para que el cliente sepa qué corregir. */
    public void rechazar(Sesion s, List<String> p) throws SQLException {
        existe(DMaterial.validar(entero(p, 0), "rechazado", texto(p, 1), s.id),
                "El material no existe o ya fue revisado");
    }
}
