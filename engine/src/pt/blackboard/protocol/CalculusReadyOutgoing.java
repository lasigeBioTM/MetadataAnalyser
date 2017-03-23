package pt.blackboard.protocol;

import java.util.UUID;

import pt.blackboard.protocol.enums.ComponentList;
import pt.ma.metadata.MetaClass;
import pt.ma.metadata.MetaData;

/**
 * 
 * 
 *
 */
public class CalculusReadyOutgoing extends MessageProtocol {

	/**
	 * 
	 */
	private MetaData body;
	
	/**
	 * 
	 * @param uniqueID
	 * @param body
	 * @param requestType
	 */
	public CalculusReadyOutgoing(
			UUID uniqueID, 
			MetaData body, 
			ComponentList target) {
		super(uniqueID, target);		
		//
		this.body = body;
		
	}
	
	/**
	 * 
	 * @return
	 */
	public MetaData getBody() {
		return body;
	}

}
