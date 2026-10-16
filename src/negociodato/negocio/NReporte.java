package negociodato.negocio;

import java.sql.SQLException;
import java.util.List;
import negociodato.dato.DReporte;

/** CU8 - Reportes y estadísticas. */
public class NReporte {

    public List<String[]> redes() throws SQLException {
        return DReporte.redes();
    }

    public List<String[]> tipos() throws SQLException {
        return DReporte.tipos();
    }

    public List<String[]> mensual() throws SQLException {
        return DReporte.mensual();
    }

    public List<String[]> paginas() throws SQLException {
        return DReporte.paginas();
    }

    public List<String[]> cotizaciones() throws SQLException {
        return DReporte.cotizaciones();
    }
}
