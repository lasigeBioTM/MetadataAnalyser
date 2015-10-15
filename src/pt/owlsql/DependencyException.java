package pt.owlsql;


public class DependencyException extends Exception {
    
    private static final long serialVersionUID = -956669828519023232L;
    
    
    public DependencyException(ExtractorSpec<?> spec, Class<?> dependency) {
        super(spec.toJSONString() + " depends on " + dependency + "; dependency cannot be satisfied.");
    }
}
