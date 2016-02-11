package pt.ma.util;

import java.io.File;
import java.io.FileInputStream;

/**
 * 
 * @author 
 *
 */
public class FileWork {

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
			/*for (int i = 0; i < bFile.length; i++) {
				System.out.print((char) bFile[i]);
			}*/
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bFile;
	}

	/**
	 * 
	 * @param file
	 * @return
	 */
	public static String readContentIntoString(File file) {
		FileInputStream fileInputStream = null;
		StringBuffer sFile = new StringBuffer();
		try {
			// convert file into string
			fileInputStream = new FileInputStream(file);
			int ch;
			while( (ch = fileInputStream.read()) != -1) {
				sFile.append((char)ch);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sFile.toString();
	}
}
