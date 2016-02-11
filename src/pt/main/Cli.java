package pt.main;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * 
 * 
 *
 */
public class Cli {
	/**
	 * 
	 */
	private Logger log;
	
	/**
	 * 
	 */
	private String[] args = null;
	
	/**
	 * 
	 */
	private Options options = new Options();

	/**
	 * 
	 */
	private boolean optionVerbose;
	private boolean optionInstallDB;
	private int optionTCPPort;

	/**
	 * 
	 * @param args
	 * @param log
	 */
	public Cli(
			String[] args, 
			Logger log) {
		//
		this.log = log;
		this.args = args;
		
		// set options default values
		optionVerbose = false;
		optionInstallDB = false;
		optionTCPPort = 8080;
		
		// add the necessary application options
		options.addOption("h", "help", false, "Show help.");
		options.addOption("v", "verbose", true, "Verbose application output.");
		options.addOption("i", "installdb", true, "Run OWL installation procedures.");
		options.addOption("p", "tcpport", true, "Run OWL installation procedures.");

	}

	/**
	 * 
	 * @return
	 */
	public boolean parse() {
		//
		boolean result = true;
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		try {
			
			// parse command line arguments
			cmd = parser.parse(options, args);
			if (cmd.hasOption("h")) {
				// show help information
				help();
				result = false;
			}
			if (result) {
				if (cmd.hasOption("v")) {
					// gather verbose option value
					optionVerbose = Boolean.valueOf(cmd.getOptionValue("v"));
					log.log(
							Level.INFO, 
							"Value for option Verbose -v = " + optionVerbose);
				}
				if (cmd.hasOption("i")) {
					// gather verbose option value
					optionInstallDB = Boolean.valueOf(cmd.getOptionValue("i"));
					log.log(
							Level.INFO, 
							"Value for option Install DB -v = " + optionInstallDB);
				}
				if (cmd.hasOption("p")) {
					try {
						// gather verbose option value
						optionTCPPort = Integer.valueOf(cmd.getOptionValue("p"));
						log.log(
								Level.INFO, 
								"Value for option TCP Port -v = " + optionTCPPort);
						
					} catch (NumberFormatException e) {
						log.log(
								Level.SEVERE, 
								"Failed to parse comand line TCPPort. Incorrect format.", 
								e);
						help();
						result = false;
					}
				}
			}
			
		} catch (ParseException e) {
			log.log(
					Level.SEVERE, 
					"Failed to parse comand line properties", 
					e);
			help();
			result = false;
		}
		
		//
		return result; 
	}

	/**
	 * 
	 */
	private void help() {
		// This prints out some help
		HelpFormatter formater = new HelpFormatter();
		formater.printHelp("Main", options);
		
	}

	/**
	 * 
	 * @return
	 */
	public boolean isOptionVerbose() {
		return optionVerbose;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isOptionInstallDB() {
		return optionInstallDB;
	}

	/**
	 * 
	 * @return
	 */
	public int getOptionTCPPort() {
		return optionTCPPort;
	}
	
}
