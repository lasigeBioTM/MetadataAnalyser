package pt.owlsql;

public class OwlSqlException extends Exception {
    
    private static final long serialVersionUID = -5112162312130254973L;
    
    
    public OwlSqlException(String message) {
        super(message);
    }
    
    
    public OwlSqlException(String message, Throwable e) {
        super(message, e);
    }
}
