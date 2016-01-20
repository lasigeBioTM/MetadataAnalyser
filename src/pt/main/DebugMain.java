package pt.main;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

import pt.ma.database.MySQLLogin;

public class DebugMain {

	public static void main(String[] args) throws SQLException {
		
		/**
		 * "http://data.bioontology.org/ontologies/NCBITAXON/download?apikey=c96ea8d0-efe1-48bb-b123-bad6404dce85",
		"http://data.bioontology.org/ontologies/CHMO/download?apikey=c96ea8d0-efe1-48bb-b123-bad6404dce85",
		"http://data.bioontology.org/ontologies/OBI/download?apikey=c96ea8d0-efe1-48bb-b123-bad6404dce85"
		"http://data.bioontology.org/ontologies/MS/download?apikey=c96ea8d0-efe1-48bb-b123-bad6404dce85",
		"http://data.bioontology.org/ontologies/BTO/download?apikey=c96ea8d0-efe1-48bb-b123-bad6404dce85",				"http://data.bioontology.org/ontologies/UO/download?apikey=c96ea8d0-efe1-48bb-b123-bad6404dce85"
		 */
/*		Connection database = MySQLLogin.getConnection();
		CallableStatement statement = database.prepareCall("{call sp_conceptspec(?, ?)}");
		
		String conceptIRI = "http://www.ebi.ac.uk/efo/EFO_0001779";
		statement.setString("concept_iri", conceptIRI);
		statement.registerOutParameter("spec_value", Types.NUMERIC);
		statement.execute();
		double specValue = (double)statement.getDouble("spec_value");
		
		System.out.println(specValue);
*/		
		
		
		double avgClassSpec = (double)(
				Double.valueOf(0.2f) / 
				Double.valueOf(0));

		System.out.println(avgClassSpec);
		
		
	}
}
