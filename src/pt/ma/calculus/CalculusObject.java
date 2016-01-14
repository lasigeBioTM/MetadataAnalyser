package pt.ma.calculus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
import pt.blackboard.protocol.CalculusDelegateOutgoing;
import pt.blackboard.protocol.CalculusReadyOutgoing;
import pt.blackboard.protocol.MessageProtocol;
import pt.blackboard.protocol.OWLOutgoing;
import pt.blackboard.protocol.ParseReadyOutgoing;
import pt.blackboard.protocol.enums.ComponentList;
import pt.ma.parse.MetaClass;
import pt.ma.parse.MetaData;

/**
 * 
 * @author Bruno
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
	public CalculusObject(
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
		UUID jobUUID = null; MetaData jobActive = null; 
		switch (source) {
		
			case PARSE:
				// a new message from Parse component
				ParseReadyOutgoing protocolParse = gson.fromJson(
						message, 
						ParseReadyOutgoing.class);
				
				// add request to active job list
				jobActive = protocolParse.getBody();
				jobUUID = protocolParse.getUniqueID();
				metadataActiveJobs.put(jobUUID, jobActive);
				
				// ask OWL component for class specificity
				for (MetaClass metaClass : jobActive.getMetaClasses()) {					
					// delegate class search job
					CalculusDelegateOutgoing classProtocol = new CalculusDelegateOutgoing(
							jobUUID,
							metaClass,
							ComponentList.OWL);
					blackboardOutgoingQueue.add(classProtocol);

				}
				
				break;
				
			case OWL:
				// a new message from OWL component				
				OWLOutgoing protocolOWL = gson.fromJson(
						message, 
						OWLOutgoing.class);
				
				// collect the specificity value from OWL response
				jobUUID = protocolOWL.getUniqueID();
				MetaClass bodyOWL = protocolOWL.getBody();
				setMetaClassSpecValues(jobUUID, bodyOWL);
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
	 * @param metaClass
	 */
	private void setMetaClassSpecValues(
			UUID jobUUID, 
			MetaClass metaClass) {
		
		// get active job for uuid
		MetaData jobActive = metadataActiveJobs.get(jobUUID);
		
		// iterate through all meta classes and set the spec value
		boolean classFound = false;
		Iterator<MetaClass> classIterator = jobActive.getMetaClasses().iterator();
		while(classIterator.hasNext() && !classFound) {
			MetaClass itemClass = classIterator.next();
			
			// is this the meta class we are searching for 
			if (metaClass.equals(itemClass)) {
				// set spec and cov values
				itemClass.setSpecValue(itemClass.getSpecValue());
				itemClass.setCovValue(itemClass.getCovValue());
				classFound = true;
				
				// TODO: log action
				
			}
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
