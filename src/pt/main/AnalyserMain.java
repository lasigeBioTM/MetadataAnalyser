package pt.main;

import java.util.logging.Level;
import java.util.logging.Logger;

import pt.blackboard.Blackboard;
import pt.ma.component.annotation.AnnotationObject;
import pt.ma.component.calculus.CalculusObject;
import pt.ma.component.log.LogObject;
import pt.ma.component.log.LogTarget;
import pt.ma.component.owl.OWLObject;
import pt.ma.component.parse.ParseObject;
import pt.ma.component.proxy.ProxyObject;
import pt.ma.component.term.TermObject;

/**
 * 
 * 
 *
 */
public class AnalyserMain {
		
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
		// set logger instance for this class
		Logger logger = Logger.getLogger(AnalyserMain.class.getName());
				
		// gather configuration arguments
		Cli configuration = new Cli(
				args, 
				Logger.getLogger(Cli.class.getName()));
				
		// read configuration file
		if (configuration.parse()) {
			
			// log action
			if (configuration.isOptionVerbose()) {
				logger.log(
						Level.INFO, 
						"Starting MetaData Analyser Main Process.");
			}

			//
			startStamp = System.currentTimeMillis();
			//
			blackboard = new Blackboard();
			// start all solution components
			log = new LogObject(
					blackboard, 
					LogTarget.FILE);
			owl = new OWLObject(
					blackboard, 
					configuration.isOptionInstallDB(), 
					configuration.isOptionVerbose());
			calculus = new CalculusObject(
					blackboard, 
					configuration.isOptionVerbose());
			terms = new TermObject(
					blackboard, 
					configuration.isOptionVerbose());
			concepts = new AnnotationObject(
					blackboard, 
					configuration.isOptionVerbose());
			parse = new ParseObject(
					blackboard, 
					configuration.isOptionVerbose());
			proxy = new ProxyObject(
					blackboard, 
					configuration.getOptionTCPPort(), 
					configuration.isOptionVerbose());

			// log action
			if (configuration.isOptionVerbose()) {
				logger.log(
						Level.INFO, 
						"MetaData Analyser Main Process loading complete.");
			}
			
		} else {
			// log action
			logger.log(
					Level.INFO, 
					"MetaData Analyser Main Process load failed.");

			
		}
	}

	
}
