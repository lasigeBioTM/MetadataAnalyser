package pt.blackboard.protocol;

import java.util.List;
import java.util.UUID;

import pt.blackboard.protocol.enums.ComponentList;
import pt.ma.metadata.MetaClass;

/**
 * 
 * 
 *
 */
public class AnnotationsOutgoing extends MessageProtocol {

	/**
	 * 
	 */
	private List<MetaClass> body;
	
	/**
	 * 
	 * @param uniqueID
	 * @param body
	 * @param requestType
	 */
	public AnnotationsOutgoing(
			UUID uniqueID, 
			List<MetaClass> body, 
			ComponentList target) {
		super(uniqueID, target);
		//
		this.body = body;	
		
	}

	/**
	 * 
	 * @return
	 */
	public List<MetaClass> getBody() {
		return this.body;
		
	}
		
}
