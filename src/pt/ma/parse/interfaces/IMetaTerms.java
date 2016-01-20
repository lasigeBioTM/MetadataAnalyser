package pt.ma.parse.interfaces;

import java.util.List;

import pt.ma.metadata.MetaClass;
import pt.ma.metadata.MetaTerm;

/**
 * 
 * @author 
 *
 */
public interface IMetaTerms {

	/**
	 * 
	 * @param metaClass
	 * @return
	 */
	public List<MetaTerm> getMetaTerms(MetaClass metaClass);
	
	/**
	 * 
	 * @param file
	 */
	public void setMetaFile(byte[] file);
	
}
