package pt.ma.metadata;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import pt.ma.util.StringWork;

/**
 * 
 * 
 *
 */
public class MetaData {

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
	private String checkSum;
		
	/**
	 * 
	 */
	private long parseDate;
	
	/**
	 * 
	 */
	private long parseDuration;
		
	/**
	 * 
	 */
	private List<MetaOntology> ontologies;
	
	/**
	 * 
	 */
	private List<MetaClass> metaClasses;

	/**
	 * 
	 */
	private MetaObjective parseObjective;

	/**
	 * 
	 * @param studyID
	 * @param parseObjective
	 */
	public MetaData(
			String id,
			UUID uniqueID,
			String[] classNames,
			MetaObjective parseObjective) {
		super();
		
		//
		this.id = id;
		this.uniqueID = uniqueID;
		this.parseDate = System.currentTimeMillis();
		this.parseDuration = 0;
		this.parseObjective = parseObjective;
		
		//
		ontologies = new ArrayList<MetaOntology>();

		//
		metaClasses = new ArrayList<MetaClass>();
						
		//
		buildMetaClassCollection(classNames);
		
	}

	/**
	 * 
	 * @param classNames
	 */
	public void buildMetaClassCollection(String[] classNames) {
		//
		int index = 1;
		for (String name : classNames) {
			String classID = name + "_" + String.valueOf(index);
			MetaClass metaClass = new MetaClass(classID, name);
			metaClasses.add(metaClass);
			
		}
		
	}
	
	/**
	 * 
	 * @param metaClass
	 * @return
	 */
	public MetaData addOntology(MetaOntology ontology) {
		ontologies.add(ontology);
		return this;
		
	}
	
	/**
	 * 
	 * @param ontologies
	 * @return
	 */
	public MetaData addOntologies(List<MetaOntology> ontologies){
		this.ontologies.addAll(ontologies);
		return this;
	}
	
	/**
	 * 
	 * @return
	 */
	public List<MetaOntology> getOntologies() {
		return ontologies;
		
	}
	
	/**
	 * 
	 * @param metaClass
	 * @return
	 */
	public MetaData addMetaClass(MetaClass metaClass) {
		metaClasses.add(metaClass);
		return this;
		
	}
	
	/**
	 * 
	 * @param classes
	 * @return
	 */
	public MetaData addMetaClasses(ArrayList<MetaClass> classes) {
		metaClasses.addAll(classes);
		return this;
	}
	
	/**
	 * 
	 * @return
	 */
	public List<MetaClass> getMetaClasses() {
		return metaClasses;
		
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
	public MetaObjective getParseObjective() {
		return parseObjective;
	}

	/**
	 * 
	 * @return
	 */
	public String getCheckSum() {
		return checkSum;
	}

	/**
	 * 
	 * @param checkSum
	 * @throws NoSuchAlgorithmException 
	 */
	public void setCheckSum(byte[] metaFile) throws NoSuchAlgorithmException {
		//
		try {
			checkSum = StringWork.makeSHA1Hash(metaFile);
			
		} catch (UnsupportedEncodingException e) {
			// TODO log action

		}
		
	}

	@Override
	public String toString() {		
		/*
		StringBuilder result = new StringBuilder();
		result.append("--[MetaData Result: \n");
		result.append("\tID: " + this.id + "\n");
		result.append("\tUniqueID: " + this.uniqueID + "\n");
		result.append("\tCheckSum: " + this.checkSum + "\n");
		result.append("\tCheckCount: " + this.checkCount + "\n");
		result.append("\tParseDate: " + this.parseDate + "\n");
		result.append("\tParseDuration: " + this.parseDuration + " (miliseconds)\n");
		result.append("\tOntologies: (" + this.ontologies.size() + ")" + "\n");
		for (MetaOntology metaOntology : this.ontologies) {
			result.append("\t\t" + metaOntology + "\n");	
		}
		result.append("\tClasses: (" + this.metaClasses.size() + ")" + "\n");
		for (MetaClass metaClass : this.metaClasses) {
			result.append("\t" + metaClass + "\n");	
		}
		result.append("]--");
		//
		return result.toString();
		*/
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String result = gson.toJson(this);
		return result;

	}

}
