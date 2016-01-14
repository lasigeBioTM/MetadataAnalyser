package pt.ma.owl.object;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONObject;

public class BioontologyDownloader extends ADownloader {

	/**
	 * 
	 */
	private String baseURL;

	/**
	 * 
	 */
	private String apiKey;

	@Override
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	@Override
	public void setBaseURL(String url) {
		this.baseURL = url;
	}

	@Override
	public JSONArray getOntologiesJSONArray() {
		JSONArray result = null;

		//
		String queryApiKey = null;
		try {
			queryApiKey = "?apikey=" + URLEncoder.encode(apiKey, "UTF-8");

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		//
		JSONArray ontologies = readJSONFromURL(baseURL + queryApiKey);
		if (ontologies.length() > 0) {
			//
			result = new JSONArray();

			// iterate trough all ontologies in the array
			for (int index = 0; index < ontologies.length(); index++) {
				JSONObject ontology = ontologies.getJSONObject(index);
				JSONObject ontologyLinks = ontology.getJSONObject("links");

				// for each one read id, acronym, name and download address
				String downloadURL = ontologyLinks.getString("download") + queryApiKey;
				JSONObject record = new JSONObject();
				record.put("id", ontology.getString("@id"));
				record.put("acronym", ontology.getString("acronym"));
				record.put("name", ontology.getString("name"));
				record.put("download", downloadURL);

				// add this new ontology to the result
				result.put(record);

			}
		}

		return result;
	}

	@Override
	public JSONArray getOntologyOWL(String acronym) {
		// TODO Auto-generated method stub
		return null;
	}

}
