package pt.ma.component.log;

import com.google.gson.Gson;

import pt.blackboard.DSL;
import pt.blackboard.IBlackboard;
import pt.blackboard.Tuple;
import pt.blackboard.TupleKey;
import pt.blackboard.protocol.LogIngoing;

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
	 */
	private LogTarget logType;
	
	/**
	 * 
	 * @param blackboard
	 * @param logType
	 */
	public LogObject(
			IBlackboard blackboard, 
			LogTarget logType) {
		
		// assign blackboard instance
		this.blackboard = blackboard;
		
		// open threads for reading from blackboard
		new Thread(new ParseBlackboardAllRead(
				this.blackboard,
				this.logType)).start();
				
	}
	
	// PRIVATE METHODS
	
	/**
	 * 
	 * @param message
	 * @param source
	 */
	private void receiveBLBMessage(String message) {
		
		//
		// a new message from Parse component
		Gson gson = new Gson(); 
		LogIngoing protocolLog = gson.fromJson(
				message, 
				LogIngoing.class);

		switch (protocolLog.getLogType()) {
			case ERROR:
				AppHelper.logError(protocolLog.getBody());
				break;
			
			case ERRORWITHTHROWABLE:
				AppHelper.logError(
						protocolLog.getBody(), 
						protocolLog.getThrowable());
				break;
				
			default:
				AppHelper.logInfo(protocolLog.getBody());
				break;
				
		}
				
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
		 */
		private LogTarget logType;

		/**
		 * 
		 * @param blackboard
		 * @param logType
		 */
		public ParseBlackboardAllRead(
				IBlackboard blackboard,
				LogTarget logType) {
			this.blackboard = blackboard;
			this.logType = logType;
			
		}

		@Override
		public void run() {
			
			// Infinite loop
			while (!Thread.currentThread().isInterrupted()) {

				// waits for a PARSEOUT tuple
				Tuple tuple = this.blackboard.get(MatchTuple(TupleKey.LOGIN));
				String protocol = tuple.getData().toArray()[1].toString();
				
				// TODO: Logging action
				
				// process request sent from parse component
				receiveBLBMessage(protocol);
				
			}

		}
	}	
		
}
