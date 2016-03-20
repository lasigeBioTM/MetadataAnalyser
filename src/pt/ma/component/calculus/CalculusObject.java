package pt.ma.component.calculus;

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
import pt.blackboard.protocol.CalculusDelegateOutgoing;
import pt.blackboard.protocol.CalculusReadyOutgoing;
import pt.blackboard.protocol.DigestReady;
import pt.blackboard.protocol.LogIngoing;
import pt.blackboard.protocol.MessageProtocol;
import pt.blackboard.protocol.OWLReadyOutgoing;
import pt.blackboard.protocol.ParseReadyOutgoing;
import pt.blackboard.protocol.enums.ComponentList;
import pt.blackboard.protocol.enums.RequestType;
import pt.ma.component.log.LogType;
import pt.ma.exception.InactiveJobException;
import pt.ma.metadata.MetaAnnotation;
import pt.ma.metadata.MetaClass;
import pt.ma.metadata.MetaData;
import pt.ma.util.StringWork;

/**
 * 
 * 
 *
 */
public class CalculusObject extends DSL {

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
	private Map<UUID, CalculusJob> metadataActiveJobs;
	
	/**
	 * 
	 */
	private boolean verbose;

	/**
	 * 
	 */
	private int threadLoop;
	
	/**
	 * 
	 * @param blackboard
	 * @param verbose
	 */
	public CalculusObject(
			IBlackboard blackboard,
			int threadLoop,
			boolean verbose) {
		
		//
		this.threadLoop = threadLoop;
		this.verbose = verbose;
		
		
		// assign blackboard instance
		this.blackboard = blackboard;
				
		// start active parse jobs data structure
		this.metadataActiveJobs = new ConcurrentHashMap<UUID, CalculusJob>();
		
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
		new Thread(new ParseBlackboardParseRead(
				this.blackboard, 
				this.verbose)).start();
		new Thread(new ParseBlackboardOWLRead(
				this.blackboard, 
				this.verbose)).start();
		
		// open a thread to check job completeness
		new Thread(new ParseProcessJobList(
				this.blackboard,
				this.metadataActiveJobs,
				this.blackboardOutgoingQueue, 
				this.verbose)).start();
		
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
		switch (source) {
		
			case PARSE:
				
				try {
					// a new message from Parse component
					ParseReadyOutgoing protocolParse = gson.fromJson(
							message, 
							ParseReadyOutgoing.class);
					
					// log action
					if (this.verbose) {
						blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: Blackboard message received " + 
									"from PARSE component, for Job ID: " + protocolParse.getUniqueID() + 
									". About to initiate CALCULUS parsing process." + 
									"to Client.",
							LogType.INFO,
							ComponentList.LOG));
					}
					
					// build a digest blackboard message
					int classCount = protocolParse.getBody().getMetaClasses().size();
					DigestReady digestProtocol = new DigestReady(
							protocolParse.getUniqueID(),
							"[" + StringWork.getNowDate() + "] " +
							"Metadata Calculus Parsing is about to start (Classes #" + classCount + ") for Job ID: " + 
							protocolParse.getUniqueID(),
							ComponentList.DIGEST); 
					sendBLBMessage(digestProtocol);

					// start calculation process for this request
					parseParseRequest(
							protocolParse.getUniqueID(),
							protocolParse.getBody(),
							protocolParse.getRequestType());
					
				} catch (InactiveJobException e) {
					// log action
					blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: An error as occured parsing a " + 
							"PARSE blackboard component message. Error: " + e.getMessage(),
							LogType.ERROR,
							ComponentList.LOG));
				}
				break;
				
			case OWL:
				
				try {
					// a new message from the OWL component				
					OWLReadyOutgoing protocolOWL = gson.fromJson(
							message, 
							OWLReadyOutgoing.class);
					
					// log action
					if (this.verbose) {
						blackboardOutgoingQueue.add(new LogIngoing( 
								"[" + this.getClass().getName() + "]: Blackboard message received " + 
										"from OWL component, for Job ID: " + protocolOWL.getUniqueID() + 
										"." + 
										"to Client.",
								LogType.INFO,
								ComponentList.LOG));
					}
					
					// build a digest blackboard message
					String className = protocolOWL.getBody().getClassName();
					DigestReady digestProtocol = new DigestReady(
							protocolOWL.getUniqueID(),
							"[" + StringWork.getNowDate() + "] " +
							"Metadata Calculus Parsing concluded for Class " + className + " in Job ID: " + 
							protocolOWL.getUniqueID(),
							ComponentList.DIGEST); 
					sendBLBMessage(digestProtocol);

					// start OWL response parsing
					parseOWLResponse(
							protocolOWL.getUniqueID(), 
							protocolOWL.getBody());
					
				} catch (InactiveJobException e) {
					// log action
					blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: An error as occured parsing a " + 
							"OWL blackboard component message. Error: " + e.getMessage(),
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

			case OWL:
				// log action
				if (this.verbose) {
					blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: About to send a Blackboard message " + 
							"to OWL Component, for Job ID: " + protocol.getUniqueID(),
							LogType.INFO,
							ComponentList.LOG));
				}
				
				// blackboard message to owl component
				message = gson.toJson((CalculusDelegateOutgoing)protocol);
				blackboard.put(Tuple(TupleKey.OWLIN, message));
				break;
				
			case PROXY:
				// log action
				if (this.verbose) {
					blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: About to send a Blackboard message " + 
							"to PROXY Component, for Job ID: " + protocol.getUniqueID(),
							LogType.INFO,
							ComponentList.LOG));
				}

				// blackboard message to proxy component
				message = gson.toJson((CalculusReadyOutgoing)protocol);
				blackboard.put(Tuple(TupleKey.PROXYIN, message));
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
	 * @param requestBody
	 * @throws InactiveJobException 
	 */
	private void parseParseRequest(
			UUID jobUUID, 
			MetaData requestBody,
			RequestType requestType) 
					throws InactiveJobException {
		
		// add to active jobs list
		CalculusJob jobActive = new CalculusJob(
				jobUUID, 
				requestBody, 
				requestType);
		metadataActiveJobs.put(jobUUID, jobActive);
		
		// ask OWL component for each class annotations specificity
		MetaData metaData = jobActive.getMetaData();
		for (MetaClass metaClass : metaData.getMetaClasses()) {					

			// set reference to this job class 
			jobActive.setJobTask(metaClass);
				
			// delegate class search job
			CalculusDelegateOutgoing classProtocol = new CalculusDelegateOutgoing(
					jobUUID,
					metaClass,
					ComponentList.OWL);
			blackboardOutgoingQueue.add(classProtocol);

		}
	}
	
	/**
	 * 
	 * @param jobUUID
	 * @param respClass
	 * @throws InactiveJobException
	 */
	private void parseOWLResponse(
			UUID jobUUID, 
			MetaClass respClass) throws InactiveJobException {
		
		// retrieve metadata instance from active job
		if (!metadataActiveJobs.containsKey(jobUUID)) {
			throw new InactiveJobException(
					"CalculusObject:parseOWLResponse - Invalid Job UUID: " 
					+ jobUUID);
		}
		CalculusJob jobActive = metadataActiveJobs.get(jobUUID);
		
		// log action
		if (this.verbose) {
			blackboardOutgoingQueue.add(new LogIngoing( 
					"[" + this.getClass().getName() + "]: About to parse OWL component response, " + 
					"for Job ID: " + jobUUID + ", regarding Class Name: " + respClass.getClassName(),
					LogType.INFO,
					ComponentList.LOG));
		}
		
		// collect the specificity value from OWL response
		MetaClass metaClass = setClassAnnotationsSpecValues(
				jobUUID, 
				jobActive.getMetaData(), 
				respClass);
		
		// calculate specificity average value for each class item in metadata
		calculateClassAvgSpecValue(
				jobUUID, 
				metaClass);
		
		// calculate coverage value for each class item in metadata
		calculateClassAvgCovValue(
				jobUUID, 
				metaClass);
		
		// calculate meta data specificity and coverage values
		calculateMetaOverAllValues(
				jobUUID, 
				jobActive.getMetaData());		
		
		// log action
		if (this.verbose) {
			blackboardOutgoingQueue.add(new LogIngoing( 
					"[" + this.getClass().getName() + "]: Calculus completed for Class Name: " + 
					respClass.getClassName() + ", Average Specificity: " + metaClass.getSpecValue() + 
					", Coverage Value: " + metaClass.getCovValue(),
					LogType.INFO,
					ComponentList.LOG));
		}
		
		// remove this class reference from job list
		jobActive.completeJobTask(metaClass);
		
	}
	
	/**
	 * Determines the specificity and coverage meta-data values. The specificity value is 
	 * calculated over the average of all annotations present in the study. The coverage 
	 * value is calculated taking into account all the annotated and non-annotated terms.
	 * 
	 * @param jobUUID
	 * @param metaData
	 */
	private void calculateMetaOverAllValues(
			UUID jobUUID,
			MetaData metaData) {
		
		// average specificity and coverage values
		double avgMetaSpec = 0f; double avgMetaCov = 0f;
		
		// iterate over all classes in meta-data
		int metaTermCounter = 0;
		int metaAnnoCounter = 0; int metaAnnoNACounter = 0;
		for (MetaClass metaClass : metaData.getMetaClasses()) {
			// iterate over each class annotation collection
			for (MetaAnnotation metaAnnotation : metaClass.getMetaAnnotations()) {
				double specValue = metaAnnotation.getSpecValue();
				if (specValue >= 0) {
					// increment global meta specification value
					avgMetaSpec += specValue;
					
				} else {
					// no value was found for this annotation, so this one will not 
					// be considered for meta specificity calculation
					metaAnnoNACounter++;
					
				}
				// increment global meta-data annotations counter
				metaAnnoCounter++;
			}
			// sum all used terms in the classes
			metaTermCounter += metaClass.getMetaTerms().size();
		}
				
		// determine average meta specificity value taking only in consideration 
		// annotations with values over 0
		double validAnnotations = Double.valueOf(metaAnnoCounter - metaAnnoNACounter);
		metaData.setSpecValue(avgMetaSpec);
		if (avgMetaSpec > 0) {
			avgMetaSpec = (double)(avgMetaSpec / validAnnotations); 
			metaData.setSpecValue(avgMetaSpec);
		}

		// determine average coverage ratio between annotations and terms
		metaData.setCovValue(avgMetaCov);
		if (validAnnotations > 0 && metaTermCounter > 0) {
			avgMetaCov = (double)(validAnnotations / Double.valueOf(metaTermCounter)); 
			metaData.setCovValue(avgMetaCov);
		}
	}
	
	/**
	 * @requires a valid uuid job
	 * @param jobUUID
	 * @param metaClass
	 * 	 */
	private MetaClass setClassAnnotationsSpecValues(
			UUID jobUUID,
			MetaData metaData,
			MetaClass respClass) {
		MetaClass metaClass = null;
		
		// iterate through all meta classes and set the spec value
		boolean classFound = false;
		Iterator<MetaClass> classIterator = metaData.getMetaClasses().iterator();
		while(classIterator.hasNext() && !classFound) {
			MetaClass itemClass = classIterator.next();
			
			// is this the meta class we are searching for 
			if (respClass.equals(itemClass)) {
				
				// iterate trough all response annotations and set the new value
				for (MetaAnnotation respAnnotation : respClass.getMetaAnnotations()) {
					boolean annoFound = false;
					Iterator<MetaAnnotation> annoIterator = itemClass.getMetaAnnotations().iterator();
					while(annoIterator.hasNext() && !annoFound) {
						MetaAnnotation itemAnno = annoIterator.next();
						// is this the meta annotation we are searching for
						if (respAnnotation.equals(itemAnno)) {
							// set class annotation spec value
							itemAnno.setSpecValue(respAnnotation.getSpecValue());
							annoFound = true;
						}
					}
				}
				metaClass = itemClass;
				classFound = true;
			}
		}
		//
		return metaClass;
	}
	
	/**
	 * Determine the average specificity value for the given meta class. Only annotations found 
	 * in the database are eligible for calculation.
	 * 
	 * @param jobUUID engine job unique identifier
	 * @param metaClass the class used to calculate the average value
	 */
	private void calculateClassAvgSpecValue(
			UUID jobUUID,
			MetaClass metaClass) {
		
		// average specificity and coverage values
		double avgClassSpec = 0f;
		
		// iterate trough all meta class annotations
		int metaAnnoCounter = 0; int metaAnnoNACounter = 0;
		for (MetaAnnotation metaAnnotation : metaClass.getMetaAnnotations()) {
			double specValue = metaAnnotation.getSpecValue();
			if (specValue >= 0) {
				// increment global class specification value
				avgClassSpec += specValue;
				
			} else {
				// no value was found for this annotation, so this one will not 
				// be considered for class specificity calculation
				metaAnnoNACounter++;
				
			}
			// increment global class annotations counter
			metaAnnoCounter++;
		}
		
		// calculate and set average specificity value for this class
		double validAnnotations = Double.valueOf(metaAnnoCounter - metaAnnoNACounter);
		if (avgClassSpec > 0) {
			avgClassSpec = (double)(avgClassSpec / validAnnotations);
		}
		metaClass.setSpecValue(avgClassSpec);
	}

	/**
	 * Determine the average coverage value for the given meta class. The coverage is calculated 
	 * by finding the ratio between all valid annotations, i.e. the ones that a specificity was 
	 * found, and all class used terms.
	 * 
	 * @param jobUUID engine job unique identifier
	 * @param metaClass the class used to calculate the average value
	 */
	private void calculateClassAvgCovValue(
			UUID jobUUID,
			MetaClass metaClass) {
		
		// average coverage value, defaults to 0
		double avgCovValue = 0f;
		
		// determine valid annotations counter
		// iterate trough all meta class annotations
		int metaAnnoCounter = 0; int metaAnnoNACounter = 0;
		for (MetaAnnotation metaAnnotation : metaClass.getMetaAnnotations()) {
			double specValue = metaAnnotation.getSpecValue();
			if (specValue < 0) {
				// no value was found for this annotation, so this one will not 
				// be considered for class specificity calculation
				metaAnnoNACounter++;
				
			}
			// increment annotations global class counter
			metaAnnoCounter++;
		}

		// calculate average specificity value for this class
		double termCounter = Double.valueOf(metaClass.getMetaTerms().size()); 
		double validAnnotations = Double.valueOf(metaAnnoCounter - metaAnnoNACounter);		
		if (validAnnotations > 0 && termCounter > 0) {
			avgCovValue = (double)(validAnnotations / termCounter);
		}
		metaClass.setCovValue(avgCovValue);

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
				Tuple tuple = this.blackboard.get(MatchTuple(TupleKey.PARSEOUT));
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
	private class ParseBlackboardOWLRead extends DSL implements Runnable {

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
		public ParseBlackboardOWLRead(
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
						"[" + this.getClass().getName() + "]: Blackboard OWL Read Thread has started.",
						LogType.INFO,
						ComponentList.LOG));
			}
			
			// Infinite loop
			while (!Thread.currentThread().isInterrupted()) {

				// waits for a PARSEOUT tuple
				Tuple tuple = this.blackboard.get(MatchTuple(TupleKey.OWLOUT));
				String protocol = tuple.getData().toArray()[1].toString();
				
				// log action
				if (this.verbose) {
					blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: Blackboard OWL message received.",
							LogType.INFO,
							ComponentList.LOG));
				}
				
				// process request sent from owl component
				receiveBLBMessage(
						protocol, 
						ComponentList.OWL);
				
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
		private Map<UUID, CalculusJob> metadataActiveJobs;
		
		/**
		 * 
		 */
		private boolean verbose;
		
		/**
		 * 
		 * @param blackboard
		 * @param outgoingQueue
		 */
		public ParseProcessJobList(
				IBlackboard blackboard, 
				Map<UUID, CalculusJob> metadataActiveJobs,
				Queue<MessageProtocol> outgoingQueue,
				boolean verbose) {
			
			this.blackboard = blackboard;
			this.metadataActiveJobs = metadataActiveJobs;
			this.outgoingQueue = outgoingQueue;
			this.verbose = verbose;
		}

		@Override
		public void run() {
			
			// Infinite loop
			while (!Thread.currentThread().isInterrupted()) {
				
				// check if there are any ready jobs
				ArrayList<UUID> jobsToDelete = new ArrayList<UUID>();
				for (Entry<UUID, CalculusJob> entry : metadataActiveJobs.entrySet()) {
					
					CalculusJob activeJob = entry.getValue();
					if (activeJob.isTaskListComplete() ) {
						
						// get all meta data file information gathered
						MetaData metaData = activeJob.getMetaData();
						
						// log action
						if (this.verbose) {
							blackboardOutgoingQueue.add(new LogIngoing( 
									"[" + this.getClass().getName() + "]: Calculus Job ID: " + entry.getKey() + "is complet.",
									LogType.INFO,
									ComponentList.LOG));
						}
										
						// set completion time stamp
						metaData.setParseDuration(System.currentTimeMillis());
						
						// build a digest blackboard message
						DigestReady digestProtocol = new DigestReady(
								entry.getKey(),
								"[" + StringWork.getNowDate() + "] " +
								"Metadata Calculus Parsing has ended for Job ID: " + 
								entry.getKey(),
								ComponentList.DIGEST); 
						sendBLBMessage(digestProtocol);
						
						// send a blackboard message to proxy component
						CalculusReadyOutgoing protocol = new CalculusReadyOutgoing(
								entry.getKey(), 
								metaData,
								ComponentList.PROXY);
						sendBLBMessage(protocol);
						
						// add this job to deletion job list
						jobsToDelete.add(entry.getKey());
						
					}
				}
				
				// delete all processed jobs
				for (UUID entry : jobsToDelete) {
					metadataActiveJobs.remove(entry);
				}
				
				// wait for 5 seconds
				try {
					Thread.sleep(threadLoop);
				} catch (InterruptedException e) {
					// TODO: log action
					
				}
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
			//
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
						Thread.sleep(threadLoop);
					} catch (InterruptedException e) {
						// TODO: log action
						
					}
				}
			}

		}

	}
	
	
}
