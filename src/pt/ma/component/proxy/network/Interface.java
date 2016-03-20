package pt.ma.component.proxy.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;


/**
 * 
 * 
 *
 */
public class Interface extends Observable {

	/**
	 * Server socket address (ex. 127.0.0.1) 
	 */
	private InetAddress sourceAddress;
	
	/**
	 * Server socket TCP port (ex. 8080)
	 */
	private int sourceTCPPort;
	
	/**
	 * 
	 */
	private boolean verbose;

	/**
	 * 
	 * @param observer
	 * @param sourceAddress
	 * @param sourceTCPPort
	 * @param verbose
	 * @throws UnknownHostException 
	 */
	public Interface(
			Observer observer,
			String sourceAddress,
			int sourceTCPPort, 
			boolean verbose) throws UnknownHostException {
		super();
		
		// Establish class properties 
		this.sourceAddress = InetAddress.getByName(sourceAddress);
		this.sourceTCPPort = sourceTCPPort;
		this.verbose = verbose;
		
		// Add a new class observer
		this.addObserver(observer);

		// log action
		if (verbose) {
			System.out.println("[Network :Info] Starting Network Layer.");
		}
		
		// start listening to incoming messages
		new Thread(
				new InputMessagingThread(
						this.sourceAddress ,
						this.sourceTCPPort)).start();
		
	}
	
	/**
	 * Sends a new message <Message> to the destination address <InetAddress>
	 * 
	 * @param destination
	 * @param message
	 */
	public void sendMessage (Message message) {
		
		// log action
		if (verbose) {
			System.out.println("[Network :Info] SEND procedure for message: "
					+message.toString());
		}
		
		// send the message
		try {
			// set source tcp port
			new Thread(
					new OutputMessagingThread(message)).start();
			
		} catch (UnknownHostException e) {
			// TODO: logging action

		}
	}
	
	/**
	 * 
	 * @param message
	 */
	protected synchronized void inputMessageRecevied(Message message) {
		// log action
		if (verbose) {
			System.out.println("[Network :Info] Observers notification for received message");
		}
		
		// notify observers
		this.setChanged();
		this.notifyObservers(message);
		
	}
	
	// PRIVATE CLASSES
	
	/**
	 * 
	 * 
	 *
	 */
	private class OutputMessagingThread implements Runnable {

		/**
		 * 
		 */
		private Socket socket;
		
		/**
		 * 
		 */
		private ObjectOutputStream outputStream = null;
		
		/**
		 * 
		 */
		private InetAddress destinationIP;
		
		/**
		 * 
		 */
		private int destinationPort;
		 
		
		/**
		 * 
		 */
		private Message message;

		/**
		 * 
		 * @param message
		 * @throws UnknownHostException
		 */
		public OutputMessagingThread (Message message) 
				throws UnknownHostException {
			super();
			
			// Establish class properties
			this.destinationIP = InetAddress.getByName(message.getReceiverTCPIP());
			this.destinationPort = message.getReceiverTCPPort();
			this.message = message;
			
			//
			if (verbose) {
				System.out.println("[Network,ClientThread :Info] SEND procedure, client "+
					"thread instanciated");
			}
			
		}
		
