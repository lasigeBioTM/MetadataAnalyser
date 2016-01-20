package pt.ma.metadata;

import java.io.Serializable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 
 * 
 *
 */
public class MetaOntology implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4673782347291365501L;

	/**
	 * 
	 */
	private String id;
	
	/**
	 * 
	 */
	private String uri;
	
	/**
	 * 
	 */
	private String name;

	/**
	 * 
	 * @param id
	 * @param uri
	 */
	public MetaOntology(String id, String uri) {
		super();
		//
		this.id = id;
		this.uri = uri;
		
	}

	/**
	 * 
	 * @return
	 */
	public String getId() {
		return id;
	}

	/**
	 * 
	 * @return
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		/*
		StringBuilder result = new StringBuilder();
		result.append("--[");
		result.append("ID: " + this.id);
		result.append(", URI: " + this.uri);
		result.append(", Name: " + this.name);
		result.append("]--");
		return result.toString();
		*/
		
		//
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String result = gson.toJson(this);
		return result;

	}
	
	
	
}
