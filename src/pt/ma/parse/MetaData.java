package pt.ma.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 
 * 
 *
 */
public class MetaData {

	/**
	 * 
	 */
	private String studyID;
	
	/**
	 * 
	 */
	private UUID parseID;
	
	/**
	 * 
	 */
	private String checkSum;
	
	/**
	 * 
	 */
	private int checkCount;
	
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
	private ParseObjective parseObjective;
	
	/**
	 * 
	 */
	private List<Ontology> ontologies;
	
	/**
	 * 
	 */
	private List<MetaClass> metaClasses;

	/**
	 * 
	 */
	private List<MetaTerm> metaTerms;

	/**
	 * 
	 * @param studyID
	 * @param parseObjective
	 */
	public MetaData(
			String studyID, 
			ParseObjective parseObjective) {
		super();
		
		//
		this.studyID = studyID;
		this.parseID = UUID.randomUUID();	// TODO: APROVEITAR O UUID DA PROXY
		this.parseDate = System.currentTimeMillis();
		this.parseDuration = 0;
		this.parseObjective = parseObjective;
		this.checkCount = 0;
		
		//
		metaClasses = new ArrayList<MetaClass>();
		
		//
		metaTerms = new ArrayList<MetaTerm>();
		
		//
		ontologies = new ArrayList<Ontology>();
		
	}

	/**
	 * 
	 * @return
	 */
	public boolean isReady() {
		boolean result = false;
		
		//
		switch (parseObjective) {
			case CONCEPTANALYSIS:
				
				break;
				
			case METADATAANALYSIS:
				if (studyID.length() > 0) {
					if (metaClasses.size() > 0 && metaTerms.size() > 0) {
						parseDuration = (long)(System.currentTimeMillis() - parseDate);
						result = true;
					}
				}
				break;
	
			default:
				result = false;
				break;
		}
		
		//
		checkCount++;
		return result;
	}
	
	/**
	 * 
	 * @param metaClass
	 * @return
	 */
	public MetaData addOntology(Ontology ontology) {
		ontologies.add(ontology);
		return this;
		
	}
	
	/**
	 * 
	 * @param ontologies
	 * @return
	 */
	public MetaData addOntologies(List<Ontology> ontologies){
		ontologies.addAll(ontologies);
		return this;
	}
	
	/**
	 * 
	 * @return
	 */
	public List<Ontology> getOntologies() {
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
	 * @param metaTerm
	 * @return
	 */
	public MetaData addMetaTerm(MetaTerm metaTerm) {
		metaTerms.add(metaTerm);
		return this;
		
	}
	
	/**
	 * 
	 * @param terms
	 * @return
	 */
	public MetaData addMetaTerms(ArrayList<MetaTerm> terms) {
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
	 * @return
	 */
	public UUID getUniqueID() {
		return parseID;
	}
	
	/**
	 * 
	 * @return
	 */
	public ParseObjective getParseObjective() {
		return parseObjective;
	}
	
}
