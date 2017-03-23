package pt.ma.webservice;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import pt.ma.component.proxy.network.Interface;
import pt.ma.component.proxy.network.Message;
import pt.ma.component.proxy.network.MessageType;
import pt.ma.database.MySQLLogin;
import pt.ma.protocol.UploadFileResult;
import pt.ma.util.FileWork;
import pt.ma.util.StringWork;

/**
 * 
 * 
 *
 */
@Path("/metadata")
public class MetaRequest {

	/**
	 * 
	 */
	private static final String VERSION = "1.0";

	/**
	 * 
	 */
	private static final int THREAD_LOOP_DURATION = 500;
	
	/**
	 * 
	 */
	private static final String SOURCE_ADDRESS = "127.3.70.1";
	
	/**
	 * 
	 */
	private static final String DESTINATION_ADDRESS = "127.3.70.1";
	private static final int DESTINATION_PORT = 15000;
	
	/**
	 * 
	 */
	final static Map<String, AsyncResponse> waiters = new ConcurrentHashMap<String, AsyncResponse>();
	final static ExecutorService executor = Executors.newCachedThreadPool();

	@GET
	@Path("/tojson/{poolid}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getJSONResult( 
			@PathParam("poolid") String poolid) {

		//
		String result = "{}";
		
		// get an available database connection
		Connection database = MySQLLogin.getConnection();

		// create the prepared statement and add the criteria
		String sqlStm = "SELECT json_value FROM requests WHERE uuid = ?";
	    PreparedStatement preStm = null; ResultSet resultSet = null;
		try {
			preStm = database.prepareStatement(sqlStm);
		    preStm.setString(1, poolid);
		    resultSet = preStm.executeQuery();
		    if (resultSet.next()) {
		    	// read the JSON value field
		    	result = resultSet.getString("json_value");
		    	
		    } else {
		    	// set error output message
		    	result = "{'message':'There is not any JSON Metadata analysis for the given Job ID: " + poolid + "'}";
		    }
		    resultSet.close();
		    		    
		} catch (SQLException e) {
	    	// set error output message
	    	result = "{'exception':'An exception has occurred for the given Job ID: " + poolid + "'}";

		} 

		// return the result
		return Response.ok(
				result, 
				MediaType.APPLICATION_JSON).
				build();

	}

	@GET
	@Path("/version")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMetaAnalyserVersion() {
		UploadFileResult result = null;
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		// build the response object
		result = new UploadFileResult(
				null, 
				"MetadataAnaylser Restful Web Service Endpoint, V" + 
				VERSION + ", Long Pooling Objects: " + waiters.size());
		
		// return the result
		return Response.ok(
				gson.toJson(result), 
				MediaType.APPLICATION_JSON).
				build();
	}

	@GET
	@Path("/polling/{poolid}")
	@Produces(MediaType.APPLICATION_JSON)
	public void getLongPolling(
			@Suspended AsyncResponse asyncResp, 
			@PathParam("poolid") String poolid) {

		// adds a new long pooling response object to pool
		waiters.put(poolid, asyncResp);
		
	}

