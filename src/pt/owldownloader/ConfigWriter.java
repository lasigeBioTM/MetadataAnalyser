package pt.owldownloader;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 
 * 
 *
 */
public class ConfigWriter {

	/**
	 * 
	 */
	private String configFile;

	/**
	 * 
	 * @param configFile
	 */
	public ConfigWriter(String configFile) {
		this.configFile = configFile;

	}

	/**
	 * 
	 * @return
	 */
	public String getConfigFile() {
		return configFile;
	}

	/**
	 * 
	 * @param configuration
	 */
	public void writeConfiguration() {

	}

	/**
	 * 
	 * @return
	 */
	public JSONObject readConfiguration() {
		JSONObject result = null;

		return result;
	}

	/**
	 * 
	 * @param parameter
	 * @return
	 */
	public JSONObject readObjectParameter(String parameter) {
		JSONObject result = null;
		
		return null;

	}

	/**
	 * 
	 * @param parameter
	 * @return
	 */
	public JSONArray readArrayParameter(String parameter) {
		JSONArray result = null;
		
		return result;
	}

	/**
	 * 
	 * @param parameter
	 */
	public void writeParameter(JSONObject parameter) {
		
	}

	/**
	 * 
	 * @param parameter
	 */
	public void writeParameter(JSONArray parameter) {
		
	}

}
