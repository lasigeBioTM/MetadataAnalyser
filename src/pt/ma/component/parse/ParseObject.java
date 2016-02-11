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
import pt.blackboard.protocol.DigestReady;
import pt.blackboard.protocol.LogIngoing;
import pt.blackboard.protocol.MessageProtocol;
import pt.blackboard.protocol.ParseDelegateOutgoing;
import pt.blackboard.protocol.ParseReadyOutgoing;
import pt.blackboard.protocol.ProxyDelegateOutgoing;
import pt.blackboard.protocol.TermsOutgoing;
import pt.blackboard.protocol.enums.ComponentList;
import pt.blackboard.protocol.enums.RequestType;
import pt.ma.component.log.LogType;
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
import pt.ma.util.StringWork;

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
	private static final String[] CLASS_DEFAULT = {"Default"};
	
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

		// log action
		if (this.verbose) {
			String logmsg = "[" + this.getClass().getName() + "]: Component has started";
			LogIngoing protocol = new LogIngoing( 
					logmsg,
					LogType.INFO,
					ComponentList.LOG);
			blackboardOutgoingQueue.add(protocol);
		}

		// open threads for reading from blackboard
		new Thread(new ParseBlackboardProxyRead(
				this.blackboard,
				this.verbose)).start();
		new Thread(new ParseBlackboardAnnotationsRead(
				this.blackboard, 
				this.verbose)).start();
		new Thread(new ParseBlackboardTermsRead(
				this.blackboard, 
				this.verbose)).start();

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
				try {
					// message sent from Proxy component
					DigestReady digestProtocol = null;
					ProxyDelegateOutgoing protocolProxy = gson.fromJson(
							message, 
							ProxyDelegateOutgoing.class);
					switch (protocolProxy.getRequestType()) {
					
						case CONCEPTANALYSIS:
							// log action
							if (this.verbose) {
								blackboardOutgoingQueue.add(new LogIngoing( 
										"[" + this.getClass().getName() + "]: Blackboard message received " + 
												"from PROXY component, for Job ID: " + protocolProxy.getUniqueID() + 
												". About to initiate CONCEPT parsing process." + 
												"to Client.",
										LogType.INFO,
										ComponentList.LOG));
							}
							
							// an concept analysis only 
							parseMetadataConcept(
									protocolProxy.getUniqueID(), 
									protocolProxy.getBody(),
									protocolProxy.getRepositoryType());			
							
							// build a digest blackboard message
							digestProtocol = new DigestReady(
									protocolProxy.getUniqueID(),
									"[" + StringWork.getNowDate() + "] " +
									"Concept Parsing Process has started for Job ID: " + 
											protocolProxy.getUniqueID(),
									ComponentList.DIGEST); 
							blackboardOutgoingQueue.add(digestProtocol);
							break;
							
						case METADATAANALYSIS:
							// log action
							if (this.verbose) {
								blackboardOutgoingQueue.add(new LogIngoing( 
										"[" + this.getClass().getName() + "]: Blackboard message received " + 
												"from PROXY component, for Job ID: " + protocolProxy.getUniqueID() + 
												". About to initiate METADATA FILE parsing process.",
										LogType.INFO,
										ComponentList.LOG));
							}

							// do an entire metadata file analysis 
							parseMetadataFile(
									protocolProxy.getUniqueID(), 
									protocolProxy.getBody(),
									protocolProxy.getRepositoryType());
							
							// build a digest blackboard message
							digestProtocol = new DigestReady(
									protocolProxy.getUniqueID(),
									"[" + StringWork.getNowDate() + "] " +
									"Metadata Parsing Process has started for Job ID: " + 
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
							"PROXY blackboard component message. Error: " + e.getMessage(),
							LogType.ERROR,
							ComponentList.LOG));
				}
				break;

			case ANNOTATIONS:
				try {
					// message sent from concepts component
					AnnotationsOutgoing protocolAnnotation = gson.fromJson(
							message, 
							AnnotationsOutgoing.class);
					
					// log action
					if (this.verbose) {
						blackboardOutgoingQueue.add(new LogIngoing( 
								"[" + this.getClass().getName() + "]: Blackboard message received " + 
										"from ANNOTATIONS component, for Job ID: " + 
										protocolAnnotation.getUniqueID() + 
										".",
								LogType.INFO,
								ComponentList.LOG));
					}
					
					// parse annotation component response
					parseAnnotationResponse(
							protocolAnnotation.getUniqueID(),
							protocolAnnotation.getBody());
					
				} catch (InactiveJobException e) {
					// log action
					blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: An error as occured parsing a " + 
							"ANNOTATIONS blackboard component message. Error: " + e.getMessage(),
							LogType.ERROR,
							ComponentList.LOG));
				}				
				break;
				
			case TERMS:
				try {
					// message sent from concepts component
					TermsOutgoing protocolTerm = gson.fromJson(
							message, 
							TermsOutgoing.class);
					
					// log action
					if (this.verbose) {
						blackboardOutgoingQueue.add(new LogIngoing( 
								"[" + this.getClass().getName() + "]: Blackboard message received " + 
										"from TERMS component, for Job ID: " + protocolTerm.getUniqueID() + 
										".",
								LogType.INFO,
								ComponentList.LOG));
					}
					
					// parse term component response
					parseTermResponse(
							protocolTerm.getUniqueID(),
							protocolTerm.getBody());
					
				} catch (InactiveJobException e) {
					// log action
					blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: An error as occured parsing a " + 
							"TERMS blackboard component message. Error: " + e.getMessage(),
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

			case ANNOTATIONS:
				// log action
				if (this.verbose) {
					blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: About to send a Blackboard message " + 
							"to ANNOTATIONS Component, for Job ID: " + protocol.getUniqueID(),
							LogType.INFO,
							ComponentList.LOG));
				}

				// blackboard message to concepts component
				message = gson.toJson((ParseDelegateOutgoing)protocol);
				blackboard.put(Tuple(TupleKey.ANNOTATIONSIN, message));				
				break;
				
			case TERMS:
				// log action
				if (this.verbose) {
					blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: About to send a Blackboard message " + 
							"to TERMS Component, for Job ID: " + protocol.getUniqueID(),
							LogType.INFO,
							ComponentList.LOG));
				}
				
				// blackboard message to terms component
				message = gson.toJson((ParseDelegateOutgoing)protocol);
				blackboard.put(Tuple(TupleKey.TERMSIN, message));
				break;

			case CALCULUS:
				// log action
				if (this.verbose) {
					blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: About to send a Blackboard message " + 
							"to CALCULUS Component, for Job ID: " + protocol.getUniqueID(),
							LogType.INFO,
							ComponentList.LOG));
				}

				// blackboard message to calculus component
				message = gson.toJson((ParseReadyOutgoing)protocol);
				blackboard.put(Tuple(TupleKey.PARSEOUT, message));
				break;

			default:
				// log action
				message = gson.toJson((LogIngoing)protocol);
				blackboard.put(Tuple(TupleKey.LOGIN, message));
				break;
		}

	}

	private void parseMetadataConcept(
			UUID jobUUID, 
			byte[] body,
			RepositoryType repository) {
	
		// check target repository
		boolean parserFound = false;
		MetaData metaData = null; int parseStatus = 0;
		switch (repository) {
		
			case METOBOLIGHTS:
				// this is a metobolights repository concept
				metaData = parseMetoboLightsConcept(jobUUID, body);

				// set parser initial status
				parseStatus = 7;
				parserFound = true;
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
		
		// there's a parser for the request
		if (parserFound) {
			
			// add to active jobs list
			ParseJob parseJob = new ParseJob(
					jobUUID, 
					metaData, 
					parseStatus, 
					RequestType.CONCEPTANALYSIS);
			metadataActiveJobs.put(jobUUID, parseJob);

			// send the result directly to calculus component
			ParseReadyOutgoing classProtocol = new ParseReadyOutgoing(
					jobUUID,
					metaData,
					ComponentList.CALCULUS,
					RequestType.CONCEPTANALYSIS);
			blackboardOutgoingQueue.add(classProtocol);				

		}
	}
	
	/**
	 * 
	 * @param jobUUID
	 */
	private void parseMetadataFile(
			UUID jobUUID, 
			byte[] body,
			RepositoryType repository) {
		
		//
		boolean parserFound = false;
		MetaData metaData = null; int parseStatus = 0;
		switch (repository) {
		
			case METOBOLIGHTS:
				// this is a metobolights repository file
				metaData = parseMetoboLightsFile(jobUUID, body);
				
				// set parser initial status
				parseStatus = 3;
				parserFound = true;
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
		
		// there's a parser for the request
		if (parserFound) {
			
			// add to active jobs list
			ParseJob parseJob = new ParseJob(
					jobUUID, 
					metaData, 
					parseStatus, 
					RequestType.METADATAANALYSIS);
			metadataActiveJobs.put(jobUUID, parseJob);

			// delegate class search job
			ParseDelegateOutgoing classProtocol = new ParseDelegateOutgoing(
					jobUUID,
					metaData.getMetaClasses(),
					body,
					ComponentList.ANNOTATIONS,
					RequestType.METADATAANALYSIS,
					repository);
			blackboardOutgoingQueue.add(classProtocol);
			
			// delegate term search job
			ParseDelegateOutgoing termProtocol = new ParseDelegateOutgoing(
					jobUUID,
					metaData.getMetaClasses(),
					body,
					ComponentList.TERMS,
					RequestType.METADATAANALYSIS,
					repository); 
			blackboardOutgoingQueue.add(termProtocol);
		}
	}
	
	/**
	 * 
	 * @param jobUUID
	 * @param body
	 * @return
	 */
	private MetaData parseMetoboLightsConcept(
			UUID jobUUID,
			byte[] body) {

		// build stub metadata object
		MetaData metaData = new MetaData(
				null,
				jobUUID,
				CLASS_DEFAULT,
				MetaObjective.CONCEPTANALYSIS);		

		try {
			// define checksum property
			metaData.setCheckSum(body);
			
		} catch (NoSuchAlgorithmException e) {
			// log action
			blackboardOutgoingQueue.add(new LogIngoing( 
					"[" + this.getClass().getName() + "]: An error as occured parsing calculating " + 
					"metadata file body.",
					LogType.ERROR,
					ComponentList.LOG));
		}		
		
		// add the new concept to default class
		metaData.addConceptToClass(
				new String(body), 
				CLASS_DEFAULT[0]);
		
		return metaData;
	}
	
	/**
	 * 
	 * @param jobUUID
	 * @param body
	 * @return
	 */
	private MetaData parseMetoboLightsFile(
			UUID jobUUID,
			byte[] body) {
		
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
			// log action
			blackboardOutgoingQueue.add(new LogIngoing( 
					"[" + this.getClass().getName() + "]: An error as occured parsing calculating " + 
					"metadata file body.",
					LogType.ERROR,
					ComponentList.LOG));
		}
		
		// set all metadata ontologies
		IMetaOntologies parseOntologies = new ParseOntologiesMetaboLights(body);
		metaData.addOntologies(parseOntologies.getMetaOntologies());		
		
		//
		return metaData;
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
		
		// log action
		if (this.verbose) {
			blackboardOutgoingQueue.add(new LogIngoing( 
					"[" + this.getClass().getName() + "]: About to parse ANNOTATIONS component response, " + 
					"for Job ID: " + jobUUID,
					LogType.INFO,
					ComponentList.LOG));
		}
				
		// iterate trough all metadata classes and add the new annotations
		for (MetaClass metaClass : metaData.getMetaClasses()) {
			//
			boolean classFound = false;
			Iterator<MetaClass> respIterator = responseBody.iterator();
			while(respIterator.hasNext() && !classFound) {
				MetaClass respClass = respIterator.next();
				if (metaClass.equals(respClass)) {					
					//
					metaClass.removeAllTerms();
					for (MetaAnnotation respAnnotation : respClass.getMetaAnnotations()) {
						// TODO: set any term remaining property
						MetaAnnotation itemAnnotation = new MetaAnnotation(
								respAnnotation.getId(), 
								respAnnotation.getURI());
						metaClass.addMetaAnnotation(itemAnnotation);			
					}
					
					// log action
					if (this.verbose) {
						blackboardOutgoingQueue.add(new LogIngoing( 
								"[" + this.getClass().getName() + "]: Found #" + respClass.getMetaAnnotations() + 
								" Annotations for Class Name: " + metaClass.getClassName(),
								LogType.INFO,
								ComponentList.LOG));
					}

					//
					classFound = true;
				}
			}
		}
		
		// set active job status
		jobActive.setParseStatus(3);
		
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
		
		// log action
		if (this.verbose) {
			blackboardOutgoingQueue.add(new LogIngoing( 
					"[" + this.getClass().getName() + "]: About to parse TERMS component response, " + 
					"for Job ID: " + jobUUID,
					LogType.INFO,
					ComponentList.LOG));
		}
		
		// iterate trough all metadata classes and add the new annotations
		for (MetaClass metaClass : metaData.getMetaClasses()) {
			//
			boolean classFound = false;
			Iterator<MetaClass> respIterator = responseBody.iterator();
			while(respIterator.hasNext() && !classFound) {
				MetaClass respClass = respIterator.next();
				if (metaClass.equals(respClass)) {					
					//
					metaClass.removeAllTerms();
					for (MetaTerm respTerm : respClass.getMetaTerms()) {
						// TODO: set any term remaining property
						MetaTerm itemTerm = new MetaTerm(
								respTerm.getId(), 
								respTerm.getName());
						metaClass.addMetaTerm(itemTerm);
						
					}
					// log action
					if (this.verbose) {
						blackboardOutgoingQueue.add(new LogIngoing( 
								"[" + this.getClass().getName() + "]: Found #" + respClass.getMetaAnnotations() + 
								" Terms for Class Name: " + metaClass.getClassName(),
								LogType.INFO,
								ComponentList.LOG));
					}
					//
					classFound = true;
				}
			}
		}
		
		// set active job status
		jobActive.setParseStatus(4);
	
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
		 */
		private boolean verbose;
		
		/**
		 * 
		 * @param blackboard
		 */
		public ParseBlackboardProxyRead(
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
						"[" + this.getClass().getName() + "]: Blackboard PROXY Read Thread has started.",
						LogType.INFO,
						ComponentList.LOG));
			}
			
			// Infinite loop
			while (!Thread.currentThread().isInterrupted()) {

				// waits for a PROXYIN tuple
				Tuple tuple = this.blackboard.get(MatchTuple(TupleKey.PROXYOUT));
				String protocol = tuple.getData().toArray()[1].toString();
				
				// log action
				if (this.verbose) {
					blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: Blackboard Proxy message received.",
							LogType.INFO,
							ComponentList.LOG));
				}
				
				// prepare the new message to be sent
				receiveBLBMessage(
						protocol, 
						ComponentList.PROXY);	
			}
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
		 */
		private boolean verbose;
		
		/**
		 * 
		 * @param blackboard
		 */
		public ParseBlackboardAnnotationsRead(
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
						"[" + this.getClass().getName() + "]: Blackboard Annotation Read Thread has started.",
						LogType.INFO,
						ComponentList.LOG));
			}
			
			// Infinite loop
			while (!Thread.currentThread().isInterrupted()) {

				// waits for a PROXYIN tuple
				Tuple tuple = this.blackboard.get(MatchTuple(TupleKey.ANNOTATIONSOUT));
				String protocol = tuple.getData().toArray()[1].toString();
				
				// log action
				if (this.verbose) {
					blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: Blackboard Annotation message received.",
							LogType.INFO,
							ComponentList.LOG));
				}
				
				// prepare the new message to be sent
				receiveBLBMessage(
						protocol, 
						ComponentList.ANNOTATIONS);		
			}
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
		 */
		private boolean verbose;
		
		/**
		 * 
		 * @param blackboard
		 */
		public ParseBlackboardTermsRead(
				IBlackboard blackboard,
				boolean verbose) {
			this.blackboard = blackboard;
			
		}

		@Override
		public void run() {

			// log action
			if (this.verbose) {
				blackboardOutgoingQueue.add(new LogIngoing( 
						"[" + this.getClass().getName() + "]: Blackboard Terms Read Thread has started.",
						LogType.INFO,
						ComponentList.LOG));
			}
			
			// Infinite loop
			while (!Thread.currentThread().isInterrupted()) {

				// waits for a PROXYIN tuple
				Tuple tuple = this.blackboard.get(MatchTuple(TupleKey.TERMSOUT));
				String protocol = tuple.getData().toArray()[1].toString();
				
				// log action
				if (this.verbose) {
					blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: Blackboard Terms message received.",
							LogType.INFO,
							ComponentList.LOG));
				}
				
				// prepare the new message to be sent
				receiveBLBMessage(
						protocol, 
						ComponentList.TERMS);	
			}
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
							// the parse job is complete, so send a blackboard message 
							// to calculus component
							metaData = activeJob.getMetaData();
							ParseReadyOutgoing protocol = new ParseReadyOutgoing(
									entry.getKey(), 
									metaData,
									ComponentList.CALCULUS,
									activeJob.getRequestType());
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
