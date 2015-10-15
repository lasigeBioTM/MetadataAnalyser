package pt.owlsql.extractors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;

import pt.owlsql.Config;
import pt.owlsql.Extractor;


/**
 * This extractor is responsible for the most basic extraction processes of information. It stores in the database:
 * <ul>
 * <li>all the entities mentioned in the ontologies (associated with an internal ID); and
 * <li>a many-to-many mapping between the entities and the ontologies where they are mentioned.
 * </ul>
 * Additionally, it creates an all-purpose key-value table that can be used by any further extractors.
 */
public final class SQLCoreUtils extends Extractor {
    
    /**
     * A mapping from OWL EntityType names to the actual EntityType object. This is needed because we store entities in
     * the database with a string reference to their type in the ontology
     */
    private static final Hashtable<String, EntityType<?>> nameToType = new Hashtable<>();
    
    static {
        // TODO If OWL-API changes the entity types, this will probably not make sense anymore.
        // To solve this, we should log the version of the OWL-API that was used to perform the previous
        // saved information and thrown an exception if the number changes.
        for (EntityType<?> type : EntityType.values()) {
            nameToType.put(type.getName(), type);
        }
    }
    
    
    /**
     * For a given OWL entity, sotre it in the database under a unique internal identifier and then associate it with
     * the ontology that is being processed. If the entity already exists on the database, use its unique id and do not
     * add a new one. This is an auxilliar method to {@link #update()}.
     * 
     * @param entity The {@link OWLEntity} being stored
     * @param done The map that contains, for each entity, its previously determined internal ID. If no internal ID has
     *            been calculated previously, no association will be present in this map.
     * @param newEntity The insert statement that inserts entities in the appropriate table
     * @param entityOntologyAssociation The insert statement that associated entities to their ontology. This statement
     *            must already contain the ontology ID in it.
     */
    private static void extractEntityID(OWLEntity entity, Hashtable<OWLEntity, Integer> done,
            PreparedStatement newEntity, PreparedStatement entityOntologyAssociation) throws SQLException {
        
        // Store (or retrieve) the internal ID of this entity to associate it with the current ontology
        int id;
        
        if (done.containsKey(entity))
            id = done.get(entity);
        
        else {
            newEntity.setString(1, entity.getEntityType().getName());
            newEntity.setString(2, entity.getIRI().toString());
            newEntity.executeUpdate(); // Insert this entity
            
            // Retrieve the id that was assigned to it and associate it in the `done` map
            try (ResultSet generatedKeys = newEntity.getGeneratedKeys()) {
                generatedKeys.next();
                id = generatedKeys.getInt(1);
            }
            done.put(entity, id);
        }
        
        entityOntologyAssociation.setInt(1, id);
        // The ontology id has already been included in this statement
        entityOntologyAssociation.executeUpdate(); // Add the association
    }
    
    
    @Override
    public Extractor[] getDirectDependencies() {
        return null;
    }
    
    
    @Override
    public void prepareForFirstUse() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            // Create the table that contains the IRI's of OWLEntities
            statement.executeUpdate(""
                    + "CREATE TABLE owl_objects ("
                    + "  id INT PRIMARY KEY AUTO_INCREMENT, "
                    + "  type VARCHAR(32) NOT NULL, "
                    + "  iri TEXT NOT NULL, "
                    + "  INDEX (type), "
                    + "  INDEX (iri(256))"
                    + ")");
            
            // Create the table that contains the ontology IRIs (both ontology and version IRIs)
            statement.executeUpdate(""
                    + "CREATE TABLE IF NOT EXISTS ontologies ("
                    + "  id INT PRIMARY KEY AUTO_INCREMENT, "
                    + "  ontology_iri TEXT NOT NULL,"
                    + "  version_iri TEXT NOT NULL"
                    + ")");
            
            // Create table to associate each object with a certain loaded ontology
            statement.executeUpdate(""
                    + "CREATE TABLE object_ontology ("
                    + "  object_id INT NOT NULL, "
                    + "  ontology_id INT NOT NULL, "
                    + "  INDEX (object_id), "
                    + "  INDEX (ontology_id)"
                    + ")");
            
