package pt.ma.component.proxy.network;

import java.io.Serializable;
import java.util.UUID;

/**
 * 
 * @author Bruno Inacio (fc40846@alunos.fc.ul.pt)
 *
 */
public class Message implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 5307680282116659765L;

	/**
	 * Message unique random identifier
	 */
    private static UUID uuid;
    
    /**
     * 
     */
    private String senderTCPIP; 					
    private int senderTCPPort;
    
    /**
     * 
     */
    private String receiverTCPIP; 					
    private int receiverTCPPort;
 				
    /**
     * 
     */
    private long timestamp;				 

    /**
     * 
     */
    private MessageType type;

    /**
     * 
     */
    private byte[] body;

    /**
     * 
     */
    private String targetRepository;
    
    /**
     * 
     * @param uuid
     * @param sender
     * @param receiver
     * @param type
     * @param body
     */
    public Message( 
    		String receiverTCPIP, 
    		int receiverTCPPort,
    		MessageType type, 
    		byte[] body) {
    	
    	//
    	this.receiverTCPIP = receiverTCPIP;
    	this.receiverTCPPort = receiverTCPPort;
    	this.type = type;
    	this.body = body;
    	this.targetRepository = "0";
    	
    	//
    	this.uuid = UUID.randomUUID();
    	this.timestamp = System.currentTimeMillis();
    	
    }

    /**
     * 
     * @param receiverTCPIP
     * @param receiverTCPPort
     * @param type
     * @param body
     * @param targetRepository
     */
    public Message( 
    		String receiverTCPIP, 
    		int receiverTCPPort,
    		MessageType type, 
    		byte[] body,
    		String targetRepository) {
    	
    	//
    	this.receiverTCPIP = receiverTCPIP;
    	this.receiverTCPPort = receiverTCPPort;
    	this.type = type;
    	this.body = body;
    	this.targetRepository = targetRepository;
    	
    	//
    	this.uuid = UUID.randomUUID();
    	this.timestamp = System.currentTimeMillis();
    	
    }

    /**
     * 
     * @param senderTCPIP
     */
    public void setSenderTCPIP(String senderTCPIP) {
		this.senderTCPIP = senderTCPIP;
	}

    /**
     * 
     * @param senderTCPPort
     */
    public void setSenderTCPPort(int senderTCPPort) {
		this.senderTCPPort = senderTCPPort;
	}

    /**
     * 
     * @return
     */
	public String getReceiverTCPIP() {
		return receiverTCPIP;
	}

	/**
	 * 
	 * @return
	 */
	public int getReceiverTCPPort() {
		return receiverTCPPort;
	}

	/**
	 * 
	 * @return
	 */
	public String getSenderTCPIP() {
		return senderTCPIP;
	}

	/**
	 * 
	 * @return
	 */
	public int getSenderTCPPort() {
		return senderTCPPort;
	}

	/**
     * 
     * @return
     */
	public UUID getUUID() {
		return uuid;
	}

	/**
	 * 
	 * @return
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * 
	 * @return
	 */
	public MessageType getType() {
		return type;
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
	public String getTargetRepository() {
		return targetRepository;
	}

	/**
	 * 
	 * @param targetRepository
	 */
	public void setTargetRepository(String targetRepository) {
		this.targetRepository = targetRepository;
	}
	
}
