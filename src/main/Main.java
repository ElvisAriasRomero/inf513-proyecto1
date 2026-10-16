package main;

/**
 * Sistema de Publicidad Universitaria - Grupo 13.
 */
public class Main {

    public static void main(String[] args) {
        if (negociodato.utils.Constantes.PASSWORD.isEmpty()) {
            System.err.println("Falta la contraseña del grupo. En PowerShell: $env:GRUPO_PASSWORD=\"...\" y vuelva a ejecutar.");
            return;
        }
        new Aplication().start();
    }
}
