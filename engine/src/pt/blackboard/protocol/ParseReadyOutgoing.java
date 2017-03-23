package pt.blackboard.protocol;

import java.util.UUID;

import pt.blackboard.protocol.enums.ComponentList;
import pt.blackboard.protocol.enums.RequestType;
import pt.ma.metadata.MetaData;

/**
 * 
 * 
 *
 */
public class ParseReadyOutgoing extends MessageProtocol {

	/**
	 * 
	 */
	private MetaData body;
		
	/**
	 * 
	 */
	private RequestType requestType;
	
	/**
	 * 
	 * @param uniqueID
	 * @param body
	 * @param requestType
	 */
	public ParseReadyOutgoing(
			UUID uniqueID, 
			MetaData body, 
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
	public MetaData getBody() {
		return body;
	}

	/**
	 * 
	 * @return
	 */
	public RequestType getRequestType() {
		return requestType;
	}

	/**
	 * 
	 * @param requestType
	 */
	public void setRequestType(RequestType requestType) {
		this.requestType = requestType;
	}

}
