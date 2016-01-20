package pt.main;

import java.io.File;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.jetty.webapp.MetaData;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import pt.ma.component.proxy.network.Interface;
import pt.ma.component.proxy.network.Message;
import pt.ma.component.proxy.network.MessageType;
import pt.ma.util.FileWork;

/**
 * 
 * @author
 *
 */
public class AnalyserDebug implements Observer {

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
		AnalyserDebug analyser = new AnalyserDebug(VERBOSE);

	}

	/**
	 * 
	 * @param versbose
	 */
	public AnalyserDebug(boolean versbose) {

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
				byte[] body = message.getBody();
				String jsonBody = new String(body);
				System.out.println(jsonBody);

			} catch (Exception e) {

			}
		}

	}

}
