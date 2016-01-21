package pt.ma.component.parse.metabolights;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pt.ma.component.parse.interfaces.IMetaTerms;
import pt.ma.metadata.MetaAnnotation;
import pt.ma.metadata.MetaClass;
import pt.ma.metadata.MetaTerm;
import pt.ma.util.StringWork;

/**
 * 
 * 
 *
 */
public class ParseTermsMetaboLights implements IMetaTerms {

	/**
	 * 
	 */
	private byte[] metafile;
	
	/**
	 * 
	 * @param metafile
	 */
	public ParseTermsMetaboLights(byte[] metafile) {
		this.metafile = metafile;
	}

	@Override
	public List<MetaTerm> getMetaTerms(MetaClass metaClass) {
		List<MetaTerm> terms = new ArrayList<MetaTerm>();
		
		// read class name
		String className = metaClass.getClassName();
		
		// set regular expression to compile
		Matcher matcher = null;
		String regexP = "^Study\\s" + className + "\\s*\\S*\\s*Type\\s*Term\\s*Accession\\s*Number";
		String regexC = "\\s*Source\\s*";
		String regexD = "\"([^\"]+)\"";
		Pattern patternP = Pattern.compile(regexP);
		Pattern patternC = Pattern.compile(regexC);
		Pattern patternD = Pattern.compile(regexD);
		
		// iterate trough all the lines in metafile
		int termCounter = 0; String previousLine = null;
		String source = new String(metafile);
		Scanner scanner = new Scanner(source);
		while(scanner.hasNext()) {
			//
			String actualLine = scanner.nextLine();
			matcher = patternP.matcher(actualLine);
			if (matcher.find()) {
				//
				matcher = patternC.matcher(previousLine);
				if (!matcher.find()) {
					//
					matcher = patternD.matcher(previousLine);
					while(matcher.find()) {
						for(int i = 0; i < matcher.groupCount(); i++) {
							try {
								// 
								String termName = StringWork.sanitaze(matcher.group(i).trim());
								if (termName.length() > 0) {
									String termID = "TERM_" + String.valueOf(termCounter); 
									MetaTerm term = new MetaTerm(termID, termName);
									terms.add(term);
									termCounter++;
								}

							} catch (Exception e) {
								// TODO: log action
							}
						}				

					}
				}
			}
			previousLine = actualLine;
		}
		scanner.close();
		
		//
		return terms;
	}

	@Override
	public void setMetaFile(byte[] file) {
		this.metafile = metafile;
		
	}

}
