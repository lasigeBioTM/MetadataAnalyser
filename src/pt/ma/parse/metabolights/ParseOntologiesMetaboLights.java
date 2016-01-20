package pt.ma.parse.metabolights;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pt.ma.metadata.MetaOntology;
import pt.ma.parse.interfaces.IMetaOntologies;
import pt.ma.util.FileWork;
import pt.ma.util.StringWork;

/**
 * 
 * 
 *
 */
public class ParseOntologiesMetaboLights implements IMetaOntologies {

	/**
	 * 
	 */
	private byte[] metafile;
	
	/**
	 * 
	 * @param metafile
	 */
	public ParseOntologiesMetaboLights(byte[] metafile) {
		this.metafile = metafile;
	}
	
	/**
	 * 
	 */
	@Override
	public List<MetaOntology> getMetaOntologies() {
		List<MetaOntology> ontologies = new ArrayList<MetaOntology>();

		// set regular expression to compile
		Matcher matcher = null;
		String regex = "^Term Source File\\t*\"http\\:\\/\\/[a-zA-Z0-9\\-\\.]+\\S+\"";
		Pattern pattern = Pattern.compile(regex);
		
		//
		boolean itemFound = false; int ontoCounter = 0;
		String source = new String(metafile);
		Scanner scanner = new Scanner(source);
		while(scanner.hasNext() && !itemFound) {
			//
			String line = scanner.nextLine();
			matcher = pattern.matcher(line);
			if (matcher.find()) {
				
				//
				regex = "\"(http\\:\\/\\/[a-zA-Z0-9\\-\\.]+\\S+)\"";
				pattern = Pattern.compile(regex);
				matcher = pattern.matcher(line);
				while (matcher.find()) {
					for(int i = 0; i < matcher.groupCount(); i++) {
						try {
							// an ontology have matched
							String ontoURI = StringWork.sanitaze(matcher.group(i).trim());
							if (ontoURI.length() > 0) {
								String ontoID = "ONTO_" + String.valueOf(ontoCounter); 
								MetaOntology ontology = new MetaOntology(ontoID, ontoURI);
								ontologies.add(ontology);
								ontoCounter++;
							}

						} catch (Exception e) {
							// TODO: log action
						}
					}
				}				
				itemFound = true;
				
			}
		}
		scanner.close();
		//
		return ontologies;
	}

	@Override
	public void setMetaFile(byte[] file) {
		// TODO Auto-generated method stub
		
	}

}
