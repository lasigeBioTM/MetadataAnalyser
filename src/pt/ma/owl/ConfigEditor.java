package pt.ma.owl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * 
 *
 */
public class ConfigEditor {

	/**
	 * 
	 */
	private JSONObject configuration;
	
	/**
	 * 
	 */
	private String templateFile;
	
	/**
	 * 
	 */
	private String targetFile;

	/**
	 * 
	 * @param template
	 * @param target
	 */
	public ConfigEditor(String template, String target) {
		this.templateFile = template;
		this.targetFile = target;

	}

	/**
	 * 
	 * @return
	 */
	public JSONObject getConfiguration() {
		return configuration;
	}

	/**
	 * 
	 * @return
	 */
	public String getTemplateFile() {
		return templateFile;
	}

	/**
	 * 
	 * @return
	 */
	public String getTargetFile() {
		return targetFile;
	}

	/**
	 * 
	 * @param configuration
	 */
	public void writeConfiguration() {
		//
		writeJSONtoFile(targetFile);
		
	}

	/**
	 * 
	 * @return
	 */
	public JSONObject readConfiguration() {
		// reads the template configuration file
		JSONObject result = readJSONFromFile(templateFile);
		
		//
		if (result.length() > 0) {
			configuration = result;
		}
		
		return result;
	}

	/**
	 * 
	 * @param parameter
	 * @return
	 */
	public JSONObject getObjectParameter(String parameter) {
		JSONObject result = null;
		
		// returns a JSON object if it exists
		if (configuration.has(parameter)) {
			result = configuration.getJSONObject(parameter);
		}
		
		return null;

	}

	/**
	 * 
	 * @param parameter
	 * @return
	 */
	public JSONArray getArrayParameter(String parameter) {
		JSONArray result = null;

		// returns a JSON object if it exists
		if (configuration.has(parameter)) {
			result = configuration.getJSONArray(parameter);
		}

		return result;
	}

	/**
	 * 
	 * @param parameter
	 */
	public void writeParameter(String key, JSONObject parameter) {
		// writes the given parameter into configuration file
		configuration.put(key, parameter);
	}

	/**
	 * 
	 * @param parameter
	 */
	public void writeParameter(String key,JSONArray parameter) {
		// writes the given parameter into configuration file
		configuration.put(key, parameter);
		
	}

	/**
	 * 
	 * @param file
	 * @return
	 */
	private void writeJSONtoFile(String file) {
		//
		BufferedWriter bWriter = null;
		try {
			//
			File targetFile = new File(file);
			FileOutputStream fileOStream = new FileOutputStream(targetFile);
			bWriter = new BufferedWriter(new OutputStreamWriter(fileOStream));

			// start JSON file
			bWriter.write("{");
			bWriter.newLine();

			// iterate trough all JSON configuration keys
			int elementCounter = 0;
			Iterator<String> keyNames = configuration.keys();
			while(keyNames.hasNext()) {

				// for each key write a pretty print representation
				String objectKey = keyNames.next();
				Object jsonElement = configuration.get(objectKey);
				String jsonPrintOut = null;
				if (jsonElement instanceof JSONObject) {
					JSONObject jsonObject = (JSONObject) jsonElement;
					jsonPrintOut = objectKey + " : " + jsonObject.toString(4);
					
				} else if (jsonElement instanceof JSONArray) {
					JSONArray jsonArray = (JSONArray) jsonElement;
					jsonPrintOut = objectKey + " : " + jsonArray.toString(4);
					
				}
				
				//
				elementCounter++;
				if (elementCounter < configuration.length()) {
					jsonPrintOut += ",";
				}
				
				//
				bWriter.write(jsonPrintOut);
				bWriter.newLine();
								
			}
			
			// end JSON file
			bWriter.write("}");
			
		} catch (Exception e) {
			// TODO: logging action
			System.out.println(e);

		} finally {
			//
			if (bWriter != null) {
				try {
					bWriter.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
	
	/**
	 * 
	 * @param url
	 * @return
	 */
	private JSONObject readJSONFromFile(String file) {
		//
		FileReader fileReader = null;
		BufferedReader bufReader = null;
		JSONObject result = null;

		try {					
			// open buffer stream reader for the given url
			fileReader = new FileReader(file);
			bufReader = new BufferedReader(fileReader);
			
			// fetch the result into a string
			String jsonText = readStreamToString(bufReader);
			
			// parse the result into a JSON array object
			result = new JSONObject(jsonText);

		} catch (JSONException e) {
			// TODO: logging action
			System.out.println(e);
			
		} catch (Exception e) {
			// TODO: logging action
			System.out.println(e);
			
		} finally {
			if (bufReader != null) {
				try {
					fileReader.close();
					bufReader.close();

				} catch (IOException e) {
					// TODO: logging action
				}
			}
		}
		
		//
		return result;
	}

	/**
	 * 
	 * @param stream
	 * @return
	 * @throws IOException
	 */
	private static String readStreamToString(Reader stream) throws IOException {
		StringBuilder sBuilder = new StringBuilder();
		int cp;
		while ((cp = stream.read()) != -1) {
			sBuilder.append((char) cp);
		}
		return sBuilder.toString();
	}
}
