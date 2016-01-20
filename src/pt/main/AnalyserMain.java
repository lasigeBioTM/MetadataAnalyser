package pt.main;

import pt.blackboard.Blackboard;
import pt.ma.annotation.AnnotationObject;
import pt.ma.calculus.CalculusObject;
import pt.ma.log.LogObject;
import pt.ma.owl.OWLObject;
import pt.ma.parse.ParseObject;
import pt.ma.proxy.ProxyObject;
import pt.ma.term.TermObject;

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
	private static boolean INSTALL_DB = false;
	
	/**
	 * 
	 */
	private static Blackboard blackboard;
	
	/**
	 * 
	 */
	private static LogObject log;
	private static OWLObject owl;
	private static CalculusObject calculus;
	private static TermObject terms;
	private static AnnotationObject concepts;
	private static ParseObject parse; 
	private static ProxyObject proxy;
	
	/**
	 * 
	 */
	private static long startStamp;
	
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		//
		startStamp = System.currentTimeMillis();
		
		//
		blackboard = new Blackboard();
		
		// start all solution components
		log = new LogObject(blackboard);
		owl = new OWLObject(blackboard, INSTALL_DB, VERBOSE);
		calculus = new CalculusObject(blackboard, VERBOSE);
		terms = new TermObject(blackboard, VERBOSE);
		concepts = new AnnotationObject(blackboard, VERBOSE);
		parse = new ParseObject(blackboard, VERBOSE);
		proxy = new ProxyObject(blackboard, VERBOSE);
		
	}

}
