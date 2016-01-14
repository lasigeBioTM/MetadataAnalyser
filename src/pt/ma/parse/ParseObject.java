package pt.ma.parse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.gson.Gson;

import pt.blackboard.DSL;
import pt.blackboard.IBlackboard;
import pt.blackboard.Tuple;
import pt.blackboard.TupleKey;
import pt.blackboard.protocol.MessageProtocol;
import pt.blackboard.protocol.ConceptsOutgoing;
import pt.blackboard.protocol.ParseDelegateOutgoing;
import pt.blackboard.protocol.ParseReadyOutgoing;
import pt.blackboard.protocol.ProxyDelegateOutgoing;
import pt.blackboard.protocol.TermsOutgoing;
import pt.blackboard.protocol.enums.ComponentList;
import pt.blackboard.protocol.enums.RequestType;
import pt.ma.parse.interfaces.IMetaHeader;
import pt.ma.parse.interfaces.IMetaOntologies;
import pt.ma.parse.metabolights.ParseHeaderMetaboLights;
import pt.ma.parse.metabolights.ParseOntologiesMetaboLights;

/**
 * 
 * @author Bruno Inácio (fc40846@alunos.fc.ul.pt)
 *
 */
public class ParseObject extends DSL {
	
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
	private Map<UUID, MetaData> metadataActiveJobs;

	/**
	 * 
	 */
	private boolean verbose;
	
	/**
	 * 
	 * @param blackboard
	 * @param verbose
	 */
	public ParseObject(
			IBlackboard blackboard, 
			boolean verbose) {
		this.verbose = verbose;
		
		// assign blackboard instance
		this.blackboard = blackboard;
		
		// start active parse jobs data structure
		this.metadataActiveJobs = new HashMap<UUID, MetaData>();
		
		// set blackboard outgoing messages queue
		this.blackboardOutgoingQueue = new LinkedBlockingQueue<MessageProtocol>();

		// open threads for reading from blackboard
		new Thread(new ParseBlackboardProxyRead(this.blackboard)).start();
		new Thread(new ParseBlackboardConceptsRead(this.blackboard)).start();
		new Thread(new ParseBlackboardTermsRead(this.blackboard)).start();

		// open a thread to check job completeness
		new Thread(new ParseProcessJobList(
				this.blackboard,
				this.metadataActiveJobs,
				this.blackboardOutgoingQueue)).start();
		
		// open a thread for writing to the blackboard
		new Thread(new ParseBlackboardWrite(
				this.blackboard, 
				blackboardOutgoingQueue)).start();
		
	}

	// PRIVATE METHDOS
	
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
		
			case PROXY:		
				// message sent from Proxy component
				ProxyDelegateOutgoing protocolProxy = gson.fromJson(
						message, 
						ProxyDelegateOutgoing.class);
				byte[] bodyProxy = protocolProxy.getBody();
				switch (protocolProxy.getRequestType()) {
				
					case CONCEPTANALYSIS:
						// an concept analysis only
						break;
						
					case METADATAANALYSIS:
						// an hole metadata file analysis 
						parseMetadataFile(
								protocolProxy.getUniqueID(), 
								bodyProxy);
						break;
						
					default:
						// TODO: something's wrong
						break;
						
				}
				break;

			case CONCEPTS:
				// message sent from concepts component
				ConceptsOutgoing protocolConcepts = gson.fromJson(
						message, 
						ConceptsOutgoing.class);
				ArrayList<MetaClass> bodyConcepts = protocolConcepts.getBody();
				
				// retreive active job
				jobUUID = protocolConcepts.getUniqueID();
				jobActive = metadataActiveJobs.get(jobUUID);
				
				// add collected class concetps
				jobActive.addMetaClasses(bodyConcepts);
				break;
				
			case TERMS:
				// message sent from concepts component
				TermsOutgoing protocolTerms = gson.fromJson(message, TermsOutgoing.class);
				ArrayList<MetaTerm> bodyTerms = protocolTerms.getBody();
				
				// retreive active job
				jobUUID = protocolTerms.getUniqueID();
				jobActive = metadataActiveJobs.get(jobUUID);
				
				// add collected class concetps
				jobActive.addMetaTerms(bodyTerms);
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
		
			case CONCEPTS:
				// blackboard message to concepts component
				message = gson.toJson((ParseDelegateOutgoing)protocol);
				blackboard.put(Tuple(TupleKey.CONCEPTSIN, message));				
				break;
				
			case TERMS:
				// blackboard message to terms component
				message = gson.toJson((ParseDelegateOutgoing)protocol);
				blackboard.put(Tuple(TupleKey.TERMSIN, message));
				break;

			case CALCULUS:
				// blackboard message to calculus component
				message = gson.toJson((ParseReadyOutgoing)protocol);
				blackboard.put(Tuple(TupleKey.PARSEOUT, message));
				break;

