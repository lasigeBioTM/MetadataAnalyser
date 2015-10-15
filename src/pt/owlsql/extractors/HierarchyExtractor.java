package pt.owlsql.extractors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import pt.owlsql.Config;
import pt.owlsql.DependencyException;
import pt.owlsql.Extractor;


/**
 * This extractor retrieves the class-subclass axioms form the ontologies and builds a full hierarchy of concepts. It is
 * both single-use and constant.
 */
public final class HierarchyExtractor extends Extractor {
    
    private SQLCoreUtils utils;
    
    
    @Override
    public Extractor[] getDirectDependencies() throws DependencyException {
        utils = getDependency(SQLCoreUtils.class);
        return new Extractor[] { utils };
    }
    
    
    @Override
    public void prepareForFirstUse() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            statement.executeUpdate(""
                    + "CREATE TABLE hierarchy ("
                    + "  subclass INT NOT NULL, "
                    + "  superclass INT NOT NULL, "
                    + "  distance INT NOT NULL, "
                    + "  INDEX (subclass), "
                    + "  INDEX (superclass), "
                    + "  INDEX (distance), "
                    + "  UNIQUE (subclass, superclass)"
                    + ")");
        }
    }
    
    
    @Override
    public void removeFromDatabase() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            statement.executeUpdate("DROP TABLE hierarchy");
        }
    }
    
    
    @Override
    public void update() throws SQLException {
        // As a SingleUseExtractor, an update is equivalent to removing everything we have stored and starting from
        // scratch. We implement this by running the two methods removeFromDatabase() and prepareForFirstUse().
        removeFromDatabase();
        prepareForFirstUse();
        
        try (PreparedStatement insertStatement = getConnection()
                .prepareStatement("" + "INSERT IGNORE INTO hierarchy (subclass, superclass, distance) VALUES (?, ?, ?)")) {
            
            getLogger().info("Finding all the classes");
            
            // Start by getting a reference to all the classes
            HashSet<OWLClass> noSuperclass = utils.getAllEntities(EntityType.CLASS);
            
            // And by saying that everything is a subclass of itself (with distance 0)
            insertStatement.setInt(3, 0); // The distance will be 0 here
            for (OWLClass owlClass : noSuperclass) {
                int id = utils.getID(owlClass);
                insertStatement.setInt(1, id);
                insertStatement.setInt(2, id);
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
            
            // We now go through each subclassOf axiom and populate the first part of the hierarchy
            getLogger().info("Finding direct class-subclass relations");
            int counter = 0;
            
            insertStatement.setInt(3, 1); // The distance will be 1 for all direct axioms
            for (OWLOntology ontology : Config.getOntologies()) {
                Set<OWLSubClassOfAxiom> axioms = ontology.getAxioms(AxiomType.SUBCLASS_OF, true);
                for (OWLSubClassOfAxiom axiom : axioms) {
                    OWLClassExpression subClass = axiom.getSubClass();
                    OWLClassExpression superClass = axiom.getSuperClass();
                    
                    if (subClass.isAnonymous() || superClass.isAnonymous())
                        continue;
                    
                    OWLClass subOWLClass = subClass.asOWLClass();
                    OWLClass superOWLClass = superClass.asOWLClass();
                    counter++;
                    
                    // We found a class with a superclass, so remove it from the set of noSuperclass classes
                    noSuperclass.remove(subOWLClass);
                    
                    // Add this information to the database
                    int subClassID = utils.getID(subOWLClass);
                    int superClassID = utils.getID(superOWLClass);
                    insertStatement.setInt(1, subClassID);
                    insertStatement.setInt(2, superClassID);
                    insertStatement.addBatch();
                    
                    if (counter % 1000 == 0) {
                        getLogger().info("... found " + counter + " direct relations by now ...");
                    }
                }
            }
            
            // Those classes that do not have a superclass should be now processed so that owl:Thing is their superclass
            // This includes owl:Thing, which is part of the noSuperclass set
            insertStatement.setInt(2, utils.getID(factory.getOWLThing())); // Set the superclass to owlThing
            insertStatement.setInt(3, 1); // Set the distance to 1
            for (OWLClass owlClass : noSuperclass) {
                // Store this information on memory
                counter++;
                
                // Add this information to the database
                int subClassID = utils.getID(owlClass);
                insertStatement.setInt(1, subClassID);
                insertStatement.addBatch();
            }
            getLogger().info(counter + " direct relations");
            
            insertStatement.executeBatch();
        }
        
        try (PreparedStatement newInsertDistance = getConnection()
                .prepareStatement(""
                                          + "INSERT IGNORE INTO hierarchy (subclass, superclass, distance)"
                                          + "  SELECT h1.subclass, h2.superclass, h1.distance + 1"
                                          + "  FROM hierarchy AS h1, hierarchy AS h2"
                                          + "  WHERE h1.superclass = h2.subclass AND"
                                          + "        h1.distance = ? AND"
                                          + "        h2.distance = 1")) {
            
            int distance = 1;
            while (true) {
                newInsertDistance.setInt(1, distance);
                int newRows = newInsertDistance.executeUpdate();
                getLogger().info(newRows + " relations with distance = " + (distance + 1));
                if (newRows == 0)
                    break;
                distance++;
            }
            
        }
    }
    
    
    private PreparedStatement getMaxDepthStatement;
    private PreparedStatement selectAncestrySizeStatement;
    private PreparedStatement selectAncestryStatement;
    private PreparedStatement selectDescendantsSizeStatement;
    private PreparedStatement selectDescendantsStatement;
    private PreparedStatement getDepthStatement;
    
    
    @Override
    public void prepareForDependents() throws SQLException {
        Connection connection = getConnection();
        
        selectAncestryStatement = connection.prepareStatement(""
                + "SELECT superclass "
                + "FROM hierarchy "
                + "WHERE subclass = ?");
        selectAncestrySizeStatement = connection.prepareStatement(""
                + "SELECT COUNT(*) "
                + "FROM hierarchy "
                + "WHERE subclass = ?");
        selectDescendantsStatement = connection.prepareStatement(""
                + "SELECT subclass "
                + "FROM hierarchy "
                + "WHERE superclass = ?");
        selectDescendantsSizeStatement = connection.prepareStatement(""
                + "SELECT COUNT(*) "
                + "FROM hierarchy "
                + "WHERE superclass = ?");
        getMaxDepthStatement = connection.prepareStatement("SELECT MAX(distance) FROM hierarchy");
        getDepthStatement = connection.prepareStatement("SELECT MAX(distance) FROM hierarchy WHERE subclass = ?");
    }
    
    
    public int getDepth(int id) throws SQLException {
        getDepthStatement.setInt(1, id);
        try (ResultSet resultSet = getDepthStatement.executeQuery()) {
            if (resultSet.next())
                return resultSet.getInt(1);
        }
        return -1;
    }
    
    
    public int getMaxDepth() throws SQLException {
        try (ResultSet resultSet = getMaxDepthStatement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
    
    
    public int getNumberOfSubclasses(int id) throws SQLException {
        selectDescendantsSizeStatement.setInt(1, id);
        try (ResultSet resultSet = selectDescendantsSizeStatement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
    
    
    public int getNumberOfSuperclasses(int id) throws SQLException {
        selectAncestrySizeStatement.setInt(1, id);
        try (ResultSet resultSet = selectAncestrySizeStatement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
    
    
    public int[] getSubclasses(int id) throws SQLException {
        selectDescendantsStatement.setInt(1, id);
        try (ResultSet resultSet = selectDescendantsStatement.executeQuery()) {
            int i = 0;
            int[] result = new int[SQLCoreUtils.countRows(resultSet)];
            while (resultSet.next()) {
                result[i++] = resultSet.getInt(1);
            }
            return result;
        }
    }
    
    
    public int[] getSuperclasses(int id) throws SQLException {
        selectAncestryStatement.setInt(1, id);
        try (ResultSet resultSet = selectAncestryStatement.executeQuery()) {
            int i = 0;
            int amount = SQLCoreUtils.countRows(resultSet);
            int[] result = new int[amount];
            while (resultSet.next()) {
                result[i++] = resultSet.getInt(1);
            }
            return result;
        }
        
    }
}
