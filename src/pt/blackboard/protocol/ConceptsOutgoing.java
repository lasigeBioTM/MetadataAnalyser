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
public class ConceptsOutgoing extends MessageProtocol {

	/**
	 * 
	 */
	private ArrayList<MetaClass> body;

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
	public ConceptsOutgoing(
			UUID uniqueID, 
			ArrayList<MetaClass> body, 
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
	public ArrayList<MetaClass> getBody() {
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
