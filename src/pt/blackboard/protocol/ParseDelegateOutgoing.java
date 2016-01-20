package pt.blackboard.protocol;

import java.util.List;
import java.util.UUID;

import pt.blackboard.protocol.enums.ComponentList;
import pt.blackboard.protocol.enums.RequestType;
import pt.ma.metadata.MetaClass;

/**
 * 
 * 
 *
 */
public class ParseDelegateOutgoing extends MessageProtocol {

	/**
	 * 
	 */
	private byte[] body;
	
	/**
	 * 
	 */
	private List<MetaClass> metaClasses;
	
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
	public ParseDelegateOutgoing(
			UUID uniqueID, 
			List<MetaClass> metaClasses,
			byte[] body, 
			ComponentList target,
			RequestType requestType) {
		super(uniqueID, target);		
		//
		this.body = body;
		this.requestType = requestType;
		this.metaClasses = metaClasses;
		
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
	public List<MetaClass> getMetaClasses() {
		return metaClasses;
	}
	
	/**
	 * 
	 * @return
	 */
	public RequestType getRequestType() {
		return requestType;
	}

}
