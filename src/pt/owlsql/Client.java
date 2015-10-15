package pt.owlsql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import pt.json.JSONException;


public class Client {
    
    private static Connection connection;
    
    
    public static void connect(String configFilename) throws IOException, JSONException, SQLException {
        // Read the configuration file
        Config.read(configFilename);
        
        // Connect to the database and instantiate the necessary extractors
        connection = Config.connectToDatabase();
    }
    
    
    public static Connection getConnection() {
        if (connection != null)
            return connection;
        else
            throw new RuntimeException("Not connected to the database");
    }
}