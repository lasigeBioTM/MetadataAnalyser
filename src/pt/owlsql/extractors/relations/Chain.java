package pt.owlsql.extractors.relations;

import java.util.ArrayList;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;


public final class Chain {
    
    private final OWLClass endPoint;
    private final ArrayList<OWLObjectProperty> reversedChain;
    
    
    public Chain(Chain chain) {
        this.endPoint = chain.endPoint;
        this.reversedChain = new ArrayList<>(chain.reversedChain);
    }
    
    
    public Chain(OWLClass endPoint) {
        this.endPoint = endPoint;
        this.reversedChain = new ArrayList<>();
    }
    
    
    public Chain(OWLObjectProperty[] properties, OWLClass endPoint) {
        this.endPoint = endPoint;
        this.reversedChain = new ArrayList<>();
        for (int i = properties.length - 1; i >= 0; i--) {
            reversedChain.add(properties[i]);
        }
    }
    
    
    public OWLObjectProperty[] getChain() {
        OWLObjectProperty[] result = new OWLObjectProperty[reversedChain.size()];
        int length = result.length;
        for (int i = 0; i < length; i++) {
            result[i] = reversedChain.get(length - i - 1);
        }
        return result;
    }
    
    
    public OWLClass getEndPoint() {
        return endPoint;
    }
    
    
    public void prepend(OWLObjectProperty property) {
        reversedChain.add(property);
    }
    
    
    public int propertiesLength() {
        return reversedChain.size();
    }
}