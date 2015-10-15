package pt.owlsql.extractors.relations;

import java.sql.SQLException;
import java.sql.Statement;

import org.semanticweb.owlapi.model.EntityType;

import pt.owlsql.DependencyException;
import pt.owlsql.Extractor;
import pt.owlsql.extractors.SQLCoreUtils;


public class NeighborhoodExtractor extends Extractor {
    
    private SQLCoreUtils utils;
    private RelationsExtractor relations;
    
    
    @Override
    public Extractor[] getDirectDependencies() throws DependencyException {
        utils = getDependency(SQLCoreUtils.class);
        relations = getDependency(RelationsExtractor.class);
        return new Extractor[] { utils, relations };
    }
    
    
    @Override
    public void prepareForFirstUse() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            statement.executeUpdate(""
                    + "CREATE TABLE neighborhood ("
                    + "  start INT, "
                    + "  chain TEXT, "
                    + "  end INT, "
                    + "  distance INT, "
                    + "  INDEX (start), "
                    + "  INDEX (chain(256)), "
                    + "  INDEX (end), "
                    + "  INDEX (distance)"
                    + ")");
        }
    }
    
    
    @Override
    public void removeFromDatabase() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            statement.execute("DROP TABLE neighborhood");
        }
    }
    
    
    @Override
    public void update() throws SQLException {
        int[] allIds = utils.getAllIds(EntityType.CLASS);
        
        for (int id : allIds) {
            extractWith(id);
        }
        
        // And now update the distance column
        try (Statement statement = getConnection().createStatement()) {
            statement.executeUpdate(""
                    + "UPDATE neighborhood "
                    + "SET distance = "
                    + "  LENGTH(chain) - LENGTH(REPLACE(chain), ',', '')) + 1");
        }
    }
    
    
    private void extractWith(int id) throws SQLException {
        int[][] store = relations.getInternalRelations(id);
        int[] distance = new int[store.length];
        for (int i = 0; i < store.length; i++) {
            distance[i] = store[i].length;
        }
    }
}
