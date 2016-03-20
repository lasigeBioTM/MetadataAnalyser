package pt.ma.component.owl;

import java.util.ArrayList;

/**
 * 
 * @author Bruno
 *
 */
public class OntologyList {

	/**
	 * 
	 */
	private ArrayList<String> ontologies;
	
	/**
	 * 
	 * @param ontologies
	 */
	public OntologyList(ArrayList<String> ontologies) {
		this.ontologies = ontologies;
		
	}

	/**
	 * 
	 * @return
	 */
	public ArrayList<String> getOntologies() {
		return ontologies;
	}

	/**
	 * 
	 * @param ontologies
	 */
	public void setOntologies(ArrayList<String> ontologies) {
		this.ontologies = ontologies;
	}

}
