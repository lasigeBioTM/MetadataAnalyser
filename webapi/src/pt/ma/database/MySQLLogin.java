package pt.ma.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;


/**
 * This class represents a oracle database connection singleton factory
 *
 */
public class MySQLLogin {

	// oracle database connection parameters
	private static final String DEFAULT_DRIVER = "com.mysql.jdbc.Driver";
	private static final String DEFAULT_URL = "jdbc:mysql://127.3.70.2/metadataanalyser";
	private static final String DEFAULT_USER = "admintIR128q";
	private static final String DEFAULT_PASSWORD = "A9tRMiU-wEW1";
	private static final MySQLLogin INSTANCE = new MySQLLogin();
	
	private static Connection connection;
	private static String url = DEFAULT_URL;
	private static String user = DEFAULT_USER;
	private static String password = DEFAULT_PASSWORD;
	private static String driver = DEFAULT_DRIVER;
	
	private Properties LoginParms = new Properties();		
	
	/**
	 * 
	 * @return
	 */
	public static Connection getConnection() {
		try {
			if (connection == null || connection.isClosed()) {
				connection = createConnection();
			}
		} catch (Exception ex) {

		}
		return connection;
	}
	
	/**
	 * 
	 * @param driver
	 */
	public void setDriver(String driver) {
		MySQLLogin.driver = driver;
	}
	
	/**
	 * 
	 * @param url
	 */
	public void setURL (String url) {
		MySQLLogin.url = url;
	}
	
	/**
	 * 
	 * @return
	 */
	public static MySQLLogin getInstance() {
		return INSTANCE;
	}
	
	/**
	 * 
	 * @param user
	 * @param password
	 */
	public void setAuthentication(String user, String password) {
		MySQLLogin.user = user;
		MySQLLogin.password = password;
	}
	
	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	private static Connection createConnection() throws Exception {
		Class.forName(driver).newInstance();
		return DriverManager.getConnection(url, user, password);
	}
	
	/**
	 * 
	 * @return
	 * @throws SQLException
	 */
	public Statement getStatment() throws SQLException {
		;
		MySQLLogin.getInstance();
		return MySQLLogin.getConnection().createStatement();
	}
	
	/**
	 * 
	 */
	public static void close() {
		if (MySQLLogin.connection != null) {
			try {
				MySQLLogin.connection.close();
			} catch (SQLException ex) {
				//ignore
			}
		}
	}
	
}
