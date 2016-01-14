package pt.ma.proxy.network;

import java.io.Serializable;
import java.util.UUID;

/**
 * 
 * @author Bruno Inácio (fc40846@alunos.fc.ul.pt)
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
     * Sender and receiver node TCP IP addresses
     */
    private String senderAddress; 					// node ip address that sends this message
    private String receiverAddress; 				// node ip address that receives this message

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
     * @param uuid
     * @param sender
     * @param receiver
     * @param type
     * @param body
     */
    public Message( 
    		String receiverAddress, 
    		MessageType type, 
    		byte[] body) {
    	
    	//
    	this.receiverAddress = receiverAddress;
    	this.type = type;
    	this.body = body;
    	
    	//
    	this.uuid = UUID.randomUUID();
    	this.timestamp = System.currentTimeMillis();
    	
    }

    /**
     * 
     * @param senderAddress
     */
    public void setSenderAddress(String senderAddress) {
		this.senderAddress = senderAddress;
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
	public String getSenderAddress() {
		return senderAddress;
	}

	/**
	 * 
	 * @return
	 */
	public String getReceiverAddress() {
		return receiverAddress;
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
	
}
