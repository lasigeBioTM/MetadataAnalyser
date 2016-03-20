package pt.main;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
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
import pt.ma.util.FileWork;

/**
 * Main application start 
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
			
			if (configuration.getOptionTestURL() != null && 
					configuration.getOptionTestURL().length() > 0) {
				
				// log action
				logger.log(
						Level.INFO, 
						"MetaData Analyser Remote Connection Test enabled.");
				
				try {
					//					
					byte[] result = FileWork.readRemoteIntoByteArray(
							configuration.getOptionTestURL());
					
					// log action
					logger.log(
							Level.INFO, 
							"Remote file loaded: Length " + result.length + " bytes");					
					
				} catch (MalformedURLException e) {
					// log action
					logger.log(
							Level.SEVERE, 
							"An Malformed URL Exception has occurred reading remote file: " + configuration.getOptionTestURL(), 
							e);

				} catch (IOException e) {
					// log action
					logger.log(
							Level.SEVERE, 
							"An IO Exception has occurred reading remote file: " + configuration.getOptionTestURL(), 
							e);
					
				} catch (Exception e) {
					// log action
					logger.log(
							Level.SEVERE, 
							"An Exception has occurred reading remote file: " + configuration.getOptionTestURL(), 
							e);
				}
				
			} else {
				//
				blackboard = new Blackboard();
				// start all solution components
				log = new LogObject(
						blackboard, 
						LogTarget.FILE);
				owl = new OWLObject(
						blackboard, 
						configuration.isOptionInstallDB(), 
						configuration.getOptionDBName(),
						configuration.getOptionDBServer(),
						configuration.getOptionDBUser(),
						configuration.getOptionDBPassword(),
						configuration.getOptionLoopDuration(),
						configuration.isOptionVerbose(),
						configuration.isOptionCache());
				calculus = new CalculusObject(
						blackboard, 
						configuration.getOptionLoopDuration(),
						configuration.isOptionVerbose());
				terms = new TermObject(
						blackboard, 
						configuration.getOptionLoopDuration(),
						configuration.isOptionVerbose());
				concepts = new AnnotationObject(
						blackboard, 
						configuration.getOptionLoopDuration(),
						configuration.isOptionVerbose());
				parse = new ParseObject(
						blackboard, 
						configuration.getOptionLoopDuration(),
						configuration.isOptionVerbose());
				try {
					proxy = new ProxyObject(
							blackboard, 
							configuration.getOptionTCPAddress(),
							configuration.getOptionTCPPort(), 
							configuration.getOptionLoopDuration(),
							configuration.isOptionVerbose());
					
				} catch (UnknownHostException e) {
					logger.log(
							Level.SEVERE, 
							"MetaData Analyser Main Process loading failed.", 
							e);
				}

				// log action
				if (configuration.isOptionVerbose()) {
					logger.log(
							Level.INFO, 
							"MetaData Analyser Main Process loading complete.");
				}
			}

		} else {
			// log action
			logger.log(
					Level.INFO, 
					"MetaData Analyser Main Process load failed.");

			
		}
	}

	
}