		@Override
		public void run() {
			try {
				// log action
				if (verbose) {
					System.out.println("[Network,ClientThread :Info] SEND procedure, about to open "+
							"server connection: " + destinationIP + ":" + destinationPort);
				}
				
				// open the client socket connection
				socket = new Socket(
						destinationIP, 
						destinationPort);
				outputStream = new ObjectOutputStream(socket.getOutputStream());
				
				// log action
				if (verbose) {
					System.out.println("[Network,ClientThread :Info] SEND procedure, server connection "+
							"sucessfully opened");
				}
				
				// set sender message identifier
				message.setSenderTCPIP(socket.getInetAddress().getHostAddress());
				message.setSenderTCPPort(sourceTCPPort);
				
				// send the message to destination
				outputStream.writeObject(message);
				outputStream.flush();
				
				// log action
				if (verbose) {
					System.out.println("[Network,ClientThread :Info] SEND procedure, message sent");
				}
				
			} catch (IOException e) {
				// log action
				System.out.println("[Network,ClientThread :Error] Error in sending the message: "
						+e.getMessage());
								
			} finally {
				if (socket != null) {
					try {
						outputStream.close();
						socket.close();
						
						// log action
						if (verbose) {
							System.out.println("[Network,ClientThread :Info] SEND procedure, "+
								"server connection closed");
						}

					} catch (IOException e) {
						// log action
						System.out.println("[Network,ClientThread :Error] Error in closing the connection: "
								+e.getMessage());
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
	private class InputMessagingThread implements Runnable {
		
		/**
		 * 
		 */
		private ServerSocket server;

		/**
		 * 
		 */
		private InetAddress address;
		
		/**
		 * 
		 */
		private int tcpPort;
		
		/**
		 * 
		 * @param tcpPort
		 */
		public InputMessagingThread (
				InetAddress address,
				int tcpPort) {
			super();
			
			// Establish class properties
			this.address = address;
			this.tcpPort = tcpPort;
			
			//
			if (verbose) {
				System.out.println("[Network,ClientThread :Info] SEND procedure, client "+
					"thread instanciated");
			}
			
		}

		/**
		 * 
		 */
		@Override
		public void run() {
			try {
				// log action
				if (verbose) {
					System.out.println("[Network,ServerThread :Info] TCP, about to open server "+
							"socket at: " + this.tcpPort);
				}
				
				// try to open a server socket
				server = new ServerSocket(tcpPort, 0, address);
				
				// Infinite loop
				while (!Thread.currentThread().isInterrupted()) {
					try {
						
						// log action
						if (verbose) {
							System.out.println("[Network,ServerThread :Info] TCP, open server "+
									"socket on: " + this.tcpPort);
						}
						
						// keep listening to incoming connections
						Socket connection = server.accept();

						// log action
						if (verbose) {
							System.out.println("[Network,ServerThread :Info] TCP connection received.");
						}
						
						// log action
						if (verbose) {
							System.out.println("[Network,ServerThread :Info] About to start a working "+
									"thread to deal with the request.");
						}
						
						// deal with the new connection
						new Thread(
								new ConnectionThread(connection)
								).start();
						
					} catch (IOException e) {
						// log action
						System.out.println("[Network,ServerThread :Error] Error on receiving a message"+
								": "+e.getMessage());

					}
					
				}

			} catch (IOException e) {
				System.out.println("[Network,ServerThread :Error] Error on openning the server socket"+
						": "+e.getMessage());
				
			} finally {
				if (server != null) {
					try {
						server.close();
						// log action
						if (verbose) {
							System.out.println("[Network,ServerThread :Info] Server socket closed");
						}
						
					} catch (IOException e) {
						// log action
						System.out.println("[Network,ServerThread :Error] Error on closing the "+
								"server socket: "+e.getMessage());
						
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
	private class ConnectionThread implements Runnable {

		/**
		 * 
		 */
		private Socket socket;
		
		/**
		 * 
		 */
		private ObjectInputStream inputStream = null;

		/**
		 * 
		 * @param socket
		 */
		public ConnectionThread(Socket socket) {
			this.socket = socket;
			
			// log action
			if (verbose) {
				System.out.println("[Network,ConnectionThread :Info] Working thread started "+
						"to deal with the request");
			}
			
		}
		
		@Override
		public void run() {
			//
			try {
				Message message;
				inputStream = new ObjectInputStream(socket.getInputStream());
				
				// log action
				if (verbose) {
					System.out.println("[Network,ConnectionThread :Info] About to read the message received");
				}
				
				// waits for a single message from client
				message = (Message)inputStream.readObject();				
				
				// log action
				if (verbose) {
					System.out.println("[Network,ConnectionThread :Info] Message read from input stream: "
							+message.toString());
				}
				
				// start receiving procedure
				inputMessageRecevied(message);
				
			} catch (IOException e) {
				// log action
				System.out.println("[Network,ConnectionThread :Error] Error in reading the message: "
						+e.getMessage());
				
			} catch (ClassNotFoundException e) {
				// log action
				System.out.println("[Network,ConnectionThread :Error] Error in reading the message: "
						+e.getMessage());
				
			} finally {
				if (socket != null) {
					try {
						inputStream.close();
						socket.close();
						
						// log action
						if (verbose) {
							System.out.println("[Network,ConnectionThread :Info] Working socket "+
									"sucessfully closed.");
						}
						
					} catch (IOException e) {
						// log action
						System.out.println("[Network,ConnectionThread :Error] Error in closing "+
								"the working thread: "+e.getMessage());

					}
				}
			}			
		}
		
	}
	
}

