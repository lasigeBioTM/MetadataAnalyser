package pt.ma.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 
 * 
 *
 */
public class MetaClass implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7334940448848314424L;

	/**
	 * 
	 */
	private String id;
	
	/**
	 * Class unique identifier
	 */
	private UUID uniqueID;
	
	/**
	 * 
	 */
	private String name;
	
	/**
	 * Specificity value for the entire class
	 */
	private double specValue;
	
	/**
	 * Coverage value for the entire class
	 */
	private double covValue;

	/**
	 * Class annotations collection
	 */
	private List<MetaAnnotation> metaAnnotations;

	/**
	 * Classe terms collection
	 */
	private List<MetaTerm> metaTerms;

	/**
	 * 
	 * @param id
	 * @param name
	 */
	public MetaClass(
			String id, 
			String name) {
		super();
		
		//
		this.id = id;
		this.uniqueID = UUID.randomUUID();
		this.name = name;
		this.specValue = 0f;
		this.covValue = 0f;

		// instantiate annotation collection data structure
		metaAnnotations = new ArrayList<MetaAnnotation>();

		// instantiate term collection data structure
		metaTerms = new ArrayList<MetaTerm>();

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
	 * @param metaAnnotation
	 * @return
	 */
	public MetaClass addMetaAnnotation(MetaAnnotation metaAnnotation) {
		metaAnnotations.add(metaAnnotation);
		return this;
		
	}

	/**
	 * 
	 * @param id
	 * @param url
	 * @return
	 */
	public MetaAnnotation addMetaAnnotation(String id, String url) {
		//
		MetaAnnotation metaAnnotation = new MetaAnnotation(id, url);
		metaAnnotations.add(metaAnnotation);
		//
		return metaAnnotation;
	}

	/**
	 * 
	 * @param terms
	 * @return
	 */
	public MetaClass addMetaAnnotations(ArrayList<MetaAnnotation> annotations) {
		metaAnnotations.addAll(metaAnnotations);
		return this;
	}
	
	/**
	 * 
	 * @return
	 */
	public List<MetaAnnotation> getMetaAnnotations() {
		return metaAnnotations;
		
	}
		
	/**
	 * 
	 */
	public void removeAllAnnotations() {
		metaAnnotations = new ArrayList<MetaAnnotation>();
		
	}
	
	/**
	 * 
	 * @param metaTerm
	 * @return
	 */
	public MetaClass addMetaTerm(MetaTerm metaTerm) {
		metaTerms.add(metaTerm);
		return this;
		
	}
	
	/**
	 * 
	 * @param terms
	 * @return
	 */
	public MetaClass addMetaTerms(ArrayList<MetaTerm> terms) {
		metaTerms.addAll(metaTerms);
		return this;
	}
	
	/**
	 * 
	 * @return
	 */
	public List<MetaTerm> getMetaTerms() {
		return metaTerms;
		
	}
	
	/**
	 * 
	 */
	public void removeAllTerms() {
		metaTerms = new ArrayList<MetaTerm>();
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isEvaluated() {
		boolean result = false;
		if (specValue >= 0 && covValue >= 0) {
			result = true;
		}
		
		return result;
	}
	
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
	public double getCovValue() {
		return covValue;
	}

	/**
	 * 
	 * @param covValue
	 */
	public void setCovValue(double covValue) {
		this.covValue = covValue;
	}

	/**
	 * 
	 * @return
	 */
	public String getClassID() {
		return id;
	}

	/**
	 * 
	 * @return
	 */
	public String getClassName() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uniqueID == null) ? 0 : uniqueID.hashCode());
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
		MetaClass other = (MetaClass) obj;
		if (uniqueID == null) {
			if (other.uniqueID != null)
				return false;
		} else if (!uniqueID.equals(other.uniqueID))
			return false;
		return true;
	}

	@Override
	public String toString() {
		/*
		StringBuilder result = new StringBuilder();
		result.append("--[");
		result.append("\tID: " + this.id + "\n");
		result.append("\tUniqueID: " + this.uniqueID + "\n");
		result.append("\tName: " + this.name + "\n");
		result.append("\tSpecValue: " + this.specValue + "\n");
		result.append("\tCovrValue: " + this.covValue + "\n");
		result.append("\tAnnotations: (" + this.metaAnnotations.size() + ")" + "\n");
		for (MetaAnnotation metaAnnotation : this.metaAnnotations) {
			result.append("\t\t\t" + metaAnnotation + "\n");	
		}
		result.append("\tTerms: (" + this.metaTerms.size() + ")" + "\n");
		for (MetaTerm metaTerm : this.metaTerms) {
			result.append("\t\t\t" + metaTerm + "\n");	
		}
		result.append("\t]--");
		return result.toString();
		*/
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String result = gson.toJson(this);
		return result;
	}

	
}
