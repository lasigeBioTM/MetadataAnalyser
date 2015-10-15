package pt.owlsql.extractors;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;

import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.OWLClass;

import pt.json.JSONException;
import pt.owlsql.DependencyException;
import pt.owlsql.Extractor;

import com.google.gson.JsonElement;


public final class RelevanceExtractor extends Extractor {
    
    private SQLCoreUtils utils;
    private LeavesExtractor leaves;
    private IntrinsicICExtractor intrinsicIC;
    private ExtrinsicICExtractor extrinsicIC;
    
    private boolean useIC;
    
    
    @Override
    protected void processOption(String key, JsonElement element) throws JSONException {
        if (key.equals("use_ic")) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean())
                throw new JSONException("must be a boolean", "use_ic");
            useIC = element.getAsBoolean();
        }
        else
            super.processOption(key, element);
    }
    
    
    @Override
    public Extractor[] getDirectDependencies() throws DependencyException {
        utils = getDependency(SQLCoreUtils.class);
        leaves = getDependency(LeavesExtractor.class);
        
        if (useIC) {
            intrinsicIC = getDependency(IntrinsicICExtractor.class);
            extrinsicIC = getDependency(ExtrinsicICExtractor.class);
            
            return new Extractor[] { utils, leaves, intrinsicIC, extrinsicIC };
        }
        
        else
            return new Extractor[] { utils, leaves };
    }
    
    private PreparedStatement getChildrenStatement;
    private PreparedStatement countChildrenStatement;
    private PreparedStatement getICStatement;
    
    private int totalLeaves;
    
    private int nChildren;
    private int nChildrenAdjusted;
    private int hIndex;
    private int hIndexAdjusted;
    private double ratioLeaves;
    private double ratioExternalSeco;
    private double ratioExternalZhou;
    private double ratioExternalSanchez;
    private double ratioExternalLeaves;
    
    
    private void calculate(int id) throws SQLException {
        int[] children = getChildren(id);
        
        nChildren = nChildrenAdjusted = children.length;
        if (leaves.isLeaf(id))
            nChildrenAdjusted = Integer.MAX_VALUE;
        getHIndex(children);
        
        int nLeaves = leaves.getLeafDescendantsSize(id);
        ratioLeaves = (double) nLeaves / totalLeaves;
        
        if (useIC) {
            double extrinsicICValue = extrinsicIC.getIC(id);
            
            getICStatement.setInt(1, id);
            try (ResultSet resultSet = getICStatement.executeQuery()) {
                if (!resultSet.next())
                    throw new SQLException("OWL object with ID " + id + " does not have IC information");
                ratioExternalSeco = extrinsicICValue / resultSet.getDouble(1);
                ratioExternalZhou = extrinsicICValue / resultSet.getDouble(2);
                ratioExternalSanchez = extrinsicICValue / resultSet.getDouble(3);
                ratioExternalLeaves = extrinsicICValue / resultSet.getDouble(4);
            }
        }
    }
    
    
    private int countChildren(int id) throws SQLException {
        countChildrenStatement.setInt(1, id);
        try (ResultSet resultSet = countChildrenStatement.executeQuery()) {
            if (resultSet.next())
                return resultSet.getInt(1);
        }
        
        return 0;
    }
    
    
    private int[] getChildren(int id) throws SQLException {
        getChildrenStatement.setInt(1, id);
        
        try (ResultSet resultSet = getChildrenStatement.executeQuery()) {
            int[] result = new int[SQLCoreUtils.countRows(resultSet)];
            int i = 0;
            while (resultSet.next()) {
                result[i] = resultSet.getInt(1);
                i++;
            }
            return result;
        }
    }
    
    
    private void getHIndex(int[] children) throws SQLException {
        if (children.length == 0) {
            hIndex = 0;
            hIndexAdjusted = Integer.MAX_VALUE;
            return;
        }
        
        // For each children, count the number of their children
        int[] grandchildren = new int[children.length];
        for (int i = 0; i < children.length; i++) {
            grandchildren[i] = countChildren(children[i]);
        }
        
        Arrays.sort(grandchildren);
        
        if (grandchildren[grandchildren.length - 1] == 0) {
            // All children have 0 children of their own; so this class has the maximum relevance
            hIndex = 0;
            hIndexAdjusted = Integer.MAX_VALUE;
            return;
        }
        
        int result = 0;
        for (int i = 1; i <= grandchildren.length; i++) {
            int index = grandchildren.length - i;
            if (grandchildren[index] >= i)
                result++;
            else
                break;
        }
        
        hIndex = hIndexAdjusted = result;
        return;
    }
    
    
    @Override
    public void prepareForFirstUse() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            // n_children --> number of direct children
            // n_children_adjusted --> number of direct children; Integer.MAX_VALUE for leaves
            // h_index --> class has at least h children each with h children as well
            // h_index_adjusted --> like h_index, but Integer.MAX_VALUE for leaves and leaf parents
            // ratio_leaves --> number of subclasses that are leaves / total number of leaves
            // ratio_external_* --> IC ratios between external and internal measures
            
            if (useIC) {
                statement.executeUpdate(""
                        + "CREATE TABLE relevance ("
                        + "  id INT PRIMARY KEY, "
                        + "  n_children INT NOT NULL, "
                        + "  n_children_adjusted INT NOT NULL, "
                        + "  h_index INT NOT NULL, "
                        + "  h_index_adjusted INT NOT NULL, "
                        + "  ratio_leaves DOUBLE NOT NULL, "
                        + "  ratio_external_seco DOUBLE NOT NULL, "
                        + "  ratio_external_zhou DOUBLE NOT NULL, "
                        + "  ratio_external_sanchez DOUBLE NOT NULL, "
                        + "  ratio_external_leaves DOUBLE NOT NULL"
                        + ")");
            }
            else {
                statement.executeUpdate(""
                        + "CREATE TABLE relevance ("
                        + "  id INT PRIMARY KEY NOT NULL, "
                        + "  n_children INT NOT NULL, "
                        + "  n_children_adjusted INT NOT NULL, "
                        + "  h_index INT NOT NULL, "
                        + "  h_index_adjusted INT NOT NULL, "
                        + "  ratio_leaves DOUBLE NOT NULL"
                        + ")");
            }
        }
    }
    
    
    @Override
    public void update() throws SQLException {
        getChildrenStatement = getConnection().prepareStatement(""
                + "SELECT subclass "
                + "FROM hierarchy "
                + "WHERE superclass = ?"
                + "  AND distance = 1");
        countChildrenStatement = getConnection().prepareStatement(""
                + "SELECT COUNT(subclass) "
                + "FROM hierarchy "
                + "WHERE superclass = ?"
                + "  AND distance = 1");
        
        if (useIC) {
            getICStatement = getConnection().prepareStatement(""
                    + "SELECT seco, zhou, sanchez, leaves "
                    + "FROM intrinsic_ic "
                    + "WHERE id = ?");
        }
        
        String query;
        if (useIC) {
            query = ""
                    + "INSERT INTO relevance ("
                    + "    id, "
                    + "    n_children, "
                    + "    n_children_adjusted, "
                    + "    h_index, "
                    + "    h_index_adjusted, "
                    + "    ratio_leaves, "
                    + "    ratio_external_seco, "
                    + "    ratio_external_zhou, "
                    + "    ratio_external_sanchez, "
                    + "    ratio_external_leaves)"
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }
        else {
            query = ""
                    + "INSERT INTO relevance ("
                    + "    id, "
                    + "    n_children, "
                    + "    n_children_adjusted, "
                    + "    h_index, "
                    + "    h_index_adjusted, "
                    + "    ratio_leaves)"
                    + "VALUES (?, ?, ?, ?, ?, ?)";
        }
        
        try (PreparedStatement insertStatement = getConnection().prepareStatement(query)) {
            // Start by getting a reference to all the classes
            HashSet<OWLClass> allClasses = utils.getAllEntities(EntityType.CLASS);
            totalLeaves = leaves.getNumberOfLeaves();
            
            int counter = 0;
            for (OWLClass owlClass : allClasses) {
                int id = utils.getID(owlClass);
                if (id == -1)
                    continue;
                
                calculate(id);
                insertStatement.setInt(1, id);
                insertStatement.setDouble(2, nChildren);
                insertStatement.setDouble(3, nChildrenAdjusted);
                insertStatement.setDouble(4, hIndex);
                insertStatement.setDouble(5, hIndexAdjusted);
                insertStatement.setDouble(6, ratioLeaves);
                
                if (useIC) {
                    insertStatement.setDouble(6, ratioExternalSeco);
                    insertStatement.setDouble(7, ratioExternalZhou);
                    insertStatement.setDouble(8, ratioExternalSanchez);
                    insertStatement.setDouble(9, ratioExternalLeaves);
                }
                
                insertStatement.addBatch();
                
                counter++;
                if (counter % 1000 == 0) {
                    getLogger().info("... relevance for " + counter + " classes found ...");
                    insertStatement.executeBatch();
                }
            }
            
            insertStatement.executeBatch();
        }
    }
    
    
    @Override
    public void removeFromDatabase() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            statement.executeUpdate("DROP TABLE relevance");
        }
    }
}
