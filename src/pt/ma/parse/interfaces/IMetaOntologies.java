package pt.ma.parse.interfaces;

import java.util.List;

import pt.ma.metadata.MetaOntology;

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
	public List<MetaOntology> getMetaOntologies();

	/**
	 * 
	 * @param file
	 */
	public void setMetaFile(byte[] file);
	
}
