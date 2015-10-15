package pt.owlsql.extractors;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;

import pt.json.JSONException;
import pt.owlsql.Config;
import pt.owlsql.DependencyException;
import pt.owlsql.Extractor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;


public final class NamesExtractor extends Extractor {
    
    private SQLCoreUtils utils;
    private final ArrayList<OWLAnnotationProperty> properties = new ArrayList<>();
    
    
    @Override
    public Extractor[] getDirectDependencies() throws DependencyException {
        utils = getDependency(SQLCoreUtils.class);
        return new Extractor[] { utils };
    }
    
    
    @Override
    public void prepareForFirstUse() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            statement.executeUpdate(""
                    + "CREATE TABLE names ("
                    + "  id INT NOT NULL, "
                    + "  property INT NOT NULL, "
                    + "  priority INT NOT NULL, "
                    + "  name TEXT NOT NULL, "
                    + "  INDEX (id), "
                    + "  INDEX (name(64)), "
                    + "  UNIQUE (id, priority)"
                    + ")");
        }
    }
    
    
    @Override
    protected String[] getMandatoryOptions() {
        return new String[] { "properties" };
    }
    
    
    @Override
    protected void processOption(String key, JsonElement element) throws JSONException {
        if (key.equals("properties")) {
            if (!element.isJsonArray())
                throw new JSONException("must be a list", "properties");
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                JsonElement inner = array.get(i);
                if (!inner.isJsonPrimitive() || !inner.getAsJsonPrimitive().isString())
                    throw new JSONException("must be a string", "properties", "[" + i + "]");
                String string = inner.getAsString();
                properties.add(factory.getOWLAnnotationProperty(IRI.create(string)));
            }
            
            if (properties.isEmpty())
                throw new JSONException("Needs at least one property", "properties");
        }
        else {
            super.processOption(key, element);
        }
    }
    
    
    @Override
    public void removeFromDatabase() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            statement.executeUpdate("DROP TABLE names");
        }
    }
    
    
    @Override
    public void update() throws SQLException {
        try (PreparedStatement getPriority = getConnection().prepareStatement(""
                     + "SELECT MAX(priority) FROM names WHERE id = ?");
             PreparedStatement insertName = getConnection().prepareStatement(""
                     + "INSERT INTO names (id, property, priority, name) VALUES (?, ?, ?, ?)");) {
            int counter = 0;
            for (OWLOntology ontology : Config.getOntologies()) {
                for (OWLEntity owlEntity : ontology.getSignature(true)) {
                    int id = utils.getID(owlEntity);
                    for (OWLAnnotationProperty property : properties) {
                        int propertyID = utils.getID(property);
                        Set<OWLAnnotation> annotations = owlEntity.getAnnotations(ontology, property);
                        for (OWLOntology closure : ontology.getImportsClosure()) {
                            annotations.addAll(owlEntity.getAnnotations(closure, property));
                        }
                        for (OWLAnnotation annotation : annotations) {
                            OWLAnnotationValue value = annotation.getValue();
                            if (value instanceof OWLLiteral) {
                                OWLLiteral literal = (OWLLiteral) value;
                                String name = literal.getLiteral();
                                
                                int priority;
                                getPriority.setInt(1, id);
                                try (ResultSet resultSet = getPriority.executeQuery()) {
                                    resultSet.next();
                                    priority = resultSet.getInt(1) + 1;
                                }
                                
                                insertName.setInt(1, id);
                                insertName.setInt(2, propertyID);
                                insertName.setInt(3, priority);
                                insertName.setString(4, name);
                                insertName.executeUpdate();
                                
                                counter++;
                                if (counter % 1000 == 0) {
                                    getLogger().info("... " + counter + " names found ...");
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private PreparedStatement getAllNames;
    private PreparedStatement getAllNamesOnProperty;
    private PreparedStatement getOneName;
    private PreparedStatement getOneNameOnProperty;
    
    
    @Override
    public void prepareForDependents() throws SQLException {
        getAllNames = getConnection().prepareStatement("SELECT name FROM names WHERE id = ?");
        getAllNamesOnProperty = getConnection().prepareStatement(""
                + "SELECT name "
                + "FROM names "
                + "WHERE id = ? AND property = ?");
        getOneName = getConnection().prepareStatement(""
                + "SELECT name "
                + "FROM names "
                + "WHERE id = ? "
                + "ORDER BY priority LIMIT 1");
        getOneNameOnProperty = getConnection().prepareStatement(""
                + "SELECT name "
                + "FROM names "
                + "WHERE id = ? AND property = ? "
                + "ORDER BY priority LIMIT 1");
    }
    
    
    public HashSet<String> getAllNames(int id) throws SQLException {
        HashSet<String> result = new HashSet<>();
        
        getAllNames.setInt(1, id);
        try (ResultSet resultSet = getAllNames.executeQuery()) {
            while (resultSet.next()) {
                result.add(resultSet.getString(1));
            }
        }
        return result;
    }
    
    
    public HashSet<String> getAllNamesOnProperty(int id, int propertyID) throws SQLException {
        HashSet<String> result = new HashSet<>();
        
        getAllNamesOnProperty.setInt(1, id);
        getAllNamesOnProperty.setInt(2, propertyID);
        try (ResultSet resultSet = getAllNamesOnProperty.executeQuery()) {
            while (resultSet.next()) {
                result.add(resultSet.getString(1));
            }
        }
        return result;
    }
    
    
    public String getMainName(int id) throws SQLException {
        getOneName.setInt(1, id);
        try (ResultSet resultSet = getOneName.executeQuery()) {
            if (resultSet.next())
                return resultSet.getString(1);
        }
        return null;
    }
    
    
    public String getMainNameOnProperty(int id, int propertyID) throws SQLException {
        getOneNameOnProperty.setInt(1, id);
        getOneNameOnProperty.setInt(2, propertyID);
        try (ResultSet resultSet = getOneName.executeQuery()) {
            if (resultSet.next())
                return resultSet.getString(1);
        }
        return null;
    }
}
