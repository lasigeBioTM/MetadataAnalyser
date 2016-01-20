package pt.ma.component.parse;

import java.util.UUID;

import pt.ma.metadata.MetaData;

/**
 * 
 * 
 *
 */
public class ParseJob {

	/**
	 * 
	 */
	private UUID jobID;
	
	/**
	 * 
	 */
	private MetaData metaData;
	
	/**
	 * 1 - header; 2 - ontologies; 3 - annotations; 4 - terms
	 */
	private int parseStatus;
	
	/**
	 * 
	 * @param metaData
	 */
	public ParseJob(MetaData metaData) {
		super();
		//
		this.jobID = UUID.randomUUID();
		this.metaData = metaData;
		this.parseStatus = 0;
		
	}

	/**
	 * 
	 * @param jobID
	 * @param metaData
	 */
	public ParseJob(
			UUID jobID, 
			MetaData metaData) {
		super();
		//
		this.jobID = jobID;
		this.metaData = metaData;
		
	}

	/**
	 * 
	 * @param jobID
	 * @param metaData
	 */
	public ParseJob(
			UUID jobID, 
			MetaData metaData,
			int parseStatus) {
		super();
		//
		this.jobID = jobID;
		this.metaData = metaData;
		this.parseStatus = 3;
		
	}

	/**
	 * 
	 * @param parseStatus
	 */
	public void setParseStatus(int parseStatus) {
		this.parseStatus += parseStatus;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getParseStatus() {
		return this.parseStatus;
	}
	
	/**
	 * 
	 * @return
	 */
	public UUID getJobID() {
		return jobID;
	}

	/**
	 * 
	 * @param jobID
	 */
	public void setJobID(UUID jobID) {
		this.jobID = jobID;
	}

	/**
	 * 
	 * @return
	 */
	public MetaData getMetaData() {
		return metaData;
	}

	/**
	 * 
	 * @param metaData
	 */
	public void setMetaData(MetaData metaData) {
		this.metaData = metaData;
	}

}
