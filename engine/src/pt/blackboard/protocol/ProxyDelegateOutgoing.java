package pt.blackboard.protocol;

import java.util.UUID;

import pt.blackboard.protocol.enums.ComponentList;
import pt.blackboard.protocol.enums.RequestType;
import pt.ma.component.parse.RepositoryType;

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
	 */
	private RepositoryType repositoryType;
	
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
			RequestType requestType,
			RepositoryType repositoryType) {
		super(uniqueID, target);
		//
		this.body = body;
		this.requestType = requestType;
		this.repositoryType = repositoryType;
		
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

	/**
	 * 
	 * @return
	 */
	public RepositoryType getRepositoryType() {
		return repositoryType;
	}

	/**
	 * 
	 * @param repositoryType
	 */
	public void setRepositoryType(RepositoryType repositoryType) {
		this.repositoryType = repositoryType;
	}
	
}
