package pt.ma.parse.interfaces;

import java.util.List;

import pt.ma.metadata.MetaAnnotation;
import pt.ma.metadata.MetaClass;

/**
 * 
 * @author 
 *
 */
public interface IMetaAnnotations {

	/**
	 * 
	 * @param metaClass
	 * @return
	 */
	public List<MetaAnnotation> getMetaAnnotations(MetaClass metaClass);
	
	/**
	 * 
	 * @param file
	 */
	public void setMetaFile(byte[] file);
	
}
