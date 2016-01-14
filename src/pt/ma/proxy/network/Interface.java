package pt.ma.proxy.network;

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
	 * 
	 */
	private int tcpPort;
	
	/**
	 * 
	 */
	private boolean verbose;
	
	/**
	 * 
	 */
	public Interface(
			Observer observer, 
			int tcpPort, 
			boolean verbose) {
		super();
		
		// Establish class properties 
		this.tcpPort = tcpPort;
		this.verbose = verbose;
		
		// Add a new class observer
		this.addObserver(observer);

		// log action
		if (verbose) {
			System.out.println("[Network :Info] Starting Network Layer.");
		}
		
		// start listening to incoming messages
		new Thread(new InputMessagingThread(this.tcpPort)).start();
		
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
			new Thread(
					new OutputMessagingThread( 
							message,
							tcpPort)
					).start();
			
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
		private InetAddress destination;
		
		/**
		 * 
		 */
		private int tcpPort;
		 
		
		/**
		 * 
		 */
		private Message message;
		
		/**
		 * @throws UnknownHostException 
		 * 
		 */
		public OutputMessagingThread ( 
				Message message,
				int tcpPort) throws UnknownHostException {
			super();
			
			// Establish class properties
			this.destination = InetAddress.getByName(message.getReceiverAddress());
			this.message = message;
			this.tcpPort = tcpPort;
			
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
							"server connection: "+destination+":" + this.tcpPort);
				}
				
				// open the client socket connection
				socket = new Socket(destination, this.tcpPort);
				outputStream = new ObjectOutputStream(socket.getOutputStream());
				
				// log action
				if (verbose) {
					System.out.println("[Network,ClientThread :Info] SEND procedure, server connection "+
							"sucessfully opened");
				}
				
				// set sender message identifier
				message.setSenderAddress(socket.getInetAddress().getHostAddress());
				
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
		private int tcpPort;
		
		/**
		 * 
		 * @param tcpPort
		 */
		public InputMessagingThread ( 
				int tcpPort) {
			super();
			
			// Establish class properties
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
				server = new ServerSocket(this.tcpPort);
				
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

