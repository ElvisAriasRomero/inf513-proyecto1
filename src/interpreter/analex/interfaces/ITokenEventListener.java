package interpreter.analex.interfaces;

import interpreter.events.TokenEvent;

/**
 * Un método por caso de uso.
 */
public interface ITokenEventListener {

    void help(TokenEvent event);

    void rol(TokenEvent event);          // CU1

    void usuario(TokenEvent event);      // CU1

    void cuenta(TokenEvent event);       // CU2

    void servicio(TokenEvent event);     // CU3

    void cotizacion(TokenEvent event);   // CU3

    void detalle(TokenEvent event);      // CU3

    void cuota(TokenEvent event);        // CU3

    void material(TokenEvent event);     // CU4

    void pauta(TokenEvent event);        // CU5

    void pagina(TokenEvent event);       // CU6

    void metrica(TokenEvent event);      // CU7

    void reporte(TokenEvent event);      // CU8

    void error(TokenEvent event);
}
