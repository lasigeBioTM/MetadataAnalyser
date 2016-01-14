package pt.blackboard.protocol;

import java.util.ArrayList;
import java.util.UUID;

import pt.blackboard.protocol.enums.ComponentList;
import pt.ma.parse.MetaTerm;

/**
 * 
 * 
 *
 */
public class TermsOutgoing extends MessageProtocol {

	/**
	 * 
	 */
	private ArrayList<MetaTerm> body;
	
	/**
	 * 
	 * @param uniqueID
	 * @param body
	 * @param requestType
	 */
	public TermsOutgoing(
			UUID uniqueID, 
			ArrayList<MetaTerm> body, 
			ComponentList target) {
		super(uniqueID, target);
		//
		this.body = body;
		
	}

	/**
	 * 
	 * @return
	 */
	public ArrayList<MetaTerm> getBody() {
		return body;
	}
	
}
