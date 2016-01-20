package pt.ma.component.log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utilities {

	/**
	 * Verifica se uma string é numerica
	 * @param str
	 * @return
	 */
	public static boolean isNumeric(String str) {
		for (char n : str.toCharArray()) {
			if (!Character.isDigit(n))
				return false;
		}
		return true;
	}
	
	public static String DameDataYYYY_MM_DD () {
		DateFormat dt = new SimpleDateFormat("yyyy-mm-dd");
		Date date = new Date();
		return dt.format(date);
	}


	public static String dameDataHora() {
		DateFormat dt = new SimpleDateFormat("yyyy-mm-dd HH_mm_ss");
		Date date = new Date();
		return dt.format(date);
	}
	
	
	//verificar se o endereço ip esta no formato correcto.
	// 
	public static boolean checkIpAdress(String ip) {
		Pattern pattern;
	    Matcher matcher;
	 
	    final String IPADDRESS_PATTERN = 
			"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
			"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
			"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
			"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
	 
		  pattern = Pattern.compile(IPADDRESS_PATTERN);
	    
		  matcher = pattern.matcher(ip);
		  return matcher.matches();	  
		  
	}
	
	/**
	 * 
	 * @param ip
	 * @return
	 * @throws UnknownHostException 
	 * @throws SocketException 
	 */
	public static InetAddress getInetAddressFromIp(
			String ip
				) throws UnknownHostException, 
						 SocketException {
		
		// by default return the local host address
		InetAddress address = InetAddress.getLocalHost();
		
		//
		byte[] byteOctets = addressStringToByte(ip);
				
		// iterate trough all local network interfaces		
		Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netint : Collections.list(nets)) {
        	Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
        	for (InetAddress inetAddress : Collections.list(inetAddresses)) {
        		
        		// ip address found, so get the InetAddress instance
        		byte[] byteAddress = inetAddress.getAddress();
        		if (Arrays.equals(byteOctets, byteAddress)) {
        			address = inetAddress;
        		}
            }
        }
		
		//
		return address;
		
	}
	
	/**
	 * 
	 * @param ip
	 * @return
	 */
	public static byte[] addressStringToByte(String ip) {
		//
		String[] parsedOctets = ip.split("[.]");
		byte[] byteOctets = new byte[4];
		for (int count = 0; count < byteOctets.length; count++) {
			int valueInt = Integer.valueOf(parsedOctets[count]);
			byteOctets[count] = (byte) valueInt;
		}
		
		return byteOctets;
	}


	public static Object tiraData(Date valueDate) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(valueDate);
		return cal.get(cal.YEAR)+"-"+cal.get(cal.MONTH)+"-"+cal.get(cal.DAY_OF_MONTH);
	}
	
	public static Object tiraHora(Date valueDate) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(valueDate);
		return cal.get(cal.HOUR_OF_DAY)+":"+cal.get(cal.MINUTE);
	}
	
}


