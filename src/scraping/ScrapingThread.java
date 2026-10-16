package scraping;

import java.util.logging.Level;
import java.util.logging.Logger;
import negociodato.negocio.NMetrica;
import negociodato.negocio.NPagina;
import negociodato.utils.Constantes;

/**
 * Trabaja solo, sin que nadie mande un correo:
 * - cada 6 horas captura las métricas de nuestras publicaciones (CU7);
 * - una vez al día busca páginas externas (CU6).
 */
public class ScrapingThread implements Runnable {

    @Override
    public void run() {
        for (long vuelta = 0; ; vuelta++) {
            try {
                System.out.println("Scraping: " + new NMetrica().capturar());
                if (vuelta % Constantes.CAPTURAS_POR_ESCANEO == 0) {
                    System.out.println("Scraping: " + new NPagina().escanear(Constantes.PALABRA_CLAVE));
                }
            } catch (Exception ex) {
                Logger.getLogger(ScrapingThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                Thread.sleep(Constantes.HORAS_ENTRE_CAPTURAS * 3_600_000L);
            } catch (InterruptedException ex) {
                return;
            }
        }
    }
}
