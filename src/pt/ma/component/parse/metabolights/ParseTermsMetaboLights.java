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
		//String regexP = "^Study\\s" + className + "\\s*\\S*\\s*Type\\s*Term\\s*Accession\\s*Number";
		String regexP = "^Study\\s*" + className + "\\s*.*\\s*Term\\s*Accession\\s*Number";
		String regexC = "\\s*Source\\s*";
		//String regexD = "\"(.*?)\"";
		String regexD = "\"([^\"\t]+)\"";
		Pattern patternP = Pattern.compile(regexP);
		Pattern patternC = Pattern.compile(regexC);
		Pattern patternD = Pattern.compile(regexD);
		
		// iterate trough all the lines in metafile
		int termCounter = 0; String previousLine = null;
		String source = new String(metafile);
		Scanner scanner = new Scanner(source);
		try {
			while(scanner.hasNext()) {
				String actualLine = scanner.nextLine();
				try {
					//
					matcher = patternP.matcher(actualLine);
					if (matcher.find()) {
						try {
							//
							matcher = patternC.matcher(previousLine);
							if (!matcher.find()) {
								try {
									//
									matcher = patternD.matcher(previousLine);
									while(matcher.find()) {
										for(int i = 0; i < matcher.groupCount(); i++) {
											// 
											String termName = StringWork.sanitaze(matcher.group(i).trim());
											if (termName.length() > 0) {
												String termID = "TERM_" + String.valueOf(termCounter); 
												MetaTerm term = new MetaTerm(termID, termName);
												if (!haveTerm(terms, term)) {
													terms.add(term);
												}
												termCounter++;
											}
										}
									}
									
								} catch (Exception e) {
									System.out.println("Terms Parse RegExD Exception: " + 
											previousLine + "; " + e.getMessage());
								}
							}
							
						} catch (Exception e) {
							//
							System.out.println("Terms Parse RegExC Exception: " + 
									previousLine + "; " + e.getMessage());

						}
					}
					
				} catch (Exception e) {
					//
					System.out.println("Terms Parse RegExP Exception: " + 
							actualLine + "; " + e.getMessage());
				}
				previousLine = actualLine;
			}
			
		} catch (IllegalStateException e) {
			//
			System.out.println("Terms Parse Illegal State Exception: " + 
					source + "; " + e.getMessage());
		} catch (Exception e) {
			//
			System.out.println("Terms Parse Exception: " + 
					source + "; " + e.getMessage());
		} finally {
			//
			scanner.close();
		}

		//
		return terms;
	}

	@Override
	public void setMetaFile(byte[] file) {
		this.metafile = metafile;
		
	}
	
	// PRIVATE METHODS
	
	private boolean haveTerm(
			List<MetaTerm> terms, 
			MetaTerm term) {
		//
		boolean result = false;
		//
		int counter = 0;
		while (!result && counter < terms.size()) {
			MetaTerm item = terms.get(counter);
			if (term.equals(item)) {
				result = true;
			}
			counter++;
		}	
		//
		return result;
	}

}
