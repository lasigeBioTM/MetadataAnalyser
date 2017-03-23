package pt.ma.metadata;

import java.io.Serializable;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 
 * 
 *
 */
public class MetaAnnotation implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6316590757445585362L;

	/**
	 * 
	 */
	private String id;
	
	/**
	 * 
	 */
	private UUID uniqueID;
	
	/**
	 * 
	 */
	private String uri;
	
	/**
	 * 
	 */
	private String baseAddress;
	
	/**
	 * 
	 */
	private String ontologyAcronym;
	
	/**
	 * 
	 */
	private String conceptIdentifier;
	
	/**
	 * 
	 */
	private double specValue;
	
	/**
	 * 
	 * @param id
	 * @param url
	 */
	public MetaAnnotation (String id, String url) {
		super();
		
		//
		this.id = id;
		this.uniqueID = UUID.randomUUID();
		this.uri = url;
		this.specValue = -1f;	// a default value less than 0 is necessary
		
		//
		setBaseAddress();
		setOntologyAcronym();
		setConceptIdentifier();
		
	}
	
	// PUBLIC  METHODS
	
	/**
	 * 
	 * @return
	 */
	public double getSpecValue() {
		return specValue;
	}
	
	/**
	 * 
	 * @param specValue
	 */
	public void setSpecValue(double specValue) {
		this.specValue = specValue;
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
	public UUID getUniqueID() {
		return uniqueID;
	}

	/**
	 * 
	 * @return
	 */
	public String getURI() {
		return uri;
	}

	/**
	 * 
	 * @return
	 */
	public String getBaseAddress() {
		return baseAddress;
	}

	/**
	 * 
	 * @return
	 */
	public String getOntologyAcronym() {
		return ontologyAcronym;
	}

	/**
	 * 
	 * @return
	 */
	public String getConceptIdentifier() {
		return conceptIdentifier;
	}
	
	
	
	// PRIVATE METHODS
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MetaAnnotation other = (MetaAnnotation) obj;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.toLowerCase().equals(other.uri.toLowerCase()))
			return false;
		return true;
	}

	/**
	 * 
	 */
	private void setBaseAddress() {
		
	}
	
	/**
	 * 
	 */
	private void setOntologyAcronym() {
		
	}
	
	/**
	 * 
	 */
	private void setConceptIdentifier() {
		
	}

	@Override
	public String toString() {
		/*
		StringBuilder result = new StringBuilder();
		result.append("--[");
		result.append("ID: " + this.id);
		result.append(", URI: " + this.uri);
		result.append(", SpecValue: " + this.specValue);		
		result.append(", BaseAddress: " + this.baseAddress);
		result.append(", OntologyAcronym: " + this.ontologyAcronym);
		result.append(", ConceptIdentifier: " + this.conceptIdentifier);		
		result.append(", SpecValue: " + this.specValue);
		result.append("]--");
		return result.toString();
		*/
		
		//
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String result = gson.toJson(this);
		return result;

	}
	
	
}
