package pt.owlsql.extractors;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map.Entry;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import pt.json.JSONException;
import pt.owlsql.Config;
import pt.owlsql.DependencyException;
import pt.owlsql.Extractor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


public final class ExtendedHierarchyExtractor extends Extractor {
    
    private class ExtensionSpec {
        private boolean emulateReflexive;
        private boolean emulateNotReflexive;
        private boolean emulateTransitive;
        private boolean emulateNotTransitive;
        
        private boolean transitive;
        private boolean reflexive;
        private boolean subProperties = true;
        
        private final HashSet<OWLObjectProperty> properties = new HashSet<>();
        
        
        private void setPropertyProperties() {
            if (emulateReflexive)
                reflexive = true;
            else if (emulateNotReflexive)
                reflexive = false;
            else {
                reflexive = true;
                for (OWLObjectProperty property : properties) {
                    if (!property.isReflexive(Config.getOntologies())) {
                        reflexive = false;
                        break;
                    }
                }
            }
            
            if (emulateTransitive)
                transitive = true;
            else if (emulateNotTransitive)
                transitive = false;
            else {
                for (OWLObjectProperty property : properties) {
                    if (!property.isTransitive(Config.getOntologies())) {
                        reflexive = false;
                        break;
                    }
                }
            }
        }
        
        
        private void getSubProperties() {
            if (!subProperties)
                return;
            
            getLogger().info("finding the subproperties of ");
            for (OWLObjectProperty property : properties) {
                getLogger().info("- " + property.toStringID());
            }
            
            // Store the actual properties given by the user
            ArrayDeque<OWLObjectProperty> toAdd = new ArrayDeque<>(properties);
            
            // Clear the current properties (will add them iteratively alter
            properties.clear();
            
            while (toAdd.size() > 0) {
                OWLObjectProperty property = toAdd.pop();
                if (properties.contains(property))
                    continue;
                properties.add(property);
                getLogger().info(">>> " + property.toStringID());
                
                for (OWLObjectPropertyExpression propertyExpression : property.getSubProperties(Config.getOntologies())) {
                    if (!propertyExpression.isAnonymous())
                        toAdd.add(propertyExpression.asOWLObjectProperty());
                }
            }
        }
    }
    
    
    private Hashtable<String, ExtensionSpec> specs = new Hashtable<>();
    
    
    @Override
    protected String[] getMandatoryOptions() {
        return new String[] { "specs" };
    }
    
    
    @Override
    protected void processOption(String key, JsonElement element) throws JSONException {
        if (key.equals("specs")) {
            if (!element.isJsonObject())
                throw new JSONException("must be a JSON object", "specs");
            JsonObject object = element.getAsJsonObject();
            for (Entry<String, JsonElement> entry : object.entrySet()) {
                String identifier = entry.getKey();
                if (identifier.length() > 256)
                    throw new JSONException("Identifiers cannot have more than 256 characters", "specs", identifier);
                
                if (!entry.getValue().isJsonObject())
                    throw new JSONException("must be a JSON object", "specs", identifier);
                
                try {
                    ExtensionSpec spec = createSpec(entry.getValue().getAsJsonObject());
                    specs.put(identifier, spec);
                }
                catch (JSONException e) {
                    throw e.withPrefix("specs", identifier);
                }
                
            }
        }
        else
            super.processOption(key, element);
    }
    
    
    private ExtensionSpec createSpec(JsonObject object) throws JSONException {
        ExtensionSpec spec = new ExtensionSpec();
        
        for (Entry<String, JsonElement> entry : object.entrySet()) {
            String key = entry.getKey();
            JsonElement element = entry.getValue();
            
            if (key.equals("properties")) {
                if (!element.isJsonArray())
                    throw new JSONException("must be a list");
                JsonArray array = element.getAsJsonArray();
                for (int i = 0; i < array.size(); i++) {
                    JsonElement inner = array.get(i);
                    if (!inner.isJsonPrimitive() || !inner.getAsJsonPrimitive().isString())
                        throw new JSONException("must be a string", "[" + i + "]");
                    String string = inner.getAsString();
                    spec.properties.add(factory.getOWLObjectProperty(IRI.create(string)));
                }
            }
            else if (key.equals("emulate")) {
                if (!element.isJsonArray())
                    throw new JSONException("must be a list");
                JsonArray array = element.getAsJsonArray();
                
                for (int i = 0; i < array.size(); i++) {
                    JsonElement inner = array.get(i);
                    if (!inner.isJsonPrimitive() || !inner.getAsJsonPrimitive().isString())
                        throw new JSONException("must be a string", "[" + i + "]");
                    
                    String string = inner.getAsString();
                    if (string.equals("transitive")) {
                        if (spec.emulateNotTransitive)
                            throw new JSONException("\"transitive\" and \"not transitive\" are mutually exclusive");
                        spec.emulateTransitive = true;
                    }
                    else if (string.equals("not transitive")) {
                        if (spec.emulateTransitive)
                            throw new JSONException("\"transitive\" and \"not transitive\" are mutually exclusive");
                        spec.emulateNotTransitive = true;
                    }
                    else if (string.equals("reflexive")) {
                        if (spec.emulateNotReflexive)
                            throw new JSONException("\"reflexive\" and \"not reflexive\" are mutually exclusive");
                        spec.emulateReflexive = true;
                    }
                    else if (string.equals("not reflexive")) {
                        if (spec.emulateReflexive)
                            throw new JSONException("\"reflexive\" and \"not reflexive\" are mutually exclusive");
                        spec.emulateNotReflexive = true;
                    }
                    else
                        throw new JSONException("unknown emulation mode: \"" + string + "\"");
                }
            }
            else if (key.equals("subproperties")) {
                if (!element.isJsonPrimitive() && !element.getAsJsonPrimitive().isBoolean())
                    throw new JSONException("must be a boolean");
                spec.subProperties = element.getAsBoolean();
            }
            else
                throw new JSONException("unexpected parameter", key);
        }
        
        return spec;
    }
    
    
    private SQLCoreUtils utils;
    
    
    @Override
    public Extractor[] getDirectDependencies() throws DependencyException {
        utils = getDependency(SQLCoreUtils.class);
        return new Extractor[] { utils };
    }
    
    
    @Override
    public void prepareForFirstUse() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            statement.executeUpdate("CREATE TABLE extended_hierarchy ("
                    + "  extension VARCHAR(256) NOT NULL,"
                    + "  subclass INT NOT NULL,"
                    + "  superclass INT NOT NULL,"
                    + "  distance INT NOT NULL,"
                    + "  INDEX (extension),"
                    + "  INDEX (subclass),"
                    + "  INDEX (superclass),"
                    + "  INDEX (distance),"
                    + "  UNIQUE (extension, subclass, superclass))");
        }
    }
    
    
    @Override
    public void removeFromDatabase() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            statement.executeUpdate("DROP TABLE extended_hierarchy");
        }
    }
    
    
    @Override
    public void update() throws SQLException {
        for (Entry<String, ExtensionSpec> entry : specs.entrySet()) {
            String identifier = entry.getKey();
            ExtensionSpec spec = entry.getValue();
            
            spec.setPropertyProperties();
            spec.getSubProperties();
            
            // Let's insert the direct relations on the table
            // If Subclass(A ObjectSomeValuesFrom(P B)):
            // __ If A == B:
            // __ __ insert (A, B, 1)
            // __ Else:
            // __ __ insert (A, B, 0)
            try (PreparedStatement statement = getConnection().prepareStatement(""
                    + "INSERT IGNORE INTO extended_hierarchy (extension, subclass, superclass, distance) "
                    + "VALUES (?, ?, ?, ?)")) {
                statement.setString(1, identifier);
                
                int counter = 0;
                
                for (OWLOntology ontology : Config.getOntologies()) {
                    for (OWLSubClassOfAxiom axiom : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
                        if (axiom.getSubClass().isAnonymous())
                            continue;
                        if (axiom.getSuperClass() instanceof OWLObjectSomeValuesFrom) {
                            OWLObjectSomeValuesFrom restriction = (OWLObjectSomeValuesFrom) axiom.getSuperClass();
                            if (spec.properties.contains(restriction.getProperty())
                                    && restriction.getFiller() instanceof OWLClass) {
                                int id1 = utils.getID((OWLClass) axiom.getSubClass());
                                int id2 = utils.getID((OWLClass) restriction.getFiller());
                                int distance = id1 == id2 ? 0 : 1;
                                statement.setInt(2, id1);
                                statement.setInt(3, id2);
                                statement.setInt(4, distance);
                                statement.executeUpdate();
                                
                                counter++;
                                if (counter % 1000 == 0) {
                                    getLogger().info("... " + counter + " relations found ...");
                                }
                            }
                        }
                    }
                }
                
                getLogger().info("... " + counter + " direct relations found ...");
                statement.executeBatch();
            }
            
            if (spec.transitive) {
                getLogger().info("... closing the graph because this is a transitive relation");
                try (PreparedStatement stmt = getConnection().prepareStatement(""
                        + "INSERT IGNORE INTO extended_hierarchy (extension, subclass, superclass, distance) "
                        + "SELECT ?, e1.subclass, e2.superclass, e1.distance + 1 "
                        + "FROM extended_hierarchy AS e1, extended_hierarchy AS e2 "
                        + "WHERE e1.extension = ? AND e2.extension = ? AND "
                        + "      e1.superclass = e2.subclass AND"
                        + "      e1.distance = ? AND"
                        + "      e2.distance = 1")) {
                    stmt.setString(1, identifier);
                    stmt.setString(2, identifier);
                    stmt.setString(3, identifier);
                    
                    int distance = 1;
                    while (true) {
                        stmt.setInt(4, distance);
                        int newRows = stmt.executeUpdate();
                        getLogger().info("  " + newRows + " relations with distance = " + (distance + 1));
                        if (newRows == 0)
                            break;
                        distance++;
                    }
                }
            }
            
            
            if (spec.reflexive) {
                getLogger().info("... inserting reflexive relations because this is a reflexive relation");
                try (PreparedStatement stmt = getConnection().prepareStatement(""
                        + "INSERT INTO extended_hierarchy (extension, subclass, superclass, distance) "
                        + "SELECT ?, id, id, 0 "
                        + "FROM owl_objects "
                        + "WHERE type = 'Class' "
                        + "ON DUPLICATE KEY UPDATE distance = 0")) {
                    stmt.setString(1, identifier);
                    stmt.executeUpdate();
                }
            }
            
            
            getLogger().info("... closing everything based on class-subclass relations");
            try (PreparedStatement stmt = getConnection().prepareStatement(""
                    + "INSERT INTO extended_hierarchy (extension, subclass, superclass, distance) "
                    + "SELECT ?, h1.subclass, h2.superclass, h1.distance + h2.distance + e.distance "
                    + "FROM hierarchy AS h1, "
                    + "     hierarchy AS h2, "
                    + "     extended_hierarchy AS e "
                    + "WHERE h1.superclass = e.subclass AND "
                    + "      h2.subclass = e.superclass AND "
                    + "      e.extension = ? AND"
                    + "      h1.distance = ? "
                    + "ON DUPLICATE KEY UPDATE distance = LEAST(extended_hierarchy.distance, VALUES(distance))")) {
                stmt.setString(1, identifier);
                stmt.setString(2, identifier);
                
                // Add based on different distances in h1. This is a way to divide the huge insert statement into
                // smaller
                // chunks, and allows a certain amount of visualization either from the output or actually from the
                // state
                // of the database
                for (int distance = 0; /* Stop condition is inside the body */; distance++) {
                    stmt.setInt(3, distance);
                    int inserted = stmt.executeUpdate();
                    getLogger().info("... inserted " + inserted + " pairs");
                    if (inserted == 0)
                        break;
                }
            }
        }
    }
}
