package pt.ma.component.annotation;

import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.gson.Gson;

import pt.blackboard.DSL;
import pt.blackboard.IBlackboard;
import pt.blackboard.Tuple;
import pt.blackboard.TupleKey;
import pt.blackboard.protocol.AnnotationsOutgoing;
import pt.blackboard.protocol.DigestReady;
import pt.blackboard.protocol.LogIngoing;
import pt.blackboard.protocol.MessageProtocol;
import pt.blackboard.protocol.ParseDelegateOutgoing;
import pt.blackboard.protocol.ParseReadyOutgoing;
import pt.blackboard.protocol.enums.ComponentList;
import pt.ma.component.log.LogType;
import pt.ma.component.parse.RepositoryType;
import pt.ma.component.parse.interfaces.IMetaAnnotations;
import pt.ma.component.parse.metabolights.ParseAnnotationsMetaboLights;
import pt.ma.metadata.MetaAnnotation;
import pt.ma.metadata.MetaClass;
import pt.ma.metadata.MetaData;
import pt.ma.util.StringWork;

/**
 * 
 * @author Bruno
 *
 */
public class AnnotationObject extends DSL {
	
	/**
	 * 
	 */
	private IBlackboard blackboard;
	
	/**
	 * 
	 */
	private Queue<MessageProtocol> blackboardOutgoingQueue;

	/**
	 * 
	 */
	private boolean verbose;

	/**
	 * 
	 * @param blackboard
	 * @param verbose
	 */
	public AnnotationObject(
			IBlackboard blackboard, 
			boolean verbose) {
		this.verbose = verbose;
		
		// assign blackboard instance
		this.blackboard = blackboard;
				
		// set blackboard outgoing messages queue
		this.blackboardOutgoingQueue = new LinkedBlockingQueue<MessageProtocol>();

		// log action
		if (this.verbose) {
			blackboardOutgoingQueue.add(new LogIngoing( 
					"[" + this.getClass().getName() + "]: Component has started",
					LogType.INFO,
					ComponentList.LOG));
		}

		// open threads for reading from blackboard
		new Thread(new ParseBlackboardParseRead(
				this.blackboard,
				this.verbose)).start();
				
		// open a thread for writing to the blackboard
		new Thread(new ParseBlackboardWrite(this.blackboardOutgoingQueue)).start();

	}
	
	// PRIVATE METHODS
	
	/**
	 * 
	 * @param message
	 * @param source
	 */
	private void receiveBLBMessage(
			String message, 
			ComponentList source) {

		// parse protocol message
		Gson gson = new Gson(); 
		switch (source) {
			
			case PARSE:
				try {
					// message sent from Proxy component
					DigestReady digestProtocol = null;
					ParseDelegateOutgoing protocolProxy = gson.fromJson(
							message, 
							ParseDelegateOutgoing.class);
					switch (protocolProxy.getRequestType()) {
					
						case CONCEPTANALYSIS:
							// log action
							if (this.verbose) {
								blackboardOutgoingQueue.add(new LogIngoing( 
										"[" + this.getClass().getName() + "]: Blackboard message received " + 
												"from PARSE component, for Job ID: " + protocolProxy.getUniqueID() + 
												". About to initiate CONCEPT parsing process.",
										LogType.INFO,
										ComponentList.LOG));
							}

							// an concept analysis only 
							parseMetadataFile(
									protocolProxy.getUniqueID(),
									protocolProxy.getMetaClasses(),
									protocolProxy.getBody(),
									protocolProxy.getRepositoryType());
							
							// build a digest blackboard message
							digestProtocol = new DigestReady(
									protocolProxy.getUniqueID(),
									"[" + StringWork.getNowDate() + "] " +
									"Concept Annotation Parsing has ended for Job ID: " + 
											protocolProxy.getUniqueID(),
									ComponentList.DIGEST); 
							blackboardOutgoingQueue.add(digestProtocol);
							break;
							
						case METADATAANALYSIS:
							// log action
							if (this.verbose) {
								blackboardOutgoingQueue.add(new LogIngoing( 
										"[" + this.getClass().getName() + "]: Blackboard message received " + 
												"from PARSE component, for Job ID: " + protocolProxy.getUniqueID() + 
												". About to initiate METADATA FILE parsing process.",
										LogType.INFO,
										ComponentList.LOG));
							}

							// an hole metadata file analysis 
							parseMetadataFile(
									protocolProxy.getUniqueID(),
									protocolProxy.getMetaClasses(),
									protocolProxy.getBody(),
									protocolProxy.getRepositoryType());
							
							// build a digest blackboard message
							digestProtocol = new DigestReady(
									protocolProxy.getUniqueID(),
									"[" + StringWork.getNowDate() + "] " +
									"Metadata Annotation Parsing has ended for Job ID: " + 
											protocolProxy.getUniqueID(),
									ComponentList.DIGEST); 
							blackboardOutgoingQueue.add(digestProtocol);
							break;
							
						default:
							// log action
							if (this.verbose) {
								blackboardOutgoingQueue.add(new LogIngoing( 
										"[" + this.getClass().getName() + "]: Blackboard message received " + 
												"is not possible to determine REQUEST TYPE.",
										LogType.ERROR,
										ComponentList.LOG));
							}
							break;
							
					}
					
				} catch (Exception e) {
					// log action
					blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: An error as occured parsing a " + 
							"PARSE blackboard component message. Error: " + e.getMessage(),
							LogType.ERROR,
							ComponentList.LOG));
				}
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
	
