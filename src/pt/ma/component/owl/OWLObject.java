package pt.ma.component.owl;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
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
import pt.blackboard.protocol.LogIngoing;
import pt.blackboard.protocol.MessageProtocol;
import pt.blackboard.protocol.OWLReadyOutgoing;
import pt.blackboard.protocol.enums.ComponentList;
import pt.ma.component.log.LogType;
import pt.ma.database.MySQLFactory;
import pt.ma.exception.InactiveJobException;
import pt.ma.metadata.MetaAnnotation;
import pt.ma.metadata.MetaClass;
import pt.ma.util.FileWork;

/**
 * 
 * 
 *
 *
 */
public class OWLObject extends DSL {

	/**
	 * 
	 */
	private OntologyList dataprefixs;
	
	/**
	 * 
	 */
	private IBlackboard blackboard;
	
	/**
	 * 
	 */
	private Map<String, Connection> connections;
	
	/**
	 * 
	 */
	private Map<String, Double> metadataCacheAnnotations;

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
	 */
	private boolean verbose;
	
	/**
	 * 
	 */
	private boolean cache;
	
	/**
	 * 
	 */
	private int threadLoop;
	
	/**
	 * 
	 */
	private String dbServerAddress;
	
	/**
	 * 
	 */
	private String dbUserName;
	
	/**
	 * 
	 */
	private String dbUserPassword;
	
