package pt.ma.parse.interfaces;

/**
 * 
 * @author 
 *
 */
public interface IMetaHeader {

	/**
	 * 
	 * @return
	 */
	public String getStudyID();
	
	/**
	 * 
	 * @return
	 */
	public String getCheckSum();
	
	/**
	 * 
	 * @param file
	 */
	public void setMetaFile(byte[] file);
	
}
