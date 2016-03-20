package pt.ma.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;


/**
 * This class represents a oracle database connection factory
 *
 */
public class MySQLFactory {

	// oracle database connection parameters
	private final String DEFAULT_DRIVER = "com.mysql.jdbc.Driver";
	private final String DEFAULT_URL = "jdbc:mysql://127.0.0.1/owltosql";
	private final String DEFAULT_USER = "admintIR128q";
	private final String DEFAULT_PASSWORD = "A9tRMiU-wEW1";
	
	private Connection connection;
	private String url = DEFAULT_URL;
	private String user = DEFAULT_USER;
	private String password = DEFAULT_PASSWORD;
	private String driver = DEFAULT_DRIVER;
	
	private Properties LoginParms = new Properties();		
	
	/**
	 * 
	 * @return
	 * @throws Exception 
	 * @throws SQLException 
	 */
	public Connection getConnection() throws SQLException, Exception {
		// create a new database connection
		if (connection == null || connection.isClosed()) {
			connection = createConnection();
		}
		return connection;
	}
	
	/**
	 * 
	 * @param driver
	 */
	public void setDriver(String driver) {
		this.driver = driver;
	}
	
	/**
	 * 
	 * @param url
	 */
	public void setURL (String url) {
		this.url = url;
	}
	
	/**
	 * 
	 * @return
	 */
	public static MySQLFactory getInstance() {
		return new MySQLFactory();
	}
	
	/**
	 * 
	 * @param user
	 * @param password
	 */
	public void setAuthentication(String user, String password) {
		this.user = user;
		this.password = password;
	}
	
	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	private Connection createConnection() throws Exception {
		Class.forName(driver).newInstance();
		return DriverManager.getConnection(url, user, password);
	}

	/**
	 * 
	 */
	public void close() {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException ex) {
				//ignore
			}
		}
	}
	
}