	/**
	 * 
	 * @param protocol
	 */
	private void sendBLBMessage(MessageProtocol protocol) {
		
		// send outgoing protocol message to the blackboard
		Gson gson = new Gson(); String message = null;
		switch (protocol.getComponentTarget()) {
		
			case DIGEST:
				// log action
				if (this.verbose) {
					blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: About to send a Blackboard message " + 
							"to PROXY Component, for Job ID: " + protocol.getUniqueID(),
							LogType.INFO,
							ComponentList.LOG));
				}
	
				// blackboard message to concepts component
				message = gson.toJson((DigestReady)protocol);
				blackboard.put(Tuple(TupleKey.PROXYDIGEST, message));				
				break;

			case PARSE:
				// log action
				if (this.verbose) {
					blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: About to send a Blackboard message " + 
							"to PARSE Component, for Job ID: " + protocol.getUniqueID(),
							LogType.INFO,
							ComponentList.LOG));
				}

				// blackboard message to parse component
				message = gson.toJson((AnnotationsOutgoing)protocol);
				blackboard.put(Tuple(TupleKey.ANNOTATIONSOUT, message));				
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
	 * @param jobUUID
	 * @param body
	 */
	private void parseMetadataFile(
			UUID jobUUID, 
			List<MetaClass> metaClasses,
			byte[] body,
			RepositoryType repository) {
		
		switch (repository) {
			case METOBOLIGHTS:
				// parse annotations for each meta class given
				IMetaAnnotations parseAnnotations = new ParseAnnotationsMetaboLights(body);
				for (MetaClass metaClass: metaClasses) {
					
					// read all available annotations for this meta class			
					List<MetaAnnotation> annotations = parseAnnotations.getMetaAnnotations(metaClass);
					
					// add all read new annotations to the meta class
					metaClass.removeAllAnnotations();
					for (MetaAnnotation annotation : annotations) {
						metaClass.addMetaAnnotation(annotation);	
					}
					
					// log action
					if (this.verbose) {
						blackboardOutgoingQueue.add(new LogIngoing( 
								"[" + this.getClass().getName() + "]: Parsing process for MetaClass: " + 
								metaClass.getClassName() + " returned #" + annotations.size() + " Annotations, for Job ID: " + 
								jobUUID,
								LogType.INFO,
								ComponentList.LOG));
					}
					
				}
				
				// send a blackboard message to parse component with the results
				AnnotationsOutgoing protocol = new AnnotationsOutgoing(
						jobUUID,
						metaClasses,
						ComponentList.PARSE);
				blackboardOutgoingQueue.add(protocol);
				break;
	
			default:
				// log action
				if (this.verbose) {
					blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: There isn't any repository type to analyse.",
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
	private class ParseBlackboardParseRead extends DSL implements Runnable {

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
		public ParseBlackboardParseRead(
				IBlackboard blackboard,
				boolean verbose) {
			this.blackboard = blackboard;
			this.verbose = verbose;
		}

		@Override
		public void run() {

			// log action
			if (this.verbose) {
				blackboardOutgoingQueue.add(new LogIngoing( 
						"[" + this.getClass().getName() + "]: Blackboard PARSE Read Thread has started.",
						LogType.INFO,
						ComponentList.LOG));
			}
			
			// Infinite loop
			while (!Thread.currentThread().isInterrupted()) {

				// waits for a PARSEOUT tuple
				Tuple tuple = this.blackboard.get(MatchTuple(TupleKey.ANNOTATIONSIN));
				String protocol = tuple.getData().toArray()[1].toString();
				
				// log action
				if (this.verbose) {
					blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: Blackboard Parse message received.",
							LogType.INFO,
							ComponentList.LOG));
				}
				
				// process request sent from parse component
				receiveBLBMessage(
						protocol, 
						ComponentList.PARSE);	
			}
		}
	}	
		
	/**
	 * 
	 * 
	 *
	 */
	private class ParseBlackboardWrite extends DSL implements Runnable {

		/**
		 * 
		 */
		private Queue<MessageProtocol> outgoingQueue;

		/**
		 * 
		 * @param blackboard
		 * @param outgoingQueue
		 */
		public ParseBlackboardWrite(Queue<MessageProtocol> outgoingQueue) {
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
	
}
