package pt.owlsql.extractors;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import pt.owlsql.DependencyException;
import pt.owlsql.Extractor;


public final class ExtrinsicICExtractor extends Extractor {
    
    private SQLCoreUtils utils;
    
    
    @Override
    public Extractor[] getDirectDependencies() throws DependencyException {
        utils = getDependency(SQLCoreUtils.class);
        return new Extractor[] { utils, getDependency(AnnotationExtractor.class) };
    }
    
    
    @Override
    public void prepareForFirstUse() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            statement.executeUpdate("CREATE TABLE extrinsic_ic (id INT PRIMARY KEY NOT NULL, ic DOUBLE NOT NULL)");
        }
    }
    
    
    @Override
    public void update() throws SQLException {
        // Get the number of all annotated entities
        int nModels;
        try (Statement statement = getConnection().createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(DISTINCT entity) FROM annotations")) {
            resultSet.next();
            nModels = resultSet.getInt(1);
        }
        
        getLogger().info("Computing the extrinsic IC values for all concepts");
        
        try (PreparedStatement insertStatement = getConnection().prepareStatement(""
                + "INSERT INTO extrinsic_ic (class, ic) "
                + "SELECT superclass, 1 - LOG(COUNT(DISTINCT entity)) / LOG(?)"
                + "FROM annotations "
                + "JOIN hierarchy ON hierarchy.subclass = annotations.annotation "
                + "GROUP BY superclass")) {
            insertStatement.setInt(1, nModels);
            insertStatement.executeUpdate();
            insertStatement.close();
        }
    }
    
    
    @Override
    public void removeFromDatabase() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            statement.execute("DROP TABLE extrinsic_ic");
        }
    }
    
    
    private PreparedStatement getICStatement;
    
    
    @Override
    public void prepareForDependents() throws SQLException {
        getICStatement = getConnection().prepareStatement("SELECT ic FROM extrinsic_ic WHERE class = ?");
    }
    
    
    public double getIC(int id) throws SQLException {
        getICStatement.setInt(1, id);
        try (ResultSet resultsSet = getICStatement.executeQuery()) {
            return resultsSet.getDouble(1);
        }
    }
}
