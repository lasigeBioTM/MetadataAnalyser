package pt.blackboard.protocol;

import java.util.UUID;

import pt.blackboard.protocol.enums.ComponentList;
import pt.blackboard.protocol.enums.RequestType;

/**
 * 
 * 
 *
 */
public class ProxyDelegateOutgoing extends MessageProtocol {

	/**
	 * 
	 */
	private byte[] body;

	/**
	 * 
	 */
	private RequestType requestType;
	
	/**
	 * 
	 * @param uniqueID
	 * @param body
	 * @param target
	 * @param requestType
	 */
	public ProxyDelegateOutgoing(
			UUID uniqueID, 
			byte[] body, 
			ComponentList target, 
			RequestType requestType) {
		super(uniqueID, target);
		//
		this.body = body;
		this.requestType = requestType;
				
	}

	/**
	 * 
	 * @return
	 */
	public byte[] getBody() {
		return body;
	}
	
	/**
	 * 
	 * @return
	 */
	public RequestType getRequestType() {
		return requestType;
	}
	
}
