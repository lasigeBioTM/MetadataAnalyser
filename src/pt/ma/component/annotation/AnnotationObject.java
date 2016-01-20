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
import pt.blackboard.protocol.MessageProtocol;
import pt.blackboard.protocol.ParseDelegateOutgoing;
import pt.blackboard.protocol.ParseReadyOutgoing;
import pt.blackboard.protocol.enums.ComponentList;
import pt.ma.component.parse.interfaces.IMetaAnnotations;
import pt.ma.component.parse.metabolights.ParseAnnotationsMetaboLights;
import pt.ma.metadata.MetaAnnotation;
import pt.ma.metadata.MetaClass;
import pt.ma.metadata.MetaData;

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

		// open threads for reading from blackboard
		new Thread(new ParseBlackboardParseRead(this.blackboard)).start();
				
		// open a thread for writing to the blackboard
		new Thread(new ParseBlackboardWrite(blackboardOutgoingQueue)).start();

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
		UUID jobUUID = null; MetaData jobActive = null; 
		switch (source) {
			
			case PARSE:
				
				// message sent from Proxy component
				ParseDelegateOutgoing protocolProxy = gson.fromJson(
						message, 
						ParseDelegateOutgoing.class);
				List<MetaClass> bodyClasses = protocolProxy.getMetaClasses();
				byte[] bodyParse = protocolProxy.getBody();
				switch (protocolProxy.getRequestType()) {
				
					case CONCEPTANALYSIS:
						// an concept analysis only
						break;
						
					case METADATAANALYSIS:
						// an hole metadata file analysis
						jobUUID = protocolProxy.getUniqueID(); 
						parseMetadataFile(
								jobUUID,
								bodyClasses,
								bodyParse);
						break;
						
					default:
						// TODO: something's wrong
						break;
						
				}
				break;
				
			default:
				// TODO: something's wrong
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
		
			case PARSE:
				// blackboard message to parse component
				message = gson.toJson((AnnotationsOutgoing)protocol);
				blackboard.put(Tuple(TupleKey.ANNOTATIONSOUT, message));				
				break;
				
			default:
				// TODO: log action
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
			byte[] body) {
		
		// TODO: Averiguar a hipóstese de saber que parser utilizar
		
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
			
		}
		
		// send a blackboard message to parse component with the results
		AnnotationsOutgoing protocol = new AnnotationsOutgoing(
				jobUUID,
				metaClasses,
				ComponentList.PARSE);
		blackboardOutgoingQueue.add(protocol);
		
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
		 * @param blackboard
		 */
		public ParseBlackboardParseRead(
				IBlackboard blackboard) {
			this.blackboard = blackboard;
			
		}

		@Override
		public void run() {

			// TODO: Logging action
			
			
			// Infinite loop
			while (!Thread.currentThread().isInterrupted()) {

				// waits for a PARSEOUT tuple
				Tuple tuple = this.blackboard.get(MatchTuple(TupleKey.ANNOTATIONSIN));
				String protocol = tuple.getData().toArray()[1].toString();
				
				// TODO: Logging action
				
				// process request sent from parse component
				receiveBLBMessage(
						protocol, 
						ComponentList.PARSE);
				
			}
			
			// TODO: Logging action

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
