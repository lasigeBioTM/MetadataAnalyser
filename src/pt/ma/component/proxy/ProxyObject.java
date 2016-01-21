package pt.ma.component.proxy;

import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import pt.blackboard.DSL;
import pt.blackboard.IBlackboard;
import pt.blackboard.Tuple;
import pt.blackboard.TupleKey;
import pt.blackboard.protocol.CalculusReadyOutgoing;
import pt.blackboard.protocol.LogIngoing;
import pt.blackboard.protocol.MessageProtocol;
import pt.blackboard.protocol.ProxyDelegateOutgoing;
import pt.blackboard.protocol.enums.ComponentList;
import pt.blackboard.protocol.enums.RequestType;
import pt.ma.component.log.LogType;
import pt.ma.component.proxy.network.Interface;
import pt.ma.component.proxy.network.Message;
import pt.ma.component.proxy.network.MessageType;

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
	private Queue<MessageProtocol> blackboardOutgoingQueue;
	
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
		//
		this.verbose = verbose;

		// Assign blackboard instance
		this.blackboard = blackboard;
					
		//
		this.tpcReceivedMessagesMap = new ConcurrentHashMap<UUID, ProxyMapObject>();
		
		//
		this.blackboardOutgoingQueue = new LinkedBlockingQueue<MessageProtocol>();

		// log action
		if (this.verbose) {
			String logmsg = "[" + this.getClass().getName() + "]: Component has started";
			LogIngoing protocol = new LogIngoing( 
					logmsg,
					LogType.INFO,
					ComponentList.LOG);
			blackboardOutgoingQueue.add(protocol);

		}

		// open a thread for reading from blackboard
		new Thread(new ProxyBlackboardCalculusRead(
				this.blackboard, 
				this.verbose)).start();

		// open a thread for writing to the blackboard
		new Thread(new ProxyBlackboardWrite(
				this.blackboard, 
				this.blackboardOutgoingQueue, 
				this.verbose)).start();
		
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
				if (this.verbose) {
					blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: Observers notification for " + 
							"received message, with Message ID: " + message.getUUID(),
							LogType.INFO,
							ComponentList.LOG));
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
		
		// log action
		if (this.verbose) {
			blackboardOutgoingQueue.add(new LogIngoing( 
					"[" + this.getClass().getName() + "]: New TCP message received, " + 
					"Job ID assigned: " + requestUUID,
					LogType.INFO,
					ComponentList.LOG));
		}

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
				// log action
				if (this.verbose) {
					blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: About to send a Blackboard message " + 
							"to Parse Component, for Job ID: " + protocol.getUniqueID(),
							LogType.INFO,
							ComponentList.LOG));
				}

				// blackboard message to Parse component
				message = gson.toJson((ProxyDelegateOutgoing)protocol);
				blackboard.put(Tuple(TupleKey.PROXYOUT, message));				
				break;

			default:
				// log action
				message = gson.toJson((LogIngoing)protocol);
				blackboard.put(Tuple(TupleKey.LOGIN, message));
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
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		switch (source) {
		
			case CALCULUS:
				// a new message from Calculus component
				CalculusReadyOutgoing protocolCalculus = gson.fromJson(
						message, 
						CalculusReadyOutgoing.class);

				// get proxy map object for this uuid
				ProxyMapObject mapObject = tpcReceivedMessagesMap.get(protocolCalculus.getUniqueID());

				// prepare the output message to be sent to original unique id				
				String jsonBody = gson.toJson(protocolCalculus.getBody());
				byte[] respbody = jsonBody.getBytes();
				
				// log action
				if (this.verbose) {
					blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: Blackboard message received " + 
									"from CALCULUS component, for Job ID: " + protocolCalculus.getUniqueID() + 
									". About to send TCP response " + 
									"to Client.",
							LogType.INFO,
							ComponentList.LOG));
				}

				// build and send a new tcp message
				Message tcpMessage = new Message( 
						mapObject.getSenderTCPIP(),
						mapObject.getSenderTCPPort(),
						MessageType.TCPRESPONSE, 
						respbody);
				network.sendMessage(tcpMessage);
				break;
				
			default:
				// log action
				if (this.verbose) {
					blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: Blackboard message received " + 
									"is not possible to determine source COMPONENT.",
							LogType.ERROR,
							ComponentList.LOG));
				}
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
		 */
		private boolean verbose;
		
		/**
		 * 
		 * @param blackboard
		 */
		public ProxyBlackboardCalculusRead(
				IBlackboard blackboard,
				boolean verbose) {
			this.blackboard = blackboard;
			
		}

		@Override
		public void run() {

			// log action
			if (this.verbose) {
				blackboardOutgoingQueue.add(new LogIngoing( 
						"[" + this.getClass().getName() + "]: Blackboard Calculus Read Thread has started.",
						LogType.INFO,
						ComponentList.LOG));
			}
		
			// Infinite loop
			while (!Thread.currentThread().isInterrupted()) {

				// waits for a PROXYIN tuple
				Tuple tuple = this.blackboard.get(MatchTuple(TupleKey.PROXYIN));
				String protocol = tuple.getData().toArray()[1].toString();
				
				// log action
				if (this.verbose) {
					blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: Blackboard Calculus message received.",
							LogType.INFO,
							ComponentList.LOG));
				}
				
				// parse the blackboard message received 
				receiveBLBMessage(
						protocol, 
						ComponentList.CALCULUS);
				
			}
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
		private boolean verbose;
		
		/**
		 * 
		 */
		private Queue<MessageProtocol> outgoingQueue;

		/**
		 * 
		 * @param blackboard
		 * @param outgoingQueue
		 */
		public ProxyBlackboardWrite(
				IBlackboard blackboard, 
				Queue<MessageProtocol> outgoingQueue,
				boolean verbose) {
			this.blackboard = blackboard;
			this.outgoingQueue = outgoingQueue;
			this.verbose = verbose;
			
		}

		@Override
		public void run() {
			
			// Infinite loop
			while (!Thread.currentThread().isInterrupted()) {
				
				// check for new blackboard outgoing messages
				if (outgoingQueue.size() > 0) {
					
					// send this message to blackboard
					sendBLBMessage(outgoingQueue.poll());

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
