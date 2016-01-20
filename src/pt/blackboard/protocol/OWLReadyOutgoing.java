package pt.blackboard.protocol;

import java.util.ArrayList;
import java.util.UUID;

import pt.blackboard.protocol.enums.ComponentList;
import pt.ma.metadata.MetaClass;

/**
 * 
 * 
 *
 */
public class OWLReadyOutgoing extends MessageProtocol {

	/**
	 * 
	 */
	private MetaClass body;
	
	/**
	 * 
	 * @param uniqueID
	 * @param body
	 * @param requestType
	 */
	public OWLReadyOutgoing(
			UUID uniqueID, 
			MetaClass body, 
			ComponentList target) {
		super(uniqueID, target);
		//
		this.body = body;	
		
	}

	/**
	 * 
	 * @return
	 */
	public MetaClass getBody() {
		return body;
	}
	
	
}
