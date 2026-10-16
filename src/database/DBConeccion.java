package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import static negociodato.utils.Constantes.*;

/**
 * Abre una conexión nueva a PostgreSQL. Quien la usa la cierra (try-with-resources).
 */
public class DBConeccion {

    private static final String URL = "jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;

    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL, DB_USER, DB_PASSWORD);
    }
}
