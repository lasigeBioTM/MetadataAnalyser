package pt.blackboard.protocol;

import java.util.List;
import java.util.UUID;

import pt.blackboard.protocol.enums.ComponentList;
import pt.ma.parse.MetaClass;

/**
 * 
 * 
 *
 */
public class CalculusDelegateOutgoing extends MessageProtocol {

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
	public CalculusDelegateOutgoing(
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
