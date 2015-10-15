package pt.owldownloader.object;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;

public abstract class ADownloader {

	/**
	 * 
	 * @param apiKey
	 */
	public abstract void setApiKey(String apiKey);
	
	/**
	 * 
	 * @param url
	 */
	public abstract void setBaseURL(String url);

	/**
	 * 
	 * @return JSONArray
	 */
	public abstract JSONArray getOntologiesList();
	
	/**
	 * 
	 * @param acronym
	 * @return JSONArray
	 */
	public abstract JSONArray getOntologyOWL(String acronym);
	
	/**
	 * 
	 * @param url
	 * @return
	 */
	protected JSONArray readJSONFromURL(String url) {
		//
		InputStream inStream = null;
		InputStreamReader inSReader = null;
		BufferedReader bufReader = null;
		JSONArray result = null;

		try {
			// configure the connection to be made
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpGet getRequest = new HttpGet(url);
			getRequest.addHeader("accept", "application/json");
			HttpResponse response = httpClient.execute(getRequest);
					
			// open buffer stream reader for the given url
			inStream = response.getEntity().getContent();
			inSReader = new InputStreamReader(inStream, Charset.forName("UTF-8"));
			bufReader = new BufferedReader(inSReader);
			
			// fetch the result into a string
			String jsonText = readStreamToString(bufReader);
			
			// parse the result into a JSON array object
			result = new JSONArray(jsonText);

		} catch (JSONException e) {
			// TODO: logging action
			System.out.println(e);
			
		} catch (Exception e) {
			// TODO: logging action
			System.out.println(e);
			
		} finally {
			if (bufReader != null) {
				try {
					bufReader.close();
					inSReader.close();
					inStream.close();
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
	public static String readStreamToString(Reader stream) throws IOException {
		StringBuilder sBuilder = new StringBuilder();
		int cp;
		while ((cp = stream.read()) != -1) {
			sBuilder.append((char) cp);
		}
		return sBuilder.toString();
	}
	
}
