package pt.owlsql;

import java.util.Hashtable;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


public class ExtractorSpec<E extends Extractor> {
    
    private static Gson gson = new Gson();
    
    private final Class<E> extractorClass;
    private final Hashtable<String, JsonElement> parameters;
    
    
    public ExtractorSpec(Class<E> extractorClass) {
        this.extractorClass = extractorClass;
        this.parameters = new Hashtable<>();
    }
    
    
    public ExtractorSpec(Class<E> cls, Hashtable<String, JsonElement> parameters) {
        this.extractorClass = cls;
        this.parameters = parameters;
    }
    
    
    public Class<E> getExtractorClass() {
        return extractorClass;
    }
    
    
    public Hashtable<String, JsonElement> getParameters() {
        return parameters;
    }
    
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (extractorClass == null ? 0 : extractorClass.hashCode());
        result = prime * result + (parameters == null ? 0 : parameters.hashCode());
        return result;
    }
    
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        
        if (obj == null)
            return false;
        
        if (getClass() != obj.getClass())
            return false;
        
        ExtractorSpec<?> other = (ExtractorSpec<?>) obj;
        if (extractorClass == null) {
            if (other.extractorClass != null)
                return false;
        }
        else if (extractorClass != other.extractorClass)
            return false;
        
        if (parameters == null) {
            if (other.parameters != null)
                return false;
        }
        else if (!parameters.equals(other.parameters))
            return false;
        
        return true;
    }
    
    
    public String toJSONString() {
        JsonObject object = new JsonObject();
        object.addProperty("class", extractorClass.getName());
        for (Entry<String, JsonElement> entry : parameters.entrySet()) {
            object.add(entry.getKey(), entry.getValue());
        }
        return gson.toJson(object);
    }
}
