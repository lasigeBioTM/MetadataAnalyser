package pt.ma.owl;

import org.json.JSONArray;
import org.json.JSONObject;

import pt.ma.owl.object.BioontologyDownloader;

/**
 * 
 * 
 *
 */
public class OWLDownloader {

	/**
	 * 
	 */
	private static String TEMPLATECONFIGFILE = "owlsql-config-template.json";
	
	/**
	 * 
	 */
	private static String TARGETCONFIGFILE = "owlsql-config.json";
	
	/**
	 * 
	 */
	private static String BASEURL = "http://data.bioontology.org/ontologies";
	
	/**
	 * 
	 */
	private static String APIKEY = "c96ea8d0-efe1-48bb-b123-bad6404dce85";
	
	
	
	public static void main(String[] args) {
		
		//
		BioontologyDownloader bInstance = new BioontologyDownloader();
		bInstance.setApiKey(APIKEY);
		bInstance.setBaseURL(BASEURL);
				
		// 
		JSONArray ontologiesIRIs = bInstance.getOntologiesJSONArray();
		
		//
		ConfigEditor eInstance = new ConfigEditor(TEMPLATECONFIGFILE, TARGETCONFIGFILE);
		eInstance.readConfiguration();
		
		//
		String jsonKey = "ontologies";
		JSONArray ontologiesList = eInstance.getArrayParameter(jsonKey);
		for (int index = 0; index < ontologiesIRIs.length(); index++) {
			JSONObject ontology = ontologiesIRIs.getJSONObject(index);
			String ontologyIRI = ontology.getString("download");
			ontologiesList.put(ontologyIRI);
		}
		
		//
		eInstance.writeParameter(jsonKey, ontologiesList);
		
		//
		eInstance.writeConfiguration();


	}

}
