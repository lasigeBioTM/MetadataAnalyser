package pt.owlsql;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import difflib.Delta;
import difflib.Delta.TYPE;
import difflib.DiffUtils;
import difflib.Patch;
import pt.json.JSONException;


/**
 * This class is the starting point of the OWLtoSQL software. This class reads the configuration file and prepares a
 * database with all the vital tables and information regarding the OWL information that it will store. It then goes on
 * by executing the extractors defined in that configuration file.
 */
/**
 * @author jferreira
 *
 */
public final class Application {
    
    private static class Report {
        
        public static Report createFailReport(String message) {
            Report report = new Report();
            report.message = message;
            return report;
        }
        
        
        public static Report createPassReport() {
            Report report = new Report();
            report.canProceed = true;
            return report;
        }
        
        
        private boolean canProceed;
        private String message;
    }
    
    private static Connection extractorsConnection;
    
    private static boolean simulate = false;
    private static Timestamp lastExecuted = null;
    private static int owlsource = 0; // 0 - from configuration file; 1 - from owl repository
    private static int owlstartindex = 0;
    private static int owloffset = 0;
    
    private static int originalNextIndex;
    
    private static ArrayList<Extractor> configExtractors = new ArrayList<>();
    
    
    private static final Hashtable<ExtractorSpec<?>, Timestamp> lastUpdated = new Hashtable<>();
    private static final Hashtable<Extractor, Extractor[]> dependencies = new Hashtable<>();
    private static final Hashtable<Extractor, Timestamp> classChangeTime = new Hashtable<>();
    
    private static final HashSet<Extractor> wasUpdated = new HashSet<>();
    private static ArrayList<Extractor> preparedExtractors = new ArrayList<>();
    
    private static Extractor currentExtractor;
    