	/**
	 * 
	 * @param blackboard
	 * @param verbose
	 */
	public OWLObject(
			IBlackboard blackboard, 
			boolean installDatabase,
			String dbName,
			String dbServerAddress,
			String dbUserName,
			String dbUserPassword,
			int threadLoop,
			boolean verbose,
			boolean cache) {
		
		//
		this.threadLoop = threadLoop;
		this.verbose = verbose;
		this.cache = cache;
		
		// sets connections map
		connections	= new HashMap<String, Connection>();
		
		// establish the MySQL database parameters
		this.dbServerAddress = dbServerAddress;
		this.dbUserName = dbUserName;
		this.dbUserPassword = dbUserPassword;
		
		// establish the MySQL schema names
		Gson gson = new Gson();
		dataprefixs = gson.fromJson(FileWork.readContentIntoString(
				new File("configfiles/ontology-list.json")), 
				OntologyList.class); 
				
		// installs a new database schema if necessary
		if (installDatabase) {
			new Thread(new DatabaseInstallation()).start();
			
		} else {
			this.isDBAvailable = true;
			
		}
		
		// assign blackboard instance
		this.blackboard = blackboard;

		// instantiate annotation cache map
		this.metadataCacheAnnotations = new ConcurrentHashMap<String, Double>();
		
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
	 * @param uri
	 * @return
	 */
	private Connection getAnnotationConnection(String uri) {
		
		// find a schema prefix for the given annotation URI
		String dbname = "owltosql";
		boolean foundprefix = false; int counter = 0;
		while (!foundprefix && counter < dataprefixs.getOntologies().size()) {
			// find a prefix database match
			String dataprefix = dataprefixs.getOntologies().get(counter);			
			if (uri.trim().toLowerCase().contains(dataprefix.toLowerCase())) {
				// build schema name for this prefix				
				dbname += "_" + dataprefix;
				foundprefix = true;
			}
			counter++;
		}

		// is there a prefix match in annotation URI
		Connection result = null;
		if (foundprefix) {
			// is there already an opened connection for this database
			if (connections.containsKey(dbname)) {
				// there's a match so use the available connection
				result = connections.get(dbname);
				try {
					if (result.isClosed()) {
						result = openConnection(dbname);
					}
					
				} catch (Exception e) {
					// log action
					if (this.verbose) {
						blackboardOutgoingQueue.add(new LogIngoing( 
								"[" + this.getClass().getName() + "]: A SQL exception as occurred, " + 
										" attempting to reopen an existing database conection for database " + dbname +  
										", with message: " + e.getMessage(),
								LogType.ERROR,
								ComponentList.LOG));
					}
				}
								
			} else {
				
				try {
					// open a new connection for the given database
					result = openConnection(dbname);		
					// add it to the map collection
					connections.put(dbname, result);
				
				} catch (Exception e) {
					// log action
					if (this.verbose) {
						blackboardOutgoingQueue.add(new LogIngoing( 
								"[" + this.getClass().getName() + "]: A SQL exception as occurred, " + 
										" attempting to open a new database connection for database " + dbname +  
										", with message: " + e.getMessage(),
								LogType.ERROR,
								ComponentList.LOG));
					}
				}
			}			
		} 
		//
		return result;
	}

	/**
	 * 
	 * @param dbname
	 * @return
	 * @throws Exception 
	 * @throws SQLException 
	 */
	private Connection openConnection(String dbname) throws SQLException, Exception {
		
		// open a new connection for the given database
		MySQLFactory factory = MySQLFactory.getInstance();
		
		// set new database connection parameters
		factory.setDriver("com.mysql.jdbc.Driver");
		factory.setURL("jdbc:mysql://" + dbServerAddress + "/" + dbname);
		factory.setAuthentication(dbUserName, dbUserPassword);
		Connection result = factory.getConnection();
		
		//
		return result;
	}
	
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

					// a new message from Parse component
					CalculusDelegateOutgoing protocolParse = gson.fromJson(
							message, 
							CalculusDelegateOutgoing.class);
					parseCalculusRequest(
							protocolParse.getUniqueID(),
							protocolParse.getBody());
					
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
	private void parseCalculusRequest(
			UUID jobUUID, 
			MetaClass requestBody)  {
				
		// log action
		if (this.verbose) {
			blackboardOutgoingQueue.add(new LogIngoing( 
					"[" + this.getClass().getName() + "]: About to calculate specificity values for Job ID: " + jobUUID,
					LogType.INFO,
					ComponentList.LOG));
		}
		
		// iterate through all annotations from the meta class
		//Connection database = MySQLLogin.getConnection();	
		for (MetaAnnotation metaAnno : requestBody.getMetaAnnotations()) {
			String conceptIRI = null;
			try {
				// get specificity value from DB
				conceptIRI = metaAnno.getURI().toLowerCase();
				
				// check if there's already an available annotation value
				if (cache & metadataCacheAnnotations.containsKey(conceptIRI)) {
					
					// recover cached concept specificty value
					metaAnno.setSpecValue(metadataCacheAnnotations.get(conceptIRI));
					
				} else {
					
					try {
						// there's no cached value for this concept, so try to figure it out
						double specValue = -1f;
						Connection database = getAnnotationConnection(conceptIRI);
						if (database != null && !database.isClosed()) {
							// query about annotation specificity value
							CallableStatement statement = database.prepareCall("{call sp_conceptspec(?, ?)}");
							statement.setString("concept_iri", conceptIRI);
							statement.registerOutParameter("spec_value", Types.NUMERIC);
							statement.execute();
							specValue = statement.getDouble("spec_value");
							statement.close();
							
						}
						metaAnno.setSpecValue(specValue);

						// if no result was returned do some log action
						if (specValue < 0) {						
							// log action
							if (this.verbose) {
								blackboardOutgoingQueue.add(new LogIngoing( 
										"[" + this.getClass().getName() + "]: No ontology was found " + 
												"for concept: " + conceptIRI,
										LogType.INFO,
										ComponentList.LOG));
							}
						}
						
						// add specificity concept value to cache map
						if (cache) {
							metadataCacheAnnotations.put(
									conceptIRI, 
									metaAnno.getSpecValue());
						}
						
					} catch (SQLException e) {
						// log action
						if (this.verbose) {
							blackboardOutgoingQueue.add(new LogIngoing( 
									"[" + this.getClass().getName() + "]: A SQL exception as occurred, " + 
											" attempting to create a callable statement from database object" +  
											", with message: " + e.getMessage(),
									LogType.ERROR,
									ComponentList.LOG));
						}
					}
				}
								
			} catch (Exception e) {
				// log action
				if (this.verbose) {
					blackboardOutgoingQueue.add(new LogIngoing( 
							"[" + this.getClass().getName() + "]: A exception as occurred, " + 
									"calculanting specificity value for concept: " + conceptIRI + 
									", with message: " + e.getMessage(),
							LogType.ERROR,
							ComponentList.LOG));
				}
			}
		}
					
		// send results back to calculus component
		OWLReadyOutgoing classProtocol = new OWLReadyOutgoing(
				jobUUID,
				requestBody,
				ComponentList.CALCULUS);
		blackboardOutgoingQueue.add(classProtocol);
		
		// log action
		if (this.verbose) {
			blackboardOutgoingQueue.add(new LogIngoing( 
					"[" + this.getClass().getName() + "]: Specificity calculus is over for Job ID: " + jobUUID,
					LogType.INFO,
					ComponentList.LOG));
		}
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
			//Application.main(args);
			
			//
			isDBAvailable = true;
		}
		
	}
}
