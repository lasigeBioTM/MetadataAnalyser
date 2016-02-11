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
	 * @param blackboard
	 * @param verbose
	 */
	public CalculusObject(
			IBlackboard blackboard, 
			boolean verbose) {
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
					// a new message from OWL component				
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
	 * 
	 * @param jobUUID
	 * @param metaData
	 */
	private void calculateMetaOverAllValues(
			UUID jobUUID,
			MetaData metaData) {
		
		// sum up all class specificity and coverage values
		double sumSpecs = 0f; double sumCovs = 0f;
		for (MetaClass metaClass : metaData.getMetaClasses()) {
			sumSpecs += metaClass.getSpecValue();
			sumCovs += metaClass.getCovValue();
		}
		
		// meta data overall specificity value
		metaData.setSpecValue(sumSpecs);
		if (sumSpecs > 0) {
			metaData.setSpecValue(
					sumSpecs / 
					Double.valueOf(metaData.getMetaClasses().size()));
		}

		// meta data overall specificity value
		metaData.setCovValue(sumCovs);
		if (sumCovs > 0) {
			metaData.setCovValue(
					sumCovs / 
					Double.valueOf(metaData.getMetaClasses().size()));
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
	 * 
	 * @param jobUUID
	 * @param metaData
	 */
	private void calculateClassAvgSpecValue(
			UUID jobUUID,
			MetaClass metaClass) {
					
		// iterate trough all meta class annotations
		double avgClassSpec = 0f;
		List<MetaAnnotation> offsetAnnotations = new ArrayList<MetaAnnotation>();
		for (MetaAnnotation metaAnnotation : metaClass.getMetaAnnotations()) {
			double specValue = metaAnnotation.getSpecValue();
			if (specValue >= 0) {
				// increment global class specification value
				avgClassSpec += specValue;
				
			} else {
				// no value was found for this annotation, so this one will not 
				// be considered for class specificity calculation
				offsetAnnotations.add(metaAnnotation);
			}
		}
		
		// set denominator specificity value
		int specDenominator = (
				metaClass.getMetaAnnotations().size() - 
				offsetAnnotations.size());
		
		// calculate average specificity value for this class
		if (avgClassSpec > 0 && specDenominator > 0) {
			avgClassSpec = (double)(
					Double.valueOf(avgClassSpec) / 
					Double.valueOf(specDenominator));
			
		} else {
			avgClassSpec = 0f;
			
		}
		metaClass.setSpecValue(avgClassSpec);
		
	}

	/**
	 * 
	 * @param jobUUID
	 * @param metaData
	 */
	private void calculateClassAvgCovValue(
			UUID jobUUID,
			MetaClass metaClass) {
					
		// calculate average specificity value for this class
		double avgCovValue = 0f;
		if (metaClass.getMetaAnnotations().size() > 0 && 
				metaClass.getMetaTerms().size() > 0) {
			avgCovValue = (double)
				   (Double.valueOf(metaClass.getMetaAnnotations().size()) / 
						   Double.valueOf(metaClass.getMetaTerms().size()));
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
						
						// send a blackboard message to calculus component
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
					Thread.sleep(5000);
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
