package pt.ma.parse.interfaces;

import java.util.List;

import pt.ma.parse.MetaClass;

/**
 * 
 * @author 
 *
 */
public interface IMetaClasses {

	/**
	 * 
	 * @return
	 */
	public List<MetaClass> getMetaClasses();
	
	/**
	 * 
	 * @param file
	 */
	public void setMetaFile(byte[] file);
	
}
