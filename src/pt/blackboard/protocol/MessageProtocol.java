package pt.blackboard.protocol;

import java.util.UUID;

import pt.blackboard.protocol.enums.ComponentList;

/**
 * 
 * 
 *
 */
public abstract class MessageProtocol {

	/**
	 * 
	 */
	private long timestamp;
	
	/**
	 * 
	 */
	private UUID uniqueID;

	/**
	 * 
	 */
	private ComponentList target;
	
	/**
	 * 
	 * @param uniqueID
	 * @param body
	 * @param requestType
	 */
	public MessageProtocol(UUID uniqueID, ComponentList target) {
		super();
		
		//
		this.uniqueID = uniqueID;
		this.target = target;
		
		//
		this.timestamp = System.currentTimeMillis();
		
	}

	/**
	 * 
	 * @return
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * 
	 * @return
	 */
	public UUID getUniqueID() {
		return uniqueID;
	}
	
	/**
	 * 
	 * @return
	 */
	public ComponentList getComponentTarget() {
		return target;
	}
}
