package negociodato.negocio;

import java.sql.SQLException;
import java.util.List;
import negociodato.dato.DMetrica;
import negociodato.dato.DPauta;
import scraping.Scraper;
import static negociodato.utils.Params.*;

/** CU7 - Métricas de nuestras publicaciones. */
public class NMetrica {

    /** Entra a cada publicación de los últimos días y guarda una captura nueva. Devuelve el resumen. */
    public String capturar() throws SQLException {
        List<String[]> pautas = DPauta.paraCapturar(); // {id, url}
        int capturadas = 0;
        for (String[] pauta : pautas) {
            try {
                long[] m = Scraper.leerMetricas(Scraper.descargar(pauta[1]));
                if (m != null) {
                    capturadas += DMetrica.guardar(Integer.parseInt(pauta[0]), m[0], m[1], m[2], m[3]);
                }
            } catch (java.io.IOException e) {
                System.err.println("Scraping: sin respuesta de " + pauta[1]);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return "Métricas capturadas: " + capturadas + " de " + pautas.size() + " publicaciones."
                + (capturadas < pautas.size() ? " Las demás no muestran sus números públicamente; "
                + "se pueden registrar con: INSMET[...]" : "");
    }

    /** Registro manual: [pauta_id; vistas; reacciones; comentarios; compartidos] */
    public void guardar(List<String> p) throws SQLException {
        DMetrica.guardar(entero(p, 0), entero(p, 1), entero(p, 2), entero(p, 3), entero(p, 4));
    }

    public List<String[]> listar(List<String> p) throws SQLException {
        return DMetrica.listar(entero(p, 0));
    }
}
