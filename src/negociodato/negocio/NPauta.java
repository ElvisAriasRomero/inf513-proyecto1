package negociodato.negocio;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import negociodato.dato.DPauta;
import static negociodato.utils.Params.*;

/** CU5 - Programar la pauta en cada red. En una misma cuenta los horarios no pueden cruzarse. */
public class NPauta {

    public static final String[] TIPOS = {"post", "historia", "reel", "en_vivo"};

    /**
     * Post, historia o reel: [material_id; cuenta_id; tipo; AAAA-MM-DD HH:MM]  (ocupa 1 minuto)
     * En vivo:               [material_id; cuenta_id; en_vivo; inicio; fin]
     */
    public String guardar(List<String> p) throws SQLException {
        int material = entero(p, 0);
        int cuenta = entero(p, 1);
        String tipo = opcion(p, 2, TIPOS);
        LocalDateTime inicio = fechaHora(p, 3);
        LocalDateTime fin = tipo.equals("en_vivo") ? fechaHora(p, 4) : inicio.plusMinutes(1);
        if (!fin.isAfter(inicio)) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la de inicio");
        }

        String[] choque = DPauta.choque(cuenta, inicio, fin);
        if (choque != null) {
            throw new IllegalArgumentException("Horario ocupado: esa cuenta ya tiene la pauta " + choque[0]
                    + " (" + choque[1] + ") de " + choque[2] + " a " + choque[3]);
        }
        return existe(DPauta.guardar(material, cuenta, tipo, inicio, fin),
                "El material debe estar aprobado y la cuenta debe existir y estar activa");
    }

    /** [id; enlace_de_la_publicacion] - el scraping de métricas entra a este enlace. */
    public void publicar(List<String> p) throws SQLException {
        existe(DPauta.publicar(entero(p, 0), url(p, 1)), "La pauta no existe o no está programada");
    }

    public void cancelar(List<String> p) throws SQLException {
        existe(DPauta.cancelar(entero(p, 0)), "La pauta no existe o no está programada");
    }

    public List<String[]> listar() throws SQLException {
        return DPauta.listar();
    }
}
