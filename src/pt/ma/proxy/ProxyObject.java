package pt.ma.proxy;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.gson.Gson;

import pt.blackboard.DSL;
import pt.blackboard.IBlackboard;
import pt.blackboard.Tuple;
import pt.blackboard.TupleKey;
import pt.blackboard.protocol.CalculusReadyOutgoing;
import pt.blackboard.protocol.MessageProtocol;
import pt.blackboard.protocol.ProxyDelegateOutgoing;
import pt.blackboard.protocol.enums.ComponentList;
import pt.blackboard.protocol.enums.RequestType;
import pt.ma.proxy.network.Interface;
import pt.ma.proxy.network.Message;
import pt.ma.proxy.network.MessageType;

/**
 * 
 * @author 
 *
 */
public class ProxyObject extends DSL implements Observer {

	/**
	 * 
	 */
	private final int TCP_PORT = 8000;
	
	/**
	 * 
	 */
	private boolean verbose;

	/**
	 * 
	 */
	private IBlackboard blackboard;

	/**
	 * 
	 */
	private Map<UUID, ProxyMapObject> tpcReceivedMessagesMap;
	
	/**
	 * 
	 */
	private Queue<ProxyDelegateOutgoing> blackboardOutgoingQueue;
	
	/**
	 * 
	 */
	private Interface network;
	
	/**
	 * 
	 * @param blackboard
	 * @param verbose
	 */
	public ProxyObject(IBlackboard blackboard, boolean verbose)  {
		
		// Assign blackboard instance
		this.blackboard = blackboard;
		
		//
		this.verbose = verbose;
		
		//
		this.tpcReceivedMessagesMap = new HashMap<UUID, ProxyMapObject>();
		
		//
		this.blackboardOutgoingQueue = new LinkedBlockingQueue<ProxyDelegateOutgoing>();
		
		// open a thread for reading from blackboard
		new Thread(new ProxyBlackboardCalculusRead(this.blackboard)).start();

		// open a thread for writing to the blackboard
		new Thread(new ProxyBlackboardWrite(
				this.blackboard, 
				this.blackboardOutgoingQueue)).start();
		
		// open network interface
		network = new Interface(
				this, 
				TCP_PORT, 
				this.verbose);
	}
	
	// PUBLIC METHODS

	@Override
	public void update(Observable o, Object arg) {
		//
		if (o instanceof Interface) {
			try {
				// read the new network message
				Message message = (Message) arg;

				// take care of the new message
				receiveTCPMessage(message);
				
				// log action
				if (verbose) {
					System.out.println("[Communication :Info] Observers notification for received message");
				}
				
			} catch (Exception e) {

			}
		}
		
	}
	
	// PRIVATE METHODS
	
	/**
	 * 
	 * @param message
	 */
	private void sendTCPMessage(Message message) {
		
	}
	
	/**
	 * 
	 * @param message
	 */
	private void receiveTCPMessage(Message message) {
		
		// TODO: check message type
		
		// build a new proxy map reference object
		UUID requestUUID = UUID.randomUUID();
		ProxyMapObject mapObject = new ProxyMapObject(
				message.getUUID(), 
				message.getTimestamp(), 
				message.getSenderTCPIP(), 
				message.getSenderTCPPort(),
				System.currentTimeMillis());
		tpcReceivedMessagesMap.put(requestUUID, mapObject);
		
		// create a new blackboard outgoing message protocol 
		ProxyDelegateOutgoing protocol = new ProxyDelegateOutgoing(
				requestUUID, 
				message.getBody(),
				ComponentList.PARSE,
				RequestType.METADATAANALYSIS);
		blackboardOutgoingQueue.add(protocol);
		
	}
	
	/**
	 * 
	 * @param protocol
	 */
	private void sendBLBMessage(MessageProtocol protocol) {
		
		// send outgoing protocol message to the blackboard
		Gson gson = new Gson(); String message = null;
		switch (protocol.getComponentTarget()) {
		
			case PARSE:
				// blackboard message to Parse component
				message = gson.toJson((ProxyDelegateOutgoing)protocol);
				blackboard.put(Tuple(TupleKey.PROXYOUT, message));				
				break;

			default:
				// TODO: log action
				break;
		}
				
	}

