package pt.owlsql.extractors.relations;

import java.util.ArrayList;
import java.util.Iterator;

import org.semanticweb.owlapi.model.OWLObjectProperty;


public class RelationsStore implements Iterable<Chain> {
    
    private static final RelationsStore EMPTY = new RelationsStore() {
        @Override
        public void add(Chain chain) {
            throw new UnsupportedOperationException("Cannot change the immutable "
                    + RelationsStore.class.getName()
                    + "#EMPTY object");
        }
        
        
        @Override
        public void addAll(RelationsStore other) {
            throw new UnsupportedOperationException("Cannot change the immutable "
                    + RelationsStore.class.getName()
                    + "#EMPTY object");
        }
    };
    
    
    public static RelationsStore empty() {
        return EMPTY;
    }
    
    
    private final ArrayList<Chain> inner;
    
    
    public RelationsStore() {
        this.inner = new ArrayList<>();
    }
    
    
    public void add(Chain chain) {
        inner.add(chain);
    }
    
    
    public void addAll(RelationsStore other) {
        for (Chain chain : other.inner) {
            inner.add(new Chain(chain));
        }
    }
    
    
    @Override
    public final Iterator<Chain> iterator() {
        return new Iterator<Chain>() {
            
            Iterator<Chain> underlying = inner.iterator();
            
            
            @Override
            public boolean hasNext() {
                return underlying.hasNext();
            }
            
            
            @Override
            public Chain next() {
                return underlying.next();
            }
            
            
            @Override
            public void remove() {
                throw new UnsupportedOperationException("Operation not supported.");
            }
        };
    }
    
    
    public final RelationsStore prepend(OWLObjectProperty property) {
        RelationsStore result = new RelationsStore();
        for (Chain chain : this) {
            Chain resultChain = new Chain(chain);
            resultChain.prepend(property);
            result.inner.add(resultChain);
        }
        return result;
    }
}