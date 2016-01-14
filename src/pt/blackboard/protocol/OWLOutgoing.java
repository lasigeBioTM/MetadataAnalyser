package pt.blackboard.protocol;

import java.util.ArrayList;
import java.util.UUID;

import pt.blackboard.protocol.enums.ComponentList;
import pt.ma.parse.MetaClass;

/**
 * 
 * 
 *
 */
public class OWLOutgoing extends MessageProtocol {

	/**
	 * 
	 */
	private MetaClass body;

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
	public OWLOutgoing(
			UUID uniqueID, 
			MetaClass body, 
			ComponentList target) {
		super(uniqueID, target);
		//
		this.body = body;
		this.target = target;	
		
	}

	/**
	 * 
	 * @return
	 */
	public MetaClass getBody() {
		return body;
	}
	
	/**
	 * 
	 * @return
	 */
	public ComponentList getComponentList() {
		return target;
	}
	
}
