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
import pt.blackboard.protocol.MessageProtocol;
import pt.blackboard.protocol.OWLReadyOutgoing;
import pt.blackboard.protocol.ParseReadyOutgoing;
import pt.blackboard.protocol.enums.ComponentList;
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

		// open threads for reading from blackboard
		new Thread(new ParseBlackboardParseRead(this.blackboard)).start();
		new Thread(new ParseBlackboardOWLRead(this.blackboard)).start();
		
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
					parseParseRequest(
							protocolParse.getUniqueID(),
							protocolParse.getBody());
					
				} catch (InactiveJobException e) {
					// TODO log action

				}
				break;
				
			case OWL:
				try {
					// a new message from OWL component				
					OWLReadyOutgoing protocolOWL = gson.fromJson(
							message, 
							OWLReadyOutgoing.class);
					parseOWLResponse(
							protocolOWL.getUniqueID(), 
							protocolOWL.getBody());
					
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
		
			case OWL:
				// blackboard message to owl component
				message = gson.toJson((CalculusDelegateOutgoing)protocol);
				blackboard.put(Tuple(TupleKey.OWLIN, message));
				
				// TODO: log action
				
				break;
				
			case PROXY:
				// blackboard message to proxy component
				message = gson.toJson((CalculusReadyOutgoing)protocol);
				blackboard.put(Tuple(TupleKey.PROXYIN, message));
				
				// TODO: log action
				
				break;

			default:
				// TODO: log action
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
			MetaData requestBody) throws InactiveJobException {
		
		// add to active jobs list
		CalculusJob jobActive = new CalculusJob(jobUUID, requestBody);
		metadataActiveJobs.put(jobUUID, jobActive);
		
		// TODO: log action
		
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
		
		// TODO: log action

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
		
		// retreive metadata instance from active job
		if (!metadataActiveJobs.containsKey(jobUUID)) {
			throw new InactiveJobException(
					"CalculusObject:parseOWLResponse - Invalid Job UUID: " 
					+ jobUUID);
		}
		CalculusJob jobActive = metadataActiveJobs.get(jobUUID);
		
		// TODO: log action
		
		// collect the specificity value from OWL response
		MetaClass metaClass = setMetaClassAnnotationsSpecValues(
				jobUUID, 
				jobActive.getMetaData(), 
				respClass);
		
		// set specificity average value for each class item in metadata
		setMetaClassAvgSpecValue(
				jobUUID, 
				metaClass);
		
		// set coverage value for each class item in metadata
		setMetaClassCovValue(
				jobUUID, 
				metaClass);
		
		// remove this class reference from job list
		jobActive.completeJobTask(metaClass);
		
	}
	
	/**
	 * @requires a valid uuid job
	 * @param jobUUID
	 * @param metaClass
	 * 	 */
	private MetaClass setMetaClassAnnotationsSpecValues(
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
				
				// TODO: log action
				
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
	private void setMetaClassAvgSpecValue(
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

				// TODO: log action
				
			}
		}
		
		// set denominator specificity value
		int specDenominator = (metaClass.getMetaAnnotations().size() - offsetAnnotations.size());
		
		// calculate average specificity value for this class
		if (avgClassSpec > 0 && specDenominator > 0) {
			avgClassSpec = (double)(
					Double.valueOf(avgClassSpec) / 
					Double.valueOf(specDenominator));
			
		} else {
			avgClassSpec = 0f;
			
		}
		metaClass.setSpecValue(avgClassSpec);
		
		// TODO: log action
		
	}

	/**
	 * 
	 * @param jobUUID
	 * @param metaData
	 */
	private void setMetaClassCovValue(
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
		
		// TODO: log action
		
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
				Tuple tuple = this.blackboard.get(MatchTuple(TupleKey.PARSEOUT));
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
	private class ParseBlackboardOWLRead extends DSL implements Runnable {

		/**
		 * Locally managed blackboard instance
		 */
		private IBlackboard blackboard;
		
		/**
		 * 
		 * @param blackboard
		 */
		public ParseBlackboardOWLRead(
				IBlackboard blackboard) {
			this.blackboard = blackboard;
			
		}

		@Override
		public void run() {

			// TODO: Logging action
			
			
			// Infinite loop
			while (!Thread.currentThread().isInterrupted()) {

				// waits for a PARSEOUT tuple
				Tuple tuple = this.blackboard.get(MatchTuple(TupleKey.OWLOUT));
				String protocol = tuple.getData().toArray()[1].toString();
				
				// TODO: Logging action
				
				// process request sent from owl component
				receiveBLBMessage(
						protocol, 
						ComponentList.OWL);
				
			}
			
			// TODO: Logging action

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
		 * @param blackboard
		 * @param outgoingQueue
		 */
		public ParseProcessJobList(
				IBlackboard blackboard, 
				Map<UUID, CalculusJob> metadataActiveJobs,
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
				for (Entry<UUID, CalculusJob> entry : metadataActiveJobs.entrySet()) {
					
					CalculusJob activeJob = entry.getValue();
					if (activeJob.isTaskListComplete() ) {
						
						// get all meta data file information gathered
						MetaData metaData = activeJob.getMetaData();
System.out.println(metaData);
						
						// TODO: log action
						
						
						// send a blackboard message to calculus component
						CalculusReadyOutgoing protocol = new CalculusReadyOutgoing(
								entry.getKey(), 
								metaData,
								ComponentList.PROXY);
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
