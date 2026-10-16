package negociodato.negocio;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import negociodato.dato.DCuenta;
import negociodato.dato.DPagina;
import scraping.Scraper;
import static negociodato.utils.Params.*;

/** CU6 - Detección de páginas externas que usan el nombre de la universidad. */
public class NPagina {

    private static final String[][] REDES = {
        {"Facebook", "facebook.com"}, {"Instagram", "instagram.com"}, {"TikTok", "tiktok.com"}, {"YouTube", "youtube.com"}
    };

    /**
     * Busca la palabra clave en cada red y guarda las páginas que no son cuentas oficiales.
     * Devuelve el resumen para el correo.
     */
    public String escanear(String palabra) throws SQLException {
        List<String> propias = new ArrayList<>();
        for (String[] f : DCuenta.urls()) {
            propias.add(Scraper.normalizar(f[0]));
        }

        int nuevas = 0;
        List<String> sinRespuesta = new ArrayList<>();
        for (String[] red : REDES) {
            try {
                for (String[] pagina : Scraper.buscarPaginas(red[1], palabra)) { // {nombre, url, seguidores}
                    if (!esPropia(Scraper.normalizar(pagina[1]), propias)) {
                        nuevas += DPagina.guardar(red[0], pagina[0], pagina[1], palabra, Long.parseLong(pagina[2]));
                    }
                }
                Thread.sleep(2000); // pausa entre búsquedas para no ser bloqueados
            } catch (java.io.IOException e) {
                sinRespuesta.add(red[0]);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        String resumen = "Búsqueda de \"" + palabra + "\": " + nuevas + " páginas nuevas sospechosas. "
                + "Pendientes de revisar: " + DPagina.pendientes() + ".";
        return sinRespuesta.isEmpty() ? resumen : resumen + " Sin respuesta de: " + String.join(", ", sinRespuesta) + ".";
    }

    /** La cuenta oficial misma o cualquier publicación dentro de ella. */
    public static boolean esPropia(String url, List<String> propias) {
        for (String propia : propias) {
            if (url.equals(propia) || url.startsWith(propia + "/")) {
                return true;
            }
        }
        return false;
    }

    /** Registro manual: [red_social; nombre_pagina; url; palabra_clave; seguidores] */
    public void guardar(List<String> p) throws SQLException {
        String url = url(p, 2);
        existe(DPagina.guardar(opcion(p, 0, NCuenta.REDES), texto(p, 1), "https://" + Scraper.normalizar(url),
                texto(p, 3), opcional(p, 4) == null ? 0 : Long.parseLong(texto(p, 4))), "Esa página ya estaba registrada");
    }

    public void denunciar(Sesion s, List<String> p) throws SQLException {
        existe(DPagina.revisar(entero(p, 0), "denunciada", s.id), "La página no existe o ya fue revisada");
    }

    public void descartar(Sesion s, List<String> p) throws SQLException {
        existe(DPagina.revisar(entero(p, 0), "descartada", s.id), "La página no existe o ya fue revisada");
    }

    public List<String[]> listar() throws SQLException {
        return DPagina.listar();
    }
}
