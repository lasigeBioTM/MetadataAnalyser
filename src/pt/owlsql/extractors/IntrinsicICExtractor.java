package pt.owlsql.extractors;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.semanticweb.owlapi.model.EntityType;

import pt.ma.json.JSONException;
import pt.owlsql.DependencyException;
import pt.owlsql.Extractor;

import com.google.gson.JsonElement;


public final class IntrinsicICExtractor extends Extractor {
    
    private SQLCoreUtils utils;
    private HierarchyExtractor hierarchy;
    private LeavesExtractor leaves;
    
    private double zhouK;
    private double secoIC;
    private double zhouIC;
    private double sanchezIC;
    private double leavesIC;
    private double log_tC;
    private double log_tL;
    private double log_mD1;
    private int maxDepth;
    
    
    @Override
    protected String[] getMandatoryOptions() {
        return new String[] { "zhou_k" };
    }
    
    
    @Override
    protected void processOption(String key, JsonElement element) throws JSONException {
        if (key.equals("zhou_k")) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber())
                throw new JSONException("must be a number");
            
            zhouK = element.getAsDouble();
            if (zhouK < 0 || zhouK > 1)
                throw new JSONException("must be a number between 0 and 1");
        }
        else {
            super.processOption(key, element);
        }
    }
    
    
    @Override
    public Extractor[] getDirectDependencies() throws DependencyException {
        utils = getDependency(SQLCoreUtils.class);
        hierarchy = getDependency(HierarchyExtractor.class);
        leaves = getDependency(LeavesExtractor.class);
        
        return new Extractor[] { utils, hierarchy };
    }
    
    
    @Override
    public void prepareForFirstUse() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            statement.executeUpdate(""
                    + "CREATE TABLE intrinsic_ic ("
                    + "  id INT NOT NULL,"
                    + "  seco DOUBLE NOT NULL,"
                    + "  zhou DOUBLE NOT NULL,"
                    + "  sanchez DOUBLE NOT NULL,"
                    + "  leaves DOUBLE NOT NULL,"
                    + "  UNIQUE (id)"
                    + ")");
        }
    }
    
    
    @Override
    public void removeFromDatabase() throws SQLException {
        try (Statement statement = getConnection().createStatement()) {
            statement.executeUpdate("DROP TABLE intrinsic_ic");
        }
    }
    
    
    private void calculate(int id) throws SQLException {
        int nDescendants = hierarchy.getNumberOfSubclasses(id);
        int nAncestors = hierarchy.getNumberOfSuperclasses(id);
        int nLeaves = leaves.getLeafDescendantsSize(id);
        int depth = hierarchy.getDepth(id);
        
        double log_nD = Math.log(nDescendants);
        double log_nL = Math.log(nLeaves);
        
        // Formulas are correct but look different to increase calculation speed
        // They are also normalized so that all scores are form 0 to 1
        secoIC = 1 - log_nD / log_tC;
        zhouIC = zhouK * secoIC + (1 - zhouK) * Math.log(depth + 1) / log_mD1;
        sanchezIC = (log_tL + Math.log(nAncestors) - log_nL) / (log_tC + log_tL);
        leavesIC = 1 - log_nL / log_tL;
    }
    
    
    @Override
    public void update() throws SQLException {
        // As a SingleUseExtractor, an update is equivalent to removing everything we have stored and starting from
        // scratch. We implement this by running the two methods removeFromDatabase() and prepareForFirstUse().
        removeFromDatabase();
        prepareForFirstUse();
        
        try (PreparedStatement insertStatement = getConnection().prepareStatement(""
                + "INSERT INTO intrinsic_ic (id, seco, zhou, sanchez, leaves) "
                + "VALUES (?, ?, ?, ?, ?)")) {
            
            getLogger().info("Finding all the intrinsic IC values (SECO, ZHOU, SANCHEZ and LEAVES)");
            
            // Start by getting a reference to all the classes
            int[] allClasses = utils.getAllIds(EntityType.CLASS);
            log_tC = Math.log(allClasses.length);
            log_tL = Math.log(leaves.getNumberOfLeaves());
            
            maxDepth = hierarchy.getMaxDepth();
            log_mD1 = Math.log(maxDepth + 1);
            
            int counter = 0;
            for (int id : allClasses) {
                calculate(id);
                insertStatement.setInt(1, id);
                insertStatement.setDouble(2, secoIC);
                insertStatement.setDouble(3, zhouIC);
                insertStatement.setDouble(4, sanchezIC);
                insertStatement.setDouble(5, leavesIC);
                insertStatement.addBatch();
                
                counter++;
                if (counter % 1000 == 0) {
                    getLogger().info("... IC for " + counter + " classes found ...");
                    insertStatement.executeBatch();
                }
            }
            
            insertStatement.executeBatch();
        }
    }
}
