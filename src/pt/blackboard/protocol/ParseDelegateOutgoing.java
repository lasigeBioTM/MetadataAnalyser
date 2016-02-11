package pt.blackboard.protocol;

import java.util.List;
import java.util.UUID;

import pt.blackboard.protocol.enums.ComponentList;
import pt.blackboard.protocol.enums.RequestType;
import pt.ma.component.parse.RepositoryType;
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
	 */
	private RepositoryType repository;
	
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
			RequestType requestType,
			RepositoryType repository) {
		super(uniqueID, target);		
		//
		this.body = body;
		this.requestType = requestType;
		this.metaClasses = metaClasses;
		this.repository = repository;
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

	/**
	 * 
	 * @param requestType
	 */
	public void setRequestType(RequestType requestType) {
		this.requestType = requestType;
	}

	/**
	 * 
	 * @return
	 */
	public RepositoryType getRepositoryType() {
		return repository;
	}

	/**
	 * 
	 * @param repository
	 */
	public void setRepositoryType(RepositoryType repository) {
		this.repository = repository;
	}

}
