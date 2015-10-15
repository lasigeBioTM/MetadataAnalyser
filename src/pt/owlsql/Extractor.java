package pt.owlsql;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;

import pt.json.JSONException;
import pt.owlsql.extractors.SQLCoreUtils;

import com.google.gson.JsonElement;


/**
 * This abstract class is the core class fo OWLtoSQL. It serves as the hierarchy basis of all the classes that read from
 * the OWL ontologies and insert information from them in the underlying database.
 * <p>
 * <b>Implementation notes</b>:
 * <ul>
 * <li>All implementations of this class that are going to be called directly by the user must be marked as final;
 * <li>Implementation of this class need to define {@link #prepareForFirstUse()}, {@link #update()} and
 * {@link #removeFromDatabase()} (see their javadoc for more information);
 * <li>In the case of a change in table specifications (particularly if the name of a table changes between two versions
 * of the extractor), care must be taken to manually update the underlying database. For example, if an extractor
 * inserts information in a table named <tt>Infromation</tt> (<i>sic</i>) and this misspelling is corrected, it is
 * possible that the older table is never deleted from the database.
 * </ul>
 */
public abstract class Extractor {
    
    protected static final OWLDataFactory factory = OWLManager.getOWLDataFactory();
    
    
    public static <U extends Extractor> U createFromSpec(ExtractorSpec<U> spec, Connection connection)
            throws InstantiationException, JSONException {
        Class<U> cls = spec.getExtractorClass();
        Hashtable<String, JsonElement> parameters = spec.getParameters();
        
        U extractor;
        try {
            extractor = cls.newInstance();
            
            // For some weird reason, we can't access the "spec" field of a U variable (even though it is an instance of
            // Extractor). So we need to make this not so obvious cast.
            ((Extractor) extractor).spec = spec;
        }
        catch (IllegalAccessException e) {
            InstantiationException ex = new InstantiationException();
            ex.initCause(e);
            throw ex;
        }
        
        // Process its options
        HashSet<String> mandatoryParameters = new HashSet<>();
        for (String parameterName : extractor.getMandatoryOptions()) {
            mandatoryParameters.add(parameterName);
        }
        
        for (Entry<String, JsonElement> entry : parameters.entrySet()) {
            String key = entry.getKey();
            if (key.equals("class"))
                continue;
            extractor.processOption(key, entry.getValue());
            mandatoryParameters.remove(key);
        }
        
        if (mandatoryParameters.size() > 0) {
            StringBuilder sb = new StringBuilder();
            String[] params = mandatoryParameters.toArray(new String[mandatoryParameters.size()]);
            for (int i = 0; i < params.length - 1; i++) {
                sb.append(", \"").append(params[i]).append("\"");
            }
            
            String options;
            if (params.length == 1)
                options = "option \"" + params[0] + "\"";
            else
                options = "options " + sb.substring(2) + " and \"" + params[params.length - 1] + "\"";
            
            throw new InstantiationException("Failed to provide " + options + " to extractor " + cls.getName());
        }
        
        // As above, cast makes sure that the connection field is visible
        ((Extractor) extractor).connection = connection;
        return extractor;
    }
    
    
    private Connection connection;
    private ExtractorSpec<?> spec;
    private Logger logger;
    
    
    /**
     * Determines the last time this class definition has changed, so that it can be compared with the last time this
     * extractor was executed.
     * 
     * @return a {@link Timestamp} that corresponds to the last time this class definition was changed.
     * 
     * @throws OwlSqlException If the modification date of the .class file cannot be determined.
     */
    final Timestamp classChangedOn() throws OwlSqlException {
        ClassLoader loader = getClass().getClassLoader();
        String filename = getClass().getName().replace('.', '/') + ".class";
        
        // Get the corresponding class file as a Resource
        URL resource = (loader != null) ? loader.getResource(filename) : ClassLoader.getSystemResource(filename);
        @SuppressWarnings("hiding")
        URLConnection connection;
        try {
            connection = resource.openConnection();
        }
        catch (IOException e) {
            throw new OwlSqlException("Cannot find last modification date for " + getClass());
        }
        
        // Get the last modification date of this class file
        long time = connection.getLastModified();
        
        if (time == 0)
            throw new OwlSqlException("Cannot find last modification date for " + getClass());
        
        return new Timestamp(time);
    }
    
    
    /**
     * Returns the specification that resulted in the creation of this extractor.
     *
     * @return an {@link ExtractorSpec} object.
     */
    final ExtractorSpec<?> getSpec() {
        return spec;
    }
    
    
    /**
     * Returns the connection that is being used to communicate to the database. This connection is the same for all
     * extractors, so beware of thread-safety issues that may creep up. If thread safety is an issue, you sould instead
     * use {@link Config#connectToDatabase()}.
     * 
     * @return A connection to the underlying SQL database that is shared among all extractors
     */
    protected final Connection getConnection() {
        return connection;
    }
    
    
    /**
     * This method creates (or retrieves) an instance of an Extractor based on the class that was provided. This must be
     * an extractor that exists in the metadata of the database and that has already been processed. If exactly one
     * extractor instantiates the given class, it is returned; otherwise (zero or more than two instances), an
     * {@link InstantiationException} is thrown.
     * <p>
     * <b>Note:</b> if you want to retrieve an extractor based on parameters, use {@link #getDependency(DependencySpec)}.
     * 
     * @param cls The subclass of {@link Extractor} that describes the requested extractor.
     * 
     * @return An extractor that instantiates the given class that has already been executed.
     * 
     * @throws DependencyException If the dependency cannot be satisfied
     */
    protected final <U extends Extractor> U getDependency(Class<U> cls) throws DependencyException {
        for (Extractor extractor : Application.getPreparedExtractors()) {
            if (cls.isInstance(extractor))
                return cls.cast(extractor);
        }
        
        throw new DependencyException(this.spec, cls);
    }
    
    
    /**
     * This method returns the parameters that must be provided for this extractor to be created.
     */
    @SuppressWarnings("static-method")
    protected String[] getMandatoryOptions() {
        return new String[0];
    }
    
    
    /**
     * Returns a {@link Logger} that can be used to write messages in a log file.
     */
    protected Logger getLogger() {
        if (logger != null)
            return logger;
        
        return logger = Logger.getLogger(getClass().getName());
    }
    
    
    /**
     * Determines whether the extractor needs an update, even if the Executor determines that it does not. This method
     * is especially relevant in extractors that depend on external resources and that need to update if the external
     * resource has changed since the last update time.
     * <p>
     * Notice that even if this method returns <code>false</code>, the extractor may be updated, if the application
     * determines that it needs to.
     * 
     * @param lastUpdateTime The time this extractor was last executed.
     * 
     * @return <code>true</code> if the extractor needs a mandatory update; <code>false</code> otherwise.
     */
    @SuppressWarnings("static-method")
    protected boolean mustUpdate(Timestamp lastUpdateTime) {
        return false;
    }
    
    
    /**
     * This method may be overridden by implementations of this class so that parameters can be given to them.
     * Parameters are given with a string name and a {@link JsonElement} as a value, which the implementation then needs
     * to process.
     * 
     * @param key The name of the parameter
     * @param element The json element that contains the value of the parameter. Implementations must extract the actual
     *            value themselves.
     * 
     * @throws JSONException if some unexpected JSON exception occurs during the processing of the value.
     */
    protected void processOption(String key, JsonElement element) throws JSONException {
        throw new JSONException("unexpected parameter on " + getClass().getName(), key);
    }
    
    
    /**
     * This method returns an array containing the extractors this instance depends on. It should call only the
     * {@link #getDependency(Class)} and {@link #getDependency(DependencySpec)} methods, the result of which is
     * generally assigned to instance fields. It is also the only location of the the implementation that can call these
     * methods.
     * <p>
     * The resulting array defines the direct dependencies of the extractor. These are the extractors that must be
     * executed before this one, as they store information in the database that will be needed by this instance.
     * <p>
     * A <code>null</code> result is equivalent to an empty array.
     * <p>
     * <b>Note:</b> Implementations usually depend on {@link SQLCoreUtils} (although that is not required).
     * 
     * @throws DependencyException If one of the dependencies cannot be satisfied at the time this method is called.
     */
    public abstract Extractor[] getDirectDependencies() throws DependencyException;
    
    
    /**
     * When an extractor has dependents, the information they have stored in the database may be too difficult to access
     * with actual queries. In order to facilitate this retrieval of information based on an extractor, implementations
     * may provide their own methods (<i>e.g.</i> {@link SQLCoreUtils#getEntity(int)}). These extra methods may need
     * some initialization on an instance-basis, for example to prepare SQL statements for further use. This method is
     * responsible for those actions.
     * 
     * @throws SQLException This method can throw an <code>SQLException</code> if needed, to signal that the process was
     *             interrupted by an error in the SQL connection.
     */
    public void prepareForDependents() throws SQLException {}
    
    
    /**
     * This defines what happens when the extractor is first included in the database. Subsequent calls of this
     * extractor will not trigger this method to run. This is typically where tables are created or new columns are
     * inserted in already existing tables.
     * 
     * @throws SQLException This method can throw an <code>SQLException</code> if needed, to signal that the process was
     *             interrupted by an error in the SQL connection.
     */
    public abstract void prepareForFirstUse() throws SQLException;
    
    
    /**
     * This defines what happens when the information provided by this extractor is to be removed from the database.
     * Typically, this corresponds to removing the tables and/or columns that were initially inserted with the
     * {@link #prepareForFirstUse()} method.
     * <p>
     * <b>Implementation note:</b> This method must not depend on any other extractor.
     * 
     * @throws SQLException This method can throw an <code>SQLException</code> if needed, to signal that the process was
     *             interrupted by an error in the SQL connection.
     */
    public abstract void removeFromDatabase() throws SQLException;
    
    
    /**
     * This defines the actions to compute and store the information from the ontologies in the database. The set of
     * loaded ontologies can be retrieved with {@link Config#getOntologies()}; the connection to the database can be
     * retrieved with the protected method {@link #getConnection()}.
     * 
     * @throws SQLException This method can throw an <code>SQLException</code> if needed, to signal that the process was
     *             interrupted by an error in the SQL connection.
     */
    public abstract void update() throws SQLException;
}