			default:
				// TODO: log action
				break;
		}

	}

	/**
	 * 
	 * @param jobUUID
	 */
	private void parseMetadataFile(
			UUID jobUUID, 
			byte[] body) {
				
		// TODO: Averiguar a hipóstese de saber que parser utilizar
		
		// parse metadata header
		IMetaHeader parseHeader = new ParseHeaderMetaboLights(body);
		MetaData metaData = new MetaData(
				parseHeader.getStudyID(), 
				ParseObjective.METADATAANALYSIS);

		// set all metadata ontologies
		IMetaOntologies parseOntologies = new ParseOntologiesMetaboLights(body);
		metaData.addOntologies(parseOntologies.getMetaOntologies());
		
		// add to active jobs list
		metadataActiveJobs.put(jobUUID, metaData);
		
		// delegate class search job
		ParseDelegateOutgoing classProtocol = new ParseDelegateOutgoing(
				jobUUID,
				body,
				ComponentList.CONCEPTS,
				RequestType.METADATAANALYSIS);
		blackboardOutgoingQueue.add(classProtocol);
		
		// delegate term search job
		ParseDelegateOutgoing termProtocol = new ParseDelegateOutgoing(
				jobUUID,
				body,
				ComponentList.TERMS,
				RequestType.METADATAANALYSIS); 
		blackboardOutgoingQueue.add(termProtocol);
		
		// TODO: Log Action
	}
	
	// PRIVATE CLASSES
	
	/**
	 * 
	 * @author Bruno
	 *
	 */
	private class ParseBlackboardProxyRead extends DSL implements Runnable {

		/**
		 * Locally managed blackboard instance
		 */
		private IBlackboard blackboard;
		
		/**
		 * 
		 * @param blackboard
		 */
		public ParseBlackboardProxyRead(
				IBlackboard blackboard) {
			this.blackboard = blackboard;
			
		}

		@Override
		public void run() {

			// TODO: Logging action
			
			
			// Infinite loop
			while (!Thread.currentThread().isInterrupted()) {

				// waits for a PROXYIN tuple
				Tuple tuple = this.blackboard.get(MatchTuple(TupleKey.PROXYOUT));
				String protocol = tuple.getData().toArray()[1].toString();
				
				// TODO: Logging action
				
				// prepare the new message to be sent
				receiveBLBMessage(
						protocol, 
						ComponentList.PROXY);
				
			}
			
			// TODO: Logging action

		}
		
	}	
	
	/**
	 * 
	 * 
	 *
	 */
	private class ParseBlackboardConceptsRead extends DSL implements Runnable {

		/**
		 * Locally managed blackboard instance
		 */
		private IBlackboard blackboard;
		
		/**
		 * 
		 * @param blackboard
		 */
		public ParseBlackboardConceptsRead(
				IBlackboard blackboard) {
			this.blackboard = blackboard;
			
		}

		@Override
		public void run() {

			// TODO: Logging action
			
			
			// Infinite loop
			while (!Thread.currentThread().isInterrupted()) {

				// waits for a PROXYIN tuple
				Tuple tuple = this.blackboard.get(MatchTuple(TupleKey.CONCEPTSOUT));
				String protocol = tuple.getData().toArray()[1].toString();
				
				// TODO: Logging action
				
				// prepare the new message to be sent
				receiveBLBMessage(
						protocol, 
						ComponentList.CONCEPTS);
				
			}
			
			// TODO: Logging action

		}
		
	}	

	/**
	 * 
	 * 
	 *
	 */
	private class ParseBlackboardTermsRead extends DSL implements Runnable {

		/**
		 * Locally managed blackboard instance
		 */
		private IBlackboard blackboard;
		
		/**
		 * 
		 * @param blackboard
		 */
		public ParseBlackboardTermsRead(
				IBlackboard blackboard) {
			this.blackboard = blackboard;
			
		}

		@Override
		public void run() {

			// TODO: Logging action
			
			
			// Infinite loop
			while (!Thread.currentThread().isInterrupted()) {

				// waits for a PROXYIN tuple
				Tuple tuple = this.blackboard.get(MatchTuple(TupleKey.TERMSOUT));
				String protocol = tuple.getData().toArray()[1].toString();
				
				// TODO: Logging action
				
				// prepare the new message to be sent
				receiveBLBMessage(protocol, ComponentList.TERMS);
				
			}
			
			// TODO: Logging action

		}
		
	}	

	/**
	 * 
	 * @author Bruno
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
	
	/**
	 * 
	 * 
	 *
	 */
	private class ParseProcessJobList extends DSL implements Runnable {

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
		 */
		private Map<UUID, MetaData> metadataActiveJobs;
		
		/**
		 * 
		 * @param blackboard
		 * @param outgoingQueue
		 */
		public ParseProcessJobList(
				IBlackboard blackboard, 
				Map<UUID, MetaData> metadataActiveJobs,
				Queue<MessageProtocol> outgoingQueue) {
			
			this.blackboard = blackboard;
			this.metadataActiveJobs = metadataActiveJobs;
			this.outgoingQueue = outgoingQueue;
			
		}

		@Override
		public void run() {
			
			// Infinite loop
			while (!Thread.currentThread().isInterrupted()) {
				
				// check if there are any ready jobs
				ArrayList<UUID> jobsToDelete = new ArrayList<UUID>();
				for (Entry<UUID, MetaData> entry : metadataActiveJobs.entrySet()) {
					MetaData activeJob = entry.getValue();
					if (activeJob.isReady()) {
						
						// send a blackboard message to calculus component
						ParseReadyOutgoing protocol = new ParseReadyOutgoing(
								entry.getKey(), 
								activeJob,
								ComponentList.CALCULUS);
						sendBLBMessage(protocol);
						
						// TODO: log action
						
						// add this job to deletion job list
						jobsToDelete.add(entry.getKey());
						
					}
				}
				
				// delete all processed jobs
				for (UUID entry : jobsToDelete) {
					metadataActiveJobs.remove(entry);
				}
				
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
