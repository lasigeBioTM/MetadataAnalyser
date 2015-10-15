package pt.owlsql;

import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;


public class DependencySpec<U extends Extractor> {
    
    private static class Ranges {
        double[] starts;
        double[] ends;
        
        
        public Ranges(String rangeSpec) throws NumberFormatException {
            String[] fields = rangeSpec.split(",");
            starts = new double[fields.length];
            ends = new double[fields.length];
            int i = 0;
            
            for (String rangeItem : fields) {
                double start;
                double end;
                if (!rangeItem.contains("-"))
                    start = end = Double.parseDouble(rangeItem.trim());
                
                else {
                    String[] innerFields = rangeItem.split("-");
                    if (innerFields.length != 2)
                        throw new NumberFormatException("Ranges must have a single \"-\"");
                    
                    if (innerFields[0].trim().isEmpty())
                        start = Double.NEGATIVE_INFINITY;
                    else
                        start = Double.parseDouble(innerFields[0].trim());
                    
                    if (innerFields[1].trim().isEmpty())
                        end = Double.POSITIVE_INFINITY;
                    else
                        end = Double.parseDouble(innerFields[1].trim());
                }
                
                starts[i] = start;
                ends[i] = end;
                i++;
            }
        }
        
        
        public boolean inRange(double value) {
            for (int i = 0; i < starts.length; i++) {
                if (starts[i] <= value && value <= ends[i])
                    return true;
            }
            return false;
        }
    }
    
    
    private final Class<U> extractorClass;
    private final Hashtable<String, Pattern> stringRegex = new Hashtable<>();
    private final Hashtable<String, Ranges> numericRanges = new Hashtable<>();
    
    
    public DependencySpec(Class<U> extractorClass) {
        this.extractorClass = extractorClass;
    }
    
    
    public void putStringCriteria(String parameter, String regex) {
        stringRegex.put(parameter, Pattern.compile(regex));
    }
    
    
    public void putNumericCriteria(String parameter, String ranges) {
        numericRanges.put(parameter, new Ranges(ranges));
    }
    
    
    public Class<U> getExtractorClass() {
        return extractorClass;
    }
    
    
    public boolean noParameterCriteria() {
        return stringRegex.isEmpty() && numericRanges.isEmpty();
    }
    
    
    public boolean isSatisfiedBy(ExtractorSpec<?> extractorSpec) {
        if (extractorSpec.getExtractorClass() != extractorClass)
            return false;
        
        for (Entry<String, Pattern> entrySet : stringRegex.entrySet()) {
            String key = entrySet.getKey();
            if (!extractorSpec.getParameters().containsKey(key))
                return false;
            
            JsonElement providedValue = extractorSpec.getParameters().get(key);
            if (!providedValue.isJsonPrimitive() || providedValue.getAsJsonPrimitive().isString())
                return false;
            
            Pattern pattern = entrySet.getValue();
            Matcher matcher = pattern.matcher(providedValue.getAsString());
            if (!matcher.matches())
                return false;
        }
        
        for (Entry<String, Ranges> entrySet : numericRanges.entrySet()) {
            String key = entrySet.getKey();
            if (!extractorSpec.getParameters().containsKey(key))
                return false;
            
            JsonElement providedValue = extractorSpec.getParameters().get(key);
            if (!providedValue.isJsonPrimitive() || providedValue.getAsJsonPrimitive().isNumber())
                return false;
            
            Ranges ranges = entrySet.getValue();
            if (ranges.inRange(providedValue.getAsDouble()))
                return false;
        }
        
        return true;
    }
    
    
    @Override
    public String toString() {
        String result = "DependencySpec [extractorClass=" + extractorClass.getName();
        if (!stringRegex.isEmpty())
            result += ", stringRegex=" + stringRegex;
        if (!numericRanges.isEmpty())
            result += ", numericRanges=" + numericRanges;
        result += "]";
        return result;
    }
    
}
