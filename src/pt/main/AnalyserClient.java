package pt.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Observable;
import java.util.Observer;

import com.google.gson.Gson;

import pt.ma.component.proxy.network.Interface;
import pt.ma.component.proxy.network.Message;
import pt.ma.component.proxy.network.MessageType;
import pt.ma.metadata.MetaData;
import pt.ma.util.FileWork;

/**
 * 
 * @author
 *
 */
public class AnalyserClient implements Observer {

	/**
	 * 
	 */
	private static boolean VERBOSE = true;

	/**
	 * 
	 */
	private final int TCP_PORT = 8001;

	/**
	 * 
	 */
	private Interface network;

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		//
		AnalyserClient analyser = new AnalyserClient(VERBOSE);

	}

	/**
	 * 
	 * @param versbose
	 */
	public AnalyserClient(boolean versbose) {

		//
		network = new Interface(this, TCP_PORT, versbose);

/*		for (int i = 1; i <= 1; i++) {
			//
			try {
				byte[] body = FileWork.readContentIntoByteArray(
						new File("C:\\Users\\Bruno\\PEIWorkplace\\MetadataAnalyser-master\\metadata-files\\"
								+ "i_Investigation_" + i + ".txt"));

				// build the tcp message to be sent
				Message message = new Message(
						"127.0.0.1", 
						8000, 
						MessageType.TCPREQUESTMETADATA, 
						body);
				network.sendMessage(message);

			} catch (Exception e) {

			}

		}
*/	
		String concept = "http://purl.obolibrary.org/obo/CHMO_0000796";
		Message message = new Message(
				"127.0.0.1", 
				8000, 
				MessageType.TCPREQUESTCONCEPT, 
				concept.getBytes());
		network.sendMessage(message);		

	}

	@Override
	public void update(Observable arg0, Object arg1) {
		//
		if (arg0 instanceof Interface) {
			try {
				Gson gson = new Gson();

				// read the new network message
				byte[] body = null;
				Message message = (Message) arg1;
				switch (message.getType()) {
					case TCPRESPONSE:
						body = message.getBody();
						MetaData metaData = gson.fromJson(new String(body), MetaData.class);	
						File file = new File(
								"C:\\Users\\Bruno\\PEIWorkplace\\MetadataAnalyser-master\\metadata-files\\parse-results\\"
										+ metaData.getUniqueID().toString() + ".txt");
						FileOutputStream stream = new FileOutputStream(file);
						try {
							stream.write(body);
						} finally {
							stream.close();
						}
						break;
	
					case TCPDIGEST:
						body = message.getBody();
						String filename= "C:\\Users\\Bruno\\PEIWorkplace\\MetadataAnalyser-master\\metadata-files\\parse-results\\digest.txt";
					    FileWriter fw = new FileWriter(filename,true); //the true will append the new data
					    fw.write(new String(body) + "\n");//appends the string to the file
					    fw.close();
						break;
						
					default:
						break;
				}
				// just print out the return message

			} catch (Exception e) {

			}
		}

	}

}
