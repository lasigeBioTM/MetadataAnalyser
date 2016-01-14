package pt.ma.owl;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import pt.blackboard.DSL;
import pt.blackboard.IBlackboard;
import pt.blackboard.Tuple;
import pt.blackboard.TupleKey;
import pt.blackboard.protocol.MessageProtocol;
import pt.blackboard.protocol.enums.ComponentList;
import pt.owlsql.Application;

/**
 * 
 * 
 *
 */
public class OWLObject extends DSL {

	
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
	private boolean isDBAvailable;

	/**
	 * 
	 * @param blackboard
	 * @param verbose
	 */
	public OWLObject(
			IBlackboard blackboard, 
			boolean verbose) {
		
		// start database instalation
		new Thread(new DatabaseInstallation()).start();
		
		// assign blackboard instance
		this.blackboard = blackboard;
				
		// set blackboard outgoing messages queue
		this.blackboardOutgoingQueue = new LinkedBlockingQueue<MessageProtocol>();

		// open threads for reading from blackboard
		new Thread(new ParseBlackboardCalculusRead(this.blackboard)).start();
				
		// open a thread for writing to the blackboard
		new Thread(new ParseBlackboardWrite(
				this.blackboard, 
				blackboardOutgoingQueue)).start();

	}
	
	// PRIVATE CLASSES
	
	/**
	 * 
	 * 
	 *
	 */
	private class ParseBlackboardCalculusRead extends DSL implements Runnable {

		/**
		 * Locally managed blackboard instance
		 */
		private IBlackboard blackboard;
		
		/**
		 * 
		 * @param blackboard
		 */
		public ParseBlackboardCalculusRead(
				IBlackboard blackboard) {
			this.blackboard = blackboard;
			
		}

		@Override
		public void run() {

			// TODO: Logging action
			
			
			// Infinite loop
			while (!Thread.currentThread().isInterrupted()) {

				// waits for a PARSEOUT tuple
				Tuple tuple = this.blackboard.get(MatchTuple(TupleKey.CALCULUSOUT));
				String protocol = tuple.getData().toArray()[1].toString();
				
				// TODO: Logging action
				
				// process request sent from parse component
//				receiveBLBMessage(protocol, ComponentList.CALCULUS);
				
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
//					sendBLBMessage(outgoingQueue.poll());
									
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
	private class DatabaseInstallation implements Runnable {

		@Override
		public void run() {
			
			// call static class OWLSQL implementation
			String[] args = new String[2];
			args[0] = "-u";
			args[1] = "file";
			Application.main(args);
			
			//
			isDBAvailable = true;
		}
		
	}
}
