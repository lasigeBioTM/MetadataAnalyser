package pt.ma.component.calculus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import pt.ma.metadata.MetaClass;
import pt.ma.metadata.MetaData;

/**
 * 
 * 
 *
 */
public class CalculusJob {

	/**
	 * 
	 */
	private UUID jobID;
	
	/**
	 * 
	 */
	private MetaData metaData;
	
	/**
	 * 
	 */
	private Map<UUID, MetaClass> jobTaskList;
	
	/**
	 * 
	 */
	private boolean taskSet;
	
	/**
	 * 
	 * @param metaData
	 */
	public CalculusJob(MetaData metaData) {
		super();
		//
		this.jobID = UUID.randomUUID();
		this.metaData = metaData;
		this.taskSet = false;
		jobTaskList = new HashMap<UUID, MetaClass>();
		
	}

	/**
	 * 
	 * @param jobID
	 * @param metaData
	 */
	public CalculusJob(
			UUID jobID, 
			MetaData metaData) {
		super();
		//
		this.jobID = jobID;
		this.metaData = metaData;
		this.taskSet = false;
		jobTaskList = new HashMap<UUID, MetaClass>();
		
	}

	/**
	 * 
	 * @return
	 */
	public boolean isTaskListComplete() {
		return (jobTaskList.size() == 0 && taskSet);
		
	}
	
	/**
	 * 
	 * @param jobClass
	 */
	public void setJobTask(MetaClass jobClass) {
		if (!jobTaskList.containsKey(jobClass.getUniqueID())) {
			// add a new class to job map
			jobTaskList.put(
					jobClass.getUniqueID(), 
					jobClass);
			taskSet = true;
		}
		
	}
	
	/**
	 * 
	 * @param jobClass
	 */
	public void completeJobTask(MetaClass jobClass) {
		if (jobTaskList.containsKey(jobClass.getUniqueID())) {
			// remove the given class from job map
			jobTaskList.remove(jobClass.getUniqueID());
		}
		
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
