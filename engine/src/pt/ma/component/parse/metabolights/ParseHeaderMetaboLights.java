package pt.ma.component.parse.metabolights;

import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pt.ma.component.parse.interfaces.IMetaHeader;

/**
 * 
 * @author 
 *
 */
public class ParseHeaderMetaboLights implements IMetaHeader {

	/**
	 * 
	 */
	private byte[] metafile;
	
	/**
	 * 
	 * @param metafile
	 */
	public ParseHeaderMetaboLights(byte[] metafile) {
		super();
		
		//
		this.metafile = metafile;
	}
	
	@Override
	public String getStudyID() {
		String result = null;

		// set regular expression to compile
		String regex = "^Study Identifier\\t*\"([a-zA-Z0-9]+)\"$";
		Pattern pattern = Pattern.compile(regex);
		
		//
		boolean itemFound = false;
		String source = new String(metafile);
		Scanner scanner = new Scanner(source);
		while(scanner.hasNext() && !itemFound) {
			String line = scanner.nextLine().trim();
			//
			Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				result = matcher.group(1).trim().toUpperCase();
				itemFound = true;
			}
		}
		scanner.close();
		return result;
	}

	@Override
	public String getCheckSum() {
		
		return null;
	}

	@Override
	public void setMetaFile(byte[] metafile) {
		//
		this.metafile = metafile;
		
	}

}
