package pt.ma.component.term;

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
import pt.blackboard.protocol.TermsOutgoing;
import pt.blackboard.protocol.enums.ComponentList;
import pt.ma.component.parse.interfaces.IMetaTerms;
import pt.ma.component.parse.metabolights.ParseTermsMetaboLights;
import pt.ma.metadata.MetaClass;
import pt.ma.metadata.MetaData;
import pt.ma.metadata.MetaTerm;

/**
 * 
 * 
 *
 */
public class TermObject extends DSL {
	
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
	public TermObject(
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
		new Thread(new ParseBlackboardWrite(
				this.blackboard, 
				blackboardOutgoingQueue)).start();

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
						parseMetadataFile(
								protocolProxy.getUniqueID(),
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
	 * @param jobUUID
	 * @param body
	 */
	private void parseMetadataFile(
			UUID jobUUID, 
			List<MetaClass> metaClasses,
			byte[] body) {
		
		// TODO: Averiguar a hipóstese de saber que parser utilizar
		
		// parse terms for each meta class given
		IMetaTerms parseTerms = new ParseTermsMetaboLights(body);
		for (MetaClass metaClass: metaClasses) {

			// read all available terms for this meta class			
			List<MetaTerm> terms = parseTerms.getMetaTerms(metaClass);
			
			// add all read new annotations to the meta class
			metaClass.removeAllTerms();
			for (MetaTerm term : terms) {
				metaClass.addMetaTerm(term);	
			}
			
		}
		
		// send a blackboard message to parse component with the results
		TermsOutgoing protocol = new TermsOutgoing(
				jobUUID,
				metaClasses,
				ComponentList.PARSE);
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
				// blackboard message to parse component
				message = gson.toJson((TermsOutgoing)protocol);
				blackboard.put(Tuple(TupleKey.TERMSOUT, message));				
				break;
				
			default:
				// TODO: log action
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
				Tuple tuple = this.blackboard.get(MatchTuple(TupleKey.TERMSIN));
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
		 * Locally managed blackboard instance
		 */
		private IBlackboard blackboard;

		/**
		 * 
		 */
		private Queue<MessageProtocol> outgoingQueue;

		/**
		 * 
		 * @param blackboard
		 * @param outgoingQueue
		 */
		public ParseBlackboardWrite(
				IBlackboard blackboard, 
				Queue<MessageProtocol> outgoingQueue) {
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

}
