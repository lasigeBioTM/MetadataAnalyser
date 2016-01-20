package pt.ma.component.owl;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.gson.Gson;

import pt.blackboard.DSL;
import pt.blackboard.IBlackboard;
import pt.blackboard.Tuple;
import pt.blackboard.TupleKey;
import pt.blackboard.protocol.CalculusDelegateOutgoing;
import pt.blackboard.protocol.MessageProtocol;
import pt.blackboard.protocol.OWLReadyOutgoing;
import pt.blackboard.protocol.enums.ComponentList;
import pt.ma.database.MySQLLogin;
import pt.ma.exception.InactiveJobException;
import pt.ma.metadata.MetaAnnotation;
import pt.ma.metadata.MetaClass;
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
			boolean installDatabase,
			boolean verbose) {
		
		// start database installation
		if (installDatabase) {
			new Thread(new DatabaseInstallation()).start();
			
		} else {
			this.isDBAvailable = true;
			
		}
		
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
		
			case CALCULUS:
				try {
					// a new message from Parse component
					CalculusDelegateOutgoing protocolParse = gson.fromJson(
							message, 
							CalculusDelegateOutgoing.class);
					parseCalculusRequest(
							protocolParse.getUniqueID(),
							protocolParse.getBody());
					
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
						
			case CALCULUS:
				// blackboard message to proxy component
				message = gson.toJson((OWLReadyOutgoing)protocol);
				blackboard.put(Tuple(TupleKey.OWLOUT, message));
				
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
	private void parseCalculusRequest(
			UUID jobUUID, 
			MetaClass requestBody) throws InactiveJobException {
				
		// TODO: log action
		
		try {
			// iterate through all annotations from the meta class
			Connection database = MySQLLogin.getConnection();
			CallableStatement statement = database.prepareCall("{call sp_conceptspec(?, ?)}");
			for (MetaAnnotation metaAnno : requestBody.getMetaAnnotations()) {
				
				try {
					// get specificity value from DB
					String conceptIRI = metaAnno.getURI();
					statement.setString("concept_iri", conceptIRI);
					statement.registerOutParameter("spec_value", Types.NUMERIC);
					statement.execute();
					double specValue = statement.getDouble("spec_value");
					if (specValue >= 0) {
						metaAnno.setSpecValue(specValue);
						
					} else {
						// no result was returned, specify default value
						metaAnno.setSpecValue(-1f);
						
						// TODO log action
					}
					
				} catch (Exception e) {
					// TODO log action
					
				}
				

			}
			
		} catch (SQLException e) {
			// TODO log action

		}
		
		// send results back to calculus component
		OWLReadyOutgoing classProtocol = new OWLReadyOutgoing(
				jobUUID,
				requestBody,
				ComponentList.CALCULUS);
		blackboardOutgoingQueue.add(classProtocol);
		
		// TODO: log action

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
				Tuple tuple = this.blackboard.get(MatchTuple(TupleKey.OWLIN));
				String protocol = tuple.getData().toArray()[1].toString();
				
				// TODO: Logging action
				
				// process request sent from parse component
				receiveBLBMessage(protocol, ComponentList.CALCULUS);
				
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
