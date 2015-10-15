package pt.owlsql.extractors.relations;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;

import pt.owlsql.Config;
import pt.owlsql.DependencyException;
import pt.owlsql.Extractor;
import pt.owlsql.extractors.SQLCoreUtils;


public final class ExistentialRelationsExtractor extends Extractor {
    
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
                    + "CREATE TABLE existential_relations ("
                    + "  start INT, "
                    + "  chain TEXT, "
                    + "  end INT, "
                    + "  INDEX (start), "
                    + "  INDEX (chain(256)), "
                    + "  INDEX (end)"
                    + ")");
        }
    }
    
    
    @Override
    public void removeFromDatabase() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            statement.execute("DROP TABLE existential_relations");
        }
    }
    
    
    @Override
    public void update() throws SQLException {
        try (PreparedStatement insertStatement = getConnection().prepareStatement("" //
                + "INSERT INTO existential_relations (start, chain, end) "
                + "VALUES (?, ?, ?)")) {
            
            getLogger().info("Finding the direct existential relations between pairs of concepts");
            
            // Start by getting a reference to all the classes and create an ExistentialUnfolder object
            HashSet<OWLClass> allClasses = utils.getAllEntities(EntityType.CLASS);
            ExistentialUnfolder unfolder = new ExistentialUnfolder();
            
            HashSet<OWLOntology> ontologies = Config.getOntologies();
            
            int counter = 0;
            for (OWLClass owlClass : allClasses) {
                Set<OWLClassExpression> superclasses = owlClass.getSuperClasses(ontologies);
                superclasses.addAll(owlClass.getEquivalentClasses(ontologies));
                
                RelationsStore allRelations = new RelationsStore();
                for (OWLClassExpression superclass : superclasses) {
                    if (superclass.isAnonymous())
                        allRelations.addAll(superclass.accept(unfolder));
                }
                
                int id = utils.getID(owlClass);
                insertStatement.setInt(1, id);
                for (Chain chain : allRelations) {
                    if (chain.propertiesLength() == 0)
                        continue;
                    
                    OWLObjectProperty[] properties = chain.getChain();
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < properties.length; i++) {
                        if (i > 0)
                            sb.append(",");
                        sb.append(utils.getID(properties[i]));
                    }
                    insertStatement.setString(2, sb.toString());
                    insertStatement.setInt(3, utils.getID(chain.getEndPoint()));
                    insertStatement.addBatch();
                }
                
                counter++;
                if (counter % 1000 == 0) {
                    getLogger().info("... existential relations for " + counter + " classes found ...");
                    insertStatement.executeBatch();
                }
            }
            
            getLogger().info("... existential relations for " + counter + " classes found ...");
            insertStatement.executeBatch();
        }
    }
    
    
    private PreparedStatement getRelationsStatement;
    
    
    @Override
    public void prepareForDependents() throws SQLException {
        getRelationsStatement = getConnection().prepareStatement("SELECT chain, end FROM existential_relations WHERE start = ?");
    }
    
    
    public RelationsStore getRelations(int id) throws SQLException {
        RelationsStore result = new RelationsStore();
        
        getRelationsStatement.setInt(1, id);
        try (ResultSet resultSet = getRelationsStatement.executeQuery()) {
            while (resultSet.next()) {
                String chainString = resultSet.getString(1);
                int endID = resultSet.getInt(2);
                
                String[] fields = chainString.split(",");
                OWLObjectProperty[] properties = new OWLObjectProperty[fields.length];
                for (int i = 0; i < properties.length; i++) {
                    properties[i] = utils.getEntity(Integer.parseInt(fields[i])).asOWLObjectProperty();
                }
                
                OWLClass endPoint = utils.getEntity(endID).asOWLClass();
                result.add(new Chain(properties, endPoint));
            }
        }
        
        return result;
    }
    
    
    public int[][] getInternalRelations(int id) throws SQLException {
        int[][] result;
        
        getRelationsStatement.setInt(1, id);
        try (ResultSet resultSet = getRelationsStatement.executeQuery()) {
            int amount = SQLCoreUtils.countRows(resultSet);
            result = new int[amount][];
            int row = 0;
            while (resultSet.next()) {
                String chainString = resultSet.getString(1);
                int endID = resultSet.getInt(2);
                
                String[] fields = chainString.split(",");
                int[] instances = new int[fields.length + 1];
                for (int i = 0; i < fields.length; i++) {
                    instances[i] = Integer.parseInt(fields[i]);
                }
                instances[instances.length - 1] = endID;
                result[row++] = instances;
            }
        }
        
        return result;
    }
}