            // Create the table for extra information
            statement.executeUpdate(""
                    + "CREATE TABLE extras ("
                    + "  tag VARCHAR(256) NOT NULL, "
                    + "  value TEXT, "
                    + "  UNIQUE (tag)"
                    + ")");
        }
    }
    
    
    @Override
    public void removeFromDatabase() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            statement.executeUpdate("DROP TABLE owl_objects");
            statement.executeUpdate("DROP TABLE ontologies");
            statement.executeUpdate("DROP TABLE object_ontology");
            statement.executeUpdate("DROP TABLE extras");
        }
    }
    
    
    @Override
    public void update() throws SQLException {
        // As a SingleUseExtractor, an update is equivalent to removing everything we have stored and starting from
        // scratch. We implement this by running the two methods removeFromDatabase() and prepareForFirstUse().
        removeFromDatabase();
        prepareForFirstUse();
        
        // We now extract the information from the ontologies
        HashSet<OWLOntology> ontologies = Config.getOntologies();
        
        // Create an internal ID for each ontology
        Hashtable<OWLOntology, Integer> ontologyInternalID = new Hashtable<>();
        try (PreparedStatement insert = getConnection().prepareStatement("INSERT INTO ontologies (ontology_iri, version_iri) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            for (OWLOntology ontology : ontologies) {
                OWLOntologyID ontologyID = ontology.getOntologyID();
                String ontologyIRIString = ontologyID.getOntologyIRI().toString();
                IRI versionIRI = ontologyID.getVersionIRI();
                String versionIRIString = versionIRI == null ? "" : versionIRI.toString();
                
                insert.setString(1, ontologyIRIString);
                insert.setString(2, versionIRIString);
                insert.executeUpdate();
                
                try (ResultSet generatedKeys = insert.getGeneratedKeys()) {
                    generatedKeys.next();
                    ontologyInternalID.put(ontology, generatedKeys.getInt(1));
                }
            }
        }
        
        // Now, let's find all the named entities of these ontologies and put them in the database
        try (PreparedStatement newEntity = getConnection().prepareStatement("INSERT INTO owl_objects (type, iri) VALUES (?, ?)",
                     Statement.RETURN_GENERATED_KEYS);
             PreparedStatement entityOntologyAssociation = getConnection().prepareStatement("INSERT INTO object_ontology (object_id, ontology_id) VALUES (?, ?)")) {
            
            Hashtable<OWLEntity, Integer> done = new Hashtable<>();
            
            for (OWLOntology ontology : ontologies) {
                Set<OWLEntity> entities = ontology.getSignature(true);
                entityOntologyAssociation.setInt(2, ontologyInternalID.get(ontology));
                for (OWLEntity entity : entities) {
                    extractEntityID(entity, done, newEntity, entityOntologyAssociation);
                }
                
                // Also add owl:Thing; it is present, even if only virtually, in all ontologies
                extractEntityID(factory.getOWLThing(), done, newEntity, entityOntologyAssociation);
            }
        }
    }
    
    private PreparedStatement entityToIndex;
    private PreparedStatement indexToEntity;
    private PreparedStatement entityToOntologyID;
    
    private PreparedStatement setExtra;
    private PreparedStatement getExtra;
    private PreparedStatement getEntities;
    private PreparedStatement getAllEntities;
    
    private Hashtable<Integer, OWLEntity> idToEntity;
    private Hashtable<OWLEntity, Integer> entityToID;
    
    
    @Override
    public void prepareForDependents() throws SQLException {
        Connection connection = getConnection();
        
        idToEntity = new Hashtable<>();
        entityToID = new Hashtable<>();
        
        entityToIndex = connection.prepareStatement(""
                + "SELECT id "
                + "FROM owl_objects "
                + "WHERE type = ? AND iri = ?");
        indexToEntity = connection.prepareStatement("SELECT type, iri FROM owl_objects WHERE id = ?");
        
        entityToOntologyID = connection.prepareStatement(""
                + "SELECT ontology_iri, version_iri "
                + "FROM owl_objects "
                + "JOIN ontologies ON ontologies.object_id = owl_objects.id "
                + "WHERE owl_objects.id = ?");
        
        setExtra = connection.prepareStatement("REPLACE INTO extras (tag, value) VALUES (?, ?)");
        getExtra = connection.prepareStatement("SELECT value FROM extras WHERE tag = ?");
        
        getAllEntities = connection.prepareStatement("SELECT id, type, iri FROM owl_objects");
        getEntities = connection.prepareStatement("SELECT id, iri FROM owl_objects WHERE type = ?");
    }
    
    
    /**
     * Retrieves all the entities stored in the database.
     * 
     * @return The set of OWL entities.
     */
    public HashSet<OWLEntity> getAllEntities() throws SQLException {
        HashSet<OWLEntity> result = new HashSet<>();
        try (ResultSet resultSet = getAllEntities.executeQuery()) {
            while (resultSet.next()) {
                EntityType<?> type = nameToType.get(resultSet.getString(2));
                result.add(factory.getOWLEntity(type, IRI.create(resultSet.getString(3))));
            }
        }
        return result;
    }
    
    
    /**
     * Retrieves an array of integers that correspond to the internal IDs of all the entities stored in the database.
     * 
     * @return An array containing the internal IDS of all entities.
     */
    public int[] getAllIds() throws SQLException {
        try (ResultSet resultSet = getAllEntities.executeQuery()) {
            int[] result = new int[countRows(resultSet)];
            int i = 0;
            while (resultSet.next()) {
                result[i++] = resultSet.getInt(1);
            }
            return result;
        }
    }
    
    
    /**
     * Retrieves all the entities stored in the database that are of a specified type.
     * 
     * @param type The type of entity to return
     * 
     * @return A set of OWL entities of the given type.
     */
    public <U extends OWLEntity> HashSet<U> getAllEntities(EntityType<U> type) throws SQLException {
        HashSet<U> result = new HashSet<>();
        getEntities.setString(1, type.getName());
        try (ResultSet resultSet = getEntities.executeQuery()) {
            while (resultSet.next()) {
                result.add(factory.getOWLEntity(type, IRI.create(resultSet.getString(2))));
            }
        }
        return result;
    }
    
    
    /**
     * Retrieves an array of integers that correspond to the internal IDs of all the entities stored in the database
     * that are of a specified type.
     * 
     * @param type The type of entity to return
     * 
     * @return An array containing the internal IDS of all entities of the given type.
     */
    public int[] getAllIds(EntityType<?> type) throws SQLException {
        getEntities.setString(1, type.getName());
        try (ResultSet resultSet = getEntities.executeQuery()) {
            int[] result = new int[countRows(resultSet)];
            int i = 0;
            while (resultSet.next()) {
                result[i++] = resultSet.getInt(1);
            }
            return result;
        }
    }
    
    
    /**
     * Retrieves the ontologies that mention the entity associated with the given internal ID.
     * 
     * @param entity The internal ID of the OWL entity being searched.
     * 
     * @return A set of {@link OWLOntologyID} objects describing the ontologies thta mention the given OWL entity.
     * 
     * @see #getDefiningOntologies(OWLEntity)
     */
    public Set<OWLOntologyID> getDefiningOntologies(int id) throws SQLException {
        Set<OWLOntologyID> result = new HashSet<>();
        entityToOntologyID.setInt(1, id);
        try (ResultSet resultSet = entityToOntologyID.executeQuery()) {
            while (resultSet.next()) {
                String ontologyString = resultSet.getString(1);
                IRI ontologyIRI = IRI.create(ontologyString);
                
                String versionString = resultSet.getString(2);
                IRI versionIRI = versionString.equals("") ? null : IRI.create(versionString);
                
                result.add(new OWLOntologyID(ontologyIRI, versionIRI));
            }
        }
        return result;
    }
    
    
    /**
     * Retrieves the ontologies that mention the given OWL entity. This is the same as
     * {@link #getDefiningOntologies(int)}, but the supplied information is the actual OWL entity, which is then
     * converted into its internal ID.
     * 
     * @param entity The OWL entity being searched.
     * 
     * @return A set of {@link OWLOntologyID} objects describing the ontologies thta mention the given OWL entity.
     */
    public Set<OWLOntologyID> getDefiningOntologies(OWLEntity entity) throws SQLException {
        return getDefiningOntologies(getID(entity));
    }
    
    
    /**
     * Returns the OWL entity that is associated with the given internal ID.
     * 
     * @param id The internal ID to convert to an OWL entity
     * 
     * @return The {@link OWLEntity} associated with the given id.
     */
    public OWLEntity getEntity(int id) throws SQLException {
        if (idToEntity.containsKey(id))
            return idToEntity.get(id);
        
        OWLEntity result = null;
        
        indexToEntity.setInt(1, id);
        try (ResultSet results = indexToEntity.executeQuery()) {
            if (results.next()) {
                String typeName = results.getString(1);
                String iri = results.getString(2);
                EntityType<?> type = nameToType.get(typeName);
                result = factory.getOWLEntity(type, IRI.create(iri));
            }
        }
        
        if (result != null)
            idToEntity.put(id, result);
        
        return result;
    }
    
    
    /**
     * Retrieves a value associated with a key on the database (see {@link #setExtra(String, String)}).
     * 
     * @param key The key. It must be a string with no more than 256 characters.
     * 
     * @throws IllegalArgumentException if the supplied key is longer than 256 characters.
     */
    public String getExtra(String key) throws SQLException {
        if (key.length() > 256)
            throw new IllegalArgumentException(String.format("Supplied key is longer than 256 characters"));
        
        getExtra.setString(1, key);
        
        try (ResultSet resultSet = getExtra.executeQuery()) {
            if (resultSet.next())
                return resultSet.getString(1);
        }
        
        return null;
    }
    
    
    /**
     * Returns the internal ID that is associated with the given OWL entity.
     * 
     * @param id The OWL entity to convert to an ID
     * 
     * @return The unique id associated with the given OWL entity.
     */
    public int getID(OWLEntity entity) throws SQLException {
        if (entityToID.containsKey(entity))
            return entityToID.get(entity);
        
        int result = -1;
        
        entityToIndex.setString(1, entity.getEntityType().getName());
        entityToIndex.setString(2, entity.getIRI().toString());
        
        try (ResultSet results = entityToIndex.executeQuery()) {
            if (results.next())
                result = results.getInt(1);
        }
        
        entityToID.put(entity, result);
        return result;
    }
    
    
    /**
     * Associates a key to a value and stores that information on the database for later retrieval with the
     * {@link #getExtra(String)} method.
     * 
     * @param key The key. It must be a string with no more than 256 characters.
     * @param value The value. There is no limit on the amount of characters for the value.
     * 
     * @throws IllegalArgumentException if the supplied key is longer than 256 characters.
     */
    public void setExtra(String key, String value) throws SQLException {
        if (key.length() > 256)
            throw new IllegalArgumentException(String.format("Supplied key is longer than 256 characters"));
        
        setExtra.setString(1, key);
        setExtra.setString(2, value);
        setExtra.executeUpdate();
    }
    
    
    /**
     * Counts the number of rows in a {@link ResultSet}. This method implies moving the pointer of the result set around
     * (which means that it is not thread safe). Additionally, the pointer is moved back to its initial position at the
     * end of the method. However, this repositioning may fail, changing the state of the set.
     * 
     * @param resultSet The {@link ResultSet} whose rows will be counted.
     * 
     * @return The number of rows in the given result set.
     * 
     * @throws SQLException if a database access error occurs or this method is called on a closed result set
     * @throws SQLFeatureNotSupportedException if the JDBC driver does not support {@link ResultSet#getRow()},
     *             {@link ResultSet#last()} or {@link ResultSet#absolute(int)}.
     */
    public static int countRows(ResultSet resultSet) throws SQLException {
        int current = resultSet.getRow();
        resultSet.last();
        // Note: the previous lines will throw a SQLFeatureNotSupportedException if the driver does not allow jumping
        // around. If it fails, the state of the set is not changed. However, if the next lines fail, we need to restore
        // the initial state, which we do in the finally block
        
        try {
            return resultSet.getRow();
        }
        finally {
            // No matter what happens, return to initial state
            if (current == 0)
                resultSet.beforeFirst();
            else
                resultSet.absolute(current);
        }
    }
}
