package pt.ma.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StringWork {

	/**
	 * 
	 * @param value
	 * @return
	 */
	public static String sanitaze(String value) {
		String result = null;

		result = value.trim();
		result = result.replaceAll("\"", "");
		result = result.replaceAll("\t", "");
		result = result.replaceAll("\r", "");
		result = result.replaceAll("\n", "");

		return result;
	}

	/**
	 * 
	 * @param buffer
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	public static String makeSHA1Hash(byte[] buffer) 
			throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest md = MessageDigest.getInstance("SHA1");
		md.reset(); md.update(buffer);
		byte[] digest = md.digest();
		String hexStr = "";
		for (int i = 0; i < digest.length; i++) {
			hexStr += Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1);
		}
		return hexStr;
	}
}
