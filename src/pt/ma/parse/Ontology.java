package pt.ma.parse;

/**
 * 
 * @author Bruno
 *
 */
public class Ontology {
	
	private String id;
	
	private String uri;
	
	private String name;

	/**
	 * 
	 * @param uri
	 */
	public Ontology(String uri) {
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
	
}
