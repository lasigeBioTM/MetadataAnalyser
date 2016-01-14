package pt.main;

import pt.blackboard.Blackboard;
import pt.ma.calculus.CalculusObject;
import pt.ma.concepts.ConceptsObject;
import pt.ma.owl.OWLObject;
import pt.ma.parse.ParseObject;
import pt.ma.proxy.ProxyObject;
import pt.ma.terms.TermsObject;

/**
 * 
 * 
 *
 */
public class AnalyserMain {

	/**
	 * 
	 */
	private static boolean VERBOSE = true;
	
	/**
	 * 
	 */
	private static Blackboard blackboard;
	
	/**
	 * 
	 */
	private static OWLObject owl;
	private static CalculusObject calculus;
	private static TermsObject terms;
	private static ConceptsObject concepts;
	private static ParseObject parse; 
	private static ProxyObject proxy;
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		//
		blackboard = new Blackboard();
		
		// start all solution components
		//owl = new OWLObject(blackboard, VERBOSE);
		calculus = new CalculusObject(blackboard, VERBOSE);
		terms = new TermsObject(blackboard, VERBOSE);
		concepts = new ConceptsObject(blackboard, VERBOSE);
		parse = new ParseObject(blackboard, VERBOSE);
		proxy = new ProxyObject(blackboard, VERBOSE);
		
	}

}
