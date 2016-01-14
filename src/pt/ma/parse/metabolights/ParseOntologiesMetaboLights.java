package pt.ma.parse.metabolights;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pt.ma.parse.Ontology;
import pt.ma.parse.interfaces.IMetaOntologies;

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
	
	@Override
	public List<Ontology> getMetaOntologies() {
		List<Ontology> ontologies = new ArrayList<Ontology>();

		// set regular expression to compile
		String regex = "^Term Source File\\t*\"http\\:\\/\\/[a-zA-Z0-9\\-\\.]+\\S+\"";
		Pattern pattern = Pattern.compile(regex);
		
		//
		Matcher matcher = null;
		boolean itemFound = false;
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
							String uri = matcher.group(i).trim();
							if (uri.length() > 0) {
								Ontology ontology = new Ontology(uri);
								ontologies.add(ontology);
							}

						} catch (Exception e) {
							// TODO: log action
						}
					}
				}				
				itemFound = true;
				
			}
		}
				
		return ontologies;
	}

	@Override
	public void setMetaFile(byte[] file) {
		// TODO Auto-generated method stub
		
	}

}
