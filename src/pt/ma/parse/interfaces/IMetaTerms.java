package pt.ma.parse.interfaces;

import java.util.List;

import pt.ma.parse.MetaTerm;

/**
 * 
 * @author 
 *
 */
public interface IMetaTerms {

	/**
	 * 
	 * @return
	 */
	public List<MetaTerm> getMetaTerms();
	
	/**
	 * 
	 * @param file
	 */
	public void setMetaFile(byte[] file);
	
}
