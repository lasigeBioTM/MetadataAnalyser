package pt.ma.parse.interfaces;

import java.util.List;

import pt.ma.parse.Ontology;

/**
 * 
 * @author 
 *
 */
public interface IMetaOntologies {

	/**
	 * 
	 * @return
	 */
	public List<Ontology> getMetaOntologies();

	/**
	 * 
	 * @param file
	 */
	public void setMetaFile(byte[] file);
	
}
