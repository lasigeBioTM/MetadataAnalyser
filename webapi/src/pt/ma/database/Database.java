package pt.ma.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;




/**
 * 
 * 
 *
 */
public class Database {

	/**
	 * 
	 */
	protected static Database instance;

	/**
	 * 
	 */
	private Connection connection;

	/**
	 * 
	 * @return
	 * @throws SQLException
	 */
	public static Database getInstance() {
		if (instance == null) {
			instance = new Database();
		}
		//
		return instance;
	}

	/**
	 * @throws SQLException
	 * 
	 */
	protected Database() {
		// initialises connection object
		connection = null;

	}

	/**
	 * 
	 * @return
	 */
	public boolean hasDBConnection() {
		// gets connection singleton
		connection = MySQLLogin.getConnection();
		// return result
		return (boolean) (connection != null);
	}

	/**
	 * 
	 * DATABASE SELECT BLOCK (USED FOR WS RETRIEVE DATA FROM DATABASE)
	 * 
	 */

	/**
	 * 
	 * @return
	 * @throws SQLException
	 */
	public ResultSet getSP(String conceptIRI) throws SQLException {
		ResultSet resultSet = null;
		
		// get connection prepared statement
		connection = MySQLLogin.getConnection();

		// build the query statement
		StringBuilder query = new StringBuilder();
		query.append("CALL sp_conceptspec('" + conceptIRI + "');");
		// run the database query
		CallableStatement cs = connection.prepareCall(query.toString()); 
		boolean result = cs.execute();
		if (result) {
			resultSet = cs.getGeneratedKeys();
		}
		

		return resultSet;

	}


}
