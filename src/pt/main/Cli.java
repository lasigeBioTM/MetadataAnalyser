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
	private String optionTCPAddress;
	private int optionTCPPort;
	private String optionDBName;
	private String optionDBServer;
	private String optionDBUser;
	private String optionDBPassword;
	private int optionLoopDuration;
	private String optionTestURL;
	private boolean optionCache;
	
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
		optionTCPAddress = "127.0.0.1";
		optionTCPPort = 8080;
		optionDBName = "owltosql";
		optionDBServer = "127.0.0.1";
		optionDBUser = "owltosql";
		optionDBPassword = "owltosql";
		optionLoopDuration = 100;
		optionTestURL = null;
		optionCache = true;
		
		// add the necessary application options
		options.addOption("h", "help", false, "Show help.");
		options.addOption("v", "verbose", true, "Verbose application output.");
		options.addOption("i", "installdb", true, "Run OWL installation procedures.");
		options.addOption("a", "tcpaddress", true, "Establish TCP listen socket address.");
		options.addOption("p", "tcpport", true, "Establish TCP listen socket port.");
		options.addOption("n", "dbname", true, "MySQL Server Database name.");
		options.addOption("s", "dbserver", true, "MySQL Server address.");
		options.addOption("u", "dbuser", true, "MySQL Server user.");
		options.addOption("w", "dbpassword", true, "MySQL Server user password.");
		options.addOption("l", "loopduration", true, "Thread loop duration in mileseconds.");
		options.addOption("t", "testurl", true, "URL to test output connections.");
		options.addOption("c", "cache", true, "Cache database specificity results.");

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
							"Value for option Install DB -i = " + optionInstallDB);
				}
				if (cmd.hasOption("a")) {
					// gather verbose option value
					optionTCPAddress = String.valueOf(cmd.getOptionValue("a"));
					log.log(
							Level.INFO, 
							"Value for option TCP Address -a = " + optionTCPAddress);
				}
				if (cmd.hasOption("p")) {
					try {
						// gather verbose option value
						optionTCPPort = Integer.valueOf(cmd.getOptionValue("p"));
						log.log(
								Level.INFO, 
								"Value for option TCP Port -p = " + optionTCPPort);
						
					} catch (NumberFormatException e) {
						log.log(
								Level.SEVERE, 
								"Failed to parse comand line TCPPort. Incorrect format.", 
								e);
						help();
						result = false;
					}
				}
				if (cmd.hasOption("n")) {
					// gather verbose option value
					optionDBName = String.valueOf(cmd.getOptionValue("n"));
					log.log(
							Level.INFO, 
							"Value for option DB name -n = " + optionDBName);
				}				
				if (cmd.hasOption("s")) {
					// gather verbose option value
					optionDBServer = String.valueOf(cmd.getOptionValue("s"));
					log.log(
							Level.INFO, 
							"Value for option DB Server address -s = " + optionDBServer);
				}
				if (cmd.hasOption("u")) {
					// gather verbose option value
					optionDBUser = String.valueOf(cmd.getOptionValue("u"));
					log.log(
							Level.INFO, 
							"Value for option DB user name -u = " + optionDBUser);
				}
				if (cmd.hasOption("w")) {
					// gather verbose option value
					optionDBPassword = String.valueOf(cmd.getOptionValue("w"));
					log.log(
							Level.INFO, 
							"Value for option DB user password -w = " + optionDBPassword);
				}
				if (cmd.hasOption("l")) {
					try {
						// gather verbose option value
						optionLoopDuration = Integer.valueOf(cmd.getOptionValue("l"));
						log.log(
								Level.INFO, 
								"Value for option Thread Loop Duration -l = " + optionLoopDuration);
						
					} catch (NumberFormatException e) {
						log.log(
								Level.SEVERE, 
								"Failed to parse comand line LoopDuration. Incorrect format.", 
								e);
						help();
						result = false;
					}					
				}
				if (cmd.hasOption("t")) {
					// gather verbose option value
					optionTestURL = String.valueOf(cmd.getOptionValue("t"));
					log.log(
							Level.INFO, 
							"Value for option test URL -t = " + optionTestURL);
				}
				if (cmd.hasOption("c")) {
					// gather verbose option value
					optionCache = Boolean.valueOf(cmd.getOptionValue("c"));
					log.log(
							Level.INFO, 
							"Value for option Cache -c = " + optionCache);
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
	public String getOptionTCPAddress() {
		return optionTCPAddress;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getOptionTCPPort() {
		return optionTCPPort;
	}

	/**
	 * 
	 * @return
	 */
	public String getOptionDBName() {
		return optionDBName;
	}

	/**
	 * 
	 * @return
	 */
	public String getOptionDBServer() {
		return optionDBServer;
	}

	/**
	 * 
	 * @return
	 */
	public String getOptionDBUser() {
		return optionDBUser;
	}

	/**
	 * 
	 * @return
	 */
	public String getOptionDBPassword() {
		return optionDBPassword;
	}

	/**
	 * 
	 * @return
	 */
	public int getOptionLoopDuration() {
		return optionLoopDuration;
	}

	/**
	 * 
	 * @return
	 */
	public String getOptionTestURL() {
		return optionTestURL;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isOptionCache() {
		return optionCache;
	}

}