    private static final Logger logger = Logger.getLogger(Application.class.getName());
    
    
    /**
     * Determines whether the underlying database already exists
     * 
     * @return <code>true</code> if it does; <code>false</code> otherwise.
     * @throws SQLException If the SQL server is not able to answer to this question
     */
    private static boolean databaseExists() throws SQLException {
        String database = Config.getDatabase();
        try (Connection connection = Config.connectToSQLServer();
             ResultSet resultSet = connection.getMetaData().getCatalogs()) {
            while (resultSet.next()) {
                if (resultSet.getString(1).equals(database))
                    return true;
            }
            return false;
        }
    }
    
    
    /**
     * This method performs a series of steps given an {@link Extractor}:
     * <ol>
     * <li>Ask for the dependencies of the extractor;
     * <li>Determine which actions of the extractor need to be performed:
     * <ul>
     * <li>{@link Extractor#removeFromDatabase()};
     * <li>{@link Extractor#prepareForFirstUse()};
     * <li>{@link Extractor#update()};
     * </ul>
     * <li>Prepare the extractor for use by any possible dependants.
     * </ol>
     * An extractor needs only to perform its actions if
     * <ul>
     * <li>it has not been executed before;
     * <li>the java class definition has been changed since its last execution; or
     * <li>the java class definition of one of its dependencies has changed since its last execution.
     * </ul>
     * 
     * @param extractor The extractor to process.
     * 
     * @throws SQLException If an SQL exception occurred during one of the many steps described above
     */
    private static void execute(Extractor extractor) throws SQLException {
        // To determine which actions need to be performed, we need these data
        Timestamp lastUpdateTime = lastUpdated.get(extractor.getSpec());
        boolean previouslyUpdated = lastUpdateTime != null;
        boolean mustUpdate;
        try {
            mustUpdate = !previouslyUpdated || needsUpdate(extractor, lastUpdateTime);
        }
        catch (OwlSqlException e) {
            throw new RuntimeException(e);
        }
        
        // The actual update action is wrapped in a try-catch environment so that we can attempt to remove any
        // incomplete information that may have been inserted in the database.
        
        logger.info("Executing " + extractor.getSpec().toJSONString());
        if (mustUpdate) {
            // From this point on, we are executing an extractor that *may* fail.
            // To recover from errors of this extractor, we need to make sure that the whole class knows the
            // identity of the extractopr being executed
            currentExtractor = extractor;
            
            // We first ensure that duplicate data is not inserted by removing previous data from the database
            if (previouslyUpdated) {
                logger.info("  removing previous information ...");
                if (!simulate)
                    extractor.removeFromDatabase();
            }
            logger.info("  preparing the tables to receive information ...");
            if (!simulate)
                extractor.prepareForFirstUse();
            
            logger.info("  updating ...");
            if (!simulate) {
                try {
                    extractor.update();
                }
                catch (Exception e) {
                    System.err.println("An error occurred during the update");
                    System.err.println("Trying to remove any invalid information that may have been added ...");
                    try {
                        extractor.removeFromDatabase();
                    }
                    catch (Exception e1) {
                        System.err.println("Failed!!!");
                        e1.addSuppressed(e);
                        throw e1;
                    }
                    throw e;
                }
                wasUpdated.add(extractor);
            }
            
            // No longer are we executing an extractor.
            // This means that in the case of a failure prepareForDependants (a few lines below) no recovery
            // will be performed. This suits, however, has recovering means removing everything from the database
            // and failing to prepare for dependencies does not necessarily imply an error in the database
            currentExtractor = null;
        }
        else
            logger.info("  no need to update this extractor");
        
        logger.info("  preparing for dependencies ...");
        if (!simulate)
            extractor.prepareForDependents();
        
        markExecutedExtractor(extractor);
    }
    
    
    private static void markExecutedExtractor(Extractor extractor) throws SQLException {
        try (PreparedStatement statement = extractorsConnection.prepareStatement(""
                + "INSERT INTO _extractors_realtime (json, last_updated) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, extractor.getSpec().toJSONString());
            statement.setTimestamp(2, new Timestamp(new Date().getTime()));
            statement.executeUpdate();
        }
    }
    
    
    private static void exit(String message, Throwable e) {
        System.err.println(message);
        if (e != null)
            e.printStackTrace();
        System.exit(1);
    }
    
    
    private static void exit(String message) {
        exit(message, null);
    }
    
    
    /**
     * Returns a report that reports on at most one ontology in the database that (a) is not present in the
     * configuration file; or (b) has been changed since the last time OWLtoSQL has run. If no ontology is at fault, a
     * passing report is returned.
     * 
     * @param connection A connection to the database
     * 
     * @return A report detailing if (a) execution can proceed; or (b) at least one ontology is at fault.
     * 
     * @throws SQLException If connection with the database failed for some reason.
     */
    private static Report findChangedOntology(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // We need to make sure the set of ontologies in the database and in the configuration is the same
            // We do this by going through the list of ontologies in the database and making sure that it exists in the
            // configuration and then by making sure that all the ontologies in the configuration file have been seen in
            // the database as well. We do this manually (instead of relying on a simple .equals()) because we want to
            // report on the difference. Finally, we must determine if the modification date of each ontology is more
            // recent than the last time OWLtoSQL executed
            
            // We use a copy because we need to remove the ontology UIRs found in the database
            HashSet<URI> fromConfig = new HashSet<>(Config.getOntologiesURI());
            
            try (ResultSet resultSet = statement.executeQuery("SELECT uri FROM _ontologies")) {
                while (resultSet.next()) {
                    URI uri = URI.create(resultSet.getString(1)).normalize();
                    if (!fromConfig.contains(uri))
                        return Report.createFailReport(""
                                + "Ontology on "
                                + uri
                                + " is no longer supplied by the configuration file.");
                    fromConfig.remove(uri);
                    
                    // Let's determine if the ontology has changed in the mean time
                    URLConnection urlConnection;
                    try {
                        urlConnection = uri.toURL().openConnection();
                    }
                    catch (IOException e) {
                        return Report.createFailReport("Unable to open ontology at " + uri + ".\n" + e.getMessage());
                    }
                    
                    Timestamp ts = new Timestamp(urlConnection.getLastModified());
                    if (ts.after(lastExecuted))
                        // This ontology is newer than the last time the software was run.
                        return Report.createFailReport("Ontology " + uri + " has been modified since " + lastExecuted);
                }
            }
            
            // All the URIs from the database are present in the configuration file.
            // We also need to determine if all the URIs in the configuration file were found in the database
            if (!fromConfig.isEmpty())
                return Report.createFailReport("" //
                        + "Ontology on "
                        + fromConfig.iterator().next() // Select one of the URIs on the set
                        + " has been removed from the configuration file.");
        }
        
        return Report.createPassReport();
    }
    
    
    /**
     * Returns the name of a metadata table that does not exist in this database.
     * 
     * @return The name of a metadata table that does not exist in this database. If all tables exist, return null.
     * 
     * @throws SQLException
     */
    private static String missingMetadataTable() throws SQLException {
        try (Connection connection = Config.connectToDatabase()) {
            DatabaseMetaData md = connection.getMetaData();
            for (String tablename : "_owlsql,_ontologies,_extractors,_extractors_realtime".split(",")) {
                try (ResultSet resultSet = md.getTables(null, null, tablename, null)) {
                    if (!resultSet.next())
                        return tablename;
                }
            }
            return null;
        }
    }
    
    
    /**
     * An extractor needs an update in four different scenarios:
     * <ul>
     * <li>If it has never been executed before (this happens on the first use of OWLtoSQL or if an executor
     * specification of the configuration file is determined to an addition to the ones stored in the database);
     * <li>If the class file of the extractor has changed since the last time it ran (this is detected based on the
     * timestamp of the .class file);
     * <li>If one of the dependencies of the extractor was also updated;
     * <li>If the extractor itself mandates that it must execute. This can be useful when an extractor uses external
     * resources and only neds to update if those resources are changed.
     * </ul>
     * 
     * @param extractor
     * @param lastUpdateTime
     * 
     * @return
     * 
     * @throws OwlSqlException
     */
    private static boolean needsUpdate(Extractor extractor, Timestamp lastUpdateTime) throws OwlSqlException {
        if (lastUpdateTime == null) {
            return true;
        }
        
        if (classChangeTime.get(extractor).after(lastUpdateTime)) {
            return true;
        }
        
        for (Extractor dependency : dependencies.get(extractor)) {
            if (wasUpdated.contains(dependency)) {
                return true;
            }
        }
        
        if (extractor.mustUpdate(lastUpdateTime))
            return true;
        
        return false;
    }
    
    
    /**
     * This method creates a new database in the SQL server, if necessary, and then determines whether that database can
     * be used as an underlying storage machine for the OWLtoSQL software. This includes making sure that the database
     * is empty or, in case it is not, whether it has previously been used as an OWLtoSQL database. We detect assume
     * that if the metadata tables are all present, the database is an OWLtoSQL one.
     * <p>
     * It also includes making sure that the ontologies specified in the configuration file are the same as the ones
     * stored in the database, and that the files have not been changed since the last time the software ran.
     * <p>
     * Notice that the software never deletes the database. The user must go idrectly into the SQL server and do that
     * themself.
     * <p>
     * The method returns a {@link Report} object that either allows the continuation of the execution or provides an
     * explanation of why continuation cannot occur.
     * 
     * @return If the database is ready for use, returns a Report allowing continuation of execution; otherwise, returns
     *         a report detailing the reason for not allowing further execution of OWLtoSQL.
     * 
     * @throws SQLException
     */
    private static Report verifyDatabase() throws SQLException {
        if (!databaseExists()) {
            // Database does not exist. Create it.
            createDatabase();
            createPLObjects();
        }
        
        else {
            // Database exists. Make sure it is an OWLtoSQL database by looking for the metadata tables
            String missingMetadataTable = missingMetadataTable();
            if (missingMetadataTable != null) {
                String message = "The metadata table " + missingMetadataTable + " is missing.\n";
                message += "You need to either "
                        + "(a) delete the database entirely and start from scratch or; "
                        + "(b) change the name of the database in the configuration file to the correct one.";
                return Report.createFailReport(message);
            }
        }
        
        // The database and needed tables are physically there. Now we need to make sure that the set of ontologies has
        // not changed between the previous run and this one.
        try (Connection connection = Config.connectToDatabase()) {
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT last_execution FROM _owlsql")) {
                if (resultSet.next())
                    lastExecuted = resultSet.getTimestamp(1);
            }
            
            if (lastExecuted != null) {
                // If OWLtoSQL has never executed on this database, we assume that loading the ontologies will be OK.
                // Otherwise, we need to make sure that the set of ontologies in the database an in the cofniguration
                // file are the same and have not changed in the meantime
                Report changedOntologyReport = findChangedOntology(connection);
                if (!changedOntologyReport.canProceed) {
                    return Report.createFailReport(changedOntologyReport.message
                            + "\nTo prevent misaligned information between the OWL files and the stored information, "
                            + "you should wipe the database and start from scratch.");
                }
            }
            
            // Right now, tables are correctly prepared. We can now update the metadata tables related to this topic
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("TRUNCATE _owlsql");
                statement.executeUpdate("TRUNCATE _ontologies");
            }
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO _ontologies (uri) VALUES (?)")) {
                for (URI uri : Config.getOntologiesURI()) {
                    statement.setString(1, uri.toString());
                    statement.executeUpdate();
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(""
                    + "INSERT INTO _owlsql (last_execution) VALUES (?)")) {
                statement.setTimestamp(1, new Timestamp(new Date().getTime()));
                statement.executeUpdate();
            }
        }
        
        // As everything is OK, we can now establish a connection to the database
        extractorsConnection = Config.connectToDatabase();
        
        return Report.createPassReport();
    }
    
    
    private static ArrayList<ExtractorSpec<?>> retrieveSpecsFromDatabase() throws SQLException {
        JsonParser jsonParser = new JsonParser();
        
        try (Statement statement = extractorsConnection.createStatement();
             ResultSet resultSet = statement.executeQuery(""
                     + "SELECT id, json, last_updated FROM _extractors ORDER BY id")) {
            ArrayList<ExtractorSpec<?>> result = new ArrayList<>();
            
            while (resultSet.next()) {
                int id = resultSet.getInt(1);
                String json = resultSet.getString(2);
                Timestamp lastUpdateTime = resultSet.getTimestamp(3);
                
                JsonElement jsonElement = jsonParser.parse(json);
                try {
                    ExtractorSpec<?> spec = Config.createExtractorSpecFromJSON(jsonElement);
                    lastUpdated.put(spec, lastUpdateTime);
                    result.add(spec);
                }
                catch (JSONException e) {
                    throw new Error("JSON parameters of extractor #" + id + " are invalid!", e);
                }
            }
            
            return result;
        }
    }
    
    
    private static void run() throws SQLException {
        
        // We now determine the differences between the extractors in the configuration file and the ones whose
        // execution has already been done (logged on the _exctrators table)
        ArrayList<ExtractorSpec<?>> sqlSpecs = retrieveSpecsFromDatabase();
        ArrayList<ExtractorSpec<?>> configSpecs = Config.getExtractorSpecs();
        Patch diff = DiffUtils.diff(sqlSpecs, configSpecs);
        List<Delta> deltas = diff.getDeltas();
        
        /*
         * Before executing an extractor, we need to make sure that its dependencies are satisfied. We do this by
         * running its Extractor.getDirectDependencies() method. This method (if well implemented) will call
         * Extractor.getDependency a number of times, each one defining a dependency of the extractor. Calling
         * Extractor.getDependency may throw an DependencyException: if it does, we assume that dependencies for this
         * extractor are not satisfied. There is a fine balance here: the Extractor creates a list of extractors which
         * can depend on other extractors. These dependencies will be retrieved from the set of already created
         * extractors. Once this "back-and-forth" has happened, we know that all dependencies are satisfied.
         */
        
        int current = 0;
        originalNextIndex = 0;
        
        for (Delta delta : deltas) {
            int revisedStart = delta.getRevised().getPosition();
            int revisedEnd = delta.getRevised().last();
            
            for (int i = current; i < revisedStart; i++) {
                execute(configExtractors.get(i));
                originalNextIndex++;
            }
            
            if (delta.getType() == TYPE.CHANGE || delta.getType() == TYPE.DELETE) {
                for (int i = delta.getOriginal().getPosition(), last = delta.getOriginal().last(); i <= last; i++) {
                    Extractor toRemove;
                    try {
                        toRemove = Extractor.createFromSpec(sqlSpecs.get(i), extractorsConnection);
                    }
                    catch (InstantiationException | JSONException e) {
                        // This should not happen, since the extractor has been created in other executions of OWLtoSQL
                        // This means that we can (possibly) ignore this exception silently. For now, let's make it a
                        // RuntimeException
                        throw new RuntimeException(e);
                    }
                    logger.info("Removing " + toRemove.getSpec().toJSONString());
                    toRemove.removeFromDatabase();
                    originalNextIndex++;
                }
            }
            
            if (delta.getType() == TYPE.CHANGE || delta.getType() == TYPE.INSERT) {
                // We always need to update these new extractors, as it is the first time they will execute
                for (int i = revisedStart; i <= revisedEnd; i++) {
                    execute(configExtractors.get(i));
                }
            }
            
            current = revisedEnd + 1;
        }
        
        for (int i = current; i < configSpecs.size(); i++) {
            execute(configExtractors.get(i));
            originalNextIndex++;
        }
    }
    
    
    private static void processArguments(String[] args) {
        String configFilename = Config.CONFIG_FILE;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-c") || args[i].equals("--config")) {
                i++;
                configFilename = args[i];
                
            } else if (args[i].equals("-s") || args[i].equals("--simulate")) {
                simulate = true;
            
            } else if (args[i].equals("-u") || args[i].equals("--owlsource")) {
            	switch (args[++i].toLowerCase()) {
					case "repo":
						owlsource = 1; // external owl repository
						break;
					default:
						owlsource = 0; // defaults to configuration file
				}
            	
            } else if (args[i].equals("-i") || args[i].equals("--startindex")) {
            	owlstartindex = Integer.parseInt(args[++i]);
            	
            } else if (args[i].equals("-o") || args[i].equals("--offset")) {
            	owloffset = Integer.parseInt(args[++i]); 
            	
            } else
                exit("Unrecognized command line argument " + args[i]);
        }
        
        try {
            Config.read(
            		configFilename, 
            		owlsource, 
            		owlstartindex, 
            		owloffset);
            
        }
        catch (IOException | JSONException e) {
            exit("Unable to read from configuration file", e);
        }
    }
    
    
    /**
     * @throws SQLException If creating the database fails
     */
    private static void createDatabase() throws SQLException {
        String database = Config.getDatabase();
        try (Connection connection = Config.connectToSQLServer()) {
            
            // Create the database
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE DATABASE " + database);
            }
            
            // The connection was opened outside of any database; just select the one created here
            connection.setCatalog(database);
            
            // Create the metadata tables
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("" //
                        + "CREATE TABLE _owlsql ("
                        + "  last_execution TIMESTAMP"
                        + ")");
                
                statement.executeUpdate("" //
                        + "CREATE TABLE _ontologies ("
                        + "  uri VARCHAR(512)"
                        + ")");
                
                statement.executeUpdate("" //
                        + "CREATE TABLE _extractors ("
                        + "  id INT PRIMARY KEY AUTO_INCREMENT, "
                        + "  json TEXT, "
                        + "  last_updated TIMESTAMP,"
                        + "  INDEX (id)"
                        + ")");
                
                statement.executeUpdate("" //
                        + "CREATE TABLE _extractors_realtime ("
                        + "  id INT PRIMARY KEY AUTO_INCREMENT, "
                        + "  json TEXT, "
                        + "  last_updated TIMESTAMP,"
                        + "  INDEX (id)"
                        + ")");
            }
        }
    }
    
    /**
     * 
     * @throws SQLException
     */
    private static void createPLObjects() throws SQLException {
        String database = Config.getDatabase();
        try (Connection connection = Config.connectToSQLServer()) {
                 
            // The connection was opened outside of any database; just select the one created here
            connection.setCatalog(database);
            
            // Create the metadata tables
            String path = "auxiliary/mysqlddl/";
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("" // "
                		+ readFromFile(path + "f_concept_ancestors_count.sql")                        
                		);
                
                statement.executeUpdate("" // "
                		+ readFromFile(path + "f_get_owlid_from_iri.sql")
                		);
                
                statement.executeUpdate("" // "
                		+ readFromFile(path + "sp_conceptspec.sql")
                		);
            }
        }
    }
    
    /**
	 * 
	 * @param url
	 * @return
	 */
	protected static String readFromFile(String file) {
		//
		BufferedReader bufReader = null;
		StringBuilder result = new StringBuilder();
		
		try {
			// open buffer stream reader for the given file
			String line = null;
			bufReader = new BufferedReader(new FileReader(file));
			while((line = bufReader.readLine()) != null) {
				result.append(line);
				result.append(System.lineSeparator());
			} 
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (bufReader != null) {
				try {
					bufReader.close();
				} catch (IOException e) {
					// TODO: logging action
					e.printStackTrace();
				}
			}
		}
		
		//
		return result.toString();
	}
    public static void main(String[] args) {
        processArguments(args);
/*        
        try {
            Report report = verifyDatabase();
            if (!report.canProceed)
                exit(report.message);
        }
        catch (SQLException e) {
            exit("Error on database connection", e);
        }
*/        
        // We load the ontologies into memory
        try {
            Config.loadOntologies();
        }
        catch (IOException e) {
            exit("Error on loading the ontologies", e);	
        }
/*        
        try {
            prepareExtractors();
        }
        catch (InstantiationException | JSONException | DependencyException | OwlSqlException e) {
            exit("Error on preparing the configuration file extractors", e);
        }
        
        // From this point on, an exception may happen *between* extractors. Therefore, we need to make sure that, if
        // an exception is thrown, the metadata in the database corresponds exactly to what was executed. Additionally,
        // we need to intercept shutdown signals that would not be caught in normal execution and perform this metadata
        // update
        Thread errorHandler = new Thread() {
            @Override
            public void run() {
                // Otherwise, we need to react to the fact that some error may have happened
                recoverFromError();
                try {
                    updateExtractorMetadata();
                }
                catch (SQLException e) {
                    exit("An error occurred when updating the metadata. The database may be severely compromised.", e);
                }
            }
        };
        
        // We default to an exit that needs to handle error and exceptions
        Runtime.getRuntime().addShutdownHook(errorHandler);
        
        try {
            run();
        }
        catch (Exception e) {
            System.err.println("An error has occurred. To prevent further problems, execution of the extractors "
                    + "has stopped. Please resolve the problem and try again. The state of the database may be "
                    + "compromised.");
            e.printStackTrace();
            recoverFromError();
        }
        finally {
            // Whether an exception occurs or not, we still update the _extractors and _extractors_realtime tables.
            try {
                updateExtractorMetadata();
            }
            catch (SQLException e) {
                exit("An error occurred when updating the metadata. The database may be severely compromised.", e);
            }
        }
        
        // At this point, no more error need to be handled, so remove the handler
        Runtime.getRuntime().removeShutdownHook(errorHandler);*/
    }
    
    
    private static void recoverFromError() {
        try {
            if (currentExtractor != null)
                currentExtractor.removeFromDatabase();
        }
        catch (SQLException e) {
            System.err.println("Unable to delete the incomplete information of the extractor!");
        }
    }
    
    
    private static void updateExtractorMetadata() throws SQLException {
        if (simulate)
            return;
        
        // At this point, the information on _extractors_realtime contains the extractors that executed successfully. We
        // also need to include the original executors that were not removed nor re-updated. We do this to maintain the
        // state of the database consistent.
        // As such, we copy some extractors from the _extractors table to the _extractors_realtime table.
        try (PreparedStatement statement = extractorsConnection.prepareStatement(""
                + "INSERT INTO _extractors_realtime (json, last_updated) "
                + "SELECT json, last_updated "
                + "FROM _extractors "
                + "WHERE id > ?")) {
            statement.setInt(1, originalNextIndex);
            statement.executeUpdate();
        }
        
        // We now need to transfer all information from _extractors_realtime into _extractors and erase the
        // _extractors_realtime table for future use
        try (Statement statement = extractorsConnection.createStatement()) {
            statement.executeUpdate("TRUNCATE _extractors");
            statement.executeUpdate(""
                    + "INSERT INTO _extractors (json, last_updated) "
                    + "SELECT json, last_updated "
                    + "FROM _extractors_realtime");
            statement.executeUpdate("TRUNCATE _extractors_realtime");
        }
    }
    
    
    /**
     * This method reads extractor specifications from the configuration file and converts them into actual extractors.
     * Alongside, it makes sure that dependencies are satisfied.
     * 
     * @throws JSONException
     * @throws InstantiationException
     * @throws DependencyException
     * @throws OwlSqlException
     */
    private static void prepareExtractors() throws InstantiationException, JSONException, DependencyException,
            OwlSqlException {
        for (ExtractorSpec<?> spec : Config.getExtractorSpecs()) {
            Extractor extractor = Extractor.createFromSpec(spec, extractorsConnection);
            
            // Add this newly created extractor to the list of extractors to be executed.
            configExtractors.add(extractor);
            
            // Find its dependencies. Notice that, per the documentation, a null result in this method is valid and
            // is equivalent to an empty array.
            Extractor[] itsDependencies = extractor.getDirectDependencies();
            if (itsDependencies == null)
                itsDependencies = new Extractor[0];
            dependencies.put(extractor, itsDependencies);
            
            classChangeTime.put(extractor, extractor.classChangedOn());
            
            preparedExtractors.add(extractor);
        }
    }
    
    
    public Application() {
        throw new RuntimeException("Cannot instantiate this class at runtime");
    }
    
    
    /**
     * Returns a list of the extractors that have already been prepared. This is only useful while the method
     * {@link #prepareExtractors()} is being run; otherwise, it will be equal to the list of all extractor to execute.
     */
    static ArrayList<Extractor> getPreparedExtractors() {
        return preparedExtractors;
    }
}
