package pt.ma.component.parse;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
import pt.blackboard.protocol.ProxyDelegateOutgoing;
import pt.blackboard.protocol.TermsOutgoing;
import pt.blackboard.protocol.enums.ComponentList;
import pt.blackboard.protocol.enums.RequestType;
import pt.ma.component.parse.interfaces.IMetaHeader;
import pt.ma.component.parse.interfaces.IMetaOntologies;
import pt.ma.component.parse.metabolights.ParseHeaderMetaboLights;
import pt.ma.component.parse.metabolights.ParseOntologiesMetaboLights;
import pt.ma.exception.InactiveJobException;
import pt.ma.metadata.MetaAnnotation;
import pt.ma.metadata.MetaClass;
import pt.ma.metadata.MetaData;
import pt.ma.metadata.MetaObjective;
import pt.ma.metadata.MetaTerm;

/**
 * 
 * @author Bruno Inácio (fc40846@alunos.fc.ul.pt)
 *
 */
public class ParseObject extends DSL {
	
	/**
	 * 
	 */
	private static final String[] CLASS_NAMES = {"Design", "Factor", "Assay", "Protocol"};
	
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
	private Map<UUID, ParseJob> metadataActiveJobs;

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
		this.metadataActiveJobs = new ConcurrentHashMap<UUID, ParseJob>();

		// set blackboard outgoing messages queue
		this.blackboardOutgoingQueue = new LinkedBlockingQueue<MessageProtocol>();

		// open threads for reading from blackboard
		new Thread(new ParseBlackboardProxyRead(this.blackboard)).start();
		new Thread(new ParseBlackboardAnnotationsRead(this.blackboard)).start();
		new Thread(new ParseBlackboardTermsRead(this.blackboard)).start();

		// open a thread to check job completeness
		new Thread(new ParseProcessJobList(
				this.blackboard,
				this.metadataActiveJobs,
				this.blackboardOutgoingQueue)).start();

		// open a thread for writing to the blackboard
		new Thread(new ParseBlackboardWrite(blackboardOutgoingQueue)).start();
		
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

			case ANNOTATIONS:
				try {
					// message sent from concepts component
					AnnotationsOutgoing protocolAnnotation = gson.fromJson(
							message, 
							AnnotationsOutgoing.class);
					parseAnnotationResponse(
							protocolAnnotation.getUniqueID(),
							protocolAnnotation.getBody());
					
				} catch (InactiveJobException e) {
					// TODO log action
					
				}				
				break;
				
			case TERMS:
				try {
					// message sent from concepts component
					TermsOutgoing protocolTerm = gson.fromJson(
							message, 
							TermsOutgoing.class);
					parseTermResponse(
							protocolTerm.getUniqueID(),
							protocolTerm.getBody());
				} catch (InactiveJobException e) {
					// TODO log action

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
		
			case ANNOTATIONS:
				// blackboard message to concepts component
				message = gson.toJson((ParseDelegateOutgoing)protocol);
				blackboard.put(Tuple(TupleKey.ANNOTATIONSIN, message));				
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
				jobUUID,
				CLASS_NAMES,
				MetaObjective.METADATAANALYSIS);		
		
		try {
			// define checksum property
			metaData.setCheckSum(body);
			
		} catch (NoSuchAlgorithmException e) {
			// TODO log action

		}
		
		// set all metadata ontologies
		IMetaOntologies parseOntologies = new ParseOntologiesMetaboLights(body);
		metaData.addOntologies(parseOntologies.getMetaOntologies());		
		
		// add to active jobs list
		ParseJob parseJob = new ParseJob(jobUUID, metaData, 3);
		metadataActiveJobs.put(jobUUID, parseJob);

		// delegate class search job
		ParseDelegateOutgoing classProtocol = new ParseDelegateOutgoing(
				jobUUID,
				metaData.getMetaClasses(),
				body,
				ComponentList.ANNOTATIONS,
				RequestType.METADATAANALYSIS);
		blackboardOutgoingQueue.add(classProtocol);
		
		// delegate term search job
		ParseDelegateOutgoing termProtocol = new ParseDelegateOutgoing(
				jobUUID,
				metaData.getMetaClasses(),
				body,
				ComponentList.TERMS,
				RequestType.METADATAANALYSIS); 
		blackboardOutgoingQueue.add(termProtocol);
		
