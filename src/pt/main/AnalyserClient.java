package pt.main;

import java.io.File;
import java.io.FileOutputStream;
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

		for (int i = 1; i <= 2; i++) {
			//
			try {
				byte[] body = FileWork.readContentIntoByteArray(
						new File("C:\\Users\\Bruno\\PEIWorkplace\\MetadataAnalyser-master\\metadata-files\\"
								+ "i_Investigation_" + i + ".txt"));

				// build the tcp message to be sent
				Message message = new Message("127.0.0.1", 8000, MessageType.TCPREQUEST, body);
				network.sendMessage(message);

			} catch (Exception e) {

			}

		}
	}

	@Override
	public void update(Observable arg0, Object arg1) {
		//
		if (arg0 instanceof Interface) {
			try {
				// read the new network message
				Message message = (Message) arg1;

				// just print out the return message
				Gson gson = new Gson();
				byte[] body = message.getBody();
				MetaData metaData = gson.fromJson(new String(body), MetaData.class); 
				
				File file = new File("C:\\Users\\Bruno\\PEIWorkplace\\MetadataAnalyser-master\\metadata-files\\parse-results\\" + metaData.getUniqueID().toString() + ".txt");
				FileOutputStream stream = new FileOutputStream(file);
				try {
				    stream.write(body);
				} finally {
				    stream.close();
				}
				
			} catch (Exception e) {

			}
		}

	}

}
