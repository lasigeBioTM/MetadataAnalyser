package pt.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Semaphore;

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
	private final int TCP_PORT = 15011;

	/**
	 * 
	 */
	private Interface network;

	private Semaphore semaphore;
	
	/**
	 * 
	 * @param args
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) throws UnknownHostException {
		//
		AnalyserClient analyser = new AnalyserClient(VERBOSE);

	}

	/**
	 * 
	 * @param versbose
	 * @throws UnknownHostException 
	 */
	public AnalyserClient(boolean verbose) throws UnknownHostException {

		//
		network = new Interface(this,"127.0.0.1",TCP_PORT,verbose);
/*
		for (int i = 1; i <= 1; i++) {
			//
			try {
				byte[] body = FileWork.readContentIntoByteArray(
						new File("C:\\Users\\Bruno\\PEIWorkplace\\MetadataAnalyser-master\\metadata-files\\"
								+ "i_Investigation_" + i + ".txt"));

				// build the tcp message to be sent
				Message message = new Message(
						"127.0.0.1", 
						15000, 
						MessageType.TCPREQUESTMETADATA, 
						body);
				network.sendMessage(message);

			} catch (Exception e) {

			}

		}*/
/*
		String concept = "http://purl.obolibrary.org/obo/CHMO_0000796";
		Message message = new Message(
				"127.0.0.1", 
				15000, 
				MessageType.TCPREQUESTCONCEPT, 
				concept.getBytes());
		network.sendMessage(message);		
*/
		String[] location = {
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS1/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS10/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS102/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS103/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS104/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS105/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS107/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS108/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS109/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS11/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS110/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS111/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS112/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS113/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS114/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS116/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS117/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS118/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS119/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS12/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS120/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS123/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS124/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS125/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS126/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS127/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS128/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS13/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS131/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS132/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS133/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS134/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS137/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS14/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS140/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS143/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS144/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS146/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS147/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS148/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS15/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS150/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS152/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS154/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS155/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS156/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS157/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS16/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS161/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS162/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS163/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS165/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS166/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS168/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS169/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS17/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS171/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS172/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS173/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS174/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS175/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS176/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS177/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS178/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS187/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS188/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS19/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS191/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS194/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS197/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS2/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS20/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS202/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS203/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS208/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS21/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS212/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS213/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS214/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS215/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS218/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS219/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS22/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS226/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS228/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS229/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS23/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS234/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS235/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS24/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS243/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS25/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS26/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS270/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS276/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS28/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS281/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS29/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS293/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS295/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS3/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS30/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS31/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS32/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS33/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS34/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS35/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS36/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS37/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS38/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS39/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS4/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS41/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS42/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS43/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS44/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS45/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS46/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS47/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS49/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS5/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS52/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS54/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS55/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS56/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS57/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS59/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS6/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS61/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS67/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS69/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS7/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS71/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS72/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS74/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS75/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS77/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS79/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS8/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS81/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS85/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS86/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS87/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS88/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS90/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS91/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS92/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS93/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS95/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS96/i_Investigation.txt",
				"ftp://ftp.ebi.ac.uk/pub/databases/metabolights/studies/public/MTBLS99/i_Investigation.txt"	
		};
		
		semaphore = new Semaphore(1);
		System.out.println("Ontologies Parser (" + location.length + "): ");
		for (int counter = 0; counter < location.length; counter++) {
			String url = location[counter];
			try {
				semaphore.acquire();
				byte[] body = FileWork.readRemoteIntoByteArray(url);
				System.out.println(counter + ": " + url + ": sucess");
				Message message = new Message(
						"127.0.0.1", 
						15010, 
						MessageType.TCPREQUESTMETADATA, 
						body);
				network.sendMessage(message);			
				
			} catch (MalformedURLException e) {
				System.out.println(counter + ": " + url + ": fail");
				System.out.println("MalFormedException: " + e.getMessage());
			} catch (IOException e) {
				System.out.println(counter + ": " + url + ": fail");
				System.out.println("IOException: " + e.getMessage());
			} catch (Exception e) {
				System.out.println(counter + ": " + url + ": fail");
				System.out.println("Exception: " + e.getMessage());
			}					
		}
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
								"C:\\Users\\Bruno\\git\\master\\metadata-files\\parse-results\\"
										+ metaData.getId().toString() + ".txt");
						FileOutputStream stream = new FileOutputStream(file);
						try {
							stream.write(body);
						} finally {
							stream.close();
						}
						semaphore.release();
						break;
	
					case TCPDIGEST:
						body = message.getBody();
						String filename= "C:\\Users\\Bruno\\git\\master\\metadata-files\\parse-results\\digest.txt";
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
