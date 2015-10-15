package pt.owldownloader;

import org.json.JSONArray;

import pt.owldownloader.object.BioontologyDownloader;

/**
 * 
 * 
 *
 */
public class OWLDownloader {

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
		
		JSONArray ontologies = bInstance.getOntologiesList();
		for (int index = 0; index < ontologies.length(); index++) {
			System.out.println(ontologies.get(index));
		}
		
		

	}

}
