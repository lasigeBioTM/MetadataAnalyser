package pt.blackboard.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import pt.blackboard.protocol.enums.ComponentList;
import pt.ma.metadata.MetaClass;

/**
 * 
 * 
 *
 */
public class DigestReady extends MessageProtocol {

	/**
	 * 
	 */
	private String body;
	
	/**
	 * 
	 * @param uniqueID
	 * @param body
	 * @param requestType
	 */
	public DigestReady(
			UUID uniqueID, 
			String body, 
			ComponentList target) {
		super(uniqueID, target);
		//
		this.body = body;
		
	}

	/**
	 * 
	 * @return
	 */
	public String getBody() {
		return body;
	}
	
}
