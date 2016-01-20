package pt.ma.component.parse.metabolights;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pt.ma.component.parse.interfaces.IMetaAnnotations;
import pt.ma.metadata.MetaAnnotation;
import pt.ma.metadata.MetaClass;
import pt.ma.util.StringWork;

/**
 * 
 * 
 *
 */
public class ParseAnnotationsMetaboLights implements IMetaAnnotations {

	/**
	 * 
	 */
	private byte[] metafile;
	
	/**
	 * 
	 * @param metafile
	 */
	public ParseAnnotationsMetaboLights(byte[] metafile) {
		this.metafile = metafile;
	}
	
	/**
	 * 
	 */
	@Override
	public List<MetaAnnotation> getMetaAnnotations(MetaClass metaClass) {
		List<MetaAnnotation> annotations = new ArrayList<MetaAnnotation>();
		
		// read class name
		String className = metaClass.getClassName();
		
		// set regular expression to compile
		Matcher matcher = null;
		String regexP = "^Study\\s" + className + "\\s*(\\S+\\s)+\"(http\\:\\/\\/[a-zA-Z0-9\\-\\.]+\\S+)\"";
		String regexC = "\"(http\\:\\/\\/[a-zA-Z0-9\\-\\.]+\\S+)\"";
		Pattern patternP = Pattern.compile(regexP);
		Pattern patternC = Pattern.compile(regexC);
		
		// iterate trough all the lines in metafile
		int annoCounter = 0;
		String source = new String(metafile);
		Scanner scanner = new Scanner(source);
		while(scanner.hasNext()) {
			//
			String line = scanner.nextLine();
			matcher = patternP.matcher(line);
			if (matcher.find()) {
				//
				matcher = patternC.matcher(line);
				while (matcher.find()) {
					for(int i = 0; i < matcher.groupCount(); i++) {
						try {
							// 
							String annoURI = StringWork.sanitaze(matcher.group(i).trim());
							if (annoURI.length() > 0) {
								String annoID = "ANNO_" + String.valueOf(annoCounter); 
								MetaAnnotation annotation = new MetaAnnotation(annoID, annoURI);
								annotations.add(annotation);
								annoCounter++;
							}

						} catch (Exception e) {
							// TODO: log action
						}
					}				
				}
				
			}
		}
		scanner.close();
		
		//
		return annotations;
	}

	/**
	 * 
	 */
	@Override
	public void setMetaFile(byte[] file) {
		this.metafile = metafile;
		
	}

}
