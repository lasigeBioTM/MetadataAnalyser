package pt.owlsql;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import au.com.bytecode.opencsv.CSVWriter;
import pt.json.JSONException;
import pt.ma.downloader.object.BioontologyDownloader;
import pt.owlsql.extractors.SQLCoreUtils;


public final class Config {
    
    private static class Resolution {
        
        private HashSet<String> dependencies;
        private final ArrayList<String> parts;
        
        private final ArrayList<String> variableNames;
        
        
        private Resolution(ArrayList<String> parts, ArrayList<String> variableNames) {
            this.parts = parts;
            this.variableNames = variableNames;
        }
        
        
        public HashSet<String> getDependencies() {
            if (dependencies != null)
                return dependencies;
            
            dependencies = new HashSet<>();
            for (String variableName : variableNames) {
                if (variableName != null)
                    dependencies.add(variableName);
            }
            return dependencies;
        }
        
        
        public String resolve() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.size(); i++) {
                if (parts.get(i) != null)
                    sb.append(parts.get(i));
                else
                    sb.append(variables.get(variableNames.get(i)));
            }
            return sb.toString();
        }
    }
    
    
    public static final String CONFIG_FILE = "owlsql-config.json";
    
    private static JsonObject doc;
    
    private static final Hashtable<String, String> variables = new Hashtable<>();
    private static final ArrayList<URI> ontologiesURI = new ArrayList<>();
    private static HashSet<OWLOntology> ontologies;
    
    private static String hostname;
    private static String database;
    private static String username;
    private static String password;
    
    private static final ArrayList<ExtractorSpec<?>> extractorSpecs = new ArrayList<>();
    private static final ExtractorSpec<SQLCoreUtils> sqlCoreUtilsSpec = new ExtractorSpec<>(SQLCoreUtils.class);
    
    
    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        }
        catch (ClassNotFoundException e) {
            throw new Error(e);
        }
    }
    
    
    private static void detectVariableCycles() throws JSONException {
        // We need to make sure that cycles do not exist.
        Hashtable<String, HashSet<String>> dependsOn = new Hashtable<>();
        Hashtable<String, HashSet<String>> isDependencyOf = new Hashtable<>();
        
        // Initialize the isDependencyOf map.
        for (String varName : variables.keySet()) {
            isDependencyOf.put(varName, new HashSet<String>());
        }
        
        Hashtable<String, Resolution> resolutions = new Hashtable<>();
        for (Entry<String, String> entry : variables.entrySet()) {
            Resolution resolution;
            String varName = entry.getKey();
            
            try {
                resolution = getResolution(entry.getValue());
            }
            catch (JSONException e) {
                throw e.withPrefix("variables", varName);
            }
            if (resolution.getDependencies().contains(varName))
                throw new JSONException("Variable is recursive", "variables", varName);
            
            resolutions.put(varName, resolution);
            
            dependsOn.put(varName, resolution.getDependencies());
            for (String dependency : resolution.getDependencies()) {
                isDependencyOf.get(dependency).add(varName);
            }
        }
        
        // We now go through each variable name
        while (dependsOn.size() > 0) {
            // Find a variable that has no dependencies. Failure to do so means that there is a cycle
            String noDependencies = null;
            for (Entry<String, HashSet<String>> entry : dependsOn.entrySet()) {
                if (entry.getValue().size() == 0) {
                    noDependencies = entry.getKey();
                    break;
                }
            }
            
            if (noDependencies != null) {
                // This variable does not have any dependencies (or all of them can be resolved)
                // so it can also be resolved. Thus, it is the next one to be resolved
                variables.put(noDependencies, resolutions.get(noDependencies).resolve());
                
                // This key does not have any dependency. Remove it from our sight
                dependsOn.remove(noDependencies);
                for (String dependent : isDependencyOf.get(noDependencies)) {
                    dependsOn.get(dependent).remove(noDependencies);
                }
                continue;
            }
            
            // If we're here, then there is at least one cycle.
            // Furthermore, all the variables remaining are part of some cycle.
            // Find one and report it, just to be cute!
            
            List<String> cycle = new ArrayList<>();
            String varName = dependsOn.keySet().iterator().next(); // Get one of the remaining variable names (any one)
            int index = -1;
            while ((index = cycle.indexOf(varName)) == -1) {
                cycle.add(varName);
                varName = dependsOn.get(varName).iterator().next(); // Get any of the current variable's dependencies
            }
            cycle = cycle.subList(index, cycle.size()); // The actual cycle is between the first and last occurrences of
                                                        // the variable; this removes extra prefix variables
            StringBuilder sb = new StringBuilder();
            for (String string : cycle) {
                sb.append(" > ").append(string);
            }
            throw new JSONException("Cyclic variable dependency detected: " + sb.substring(3), "variables");
        }
    }
    
    
    static String extractString(JsonObject object, String key, boolean mandatory) throws JSONException {
        JsonElement element = object.get(key);
        if (element == null) {
            if (mandatory)
                throw new JSONException("must have a \"" + key + "\" element");
            else
                return null;
        }
        else if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
            throw new JSONException("must be a string", key);
        else {
            try {
                Resolution resolution = getResolution(element.getAsString());
                return resolution.resolve();
            }
            catch (JSONException e) {
                throw e.withPrefix(key);
            }
        }
    }
    
    
    private static Resolution getResolution(String string) throws JSONException {
        int index = 0;
        
        ArrayList<String> parts = new ArrayList<>();
        ArrayList<String> variableNames = new ArrayList<>();
        
        while (true) {
            // Find the first dollar-sign
            int dollarIndex = string.indexOf('$', index);
            
            // No dollar-sign? Append whatever is left as a part and return the resolution object
            if (dollarIndex == -1) {
                parts.add(string.substring(index));
                variableNames.add(null);
                return new Resolution(parts, variableNames);
            }
            
            // Is this followed by another dollar sign? If so, consume one of them and continue
            if (dollarIndex + 1 < string.length() && string.charAt(dollarIndex + 1) == '$') {
                parts.add(string.substring(index, dollarIndex + 1));
                variableNames.add(null);
                index = dollarIndex + 2;
                continue;
            }
            
            // We need to take care of the characters up to this point
            parts.add(string.substring(index, dollarIndex));
            variableNames.add(null);
            
            // So, this dollar-sign must be followed by { and then a valid variable name and then }
            if (dollarIndex == string.length() - 1 || string.charAt(dollarIndex + 1) != '{')
                throw new JSONException("Expecting '{' after the dollar sign");
            int endName = string.indexOf('}', dollarIndex + 2);
            if (endName == -1)
                throw new JSONException("'${' not followed by '}'");
            if (endName == dollarIndex + 2)
                throw new JSONException("Empty variable name '${}'");
            
            String possibleName = string.substring(dollarIndex + 2, endName);
            if (!isValidName(possibleName))
                throw new JSONException("Invalid variable name '" + possibleName + "'");
            
            parts.add(null);
            variableNames.add(possibleName);
            index = endName + 1;
        }
    }
    
    
    private static boolean isValidName(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (('0' <= c || c <= '9')
                    || ('a' <= c && c <= 'z')
                    || ('A' <= c && c <= 'Z')
                    || c == '_'
                    || c == '-'
                    || c == '.')
                continue;
            return false;
        }
        return true;
    }
    
    
    private static Class<? extends Extractor> getExtractorClass(String classname) throws JSONException {
        Class<?> cls;
        try {
            cls = Class.forName(classname);
        }
        catch (ClassNotFoundException e) {
            throw new JSONException("class not found");
        }
        validateClass(cls);
        
        return cls.asSubclass(Extractor.class);
    }
    
    
    private static ExtractorSpec<?> processExtractor(JsonObject object) throws JSONException {
        String classname = extractString(object, "class", true);
        Class<? extends Extractor> extractorClass;
        try {
            extractorClass = getExtractorClass(classname);
        }
        catch (JSONException e) {
            throw e.withPrefix("class");
        }
        
        object.remove("class"); // "class" should not be in the parameters
        Hashtable<String, JsonElement> parameters = new Hashtable<>();
        for (Entry<String, JsonElement> entry : object.entrySet()) {
            parameters.put(entry.getKey(), entry.getValue());
        }
        return new ExtractorSpec<>(extractorClass, parameters);
    }
    
    
    private static ExtractorSpec<?> processExtractor(String classname) throws JSONException {
        Class<? extends Extractor> extractorClass = getExtractorClass(classname);
        return new ExtractorSpec<>(extractorClass);
    }
    
    
    private static void processExtractors() throws JSONException {
        JsonElement element = doc.get("extractors");
        if (element == null)
            return;
        else if (!element.isJsonArray())
            throw new JSONException("must be a JSON array", "extractors");
        JsonArray extractorsArray = element.getAsJsonArray();
        
        HashSet<Class<?>> seenClasses = new HashSet<>();
        
        extractorSpecs.add(sqlCoreUtilsSpec);
        for (int i = 0; i < extractorsArray.size(); i++) {
            ExtractorSpec<?> spec;
            try {
                spec = createExtractorSpecFromJSON(extractorsArray.get(i));
            }
            catch (JSONException e) {
                throw e.withPrefix("extractors", "[" + i + "]");
            }
            if (spec.getExtractorClass() == SQLCoreUtils.class)
                throw new JSONException(SQLCoreUtils.class.getName() + " cannot be specified by the user.",
                        "extractors", "[" + i + "]");
            
            if (seenClasses.contains(spec.getClass()))
                throw new JSONException("Extractor classes must be unique", "extractors", "[" + i + "]");
            
            extractorSpecs.add(spec);
        }
    }
    
    
    private static void processOntologies() throws JSONException {
        JsonElement element = doc.get("ontologies");
        if (element == null)
            return;
        else if (!element.isJsonArray())
            throw new JSONException("must be a JSON array", "ontologies");
        JsonArray array = element.getAsJsonArray();
        
        for (int i = 0; i < array.size(); i++) {
            JsonElement inner = array.get(i);
            if (!inner.isJsonPrimitive() || !inner.getAsJsonPrimitive().isString())
                throw new JSONException("must be a string", "ontologies", "[" + i + "]");
            
            URI uri;
            try {
                uri = URI.create(resolve(inner.getAsString())).normalize();
            }
            catch (IllegalArgumentException e) {
                throw new JSONException("Malformed URL", e, "ontologies", "[" + i + "]");
            }
            catch (JSONException e) {
                throw e.withPrefix("ontologies", "[" + i + "]");
            }
            
            if (ontologiesURI.contains(uri))
                throw new JSONException("Duplicate ontology uri " + uri, "ontologies", "[" + i + "]");
            
            ontologiesURI.add(uri);
        }
    }

    /**
     * 
     * @param owlstartindex
     * @param owloffset
     * @throws JSONException
     */
    private static void processOntologiesFromURL(
    		int owlstartindex, 
    		int owloffset) 
    				throws JSONException {
    	
    	// reads JSON object from configuration file
        JsonElement element = doc.get("owlrepo");
        if (element == null) {
            return;
        } else if (!element.isJsonObject()) {
            throw new JSONException("must be a JSON object", "owlrepo");
        }
        
        // check object integrity
        JsonObject owlrepo = element.getAsJsonObject();
        if (!(owlrepo.has("baseurl") && owlrepo.has("apikey"))) {
        	throw new JSONException("must have a baseurl and apikey member", "owlrepo");
        }
        
        //
		BioontologyDownloader bInstance = new BioontologyDownloader();
		bInstance.setApiKey(owlrepo.get("apikey").getAsString());
		bInstance.setBaseURL(owlrepo.get("baseurl").getAsString());
				
		// 
		JSONArray ontologiesIRIs = bInstance.getOntologiesJSONArray(); 
		if (!(ontologiesIRIs != null && ontologiesIRIs.length() > 0)) {
			throw new JSONException("there aren't no ontologies available", "owlrepo");
		} 
		
		//
		String jsonKey = "ontologies";
		for (int index = owlstartindex; owloffset > 0 && index < ontologiesIRIs.length(); index++) {
			
			// read the actual URL address for this ontology
			JSONObject ontology = ontologiesIRIs.getJSONObject(index);
			String ontologyIRI = ontology.getString("download");
			if (ontologiesURI.contains(ontologyIRI)) {
                throw new JSONException("Duplicate ontology uri " + ontologyIRI, "ontologies", "[" + index + "]");
			}
			
			URI uri;
            try {
                uri = URI.create(resolve(ontologyIRI)).normalize();
            }
            catch (IllegalArgumentException e) {
                throw new JSONException("Malformed URL", e, "ontologies", "[" + index + "]");
            }
            catch (JSONException e) {
                throw e.withPrefix("ontologies", "[" + index + "]");
            }
            
            if (ontologiesURI.contains(uri))
                throw new JSONException("Duplicate ontology uri " + uri, "ontologies", "[" + index + "]");
            
            ontologiesURI.add(uri);
            
            //
            owloffset--;
		}
		
		System.out.println(ontologiesURI.size());
    }
    
    private static void processSQLParams() throws JSONException {
        JsonElement element = doc.get("mysql");
        if (element == null)
            throw new JSONException("object \"mysql\" is mandatory");
        else if (!element.isJsonObject())
            throw new JSONException("must be a JSON object", "mysql");
        JsonObject mysqlObject = element.getAsJsonObject();
        
        
        try {
            database = extractString(mysqlObject, "database", true);
            username = extractString(mysqlObject, "username", true);
            password = extractString(mysqlObject, "password", true);
            hostname = extractString(mysqlObject, "hostname", false);
        }
        catch (JSONException e) {
            throw e.withPrefix("mysql");
        }
        
        // The host can be null, in which case we will use the localhost
        if (hostname == null)
            hostname = "localhost";
    }
    
    
    private static void processVariables() throws JSONException {
        JsonElement element = doc.get("variables");
        if (element == null)
            return;
        else if (!element.isJsonObject())
            throw new JSONException("must be a JSON object", "variables");
        JsonObject object = element.getAsJsonObject();
        
        for (Entry<String, JsonElement> entry : object.entrySet()) {
            JsonElement value = entry.getValue();
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
                throw new JSONException("must be a string", "variables", entry.getKey());
            variables.put(entry.getKey(), value.getAsString());
        }
        
        detectVariableCycles();
    }

    /**
     * 
     * @param owlsource
     * @param owlstartindex
     * @param owloffset
     * @throws JSONException
     */
    private static void readDocument(
    		int owlsource, 
    		int owlstartindex, 
    		int owloffset) 
    				throws JSONException {
    	
        processVariables();
        processSQLParams();
        switch (owlsource) {
			case 1:
				processOntologiesFromURL(owlstartindex, owloffset);
				break;
	
			default:
				processOntologies();
				break;
		}
        processExtractors();
        
    }
    
    
    private static String resolve(String string) throws JSONException {
        return getResolution(string).resolve();
    }
    
    
    private static void validateClass(Class<?> cls) throws JSONException {
        // There are a few restrictions on the classes that are valid Executables implementations
        if (cls.equals(Extractor.class) || (!Extractor.class.isAssignableFrom(cls)))
            throw new JSONException("must extend " + Extractor.class.getName());
        
        if (cls.isAnonymousClass())
            throw new JSONException("must not be an anonymous class");
        if (cls.isInterface())
            throw new JSONException("must not be an interface");
        if (cls.isLocalClass())
            throw new JSONException("must not be a local class");
        if (cls.isMemberClass())
            throw new JSONException("must not be a member class");
        if (cls.isSynthetic())
            throw new JSONException("must not be a synthetic class");
        
        if (Modifier.isAbstract(cls.getModifiers()))
            throw new JSONException("must not be an abstract class");
        if (!Modifier.isFinal(cls.getModifiers()))
            throw new JSONException("must be a final class");
    }
    
    
    static Connection connectToDatabase() throws SQLException {
        // This will load the MySQL driver
        String uri = "jdbc:mysql://" + hostname + "/" + database + "?user=" + username + "&password=" + password;
        return DriverManager.getConnection(uri);
    }
    
    
    static Connection connectToSQLServer() throws SQLException {
        // This will load the MySQL driver
        String uri = "jdbc:mysql://" + hostname + "/?user=" + username + "&password=" + password;
        return DriverManager.getConnection(uri);
    }
    
    
    /**
     * This method takes a JSON element and tries to make an {@link ExtractorSpec} from it.
     * 
     * @param element
     * @return
     * @throws JSONException
     */
    static ExtractorSpec<?> createExtractorSpecFromJSON(JsonElement element) throws JSONException {
        ExtractorSpec<?> spec;
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            spec = processExtractor(resolve(element.getAsString()));
        }
        else if (element.isJsonObject()) {
            spec = processExtractor(element.getAsJsonObject());
        }
        else
            throw new JSONException("must be either a string or a JSON object");
        
        return spec;
    }
    
    
    static String getDatabase() {
        return database;
    }
    
    
    static ArrayList<ExtractorSpec<?>> getExtractorSpecs() {
        return extractorSpecs;
    }
    
    
    static String getHostname() {
        return hostname;
    }
    
    
    static HashSet<URI> getOntologiesURI() {
        return new HashSet<>(ontologiesURI);
    }
    
    
    public static HashSet<OWLOntology> getOntologies() {
        return new HashSet<>(ontologies);
    }
    
    
    static String getPassword() {
        return password;
    }
    
    
    static String getUsername() {
        return username;
    }
    
    
    /**
     * This method loads the ontologies specified in the configuration file. Notice that laoding only happens on demand,
     * rather than when the ontology is specified with the JSON "ontologies" element.
     * 
     * @return the set of loaded ontologies
     * 
     * @throws OwlSqlException If an anonymous opnotlogy is present in the list of ontologies; those are not supported
     *             by OWLtoSQL.
     * @throws OWLOntologyCreationException If there was a problem in creating and loading the ontology. See
     *             documentation for {@link OWLOntologyManager#loadOntologyFromOntologyDocument(IRI)}.
     * @throws IOException 
     */
    static HashSet<OWLOntology> loadOntologies() throws IOException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        
        // open writer streams, with append option
        CSVWriter owlLoaded = new CSVWriter(new FileWriter("owlloaded.csv", true ), ';');
        CSVWriter owlNotLoaded = new CSVWriter(new FileWriter("owlnotloaded.csv", true), ';');
        
        // blank lines
        owlLoaded.writeNext(" ".split(",")); owlNotLoaded.writeNext(" ".split(","));
        
        System.out.println("Loading " + ontologiesURI.size() + " ontologies ...");
        int count = 1; ontologies = new HashSet<>();
        for (URI uri : ontologiesURI) {

            IRI iri = IRI.create(uri);
            System.out.println("  #" + count++ + " " + uri);
            OWLOntology ontology = null; long startPoint = 0; long duration = 0;
			try {
				
				// parse the given ontology
				startPoint = System.currentTimeMillis();
				ontology = manager.loadOntology(iri);
	            if (ontology.isAnonymous()) {
	                throw new OwlSqlException("Anonymous ontologies are not compatible with OWLtoSQL.");
	            }
	            ontologies.add(ontology);
	            
	            // log a sucessfull ontologie entry
	            duration = System.currentTimeMillis() - startPoint;
	            String entry = uri + "#" + duration + "#" + ontology.getAxiomCount();
	            owlLoaded.writeNext(entry.split("#"));
	            
	            
			} catch (OWLOntologyCreationException e) {
				// log a unsucessfull ontologie entry
	            duration = System.currentTimeMillis() - startPoint;
	            String entry = uri + "#" + duration + "#"  + e.getMessage();
	            owlNotLoaded.writeNext(entry.split("#"));
	            
			} catch (OwlSqlException e) {
				// log a unsucessfull ontologie entry
	            duration = System.currentTimeMillis() - startPoint;
	            String entry = uri + "#" + duration + "#" + e.getMessage();
	            owlNotLoaded.writeNext(entry.split("#"));
				
			} catch (Exception e) {
				// log a unsucessfull ontologie entry
	            duration = System.currentTimeMillis() - startPoint;
	            String entry = uri + "#" + duration + "#" + e.getMessage();
	            owlNotLoaded.writeNext(entry.split("#"));			
			}
        }
        
        // close and write 
        owlLoaded.close(); owlNotLoaded.close();
        
        return ontologies;
    }

    /**
     * 
     * @param filename
     * @param owlsource
     * @param owlstartindex
     * @param owloffset
     * @throws IOException
     * @throws JSONException
     */
    static void read(
    		String filename, 
    		int owlsource, 
    		int owlstartindex, 
    		int owloffset) 
    				throws IOException, JSONException {
    	
    	//
        try (FileInputStream stream = new FileInputStream("configfiles/" + filename)) {
            JsonParser parser = new JsonParser();
            try (InputStreamReader reader = new InputStreamReader(stream)) {
                doc = parser.parse(reader).getAsJsonObject();
            }
        }
        
        readDocument(owlsource, owlstartindex, owloffset);
    }
    
    
    private Config() {
        throw new RuntimeException("Cannot instantiate this class");
    }
}