	@POST
	@Path("/submitconcept")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_PLAIN)
	public Response submitMetaDataConcept(
			@FormDataParam("concept") String conceptValue,
			@FormDataParam("repository") String repository) {

		UploadFileResult result = null;
		Gson gson = new Gson();
		
		// convert uploaded to byte array
		byte[] metadata = null;
		try {
			metadata = conceptValue.getBytes();
			
		} catch (Exception e) {
			// send an error message to the client
			result = new UploadFileResult(
					null, 
					"[" + StringWork.getNowDate() + "] " +
					"An error as occurred parsing the metadata concept", 
					2);
		}
		
		// is this a brand new request
		if (metadata != null) {
			
			// generate a new unique pool id
			String poolID = UUID.randomUUID().toString();
			
			// start a new thread execution
			executor.submit(new MetaDataRunnableThread(
					poolID,
					waiters, 
					repository,
					metadata,
					RequestType.CONCEPTANALYSIS,
					SOURCE_ADDRESS, 
					StringWork.randomBetween((DESTINATION_PORT + 1), (DESTINATION_PORT + 1000)),					
					DESTINATION_ADDRESS, 
					DESTINATION_PORT));
			
			// build acknowledgement message to the client
			result = new UploadFileResult(
					poolID,
					"[" + StringWork.getNowDate() + "] " +
					"Your request was sucussfully received. The uploaded metadata " + 
					"concept getting analysed, please wait.");
			
		} else {
			//
			result = new UploadFileResult(
					null, 
					"[" + StringWork.getNowDate() + "] " +
					"Unable to correctly upload metadata file", 
					2);
		
		}
		
		// return the result
		String toJson = gson.toJson(result);
		return Response.ok(
				toJson,
				MediaType.TEXT_PLAIN).
				build();

	}
	
	@POST
	@Path("/submitfile")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_PLAIN)
	public Response submitMetaDataFile(
			@FormDataParam("file") InputStream fileInputStream,
            @FormDataParam("file") FormDataContentDisposition fileMetaData,
            @FormDataParam("location") String fileRemoteLocation,
            @FormDataParam("repository") String repository) {
		//
		UploadFileResult result = null;
		Gson gson = new Gson();
		//
		byte[] metadata = null;
		try {
			// remote location takes precedence over uploaded file
			if (fileRemoteLocation != null && fileRemoteLocation.length() > 0) {
				// convert given remote location to byte array
				metadata = FileWork.readRemoteIntoByteArray(fileRemoteLocation);
			} else {
				// convert uploaded to byte array
				metadata = IOUtils.toByteArray(fileInputStream);
			}
			
		} catch (MalformedURLException e) {
			// send an error message to the client
			result = new UploadFileResult(
					null, 
					"[" + StringWork.getNowDate() + "] " +
					"An error as occurred reading remote file location", 
					2);
			
		} catch (IOException e) {
			// send an error message to the client
			result = new UploadFileResult(
					null, 
					"[" + StringWork.getNowDate() + "] " +
					"An error as occurred parsing the metadata file: " + 
							e.getMessage(), 
					2);
		
		} catch (Exception e) {
			// send an error message to the client
			result = new UploadFileResult(
					null, 
					"[" + StringWork.getNowDate() + "] " +
					"An error as occurred parsing the metadata file" + 
							e.getMessage(), 
					2);
		}

		
		// is this a brand new request
		if (metadata != null && metadata.length > 0) {
			
			// generate a new unique pool id
			String poolID = UUID.randomUUID().toString();
			
			// start a new thread execution			
			executor.submit(
				new MetaDataRunnableThread(
					poolID,
					waiters, 
					repository,
					metadata, 
					RequestType.FILEANALYSIS,
					SOURCE_ADDRESS, 
					StringWork.randomBetween((DESTINATION_PORT + 1), (DESTINATION_PORT + 1000)),
					DESTINATION_ADDRESS, 
					DESTINATION_PORT));
			
			// build acknowledgement message to the client
			result = new UploadFileResult(
					poolID,
					"[" + StringWork.getNowDate() + "] " +
					"Your request was sucussfully received. The uploaded metadata " + 
					"file getting analysed, please wait.");
			
		}
		
		// return the result
		String toJson = gson.toJson(result);
		return Response.ok(
				toJson,
				MediaType.TEXT_PLAIN).
				build();

	}

	// PRIVATE CLASSES

	/**
	 * 
	 * 
	 *
	 */
	private class MetaDataRunnableThread implements Runnable, Observer {

		/**
		 * 
		 */
		private String poolID;
		
		/**
		 * 
		 */
		private Map<String, AsyncResponse> waiters;
		
		/**
		 * the source repository id 
		 */
		private String repository;
		
		/**
		 * 
		 */
		private byte[] body;
		
		/**
		 * the type of the request being made: concept or file analysis
		 */
		private RequestType requestType;
		
		/**
		 * 
		 */
		private Interface network;

		/**
		 * 
		 */
		private String srcAddress;

		/**
		 * 
		 */
		private int srcPort;
		
		/**
		 * 
		 */
		private String dstAddress;
		
		/**
		 * 
		 */
		private int dstPort;
		
		/**
		 * 
		 */
		private boolean verbose;
		
		/**
		 * 
		 */
		private boolean runnable;
		
		/**
		 * 
		 */
		private Queue<Message> messageQueue;
		
		
		/**
		 * 
		 * @param asyncResp
		 * @param uploadedFile
		 */
		public MetaDataRunnableThread(
				String poolID,
				Map<String, AsyncResponse> waiters,
				String repository,
				byte[] body,
				RequestType requestType,				
				String srcAddress,
				int srcPort,
				String dstAddress,
				int dstPort) {
			super();
			
			// establish thread properties
			this.poolID = poolID;
			this.waiters = waiters;
			this.repository = repository;
			this.body = body;
			this.requestType = requestType;
			this.srcAddress = srcAddress;
			this.srcPort = srcPort;
			this.dstAddress = dstAddress;
			this.dstPort = dstPort;
			this.verbose = true;
			
			// thread control flag
			this.runnable = true;
			
			// concurrent messaging queue
			this.messageQueue = new ConcurrentLinkedQueue<Message>();
			
		}
		
		@Override
		public void run() {
			
			//
			Gson gson = new Gson();
			UploadFileResult result = null;
			
			// try to start a new TCP connection
			AsyncResponse asyncResponse = null;
			boolean haveConnection = false;
			try {
				network = new Interface(
						this, 
						srcAddress,
						srcPort, 
						verbose);
				haveConnection = true;
				
			} catch (UnknownHostException e) {
				// this is only a process information message
				result = new UploadFileResult(
						null,
						new String("We're sorry, but an exception has occurred connecting to " + 
								"Metadata Analyser Node. Unable to find host."),
						2);

			} catch (Exception e) {
				// this is only a process information message
				result = new UploadFileResult(
						null,
						new String("We're sorry, but an exception has occurred connecting to " + 
								"Metadata Analyser Node."),
						2);				
			}

			// if there's a analyser node connection, send the request message
			if (haveConnection) {

				boolean messageSent = false;
				try {
					// send a network request message to analyser node
					sendAnalysisRequest();
					messageSent = true;
					
				} catch (UnknownHostException e) {
					// this is only a process information message
					result = new UploadFileResult(
							null,
							new String("We're sorry, but an exception has occurred sending the request to " + 
									"Metadata Analyser Node."),
							2);									
				}
				
				// if the message was sent start the thread main loop
				if (messageSent) {

					// start an infinite loop until all process is done
					while (!Thread.currentThread().isInterrupted() 
							&& runnable) {
		
						// if there's a message to be sent, get the pool id response object
						if (waiters.containsKey(poolID) && 
								messageQueue.size() > 0) {
							
							// read the next message in the queue
							String body = null;
							Message message = messageQueue.poll();
							switch (message.getType()) {
								
								case TCPRESPONSE:
									// this is the final process response
									body = new String(message.getBody());
									pt.ma.metadata.MetaData metaData = gson.fromJson(
											body, 
											pt.ma.metadata.MetaData.class);
									
									// build the message to be sent to the client
									result = new UploadFileResult(
											poolID, 
											gson.toJson(metaData),
											1);
									
									// persist the JSON result to the database
									try {
										// persist result to the database
										persistMetadataAnalysis(poolID, body);
										
									} catch (SQLException e) {
										// alert message to the client
										result = new UploadFileResult(
												poolID, 
												"{'message':'" + e.getMessage() + "'}",
												-1);
										
									} catch (Exception e) {
										// alert message to the client
										result = new UploadFileResult(
												poolID, 
												"{'message':'" + e.getMessage() + "'}",
												-1);
										
									} finally {
										// send the result to the client 
										String jsonValue = gson.toJson(result);
										asyncResponse = waiters.get(poolID);
										asyncResponse.resume(jsonValue);
										waiters.remove(poolID);
									}

									// mark the end of the analysis job
									runnable = false;
									break;
		
								case TCPDIGEST:
									// this is only a process information message
									body = new String(message.getBody());
									result = new UploadFileResult(
											poolID, 
											body);		

									// send the result to the client
									asyncResponse = waiters.get(poolID);
									asyncResponse.resume(gson.toJson(result));
									waiters.remove(poolID);
									break;
									
								default:
									// TODO: something's wrong
									break;
							}
														
						}
						
						// wait for 5 seconds
						try {
							Thread.sleep(THREAD_LOOP_DURATION);
							
						} catch (InterruptedException e) {
							// this is only a process information message
							result = new UploadFileResult(
									null,
									new String("We're sorry, but an exception has occurred in your request."),
									2);
							// send the result to the client
							asyncResponse = waiters.get(poolID);
							asyncResponse.resume(gson.toJson(result));
							waiters.remove(poolID);
							//
							runnable = false;
						}
					}
					
				} else {
					// if the request wasn't sent, stop the process
					asyncResponse = waiters.get(poolID);
					asyncResponse.resume(gson.toJson(result));
					waiters.remove(poolID);
					
				}
				
			} else {
				// there's no connection to analyser node, stop the process
				asyncResponse = waiters.get(poolID);
				asyncResponse.resume(gson.toJson(result));
				waiters.remove(poolID);

			}

			// wrap it up
			network.stopSocket();
			network = null;
			
		}

		@Override
		public void update(Observable arg0, Object arg1) {
			//
			if (arg0 instanceof Interface) {
				try {
					// read the new network message
					Message message = (Message) arg1;
					// add it to TCP message queue
					messageQueue.add(message);

				} catch (Exception e) {
					// TODO: something's wrong
				}
			}
		}
		
		/**
		 * 
		 * @return
		 * @throws UnknownHostException 
		 */
		private void sendAnalysisRequest() 
				throws UnknownHostException {
			
			// choose the message type to be sent
			MessageType messageType = null;
			switch (requestType) {
			
				case FILEANALYSIS:
					messageType = MessageType.TCPREQUESTMETADATA; 
					break;
	
				case CONCEPTANALYSIS:
					messageType = MessageType.TCPREQUESTCONCEPT;
					break;
					
				default:
					break;
			}
			
			// build a new request to the metadata processing system
			Message message = new Message(
					dstAddress, 
					dstPort, 
					messageType, 
					body, 
					repository);				
			
			// try to send this message 
			network.sendMessage(message);

		}
		
		/**
		 * 
		 * @param jobID
		 * @param result
		 * @throws SQLException 
		 */
		private void persistMetadataAnalysis(
				String jobID, 
				String result) throws SQLException {
			
			// get an available database connection
			Connection database = MySQLLogin.getConnection();
			
			// get the now date value
			Calendar calendar = Calendar.getInstance();
		    Date requestDate = new Date(calendar.getTime().getTime());
			
			// create this MYSQL insert prepared statement
		    String sqlStm = "INSERT INTO requests (uuid, json_value, request_date) VALUES (?, ?, ?)";
			PreparedStatement preStm = database.prepareStatement(sqlStm);
			preStm.setString(1, jobID);
			preStm.setString(2, result);
			preStm.setDate(3, requestDate);
			preStm.execute();
			preStm.close();
		}
	}

}
