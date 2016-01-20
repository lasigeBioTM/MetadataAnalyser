package pt.ma.component.log;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import pt.blackboard.DSL;
import pt.blackboard.IBlackboard;
import pt.blackboard.Tuple;
import pt.blackboard.TupleKey;
import pt.blackboard.protocol.MessageProtocol;
import pt.blackboard.protocol.enums.ComponentList;

/**
 * 
 * 
 *
 */
public class LogObject extends DSL {
	
	/**
	 * 
	 */
	private IBlackboard blackboard;
	
	/**
	 * 
	 * @param blackboard
	 */
	public LogObject(IBlackboard blackboard) {
		
		// assign blackboard instance
		this.blackboard = blackboard;
		
		// open threads for reading from blackboard
		new Thread(new ParseBlackboardAllRead(this.blackboard)).start();
				
	}
	
	// PRIVATE METHODS
	
	/**
	 * 
	 * @param message
	 * @param source
	 */
	private void receiveBLBMessage(String message) {

		
	}
	// PRIVATE CLASSES
	
	/**
	 * 
	 * 
	 *
	 */
	private class ParseBlackboardAllRead extends DSL implements Runnable {

		/**
		 * Locally managed blackboard instance
		 */
		private IBlackboard blackboard;
		
		/**
		 * 
		 * @param blackboard
		 */
		public ParseBlackboardAllRead(
				IBlackboard blackboard) {
			this.blackboard = blackboard;
			
		}

		@Override
		public void run() {

			// TODO: Logging action
			
			
			// Infinite loop
			while (!Thread.currentThread().isInterrupted()) {

				// waits for a PARSEOUT tuple
				Tuple tuple = this.blackboard.get(MatchTuple(TupleKey.LOGIN));
				String protocol = tuple.getData().toArray()[1].toString();
				
				// TODO: Logging action
				
				// process request sent from parse component
				receiveBLBMessage(protocol);
				
			}
			
			// TODO: Logging action

		}
	}	
		
}