		// TODO: Log Action
		
	}
	
	/**
	 * 
	 * @param jobUUID
	 * @param metaClasses
	 * @throws InactiveJobException for invalid job uuid 
	 */
	private void parseAnnotationResponse(
			UUID jobUUID, 
			List<MetaClass> responseBody) throws InactiveJobException {
		
		// retreive metadata instance from active job
		if (!metadataActiveJobs.containsKey(jobUUID)) {
			throw new InactiveJobException(
					"ParseObject:parseAnnotationResponse - Invalid Job UUID: " 
					+ jobUUID);
		}
		ParseJob jobActive = metadataActiveJobs.get(jobUUID);
		MetaData metaData = jobActive.getMetaData();
		
		// TODO: log action
System.out.println("Parse Annotation Response");		
		
		// iterate trough all metadata classes and add the new annotations
		for (MetaClass metaClass : metaData.getMetaClasses()) {
			//
			boolean classFound = false;
			Iterator<MetaClass> respIterator = responseBody.iterator();
			while(respIterator.hasNext() && !classFound) {
				MetaClass respClass = respIterator.next();
				if (metaClass.equals(respClass)) {
System.out.println("MetaClass: " + metaClass.getClassName() + "; RespClass: " + respClass.getClassName());					
					//
					metaClass.removeAllTerms();
					for (MetaAnnotation respAnnotation : respClass.getMetaAnnotations()) {
						// TODO: set any term remaining property
System.out.println(respAnnotation.getURI());
						MetaAnnotation itemAnnotation = new MetaAnnotation(
								respAnnotation.getId(), 
								respAnnotation.getURI());
						metaClass.addMetaAnnotation(itemAnnotation);			
					}
					//
					classFound = true;
				}
			}
		}
		
		// set active job status
		jobActive.setParseStatus(3);
		
		// TODO: log action
		
	}
	
	/**
	 * 
	 * @param jobUUID
	 * @param responseBody
	 * @throws InactiveJobException for invalid job uuid
	 */
	private void parseTermResponse(
			UUID jobUUID, 
			List<MetaClass> responseBody) throws InactiveJobException {
		
		// retreive metadata instance from active job
		if (!metadataActiveJobs.containsKey(jobUUID)) {
			throw new InactiveJobException(
					"ParseObject:parseTermResponse - Invalid Job UUID: " 
					+ jobUUID);
		}
		ParseJob jobActive = metadataActiveJobs.get(jobUUID);
		MetaData metaData = jobActive.getMetaData();
		
		// TODO: log action
System.out.println("Parse Term Response");
		
		// iterate trough all metadata classes and add the new annotations
		for (MetaClass metaClass : metaData.getMetaClasses()) {
			//
			boolean classFound = false;
			Iterator<MetaClass> respIterator = responseBody.iterator();
			while(respIterator.hasNext() && !classFound) {
				MetaClass respClass = respIterator.next();
				if (metaClass.equals(respClass)) {
System.out.println("MetaClass: " + metaClass.getClassName() + "; RespClass: " + respClass.getClassName());					
					//
					metaClass.removeAllTerms();
					for (MetaTerm respTerm : respClass.getMetaTerms()) {
						// TODO: set any term remaining property
System.out.println(respTerm.getName());
						MetaTerm itemTerm = new MetaTerm(
								respTerm.getId(), 
								respTerm.getName());
						metaClass.addMetaTerm(itemTerm);
						
					}
					//
					classFound = true;
				}
			}
		}
		
		// set active job status
		jobActive.setParseStatus(4);
		
		// TODO: log action
		
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
	private class ParseBlackboardAnnotationsRead extends DSL implements Runnable {

		/**
		 * Locally managed blackboard instance
		 */
		private IBlackboard blackboard;
		
		/**
		 * 
		 * @param blackboard
		 */
		public ParseBlackboardAnnotationsRead(
				IBlackboard blackboard) {
			this.blackboard = blackboard;
			
		}

		@Override
		public void run() {

			// TODO: Logging action
			
			
			// Infinite loop
			while (!Thread.currentThread().isInterrupted()) {

				// waits for a PROXYIN tuple
				Tuple tuple = this.blackboard.get(MatchTuple(TupleKey.ANNOTATIONSOUT));
				String protocol = tuple.getData().toArray()[1].toString();
				
				// TODO: Logging action
				
				// prepare the new message to be sent
				receiveBLBMessage(
						protocol, 
						ComponentList.ANNOTATIONS);
				
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
				receiveBLBMessage(
						protocol, 
						ComponentList.TERMS);
				
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
		 * 
		 */
		private Queue<MessageProtocol> outgoingQueue;

		/**
		 * 
		 * @param blackboard
		 * @param outgoingQueue
		 */
		public ParseBlackboardWrite( 
				Queue<MessageProtocol> outgoingQueue) {
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
		private Map<UUID, ParseJob> metadataActiveJobs;
		
		/**
		 * 
		 * @param blackboard
		 * @param outgoingQueue
		 */
		public ParseProcessJobList(
				IBlackboard blackboard, 
				Map<UUID, ParseJob> metadataActiveJobs,
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
				for (Entry<UUID, ParseJob> entry : metadataActiveJobs.entrySet()) {
					//
					MetaData metaData = null;
					ParseJob activeJob = entry.getValue();
					switch (activeJob.getParseStatus()) {
						case 10:
							// the parse job is complet, so send a blackboard message 
							// to calculus component
							metaData = activeJob.getMetaData();
							ParseReadyOutgoing protocol = new ParseReadyOutgoing(
									entry.getKey(), 
									metaData,
									ComponentList.CALCULUS);
							sendBLBMessage(protocol);
							
							// add this job to deletion job list
							jobsToDelete.add(entry.getKey());
							
							// TODO: log action
							break;

						case 6:
							// TODO: annotations parse
							break;
							
						case 3:
							// TODO: ontologies parse
							break;
							
						case 1:
							// TODO: header parse
							break;

						case 0:
							// TODO: 
							break;

						default:
							// TODO: something's wrong
							break;
							
					}
					
				}
				
				// delete all processed jobs
				if (jobsToDelete.size() > 0) {
					for (UUID entry : jobsToDelete) {
						metadataActiveJobs.remove(entry);
					}					
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
