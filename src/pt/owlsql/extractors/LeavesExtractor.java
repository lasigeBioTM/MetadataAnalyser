package pt.owlsql.extractors;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import pt.owlsql.DependencyException;
import pt.owlsql.Extractor;


public final class LeavesExtractor extends Extractor {
    
    private SQLCoreUtils utils;
    
    
    @Override
    public Extractor[] getDirectDependencies() throws DependencyException {
        utils = getDependency(SQLCoreUtils.class);
        return new Extractor[] { utils };
    }
    
    
    @Override
    public void prepareForFirstUse() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            statement.executeUpdate("CREATE TABLE leaves (id INT PRIMARY KEY NOT NULL, UNIQUE (id))");
        }
    }
    
    
    @Override
    public void removeFromDatabase() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            statement.executeUpdate("DROP TABLE leaves");
        }
    }
    
    
    @Override
    public void update() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            statement.executeUpdate(""
                    + "INSERT INTO leaves (id) "
                    + "SELECT superclass "
                    + "FROM hierarchy "
                    + "GROUP BY superclass "
                    + "HAVING COUNT(*) = 1");
        }
        
    }
    
    
    private PreparedStatement isLeafStatement;
    private PreparedStatement getLeaves;
    private PreparedStatement getLeavesSize;
    private PreparedStatement getNumberOfLeaves;
    
    
    @Override
    public void prepareForDependents() throws SQLException {
        isLeafStatement = getConnection().prepareStatement("SELECT id FROM leaves WHERE id = ?");
        getLeaves = getConnection().prepareStatement(""
                + "SELECT subclass "
                + "FROM hierarchy "
                + "JOIN leaves ON leaves.id = subclass "
                + "WHERE superclass = ?");
        getLeavesSize = getConnection().prepareStatement(""
                + "SELECT COUNT(*) "
                + "FROM hierarchy "
                + "JOIN leaves ON leaves.id = subclass "
                + "WHERE superclass = ?");
        getNumberOfLeaves = getConnection().prepareStatement("SELECT COUNT(*) FROM leaves");
    }
    
    
    public int[] getLeafDescendants(int id) throws SQLException {
        getLeaves.setInt(1, id);
        try (ResultSet resultSet = getLeaves.executeQuery()) {
            int[] result = new int[SQLCoreUtils.countRows(resultSet)];
            int i = 0;
            while (resultSet.next()) {
                result[i++] = resultSet.getInt(1);
            }
            return result;
        }
    }
    
    
    public int getLeafDescendantsSize(int id) throws SQLException {
        getLeavesSize.setInt(1, id);
        try (ResultSet resultSet = getLeavesSize.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
    
    
    public int getNumberOfLeaves() throws SQLException {
        try (ResultSet resultSet = getNumberOfLeaves.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
    
    
    public boolean isLeaf(int id) throws SQLException {
        isLeafStatement.setInt(1, id);
        try (ResultSet resultSet = isLeafStatement.executeQuery()) {
            return resultSet.next();
        }
    }
    
}
