package pt.ma.config;

/**
 * 
 * 
 *
 */
public class ConfigObject {
	
	/**
	 * 
	 */
	private boolean verbose;
	
	/**
	 * 
	 */
	private boolean installdb;
	
	/**
	 * 
	 */
	private int sourceport;

	/**
	 * 
	 * @return
	 */
	public boolean isVerbose() {
		return verbose;
	}

	/**
	 * 
	 * @param verbose
	 */
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isInstalldb() {
		return installdb;
	}

	/**
	 * 
	 * @param installdb
	 */
	public void setInstalldb(boolean installdb) {
		this.installdb = installdb;
	}

	/**
	 * 
	 * @return
	 */
	public int getSourceport() {
		return sourceport;
	}

	/**
	 * 
	 * @param sourceport
	 */
	public void setSourceport(int sourceport) {
		this.sourceport = sourceport;
	}
	
}
