package pt.ma.json;


public class JSONException extends Exception {
    
    private static final long serialVersionUID = -8597449597021295272L;
    
    
    private static String createMessage(String message, String[] path) {
        StringBuilder sb = new StringBuilder();
        
        if (path != null) {
            for (String part : path) {
                if (!part.startsWith("["))
                    sb.append("/");
                sb.append(part);
            }
        }
        
        if (sb.length() == 0) // If no path part was added, do not prepend the path to the message
            return message;
        
        sb.insert(0, "\"").append("\": ").append(message);
        return sb.toString();
    }
    
    
    private String[] path;
    private final String rawMessage;
    
    
    public JSONException(String message, Throwable cause, String ... path) {
        super(createMessage(message, path), cause);
        
        this.rawMessage = message;
        this.path = path.clone();
    }
    
    
    public JSONException(String message, String ... path) {
        this(message, null, path);
    }
    
    
    private JSONException(JSONException inner, String[] path) {
        super(createMessage(inner.rawMessage, path));
        
        this.rawMessage = inner.rawMessage;
        this.path = path.clone();
        this.setStackTrace(inner.getStackTrace());
        if (inner.getCause() != null)
            initCause(inner.getCause());
    }
    
    
    public JSONException withPrefix(String ... prefix) {
        int i = 0;
        String[] newPath = new String[prefix.length + path.length];
        for (String part : prefix) {
            newPath[i] = part;
            i++;
        }
        for (String part : path) {
            newPath[i] = part;
            i++;
        }
        
        return new JSONException(this, newPath);
    }
    
    
    public String[] getPath() {
        return path;
    }
}