	/**
	 * 
	 * @param message
	 * @param source
	 */
	private void receiveBLBMessage(
			String message, 
			ComponentList source) {
		
		// parse protocol message
		Gson gson = new Gson(); UUID msgUUID = null;  
		switch (source) {
		
			case CALCULUS:
				// a new message from Calculus component
				CalculusReadyOutgoing protocolCalculus = gson.fromJson(
						message, 
						CalculusReadyOutgoing.class);

				// get proxy map object for this uuid
				msgUUID = protocolCalculus.getUniqueID();
				ProxyMapObject mapObject = tpcReceivedMessagesMap.get(msgUUID);

				// prepare the output message to be sent to original unique id
System.out.println(protocolCalculus.getBody());				
				String jsonBody = gson.toJson(protocolCalculus.getBody());
				byte[] respbody = jsonBody.getBytes();
				
				// build and send a new tcp message
				Message tcpMessage = new Message( 
						mapObject.getSenderTCPIP(),
						mapObject.getSenderTCPPort(),
						MessageType.TCPRESPONSE, 
						respbody);
				network.sendMessage(tcpMessage);
				break;
				
			default:
				// TODO: something's wrong
				break;
		}
		
				
	}
	
	// PRIVATE CLASSES
	
	/**
	 * 
	 * 
	 *
	 */
	private class ProxyBlackboardCalculusRead extends DSL implements Runnable {

		/**
		 * Locally managed blackboard instance
		 */
		private IBlackboard blackboard;
		
		/**
		 * 
		 * @param blackboard
		 */
		public ProxyBlackboardCalculusRead(
				IBlackboard blackboard) {
			this.blackboard = blackboard;
			
		}

		@Override
		public void run() {

			// TODO: Logging action
			
			
			// Infinite loop
			while (!Thread.currentThread().isInterrupted()) {

				// waits for a PROXYIN tuple
				Tuple tuple = this.blackboard.get(MatchTuple(TupleKey.PROXYIN));
				String protocol = tuple.getData().toArray()[1].toString();
				
				// TODO: Logging action
				
				// 
				receiveBLBMessage(
						protocol, 
						ComponentList.CALCULUS);
				
			}
			
			// TODO: Logging action

		}
		
	}
	
	/**
	 * 
	 * 
	 *
	 */
	private class ProxyBlackboardWrite extends DSL implements Runnable {

		/**
		 * Locally managed blackboard instance
		 */
		private IBlackboard blackboard;

		/**
		 * 
		 */
		private Queue<ProxyDelegateOutgoing> outgoingQueue;

		/**
		 * 
		 * @param blackboard
		 * @param outgoingQueue
		 */
		public ProxyBlackboardWrite(
				IBlackboard blackboard, 
				Queue<ProxyDelegateOutgoing> outgoingQueue) {
			this.blackboard = blackboard;
			this.outgoingQueue = outgoingQueue;
			
		}

		@Override
		public void run() {
			
			// Infinite loop
			while (!Thread.currentThread().isInterrupted()) {
				
				// check for new blackboard outgoing messages
				if (outgoingQueue.size() > 0) {
					
					// send this message to blackboard
					sendBLBMessage(outgoingQueue.poll());
									
					// TODO: log action
					
					// wait for 5 seconds
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO: log action
						
					}
				}
			}

		}


	}
	
	/**
	 * 
	 * @author Bruno
	 *
	 */
	private class ProxyMapObject {
	
		/**
		 * 
		 */
		private UUID requestUUID;
		
		/**
		 * 
		 */
		private String senderTCPIP;
		
		/**
		 * 
		 */
		private int senderTCPPort;
		
		/**
		 * 
		 */
		private long sentTimestamp;
		
		/**
		 * 
		 */
		private long receivedTimestamp;

		/**
		 * 
		 * @param requestUUID
		 * @param sentTimestamp
		 * @param senderAddress
		 * @param receivedTimestamp
		 */
		public ProxyMapObject(
				UUID requestUUID,
				long sentTimestamp,
				String senderTCPIP,
				int senderTCPPort,
				long receivedTimestamp) {
			super();
			this.requestUUID = requestUUID;
			this.sentTimestamp = sentTimestamp;
			this.senderTCPIP = senderTCPIP;
			this.senderTCPPort = senderTCPPort;
			this.receivedTimestamp = receivedTimestamp;
		}

		/**
		 * 
		 * @return
		 */
		public UUID getRequestUUID() {
			return requestUUID;
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
		public long getSentTimestamp() {
			return sentTimestamp;
		}

		/**
		 * 
		 * @return
		 */
		public long getReceivedTimestamp() {
			return receivedTimestamp;
		}
		
	}
	
}
