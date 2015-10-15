package pt.owlsql.extractors;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Hashtable;
import java.util.Map.Entry;

import org.semanticweb.owlapi.model.IRI;

import pt.json.JSONException;
import pt.owlsql.DependencyException;
import pt.owlsql.Extractor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


public final class AnnotationExtractor extends Extractor {
    
    private SQLCoreUtils utils;
    private Hashtable<String, BufferedReader> fileReaders = new Hashtable<>();
    
    
    @Override
    protected String[] getMandatoryOptions() {
        return new String[] { "corpora" };
    }
    
    
    @Override
    protected void processOption(String key, JsonElement element) throws JSONException {
        if (key.equals("corpora")) {
            if (!element.isJsonObject())
                throw new JSONException("must be a JSON object", "corpora");
            JsonObject object = element.getAsJsonObject();
            
            for (Entry<String, JsonElement> entry : object.entrySet()) {
                String corpus = entry.getKey();
                JsonElement inner = entry.getValue();
                
                if (corpus.length() > 256)
                    throw new JSONException("corpus name must have at most 256 characters", corpus);
                
                if (!inner.isJsonPrimitive() || !inner.getAsJsonPrimitive().isString())
                    throw new JSONException("must be a string", "corpora", corpus);
                try {
                    fileReaders.put(corpus, new BufferedReader(new FileReader(inner.getAsString())));
                }
                catch (FileNotFoundException e) {
                    throw new JSONException(e.getMessage(), e, "corpora", corpus);
                }
            }
        }
        
        else
            super.processOption(key, element);
    }
    
    @Override
    protected boolean mustUpdate(Timestamp lastUpdateTime) {
        // TODO This should return true if the corpora have changed since the last time the extractor was executed
        // For now, we are conservative and always assume htat an update is required.
        return true;
    }
    
    @Override
    public Extractor[] getDirectDependencies() throws DependencyException {
        utils = getDependency(SQLCoreUtils.class);
        return new Extractor[] { utils };
    }
    
    
    @Override
    public void prepareForFirstUse() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            statement.executeUpdate(""
                    + "CREATE TABLE IF NOT EXISTS annotations ("
                    + "  id INT PRIMARY KEY AUTO_INCREMENT,"
                    + "  entity VARCHAR(256) NOT NULL,"
                    + "  annotation INT NOT NULL,"
                    + "  corpus VARCHAR(256) NOT NULL,"
                    + "  INDEX (entity),"
                    + "  INDEX (annotation),"
                    + "  INDEX (corpus))");
        }
    }
    
    
    @Override
    public void update() throws SQLException {
        // We read the file line by line, split each line on whitespace into three parts and use each triplet to create
        // an annotation in the database.
        
        try (PreparedStatement insertAnnotation = getConnection().prepareStatement(""
                + "INSERT INTO annotations (entity, annotation, corpus) VALUES (?, ?, ?)")) {
            for (Entry<String, BufferedReader> entry : fileReaders.entrySet()) {
                extractCorpus(insertAnnotation, entry.getKey(), entry.getValue());
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    
    private void extractCorpus(PreparedStatement insertAnnotation, String corpus, BufferedReader fileReader)
            throws SQLException, IOException {
        insertAnnotation.setString(3, corpus);
        
        String line;
        int lineNum = 0;
        int counter = 0;
        
        while ((line = fileReader.readLine()) != null) {
            lineNum++;
            line = line.trim();
            if (line.length() == 0 || line.startsWith("#"))
                continue;
            
            String[] parts = line.split("\\s+", 3); // Ignore everything from the third column onwards!
            if (parts.length < 2) {
                getLogger().warning("Ignoring line "
                        + corpus
                        + ":"
                        + lineNum
                        + ": expecting 2 columns; got "
                        + parts.length);
                continue;
            }
            
            String entity = parts[0];
            if (entity.length() > 256) {
                getLogger().warning("Ignoring line "
                        + corpus
                        + ":"
                        + lineNum
                        + ": can only use entities whose name does not have more than 256 characters");
                continue;
            }
            
            int annotationID = utils.getID(factory.getOWLClass(IRI.create(parts[1])));
            if (annotationID == -1) {
                getLogger().warning("Ignoring line " + corpus + ":" + lineNum + ": unknown ontology term");
                continue;
            }
            
            insertAnnotation.setString(1, entity);
            insertAnnotation.setInt(2, annotationID);
            insertAnnotation.addBatch();
            
            counter++;
            if (counter % 1000 == 0) {
                getLogger().info("... " + counter + " annotations found ...");
                insertAnnotation.executeBatch();
            }
        }
        
        // Finally, insert the ones that remain in the final batch
        insertAnnotation.executeBatch();
    }
    
    
    @Override
    public void removeFromDatabase() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            statement.executeQuery("DROP TABLE annotations");
        }
    }
    
    
    private PreparedStatement getTransitiveAnnotations;
    
    
    @Override
    public void prepareForDependents() throws SQLException {
        getTransitiveAnnotations = getConnection().prepareStatement(""
                + "SELECT DISTINCT hierarchy.superclass "
                + "FROM (SELECT DISTINCT annotation "
                + "      FROM annotations"
                + "      WHERE entity = ?"
                + "     ) AS t,"
                + "JOIN hierarchy ON hierarchy.subclass = t.annotation");
    }
    
    
    public int[] getTransitiveAnnotationsID(String entity) throws SQLException {
        getTransitiveAnnotations.setString(1, entity);
        try (ResultSet resultSet = getTransitiveAnnotations.executeQuery()) {
            int[] result = new int[SQLCoreUtils.countRows(resultSet)];
            int i = 0;
            while (resultSet.next()) {
                result[i++] = resultSet.getInt(1);
            }
            return result;
        }
    }
}
