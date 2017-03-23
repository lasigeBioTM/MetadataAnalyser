package pt.ma.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTPClient;

/**
 * 
 * 
 *
 */
public class FileWork {

	/**
	 * 
	 * @param location
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public static byte[] readRemoteIntoByteArray(String location) 
			throws MalformedURLException, IOException {
		
		// main function objects
		InputStream inputStream = null; byte[] bFile = null;
		
		// parse protocol, domain and path values
		String[] parse = null; 
		String protocol = ""; String domain = ""; String path = "";
		parse = location.split("\\:\\/\\/");
		if (parse.length > 0) {
			
			// it's a ftp request
			protocol = parse[0].trim().toLowerCase();
			if (protocol.equals("ftp")) {
				//
				parse = parse[1].split("\\/");
				domain = parse[0].trim().toLowerCase();
				for (int index = 1; index < parse.length; index++) {
					path += "/" + parse[index]; 
				}
				
				// open a FTP connection
				FTPClient clientFTP = new FTPClient();				
				clientFTP.connect(domain);
				clientFTP.login("anonymous", "anonymous");
				clientFTP.enterLocalPassiveMode();
				inputStream = clientFTP.retrieveFileStream(path);
				
				// read the requested file
				bFile = IOUtils.toByteArray(inputStream);
				inputStream.close();
				clientFTP.logout(); clientFTP.disconnect();
				
			} else {
				// open a HTTP connection
				URL clientHTTP = new URL(location);
				URLConnection connection = clientHTTP.openConnection();
				inputStream = connection.getInputStream();

				// read the requested file
				bFile = IOUtils.toByteArray(inputStream);
				inputStream.close();

			}
		}
		
		//
		return bFile;
	}
		
	/**
	 * 
	 * @param file
	 * @return
	 */
	public static byte[] readContentIntoByteArray(File file) {
		FileInputStream fileInputStream = null;
		byte[] bFile = new byte[(int) file.length()];
		try {
			// convert file into array of bytes
			fileInputStream = new FileInputStream(file);
			fileInputStream.read(bFile);
			fileInputStream.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bFile;
	}

}
